package org.genepattern.server.webapp.rest.api.v1.task;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.TaskLSIDNotFoundException;
import org.genepattern.server.cm.CategoryUtil;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.eula.EulaInfo;
import org.genepattern.server.eula.EulaManager;
import org.genepattern.server.eula.InitException;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.job.input.choice.ChoiceInfo;
import org.genepattern.server.job.input.choice.ChoiceInfoHelper;
import org.genepattern.server.job.input.choice.ChoiceInfoParser;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.server.tags.TagManager;
import org.genepattern.server.tags.TagManager.Tag;
import org.genepattern.server.webapp.EulaServlet;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.genepattern.server.webapp.rest.api.v1.suite.SuiteResource;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sun.jersey.api.Responses;

/**
 * RESTful implementation of the /task resource.
 * 
 * Example usage, via curl command line.
   <p>To get the task_info for a given task name:
   <pre>
   curl -u test:test http://127.0.0.1:8080/gp/rest/v1/tasks/ComparativeMarkerSelection
   </pre>
   <p>Or by task lsid:
   <pre>
   curl -u test:test http://127.0.0.1:8080/gp/rest/v1/tasks/urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:9
   </pre>
   
   This returns a JSON representation of the task, for example,
   <pre>
{
"href":"http://127.0.0.1:8080/gp/rest/v1/tasks/urn:lsid:broad.mit.edu:cancer.software.genepattern.module.test.analysis:00007:0.1",
"name":"TestJavaWrapper",
"lsid":"urn:lsid:broad.mit.edu:cancer.software.genepattern.module.test.analysis:00007:0.1",
"params":[
  {"text": {
     "attributes": {
       "default_value":"",
       "optional":"on",
       "prefix_when_specified":"--text=",
       "type":"java.lang.String",
       "fileFormat":""}}},
       
   ...
   
  {"file":{
    "attributes":{
      "default_value":"",
      "optional":"on",
      "prefix_when_specified":"--file=",
      "MODE":"IN",
      "type":"java.io.File",
      "TYPE":"FILE",
      "fileFormat":""}}}
]
}

   </pre>
 *
 * The value of the lower-case 'type' attribute should be used when testing for the type of input parameter.
 * Here are some example values:
   <pre>
"type":"java.io.File"
"type":"java.lang.String"
"type":"java.lang.Integer"
"type":"java.lang.Float"
"type":"DIRECTORY"
"type":"PASSWORD"
   </pre>
 *   
 * 
 * @author pcarr
 *
 */
@Path("/"+TasksResource.URI_PATH)
public class TasksResource {
    final static private Logger log = Logger.getLogger(TasksResource.class);
    final static public String URI_PATH="v1/tasks";
    
    public static String getTaskInfoPath(final HttpServletRequest request, final TaskInfo taskInfo) {
        String rootPath=UrlUtil.getGpUrl(request);
        if (!rootPath.endsWith("/")) {
            rootPath += "/";
        }
        rootPath += "rest/";
        rootPath += URI_PATH + "/" + taskInfo.getLsid();
        return rootPath;
    }
    
    /**
     * Get the relative path, relative to the root REST API end point, to GET the choiceInfo for the given parameter for the given task.
     * 
     * @param taskInfo
     * @param pname
     * @return
     */
    public static String getChoiceInfoPath(final HttpServletRequest request, final TaskInfo taskInfo, final String pname) { 
        // at the moment, (circa GP 3.7.0), the task LSID and the parameter name will be valid URI path components
        // if this ever changes we should encode them
        //String path = URI_PATH + "/" + UrlUtil.encodeURIcomponent( taskInfo.getLsid() ) + "/" + UrlUtil.encodeURIcomponent( pname ) + "/choiceInfo.json";
        String path = getTaskInfoPath(request, taskInfo) + "/" + pname  + "/choiceInfo.json";
        return path;
    }
    
    /**
     * Get the JSON representation for the list of one or more license agreements which the current user must agree to 
     * before they can run the given module.
     * 
     * If there are no pending licenses this may may ...
     * (1) return null, or
     * (2) return a valid json representation with no 'pendingEulas' property, or
     * (3) return a valid json representation with an empty array for the 'pendingEulas' property
     * 
     * @param request
     * @param userContext
     * @param taskInfo
     * @return
     * @throws JSONException
     */
    public static JSONObject getPendingEulaForModuleJson(final HttpServletRequest request, final GpContext userContext, final TaskInfo taskInfo) 
    throws JSONException
    {
        final boolean includePending=true;
        final boolean includeAll=false;
        final JSONObject json=getEulaForModuleJson(request, userContext, taskInfo, includePending, includeAll);
        if (!json.has("pendingEulas")) {
            return null;
        }
        if (json.getJSONArray("pendingEulas").length() == 0) {
            return null;
        }
        return json;
    }

    public static JSONObject getEulaForModuleJson(final HttpServletRequest request, final GpContext userContext, final TaskInfo taskInfo, final boolean includePending, final boolean includeAll) 
    throws JSONException
    {
        userContext.setTaskInfo(taskInfo);
        final List<EulaInfo> pendingEulas;
        if (includePending) {
            pendingEulas=EulaManager.instance(userContext).getPendingEulaForModule(userContext);
        }
        else {
            pendingEulas=null;
        }
        final List<EulaInfo> allEulas;
        if (includeAll) {
            allEulas=EulaManager.instance(userContext).getAllEulaForModule(userContext); 
        }
        else {
            allEulas=null;
        }
        
        final JSONObject eulaObject = initEulaJson( request, userContext, taskInfo, pendingEulas, allEulas );
        return eulaObject;
    }

    private static JSONObject initEulaJson(
            final HttpServletRequest request, 
            final GpContext userContext, 
            final TaskInfo taskInfo, 
            final List<EulaInfo> pendingEulas, 
            final List<EulaInfo> allEulas
    ) 
    throws JSONException {
        final JSONObject eulaObj=new JSONObject();
        eulaObj.put("currentTaskName", taskInfo.getName());
        eulaObj.put("currentLsid", taskInfo.getLsid());
        try {
            eulaObj.put("currentLsidVersion", new LSID(taskInfo.getLsid()).getVersion());
        }
        catch (MalformedURLException e) {
            log.error(e);
        }
        
        if (pendingEulas != null) {
            final JSONArray pending=new JSONArray();
            for(final EulaInfo eulaInfo : pendingEulas) {
                JSONObject eulaInfoJson=eulaInfoToJson(eulaInfo);
                pending.put(eulaInfoJson);
            }
            eulaObj.put("pendingEulas", pending);
        }
        if (allEulas != null) {
            final JSONArray all=new JSONArray();
            for(final EulaInfo eulaInfo : allEulas) {
                JSONObject eulaInfoJson=eulaInfoToJson(eulaInfo);
                all.put(eulaInfoJson);
            }
            eulaObj.put("allEulas", all);
        }
        
        // return enough info to make an HTTP request as a callback to the server to accept all pending license(s) for the module
        final String acceptUrl=EulaServlet.getServletPath(request);
        eulaObj.put("acceptUrl", acceptUrl);
        eulaObj.put("acceptType", "GET");
        final JSONObject acceptData=new JSONObject();
        acceptData.put("lsid", taskInfo.getLsid());
        eulaObj.put("acceptData", acceptData);
        return eulaObj;
    }
    
    private static JSONObject eulaInfoToJson(final EulaInfo eulaInfo) throws JSONException {
        JSONObject eulaInfoJson=new JSONObject();
        eulaInfoJson.put("moduleName", eulaInfo.getModuleName());
        eulaInfoJson.put("moduleLsid", eulaInfo.getModuleLsid());
        eulaInfoJson.put("moduleLsidVersion", eulaInfo.getModuleLsidVersion());
        try {
            eulaInfoJson.put("content", eulaInfo.getContent());
        }
        catch (InitException e) {
            log.error("Error getting content for eula for "+eulaInfo.getModuleName()+" ("+eulaInfo.getModuleLsid()+")", e);
            eulaInfoJson.put("contentError", e.getLocalizedMessage());
        }
        return eulaInfoJson;
    }

    /**
     * Returns a hash of the modules visible to the user
     * @param user
     * @return
     */
    private String getTasksHash(String user) {
        //TODO: Implement correct hash
        // Currently the hash simply returns the user and the hour
        // This results in a cache that lasts until the top of the hour each hour.
        Date date = new Date();
        DateFormat df = new SimpleDateFormat("hh:'00' a");
        String hour = df.format(date);
        return user + " " + hour;
    }
    
    /**
     * Rapid prototype method to get the latest version of all installed tasks in json format,
     * for use by the new Modules & Pipelines search panel.
     * 
     * Example usage:
     * <pre>
     * curl -u test:test http://127.0.0.1:8080/gp/rest/v1/tasks/all.json >> all_modules.json
     * </pre>
     * 
     *  
     * @param request
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("all.json")
    public Response getAllTasks(
            final @QueryParam("includeHidden") String includeHidden,
            final @Context HttpServletRequest request, 
            final @Context HttpServletResponse response) {
        final GpContext userContext = Util.getUserContext(request);
        final String userId = userContext.getUserId();

        // Check the etag and return a 304 if they match
//        String requestEtag = request.getHeader("If-None-Match");
//        String responseEtag = getTasksHash(userId);
//        if (responseEtag.equals(requestEtag)) {
//            return Response.notModified().build();
//        }
//        // Otherwise add the correct etag
//        else {
//            response.addHeader("etag", responseEtag);
//        }
        
        // Check for "return hidden modules" flag
        final boolean isInTransaction = HibernateUtil.isInTransaction();
        try {
            // Get the map of the latest tasks
            final AdminDAO adminDao = new AdminDAO();
            final TaskInfo[] allTasks;
            allTasks = adminDao.getAllTasksForUser(userId);
            final Map<String, TaskInfo> latestTasks = AdminDAO.getLatestTasks(allTasks);
            //filter out the hidden tasks
            final CategoryUtil cu=new CategoryUtil();
            // multimap of <baseLsid,categoryNames>
            final Multimap<String,String> customCategoryMap=cu.getCustomCategoriesFromDb();
            final Set<String> hiddenCategories=cu.getHiddenCategories(userContext);
            //initialize suites, multimap of <baseLsid,SuiteInfos>
            final SuiteInfo[] suiteInfos=SuiteResource.getAllSuites(userContext);
            final Multimap<String,SuiteInfo> suiteInfoMap=initSuiteInfoMap(suiteInfos);
            
            final boolean _includeHidden= includeHidden != null;
            SortedSet<TaskInfo> filteredTasks=new TreeSet<TaskInfo>( new AdminDAO.TaskNameComparator() );
            final Multimap<String, String> filteredCategories=HashMultimap.create();
            for(final Entry<String,TaskInfo> entry : latestTasks.entrySet()) {
                final TaskInfo taskInfo=entry.getValue();
                final String baseLsid=CategoryUtil.getBaseLsid(taskInfo);
                
                final Collection<String> categories;
                if (baseLsid != null && customCategoryMap.containsKey(baseLsid)) {
                    categories=customCategoryMap.get(baseLsid);
                    if (log.isDebugEnabled()) {
                        log.debug("custom category for baseLsid="+baseLsid+": "+categories);
                    }
                }
                else {
                    categories=cu.getCategoriesFromManifest(taskInfo);
                }
                
                if (categories != null) {
                    for(final String category : categories) {
                        if (_includeHidden ||  (!hiddenCategories.contains(category) && !cu.isHidden(category))) {
                            filteredTasks.add( taskInfo );
                            filteredCategories.put( taskInfo.getLsid(), category );
                        }
                    }
                }
            }

            // Return the JSON object
            JSONArray allModules = new JSONArray();
            for(final TaskInfo taskInfo : filteredTasks) {
                try {
                    final Collection<String> categories=filteredCategories.get(taskInfo.getLsid());
                    final String baseLsid=CategoryUtil.getBaseLsid(taskInfo);
                    Collection<String> suiteNames=Collections.emptySet();
                    if (baseLsid != null) {
                        final Collection<SuiteInfo> suiteInfosForModule=suiteInfoMap.get(baseLsid);
                        if (suiteInfosForModule != null && suiteInfosForModule.size()>0) {
                            suiteNames=new ArrayList<String>();
                            for(final SuiteInfo suiteInfo : suiteInfosForModule) {
                                suiteNames.add(suiteInfo.getName());
                            }
                        }
                    }
                    JSONObject jsonObj = getTaskSearchEntryJson(request, taskInfo, categories, suiteNames, userContext);
                    allModules.put(jsonObj);
                }
                catch (Exception e) {
                    log.error("Exception thrown rendering to JSON (" + taskInfo.getLsid() + "): " + e.getMessage());
                    continue;
                }
            }

            // Return the JSON object 
            JSONArray allCategories = initAllCategoriesJson(filteredCategories.values());
            JSONArray allSuites = initAllSuitesJson(suiteInfos);
            JSONObject kindToModules = initSendToModulesMap(filteredTasks);

            JSONObject jsonObj=new JSONObject();
            jsonObj.put("all_modules", allModules);
            jsonObj.put("all_categories", allCategories);
            jsonObj.put("all_suites", allSuites);
            jsonObj.put("kindToModules", kindToModules);
            return Response.ok().entity(jsonObj.toString()).build();
        }
        catch (Throwable t) {
            log.error(t);
            final String errorMessage="Error constructing json response for all.json: "+
                    t.getLocalizedMessage();
            return Response.serverError().entity(errorMessage).build();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

    private JSONObject initSendToModulesMap(SortedSet<TaskInfo> filteredTasks) {
        JSONObject toReturn = new JSONObject();

        for (TaskInfo info : filteredTasks) {
            Set<String> kinds = info._getInputFileTypes();

            for (String kind : kinds) {
                JSONArray lsids = null;
                try { lsids = toReturn.getJSONArray(kind); } catch (JSONException e) { lsids = new JSONArray(); }
                lsids.put(info.getLsid());

                try {
                    toReturn.put(kind, lsids);
                } catch (JSONException e) {
                    log.error("ERROR building sendToModule map in JSON");
                }
            }
        }

        return toReturn;
    }
    
    private JSONArray initAllCategoriesJson(final Collection<String> categoryNames) {
        final JSONArray categoriesJson = new JSONArray();
        final SortedSet<String> sortedCategoryNames = new TreeSet<String>(new Comparator<String>() {
            // sort categories alphabetically, ignoring case
            public int compare(String arg0, String arg1) {
                String arg0tl = arg0.toLowerCase();
                String arg1tl = arg1.toLowerCase();
                int rval = arg0tl.compareTo(arg1tl);
                if (rval == 0) {
                    rval = arg0.compareTo(arg1);
                }
                return rval;
            }
        });
        sortedCategoryNames.addAll( categoryNames );
        for (final String category : sortedCategoryNames) {
            try {
                final JSONObject jsonObj = getCategoryJson(category);
                categoriesJson.put(jsonObj);
            }
            catch (JSONException e) {
                log.error("Error processing category="+category, e);
            }
        } 
        return categoriesJson;
    }
    
    /**
     * Wrap a single string as a JSON object to be returned.
     * Currently used for wrapping module categories
     * @param category
     * @return
     * @throws JSONException
     */
    public static JSONObject getCategoryJson(final String category) throws JSONException {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("name", category);
        jsonObj.put("description", ""); // Description reserved for future use
        jsonObj.put("tags", new JSONArray());
        return jsonObj;
    }
    
    private Multimap<String,SuiteInfo> initSuiteInfoMap(final SuiteInfo[] suiteInfos) {
        final Multimap<String,SuiteInfo> map=HashMultimap.create();
        for(final SuiteInfo suiteInfo : suiteInfos) {
            for(final String moduleLsid : suiteInfo.getModuleLsids()) {
                final String baseLsid=CategoryUtil.getBaseLsid(moduleLsid);
                if (baseLsid != null) {
                    map.put(baseLsid, suiteInfo);
                }
            }
        }
        return map;
    }
    
    private JSONArray initAllSuitesJson(final SuiteInfo[] suiteInfos) throws JSONException {
        return SuiteResource.toJsonArray(suiteInfos);
    }

    /**
     * For the modules and pipelines panel, create a json representation for a task.
     * <pre>
     * {
        "lsid": "the full lsid of the module should be here",
        "name": "TheModuleNameGoesHere",
        "description": "The description of the module should go here",
        "version": "14.1.2",
        "documentation": "http://www.google.com",
        "categories": ["yyy", "zzz", "www"],
        "tags": ["xxx", "xxx"]
      }
     * </pre>
     * @param taskInfo
     * @return
     */
    private JSONObject getTaskSearchEntryJson(final HttpServletRequest request, final TaskInfo taskInfo, final Collection<String> categories, final Collection<String> suiteNames, GpContext userContext) throws JSONException {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("lsid", taskInfo.getLsid());
        jsonObj.put("name", taskInfo.getName());
        jsonObj.put("description", taskInfo.getDescription());
        try {
            final LSID lsid=new LSID(taskInfo.getLsid());
            jsonObj.put("version", lsid.getVersion());
        }
        catch (MalformedURLException e) {
            log.error("Error getting lsid for task.name="+taskInfo.getName(), e);
        }
        jsonObj.put("documentation", getDocLink(request, taskInfo));
        JSONArray categoriesJson=new JSONArray();
        for(final String cat : categories) {
            categoriesJson.put(cat);
        }
        jsonObj.put("categories", categoriesJson);
        JSONArray suitesJson=new JSONArray();
        for (final String suiteName : suiteNames) {
            suitesJson.put(suiteName);
        }
        jsonObj.put("suites", suitesJson);
        jsonObj.put("tags", getTags(taskInfo, userContext));
        return jsonObj;
    }

    private JSONArray getTags(final TaskInfo taskInfo, GpContext userContext) {
        Set<Tag> tags = TagManager.instance().getTags(userContext, taskInfo);
        
        JSONArray array = new JSONArray();
        for (Tag tag : tags) {
            try {
                array.put(tag.toJSON());
            }
            catch (JSONException e) {
                log.error("Error adding tag to array: " + tag);
            }
        }
        
        return array;
    }

    private static String getDocLink(final HttpServletRequest request, final TaskInfo taskInfo) {
        String cp=request.getContextPath();
        List<String> docs = TaskInfoCache.instance().getDocFilenames(taskInfo.getID(), taskInfo.getLsid());
        if (docs.size() > 0) {
            try {
                String docLink=cp+"/getTaskDoc.jsp?name=" + URLEncoder.encode(taskInfo.getLsid(), "UTF-8");
                return docLink;
            }
            catch (UnsupportedEncodingException e) {
                log.error("Error encoding lsid: " + taskInfo.getLsid());
                String docLink=cp+"/getTaskDoc.jsp?name=" + taskInfo.getLsid();
                return docLink;
            }
        }
        else {
            return "";
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{taskNameOrLsid}")
    public Response getTask(
            final @Context UriInfo uriInfo,
            final @PathParam("taskNameOrLsid") String taskNameOrLsid,
            final @Context HttpServletRequest request,
            @DefaultValue("true") @QueryParam("includeProperties") boolean includeProperties,
            @DefaultValue("true") @QueryParam("includeChildren") boolean includeChildren
            ) {
        GpContext userContext=Util.getUserContext(request);
        final String userId=userContext.getUserId();
        TaskInfo taskInfo = null;
        try {
            taskInfo=getTaskInfo(taskNameOrLsid, userId);
        }
        catch (Throwable t) {
            return Responses.notFound().entity(t.getLocalizedMessage()).build();
        }
        if(taskInfo == null) {
            String errorMessage="No task with task id: " + taskNameOrLsid + " found " + "for user " + userId;
            return Responses.notFound().entity(errorMessage).build();
        }
        
        //form a JSON response, from the given taskInfo
        String jsonStr="";
        try {
            JSONObject jsonObj = createTaskObject(taskInfo, request, includeProperties, includeChildren);

            final boolean prettyPrint=true;
            if (prettyPrint) {
                final int indentFactor=2;
                jsonStr=jsonObj.toString(indentFactor);
            }
            else {
                jsonStr=jsonObj.toString();
            }
        }
        catch (Throwable t) {
            final String errorMessage="Error constructing json response for task="+taskNameOrLsid+": "+
                    t.getLocalizedMessage();
            return Response.serverError().entity(errorMessage).build();
        }
        return Response.ok().entity(jsonStr).build();        
    }

    public static JSONObject createTaskNotFoundObject(JobSubmission js) throws JSONException {
        JSONObject toReturn = new JSONObject();
        toReturn.put("NOT_FOUND", true);
        toReturn.put("name", js.getName());
        toReturn.put("lsid", js.getLSID());
        try {
            LSID lsid = new LSID(js.getLSID());
            toReturn.put("version", lsid.getVersion());
        }
        catch (MalformedURLException e) {
            log.error("Error getting lsid for task.name=" + js.getName(), e);
        }

        return toReturn;
    }

    public static JSONObject createTaskObject(TaskInfo taskInfo, HttpServletRequest request, boolean includeProperties, boolean includeChildren) throws Exception {
        JSONObject jsonObj=new JSONObject();
        String href=getTaskInfoPath(request, taskInfo);
        jsonObj.put("href", href);
        jsonObj.put("name", taskInfo.getName());
        jsonObj.put("description", taskInfo.getDescription());

        if (includeProperties) {
            TaskInfoAttributes tia = taskInfo.getTaskInfoAttributes();

            // Author
            jsonObj.put("author", tia.get(GPConstants.AUTHOR));
            // Privacy
            jsonObj.put("privacy", tia.get(GPConstants.PRIVACY));
            // Quality level
            jsonObj.put("quality", tia.get(GPConstants.QUALITY));
            // Command line
            jsonObj.put("command_line", tia.get(GPConstants.COMMAND_LINE));
            // Categories
            jsonObj.put("categories", tia.get(GPConstants.TASK_TYPE));
            // CPU type
            jsonObj.put("cpu", tia.get(GPConstants.CPU_TYPE));
            // OS
            jsonObj.put("os", tia.get(GPConstants.OS));
            // Language
            jsonObj.put("language", tia.get(GPConstants.LANGUAGE));
            // Version comment
            jsonObj.put("version_comment", tia.get(GPConstants.VERSION));
            // File formats
            jsonObj.put("file_formats", tia.get(GPConstants.FILE_FORMAT));
        }

        if (includeChildren) {
            TaskInfoAttributes tia = taskInfo.getTaskInfoAttributes();
            String serializedModel = tia.get(GPConstants.SERIALIZED_MODEL);
            if (serializedModel != null && serializedModel.length() > 0) {
                PipelineModel model = PipelineModel.toPipelineModel(serializedModel);

                JSONArray children = new JSONArray();
                for (JobSubmission js : model.getTasks()) {
                    try {
                        TaskInfo childTask = TaskInfoCache.instance().getTask(js.getLSID());
                        JSONObject childObject = createTaskObject(childTask, request, includeProperties, includeChildren);
                        applyJobSubmission(childObject, js);
                        children.put(childObject);
                    }
                    catch (TaskLSIDNotFoundException e) {
                        // Task is not installed
                        JSONObject childObject = createTaskNotFoundObject(js);
                        children.put(childObject);
                    }
                }

                jsonObj.put("children", children);
            }
        }

        try {
            final LSID lsid=new LSID(taskInfo.getLsid());
            jsonObj.put("version", lsid.getVersion());
        }
        catch (MalformedURLException e) {
            log.error("Error getting lsid for task.name="+taskInfo.getName(), e);
        }
        jsonObj.put("documentation", getDocLink(request, taskInfo));
        jsonObj.put("lsid", taskInfo.getLsid());
        JSONArray paramsJson=new JSONArray();
        for(ParameterInfo pinfo : taskInfo.getParameterInfoArray()) {
            final JSONObject attributesJson = new JSONObject();
            for(final Object key : pinfo.getAttributes().keySet()) {
                final Object value = pinfo.getAttributes().get(key);
                if (value != null) {
                    attributesJson.put(key.toString(), value.toString());
                }
            }
            final JSONObject attrObj = new JSONObject();
            attrObj.put("attributes", attributesJson);
            attrObj.put("description", pinfo.getDescription());

            if (pinfo.getChoices() != null && pinfo.getChoices().size() > 0) {
                ChoiceInfo choices = ChoiceInfoHelper.initChoiceInfo(pinfo);
                attrObj.put("choiceInfo", ChoiceInfoHelper.initChoiceInfoJson(request, taskInfo, choices));
            }

            final JSONObject paramJson = new JSONObject();
            paramJson.put(pinfo.getName(), attrObj);
            paramsJson.put(paramJson);
        }
        jsonObj.put("params", paramsJson);

        return jsonObj;
    }

    public static void applyJobSubmission(JSONObject taskObject, JobSubmission js) throws JSONException {
        JSONArray params = taskObject.getJSONArray("params");
        boolean[] pwrs = js.getRuntimePrompt();
        Vector pias = js.getParameters();

        // For every parameter
        for (int i = 0; i < params.length(); i++) {
            // Find the correct value
            String value = null;
            if (pwrs[i] == true) {
                value = "Prompt When Run";
            }
            else {
                ParameterInfo pInfo = (ParameterInfo) pias.get(i);
                value = pInfo.getValue();
            }

            // Get the value object
            JSONObject paramObj = params.getJSONObject(i);
            String key = (String) paramObj.keys().next();
            JSONObject valObject = paramObj.getJSONObject(key);

            // Attach the value
            valObject.put("value", value);
        }
    }

    /**
     * Get the JSON representation of the choices for a given module input parameter.
     *     GET /rest/v1/tasks/{taskNameOrLsid}/{parameterName}/choiceInfo.json
     *     GET /rest/v1/tasks/DemoRNASeQC/annotation.gtf/choiceInfo.json
     *     curl -u test:test http://127.0.0.1:8080/gp/rest/v1/tasks/DemoRNASeQC/annotation.gtf/choiceInfo.json
     * 
     * Example response for a dynamic drop-down,
     * <pre>
       200 OK
       {
         "href":"http://127.0.0.1:8080/gp/rest/v1/tasks/DemoRNASeQC/annotation.gtf/choiceInfo.json",
         "status":{"flag":"OK", "message": "A user message"},
         "choiceDir":"ftp://ftp.broadinstitute.org/pub/genepattern/rna_seq/referenceAnnotation/gtf",
         "choiceAllowCustomValue":"true", 
         "selectedValue": "ftp://ftp.broadinstitute.org/pub/genepattern/rna_seq/referenceAnnotation/gtf/Arabidopsis_thaliana_Ensembl_TAIR10.gtf",
         "choices": [
           {"value":"ftp://ftp.broadinstitute.org/pub/genepattern/rna_seq/referenceAnnotation/gtf/Arabidopsis_thaliana_Ensembl_TAIR10.gtf","label":"Arabidopsis_thaliana_Ensembl_TAIR10.gtf"},
           {"value":"ftp://ftp.broadinstitute.org/pub/genepattern/rna_seq/referenceAnnotation/gtf/Arabidopsis_thaliana_Ensembl_TAIR9.gtf","label":"Arabidopsis_thaliana_Ensembl_TAIR9.gtf"},
           ...
           {"value": "", label: "" }
           ]
       }
     * </pre>
     * 
     * For a static drop-down, the 'choiceDir' will not be set.
     * 
     * Example status messages,
     *     OK, Initialized from values param (old way)
     *     OK, Initialized from choices param (new way, not dynamic)
     *     OK, Initialized from remote server (url=, date=)
     *     WARN, Initialized from cache, problem connecting to remote server
     *     ERROR, Error in module manifest, didn't initialize choices.
     *     ERROR, Connection error to remote server (url)
     *     NOT_INITIALIZED, the dynamic drop-down was not initialized from the remote server
     * 
     * @param uriInfo
     * @param taskNameOrLsid
     * @param pname
     * @param request
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{taskNameOrLsid}/{pname}/choiceInfo.json")
    public Response getChoiceInfo(
            final @Context UriInfo uriInfo,
            final @PathParam("taskNameOrLsid") String taskNameOrLsid,
            final @PathParam("pname") String pname,
            final @Context HttpServletRequest request
    ) {
        log.debug("taskNameOrLsid="+taskNameOrLsid);
        log.debug("pname="+pname);
        
        final GpContext taskContext;
        try {
            taskContext=Util.getTaskContext(request, taskNameOrLsid);
        }
        catch (Throwable t) {
            log.debug(t);
            return Responses.notFound().entity("Server error initializing taskContext for "+taskNameOrLsid).build();
        }
        if (taskContext.getTaskInfo()==null) {
            String errorMessage="No task with task id: " + taskNameOrLsid + " found " + "for user " + taskContext.getUserId();
            log.debug(errorMessage);
            return Responses.notFound().entity(errorMessage).build();
        }
        
        final Map<String,ParameterInfoRecord> paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskContext.getTaskInfo());
        if (!paramInfoMap.containsKey(pname)) {
            String errorMessage="No parameter with name="+pname;
            log.debug(errorMessage);
            return Responses.notFound().entity(errorMessage).build();
        }
        
        ParameterInfoRecord pinfoRecord=paramInfoMap.get(pname);
        if (!ChoiceInfo.hasChoiceInfo(pinfoRecord.getFormal())) {
            String errorMessage=taskContext.getTaskInfo().getName()+"."+pname + " does not have a choiceInfo";
            log.debug(errorMessage);
            return Responses.notFound().entity(errorMessage).build();
        }
        
        ChoiceInfoParser parser=ChoiceInfo.getChoiceInfoParser(taskContext);
        ChoiceInfo choiceInfo=parser.initChoiceInfo(pinfoRecord.getFormal());
        
        try {
            final JSONObject choiceInfoJson=ChoiceInfoHelper.initChoiceInfoJson(request, taskContext.getTaskInfo(), choiceInfo);            
            final String choiceInfoStr=choiceInfoJson.toString();

            //return the JSON representation of the job
            return Response.ok()
                .entity(choiceInfoStr)
                .build();
        }
        catch (Throwable t) {
            log.error("Unexpected server error in GET "+choiceInfo.getChoiceDir(), t);
            return Response.serverError().entity("Error serializing JSON response: "+t.getLocalizedMessage()).build();
        }
    }
    
    /**
     * GET the JSON representation for the list of one or more End-user license agreement(s) (EULA) for the given module.
     * This is context dependent; the list of pendingEulas may differ for each current user.
     * A 'pending' eula is one for which the current user has not yet agreed. By default, only include pendingEulas.
     * 
     * Optional request parameters:
     * <table>
     * <tr><td>all</td><td>When present, include allEulas in the response.</td></tr>
     * <tr><td>pending</td><td>When present, include pendingEulas in the response.</td></tr>
     * </table>
     * Template query:
           curl -u <user>:<password> <GenePatternURL>rest/v1/tasks/<lsid>/eulaInfo.json
     * Example queries:
     * <pre>
       curl -u test:test "http://127.0.0.1:8080/gp/rest/v1/tasks/urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00311:0.2/eulaInfo.json?pending"
     * </pre>
     * 
     * 
     * Example JSON representation,
     * <pre>
{
    "currentTaskName":"demoLicensedModule",
    "currentLsid":"urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00311:0.2",
    "currentLsidVersion":"0.2",
    "pendingEulas":[    <----- there can be a list of 0 or more pending eulas, for example, a pipeline may require multiple licensees
        { "moduleName": "demoLicensedModule",
          "moduleLsid": "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00311:0.2",
          "moduleLsidVersion", "0.2",
          "content": "the full content of the license agreement, (may not be present, if there was an error).",
          "contentError": "error message, (will only be present if there was an error initializing the content)" 
        }
    ],
    # the acceptData, acceptUrl, and acceptType objects give you enough information to construct an ajax call to accept the license
    "acceptType":"GET",
    "acceptUrl":"http://127.0.0.1:8080/gp/eula",
    "acceptData": {  
        "lsid":"urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00311:0.2"}
}

     * </pre>

     * @param uriInfo
     * @param taskNameOrLsid
     * @param all
     * @param pending
     * @param request
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{taskNameOrLsid}/eulaInfo.json")
    public Response getEulaInfo(
            final @Context UriInfo uriInfo,
            final @PathParam("taskNameOrLsid") String taskNameOrLsid,
            final @QueryParam("all") String all,
            final @QueryParam("pending") String pending,
            final @Context HttpServletRequest request
    ) {
        log.debug("taskNameOrLsid="+taskNameOrLsid);
        log.debug("all="+all);
        log.debug("pending="+pending);
        
        GpContext userContext=Util.getUserContext(request);
        final String userId=userContext.getUserId();
        TaskInfo taskInfo = null;
        try {
            taskInfo=getTaskInfo(taskNameOrLsid, userId);
        }
        catch (Throwable t) {
            return Responses.notFound().entity(t.getLocalizedMessage()).build();
        }
        if(taskInfo == null) {
            String errorMessage="No task with task id: " + taskNameOrLsid + " found " + "for user " + userId;
            return Responses.notFound().entity(errorMessage).build();
        }
        
        try {
            boolean includeAll= all != null;
            boolean includePending= !includeAll || pending != null;
            final JSONObject eulaInfoJson=TasksResource.getEulaForModuleJson(request, userContext, taskInfo, includePending, includeAll);
            final String eulaInfoStr=eulaInfoJson.toString();

            //return the JSON representation
            return Response.ok()
                .entity(eulaInfoStr)
                .build();
        }
        catch (Throwable t) {
            return Response.serverError().entity("Error serializing JSON response: "+t.getLocalizedMessage()).build();
        }
    }

    private TaskInfo getTaskInfo(final String taskLSID, final String username) 
    throws WebServiceException 
    {
        return new LocalAdminClient(username).getTask(taskLSID);
    }
}
