/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.tag;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.PinModuleDAO;
import org.genepattern.server.tag.Tag;
import org.genepattern.server.tag.TagManager;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

@Path("/"+TagResource.URI_PATH)
public class TagResource {
    final static private Logger log = Logger.getLogger(TagResource.class);
    final static public String URI_PATH="v1/tags";
    
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("pin")
    public Response pinModule(@Context UriInfo uriInfo, @Context HttpServletRequest request, String body) {
        final boolean isInTransaction = HibernateUtil.isInTransaction();
        try {
            PinModuleObject pinObj = new PinModuleObject(body);
            PinModuleDAO pmDao = new PinModuleDAO();
            pmDao.pinModule(pinObj.getUser(), pinObj.getLsid(), pinObj.getPosition());
            
            return Response.ok().entity("OK").build();
        }
        catch (Throwable t) {
            log.error(t);
            final String errorMessage = "Error constructing json response for tags.pin: " + t.getLocalizedMessage();
            return Response.serverError().entity(errorMessage).build();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Path("repin")
    public Response repinModule(@Context HttpServletRequest request, String body) {
        final boolean isInTransaction = HibernateUtil.isInTransaction();
        try {
            PinModuleObject pinObj = new PinModuleObject(body);
            PinModuleDAO pmDao = new PinModuleDAO();
            pmDao.repinModule(pinObj.getUser(), pinObj.getLsid(), pinObj.getPosition());
            
            return Response.ok().entity("OK").build();
        }
        catch (Throwable t) {
            log.error(t);
            final String errorMessage="Error constructing json response for tags.repin: " + t.getLocalizedMessage();
            return Response.serverError().entity(errorMessage).build();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("unpin")
    public Response unpinModule(@Context HttpServletRequest request, String body) {
        final boolean isInTransaction = HibernateUtil.isInTransaction();
        try {
            PinModuleObject pinObj = new PinModuleObject(body);
            PinModuleDAO pmDao = new PinModuleDAO();
            pmDao.unpinModule(pinObj.getUser(), pinObj.getLsid());
            
            return Response.ok().entity("OK").build();
        }
        catch (Throwable t) {
            log.error(t);
            final String errorMessage="Error constructing json response for tags.unpin: " + t.getLocalizedMessage();
            return Response.serverError().entity(errorMessage).build();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTags(@Context UriInfo uriInfo, @Context HttpServletRequest request)
    {
        try
        {
            GpContext userContext = Util.getUserContext(request);

            JSONArray result = new JSONArray();

            List<Tag> tags = TagManager.selectAllJobTags(HibernateUtil.instance(), userContext.getUserId(), true);
            for(Tag tag: tags)
            {
                JSONObject tagObj = new JSONObject();
                tagObj.put("value", tag.getTag());
                result.put(tagObj);
            }

            return Response.ok().entity(result.toString()).build();
        }
        catch (Throwable t) {
            log.error(t);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }


    public class PinModuleObject {
        String user;
        String lsid;
        double position;

        public PinModuleObject() { super(); }  
        
        public PinModuleObject(String json) throws JSONException { 
            JSONObject jsonObj = new JSONObject(json);
            this.setUser(jsonObj.getString("user"));
            this.setLsid(jsonObj.getString("lsid"));
            this.setPosition(jsonObj.getDouble("position"));
        } 
        
        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getLsid() {
            return lsid;
        }

        public void setLsid(String lsid) {
            this.lsid = lsid;
        }

        public double getPosition() {
            return position;
        }

        public void setPosition(double position) {
            this.position = position;
        }
    }
}
