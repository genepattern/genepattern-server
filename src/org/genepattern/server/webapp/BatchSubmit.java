package org.genepattern.server.webapp;

import java.io.File;
import java.io.IOException;
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
import org.genepattern.server.domain.AnalysisJobDAO;
import org.genepattern.server.domain.BatchJob;
import org.genepattern.server.domain.BatchJobDAO;
import org.genepattern.server.executor.JobSubmissionException;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.handler.AddNewJobHandler;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
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
    private boolean listSizesMatch = true;
    private boolean matchedFiles = true;
    private Integer id;
    private List<ParameterInfo> missingParameters = new ArrayList<ParameterInfo>();
    private static final String multiSuffix = "_batch";

    // Collect input parameters.
    // Find multi-file input file parameters
    // Validate that only one file parameter has multiple files
    // Todo in the future: Allow groups of matched files,
    // Create resolution screen for mismatches
    // Submit a job for each file.
    public BatchSubmit(HttpServletRequest request, List<FileItem> params) throws IOException, FileUploadException {
        userName = (String) request.getSession().getAttribute(GPConstants.USERID);
        readFormValuesAndLoadAttachedFiles(request, params);
    }

    public List<ParameterInfo> getMissingParameters() {
        return missingParameters;
    }

    public void submitJobs() throws WebServiceException, IOException {
        isBatch = false;

        // Look up the task name, stored in a hidden field
        String taskLsid = formValues.get("taskLSID");
        if (taskLsid == null) {
            // Try the task name instead
            taskLsid = formValues.get("taskName");
        }
        taskLsid = (taskLsid) != null ? URLDecoder.decode(taskLsid, "UTF-8") : null;

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

        if (missingParameters.size() > 0) {
            log.warn("Missing required parameters");
            return;
        }

        if (!multiFileListsAreSameSize()) {
            listSizesMatch = false;
            return;
        }
        if (!checkForMatchedParameters()) {
            matchedFiles = false;
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
                    String parameterValue = multiFileValues.get(parameter).getFiles().get(i).getCanonicalPath();
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

    private JobInfo submitJob(int taskID, ParameterInfo[] parameters) throws WebServiceException {
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
    private boolean multiFileListsAreSameSize() {
        if (multiFileValues.size() <= 1) { return true; }

        int listSize = multiFileValues.values().iterator().next().getNumFiles();
        for (MultiFileParameter multiFile : multiFileValues.values()) {
            if (multiFile.getNumFiles() != listSize) { return false; }
        }
        return true;
    }
    
    private String getBaseFilename(File file) {
        int periodIndex = file.getName().lastIndexOf('.');
        if (periodIndex > 0) {
            return file.getName().substring(0, periodIndex);
        }
        else {
            return file.getName();
        }
    }

    // If the user uploaded multiple files for multiple parameters,
    // attempt to match them up for job submissions. Automatic matching
    // can only be done by same filename - different extension.
    private boolean checkForMatchedParameters() {
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

    // Submitted multifile fields for param XX arrive in the field
    // XX_multiSuffix
    // Get the root value here
    private String undecorate(String key) {
        return key.substring(0, key.length() - multiSuffix.length());
    }

    private void assignParameter(String key, String val, ParameterInfo[] parameterInfoArray) {
        for (int i = 0; i < parameterInfoArray.length; i++) {
            ParameterInfo pinfo = parameterInfoArray[i];
            if (pinfo.getName().compareTo(key) == 0) {
                pinfo.setValue(val);
                return;
            }
        }
        log.error("Key value " + key + " was not found in parameter info");
    }

    private void readFormValuesAndLoadAttachedFiles(HttpServletRequest request, List<FileItem> params) throws IOException, FileUploadException {
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
            verifyBatchParameters();
        }
        else {
            throw new FileUploadException("Expecting form with encoding multipart/form-data");
        }
    }
    
    private void readBatchDirectories() throws FileUploadException {
        for (String i : multiFileValues.keySet()) {
            String dirUrl = formValues.get(i + "_url");
            File dir = DataServlet.getFileFromUrl(dirUrl);
            
            if (dir == null || !dir.exists() || !dir.isDirectory()) {
                throw new FileUploadException("Batch directory not valid");
            }
            
            MultiFileParameter multiFile = new MultiFileParameter(dir.listFiles());
            multiFileValues.put(i, multiFile);
        }
    }
    
    private void verifyBatchParameters() throws FileUploadException {
        boolean sizesMatch = listSizesMatch();
        boolean paramsMatch = checkForMatchedParameters();
        if (!sizesMatch) {
            throw new FileUploadException("The number of files in the batch directories do not match");
        }
        if (!paramsMatch) {
            throw new FileUploadException("The file names in batch directories do not match");
        }
    }

    private void readFormParameter(FileItem submission) {
        String formName = submission.getFieldName();
        String formValue = submission.getString();

        if (!formName.endsWith(multiSuffix)) {
            formValues.put(formName, formValue);
        }
        else {
            multiFileValues.put(undecorate(formName), null);
        }
    }

    private void loadAttachedFile(String prefix, FileItem submission) throws IOException {
        // We expect to find an attached file. But perhaps, this field was never
        // filled in
        // if the user specified a URL instead.
        if (submission.getSize() > 0) {
            // use createTempFile to guarantee a unique name, but then change it
            // to a directory
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

    public boolean listSizesMatch() {
        return listSizesMatch;
    }

    public boolean matchedFiles() {
        return matchedFiles;
    }

    private class MultiFileParameter {
        private List<File> files = new ArrayList<File>();
        private final CompareByFilename comparator = new CompareByFilename();

        public MultiFileParameter(File[] values) {
            for (File i : values) {
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

        public int getNumFiles() {
            return files.size();
        }

        public List<File> getFiles() {
            return files;
        }

    }

    private class CompareByFilename implements Comparator<File> {
        public int compare(File o1, File o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

}