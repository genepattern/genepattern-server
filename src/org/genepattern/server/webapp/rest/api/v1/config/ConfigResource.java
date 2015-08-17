/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.config;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.message.ISystemAlert;
import org.genepattern.server.message.SystemAlertFactory;
import org.genepattern.server.message.SystemMessage;
import org.genepattern.server.webapp.jsf.SystemMessageBean;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;

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

    @GET
    @Path("/system-message")
    @Produces(MediaType.TEXT_HTML)
    public Response getSystemMessage(@Context HttpServletRequest request) {
        SystemMessageBean bean = new SystemMessageBean();
        String message = bean.getMessageAsHtmlString();
        return Response.ok().entity(message).build();
    }

    @POST
    @Path("/system-message")
    public Response setSystemMessage(@Context HttpServletRequest request, String body) {
        GpContext userContext = Util.getUserContext(request);
        boolean admin = userContext.isAdmin();

        // If not admin, return an error
        if (!admin) {
            return Response.status(401).entity("Only admins can set a system message!").build();
        }

        // Parse the JSON body
        SystemMessageDTO dto = new SystemMessageDTO(body);

        try {
            // Create the system message object
            SystemMessage sm = SystemAlertFactory.getSystemAlert().getSystemMessage();
            sm.setMessage(dto.getMessage());
            sm.setStartTime(dto.getStart());
            sm.setEndTime(dto.getEnd());
            sm.setDeleteOnRestart(dto.getDeleteOnRestart());

            // Save the system message
            ISystemAlert sysAlert = SystemAlertFactory.getSystemAlert();
            sysAlert.setSystemAlertMessage(sm);

        } catch (Exception e) {
            // If an error is thrown, return a 500 to the user
            log.error(e.getLocalizedMessage());
            return Response.status(500).entity(e.getLocalizedMessage()).build();
        }

        // Return the OK status
        return Response.ok().build();
    }

    public class SystemMessageDTO {
        public Date start;
        public Date end;
        public Boolean deleteOnRestart;
        public String message;

        public SystemMessageDTO(String body) {
            try {
                JSONObject json = new JSONObject(body);
                this.setMessage(json.getString("message"));
                this.setDeleteOnRestart(json.getBoolean("deleteOnRestart"));
                this.setStart(new Date(json.getString("start")));
                this.setEnd(new Date(json.getString("end")));
            }
            catch (JSONException e) {
                log.error("Error creating SystemMessageDTO: " + e.getLocalizedMessage());
            }

        }

        public Date getStart() {
            return start;
        }

        public void setStart(Date start) {
            this.start = start;
        }

        public Date getEnd() {
            return end;
        }

        public void setEnd(Date end) {
            this.end = end;
        }

        public Boolean getDeleteOnRestart() {
            return deleteOnRestart;
        }

        public void setDeleteOnRestart(Boolean deleteOnRestart) {
            this.deleteOnRestart = deleteOnRestart;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
