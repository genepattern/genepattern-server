package org.genepattern.server.webapp;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.log4j.Logger;
import org.genepattern.server.DataManager;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.dm.serverfile.ServerFileObjFactory;
import org.genepattern.server.domain.AnalysisJobDAO;
import org.genepattern.server.domain.BatchJob;
import org.genepattern.server.domain.BatchJobDAO;
import org.genepattern.server.executor.JobSubmissionException;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.handler.AddNewJobHandler;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

public class BatchSubmit {

    private static final long serialVersionUID = 4823931484381936462L;

    private static Logger log = Logger.getLogger(BatchSubmit.class);
    private String userName;
    private Map<String, String> formValues = new HashMap<String, String>();
    private Map<String, MultiFileParameter> multiFileValues = new HashMap<String, MultiFileParameter>();
    private boolean isBatch;
    private Integer id;
    private List<ParameterInfo> missingParameters = new ArrayList<ParameterInfo>();
    private static final String multiSuffix = "_batch";

    // Collect input parameters.
    // Find multi-file input file parameters
    // Validate that only one file parameter has multiple files
    // Todo in the future: Allow groups of matched files,
    // Create resolution screen for mismatches
    // Submit a job for each file.
    public BatchSubmit(HttpServletRequest request, List<FileItem> params) throws IOException, FileUploadException, WebServiceException {
        userName = (String) request.getSession().getAttribute(GPConstants.USERID);
        readFormValuesAndLoadAttachedFiles(request, params);
    }

    public List<ParameterInfo> getMissingParameters() {
        return missingParameters;
    }
    
    protected String getTaskLsid() throws UnsupportedEncodingException {
        String taskLsid = formValues.get("taskLSID");
        if (taskLsid == null) {
            // Try the task name instead
            taskLsid = formValues.get("taskName");
        }
        taskLsid = (taskLsid) != null ? URLDecoder.decode(taskLsid, "UTF-8") : null;
        return taskLsid;
    }

    public void submitJobs() throws WebServiceException, IOException {
        isBatch = false;

        // Look up the task name, stored in a hidden field
        String taskLsid = getTaskLsid();

        // And get all the parameters that need to be filled in for this task
        ParameterInfo parameterInfoArray[] = null;
        TaskInfo taskInfo;
        if (taskLsid != null) {
            taskInfo = new LocalAdminClient(userName).getTask(taskLsid);
            parameterInfoArray = taskInfo.getParameterInfoArray();
        }
        else {
            return;
        }

        // Now try and match the parameters to the form fields we've just read
        for (int i = 0; i < parameterInfoArray.length; i++) {
            ParameterInfo pinfo = parameterInfoArray[i];
            
            String value;

            value = formValues.get(pinfo.getName());
            if (value != null && value.length() > 0) {
                pinfo.setValue(value);
            }
            else {
                // Perhaps, this form value has been submitted as a url
                value = formValues.get(pinfo.getName() + "_url");
                if (value != null && value.length() > 0) {
                    pinfo.getAttributes().put(ParameterInfo.MODE, ParameterInfo.URL_INPUT_MODE);
                    pinfo.getAttributes().remove(ParameterInfo.TYPE);
                    pinfo.setValue(value);
                }
            }

            // Was this value required?
            if ((value == null) || (value.trim().length() == 0)) {
                // Is it going to be filled in by our multi file submit process
                if (multiFileValues.get(pinfo.getName()) == null) {
                    boolean isOptional = ((String) pinfo.getAttributes().get(GPConstants.PARAM_INFO_OPTIONAL[GPConstants.PARAM_INFO_NAME_OFFSET])).length() > 0;
                    if (!isOptional) {
                        missingParameters.add(pinfo);
                    }
                }
            }
        }
        
        if (batchParamEmpty()) {
            return;
        }

        if (missingParameters.size() > 0) {
            log.info("Missing required parameters");
            return;
        }
        
        // Now, submit the job if there's no multi-file field
        // Or, submit multiple jobs for each filename in the multi-file field
        if (multiFileValues.size() == 0) {
            JobInfo job = submitJob(taskInfo.getID(), parameterInfoArray);
            id = job.getJobNumber();
        }
        else {
            BatchJob batchJob = new BatchJob(userName);

            int numFiles = multiFileValues.values().iterator().next().getNumFiles();
            for (int i = 0; i < numFiles; i++) {
                for (String parameter : multiFileValues.keySet()) {
                    MultiFileParameter param = multiFileValues.get(parameter);
                    List<GpFilePath> files = param.getFiles();
                    if (files.size() <= i) {
                        log.error("Number of files in batch directory out of sync: " + files.size() + " : " + i);
                        break;
                    }
                    GpFilePath file = files.get(i);
                    boolean urlInput = param.isUrl();
                    String parameterValue = null;
                    
                    if (urlInput) { 
                        try {
                            parameterValue = file.getUrl().toString();
                        }
                        catch (Throwable t) {
                            log.error(t);
                        }
                    }
                    else {
                        parameterValue = file.getServerFile().getCanonicalPath();
                    }
                    
                    assignParameter(parameter, parameterValue, parameterInfoArray);
                }
                // The task runner can move files to the output directory.
                // So if we're using a single file input for multiple jobs,
                // we have to make copies of the file.
                for (int p = 0; p < parameterInfoArray.length; p++) {
                    ParameterInfo pi = parameterInfoArray[p];
                    if (pi.getValue() != null && pi.getValue().trim().length() != 0) {
                        // Make sure it's not a parameter we just set
                        if (!multiFileValues.containsKey(pi.getName())) {
                            String type = (String) pi.getAttributes().get(ParameterInfo.TYPE);
                            if (type != null && type.compareTo(ParameterInfo.FILE_TYPE) == 0) {
                                String mode = (String) pi.getAttributes().get(ParameterInfo.MODE);
                                if (mode != null && mode.compareTo(ParameterInfo.INPUT_MODE) == 0) {
                                    File source = new File(pi.getValue());
                                    if (!source.isDirectory()) {
                                        // It's an input file. Make a copy. .
                                        File fileTemp = new File(pi.getValue());

                                        File tempDir = File.createTempFile(userName + "_run", null);
                                        tempDir.delete();
                                        tempDir.mkdir();
                                        File file = new File(tempDir, fileTemp.getName());
                                        GenePatternAnalysisTask.copyFile(source, file);
                                        pi.setValue(file.getCanonicalPath());
                                    }
                                }
                            }
                        }
                    }
                }
                JobInfo job = submitJob(taskInfo.getID(), parameterInfoArray);
                batchJob.getBatchJobs().add(new AnalysisJobDAO().findById(job.getJobNumber()));
            }
            new BatchJobDAO().save(batchJob);

            isBatch = true;
            id = batchJob.getJobNo();
        }
    }

    protected JobInfo submitJob(int taskID, ParameterInfo[] parameters) throws WebServiceException {
        AddNewJobHandler req = new AddNewJobHandler(taskID, userName, parameters);
        try {
            JobInfo jobInfo = req.executeRequest();
            return jobInfo;
        }
        catch (JobSubmissionException e) {
            throw new WebServiceException(e);
        }
    }

    // If the user uploaded multiple files for multiple parameters,
    // make sure we can match sets of files for submission to the batch process
    protected boolean multiFileListsAreSameSize() {
        if (multiFileValues.size() <= 1) { return true; }

        int listSize = multiFileValues.values().iterator().next().getNumFiles();
        for (MultiFileParameter multiFile : multiFileValues.values()) {
            if (multiFile.getNumFiles() != listSize) { return false; }
        }
        return true;
    }
    
    protected String getBaseFilename(GpFilePath file) {
        int periodIndex = file.getName().lastIndexOf('.');
        if (periodIndex > 0) {
            return file.getName().substring(0, periodIndex);
        }
        else {
            return file.getName();
        }
    }
    
    protected String getFileExtension(File file) {
        int periodIndex = file.getName().lastIndexOf('.');
        if (periodIndex > 0) {
            return file.getName().substring(periodIndex + 1);
        }
        else {
            return null;
        }
    }

    // If the user uploaded multiple files for multiple parameters,
    // attempt to match them up for job submissions. Automatic matching
    // can only be done by same filename - different extension.
    protected boolean checkForMatchedParameters() {
        // Make sure the filenames only differ by extension
        if (multiFileValues.size() > 1) {
            MultiFileParameter firstParameter = multiFileValues.values().iterator().next();
            int numFiles = firstParameter.getNumFiles();
            for (int i = 0; i < numFiles; i++) {
                String rootFileName = getBaseFilename(firstParameter.getFiles().get(i));
                for (String parameter : multiFileValues.keySet()) {
                    String filename = getBaseFilename(multiFileValues.get(parameter).getFiles().get(i));
                    if (rootFileName.compareTo(filename) != 0) { return false; }
                }
            }
        }
        return true;
    }

    // Get the root value here
    protected String undecorate(String key) {
        return key.substring(0, key.length() - multiSuffix.length());
    }

    protected void assignParameter(String key, String val, ParameterInfo[] parameterInfoArray) {
        for (int i = 0; i < parameterInfoArray.length; i++) {
            ParameterInfo pinfo = parameterInfoArray[i];
            if (pinfo.getName().compareTo(key) == 0) {
                pinfo.setValue(val);
                return;
            }
        }
        log.error("Key value " + key + " was not found in parameter info");
    }

    protected void readFormValuesAndLoadAttachedFiles(HttpServletRequest request, List<FileItem> params) throws IOException, FileUploadException, WebServiceException {
        // Though the batch files will have been uploaded already through our
        // upload applet and MultiFileUploadReceiver,
        // the form may still contain single attached files. Save them now.

        RequestContext reqContext = new ServletRequestContext(request);
        if (FileUploadBase.isMultipartContent(reqContext)) {
            Iterator<FileItem> it = params.iterator();
            while (it.hasNext()) {
                FileItem submission = it.next();
                if (!submission.isFormField()) {
                    loadAttachedFile(userName + "_run", submission);
                }
                else {
                    readFormParameter(submission);
                }
            }
            readBatchDirectories();
            unionBatchParameters();
        }
        else {
            throw new FileUploadException("Expecting form with encoding multipart/form-data");
        }
    }
    
    protected boolean isUrl(String url) {
        if (url.startsWith("http")) {
            return true;
        }
        else {
            return false;
        }
    }
    
    protected void readBatchDirectories() throws FileUploadException {
        Context userContext = Context.getContextForUser(userName);
        boolean isAdmin = AuthorizationHelper.adminJobs(userName);
        
        for (String key : multiFileValues.keySet()) {
            String dirUrl = formValues.get(key + "_url");
            MultiFileParameter multiFile = getMultiFileParameter(isAdmin, userContext, dirUrl);
            multiFileValues.put(key, multiFile);
        }            
    }
    

    //dirUrl is parsed from the form ( <paramname>_url )
    public static MultiFileParameter getMultiFileParameter(boolean isAdmin, Context userContext, String dirUrl) throws FileUploadException {
        boolean urlInput = false; //TODO: don't need this anymore
        GpFilePath inputDirPath = null;
        try {
            inputDirPath = GpFileObjFactory.getRequestedGpFileObj(dirUrl);
            urlInput = true;
        }
        catch (Throwable t) {
            //ignore thrown exception, it wasn't a valid url, but that is expected in some cases
        }
        
        if (inputDirPath == null) {
            //assume it is a literal server file path
            if (ServerConfigurationFactory.instance().getAllowInputFilePaths(userContext)) {
                File serverFile = new File(dirUrl);
                inputDirPath = ServerFileObjFactory.getServerFile(serverFile);
            }
        }
        if (inputDirPath == null) {
            throw new FileUploadException("Unable to attain the file at this URL: " + dirUrl);
        }
        return getMultiFileParameter(isAdmin, userContext, inputDirPath);
    }
    
    public static MultiFileParameter getMultiFileParameter(final boolean isAdmin, final Context userContext, final GpFilePath inputDirPath) throws FileUploadException {
        final String dirUrl=inputDirPath.getRelativePath();
        if (!inputDirPath.getServerFile().exists()) {
            throw new FileUploadException("Batch directory does not exist: " + dirUrl);
        }
        if (!inputDirPath.getServerFile().isDirectory()) {
            throw new FileUploadException("Batch directory is not a directory: " + dirUrl);
        }
        if (!inputDirPath.canRead(isAdmin, userContext)) {
            throw new FileUploadException("You don't have permission to read this directory: " + dirUrl);
        }

        inputDirPath.initMetadata();
        String parentUrl = "";
        try {
            parentUrl = inputDirPath.getUrl().toString();
        }
        catch (Throwable t) {
            log.error(t);
            throw new FileUploadException("Server error preparing batch input directory: " + dirUrl);
        }
        if (!parentUrl.endsWith("/")) {
            parentUrl = parentUrl + "/";
        }
        List<GpFilePath> filePaths = new ArrayList<GpFilePath>();
        File[] files = inputDirPath.getServerFile().listFiles();
        for(File file : files) {
            final String fileUrl = parentUrl + UrlUtil.encodeURIcomponent( file.getName() );
            try {
                GpFilePath filePath = GpFileObjFactory.getRequestedGpFileObj(fileUrl);
                filePath.initMetadata();
                filePaths.add(filePath);
            }
            catch (Throwable t) {
                log.error("Server error preparing batch input file in directory: " + dirUrl +", fileUrl="+fileUrl);
            }
        }
        boolean urlInput=true;
        MultiFileParameter multiFile = new MultiFileParameter(filePaths, urlInput);
        return multiFile;
    }

    protected boolean batchParamEmpty() {
        for (MultiFileParameter i : multiFileValues.values()) {
            if (i.getNumFiles() == 0) {
                return true;
            }
        }
        return false;
    }
    
    protected void unionBatchParameters() throws FileUploadException, UnsupportedEncodingException, WebServiceException {
        String taskLsid = getTaskLsid();
        if (taskLsid == null) { throw new FileUploadException("No Task LSID specified"); }
        TaskInfo taskInfo = new LocalAdminClient(userName).getTask(taskLsid);
        ParameterInfo parameterInfoArray[] = taskInfo.getParameterInfoArray();
        
        boolean firstSet = true;
        List<String> matchingFileNames = new ArrayList<String>();
        
        // Filter all parameters to only parameters with matching file types
        for (ParameterInfo i : parameterInfoArray) {
            boolean acceptAll = false;
            List<String> extensions = SemanticUtil.getFileFormats(i);
            if (extensions.size() == 0) { // Special case for inputs that accept no file types
                acceptAll = true; 
                log.debug("Input parameter for batch accepts no file types, setting to accept all") ;
            } 
            List<GpFilePath> matchedFilePaths = new ArrayList<GpFilePath>();
            MultiFileParameter param = multiFileValues.get(i.getName());
            if (param != null) {
                for (GpFilePath j : param.getFiles()) {
                    boolean match = false;
                    String ext = j.getExtension();
                    for (String k : extensions) {
                        if (k.equals(ext)) {
                            match = true;
                        }
                    }
                    if (match || acceptAll) {
                        matchedFilePaths.add(j);
                    }
                }
                multiFileValues.put(i.getName(), new MultiFileParameter(matchedFilePaths, param.isUrl()));
            }
        }
        
        // Get the union of base file names
        List<String> tenativeMatches = new ArrayList<String>();
        for (MultiFileParameter i : multiFileValues.values()) {
            if (firstSet) {
                for (GpFilePath j : i.getFiles()) {
                    tenativeMatches.add(getBaseFilename(j));
                    matchingFileNames.add(getBaseFilename(j));
                }
                firstSet = false;
            }
            
            for (String j : tenativeMatches) {
                boolean matched = false;
                for (GpFilePath k : i.getFiles()) {
                    if (j.equals(getBaseFilename(k))) {
                        matched = true;
                    }
                }
                if (!matched) {
                    matchingFileNames.remove(j);
                }
            }
        }
        
        // Filter all parameters to unioned filenames
        for (String i : multiFileValues.keySet()) {
            List<GpFilePath> filteredPaths = new ArrayList<GpFilePath>();
            for (GpFilePath j : multiFileValues.get(i).getFiles()) {
                String basename = getBaseFilename(j);
                for (String k : matchingFileNames) {
                    if (basename.equals(k)) {
                        filteredPaths.add(j);
                        break;
                    }
                }
            }
            boolean isUrl = multiFileValues.get(i).isUrl();
            multiFileValues.put(i, new MultiFileParameter(filteredPaths, isUrl));
        }
        
        if (batchParamEmpty()) {
            throw new FileUploadException("One or more batch parameters is an empty directory or doesn't conatin any files that can be matched to other batch parameters");
        }
    }

    protected void readFormParameter(FileItem submission) {
        String formName = submission.getFieldName();
        String formValue = submission.getString();

        if (!formName.endsWith(multiSuffix)) {
            formValues.put(formName, formValue);
        }
        else {
            multiFileValues.put(undecorate(formName), null);
        }
    }

    protected void loadAttachedFile(String prefix, FileItem submission) throws IOException {
        // We expect to find an attached file. 
        // But perhaps, this field was never filled in if the user specified a URL instead.
        if (submission.getSize() > 0) {
            // use createTempFile to guarantee a unique name, but then change it to a directory
            File tempDir = File.createTempFile(prefix, null);
            tempDir.delete();
            tempDir.mkdir();

            File file = new File(tempDir, submission.getName());
            try {
                submission.write(file);
            }
            catch (Exception e) {
                throw new IOException("Could not write file");
            }
            formValues.put(submission.getFieldName(), file.getCanonicalPath());
            log.debug("Storing " + submission.getFieldName() + " : " + file.getCanonicalPath());
        }
    }

    public String getId() {
        return Integer.toString(id);
    }

    public boolean isBatch() {
        return isBatch;
    }

    public static class MultiFileParameter {
        private List<GpFilePath> files = new ArrayList<GpFilePath>();
        private final Comparator<GpFilePath> comparator = new Comparator<GpFilePath>() {
                public int compare(GpFilePath o1, GpFilePath o2) {
                    return o1.getName().compareTo( o2.getName() );
                }
        };
        private boolean url;

        public MultiFileParameter(List<GpFilePath> values, boolean url) {
            this.url = url;
            for (GpFilePath i : values) {
                boolean includeThis = true;
                // Exclude unwanted system files
                for (String j : DataManager.FILE_EXCLUDES) {
                    if (i.getName().equalsIgnoreCase(j)) {
                        includeThis = false;
                    }
                }
                if (includeThis) {
                    files.add(i);
                }
            }
            
            Collections.sort(files, comparator);
        }
        
        public boolean isUrl(){
            return url;
        }

        public int getNumFiles() {
            return files.size();
        }

        public List<GpFilePath> getFiles() {
            return files;
        }

    }

}