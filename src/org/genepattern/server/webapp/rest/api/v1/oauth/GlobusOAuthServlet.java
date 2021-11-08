package org.genepattern.server.webapp.rest.api.v1.oauth;

import java.io.IOException;
import java.net.URLEncoder;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;

/**
 * Servlet implementation class GlobusOAuthServlet
 */
public class GlobusOAuthServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
	   
	
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GlobusOAuthServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse servletResponse) throws ServletException, IOException {
		
	    try {
	        final GpConfig gpConfig=ServerConfigurationFactory.instance();
	        GpContext context = GpContext.getServerContext();
	        String oAuthAuthorizeURL = gpConfig.getGPProperty(context, OAuthConstants.OAUTH_AUTHORIZE_URL_KEY, "https://auth.globus.org/v2/oauth2/authorize");
	        String oAuthClientId = gpConfig.getGPProperty(context, OAuthConstants.OAUTH_CLIENT_ID_KEY, "f4951e9d-03a2-4ffd-a65f-61a7f9f73bde");
	        String oAutScopes = gpConfig.getGPProperty(context, OAuthConstants.OAUTH_AUTHORIZE_SCOPES_KEY, "urn:globus:auth:scope:auth.globus.org:view_identities openid profile email");

	        // Handle the optional forward query parameter
			String forward = request.getParameter("forward");
			forward = forward != null ? "?forward=" + URLEncoder.encode(forward, "UTF-8"): "";

	        String callbackUrl = gpConfig.getGenePatternURL() + "oauthcallback" + forward;
	        
    	    OAuthClientRequest authRequest = OAuthClientRequest
                    .authorizationLocation(oAuthAuthorizeURL)
                    .setClientId(oAuthClientId)
                    .setResponseType("code")
                    .setScope(oAutScopes)
                    .setRedirectURI(callbackUrl)
                    .buildQueryMessage();
    	    
    	    servletResponse.sendRedirect(authRequest.getLocationUri());
	    }catch (Exception e){
	        e.printStackTrace();
	        throw new ServletException(e);
	    }
	    
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
