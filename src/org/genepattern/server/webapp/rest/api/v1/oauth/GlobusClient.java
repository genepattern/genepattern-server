package org.genepattern.server.webapp.rest.api.v1.oauth;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;

import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.genepattern.util.GPConstants;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.common.io.BaseEncoding;

public class GlobusClient {
    public static String transferAPIBaseUrl = "https://transfer.api.globusonline.org/v0.10" ;
    
    public void login(HttpServletRequest servletRequest) throws OAuthProblemException, OAuthSystemException, MalformedURLException, ProtocolException, UnsupportedEncodingException, IOException{

        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        GpContext context = GpContext.getServerContext();
        String oAuthClientId = gpConfig.getGPProperty(context, OAuthConstants.OAUTH_CLIENT_ID_KEY);
        String oAutScopes = gpConfig.getGPProperty(context, OAuthConstants.OAUTH_AUTHORIZE_SCOPES_KEY, "urn:globus:auth:scope:transfer.api.globus.org:all urn:globus:auth:scope:auth.globus.org:view_identities openid profile email offline_access");
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
        String accessToken = oAuthResponse.getAccessToken();
        
        // need a refresh token so that we can get the transfer token when we actually want to use it since they last 2 hours
        String refreshToken = oAuthResponse.getRefreshToken();// refresh token is valid for 6 months without use
        String[] transferTokens = getTransferToken(oAuthResponse);  // transfer token and refresh token for it
        JsonElement userJson = getUserDetails(accessToken);
        String email = userJson.getAsJsonObject().get("email").getAsString();
      
        // add these attributes to the session to ...
        // 1) indicate successful login
        servletRequest.getSession().setAttribute(OAuthConstants.OAUTH_USER_ID_ATTR_KEY, email);
        // 2) set the email address to be used when creating a new GenePattern account
        servletRequest.getSession().setAttribute(OAuthConstants.OAUTH_EMAIL_ATTR_KEY, email);
        // 3) set the access token
        servletRequest.getSession().setAttribute(OAuthConstants.OAUTH_TOKEN_ATTR_KEY, accessToken);
        servletRequest.getSession().setAttribute(OAuthConstants.OAUTH_TRANSFER_TOKEN_ATTR_KEY, transferTokens[0]);
        servletRequest.getSession().setAttribute(OAuthConstants.OAUTH_TRANSFER_REFRESH_TOKEN_ATTR_KEY, transferTokens[1]);
        servletRequest.getSession().setAttribute(OAuthConstants.OAUTH_USER_ID_USERPROPS_KEY, userJson);
        servletRequest.getSession().setAttribute(OAuthConstants.OAUTH_REFRESH_TOKEN_ATTR_KEY, refreshToken);
      
       
        // if the user is already logged in as another account, we want to hook things up now
        try {
            GlobusAuthentication.linkGlobusAccountToGenePattern(servletRequest, email,  accessToken,  transferTokens[0], refreshToken, transferTokens[1], null) ;
        }
        catch (AuthenticationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (WebApplicationException wex){
            // no userId present, ignore it and let the authenticator decide if a login is valid and try again
            //wex.printStackTrace();
        }
        
        
        // THESE CALLS ARE JUST FOR TEST/DEBUG
        JsonElement tokenDetails = getTokenDetails(accessToken);
        //System.out.println("@login TokenDetails: "+ accessToken+"\n\t"+ tokenDetails);
        
       
        
    }
    
    /**
     * Refresh an access token token with globus using its refresh token. Use the keys into the session
     * object to get the token and refresh token.
     * 
     * @param servletRequest
     * @param refreshTokenKey
     * @param accessTokenKey
     * @throws IOException
     */
    public void refreshToken(HttpServletRequest servletRequest, String refreshTokenKey, String accessTokenKey) throws IOException{
        String userId = (String)servletRequest.getSession().getAttribute(GPConstants.USERID);
        if (userId == null) {
            userId = servletRequest.getParameter("user_id");
        }
        String refreshToken = (String) servletRequest.getSession().getAttribute(refreshTokenKey);
        String accessToken = (String) servletRequest.getSession().getAttribute(accessTokenKey);
        if ((refreshToken == null) || (accessToken == null)){
            refreshToken = getTokenFromUserPrefs(userId, refreshTokenKey);
            accessToken = getTokenFromUserPrefs(userId, accessTokenKey);
        }
        
        //System.out.println("Refresh Token: " + refreshTokenKey);
        //System.out.println("\told refresh: " + refreshToken);
        //System.out.println("\told access: " + accessToken);
        
        String authHeaderValue = getAppAuthHeader();
        
        URL anUrl = new URL("https://auth.globus.org/v2/oauth2/token");     
        HttpURLConnection con = (HttpURLConnection) anUrl.openConnection();
        con.setDoOutput(true);
        
        // set properties
        con.setRequestProperty("Authorization", authHeaderValue); 
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setRequestMethod("POST");
        con.setRequestProperty("Accept", "application/json");
        
        // set request parameters for a refresh_token call
        String urlParameters  = "refresh_token="+ refreshToken + "&grant_type=refresh_token";
        byte[] postData = urlParameters.getBytes( StandardCharsets.UTF_8 );
        con.setRequestProperty("Content-Length", Integer.toString(postData.length ));
        try(DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
            wr.write( postData );
        }    
        // get the json response
        JsonElement je = getJsonResponse(con);
        //System.out.println(je);
        
        String newAccess = je.getAsJsonObject().get("access_token").getAsString();
        String newRefresh = je.getAsJsonObject().get("refresh_token").getAsString();
       
        //System.out.println("\tnew refresh: " + newRefresh);
        //System.out.println("\tnew access: " + newAccess);
    
        
        servletRequest.getSession().setAttribute(refreshTokenKey, newRefresh);
        servletRequest.getSession().setAttribute(accessTokenKey, newAccess);
        
        // update the one saved in the user settings
        
         setTokenIntoUserPrefs(userId,  refreshTokenKey, newRefresh);
        setTokenIntoUserPrefs(userId,  accessTokenKey, newAccess);
        
    }
    
    public String getTokenFromUserPrefs(String userId, String key){
        HibernateSessionManager hmgr = HibernateUtil.instance();
        UserDAO dao = new UserDAO(hmgr);
        
        return dao.getProperty(userId, key).getValue();
    }
    
    public void setTokenIntoUserPrefs(String userId, String key, String value){
        HibernateSessionManager hmgr = HibernateUtil.instance();
        UserDAO dao = new UserDAO(hmgr);
        boolean inTransaction = hmgr.isInTransaction();
        if (!inTransaction) hmgr.beginTransaction();
       
        dao.setProperty(userId,  key, value);
       
    }
    
  
    public JsonElement getJsonError(HttpURLConnection con) throws UnsupportedEncodingException, IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getErrorStream(), "utf-8"));
        StringBuilder response = new StringBuilder();
        String responseLine = null;
        while ((responseLine = br.readLine()) != null) {
              response.append(responseLine.trim());
        }
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(response.toString());
        return je;
    }
    
    public JsonElement getJsonResponse(HttpURLConnection con) throws UnsupportedEncodingException, IOException {
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

    public  String[] getTransferToken(OAuthJSONAccessTokenResponse oAuthResponse) {
        String transferToken = null;
        String refreshToken = null;
        
        String allTokensJson = oAuthResponse.getBody();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(allTokensJson);
        JsonArray otherTokens = je.getAsJsonObject().get("other_tokens").getAsJsonArray();
        for (int i=0; i< otherTokens.size();i++){
            JsonObject aToken = otherTokens.get(i).getAsJsonObject();
            if ("transfer.api.globus.org".equalsIgnoreCase(aToken.get("resource_server").getAsString())){
                transferToken = aToken.get("access_token").getAsString();
                refreshToken = aToken.get("refresh_token").getAsString();
                
            }
        }
        String[] ret = {transferToken, refreshToken}; 
        return ret;
    }

   
    
    public JsonElement getTokenDetails(String accessToken) throws MalformedURLException, IOException, ProtocolException, UnsupportedEncodingException {
        String authHeaderValue = getAppAuthHeader();
        
        URL anUrl = new URL("https://auth.globus.org/v2/oauth2/token/introspect");     
        HttpURLConnection con = (HttpURLConnection) anUrl.openConnection();
       
        con.setRequestProperty("Authorization", authHeaderValue);   // has application ID:secret      
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setRequestProperty("Accept", "application/json");
        
        con.setDoOutput(true);
        String urlParameters  = "token="+ accessToken + "&include=session_info";
        byte[] postData = urlParameters.getBytes( StandardCharsets.UTF_8 );
        con.setRequestProperty("Content-Length", Integer.toString(postData.length ));
        try(DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
            wr.write( postData );
        }    
        JsonElement je = getJsonResponse(con);
          
        return je;
    }

    private String getAppAuthHeader() {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        GpContext context = GpContext.getServerContext();
        String oAuthClientId = gpConfig.getGPProperty(context, OAuthConstants.OAUTH_CLIENT_ID_KEY);
        String oAuthClientSecret = gpConfig.getGPProperty(context, OAuthConstants.OAUTH_CLIENT_SECRET_KEY);
        
        
        String auth = oAuthClientId + ":" + oAuthClientSecret;
        // byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String encodedAuth = BaseEncoding.base64().encode(auth.getBytes());
        String authHeaderValue = "Basic " + encodedAuth;
        return authHeaderValue;
    }
    
    
    public JsonElement getUserDetails(String accessToken) throws MalformedURLException, IOException, ProtocolException, UnsupportedEncodingException {
        URL anUrl = new URL("https://auth.globus.org/v2/oauth2/userinfo");     
        HttpURLConnection con = (HttpURLConnection) anUrl.openConnection();
         
        String authHeaderValue = "Bearer " + accessToken;
        con.setRequestProperty("Authorization", authHeaderValue);         
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        
        JsonElement je = getJsonResponse(con);
          
        return je;
    }
    
    
    
    public String getSubmissionId(String transferToken) throws MalformedURLException, IOException, ProtocolException {
        URL url = new URL(transferAPIBaseUrl+"/submission_id");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestProperty("Authorization","Bearer "+ transferToken);
        connection.setRequestMethod("GET");
        //connection.setRequestProperty("Accept", "application/json");
        
        JsonElement submissionResponse =  getJsonResponse(connection);
        String dataType = submissionResponse.getAsJsonObject().get("DATA_TYPE").getAsString();
        
        if (!"submission_id".equals(dataType)){
            throw new IOException("Error getting submission ID.  Got a '"+ dataType + "' but expected a 'submissionId' "+ submissionResponse.getAsShort());
        }
        
        return submissionResponse.getAsJsonObject().get("value").getAsString();
    }

    public String startGlobusFileTransfer(HttpServletRequest request, String sourceEndpointId, String path, String file, String destDir) throws MalformedURLException, IOException, ProtocolException, InterruptedException {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
    
        
        String userId = (String)request.getSession().getAttribute(GPConstants.USERID);
        if (userId == null) {
            userId = request.getParameter("gp_user_id");
        }
        
        final GpContext context =  GpContext.getContextForUser(userId);
             
        refreshToken(request, OAuthConstants.OAUTH_TRANSFER_REFRESH_TOKEN_ATTR_KEY, OAuthConstants.OAUTH_TRANSFER_TOKEN_ATTR_KEY);
        
        // Get the token for making the transfer call
        String transferToken = (String)request.getSession().getAttribute(OAuthConstants.OAUTH_TRANSFER_TOKEN_ATTR_KEY);
        String user_id = (String)request.getSession().getAttribute(OAuthConstants.OAUTH_USER_ID_ATTR_KEY);
           
        // add an ACL to allow this user to write to a drop dir for the transfer
        setupOpenACLForTransfer(request);
        
        
        // get the file details from which we can retrieve the size
        JsonObject fileDetails = getGlobusFileDetails(request, sourceEndpointId, path, file);
        long fileSize = fileDetails.get("size").getAsLong();
        
        //  TBD need to call to endpoint to set ACL for this user on our shared endpoint
        // - use GP client token to set ACL via transfer service call, need Globus user ID (not just email)
        //   which we can get via inspection call using the access_token
        
        // so to transfer in we need to know our endpoint ID
        String myEndpointID = gpConfig.getGPProperty(context, OAuthConstants.OAUTH_LOCAL_ENDPOINT_ID, "eb7230ac-d467-11eb-9b44-47c0f9282fb8");
        String submissionId = getSubmissionId(transferToken);

        JsonObject transferObject = new JsonObject();
        transferObject.addProperty("DATA_TYPE", "transfer");
        transferObject.addProperty("submission_id", submissionId);
        transferObject.addProperty("source_endpoint", sourceEndpointId);
        transferObject.addProperty("destination_endpoint", myEndpointID);
        transferObject.addProperty("verify_checksum", true);
        
        JsonObject transferItem = new JsonObject();
        transferItem.addProperty("DATA_TYPE", "transfer_item");
        transferItem.addProperty("recursive", false);
        transferItem.addProperty("source_path", path + file);
        transferItem.addProperty("destination_path", "/~/GenePatternLocal/"+ userId +"/globus/"+file);
       
        JsonArray transferItems = new JsonArray();
        transferItems.add(transferItem);
        
        transferObject.add("DATA", transferItems);
        
        //System.out.println("     "+transferObject);
        URL url = new URL(transferAPIBaseUrl+"/transfer");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();        
        connection.setRequestMethod("POST");
        
        // only need users token here as long as the user permissions were set earlier (using the GenePattern client token)
        connection.setRequestProperty("Authorization","Bearer "+ transferToken);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        
        connection.setDoOutput(true);
        
        try(OutputStream os = connection.getOutputStream()) {
            byte[] input = transferObject.toString().getBytes("utf-8");
            os.write(input, 0, input.length);           
        }
        
        String taskId = null;
        JsonElement transferResponse = null;
        
        transferResponse =  getJsonResponse(connection);
        taskId = transferResponse.getAsJsonObject().get("task_id").getAsString();
        
        // spawn a new thread to wait for completion
       
        GlobusTransferMonitor.getInstance().addWaitingUser(userId, taskId, this, file, context, destDir, fileSize);
        return taskId;
    }
    
    public JsonObject getGlobusFileDetails(HttpServletRequest request, String sourceEndpointId, String path, String file) throws MalformedURLException, IOException, ProtocolException, InterruptedException {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
    
        //refreshToken(request, OAuthConstants.OAUTH_REFRESH_TOKEN_ATTR_KEY, OAuthConstants.OAUTH_TOKEN_ATTR_KEY);
        refreshToken(request, OAuthConstants.OAUTH_TRANSFER_REFRESH_TOKEN_ATTR_KEY, OAuthConstants.OAUTH_TRANSFER_TOKEN_ATTR_KEY);
        
        // Get the token for making the transfer call
        String transferToken = (String)request.getSession().getAttribute(OAuthConstants.OAUTH_TRANSFER_TOKEN_ATTR_KEY);
        String user_id = (String)request.getSession().getAttribute(OAuthConstants.OAUTH_USER_ID_ATTR_KEY);
           
        URL url = new URL(transferAPIBaseUrl+"/operation/endpoint/"+sourceEndpointId+"/ls?path="+path+"&filter=name:="+file);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();        
        connection.setRequestMethod("GET");
        
        // only need users token here as long as the user permissions were set earlier (using the GenePattern client token)
        connection.setRequestProperty("Authorization","Bearer "+ transferToken);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        
        connection.setDoOutput(true);
        
        
        JsonElement jsonResponse = null;
        
        jsonResponse =  getJsonResponse(connection);
        
        return jsonResponse.getAsJsonObject().get("DATA").getAsJsonArray().get(0).getAsJsonObject();
        
        
       
    }
    
    public JsonObject cancelTransfer(String user, JsonObject statusObject) throws IOException{
        String taskId = statusObject.get("task_id").getAsString();
        
        String token = getTokenFromUserPrefs(user, OAuthConstants.OAUTH_TRANSFER_TOKEN_ATTR_KEY);
        URL url = new URL(transferAPIBaseUrl+"/task/"+taskId+"/cancel");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization","Bearer "+ token);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Accept", "application/json");
        connection.setReadTimeout(20000);
        JsonElement jsonResponse = null;
        
        jsonResponse =  getJsonResponse(connection);
        System.out.println(jsonResponse.toString());
        return jsonResponse.getAsJsonObject();
    }
    
    public String checkTransferStatus(JsonObject statusObject){
        String transferStatus = statusObject.get("status").getAsString();
        return transferStatus;
    }
    
    public String checkTransferStatus(String taskId, String transferToken) throws IOException{
       return checkTransferStatus( getTransferDetails( taskId,  transferToken));   
    }
    
   public JsonObject getTransferDetails(String taskId, String transferToken) throws IOException{
       
       // ?: change to different call using the GP client token instead of the user's transfer token
       // to monitor the transfer progress.  This is because transfer tokens are only good for 2 hours
       // or else refresh the token
      
       String status = "ACTIVE";
       URL url = new URL(transferAPIBaseUrl+"/task/"+taskId);
       HttpURLConnection connection = (HttpURLConnection) url.openConnection();
       connection.setRequestProperty("Authorization","Bearer "+ transferToken);
       connection.setRequestMethod("GET");
       connection.setRequestProperty("Accept", "application/json");
       connection.setReadTimeout(20000);
       
       JsonElement submissionResponse =  getJsonResponse(connection);
       return  submissionResponse.getAsJsonObject();       
   }
   
   
   /**
    * Get credentials to make globus calls as the GenePattern application (and not as an end user)
    * Used for ACL setup/teardown 
    * 
    * @return
    * @throws IOException
    */
    public String getApplicationCredentials() throws IOException{
       
        String authHeaderValue = getAppAuthHeader();
        
        URL anUrl = new URL("https://auth.globus.org/v2/oauth2/token");     
        HttpURLConnection con = (HttpURLConnection) anUrl.openConnection();
        con.setDoOutput(true);
        
        // set properties
        con.setRequestProperty("Authorization", authHeaderValue); 
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setRequestMethod("POST");
        con.setRequestProperty("Accept", "application/json");
        
        // set request parameters for a refresh_token call
        String urlParameters  = "scope=urn:globus:auth:scope:transfer.api.globus.org:all&grant_type=client_credentials";
        byte[] postData = urlParameters.getBytes( StandardCharsets.UTF_8 );
        con.setRequestProperty("Content-Length", Integer.toString(postData.length ));
        try(DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
            wr.write( postData );
        }    
        // get the json response
        JsonElement je = getJsonResponse(con);
        String access_token = je.getAsJsonObject().get("access_token").getAsString();
        return access_token;
    }
   
   
    /**
     * Add the user to the writable ACL for the inbound transfer.  After the transfer is complete
     * we will remove this so that the endpoint is not left open all the time
     * @throws IOException 
     */
    public void setupOpenACLForTransfer(HttpServletRequest request) throws IOException{
        String userAccessToken = (String)request.getSession().getAttribute(OAuthConstants.OAUTH_TOKEN_ATTR_KEY);
        String userId = (String)request.getSession().getAttribute(GPConstants.USERID);
        
        
        
        GpContext context = GpContext.getServerContext();
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        String myEndpointID = gpConfig.getGPProperty(context, OAuthConstants.OAUTH_LOCAL_ENDPOINT_ID);
        String token = getTokenFromUserPrefs(userId, OAuthConstants.OAUTH_TRANSFER_TOKEN_ATTR_KEY);
        
        JsonElement tokenDetails = null;
        String userUUID = null;
        try {
            tokenDetails = getTokenDetails(userAccessToken);
            userUUID = tokenDetails.getAsJsonObject().get("sub").getAsString();
        } catch (Exception e){
            
            try {
                tokenDetails = getTokenDetails(token);
                userUUID = tokenDetails.getAsJsonObject().get("sub").getAsString();
            } catch (Exception e2){
                e.printStackTrace();
                e2.printStackTrace();
            }
        }
        
         
        String appAccessToken = getApplicationCredentials() ;
        
        URL anUrl = new URL("https://transfer.api.globusonline.org/v0.10/endpoint/"+myEndpointID+"/access");  
        
        // System.out.println("ACL CALL " + anUrl.toString());
        HttpURLConnection con = (HttpURLConnection) anUrl.openConnection();
         
        String authHeaderValue = "Bearer " + appAccessToken; 
        con.setRequestProperty("Authorization", authHeaderValue);         
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        
        JsonObject aclObject = new JsonObject();
        aclObject.addProperty("DATA_TYPE", "access");
        aclObject.addProperty("principal_type", "identity");
        aclObject.addProperty("principal",userUUID);
        // aclObject.addProperty("path", "/~/GenePatternLocal/"+ userId +"/globus/");
        aclObject.addProperty("path", "/");
        aclObject.addProperty("permissions", "rw");
        //aclObject.addProperty("notify_email", "jliefeld@cloud.ucsd.edu");
   
        try {
            OutputStream os = con.getOutputStream();
            byte[] input = aclObject.toString().getBytes("utf-8");
            os.write(input, 0, input.length);           
            JsonElement je = getJsonResponse(con);
            //System.out.println(je);
           
            
        } catch (Exception e){
            boolean realError = false;
            if (con.getResponseCode() == 409 ){
                // might just be because there is already an active ACL for this user, lets check
                JsonElement jerr = getJsonError(con);
                String code = jerr.getAsJsonObject().get("code").getAsString();
                if ("Exists".equalsIgnoreCase(code)){
                    //String ruleId = getUserACLId( userAccessToken);
                    //System.out.println("GLOBUS access rule already existed "+ ruleId);
                } else {
                    realError = true;
                }
                
            }
            
            if (realError){
                e.printStackTrace();
            }
        }
    }
    /**
     * Add the user to the writable ACL for the inbound transfer.  After the transfer is complete
     * we will remove this so that the endpoint is not left open all the time
     */
    public boolean teardownOpenACLForTransfer(String userAccessToken, String aclId) {
        
        
        try {
            GpContext context = GpContext.getServerContext();
            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            String myEndpointID = gpConfig.getGPProperty(context, OAuthConstants.OAUTH_LOCAL_ENDPOINT_ID);
            
            JsonElement tokenDetails = getTokenDetails(userAccessToken);
            String userUUID = tokenDetails.getAsJsonObject().get("sub").getAsString();
            String appAccessToken = getApplicationCredentials() ;
            
           
            URL XanUrl = new URL("https://transfer.api.globusonline.org/v0.10/endpoint/"+myEndpointID+"/access/"+aclId);  
            HttpURLConnection Xcon = (HttpURLConnection) XanUrl.openConnection();
            String XauthHeaderValue = "Bearer " + appAccessToken; 
            Xcon.setRequestProperty("Authorization", XauthHeaderValue);         
            Xcon.setRequestMethod("DELETE");
            Xcon.setRequestProperty("Accept", "application/json");
            
            JsonElement je = getJsonResponse(Xcon);
            //System.out.println(je);
                
            String code = je.getAsJsonObject().get("code").getAsString();
            return "Deleted".equalsIgnoreCase(code);
        } catch (IOException ioe){
            ioe.printStackTrace();
            return false;
        }
            
        
    }
    
  public String getUserACLId(String userAccessToken) {
        try {
            GpContext context = GpContext.getServerContext();
            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            String myEndpointID = gpConfig.getGPProperty(context, OAuthConstants.OAUTH_LOCAL_ENDPOINT_ID);
            
            JsonElement tokenDetails = getTokenDetails(userAccessToken);
            String userUUID = tokenDetails.getAsJsonObject().get("sub").getAsString();
            String appAccessToken = getApplicationCredentials() ;
           
            URL XanUrl = new URL("https://transfer.api.globusonline.org/v0.10/endpoint/"+myEndpointID+"/access_list");  
            HttpURLConnection Xcon = (HttpURLConnection) XanUrl.openConnection();
            String XauthHeaderValue = "Bearer " + appAccessToken; 
            Xcon.setRequestProperty("Authorization", XauthHeaderValue);         
            Xcon.setRequestMethod("GET");
            Xcon.setRequestProperty("Accept", "application/json");
            
            JsonElement je = getJsonResponse(Xcon);
            //System.out.println(je);
            JsonArray aclEntries = je.getAsJsonObject().get("DATA").getAsJsonArray();
            for (int i=0; i< aclEntries.size(); i++){
                JsonObject anAccess = aclEntries.get(i).getAsJsonObject();
                String accessPrincipal = anAccess.get("principal").getAsString();
                if (userUUID.equals(accessPrincipal)){
                    // its the right user, lets make sure its not part of a role.
                    // we don't use roles for the GP stuff so its probably an admin
                    // or other role we set up manually
                    JsonElement role = anAccess.get("role");
                    if (role == null){
                        return anAccess.get("id").getAsString();
                    }
                    
                }
                
            }
            return null;
            
        } catch (IOException ioe){
            ioe.printStackTrace();
            return null;
        }
            
        
    }
    
    
    
}
