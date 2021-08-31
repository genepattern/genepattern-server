/*******************************************************************************
 * Copyright (c) 2003-2021 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.oauth;

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
import org.genepattern.util.GPConstants;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Resource for OAuth2 endpoints
 *
 * @author Thorin Tabor
 */
@Path("/v1/globus")
public class GlobusTaskResource {

    
    @GET
    @Path("/currentTasks")
    public Response getCurrentTasks(@Context HttpServletRequest request, @Context HttpServletResponse response) throws IOException {
        String userId = (String)request.getSession().getAttribute(GPConstants.USERID);
        
        ArrayList<HashMap<String,String>> taskList = GlobusTransferMonitor.getInstance().getStatusForUser(userId);
 
        JsonArray taskListJson = new JsonArray();
        for (int i=0; i < taskList.size(); i++){
            HashMap<String,String> taskMap = taskList.get(i);
            JsonObject task = this.mapToJson(taskMap);
            taskListJson.add(task);
        }
        String listString = taskListJson.toString();
        
        return Response.status(200).entity(listString).build();
    
    }
    

 
    JsonObject mapToJson(HashMap<String,String> map){
        JsonObject transferObject = new JsonObject();
        transferObject.addProperty("id", map.get("id"));
        transferObject.addProperty("error", map.get("error"));
        transferObject.addProperty("status", map.get("status"));
        transferObject.addProperty("prevStatus", map.get("prevStatus"));
        transferObject.addProperty("lastStatusCheckTime", map.get("lastStatusCheckTime"));
        transferObject.addProperty("file", map.get("file"));
        
        return transferObject;
    }
    
    @GET
    @Path("/verifyGlobusLogin")
    public Response verifyGlobusLogin(@Context HttpServletRequest request, @Context HttpServletResponse response) throws IOException {
        String userId = (String)request.getSession().getAttribute(GPConstants.USERID);
        
        ArrayList<HashMap<String,String>> taskList = GlobusTransferMonitor.getInstance().getStatusForUser(userId);
        JsonObject ret = new JsonObject();
        
        try {
            GlobusClient gc = new GlobusClient();
            // try to refresh our token.  If it goes through we
            // are good to go
            gc.refreshToken(request, OAuthConstants.OAUTH_TRANSFER_REFRESH_TOKEN_ATTR_KEY, OAuthConstants.OAUTH_TRANSFER_TOKEN_ATTR_KEY);
            ret.addProperty("loginValid", true);
            // cache the session since the verify happens right before handoff to globus
            // but globus screws up byt not letting us get our cookies when it comes back due to
            // using an ajax CORS post instead of a form submit
            request.getSession().getServletContext().setAttribute("globus_session_"+userId, request.getSession());
            
        } catch (Exception e){
            e.printStackTrace();
            ret.addProperty("loginValid", false);
        }
        return Response.status(200).entity(ret.toString()).build();
    
    }
    
    
}
