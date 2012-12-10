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
    
    // These flags control which extension type to use. If neither is set the the GenomeSpace 
    // Provider will create a custom extension and pass the token, username, and email.
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
    
    /**
     * Reads the url of the GenomeSpace OpenID provider from config, uses if the test URL if the config is missing
     * @return
     */
    private String getProviderURL() {
        Context context = Context.getServerContext();
        String gsUrl = ServerConfiguration.instance().getGPProperty(context, "genomeSpaceUrl", "https://identity.genomespace.org:8444/identityServer/xrd.jsp");
        log.debug("Getting the OpenID provider URL.  URL is: " + gsUrl);
        return gsUrl;
    }

    /** 
     * Returns the URL to GenomeSpace server OpenId logout page. 
     * */
    private String getGsLogoutUrl() {
        int idx = getProviderURL().lastIndexOf("identityServer");
        if (idx < 0) { return null; }
        String gsUrl = getProviderURL().substring(0, idx) + "identityServer/openIdProvider?_action=logout";
        gsUrl += "&logout_return_to=" + ServerConfiguration.instance().getGenePatternURL();
        log.debug("Getting the OpenID logout URL.  URL is: " + gsUrl);
        return gsUrl;
    }
    
    /**
     * Initializes the OpenID servlet
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        log.debug("Initializing the OpenID servlet");
        super.init(config);

        // --- Forward proxy setup (only if needed) ---
        log.debug("Setting up the proxy properties");
        ProxyProperties proxyProps = getProxyProperties(config);
        if (proxyProps != null) {
            log.debug("ProxyProperties: " + proxyProps);
            HttpClientFactory.setProxyProperties(proxyProps);
        }

        try {
            log.debug("Initializing the consumer manager");
            manager = new ConsumerManager();
        }
        catch (Throwable e) {
            throw new ServletException(e);
        }
        manager.setAssociations(new InMemoryConsumerAssociationStore());
        manager.setNonceVerifier(new InMemoryNonceVerifier(5000));
        manager.setMinAssocSessEnc(AssociationSessionType.DH_SHA256);
    }
    
    /**
     * GET requests to the servlet are simply handled the same as POST requests
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.debug("OpenID servlet handling GET request");
        doPost(req, resp);
    }

    /**
     * Handle POST requests to the servlet.
     * This gets called when the user clicks to login through GenomeSpace, upon return from logging into GenomeSpace
     * or when the user wants to log out of GenomeSpace.
     * 
     * LOGIN:   ?login=Login
     * RETURN:  ?is_return=true
     * LOGOUT:  ?logout=Logout
     * CANCEL:  ?is_cancel=true
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.debug("OpenID servlet handling POST request");
        // Clears out the error message on the JSP
        req.getSession().removeAttribute("gsOIcClientMessage");

        if ("true".equals(req.getParameter("is_cancel"))) {
            log.debug("OpenID servlet found CANCEL action");
            // User clicked "cancel" on the openId login page, so do nothing.
            displayResult(req, resp, null, null, null, "Login was canceled.");

        }
        else if ("Logout".equals(req.getParameter("logout"))) {
            // Logs out of open id by removing server side cookies, and session username and token.
            log.debug("OpenID servlet found LOGOUT action");
            removeGenomeSpaceInfo(req);
            doOpenIdLogout(req, resp);

        }
        else if ("true".equals(req.getParameter("is_return"))) {
            // Handles the Auth Response callback from OpenId Provider. OpenId protocol requires the response be verified.
            // Login token and username are gotten from the OpenId MessageExtension.
            log.debug("OpenID servlet found RETURN action");
            ParameterList paramList = verifyResponse(req);
            String token = null;
            String username = null;
            String email = null;
            
            log.debug("ParameterList has been obtained: " + paramList);
            if (paramList != null) {
                if (paramList.hasParameter(TOKEN_ALIAS) && paramList.hasParameter(USERNAME_ALIAS)) {
                    log.debug("Getting Parameter Values");
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
                log.debug("OpenID login failed, displaying failure results");
                removeGenomeSpaceInfo(req);
                displayResult(req, resp, null, null, null, "OpenId login failed.");
            }
            else {
                log.debug("OpenID login was success, displaying results");
                putGenomeSpaceInfo(req, token, username);
                displayResult(req, resp, username, token, email, null);
            }

        }
        else {
            // Does an openId login, which will put up a login page if user has not previously logged into the openId server.
            log.debug("OpenID servlet assuming LOGIN action");
            authRequest(getProviderURL(), req, resp);
        }
    }
    
    /**
     * Attaches GenomeSpace info into the session for the purposes of OpenID authentication
     * @param req
     * @param token
     * @param username
     */
    private void putGenomeSpaceInfo(HttpServletRequest req, String token, String username) {
        log.debug("Attaching GenomeSpace info to session");
        req.getSession().setAttribute(SESSION_TOKEN, token);
        req.getSession().setAttribute(SESSION_USERNAME, username);
    }
    
    /**
     * Removes the GenomeSpace info from the session that was used in authentication
     * @param req
     */
    private void removeGenomeSpaceInfo(HttpServletRequest req) {
        log.debug("Removing GenomeSpace info from session");
        req.getSession().removeAttribute(SESSION_TOKEN);
        req.getSession().removeAttribute(SESSION_USERNAME);
    }

    /**
     * Based on what the servlet has returned, either redirects the user to an error page or
     * attaches the final GenomeSpace info to the session and redirects them a GenePattern page.
     * If they don't have an associated GenePattern account this will be the associate account page.
     * If they do have an associated account this will be the GenePattern index page.
     */
    private void displayResult(HttpServletRequest req, HttpServletResponse resp, String username, String token, String email, String error) throws ServletException, IOException {
        log.debug("Displaying the results of the OpenID call");
        if (error != null) {
            log.debug("Displaying error");
            req.getSession().setAttribute("gsOIcClientMessage", error);
            resp.sendRedirect("/gp/pages/genomespace/error.jsp");
        }
        else {
            log.debug("Displaying success and attaching GS info to session");
            // Set session variables for GS
            req.getSession().setAttribute(GenomeSpaceLoginManager.GS_USER_KEY, username);
            req.getSession().setAttribute(GenomeSpaceLoginManager.GS_TOKEN_KEY, token);
            req.getSession().setAttribute(GenomeSpaceLoginManager.GS_EMAIL_KEY, email);
            req.getSession().setAttribute(GenomeSpaceLoginManager.GS_OPENID_KEY, true);
            
            // Check if there is an associated GP Account and forward to appropriate place
            log.debug("Checking for associated GP account");
            boolean isAccountAssociated = GenomeSpaceLoginManager.isGSAccountAssociated(username);
            if (isAccountAssociated) {
                log.debug("Account is associated, redirecting to index page");
                String origin = (String) req.getSession().getAttribute("origin");
                if (origin != null) {
                    resp.sendRedirect(origin);
                }
                else {
                    resp.sendRedirect("/gp/pages/index.jsf");
                }
            }
            else {
                log.debug("Redirecting to associate accountpage");
                resp.sendRedirect("/gp/pages/genomespace/associateAccount.jsf");
            }
        }
    }

    /** 
     * Removes the GenomeSpace server cookies to effect an open id logout. 
     */
    private void doOpenIdLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        log.debug("Performing OpenID logout");
        String url = getGsLogoutUrl();
        if (url != null) {
            log.info("Logging out at " + url);
            resp.sendRedirect(url);
        }
        else {
            displayResult(req, resp, null, null, null, "OpenId Provider does not support logout.");
        }
    }
    
    /**
     * Returns the correct request URL with the fqHostname
     * @param request
     * @return
     */
    private String getRequestURL(HttpServletRequest request) {
        log.debug("Contructing the OpenID return URL");
        String servletPath = request.getServletPath();
        String pathInfo = request.getPathInfo();
        
        // Remove preceding slash
        if (servletPath.startsWith("/")) {
            servletPath = servletPath.substring(1);
        }

        String u = ServerConfiguration.instance().getGenePatternURL() + servletPath;
        if (pathInfo != null) {
            u+= pathInfo;
        }
        
        return u;
    }

    /** 
     * Logs into OpenId 
     * */
    @SuppressWarnings("rawtypes")
    private void authRequest(String claimedId, HttpServletRequest httpReq, HttpServletResponse httpResp) throws IOException, ServletException {
        log.debug("Beginning OpenID authentication");
        try {
            // "return_to URL" needs to come back to this servlet in order to verify the OP response.
            log.debug("Obtaining return URL");
            String returnToUrl = getRequestURL(httpReq) + "?is_return=true";

            // Performs openId discovery, puts association into in-memory store, and creates the auth request.
            log.debug("Obtaining list of OpenID discoveries");
            List discoveries = manager.discover(claimedId);
            log.debug("Obtaining discovery info");
            DiscoveryInformation discovered = manager.associate(discoveries);
            log.debug("Attaching discovery info to session");
            httpReq.getSession().setAttribute("openid-disc", discovered);
            log.debug("Performing authentication");
            AuthRequest authReq = manager.authenticate(discovered, returnToUrl);

            if (USE_AX) {
                // Add Attribute Exchange request for gs token and username
                log.debug("AX auth used");
                FetchRequest fetch = FetchRequest.createFetchRequest();
                fetch.addAttribute(TOKEN_ALIAS, EXTENSION_URI + TOKEN_ALIAS, true);
                fetch.addAttribute(USERNAME_ALIAS, EXTENSION_URI + USERNAME_ALIAS, true);
                fetch.addAttribute(EMAIL_ALIAS, EXTENSION_URI + EMAIL_ALIAS, true);
                authReq.addExtension(fetch);
            }

            if (USE_SREG) {
                log.debug("SREG auth used");
                SRegRequest sregReq = SRegRequest.createFetchRequest();
                sregReq.addAttribute("gender", true);
                sregReq.addAttribute("nickname", true);
                sregReq.addAttribute("email", true);
                authReq.addExtension(sregReq);
            }

            log.debug("Catching errors and redirecting, ending authentication");
            // if (discovered.isVersion2()) { throw new ServletException("No support for HTML PUT redirect from OpenId Provider."); }
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
        log.debug("Beginning OpenID response verification");
        try {
            // extract the parameters from the authentication response (which comes in as a HTTP request from the OpenID provider)
            log.debug("Creating parameter response list");
            ParameterList response = new ParameterList(httpReq.getParameterMap());

            // retrieve the previously stored discovery information
            log.debug("Obtaining discovery info from session");
            DiscoveryInformation discovered = (DiscoveryInformation) httpReq.getSession().getAttribute("openid-disc");

            // extract the receiving URL from the HTTP request
            String receivingURL = getRequestURL(httpReq);
            String queryString = httpReq.getQueryString();
            log.debug("Attaching receiving URL with query string: " + queryString);
            if (queryString != null && queryString.length() > 0) receivingURL += "?" + httpReq.getQueryString();

            // Response must match what was used to place the authentication request.
            log.debug("Verifying receivingURL, parameter response and discovery info");
            VerificationResult verification = manager.verify(receivingURL.toString(), response, discovered);

            AuthSuccess authSuccess = null;
            log.debug("VerificationResult is: " + verification);
            if (verification.getVerifiedId() == null) {
                // Client verification can fail despite the server doing a successful login (possibly related to http->https redirect?)
                // This means the server will have set token and username cookies in genomespace.org domain, which means GenomeSpace
                // will allow access. Ideally the client here should redirect to the openId logout page to remove those cookies.
                log.info("OpenId Client side verification failed.");
                removeGenomeSpaceInfo(httpReq);
            }
            else {
                log.info("OpenId Client side verification succeeded.");
                authSuccess = (AuthSuccess) verification.getAuthResponse();
            }
            log.debug("Ending OpenID response verification");
            return extractGenomespaceToken(authSuccess);

        }
        catch (OpenIDException e) {
            // present error to the user
            log.debug("ServletException thrown");
            throw new ServletException(e);
        }
    }

    /** 
     * Extracts the GenomeSpace token from the OpenId message. 
     * */
    private ParameterList extractGenomespaceToken(AuthSuccess authSuccess) throws MessageException {
        log.debug("Extracting GS token");
        if (authSuccess == null) { return null; }
        ParameterList returnList = new ParameterList();
        
        String token = authSuccess.getParameterValue("openid.ext1.value.gs-token");
        String username = authSuccess.getParameterValue("openid.ext1.value.gs-username");
        String email = authSuccess.getParameterValue("openid.ext1.value.email");
        
        log.debug("Setting token in param list");
        returnList.set(new Parameter(TOKEN_ALIAS, token));
        returnList.set(new Parameter(USERNAME_ALIAS, username));
        returnList.set(new Parameter(EMAIL_ALIAS, email));
        
        return returnList;
    }

    /**
     * Get proxy properties from the context init params.
     * @return proxy properties
     */
    private static ProxyProperties getProxyProperties(ServletConfig config) {
        log.debug("Obtaining proxy properties");
        ProxyProperties proxyProps;
        String host = config.getInitParameter("proxy.host");
        log.debug("proxy.host: " + host);
        if (host == null) {
            proxyProps = null;
        }
        else {
            log.debug("Setting proxy properties");
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
