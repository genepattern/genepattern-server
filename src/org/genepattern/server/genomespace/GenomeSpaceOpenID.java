package org.genepattern.server.genomespace;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.openid4java.OpenIDException;
import org.openid4java.association.AssociationSessionType;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.InMemoryConsumerAssociationStore;
import org.openid4java.consumer.InMemoryNonceVerifier;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.MessageException;
import org.openid4java.message.Parameter;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.sreg.SRegRequest;
import org.openid4java.util.HttpClientFactory;
import org.openid4java.util.ProxyProperties;
/**
 * Servlet for handling authentication with GenomeSpace OpenID.
 * Based off the GenomeSpace example OpenID servlet.
 * @author tabor
 */
public class GenomeSpaceOpenID extends HttpServlet {
    private static Logger log = Logger.getLogger(GenomeSpaceOpenID.class);
    
    // These flags control which extension type to use. If neither is set the
    // the GenomeSpace Provider will create a custom extension and pass the
    // token, username, and email.
    private static final boolean USE_SREG = true;
    private static final boolean USE_AX = true;

    private static final long serialVersionUID = -1L;
    private ConsumerManager manager;
    private static final String SESSION_TOKEN = "gs-token";
    private static final String SESSION_USERNAME = "gs-username";
    
    public static final String EXTENSION_URI = "http://identity.genomespace.org/openid/";
    public static final String TOKEN_ALIAS = "gs-token";
    public static final String USERNAME_ALIAS = "gs-username";
    public static final String EMAIL_ALIAS = "email";
    
    private String getProviderURL() {
        Context context = Context.getServerContext();
        String gsUrl = ServerConfiguration.instance().getGPProperty(context, "genomeSpaceUrl", "https://identityTest.genomespace.org:8444/identityServer/xrd.jsp");
        return gsUrl;
    }

    /** Returns the URL to GenomeSpace server OpenId logout page. */
    private String getGsLogoutUrl() {
        int idx = getProviderURL().lastIndexOf("identityServer");
        if (idx < 0) { return null; }
        return getProviderURL().substring(0, idx) + "identityServer/openIdProvider?_action=logout";
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        // --- Forward proxy setup (only if needed) ---
        ProxyProperties proxyProps = getProxyProperties(config);
        if (proxyProps != null) {
            log.debug("ProxyProperties: " + proxyProps);
            HttpClientFactory.setProxyProperties(proxyProps);
        }

        try {
            manager = new ConsumerManager();
        }
        catch (Throwable e) {
            throw new ServletException(e);
        }
        manager.setAssociations(new InMemoryConsumerAssociationStore());
        manager.setNonceVerifier(new InMemoryNonceVerifier(5000));
        manager.setMinAssocSessEnc(AssociationSessionType.DH_SHA256);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    // This gets called initially by index.jsp and later by openId provider.
    // OpenId Provider POSTs back to this servlet with ?is_return=true.
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Clears out the error message on the JSP
        req.getSession().removeAttribute("gsOIcClientMessage");

        if ("true".equals(req.getParameter("is_cancel"))) {
            // User clicked "cancel" on the openId login page, so do nothing.
            displayResult(req, resp, null, null, null, "Login was canceled.");

        }
        else if ("Logout".equals(req.getParameter("logout"))) {
            // Logs out of open id by removing server side cookies, and session
            // username and token.
            removeGenomeSpaceInfo(req);
            doOpenIdLogout(req, resp);

        }
        else if ("true".equals(req.getParameter("is_return"))) {
            // Handles the Auth Response callback from OpenId Provider.
            // OpenId protocol requires the response be verified.
            // Login token and username are gotten from the OpenId
            // MessageExtension.
            ParameterList paramList = verifyResponse(req);
            String token = null;
            String username = null;
            String email = null;

            if (paramList != null) {
                if (paramList.hasParameter(TOKEN_ALIAS) && paramList.hasParameter(USERNAME_ALIAS)) {
                    token = paramList.getParameterValue(TOKEN_ALIAS);
                    username = paramList.getParameterValue(USERNAME_ALIAS);
                    email = paramList.getParameterValue(EMAIL_ALIAS);
                }
                else {
                    String msg = "OpenId login succeeded but OpenId Provider did not return " + (!paramList.hasParameter(TOKEN_ALIAS) ? "token " : "") + (!paramList.hasParameter(USERNAME_ALIAS) ? "and username " : "");
                    log.error(msg);
                }
            }
            if (token == null || token.length() == 0 || username == null || username.length() == 0) {
                removeGenomeSpaceInfo(req);
                displayResult(req, resp, null, null, null, "OpenId login failed.");
            }
            else {
                putGenomeSpaceInfo(req, token, username);
                displayResult(req, resp, username, token, email, null);
            }

        }
        else {
            // Does an openId login, which will put up a login page if user
            // has not previously logged into the openId server.
            authRequest(getProviderURL(), req, resp);
        }
    }

    private void putGenomeSpaceInfo(HttpServletRequest req, String token, String username) {
        req.getSession().setAttribute(SESSION_TOKEN, token);
        req.getSession().setAttribute(SESSION_USERNAME, username);
    }

    private void removeGenomeSpaceInfo(HttpServletRequest req) {
        req.getSession().removeAttribute(SESSION_TOKEN);
        req.getSession().removeAttribute(SESSION_USERNAME);
    }

    /**
     * The result of the login is displayed in a web page. This is where a real
     * app would forward or otherwise connect. Here we go to the datamanager
     * default page. This shows that the openId cookie is sent by the browser to
     * dm.
     */
    private void displayResult(HttpServletRequest req, HttpServletResponse resp, String username, String token, String email, String error) throws ServletException, IOException {
        if (error != null) {
            req.getSession().setAttribute("gsOIcClientMessage", error);
            resp.sendRedirect("/gp/pages/genomespace/index.jsp");
        }
        else {
            // Set session variables for GS
            req.getSession().setAttribute(GenomeSpaceBean.GS_USER_KEY, username);
            req.getSession().setAttribute(GenomeSpaceBean.GS_TOKEN_KEY, token);
            req.getSession().setAttribute(GenomeSpaceBean.GS_EMAIL_KEY, email);
            
            // Check if there is an associated GP Account and forward to appropriate place
            boolean isAccountAssociated = GenomeSpaceLoginManager.isGSAccountAssociated(username);
            if (isAccountAssociated) {
                resp.sendRedirect("/gp/pages/index.jsf"); 
            }
            else {
                resp.sendRedirect("/gp/pages/genomespace/associateAccount.jsf");
            }
        }
    }

    /** 
     * Removes the GenomeSpace server cookies to effect an open id logout. 
     */
    private void doOpenIdLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String url = getGsLogoutUrl();
        if (url != null) {
            log.info("Logging out at " + url);
            resp.sendRedirect(url);
        }
        else {
            displayResult(req, resp, null, null, null, "OpenId Provider does not support logout.");
        }
    }

    /** Logs into OpenId */
    @SuppressWarnings("rawtypes")
    private void authRequest(String claimedId, HttpServletRequest httpReq, HttpServletResponse httpResp) throws IOException, ServletException {
        try {
            // "return_to URL" needs to come back to this servlet
            // in order to verify the OP response.
            String returnToUrl = httpReq.getRequestURL().toString() + "?is_return=true";

            // Performs openId discovery, puts association into in-memory
            // store, and creates the auth request.
            List discoveries = manager.discover(claimedId);
            DiscoveryInformation discovered = manager.associate(discoveries);
            httpReq.getSession().setAttribute("openid-disc", discovered);
            AuthRequest authReq = manager.authenticate(discovered, returnToUrl);

            // This block of code is needed only when you choose OpenId
            // Attribute Exchange
            // to get the gs-token, gs-username, and email. It is included here
            // to illustrate
            // AX usage, and also to test the GenomeSpace OpenId Provider.
            if (USE_AX) {
                // Add Attribute Exchange request for gs token and username
                FetchRequest fetch = FetchRequest.createFetchRequest();
                fetch.addAttribute(TOKEN_ALIAS, EXTENSION_URI + TOKEN_ALIAS, true);
                fetch.addAttribute(USERNAME_ALIAS, EXTENSION_URI + USERNAME_ALIAS, true);
                fetch.addAttribute(EMAIL_ALIAS, EXTENSION_URI + EMAIL_ALIAS, true);
                authReq.addExtension(fetch);
            }

            // This block of code is needed only when you choose OpenId Simple
            // Registration
            // to get the gs-token, gs-username, and email. It is included here
            // to illustrate
            // SReg usage, and also to test the GenomeSpace OpenId Provider.
            if (USE_SREG) {
                // Add Simple Registration request for gs token and username.
                // SReg implementation
                // disallows these names, so fudge it by using "gender" and
                // "nickname" in place of
                // "gs-token" and "gs-username" respectively. GenomeSpace OpenId
                // provider will
                // play along with this little fiction and return the expected
                // values.
                SRegRequest sregReq = SRegRequest.createFetchRequest();
                sregReq.addAttribute("gender", true);
                sregReq.addAttribute("nickname", true);
                sregReq.addAttribute("email", true);
                authReq.addExtension(sregReq);
            }

            if (discovered.isVersion2()) { throw new ServletException("No support for HTML PUT redirect from OpenId Provider."); }
            httpResp.sendRedirect(authReq.getDestinationUrl(true));

        }
        catch (org.openid4java.discovery.yadis.YadisException e) {
            log.error("Error requesting OpenId authentication.", e);
            displayResult(httpReq, httpResp, null, null, null, "OpenId Provider XRD " + getProviderURL() + " is unavailable.<BR/>The internal error is <CODE>" + e.getMessage() + "</CODE><P/>");
        }
        catch (OpenIDException e) {
            log.error("Error requesting OpenId authentication.", e);
            displayResult(httpReq, httpResp, null, null, null, "Error requesting OpenId authentication.<BR/>The internal error is <CODE>" + e.getMessage() + "</CODE><P/>");
        }
    }

    /**
     * If OP's response verifies OK, returns the ParameterList containing the
     * GenomeSpace token and username, if they are found in the response, and an
     * empty ParameterList if not found. If the verify fails, returns null.
     */
    private ParameterList verifyResponse(HttpServletRequest httpReq) throws ServletException {
        try {
            // extract the parameters from the authentication response
            // (which comes in as a HTTP request from the OpenID provider)
            ParameterList response = new ParameterList(httpReq.getParameterMap());

            // retrieve the previously stored discovery information
            DiscoveryInformation discovered = (DiscoveryInformation) httpReq.getSession().getAttribute("openid-disc");

            // extract the receiving URL from the HTTP request
            StringBuffer receivingURL = httpReq.getRequestURL();
            String queryString = httpReq.getQueryString();
            if (queryString != null && queryString.length() > 0) receivingURL.append("?").append(httpReq.getQueryString());

            // Response must match what was used to place the authentication
            // request.
            VerificationResult verification = manager.verify(receivingURL.toString(), response, discovered);

            AuthSuccess authSuccess = null;
            if (verification.getVerifiedId() == null) {
                // Client verification can fail despite the server doing a
                // successful login (possibly related to http->https redirect?)
                // This means the server will have set token and username
                // cookies in genomespace.org domain, which means GenomeSpace
                // will allow access. Ideally the client here should redirect
                // to the openId logout page to remove those cookies.
                log.info("OpenId Client side verification failed.");
                removeGenomeSpaceInfo(httpReq);
            }
            else {
                log.info("OpenId Client side verification succeeded.");
                authSuccess = (AuthSuccess) verification.getAuthResponse();
            }
            return extractGenomespaceToken(authSuccess);

        }
        catch (OpenIDException e) {
            // present error to the user
            throw new ServletException(e);
        }
    }

    /** Extracts the GenomeSpace token from the OpenId message. */
    private ParameterList extractGenomespaceToken(AuthSuccess authSuccess) throws MessageException {
        if (authSuccess == null) { return null; }
        ParameterList returnList = new ParameterList();
        
        String token = authSuccess.getParameterValue("openid.ext1.value.gs-token");
        String username = authSuccess.getParameterValue("openid.ext1.value.gs-username");
        String email = authSuccess.getParameterValue("openid.ext1.value.email");
        
        returnList.set(new Parameter(TOKEN_ALIAS, token));
        returnList.set(new Parameter(USERNAME_ALIAS, username));
        returnList.set(new Parameter(EMAIL_ALIAS, email));
        
        return returnList;
    }

    /**
     * Get proxy properties from the context init params.
     * 
     * @return proxy properties
     */
    private static ProxyProperties getProxyProperties(ServletConfig config) {
        ProxyProperties proxyProps;
        String host = config.getInitParameter("proxy.host");
        log.debug("proxy.host: " + host);
        if (host == null) {
            proxyProps = null;
        }
        else {
            proxyProps = new ProxyProperties();
            String port = config.getInitParameter("proxy.port");
            String username = config.getInitParameter("proxy.username");
            String password = config.getInitParameter("proxy.password");
            String domain = config.getInitParameter("proxy.domain");
            proxyProps.setProxyHostName(host);
            proxyProps.setProxyPort(Integer.parseInt(port));
            proxyProps.setUserName(username);
            proxyProps.setPassword(password);
            proxyProps.setDomain(domain);
        }
        return proxyProps;
    }
}
