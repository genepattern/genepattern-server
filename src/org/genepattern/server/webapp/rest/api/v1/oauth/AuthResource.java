/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.oauth;

import com.google.common.base.Strings;
import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.ParameterStyle;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;
import org.apache.oltu.oauth2.ext.dynamicreg.server.request.JSONHttpServletRequestWrapper;
import org.apache.oltu.oauth2.ext.dynamicreg.server.request.OAuthServerRegistrationRequest;
import org.apache.oltu.oauth2.ext.dynamicreg.server.response.OAuthServerRegistrationResponse;
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.recaptcha.ReCaptchaException;
import org.genepattern.server.recaptcha.ReCaptchaSession;
import org.genepattern.server.webapp.LoginManager;
import org.genepattern.server.webapp.OAuthManager;
import org.genepattern.server.webapp.jsf.ForgotPasswordBean;
import org.genepattern.server.webapp.jsf.RegistrationBean;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Resource for OAuth2 endpoints
 *
 * @author Thorin Tabor
 */
@Path("/v1/oauth2")
public class AuthResource {
    final static private Logger log = Logger.getLogger(AuthResource.class);

    final static public long TOKEN_EXPIRY_TIME = 86400l; // Default is 1 day
    final static public long CODE_EXPIRY_TIME = 300l; // Default is 5 minutes
    final static public String OAUTH_EMAIL = "email";

    /**
     * Invalidates the current authentication token and forces a logout
     *
     * @param request
     * @return
     */
    @GET
    @Path("/logout")
    public Response authorize(@Context HttpServletRequest request, @Context HttpServletResponse response) throws IOException {
        // Get the access token
        javax.servlet.http.Cookie[] cookies = request.getCookies();
        String token = null;
        if (cookies != null) {
            for (javax.servlet.http.Cookie cookie : cookies) {
                if (cookie.getName().equals("GenePatternAccess")) {
                    token = cookie.getValue();
                }
            }
        }

        // Invalidate token
        if (token != null) OAuthManager.instance().invalidateToken(token);

        // Invalidate the access cookie
        javax.servlet.http.Cookie cookie = new javax.servlet.http.Cookie("GenePatternAccess", "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        // Logout
        LoginManager.instance().logout(request, response, false);

        // Return the response
        return Response.status(200).entity("{'result': true}").build();
    }

    /**
     * This endpoint is for an OAuth Client (ex: GenePattern Notebook) to request
     * authorization from GenePattern to access a user's data on behalf of the user
     *
     * The following GET parameters should be included with a request to this endpoint:
     *
     *          response_type:
     *              Expects "code" or "token", anything else is an error
     *                  code: Returns an authorization code that is returned to the
     *                      server for a token once a user gives their permission to access
     *                      their account (Ex: WordPress wants to access your Facebook account).
     *                      This must be done from secure code because in a later step it will
     *                      require a client secret, which must be kept confidential!
     *                      Do not use this option from JavaScript! By its very nature
     *                      JavaScript code is never secret!
     *                  token: Less secure than code, but useful when execution cannot be kept
     *                      secret. Returns an authorization token rather than an access code.
     *
     *          client_id:
     *              This is the ID of the client accessing the information from GenePattern
     *              (ex: GenePattern_Notebook).
     *
     *          redirect_uri:
     *              The response will redirect back to this URI after a code or token is
     *              generated. The code or token will be attended to the URL as a GET parameter
     *              or fragment (ex: ?code=AUTH_CODE_HERE or #token=TOKEN_HERE)
     *
     *          scope:
     *              This is the scope of the access being requested. In the context of the
     *              GenePattern REST API this is a username, since the client is requesting
     *              access to that user's account.
     *
     * @param request - HttpServletRequest
     * @return - Response
     * @throws URISyntaxException
     * @throws OAuthSystemException
     */
    @GET
    @Path("/auth")
    public Response authorize(@Context HttpServletRequest request) throws URISyntaxException, OAuthSystemException {

        // Declare required variables
        OAuthAuthzRequest oauthRequest = null;
        OAuthIssuerImpl oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
        String authorizationCode = null;
        String accessToken = null;

        try {
            // Parse the request to make sure it meets spec
            oauthRequest = new OAuthAuthzRequest(request);

            // Get the response_type (code or token) and client_id
            String responseType = oauthRequest.getParam(OAuth.OAUTH_RESPONSE_TYPE);
            String clientID = oauthRequest.getClientId();

            // Create the OAuth response builder
            OAuthASResponse.OAuthAuthorizationResponseBuilder builder = OAuthASResponse.authorizationResponse(request, HttpServletResponse.SC_MOVED_TEMPORARILY);

            // Get the scopes and user
            Set<String> scopes = oauthRequest.getScopes();
            OAuthManager.validateScopes(scopes);
            String user = OAuthManager.userFromScope(scopes);

            log.info("OAuth2 auth attempted | " +
                "TYPE: " + responseType + " | " +
                "CLIENT: " + clientID
            );

            // Handle CODE requests
            if (responseType.equals(ResponseType.CODE.toString())) {
                // Attach the authorization code to the response
                authorizationCode = oauthIssuerImpl.authorizationCode();
                builder.setCode(authorizationCode);
            }

            // Handle TOKEN requests
            if (responseType.equals(ResponseType.TOKEN.toString())) {
                // Attach the access token to the response
                accessToken = oauthIssuerImpl.accessToken();
                builder.setAccessToken(accessToken);
                // Alert the user when the token expires
                builder.setExpiresIn(TOKEN_EXPIRY_TIME);
            }

            // Get the redirect URI and attach the code or token
            String redirectURI = oauthRequest.getParam(OAuth.OAUTH_REDIRECT_URI);
            final OAuthResponse response = builder.location(redirectURI).buildQueryMessage();
            URI url = new URI(response.getLocationUri());

            // Register the AUTHORIZATION CODE with the server, if applicable
            if (responseType.equals(ResponseType.CODE.toString())) {
                OAuthManager.instance().createCodeSession(user, authorizationCode, clientID, OAuthManager.calcExpiry(CODE_EXPIRY_TIME));
            }

            // Register the ACCESS TOKEN with the server, if applicable
            if (responseType.equals(ResponseType.TOKEN.toString())) {
                OAuthManager.instance().createTokenSession(user, accessToken, clientID, OAuthManager.calcExpiry(TOKEN_EXPIRY_TIME));
            }

            // Return the response to the client
            return Response.status(response.getResponseStatus()).location(url).build();

        }
        catch (OAuthProblemException e) {
            // Create the response builder
            final Response.ResponseBuilder responseBuilder = Response.status(HttpServletResponse.SC_MOVED_TEMPORARILY);

            // Get the redirect URI
            String redirectUri = e.getRedirectUri();

            // If there is no redirect URI simply return an error and message
            if (OAuthUtils.isEmpty(redirectUri)) {
                throw new WebApplicationException(responseBuilder.entity("OAuth callback url needs to be provided by client!").build());
            }

            // Build and return the response
            final OAuthResponse response = OAuthASResponse.errorResponse(HttpServletResponse.SC_MOVED_TEMPORARILY).error(e).location(redirectUri).buildQueryMessage();
            final URI location = new URI(response.getLocationUri());
            return responseBuilder.location(location).build();
        }
    }

    /**
     * Generates and returns to the user an access token that can be included with
     * requests to verify authentication.
     *
     * The following should be included as GET parameters or in the POST body:
     *
     *      grant_type:
     *          This indicates what security method is being used to grant the access
     *          token. This should only be one of the following values:
     *              authorization_code:
     *                  In this case an authorization code that was returned by the
     *                  /rest/v1/oauth/ endpoint is being provided to verify identity
     *              password:
     *                  In this case a username and password will be provided to
     *                  verify identity
     *              refresh_token:
     *                  This will grant a token that can be used to obtain a new
     *                  access token. This is currently NOT IMPLEMENTED.
     *
     *      code (optional):
     *          In the authorization_code grant_type was chosen, the authorization
     *          code must be presented here and must match what was provided to the
     *          client.
     *
     *      username (optional):
     *          If the password grant_type was selected the username of the user
     *          logging in should be included here.
     *
     *      password (optional):
     *          If the password grant_type was selected the password of the user
     *          logging in should be included here.
     *
     *      redirect_uri:
     *          The response will redirect back to this URI after a token is generated.
     *          The token will be attended to the URL as a GET parameter or fragment
     *          (ex: ?code=AUTH_CODE_HERE or #token=TOKEN_HERE).
     *
     *      client_id:
     *          This is the ID of the client accessing the information from GenePattern
     *          (ex: GenePattern_Notebook).
     *
     *      client_secret:
     *          This is a secret code provided to the client (ex: GenePattern Notebook)
     *          that no one else knows that can be used to verify the identity of the
     *          client. This should only be provided if the calls can be made secretly,
     *          and should never be made through JavaScript or not through HTTPS.
     *
     * @param request - HttpServletRequest
     * @return - Response
     * @throws OAuthSystemException
     */
    @POST
    @Path("/token")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/json")
    public Response requestToken(@Context HttpServletRequest request) throws OAuthSystemException {
        // Declare required variables
        String grantType = null;
        String authCode = null;
        String username = null;
        String password = null;
        String redirectURI = null;
        String clientID = null;
        String clientSecret = null;

        try {
            // Get all possible parameters
            grantType = request.getParameter(OAuth.OAUTH_GRANT_TYPE);
            authCode = request.getParameter(OAuth.OAUTH_CODE);
            username = request.getParameter(OAuth.OAUTH_USERNAME);
            password = request.getParameter(OAuth.OAUTH_PASSWORD);
            clientID = request.getParameter(OAuth.OAUTH_CLIENT_ID);
            redirectURI = request.getParameter(OAuth.OAUTH_REDIRECT_URI);       // Use not implemented
            clientSecret = request.getParameter(OAuth.OAUTH_CLIENT_SECRET);     // Use not implemented

            // The OLTU way of doing things (overly restrictive IMHO)
//            // Get the request
//            OAuthTokenRequest oauthRequest = new OAuthTokenRequest(request);
//            // Get all possible parameters
//            grantType = oauthRequest.getGrantType();
//            authCode = oauthRequest.getCode();
//            username = oauthRequest.getUsername();
//            password = oauthRequest.getPassword();
//            redirectURI = oauthRequest.getRedirectURI();
//            clientID = oauthRequest.getClientId();
//            clientSecret = oauthRequest.getClientSecret();

            // If the authorization_code grant_type was selected
            if (GrantType.AUTHORIZATION_CODE.toString().equals(grantType)) {
                // Verify the authorization code is valid
                if (authCode == null || !OAuthManager.instance().isCodeValid(authCode)) {
                    OAuthResponse response = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST).setError(OAuthError.TokenResponse.INVALID_GRANT)
                            .setErrorDescription("invalid authorization code").buildJSONMessage();
                    return Response.status(response.getResponseStatus()).entity(response.getBody()).build();
                }

                // Verify the client_id matches the one in the session
                if (!OAuthManager.instance().getClientIDFromCode(authCode).equals(clientID)) {
                    OAuthResponse response = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST).setError(OAuthError.TokenResponse.INVALID_CLIENT)
                            .setErrorDescription("client_id not found").buildJSONMessage();
                    return Response.status(response.getResponseStatus()).entity(response.getBody()).build();
                }

                // Invalidate the authorization code
                OAuthManager.instance().invalidateCode(authCode);

                // At this point everything is assumed valid, the token is attached below
            }

            // If the password grant_type was selected
            else if (GrantType.PASSWORD.toString().equals(grantType)) {
                // Check username and password
                try {
                    UserAccountManager.instance().authenticateUser(username, password.getBytes());
                } catch (AuthenticationException e) {
                    OAuthResponse response = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST).setError(OAuthError.TokenResponse.INVALID_GRANT)
                            .setErrorDescription("invalid username or password").buildJSONMessage();
                    return Response.status(response.getResponseStatus()).entity(response.getBody()).build();
                }

                // At this point everything is assumed valid, the token is attached below
            }

            // If the refresh_token grant_type was selected
            else if (GrantType.REFRESH_TOKEN.toString().equals(grantType)) {
                // This call is NOT IMPLEMENTED yet
                throw OAuthProblemException.error("refresh_token grant_type is not implemented");
            }

            // The grant_type is unknown, throw an error
            else {
                throw OAuthProblemException.error("unknown grant_type");
            }

            // Log the successful authentication
            log.info("Successful OAuth2 login | " +
                    "GRANT: " + grantType + " | " +
                    "USERNAME: " + username + " | " +
                    "CLIENT: " + clientID
            );

            return tokenResponse(username, clientID);
        }
        catch (OAuthProblemException e) {
            // Build and return the error message
            OAuthResponse res = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST).error(e).buildJSONMessage();
            return Response.status(res.getResponseStatus()).entity(res.getBody()).build();
        }
    }

    private Response tokenResponse(String username, String clientID) throws OAuthSystemException {
        // Generate the access token
        OAuthIssuer oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
        String token = oauthIssuerImpl.accessToken();

        // Register the access token with GenePattern
        OAuthManager.instance().createTokenSession(username, token, clientID, OAuthManager.calcExpiry(TOKEN_EXPIRY_TIME));

        // Build and return the token response
        OAuthResponse response = OAuthASResponse.tokenResponse(HttpServletResponse.SC_OK).setAccessToken(token)
                .setExpiresIn(String.valueOf(TOKEN_EXPIRY_TIME)).buildJSONMessage();
        NewCookie accessCookie = generateAccessCookie(token);
        Response actualResponse = Response.status(response.getResponseStatus()).entity(response.getBody()).cookie(accessCookie).build();
        return actualResponse;
    }

    private NewCookie generateAccessCookie(String token) {
        return new NewCookie(new Cookie("GenePatternAccess", token, "/", null));
    }

    @PUT
    @Path("/forgot-password")
    @Produces("application/json")
    public Response forgotPassword(@Context HttpServletRequest request, String body) throws OAuthSystemException {
        String usernameOrEmail = null;
        String message = null;

        // Parse the request body as JSON
        try {
            JSONObject json = new JSONObject(body);
            usernameOrEmail = json.getString("usernameOrEmail");
            if (usernameOrEmail == null) throw new JSONException("null value for usernameOrEmail");
        }
        catch (JSONException e) {
            JSONObject messageJson = new JSONObject();
            try {
                messageJson.put("message", "Unable to pase the request body as JSON or missing the usernameOrEmail parameter.");
            }
            catch (JSONException e1) {} // "message" key is never null, so this will never happen

            return Response.serverError().entity(messageJson.toString()).build();
        }

        // Instantiate the ForgotPasswordBean to email a new password
        ForgotPasswordBean bean = new ForgotPasswordBean();
        bean.setUsername(usernameOrEmail);
        String status = bean.resetPassword();

        // Parse the status and set a message for the user
        if ("success".equals(status)) message = "A new password has been emailed to you.";
        else message = "Unable to locate user. Password not reset.";

        // Create the JSON object to return
        JSONObject object = new JSONObject();
        try {
            object.put("message", message);
        }
        catch (JSONException e) {
            log.error("Error producing JSON object for AuthResource.forgotPassword()");
        }
        return Response.ok().entity(object.toString()).build();
    }

    /**
     * This endpoint allows clients to register and obtain client_id and client_secret's.
     * This would allow us to track which clients are using the REST API through OAuth2.
     *
     * @param request
     * @return
     * @throws OAuthSystemException
     */
    @POST
    @Path("/register")
    @Consumes("application/json")
    @Produces("application/json")
    public Response register(@Context HttpServletRequest request) throws OAuthSystemException {
        JSONHttpServletRequestWrapper request2 = new JSONHttpServletRequestWrapper(request);

        String username = request2.getParameter(OAuth.OAUTH_USERNAME);
        String password = request2.getParameter(OAuth.OAUTH_PASSWORD);
        String clientID = request2.getParameter(OAuth.OAUTH_CLIENT_ID);
        String email = request2.getParameter(AuthResource.OAUTH_EMAIL);
        String recaptcha_token = request2.getParameter(ReCaptchaSession.G_RECAPTCHA_RESPONSE);

        // Verify parameters
        boolean validationError = false;
        String errorMessage = null;
        if (username == null || "".equals(username)) {
            errorMessage = "missing or invalid username";
            validationError = true;
        }
        else if (password == null || "".equals(password)) {
            errorMessage = "missing or invalid password";
            validationError = true;
        }
        else if (email == null || "".equals(email)) {
            errorMessage = "missing or invalid email";
            validationError = true;
        }
        else if (clientID == null || "".equals(clientID)) {
            errorMessage = "missing or invalid client ID";
            validationError = true;
        }

        ReCaptchaSession recaptcha = ReCaptchaSession.init(ServerConfigurationFactory.instance(), GpContext.getServerContext());
        if (recaptcha.isEnabled() && recaptcha.isRequiredForRest()) {
            try {
                recaptcha.verifyReCaptcha(recaptcha_token);
            }
            catch (ReCaptchaException e) {
                errorMessage = "missing or invalid recaptcha token";
                validationError = true;
            }
        }

        // Return error response
        if (validationError) {
            OAuthResponse res = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST).error(OAuthProblemException.error(errorMessage)).buildJSONMessage();
            return Response.status(res.getResponseStatus()).entity(res.getBody()).build();
        }

        try {
            // Register the new user
            registerUserHelper(request, username, password, email);
        }
        catch (AuthenticationException e) {
            // Return registration error response
            log.info("OAuth2 register attempted, error encountered when registering the new user");
            OAuthResponse res = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST).error(OAuthProblemException.error(e.getMessage())).buildJSONMessage();
            return Response.status(res.getResponseStatus()).entity(res.getBody()).build();
        }

        // Log the new user in and return the token
        return tokenResponse(username, clientID);
    }

    private void registerUserHelper(HttpServletRequest request, String username, String password, String email) throws AuthenticationException {
        GpConfig gpConfig = ServerConfigurationFactory.instance();

        UserAccountManager.createUser(gpConfig, HibernateUtil.instance(), username, password, email);
        LoginManager.instance().addUserIdToSession(request, username);
    }
}
