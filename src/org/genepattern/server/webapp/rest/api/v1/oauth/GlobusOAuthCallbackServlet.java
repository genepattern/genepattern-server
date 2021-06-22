package org.genepattern.server.webapp.rest.api.v1.oauth;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.user.UserProp;
import org.genepattern.server.webapp.LoginManager;
import org.genepattern.util.GPConstants;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Servlet implementation class GlobusOAuthCallbackServlet
 */
public class GlobusOAuthCallbackServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GlobusOAuthCallbackServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
		// TODO Auto-generated method stub
	    String accessToken = null;
	    try {
	        final GpConfig gpConfig=ServerConfigurationFactory.instance();
            GpContext context = GpContext.getServerContext();
            String oAuthAuthorizeURL = gpConfig.getGPProperty(context, OAuthConstants.OAUTH_AUTHORIZE_URL_KEY, "https://auth.globus.org/v2/oauth2/authorize");
            String oAuthClientId = gpConfig.getGPProperty(context, OAuthConstants.OAUTH_CLIENT_ID_KEY);
            String oAutScopes = gpConfig.getGPProperty(context, OAuthConstants.OAUTH_AUTHORIZE_SCOPES_KEY, "urn:globus:auth:scope:auth.globus.org:view_identities openid profile email");
            String oAuthClientSecret = gpConfig.getGPProperty(context, OAuthConstants.OAUTH_CLIENT_SECRET_KEY);
            String oAuthTokenURL = gpConfig.getGPProperty(context, OAuthConstants.OAUTH_TOKEN_URL_KEY, "https://auth.globus.org/v2/oauth2/token");
            
            String callbackUrl = gpConfig.getGenePatternURL() + "oauthcallback";
             
            OAuthAuthzResponse oar = OAuthAuthzResponse.oauthCodeAuthzResponse(servletRequest);
            String code = oar.getCode();
            OAuthClientRequest request = OAuthClientRequest
                    .tokenLocation(oAuthTokenURL)
                    .setGrantType(GrantType.AUTHORIZATION_CODE)
                    .setClientId(oAuthClientId)
                    .setClientSecret(oAuthClientSecret)
                    .setRedirectURI(callbackUrl)
                    .setScope(oAutScopes)
                    .setCode(code)
                    .buildQueryMessage();
            
            //create OAuth client that uses custom http client under the hood
            OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
 
            OAuthJSONAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request, OAuthJSONAccessTokenResponse.class);
            accessToken = oAuthResponse.getAccessToken();
            Long expiresIn = oAuthResponse.getExpiresIn();
              
            JsonElement userJson = getUserDetails(accessToken);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonElement tokenJson = getTokenDetails(accessToken, oAuthClientId, oAuthClientSecret);
            
            String email = userJson.getAsJsonObject().get("email").getAsString();
          
            // add these attributes to the session to ...
            // 1) indicate successful login
            servletRequest.getSession().setAttribute(OAuthConstants.OAUTH_USER_ID_ATTR_KEY, email);
            // 2) set the email address to be used when creating a new GenePattern account
            servletRequest.getSession().setAttribute(OAuthConstants.OAUTH_EMAIL_ATTR_KEY, email);
            // 3) set the access token
            servletRequest.getSession().setAttribute(OAuthConstants.OAUTH_TOKEN_ATTR_KEY, accessToken);
            servletRequest.getSession().setAttribute(OAuthConstants.OAUTH_USER_ID_USERPROPS_KEY, userJson);
            // and do these to prevent an earlier session leaking through
            
            String urlTargetPostLogin = (String)servletRequest.getSession().getAttribute("origin");
            if (urlTargetPostLogin == null){
                urlTargetPostLogin  = gpConfig.getGenePatternURL().toString();
            }
             
            // XXX TODO pass in the url that was originally requested and redirect to it
            servletResponse.sendRedirect(urlTargetPostLogin);
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new ServletException(e);
        }
	    
	    
	}

    private JsonElement getUserDetails(String accessToken) throws MalformedURLException, IOException, ProtocolException, UnsupportedEncodingException {
        URL anUrl = new URL("https://auth.globus.org/v2/oauth2/userinfo");     
        HttpURLConnection con = (HttpURLConnection) anUrl.openConnection();
         
        String authHeaderValue = "Bearer " + accessToken;
        con.setRequestProperty("Authorization", authHeaderValue);         
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        
        JsonElement je = getJsonResponse(con);
          
        return je;
    }

    private JsonElement getJsonResponse(HttpURLConnection con) throws UnsupportedEncodingException, IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
        StringBuilder response = new StringBuilder();
        String responseLine = null;
        while ((responseLine = br.readLine()) != null) {
              response.append(responseLine.trim());
        }
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(response.toString());
        return je;
    }

    
    private JsonElement getTokenDetails(String accessToken, String appClientId, String appClientSecret) throws MalformedURLException, IOException, ProtocolException, UnsupportedEncodingException {
        URL anUrl = new URL("https://auth.globus.org/v2/oauth2/token/introspect");     
        HttpURLConnection con = (HttpURLConnection) anUrl.openConnection();
        String auth = appClientId + ":" + appClientSecret;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeaderValue = "Basic " + new String(encodedAuth);
 
        con.setRequestProperty("Authorization", authHeaderValue);         
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setDoOutput(true);
        String urlParameters  = "token="+ accessToken + "&include=session_info,identity_set,identity_set_detail";
        byte[] postData = urlParameters.getBytes( StandardCharsets.UTF_8 );
        con.setRequestProperty("Content-Length", Integer.toString(postData.length ));
        try(DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
            wr.write( postData );
        }    
        JsonElement je = getJsonResponse(con);
          
        return je;
    }
    
    
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
