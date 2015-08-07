/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.config;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Resource for obtaining config on the client.
 *
 * @author Thorin Tabor
 */
@Path("/v1/config")
public class ConfigResource {
    final static private Logger log = Logger.getLogger(ConfigResource.class);

    @GET
    @Path("/admin")
    @Produces(MediaType.APPLICATION_JSON)
    public Response isAdmin(@Context HttpServletRequest request) {
        GpContext userContext = Util.getUserContext(request);
        JSONObject object = new JSONObject();
        try {
            object.put("result", userContext.isAdmin());
        } catch (JSONException e) {
            log.error("Error producing JSON object for ConfigResource.isAdmin()");
        }
        return Response.ok().entity(object.toString()).build();
    }

    @GET
    @Path("/user")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@Context HttpServletRequest request) {
        GpContext userContext = Util.getUserContext(request);
        JSONObject object = new JSONObject();
        try {
            object.put("result", userContext.getUserId());
        } catch (JSONException e) {
            log.error("Error producing JSON object for ConfigResource.getUser()");
        }
        return Response.ok().entity(object.toString()).build();
    }

    @GET
    @Path("/gp-url")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGenePatternURL(@Context HttpServletRequest request) {
        JSONObject object = new JSONObject();
        String gpUrl = ServerConfigurationFactory.instance().getGenePatternURL().toString();
        try {
            object.put("result", gpUrl);
        } catch (JSONException e) {
            log.error("Error producing JSON object for ConfigResource.getGenePatternURL()");
        }
        return Response.ok().entity(object.toString()).build();
    }

    @GET
    @Path("/gp-version")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGenePatternVersion(@Context HttpServletRequest request) {
        JSONObject object = new JSONObject();
        String gpUrl = ServerConfigurationFactory.instance().getGenePatternVersion();
        try {
            object.put("result", gpUrl);
        } catch (JSONException e) {
            log.error("Error producing JSON object for ConfigResource.getGenePatternVersion()");
        }
        return Response.ok().entity(object.toString()).build();
    }

    @GET
    @Path("/property/{prop}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProperty(@Context HttpServletRequest request, @PathParam("prop") String prop) {
        GpContext userContext = Util.getUserContext(request);
        JSONObject object = new JSONObject();
        String propValue = ServerConfigurationFactory.instance().getGPProperty(userContext, prop, null);
        try {
            object.put("result", propValue);
        } catch (JSONException e) {
            log.error("Error producing JSON object for ConfigResource.getProperty()");
        }
        return Response.ok().entity(object.toString()).build();
    }
}
