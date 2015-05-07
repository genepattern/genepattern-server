/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.oauth;

/**
 * Created by tabor on 5/5/15.
 */
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.ext.dynamicreg.server.request.JSONHttpServletRequestWrapper;
import org.apache.oltu.oauth2.ext.dynamicreg.server.request.OAuthServerRegistrationRequest;
import org.apache.oltu.oauth2.ext.dynamicreg.server.response.OAuthServerRegistrationResponse;
import org.genepattern.server.webapp.rest.api.v1.oauth.demo.ServerContent;

/**
 *
 *
 *
 */
@Path("/register")
public class RegistrationEndpoint {


    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response register(@Context HttpServletRequest request) throws OAuthSystemException {


        OAuthServerRegistrationRequest oauthRequest = null;
        try {
            oauthRequest = new OAuthServerRegistrationRequest(new JSONHttpServletRequestWrapper(request));
            oauthRequest.discover();
            oauthRequest.getClientName();
            oauthRequest.getClientUrl();
            oauthRequest.getClientDescription();
            oauthRequest.getRedirectURI();

            OAuthResponse response = OAuthServerRegistrationResponse
                    .status(HttpServletResponse.SC_OK)
                    .setClientId(ServerContent.CLIENT_ID)
                    .setClientSecret(ServerContent.CLIENT_SECRET)
                    .setIssuedAt(ServerContent.ISSUED_AT)
                    .setExpiresIn(ServerContent.EXPIRES_IN)
                    .buildJSONMessage();
            return Response.status(response.getResponseStatus()).entity(response.getBody()).build();

        } catch (OAuthProblemException e) {
            OAuthResponse response = OAuthServerRegistrationResponse
                    .errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                    .error(e)
                    .buildJSONMessage();
            return Response.status(response.getResponseStatus()).entity(response.getBody()).build();
        }

    }
}