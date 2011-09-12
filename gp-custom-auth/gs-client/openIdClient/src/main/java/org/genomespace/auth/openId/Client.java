/**
 * OpenId client (Relying Party) implementation that relies on openid4java
 * ConsumerManager for detailed functionality. When used with GenomeSpace
 * OpenId Provider it will return the GenomeSpace login token.
 * 
 * Code is from openid4java trunk r640
 * http://code.google.com/p/openid4java/source/browse/trunk/samples/consumer-servlet/src/main/java/org/openid4java/samples/consumerservlet/ConsumerServlet.java?spec=svn640&r=640
 * Original author Sutra Zhou
 * Edited 2011 epolk
 */
package org.genomespace.auth.openId;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openid4java.OpenIDException;
import org.openid4java.association.AssociationSessionType;
import org.openid4java.consumer.ConsumerException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.InMemoryConsumerAssociationStore;
import org.openid4java.consumer.InMemoryNonceVerifier;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.Message;
import org.openid4java.message.MessageException;
import org.openid4java.message.MessageExtension;
import org.openid4java.message.ParameterList;
import org.openid4java.util.HttpClientFactory;
import org.openid4java.util.ProxyProperties;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Client extends javax.servlet.http.HttpServlet {

	private static final long serialVersionUID = -1L;
	private static final Logger logger = Logger.getLogger("org.genomespace.auth.openId");
	private ConsumerManager manager;
	// These must not be changed, to interoperate with GenomeSpace
	private static final String GS_TOKEN_COOKIE = "gs-token";
	private static final String GS_USERNAME_COOKIE = "gs-username";

	// Point this to the GenomeSpace Identity server
	private static final String GENOMESPACE_SERVER_NAME_AND_PORT = "identitytest.genomespace.org:8080";
	
	// Returns the URL to Yadis doc on the GenomeSpace server.
	private String getGsClaimedId(HttpServletRequest req) {
		// If there is an env variable, use it (for test purposes)
		String serverAndPort = System.getenv("GENOMESPACE_SERVER_NAME_AND_PORT");
		if (serverAndPort == null) {
			serverAndPort = GENOMESPACE_SERVER_NAME_AND_PORT;
		}
		return "http://" + serverAndPort + "/identityServer/xrd.jsp";
	}


	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		// --- Forward proxy setup (only if needed) ---
		ProxyProperties proxyProps = getProxyProperties(config);
		if (proxyProps != null) {
			logger.fine("ProxyProperties: " + proxyProps);
			HttpClientFactory.setProxyProperties(proxyProps);
		}

		try {
			Message.addExtensionFactory(GenomeSpaceMessageExtensionFactory.class);
		} catch (MessageException e) {
			throw new ServletException("Cannot register GenomeSpaceMessageExtensionFactory", e);
		}

		manager = new ConsumerManager();
		manager.setAssociations(new InMemoryConsumerAssociationStore());
		manager.setNonceVerifier(new InMemoryNonceVerifier(5000));
		manager.setMinAssocSessEnc(AssociationSessionType.DH_SHA256);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		// Uses GenomeSpace token and username from cookies, if they exist.
		String token = getCookieValue(req, GS_TOKEN_COOKIE);
		String username = getCookieValue(req, GS_USERNAME_COOKIE);

		if ("true".equals(req.getParameter("is_cancel"))) {
			// User clicked "cancel" on the openId login page, so do nothing.
			displayResult(req, resp, username, token, null);
			
		} else if ("true".equals(req.getParameter("is_return"))) {
			// Handles the callback from OpenId Provider.  OpenId protocol
			// requires the response be verified.  Token and username are
			// gotten from the OpenId MessageExtension.
			ParameterList paramList = verifyResponse(req);
			if (paramList != null) {
				token = paramList.getParameterValue(GenomeSpaceMessageExtension.GS_TOKEN_ALIAS);
				username = paramList.getParameterValue(GenomeSpaceMessageExtension.GS_USERNAME_ALIAS);
				// Sets our GenomeSpace token and username cookies
				setCookie(req, resp, GS_TOKEN_COOKIE, token);
				setCookie(req, resp, GS_USERNAME_COOKIE, username);
				displayResult(req, resp, username, token, null);

			} else {
				// Removes our GenomeSpace cookies
				removeCookie(req, resp, GS_TOKEN_COOKIE);
				removeCookie(req, resp, GS_USERNAME_COOKIE);
				token = null;
				displayResult(req, resp, null, null, "Error while verifying the OP response.  Likely a configuration or programming problem.");
			}

		} else if ("true".equals(req.getParameter("logout"))) {
			// Removes our GenomeSpace cookies
			removeCookie(req, resp, GS_TOKEN_COOKIE);
			removeCookie(req, resp, GS_USERNAME_COOKIE);
			token = null;
			displayResult(req, resp, username, token, null);

		} else if (token == null || token.length() == 0) {
			// If GenomeSpace token cookie doesn't exist, do openId login.
			authRequest(getGsClaimedId(req), req, resp);

		} else {
			// Already logged in (token exists, not a logout nor an OP return) 
			displayResult(req, resp, username, token, null);
		}
	}

	private String getCookieValue(HttpServletRequest req, String cookieName) {
		for (Cookie cookie : req.getCookies()) {
			if (cookieName.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	private void removeCookie(HttpServletRequest req,
			HttpServletResponse response, String cookieName) {
		Cookie cookie = new Cookie(cookieName, "");
		cookie.setPath("/");
		cookie.setMaxAge(0);
		String domainPattern = getCookieDomainPattern(req.getServerName());
		if (domainPattern != null)
			cookie.setDomain(domainPattern);
		response.addCookie(cookie);
	}

	private void setCookie(HttpServletRequest req,
			HttpServletResponse response, String cookieName, String value) {
		Cookie cookie = new Cookie(cookieName, value);
		cookie.setPath("/");
		cookie.setMaxAge(60*60*24*7); // Expires in 1 week
		String domainPattern = getCookieDomainPattern(req.getServerName());
		if (domainPattern != null)
			cookie.setDomain(domainPattern);
		response.addCookie(cookie);
	}

	private String getCookieDomainPattern(String serverName) {
		int firstDotIdx = serverName.indexOf(".");
		if (firstDotIdx > 0 && (firstDotIdx != serverName.lastIndexOf("."))) {
			return serverName.substring(serverName.indexOf("."));
		} else {
			return null;
		}
	}
	
	/**
	 *  The result of the login is displayed in a web page.
	 *  
	 *  This is where a real app would forward or otherwise connect.
	 */
	private void displayResult(HttpServletRequest req, HttpServletResponse resp,
			String username, String token, String error)
			throws ServletException, IOException {
		
		resp.setContentType("text/html");
		PrintWriter out = resp.getWriter();
		out.println("<html><head/><body><h3>GenomeSpace OpenId Authentication</h3><p/>");
		if (error != null) {
			out.println(error);
		}
		if (token == null) {
			out.println("User is not authenticated.");
		} else {
			out.println("User " + username + " is authenticated.");
			out.println("<br/>GenomeSpace token is " + token);
			// Put a link to do openId logout
			out.println("<p/><a href='" +
					req.getRequestURL().toString() + "?logout=true'>Logout</a>");
		}
		out.println("</body></html>");
	}

	// --- placing the authentication request ---
	@SuppressWarnings("rawtypes")
	private void authRequest(String claimedId, HttpServletRequest httpReq,
			HttpServletResponse httpResp) throws IOException, ServletException {
		try {
			// "return_to URL" needs to come back to this servlet
			// in order to verify the OP response.
			String returnToUrl = httpReq.getRequestURL().toString()
			+ "?is_return=true";

			// Performs openId discovery, puts association into in-memory
			// store, and creates the auth request.
			List discoveries = manager.discover(claimedId);
			DiscoveryInformation discovered = manager.associate(discoveries);
			httpReq.getSession().setAttribute("openid-disc", discovered);
			AuthRequest authReq = manager.authenticate(discovered, returnToUrl);

			if (discovered.isVersion2()) {
				throw new ServletException("No support for HTML PUT redirect.");
			}
			httpResp.sendRedirect(authReq.getDestinationUrl(true));

		} catch (org.openid4java.discovery.yadis.YadisException e) {
			logger.log(Level.INFO, "Error requesting OpenId authentication.", e);
			displayResult(httpReq, httpResp, null, null, "OpenId server " +
					GENOMESPACE_SERVER_NAME_AND_PORT + " is unavailable.<BR/>The internal error is <CODE>" +
					e.getMessage() + "</CODE><P/>");
		} catch (OpenIDException e) {
			logger.log(Level.INFO, "Error requesting OpenId authentication.", e);
			displayResult(httpReq, httpResp, null, null, 
					"Error requesting OpenId authentication.<BR/>The internal error is <CODE>" +
					e.getMessage() + "</CODE><P/>");
		}
	}


	/**
	 * If OP's response verifies OK, returns the ParameterList containing the
	 * GenomeSpace token and username.  If not OK, returns null.
	 */
	private ParameterList verifyResponse(HttpServletRequest httpReq)
			throws ServletException {
		try {
			// extract the parameters from the authentication response
			// (which comes in as a HTTP request from the OpenID provider)
			ParameterList response = new ParameterList(httpReq.getParameterMap());

			// retrieve the previously stored discovery information
			DiscoveryInformation discovered = (DiscoveryInformation) httpReq
					.getSession().getAttribute("openid-disc");

			// extract the receiving URL from the HTTP request
			StringBuffer receivingURL = httpReq.getRequestURL();
			String queryString = httpReq.getQueryString();
			if (queryString != null && queryString.length() > 0)
				receivingURL.append("?").append(httpReq.getQueryString());

			// Response must match what was used to place the authentication request.
			VerificationResult verification = manager.verify(
					receivingURL.toString(), response, discovered);

			if (verification.getVerifiedId() == null) {
				logger.info("OpenId Client side verification failed");
				return null;
			}
			AuthSuccess authSuccess = (AuthSuccess) verification.getAuthResponse();

			// Extracts the GenomeSpace token from the OpenId message
			if (!authSuccess.hasExtension(GenomeSpaceMessageExtension.GS_TOKEN_URI)) { 
				logger.severe("No GenomeSpace token found in successful OpenId login.  Did you use GenomeSpace OpenId Provider?");
				return null;
			}
			MessageExtension msgExt = authSuccess.getExtension(GenomeSpaceMessageExtension.GS_TOKEN_URI);
			return msgExt.getParameters();

		} catch (OpenIDException e) {
			// present error to the user
			throw new ServletException(e);
		}
	}

	/**
	 * Get proxy properties from the context init params.
	 * 
	 * @return proxy properties
	 */
	private static ProxyProperties getProxyProperties(ServletConfig config) {
		ProxyProperties proxyProps;
		String host = config.getInitParameter("proxy.host");
		logger.fine("proxy.host: " + host);
		if (host == null) {
			proxyProps = null;
		} else {
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
