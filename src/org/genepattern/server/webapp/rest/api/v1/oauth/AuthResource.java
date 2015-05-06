package org.genepattern.server.webapp.rest.api.v1.oauth;

import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;
import org.genepattern.server.webapp.OAuthManager;
import org.genepattern.server.webapp.rest.api.v1.oauth.demo.TestContent;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

/**
 * Resource for OAuth2 endpoints
 *
 * @author Thorin Tabor
 */
@Path("/v1/oauth")
public class AuthResource {
    final static private Logger log = Logger.getLogger(AuthResource.class);

    final static public long TOKEN_EXPIRY_TIME = 86400l; // Default is 1 day
    final static public long CODE_EXPIRY_TIME = 300l; // Default is 5 minutes

    /**
     * This endpoint is for an OAuth Client (ex: GenePattern Notebook) to request
     * authorization from GenePattern to access a user's data on behalf of the user
     *
     * The following GET parameters should be included with a request to this endpoint:
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
     *          client_id:
     *              This is the ID of the client accessing the information from GenePattern
     *              (ex: GenePattern_Notebook).
     *          redirect_uri:
     *              The response will redirect back to this URI after a code or token is
     *              generated. The code or token will be attended to the URL as a GET parameter
     *              or fragment (ex: ?code=AUTH_CODE_HERE or #token=TOKEN_HERE)
     *          scope:
     *              This is the scope of the access being requested. In the context of the
     *              GenePattern REST API this is a username, since the client is requesting
     *              access to that user's account.
     *
     * @param request HttpServletRequest
     * @return Response
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

            // Get the response_type (code or token)
            String responseType = oauthRequest.getParam(OAuth.OAUTH_RESPONSE_TYPE);

            // Create the OAuth response builder
            OAuthASResponse.OAuthAuthorizationResponseBuilder builder = OAuthASResponse.authorizationResponse(request, HttpServletResponse.SC_MOVED_TEMPORARILY);

            // Get the scopes and user
            Set<String> scopes = oauthRequest.getScopes();
            OAuthManager.validateScopes(scopes);
            String user = OAuthManager.userFromScope(scopes);

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
                OAuthManager.instance().createTokenSession(user, authorizationCode, OAuthManager.calcExpiry(CODE_EXPIRY_TIME));
            }

            // Register the ACCESS TOKEN with the server, if applicable
            if (responseType.equals(ResponseType.TOKEN.toString())) {
                OAuthManager.instance().createTokenSession(user, accessToken, OAuthManager.calcExpiry(TOKEN_EXPIRY_TIME));
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

    @POST
    @Path("/token")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/json")
    public Response requestToken(@Context HttpServletRequest request) throws OAuthSystemException {
        // Declare required variables
        OAuthTokenRequest oauthRequest = null;
        OAuthIssuer oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
        String grantType = null;
        String authCode = null;
        String redirectURI = null;
        String clientID = null;
        String clientSecret = null;

        try {
            // Get the request
            oauthRequest = new OAuthTokenRequest(request);

            // Get all possible parameters
            grantType = oauthRequest.getGrantType();
            authCode = oauthRequest.getParam(OAuth.OAUTH_CODE);
            redirectURI = oauthRequest.getRedirectURI();
            clientID = oauthRequest.getClientId();
            clientSecret = oauthRequest.getClientSecret();

            //check if clientid is valid
            if (!TestContent.CLIENT_ID.equals(oauthRequest.getParam(OAuth.OAUTH_CLIENT_ID))) {
                OAuthResponse response =
                        OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                                .setError(OAuthError.TokenResponse.INVALID_CLIENT).setErrorDescription("client_id not found")
                                .buildJSONMessage();
                return Response.status(response.getResponseStatus()).entity(response.getBody()).build();
            }

            //do checking for different grant types
            if (oauthRequest.getParam(OAuth.OAUTH_GRANT_TYPE).equals(GrantType.AUTHORIZATION_CODE.toString())) {
                if (!TestContent.AUTHORIZATION_CODE.equals(oauthRequest.getParam(OAuth.OAUTH_CODE))) {
                    OAuthResponse response = OAuthASResponse
                            .errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                            .setError(OAuthError.TokenResponse.INVALID_GRANT)
                            .setErrorDescription("invalid authorization code")
                            .buildJSONMessage();
                    return Response.status(response.getResponseStatus()).entity(response.getBody()).build();
                }
            }
            else if (oauthRequest.getParam(OAuth.OAUTH_GRANT_TYPE).equals(GrantType.PASSWORD.toString())) {
                if (!TestContent.PASSWORD.equals(oauthRequest.getPassword())
                        || !TestContent.USERNAME.equals(oauthRequest.getUsername())) {
                    OAuthResponse response = OAuthASResponse
                            .errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                            .setError(OAuthError.TokenResponse.INVALID_GRANT)
                            .setErrorDescription("invalid username or password")
                            .buildJSONMessage();
                    return Response.status(response.getResponseStatus()).entity(response.getBody()).build();
                }
            }
            else if (oauthRequest.getParam(OAuth.OAUTH_GRANT_TYPE).equals(GrantType.REFRESH_TOKEN.toString())) {
                OAuthResponse response = OAuthASResponse
                        .errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(OAuthError.TokenResponse.INVALID_GRANT)
                        .setErrorDescription("invalid username or password")
                        .buildJSONMessage();
                return Response.status(response.getResponseStatus()).entity(response.getBody()).build();
            }

            OAuthResponse response = OAuthASResponse
                    .tokenResponse(HttpServletResponse.SC_OK)
                    .setAccessToken(oauthIssuerImpl.accessToken())
                    .setExpiresIn("3600")
                    .buildJSONMessage();

            return Response.status(response.getResponseStatus()).entity(response.getBody()).build();
        }
        catch (OAuthProblemException e) {
            OAuthResponse res = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST).error(e)
                    .buildJSONMessage();
            return Response.status(res.getResponseStatus()).entity(res.getBody()).build();
        }
    }
}
