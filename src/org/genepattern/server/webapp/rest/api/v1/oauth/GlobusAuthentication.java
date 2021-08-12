package org.genepattern.server.webapp.rest.api.v1.oauth;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Random;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.auth.DefaultGenePatternAuthentication;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.user.UserProp;
import org.genepattern.server.webapp.LoginManager;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.genepattern.util.GPConstants;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GlobusAuthentication extends DefaultGenePatternAuthentication {
    /**
     * Handle call from the genepattern runtime and indicate whether or not the request is from a validated user.
     * The GlobusOAuthCallbackServlet is responsible for adding 'globus.userid' to the session so all we have
     * to do here is check for the existence of the session attribute.
     * 
     * @return the userid for an authenticated session or null if the session is not authenticated.
     */
    public String authenticate(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        String globusEmail = (String)request.getSession().getAttribute(OAuthConstants.OAUTH_EMAIL_ATTR_KEY);
        String accessToken = (String)request.getSession().getAttribute(OAuthConstants.OAUTH_TOKEN_ATTR_KEY);
        String transferToken = (String)request.getSession().getAttribute(OAuthConstants.OAUTH_TRANSFER_TOKEN_ATTR_KEY);
        String transferRefreshToken = (String)request.getSession().getAttribute(OAuthConstants.OAUTH_TRANSFER_REFRESH_TOKEN_ATTR_KEY);
        String refreshToken = (String)request.getSession().getAttribute(OAuthConstants.OAUTH_REFRESH_TOKEN_ATTR_KEY);
        
        // handle auth happening through globus instead
        if (globusEmail != null) {
            // The GenePattern login manager uses the 'email' and 'password' request attributes 
            // when creating new user accounts
            byte[] array = new byte[12]; // length is bounded by 12
            new Random().nextBytes(array);
            // they can use forgot password to generate a new one if they want to login
            // directly later
            String generatedPassword = new String(array, Charset.forName("UTF-8"));


            request.getSession().setAttribute("email", globusEmail);
            request.getSession().setAttribute("password", generatedPassword);
            request.setAttribute(GPConstants.USERID, globusEmail);
            request.getSession().setAttribute(GPConstants.USERID, globusEmail);

            LoginManager.instance().addUserIdToSession(request, globusEmail);;
            LoginManager.instance().attachAccessCookie(response, globusEmail);
            setGenePatternCookie(response, globusEmail);
            
            linkGlobusAccountToGenePattern(request, globusEmail, accessToken, transferToken, refreshToken, transferRefreshToken, generatedPassword);
            //if (!inTransaction) hmgr.commitTransaction();
            

            return globusEmail;
        } 

        // [optionally] use default authentication
        String userId =  super.authenticate(request, response);
        
        if (userId != null) populateFromPastGlobusLogin(request, userId);
        
        return userId;
    }

    /**
     * This is normally done in the login form but since we don't have the username
     * on the login page we do it here for the Globus auth instead
     * 
     * @param response
     * @param username
     */
    public void setGenePatternCookie(HttpServletResponse response, String username){
        // document.cookie = "GenePattern=" + $("#username").val() + "|" +
        // encodeURIComponent(btoa($("#password").val())) + ";path=/;domain=" + window.location.hostname;
        String s = username+"|"+"";
        String token = new String(Base64.getEncoder().encode(s.getBytes()));
        Cookie cookie = new Cookie("GenePattern", token);
        cookie.setPath("/");
        response.addCookie(cookie);
    }
    
    
    /**
     * Link the globus account to the GenePattern account by saving the IdProvider preferred ID, provider id and provider display name in UserProps
     * Also save the accessToken so we can use it later
     *  
     * @param request
     * @param globusEmail
     * @param accessToken
     * @param generatedPassword
     * @throws AuthenticationException
     */
    
    public static void linkGlobusAccountToGenePattern(HttpServletRequest request, String globusEmail, String accessToken, String transferToken, String refreshToken, String transferRefreshToken, String generatedPassword) throws AuthenticationException {
        JsonElement userJson = (JsonElement) request.getSession().getAttribute(OAuthConstants.OAUTH_USER_ID_USERPROPS_KEY);
        String canonicalId = userJson.getAsJsonObject().get("preferred_username").getAsString();
        String idProviderId = userJson.getAsJsonObject().get("identity_provider").getAsString();
        String idProviderName = userJson.getAsJsonObject().get("identity_provider_display_name").getAsString();

        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        final GpContext userContext=Util.getUserContext(request);
        String userId = userContext.getUserId();
        
        HibernateSessionManager hmgr = HibernateUtil.instance();
        UserDAO dao = new UserDAO(hmgr);

        boolean inTransaction = hmgr.isInTransaction();
        if (!inTransaction) hmgr.beginTransaction();
        
        User user;
        if (userId != null){
            user = dao.findById(userId);
        } else {
            user = dao.findById(globusEmail);
        }
        if (user  != null) {
            // link with an existing account
        } else {
            // no existing user with that email as the id.  We want to use
            // the UserAccountManager to create the new account and set the properties
            // to let us know its been linked
            UserAccountManager.createUser(globusEmail, generatedPassword, globusEmail);
            user = dao.findById(globusEmail);
           
        }
        dao.setProperty(user.getUserId(),  OAuthConstants.OAUTH_USER_ID_USERPROPS_KEY, canonicalId);
        dao.setProperty(user.getUserId(),  OAuthConstants.OAUTH_EMAIL_USERPROPS_KEY, globusEmail);
        dao.setProperty(user.getUserId(),  OAuthConstants.OAUTH_TOKEN_ATTR_KEY, accessToken);
        dao.setProperty(user.getUserId(),  OAuthConstants.OAUTH_TRANSFER_TOKEN_ATTR_KEY, transferToken);
        dao.setProperty(user.getUserId(),  OAuthConstants.OAUTH_TRANSFER_REFRESH_TOKEN_ATTR_KEY, transferRefreshToken);
        dao.setProperty(user.getUserId(),  OAuthConstants.OAUTH_REFRESH_TOKEN_ATTR_KEY, refreshToken);
        dao.setProperty(user.getUserId(),  OAuthConstants.OAUTH_ID_PROVIDER_ID_USERPROPS_KEY, idProviderId);
        dao.setProperty(user.getUserId(),  OAuthConstants.OAUTH_ID_PROVIDER_DISPLAY_USERPROPS_KEY, idProviderName);
    }

    
    public void populateFromPastGlobusLogin(HttpServletRequest request, String userId){
        
        HibernateSessionManager hmgr = HibernateUtil.instance();
        UserDAO dao = new UserDAO(hmgr);

        //boolean inTransaction = hmgr.isInTransaction();
        //if (!inTransaction) hmgr.beginTransaction();
        UserProp globusUserIdentityProp = dao.getProperty(userId, OAuthConstants.OAUTH_USER_ID_USERPROPS_KEY, "");
        UserProp globusUserEmailProp = dao.getProperty(userId, OAuthConstants.OAUTH_EMAIL_USERPROPS_KEY, "");
        UserProp globusAccessTokenProp = dao.getProperty(userId, OAuthConstants.OAUTH_TOKEN_ATTR_KEY, "");
        UserProp globusRefreshTokenProp = dao.getProperty(userId, OAuthConstants.OAUTH_REFRESH_TOKEN_ATTR_KEY, "");
        UserProp globusTransferTokenProp = dao.getProperty(userId, OAuthConstants.OAUTH_TRANSFER_TOKEN_ATTR_KEY, "");
        UserProp globusTransferRefreshTokenProp = dao.getProperty(userId, OAuthConstants.OAUTH_TRANSFER_REFRESH_TOKEN_ATTR_KEY, "");
        
        request.getSession().setAttribute(OAuthConstants.OAUTH_USER_ID_ATTR_KEY, globusUserEmailProp.getValue());
        request.getSession().setAttribute(OAuthConstants.OAUTH_EMAIL_ATTR_KEY, globusUserEmailProp.getValue());
        request.getSession().setAttribute(OAuthConstants.OAUTH_TOKEN_ATTR_KEY, globusAccessTokenProp.getValue());
        request.getSession().setAttribute(OAuthConstants.OAUTH_TRANSFER_TOKEN_ATTR_KEY, globusTransferTokenProp.getValue());
        request.getSession().setAttribute(OAuthConstants.OAUTH_USER_ID_USERPROPS_KEY, globusUserIdentityProp.getValue());
        request.getSession().setAttribute(OAuthConstants.OAUTH_REFRESH_TOKEN_ATTR_KEY, globusRefreshTokenProp.getValue());
        request.getSession().setAttribute(OAuthConstants.OAUTH_TRANSFER_REFRESH_TOKEN_ATTR_KEY, globusTransferRefreshTokenProp.getValue());
        
        
    }
    
    
    
    /**
     * TODO implement OpenID authentication via SOAP interface!
     * Without this your server can't authenticate users who connect to GenePattern from the SOAP interface.
     */
    public boolean authenticate(String user, byte[] credentials) throws AuthenticationException {
        return super.authenticate(user, credentials);
    }

    public void logout(String userid, HttpServletRequest request, HttpServletResponse response) {
        request.getSession().removeAttribute(OAuthConstants.OAUTH_USER_ID_ATTR_KEY);
        request.getSession().removeAttribute(OAuthConstants.OAUTH_EMAIL_ATTR_KEY);
        request.getSession().removeAttribute(OAuthConstants.OAUTH_TOKEN_ATTR_KEY);
        
        request.getSession().removeAttribute(OAuthConstants.OAUTH_TRANSFER_TOKEN_ATTR_KEY);
        request.getSession().removeAttribute(OAuthConstants.OAUTH_USER_ID_USERPROPS_KEY);
        request.getSession().removeAttribute(OAuthConstants.OAUTH_REFRESH_TOKEN_ATTR_KEY);
        request.getSession().removeAttribute(OAuthConstants.OAUTH_TRANSFER_REFRESH_TOKEN_ATTR_KEY);
        
        
        super.logout(userid, request, response);
    }

    /**
     * redirect back to the login page
     */
    public void requestAuthentication(HttpServletRequest request, HttpServletResponse response) throws IOException {
        
        super.requestAuthentication(request, response);
    }
}
