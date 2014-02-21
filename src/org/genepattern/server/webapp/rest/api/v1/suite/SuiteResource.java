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
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.webapp.rest.api.v1.Util;
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
     * 
     * @deprecated - 'all_suites' json representation is included in the response to /tasks/all.json
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("all.json")
    public Response getAllSuites(@Context HttpServletRequest request) {
        log.error("Unexpected call to /"+SuiteResource.URI_PATH+"/all.json");
        final GpContext userContext = Util.getUserContext(request);
        try {
            // Get the latest suites
            final SuiteInfo[] allSuites = getAllSuites(userContext); 
            // Return the JSON object
            JSONArray jsonArray = toJsonArray(allSuites);
            return Response.ok().entity(jsonArray.toString()).build();
        }
        catch (Throwable t) {
            log.error(t);
            final String errorMessage="Error constructing json response for suites.json: " + t.getLocalizedMessage();
            return Response.serverError().entity(errorMessage).build();
        }
    }
    
    public static SuiteInfo[] getAllSuites(final GpContext userContext) {
        if (userContext==null) {
            throw new IllegalArgumentException("userContext==null");
        }
        final boolean isInTransaction = HibernateUtil.isInTransaction();
        try {
            // Get the latest suites
            final SuiteInfo[] allSuites;
            final AdminDAO adminDao = new AdminDAO();
            allSuites = adminDao.getLatestSuitesForUser(userContext.getUserId());
            return allSuites;
        }
        catch (Throwable t) {
            log.error("Unexpected error getting suites for user="+userContext.getUserId(), t);
            return new SuiteInfo[0];
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    public static JSONArray toJsonArray(final SuiteInfo[] suiteInfos) throws JSONException {
        final JSONArray jsonArray = new JSONArray();
        for (final SuiteInfo suiteInfo : suiteInfos) {
            JSONObject jsonObj = asJson(suiteInfo);
            jsonArray.put(jsonObj);
        }
        return jsonArray;
    }
 
    /**
     * Wrap suites for the JSON call
     * @param suiteInfo
     * @return
     * @throws JSONException
     */
    private static JSONObject asJson(SuiteInfo suiteInfo) throws JSONException {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("lsid", suiteInfo.getLsid());
        jsonObj.put("name", suiteInfo.getName());
        jsonObj.put("description", suiteInfo.getDescription());
        try {
            final LSID lsid = new LSID(suiteInfo.getLsid());
            jsonObj.put("version", lsid.getVersion());
        }
        catch (MalformedURLException e) {
            if (log.isDebugEnabled()) {
                log.debug("Error getting lsid for suite.name=" + suiteInfo.getName(), e);
            }
            else {
                log.error("Error getting lsid for suite.name=" + suiteInfo.getName());
            }
        }
        jsonObj.put("documentation", getDocumentation(suiteInfo));
        jsonObj.put("tags", new JSONArray());
        return jsonObj;
    }
    
    private static String getDocumentation(SuiteInfo suiteInfo) {
        String[] files = suiteInfo.getDocumentationFiles();
        if (files == null || files.length < 1) return "";
        else return files[0];
    }
}
