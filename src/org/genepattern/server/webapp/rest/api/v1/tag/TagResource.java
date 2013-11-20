package org.genepattern.server.webapp.rest.api.v1.tag;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.PinModuleDAO;
import org.json.JSONException;
import org.json.JSONObject;

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
