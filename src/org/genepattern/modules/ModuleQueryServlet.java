/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2012) by the
 Broad Institute. All rights are reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. The Broad Institute cannot be responsible for its
 use, misuse, or functionality.
*/

package org.genepattern.modules;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.methods.MultipartPostMethod;
import org.apache.commons.io.FileUtils;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.log4j.Logger;
import org.genepattern.server.TaskLSIDNotFoundException;
import org.genepattern.server.cm.CategoryUtil;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.eula.EulaInfo;
import org.genepattern.server.eula.EulaManager;
import org.genepattern.server.eula.GetEulaAsManifestProperty;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.job.input.GroupInfo;
import org.genepattern.server.job.input.LoadModuleHelper;
import org.genepattern.server.job.input.NumValues;
import org.genepattern.server.process.ZipTask;
import org.genepattern.server.taskinstall.InstallInfo;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.Status;
import org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.util.LSIDUtil;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.TaskInfoCache;
import org.genepattern.webservice.WebServiceException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * based on PipelineQueryServer class in the org.genepattern.pipelines class
 */
public class ModuleQueryServlet extends HttpServlet {
    private static final long serialVersionUID = 4631280565736438091L;
    public static Logger log = Logger.getLogger(ModuleQueryServlet.class);

    public static final String MODULE_CATEGORIES = "/categories";
    public static final String OUTPUT_FILE_FORMATS = "/fileformats";
    public static final String UPLOAD = "/upload";
    public static final String SAVE = "/save";
    public static final String LOAD = "/load";
    public static final String GPARC = "/gparc";


    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String action = request.getPathInfo();

        // Route to the appropriate action, returning an error if unknown
        if (MODULE_CATEGORIES.equals(action)) {
            getModuleCategories(request, response);
        }
        if (OUTPUT_FILE_FORMATS.equals(action)) {
            getOutputFileFormats(response);
        } else if (LOAD.equals(action)) {
            loadModule(request, response);
        } else if (SAVE.equals(action)) {
            saveModule(request, response);
        } else if (UPLOAD.equals(action)) {
            uploadFile(request, response);
        } else if (GPARC.equals(action)) {
            gparcSubmit(request, response);
        } else {
            sendError(response, "Routing error for " + action);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        doGet(request, response);
    }

    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response) {
        doGet(request, response);
    }

    private void write(HttpServletResponse response, Object content) {
        this.write(response, content.toString());
    }

    private void write(HttpServletResponse response, String content) {
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.println(content);
            writer.flush();
        } catch (IOException e) {
            log.error("Error writing to the response in ModuleQueryServlet: " + content);
            e.printStackTrace();
        } finally {
            if (writer != null) writer.close();
        }
    }

    public void sendError(HttpServletResponse response, String message) {
        ResponseJSON error = new ResponseJSON();
        error.addError("ERROR: " + message);
        this.write(response, error);
    }

    private File getZipFile(HttpServletRequest request) throws Exception {
        String lsid = request.getParameter("lsid");
        TaskInfo taskInfo = getTaskInfo(lsid);
        String username = (String) request.getSession().getAttribute("userid");
        ZipTask zipTask = new ZipTask();
        return zipTask.packageTask(taskInfo, username);
    }

    @SuppressWarnings("deprecation")
    public void gparcSubmit(HttpServletRequest request, HttpServletResponse response) {
        try {
            // Set up the client
            HttpClient client = new HttpClient();
            client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

            // Set up the file post
            File zipfile = getZipFile(request);

            // Get the URL to post to
            GpContext context = GpContext.getContextForUser((String) request.getSession().getAttribute("userid"));
            String gparcUploadURL = ServerConfigurationFactory.instance().getGPProperty(context, "gparcUploadURL", "http://www.gparc.org/server_upload.php");

            // Set up the post method
            MultipartPostMethod post = new MultipartPostMethod(gparcUploadURL);
            post.addRequestHeader("Content-type", "multipart/form-data");
            post.addParameter("zipfilename", zipfile.getName(), zipfile);

            // Execute
            int status = client.executeMethod(post);

            // Get the token from the response
            if (status == 200) {
                String tokenJSON = post.getResponseBodyAsString();
                JSONObject tokenObject = new JSONObject(tokenJSON);
                String token = tokenObject.getString("token");

                String tokenURL = null;
                if (token != null) {
                    String gparcSubmitURL = ServerConfigurationFactory.instance().getGPProperty(context, "gparcSubmitURL", "http://www.gparc.org/uniqid");
                    tokenURL = gparcSubmitURL + "?uniqid=" + token;
                    tokenObject.put("token", tokenURL);
                } else {
                    tokenURL = "{'error': 'ERROR: No token sent, " + tokenObject.getString("error") + "'}";
                }

                // Write the token back to the UI
                this.write(response, tokenObject);
            } else {
                this.write(response, "{'error': 'ERROR: Unknown response code " + status + "'}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error preparing submit to GParc: " + e.getMessage());
            this.write(response, "ERROR: " + e.getMessage());
        }
    }

    public SortedSet<String> getAllCategories(GpContext userContext) {
        SortedSet<String> categories = new TreeSet<String>(new Comparator<String>() {
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

        for (TaskInfo ti : TaskInfoCache.instance().getAllTasks()) {

            final CategoryUtil cu = new CategoryUtil();
            final List<String> taskTypes = cu.getCategoriesForTask(userContext, ti);
            categories.addAll(taskTypes);
        }
        return Collections.unmodifiableSortedSet(categories);
    }

    public void getModuleCategories(HttpServletRequest request, HttpServletResponse response) {
        String username = (String) request.getSession().getAttribute("userid");
        if (username == null) {
            sendError(response, "No GenePattern session found.  Please log in.");
            return;
        }
        GpContext userContext = GpContext.getContextForUser(username);

        SortedSet<String> categories = null;
        try {
            categories = getAllCategories(userContext);
        } catch (Throwable t) {
            t.printStackTrace();
            log.error("Error listing categories from TaskInfoCache: " + t.getLocalizedMessage());
        }

        ResponseJSON message = new ResponseJSON();
        if (categories != null && categories.size() > 0) {
            message.addChild("categories", categories.toString());
        } else {
            message.addChild("categories", "");
        }
        this.write(response, message);
    }

    public SortedSet<String> getFileFormats() {
        SortedSet<String> fileFormats = new TreeSet<String>(new Comparator<String>() {
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

        for (TaskInfo ti : TaskInfoCache.instance().getAllTasks()) {
            String fileFormat = ti.getTaskInfoAttributes().get(GPConstants.FILE_FORMAT);
            if (fileFormat == null || fileFormat.trim().length() == 0) {
                //ignore null and blank
            } else {
                if (fileFormat.indexOf(";") != -1) {
                    String[] result = fileFormat.split(";");
                    for (String f : result) {
                        f = f.trim();
                        if (!f.equals("") && !f.equals(" ") && !fileFormats.contains(f)) {
                            fileFormats.add(f);
                        }
                    }
                } else {
                    fileFormat = fileFormat.trim();
                    if (!fileFormat.equals("") && !fileFormat.equals(" ") && !fileFormats.contains(fileFormat)) {
                        fileFormats.add(fileFormat);
                    }
                }

            }

            ParameterInfo[] pInfoArray = ti.getParameterInfoArray();
            if (pInfoArray == null) {
                continue;
            }
            for (ParameterInfo pi : pInfoArray) {
                String pFileFormat = (String) pi.getAttributes().get(GPConstants.FILE_FORMAT);

                if (!(pFileFormat == null || pFileFormat.trim().length() == 0)) {
                    if (pFileFormat.indexOf(";") != -1) {
                        String[] result = pFileFormat.split(";");
                        for (String f : result) {
                            f = f.trim();
                            if (!f.equals("") && !f.equals(" ") && !fileFormats.contains(f)) {
                                fileFormats.add(f);
                            }
                        }
                    } else {
                        pFileFormat = pFileFormat.trim();
                        if (!pFileFormat.equals("") && !pFileFormat.equals(" ") && !fileFormats.contains(pFileFormat)) {
                            fileFormats.add(pFileFormat);
                        }
                    }
                }
            }
        }
        return Collections.unmodifiableSortedSet(fileFormats);
    }

    public void getOutputFileFormats(HttpServletResponse response) {
        SortedSet<String> fileFormats = null;
        try {
            fileFormats = getFileFormats();
        } catch (Throwable t) {
            t.printStackTrace();
            log.error("Error listing categories from TaskInfoCache: " + t.getLocalizedMessage());
        }

        ResponseJSON message = new ResponseJSON();

        if (fileFormats != null) {
            message.addChild("fileformats", new JSONArray(fileFormats));
        } else {
            message.addChild("fileformats", "");
        }
        this.write(response, message);
    }

    public void uploadFile(HttpServletRequest request, HttpServletResponse response) {
        String username = (String) request.getSession().getAttribute("userid");
        if (username == null) {
            sendError(response, "No GenePattern session found.  Please log in.");
            return;
        }
        GpContext userContext = GpContext.getContextForUser(username);

        RequestContext reqContext = new ServletRequestContext(request);
        if (FileUploadBase.isMultipartContent(reqContext)) {
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            try {
                List<FileItem> postParameters = upload.parseRequest(reqContext);

                for (FileItem i : postParameters) {
                    // Only read the submitted files
                    if (!i.isFormField()) {
                        // Store in a temp directory until the module is saved

                        File fileTempDir = ServerConfigurationFactory.instance().getTemporaryUploadDir(userContext);
                        File uploadedFile = new File(fileTempDir, i.getName());

                        transferUpload(i, uploadedFile);

                        // Return a success response
                        ResponseJSON message = new ResponseJSON();
                        message.addChild("location", uploadedFile.getCanonicalPath());
                        this.write(response, message);
                    } else {
                        ResponseJSON message = new ResponseJSON();
                        message.addChild("formfield", "false");
                        this.write(response, message);
                    }
                }
            } catch (Exception e) {
                log.error("error", e);
                String message = "";
                if (e.getMessage() != null) {
                    message = e.getMessage();
                }
                sendError(response, "Exception retrieving the uploaded file: " + message);
            }
        } else {
            sendError(response, "Unable to find uploaded file");
        }
    }

    private void addLicensePlugin(TaskInfoAttributes tia) {
        String patchLSID = "urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:GenePattern_3_4_2:2";
        String patchURL = "http://www.broad.mit.edu/webservices/gpModuleRepository/download/prod/patch/?file=/GenePattern_3_4_2/broad.mit.edu:cancer.software.genepattern.server.patch/GenePattern_3_4_2/2/GenePattern_3_4_2.zip";

        //add plugin to check the GP version is >= 3.4.2

        //check if there are other plugins defined
        if (tia.get("requiredPatchLSIDs") != null
                && !tia.get("requiredPatchLSIDs").equals("")
                && tia.get("requiredPatchURLs") != null
                && !tia.get("requiredPatchURLs").equals("")) {
            if (!tia.get("requiredPatchLSIDs").contains(patchLSID)) {
                tia.put("requiredPatchLSIDs", patchLSID + "," + tia.get("requiredPatchLSIDs"));
            }
            if (!tia.get("requiredPatchURLs").contains(patchURL)) {
                tia.put("requiredPatchURLs", patchURL + "," + tia.get("requiredPatchURLs"));
            }
        } else {
            tia.put("requiredPatchLSIDs", patchLSID);
            tia.put("requiredPatchURLs", patchURL);
        }
    }

    private void removeLicensePlugin(TaskInfoAttributes tia) {
        String patchLSID = "urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:GenePattern_3_4_2:2";
        String patchURL = "http://www.broad.mit.edu/webservices/gpModuleRepository/download/prod/patch/?file=/GenePattern_3_4_2/broad.mit.edu:cancer.software.genepattern.server.patch/GenePattern_3_4_2/2/GenePattern_3_4_2.zip";

        //remove plugin to check the GP version is >= 3.4.2

        if (tia.get("requiredPatchLSIDs") != null
                && !tia.get("requiredPatchLSIDs").equals("")
                && tia.get("requiredPatchURLs") != null
                && !tia.get("requiredPatchURLs").equals("")) {
            if (tia.get("requiredPatchLSIDs").contains(patchLSID)) {
                //handle cases where there is one plugin or multiple plugins
                tia.put("requiredPatchLSIDs", tia.get("requiredPatchLSIDs").replace(patchLSID + ",", ""));
                tia.put("requiredPatchLSIDs", tia.get("requiredPatchLSIDs").replace("," + patchLSID, ""));
                tia.put("requiredPatchLSIDs", tia.get("requiredPatchLSIDs").replace(patchLSID, ""));
            }

            if (tia.get("requiredPatchURLs").contains(patchURL)) {
                //handle cases where there is one plugin or multiple plugins
                tia.put("requiredPatchURLs", tia.get("requiredPatchURLs").replace(patchURL + ",", ""));
                tia.put("requiredPatchURLs", tia.get("requiredPatchURLs").replace("," + patchURL, ""));
                tia.put("requiredPatchURLs", tia.get("requiredPatchURLs").replace(patchURL, ""));
            }
        }
    }

    public void saveModule(HttpServletRequest request, HttpServletResponse response) {
        String username = (String) request.getSession().getAttribute("userid");
        GpContext userContext = GpContext.getContextForUser(username);

        if (username == null) {
            sendError(response, "No GenePattern session found.  Please log in.");
            return;
        }

        String bundle = request.getParameter("bundle");
        if (bundle == null) {
            log.error("Unable to retrieve the saved module");
            sendError(response, "Unable to save the module");
            return;
        }

        try {
            JSONObject moduleJSON = ModuleJSON.parseBundle(bundle);
            ModuleJSON moduleObject = ModuleJSON.extract(moduleJSON);

            String name = moduleObject.getName();
            String description = moduleObject.getDescription();

            TaskInfoAttributes tia = new TaskInfoAttributes();
            tia.put(GPConstants.USERID, username);

            Iterator<String> infoKeys = moduleObject.keys();
            while (infoKeys.hasNext()) {
                String key = infoKeys.next();

                //omit module name, description, license, and support files from taskinfoattributes
                if (!key.equals(ModuleJSON.NAME) && !key.equals(ModuleJSON.DESCRIPTION)
                        && !key.equals(ModuleJSON.SUPPORTFILES) && !key.equals(ModuleJSON.FILESTODELETE)
                        && !key.equals(ModuleJSON.FILEFORMAT) && !key.equals(ModuleJSON.CATEGORIES)) {
                    tia.put(key, moduleObject.get(key));
                }
            }

            tia.put(GPConstants.FILE_FORMAT, moduleObject.getFileFormats());

            if (moduleObject.has(GPConstants.CATEGORIES)) {
                tia.put(GPConstants.CATEGORIES, moduleObject.getCategories());
            }

            //parse out privacy info
            int privacy = GPConstants.ACCESS_PRIVATE;
            if (moduleObject.get(ModuleJSON.PRIVACY) != null
                    && !((String) moduleObject.get(ModuleJSON.PRIVACY)).equalsIgnoreCase("private")) {
                privacy = GPConstants.ACCESS_PUBLIC;
            }

            //handle license file(s)
            //TODO: refactor into the EulaManager 
            if (moduleObject.get(ModuleJSON.LICENSE) == null
                    || moduleObject.get(ModuleJSON.LICENSE).equals("")) {
                tia.remove(GetEulaAsManifestProperty.LICENSE);
                removeLicensePlugin(tia);
            } else {
                tia.put(GetEulaAsManifestProperty.LICENSE, moduleObject.get(ModuleJSON.LICENSE));
                addLicensePlugin(tia);
            }

            //--------------------------Parameter Information---------------------------------------------
            ParametersJSON[] parameters = ParametersJSON.extract(moduleJSON);
            ParameterInfo[] pInfo = new ParameterInfo[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                ParametersJSON parameterJSON = parameters[i];
                ParameterInfo parameter = new ParameterInfo();
                String pName = parameterJSON.getName();
                if (pName != null && pName.length() > 0) {
                    if (Character.isDigit(pName.charAt(0))) {
                        sendError(response, "Parameter names cannot start with an integer: " + pName);
                    }
                }
                parameter.setName(parameterJSON.getName());
                parameter.setDescription(parameterJSON.getDescription());

                HashMap attributes = new HashMap();
                attributes.put(GPConstants.PARAM_INFO_DEFAULT_VALUE[0], parameterJSON.getDefaultValue());


                if (parameterJSON.getType().equalsIgnoreCase("file")) {
                    attributes.put(GPConstants.PARAM_INFO_TYPE[0], GPConstants.PARAM_INFO_TYPE_INPUT_FILE);
                    attributes.put(ParameterInfo.TYPE, ParameterInfo.FILE_TYPE);
                    attributes.put(ParameterInfo.MODE, ParameterInfo.INPUT_MODE);

                    if (parameterJSON.getFileFormats() != null) {
                        attributes.put(GPConstants.FILE_FORMAT, parameterJSON.getFileFormats());
                    }
                } else if (parameterJSON.getType().equalsIgnoreCase("integer")) {
                    attributes.put(GPConstants.PARAM_INFO_TYPE[0], GPConstants.PARAM_INFO_TYPE_INTEGER);
                } else if (parameterJSON.getType().equalsIgnoreCase("floating point")) {
                    attributes.put(GPConstants.PARAM_INFO_TYPE[0], GPConstants.PARAM_INFO_TYPE_FLOAT);
                } else if (parameterJSON.getType().equalsIgnoreCase("password")) {
                    attributes.put(GPConstants.PARAM_INFO_TYPE[0], GPConstants.PARAM_INFO_PASSWORD);
                } else if (parameterJSON.getType().equalsIgnoreCase("directory")) {
                    attributes.put(GPConstants.PARAM_INFO_TYPE[0], GPConstants.PARAM_INFO_TYPE_DIR);
                } else {
                    //then this must be a text input
                    attributes.put(GPConstants.PARAM_INFO_TYPE[0], GPConstants.PARAM_INFO_TYPE_TEXT);
                }

                if (parameterJSON.isOptional()) {
                    attributes.put(GPConstants.PARAM_INFO_OPTIONAL[0], "on");
                } else {
                    attributes.put(GPConstants.PARAM_INFO_OPTIONAL[0], "");
                }

                attributes.put(GPConstants.PARAM_INFO_PREFIX[0], parameterJSON.getPrefix());
                String defaultValue = parameterJSON.getDefaultValue();
                attributes.put(GPConstants.PARAM_INFO_DEFAULT_VALUE[0], defaultValue);

                attributes.put(ParametersJSON.FLAG, parameterJSON.get(ParametersJSON.FLAG));

                attributes.put(ParametersJSON.VALUE, parameterJSON.getValue());
                parameter.setValue(parameterJSON.getValue());
                Iterator<String> paramKeys = parameterJSON.keys();
                while (paramKeys.hasNext()) {
                    String key = paramKeys.next();

                    //add remaining parameter attributes
                    if (!attributes.containsKey(key) && !key.equals(ParametersJSON.MIN_NUM_VALUE)
                            && !key.equals(ParametersJSON.MAX_NUM_VALUE)
                            && !key.equals(ParametersJSON.MIN_NUM_GROUPS)
                            && !key.equals(ParametersJSON.MAX_NUM_GROUPS)) {
                        attributes.put(key, parameterJSON.get(key));
                    }
                }

                //add the number of files
                if (parameterJSON.getMinNumValue() != -1) {
                    String numValuesString = String.valueOf(parameterJSON.getMinNumValue());

                    if (parameterJSON.getMaxNumValue() != -1) {
                        numValuesString += ".." + String.valueOf(parameterJSON.getMaxNumValue());
                    } else {
                        numValuesString += "+";
                    }
                    attributes.put(NumValues.PROP_NUM_VALUES, numValuesString);
                }

                //add the number of groups
                if (parameterJSON.getMinGroups() != 0) {
                    String numGroupsString = String.valueOf(parameterJSON.getMinGroups());

                    if (parameterJSON.getMaxGroups() > parameterJSON.getMinGroups() && parameterJSON.getMaxGroups() != -1) {
                        numGroupsString += ".." + String.valueOf(parameterJSON.getMaxGroups());
                    } else {
                        numGroupsString += "+";
                    }
                    attributes.put(GroupInfo.PROP_NUM_GROUPS, numGroupsString);
                }

                parameter.setAttributes(attributes);
                pInfo[i] = parameter;
            }

            //check that the paramgroups.json file, which defines advanced parameters,
            //if provided is valid
            String[] moduleSupportFiles = moduleObject.getSupportFiles();
            for (String filePath : moduleSupportFiles) {
                File file = new File(filePath);
                if(file.getName().equals("paramgroups.json")) {
                    final LoadModuleHelper loadModuleHelper = new LoadModuleHelper(userContext);

                    try {
                        JSONArray paramGroupsJson = loadModuleHelper.getParameterGroupsJson(pInfo, file);
                    } catch (Exception e) {
                        log.error("Error while validating " + file.getName() + " file . " + e.getMessage());
                        throw new Exception("Error while validating " + file.getName() + " file. " + e.getMessage());
                    }
                }
            }

            String newLsid = null;
            if (moduleObject.getLsid() == null || moduleObject.getLsid().equals("")) {
                newLsid = GenePatternAnalysisTask.installNewTask(name, description, pInfo, tia, username, privacy,
                        new Status() {
                            public void beginProgress(String string) {
                            }

                            public void continueProgress(int percent) {
                            }

                            public void endProgress() {
                            }

                            public void statusMessage(String message) {
                            }
                        },
                        new InstallInfo(InstallInfo.Type.CREATE));
            } else {
                //we are modifying a task
                LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(username);
                javax.activation.DataHandler[] supportFiles = null;
                String[] supportFileNames = null;
                try {
                    supportFiles = taskIntegratorClient.getSupportFiles(moduleObject.getLsid());
                    supportFileNames = taskIntegratorClient.getSupportFileNames(moduleObject.getLsid());
                } catch (WebServiceException wse) {
                }

                newLsid = taskIntegratorClient.modifyTask(privacy,
                        moduleObject.getName(),
                        moduleObject.getDescription(),
                        pInfo,
                        tia,
                        supportFiles,
                        supportFileNames);
            }
            //copy support files from temp to the module taskLib
            String[] filesToDelete = moduleObject.getRemovedFiles();

            String[] supportFiles = moduleObject.getSupportFiles();
            TaskInfo taskInfo = TaskInfoCache.instance().getTask(newLsid);

            String taskLibDir = DirectoryManager.getTaskLibDir(taskInfo.getName(), newLsid, username);

            if (taskLibDir != null) {
                deleteRemovedFiles(filesToDelete, new File(taskLibDir));
                moveSupportFiles(supportFiles, new File(taskLibDir));
            } else {
                sendError(response, "Unable to copy support files");
                return;
            }

            ResponseJSON message = new ResponseJSON();
            message.addMessage("Module Saved");
            message.addChild("lsid", newLsid);
            message.addChild("lsidVersions", new JSONArray(getModuleVersions(newLsid)));
            this.write(response, message);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e);

            String message = "";
            if (e.getMessage() != null) {
                message = e.getMessage();
            }
            sendError(response, "An error occurred while saving the module. " + message);
        }
    }

    private boolean validateAdvancedParametersFile(File file) {
        boolean result = false;

        try
        {
            String advancedParametersStr = FileUtils.readFileToString(file);
            JSONTokener tokener = new JSONTokener( advancedParametersStr);
            while(tokener.more())
            {
                Object value = tokener.nextValue();
            }

            //if you get here then the file was parsed successfully
            result = true;
        }
        catch(IOException e)
        {
            log.error("Could not find file: " + file.getAbsolutePath());
        }
        catch(JSONException je)
        {
            log.error(je);
        }

        return result;
    }

    private void deleteRemovedFiles(String[] files, File copyTo) throws Exception
    {
        if (copyTo == null || !copyTo.isDirectory()) {
            throw new Exception("Attempting to remove files from a location that is not a directory");
        }

        for (String path : files)
        {
            File file = new File(copyTo, path);
            if (!file.exists()) {
                throw new Exception("Attempting to delete a file that does not exist: " + path);
            }

            //Delete file from directory
            boolean success = file.delete();
            if (!success) {
                throw new Exception("Unable to delete file: " + file.getName());
            }
        }
    }

    private void moveSupportFiles(String[] files, File copyTo) throws Exception {
        if (copyTo == null || !copyTo.isDirectory()) {
            throw new Exception("Attempting to copy files to a location that is not a directory");
        }

        for (String path : files) {
            File file = new File(path);
            if (!file.exists()) {
                throw new Exception("Attempting to move a file that does not exist: " + path);
            }

            // Move file to new directory
            boolean success = file.renameTo(new File(copyTo, file.getName()));
            if (!success) {
                throw new Exception("Unable to move file: " + file.getName());
            }
        }
    }

    private void transferUpload(FileItem from, File to) throws IOException {
        InputStream is = null;
        OutputStream os = null;

        try {
            is = from.getInputStream();
            os = new BufferedOutputStream(new FileOutputStream(to, true));
            final int BUFSIZE = 2048;
            final byte buf[] = new byte[BUFSIZE];
            int n;
            while ((n = is.read(buf)) != -1) {
                os.write(buf, 0, n);
            }
        }
        finally {
            if(is != null)
            {
                is.close();
            }


            if(os != null)
            {
                os.close();
            }

        }
    }

    private JSONArray getParameterList(final TaskInfo taskInfo, ParameterInfo[] pArray)
    {
        JSONArray parametersObject = new JSONArray();

        for(int i =0;i < pArray.length;i++)
        {
            ParametersJSON parameter = new ParametersJSON(pArray[i]);
            parameter.addGroupInfo(pArray[i]);
            parametersObject.put(parameter);
        }

        return parametersObject;
    }

    public void loadModule(HttpServletRequest request, HttpServletResponse response)
    {
        String username = (String) request.getSession().getAttribute("userid");
	    if (username == null) {
	        sendError(response, "No GenePattern session found.  Please log in.");
	        return;
	    }
	    GpContext taskContext=GpContext.getContextForUser(username);

	    String lsid = request.getParameter("lsid");

	    if (lsid == null) {
	        sendError(response, "No lsid received");
	        return;
	    }

        try
        {
            TaskInfo taskInfo = getTaskInfo(lsid);
            taskContext.setTaskInfo(taskInfo);

            //check if user is allowed to edit the module
            boolean createModuleAllowed = AuthorizationHelper.createModule(username);
            boolean editable = createModuleAllowed && taskInfo.getUserId().equals(username)
                    && LSIDUtil.getInstance().isAuthorityMine(taskInfo.getLsid());

            if(!editable)
            {
                sendError(response, "Module is not editable");
                return;
            }

            ResponseJSON responseObject = new ResponseJSON();

            LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(username);
            File[] allFiles = taskIntegratorClient.getAllFiles(taskInfo);

            //Get documentation file name
            String taskDoc = (String)taskInfo.getAttributes().get(GPConstants.TASK_DOC);
            //Exclude license from list of support files
            String licenseFileName=null;
            List<EulaInfo> eulas=EulaManager.instance(taskContext).getEulas(taskInfo);
            if (eulas==null) {
                log.error("IEulaManager#getEulas==null");
            }
            else if (eulas.size()>1) {
                log.error("module has more than one eula, num eulas="+eulas.size());
            }
            if (eulas!=null && eulas.size()>0) {
                licenseFileName=eulas.get(0).getLicense();
            }
            if (licenseFileName != null || taskDoc != null) {
                ArrayList<File> supportFiles = new ArrayList<File>();
                for(File file : allFiles)
                {
                    if(!file.getName().equals(licenseFileName)
                            && !file.getName().equals(taskDoc))
                    {
                        supportFiles.add(file);
                    }
                }

                allFiles = supportFiles.toArray(new File[0]);
            }

            ModuleJSON moduleObject = new ModuleJSON(taskInfo, allFiles);
            moduleObject.put("lsidVersions", new JSONArray(getModuleVersions(lsid)));

            responseObject.addChild(ModuleJSON.KEY, moduleObject);

            JSONArray parametersObject = getParameterList(taskInfo, taskInfo.getParameterInfoArray());
            responseObject.addChild(ParametersJSON.KEY, parametersObject);

            //return the list of potential documentation files if the taskDoc property does not exist
            if(!moduleObject.has(GPConstants.TASK_DOC))
            {
                List <String> docFiles = TaskInfoCache.instance().getDocFilenames(taskInfo.getID(), taskInfo.getLsid());
                responseObject.addChild("docFileNames", new JSONArray(docFiles));
            }
            this.write(response, responseObject);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            log.error(e);

            String message = "";
            if(e.getMessage() != null)
            {
                message = e.getMessage();
            }
            sendError(response, "Error: while loading the module with lsid: " + lsid + " " + message);
        }
	}

    private TaskInfo getTaskInfo(String taskLSID) throws Exception
    {
        TaskInfo taskInfo = null;
        try
        {
            taskInfo = TaskInfoCache.instance().getTask(taskLSID);
            return taskInfo;
        }
        catch(TaskLSIDNotFoundException e)
        {
            // do nothing check with lsid
        }

        String taskNoLSIDVersion = new LSID(taskLSID).toStringNoVersion();
        SortedSet<String> moduleVersions = new TreeSet<String>(new Comparator<String>() {
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

        TaskInfo[] tasks = TaskInfoCache.instance().getAllTasks();
        for(int i=0;i<tasks.length;i++)
        {
            TaskInfoAttributes tia = tasks[i].giveTaskInfoAttributes();
            String lsidString = tia.get(GPConstants.LSID);
            LSID lsid = new LSID(lsidString);
            String lsidNoVersion = lsid.toStringNoVersion();
            if(taskNoLSIDVersion.equals(lsidNoVersion))
            {
                moduleVersions.add(lsidString);
            }
        }

        if(moduleVersions.size() > 0)
        {
            taskInfo = TaskInfoCache.instance().getTask(moduleVersions.first());
        }

        return taskInfo;
    }

    private ArrayList getModuleVersions(String taskLSID) throws Exception
    {
        String taskNoLSIDVersion = new LSID(taskLSID).toStringNoVersion();

        ArrayList moduleVersions = new ArrayList();
        TaskInfo[] tasks = TaskInfoCache.instance().getAllTasks();
        for(int i=0;i<tasks.length;i++)
        {
            TaskInfoAttributes tia = tasks[i].giveTaskInfoAttributes();
            String lsidString = tia.get(GPConstants.LSID);
            LSID lsid = new LSID(lsidString);
            String lsidNoVersion = lsid.toStringNoVersion();
            if(taskNoLSIDVersion.equals(lsidNoVersion))
            {
                moduleVersions.add(lsidString);
            }
        }

        return moduleVersions;
    }
}
