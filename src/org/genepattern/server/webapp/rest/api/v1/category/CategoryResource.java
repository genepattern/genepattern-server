package org.genepattern.server.webapp.rest.api.v1.category;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.genepattern.server.cm.CategoryManager;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@Path("/"+CategoryResource.URI_PATH)
public class CategoryResource {
    final static private Logger log = Logger.getLogger(CategoryResource.class);
    final static public String URI_PATH="v1/categories";
    
    /**
     * Prototype call to get a list of all categories
     * @param request
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("all.json")
    public Response getAllCategories(@Context HttpServletRequest request) {
        ServerConfiguration.Context userContext = Util.getUserContext(request);
        final boolean isInTransaction = HibernateUtil.isInTransaction();
        try {
            // Get the map of the latest suites
            final boolean includeHidden=false;
            List<String> categories = CategoryManager.getAllCategories(userContext, includeHidden);
            
            // Return the JSON object
            JSONArray jsonArray = new JSONArray();
            for (String category : categories) {
                JSONObject jsonObj = asJson(category);
                jsonArray.put(jsonObj);
            }
            return Response.ok().entity(jsonArray.toString()).build();
        }
        catch (Throwable t) {
            log.error(t);
            final String errorMessage="Error constructing json response for categories.json: " + t.getLocalizedMessage();
            return Response.serverError().entity(errorMessage).build();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
 
    /**
     * Wrap a single string as a JSON object to be returned.
     * Currently used for wrapping module categories
     * @param string
     * @return
     * @throws JSONException
     */
    private JSONObject asJson(String category) throws JSONException {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("name", category);
        jsonObj.put("description", ""); // Description reserved for future use
        jsonObj.put("tags", new JSONArray());
        return jsonObj;
    }
}
