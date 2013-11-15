package org.genepattern.server.webapp.rest.api.v1.suite;

import java.net.MalformedURLException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.util.LSID;
import org.genepattern.webservice.SuiteInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@Path("/"+SuiteResource.URI_PATH)
public class SuiteResource {
    final static private Logger log = Logger.getLogger(SuiteResource.class);
    final static public String URI_PATH="v1/suites";
    
    /**
     * Method to get a json array of suites
     * Currently only serves up information needed for the modules & pipelines widget
     * Used in the new modules & pipelines widget
     * @param request
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("all.json")
    public Response getAllSuites(@Context HttpServletRequest request) {
        final boolean isInTransaction = HibernateUtil.isInTransaction();
        try {
            // Get the latest suites
            final AdminDAO adminDao = new AdminDAO();
            SuiteInfo[] allSuites = adminDao.getLatestSuites();
            
            // Return the JSON object
            JSONArray jsonArray = new JSONArray();
            for (SuiteInfo suiteInfo : allSuites) {
                JSONObject jsonObj = asJson(suiteInfo);
                jsonArray.put(jsonObj);
            }
            return Response.ok().entity(jsonArray.toString()).build();
        }
        catch (Throwable t) {
            log.error(t);
            final String errorMessage="Error constructing json response for suites.json: " + t.getLocalizedMessage();
            return Response.serverError().entity(errorMessage).build();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
 
    /**
     * Wrap suites for the JSON call
     * @param suiteInfo
     * @return
     * @throws JSONException
     */
    private JSONObject asJson(SuiteInfo suiteInfo) throws JSONException {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("lsid", suiteInfo.getLsid());
        jsonObj.put("name", suiteInfo.getName());
        jsonObj.put("description", suiteInfo.getDescription());
        try {
            final LSID lsid = new LSID(suiteInfo.getLsid());
            jsonObj.put("version", lsid.getVersion());
        }
        catch (MalformedURLException e) {
            log.error("Error getting lsid for suite.name=" + suiteInfo.getName(), e);
        }
        jsonObj.put("documentation", getDocumentation(suiteInfo));
        jsonObj.put("tags", new JSONArray());
        return jsonObj;
    }
    
    private String getDocumentation(SuiteInfo suiteInfo) {
        String[] files = suiteInfo.getDocumentationFiles();
        if (files == null || files.length < 1) return "";
        else return files[0];
    }
}
