/*******************************************************************************
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright (2003-2011) by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are
 * reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 *
 *******************************************************************************/
package org.genepattern.server.webapp;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * Class for parsing request parameters for running tasks
 * 
 * @author Joshua Gould
 * 
 */
public class RunTaskHelper {
    private static Logger log = Logger.getLogger(RunTaskHelper.class);
    /** map between form field name and filesystem name */
    private HashMap<String, String> inputFileParameters = new HashMap<String, String>();
    private HashMap<String, String> requestParameters = new HashMap<String, String>();
    private Set<String> urlParameters = new HashSet<String>();
    private File tempDir;
    private List<ParameterInfo> missingParameters;
    private TaskInfo taskInfo;
    private String taskLsid;
    private String taskName;
    private ParameterInfo[] parameterInfoArray;
    private BatchSubmit batchJob = null;

    /**
     * Creates a new RunTaskHelper instance.
     * 
     * @param username, The user name of the user that is running the task.
     * @param request, The HTTP request. The request should have the request or attribute taskLSID defined.
     */
    public RunTaskHelper(String username, HttpServletRequest request) throws IOException, FileUploadException {
        ServletFileUpload fub = new ServletFileUpload(new DiskFileItemFactory());
        HashMap<String, FileItem> nameToFileItemMap = new HashMap<String, FileItem>();

        // prefix is used to restrict access to input files based on username
        String prefix = username + "_";
        tempDir = File.createTempFile(prefix + "run", null);
        tempDir.delete();
        tempDir.mkdir();

        if (ServletFileUpload.isMultipartContent(new ServletRequestContext(request))) {
            List params = fub.parseRequest(request);
            if (isBatchJob(params)) {
                handleBatch(request, params);
            }

            for (Iterator iter = params.iterator(); iter.hasNext();) {
                FileItem fi = (FileItem) iter.next();
                nameToFileItemMap.put(fi.getFieldName(), fi);
            }

            for (Iterator iter = params.iterator(); iter.hasNext();) {
                FileItem fi = (FileItem) iter.next();
                String fieldName = fi.getFieldName();

                if (!fi.isFormField()) {
                    FileItem cbItem = nameToFileItemMap.get(fieldName + "_cb");
                    boolean urlChecked = cbItem != null ? "url".equals(cbItem.getString()) : false;
                    String fileName = fi.getName();
                    if (urlChecked || fileName == null || fileName.trim().equals("")) {
                        FileItem urlInput = nameToFileItemMap.get(fieldName + "_url");
                        if (urlInput != null) {
                            String url = urlInput.getString();
                            if (url != null && !url.trim().equals("")) {
                                fileName = url;
                                urlParameters.add(fieldName);
                            }
                        }
                    }

                    if (fileName != null && !fileName.trim().equals("")) {
                        if (urlParameters.contains(fieldName)) {
                            // don't bother trying to save a file that is a URL,
                            // retrieve it at execution time instead
                            inputFileParameters.put(fieldName, fileName);
                        } 
                        else {
                            File oldFile = new File(fileName);
                            fileName = FilenameUtils.getName(fileName);
                            File file = new File(tempDir, fileName);
                            if (file.exists()) {
                                if (fileName.length() < 3) {
                                    fileName += "tmp";
                                }
                                file = File.createTempFile(fileName, FilenameUtils.getExtension(fileName), tempDir);
                            }
                            try {
                                fi.write(file);
                                if (file.length() == 0) {
                                    log.debug("empty input file: "+fieldName+"="+file.getPath());
                                    //TODO: not sure under what circumstances the following applies, so I am commenting it out, pcarr
                                    //    @see: GP-3326
                                    // deal with reload files that are not uploaded and so for which
                                    // the write leaves an empty file
                                    //    file = oldFile;
                                }
                            } 
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                            inputFileParameters.put(fieldName, file.getCanonicalPath());
                        }
                    }
                } 
                else {
                    String parameterName = fieldName;
                    if (fieldName.endsWith("_url")) {
                        //strip the trailing '_url' from the fieldName
                        final int K = fieldName.length() - "_url".length();
                        parameterName = fieldName.substring(0, K);
                    }
                    FileItem cbItem = (FileItem) nameToFileItemMap.get(parameterName + "_cb");
                    boolean urlChecked = cbItem != null ? "url".equals(cbItem.getString()) : false;
                    if (cbItem == null && fieldName.endsWith("_url")) {
                        urlChecked = true;
                    }
                    if (urlChecked) {
                        urlParameters.add(parameterName);
                        inputFileParameters.put(parameterName, fi.getString());
                    } 
                    else {
                        requestParameters.put(fieldName, fi.getString());
                    }
                }
            } // loop over files
        } 
        else {
            for (Enumeration en = request.getParameterNames(); en.hasMoreElements();) {
                String k = (String) en.nextElement();
                String v = request.getParameter(k);
                requestParameters.put(k, v);
            }
        }

        this.taskLsid = requestParameters.get("taskLSID");
        this.taskLsid = taskLsid != null ? URLDecoder.decode(taskLsid, "UTF-8") : null;
        this.taskName = requestParameters.get("taskName");
        this.taskName = taskName != null ? URLDecoder.decode(taskName, "UTF-8") : null;

        if (taskLsid == null) {
            if (taskName != null) {
                taskLsid = taskName;
            }
        }

        if (taskLsid != null) {
            try {
                this.taskInfo = new LocalAdminClient(username).getTask(taskLsid);
            } 
            catch (WebServiceException e) {
                e.printStackTrace();
            }
            parameterInfoArray = taskInfo.getParameterInfoArray();
            if (parameterInfoArray == null) {
                parameterInfoArray = new ParameterInfo[0];
            }
        } 
        else {
            parameterInfoArray = new ParameterInfo[0];
        }
        setParameterValues(request);
    }
    
    public boolean isBatchJob(List<FileItem> params) {
        boolean foundBatchParam = false;
        for (FileItem i : params) {
            if (i.getFieldName().endsWith("_batch")) {
                foundBatchParam = true;
                break;
            }
        }
        return foundBatchParam;
    }
    
    public boolean isBatchJob() {
        return batchJob != null;
    }
    
    public BatchSubmit getBatchJob() {
        return batchJob;
    }
    
    private void handleBatch(HttpServletRequest request, List<FileItem> params) throws FileUploadException {
        try {
            batchJob = new BatchSubmit(request, params);
            batchJob.submitJobs();
        }
        catch (FileUploadException e) {
            log.error("Problem handling batch submission: " + e.getMessage());
            throw e;
        }
        catch (WebServiceException e) {
            log.error("Problem handling batch submission: " + e.getMessage());
        }
        catch (IOException e) {
            log.error("Problem handling batch submission: " + e.getMessage());
        }
    }

    public ParameterInfo[] getParameterInfoArray() {
        return parameterInfoArray;
    }

    private void setParameterValues(HttpServletRequest request) throws IOException {
        String server = ServerConfigurationFactory.instance().getGpUrl();
        if (server == null || server.trim().length() == 0) {
            String portStr = System.getProperty("GENEPATTERN_PORT", "");
            portStr = portStr.trim();
            if (portStr.length()>0) {
                portStr = ":"+portStr;
            }            
            server = request.getScheme() + "://" + InetAddress.getLocalHost().getCanonicalHostName() + portStr + request.getContextPath();
        }
        if (!server.endsWith("/")) {
            server += '/';
        }
        missingParameters = new ArrayList<ParameterInfo>();
        for (int i = 0; i < parameterInfoArray.length; i++) {
            ParameterInfo pinfo = parameterInfoArray[i];
            String value;
            if (pinfo.isInputFile() || pinfo._isDirectory()) {
                value = inputFileParameters.get(pinfo.getName());
                if (value == null) {
                    pinfo.getAttributes().put(ParameterInfo.TYPE, "");
                }
                if (value != null && !value.equals("")) {
                    if (pinfo._isDirectory()) {
                        GpFilePath directory = null;
                        try {
                            //TODO: improve this; it works on the first run, but
                            //    ... the input param value changes on the job status page, (from a url to a server file),
                            //    ... and reload job doesn't work as expected
                            directory = GpFileObjFactory.getRequestedGpFileObj(value);
                            value = directory.getServerFile().getAbsolutePath();
                            inputFileParameters.put(pinfo.getName(), value);
                        }
                        catch (Exception e) {
                            log.error("Could not get a GP file path to the directory " + value);
                        }
                    }
                    
                    if (urlParameters.contains(pinfo.getName())) {
                        HashMap attrs = pinfo.getAttributes();
                        attrs.put(ParameterInfo.MODE, ParameterInfo.URL_INPUT_MODE);
                        attrs.remove(ParameterInfo.TYPE);
                    } 
                }
            } 
            else {
                value = requestParameters.get(pinfo.getName());
            }

            // look for missing required params
            if ((value == null) || (value.trim().length() == 0)) {
                HashMap pia = pinfo.getAttributes();
                boolean isOptional = ((String) pia.get(GPConstants.PARAM_INFO_OPTIONAL[GPConstants.PARAM_INFO_NAME_OFFSET])).length() > 0;
                if (!isOptional) {
                    missingParameters.add(pinfo);
                }
            }
            pinfo.setValue(value);
        }
    }

    public HashMap<String, String> getInputFileParameters() {
        return inputFileParameters;
    }

    public HashMap<String, String> getRequestParameters() {
        return requestParameters;
    }

    /**
     * Returns the directory where the input files are stored.
     * 
     * @return The temp directory.
     */
    public File getTempDirectory() {
        return tempDir;
    }

    public List<ParameterInfo> getMissingParameters() {
        return missingParameters;
    }

    public TaskInfo getTaskInfo() {
        return taskInfo;
    }

    public String getTaskLsid() {
        return taskLsid;
    }

    public String getTaskName() {
        return taskName;
    }

}
