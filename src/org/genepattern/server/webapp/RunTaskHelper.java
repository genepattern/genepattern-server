/*******************************************************************************
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright (2003-2006) by the
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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
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
    /** map between form field name and filesystem name */
    private HashMap<String, String> inputFileParameters = new HashMap<String, String>();

    private HashMap<String, String> requestParameters = new HashMap<String, String>();

    private File tempDir;

    private String username;

    private boolean visualizer;

    private List<ParameterInfo> missingParameters;

    private TaskInfo taskInfo;

    private String taskLsid;

    private String taskName;

    private ParameterInfo[] parameterInfoArray;

    /**
     * Creates a new RunTaskHelper instance
     * 
     * @param username
     *            The user name of the user that is running the task
     * @param request
     *            The HTTP request. The request should have the request or
     *            attribute taskLSID defined.
     * 
     */
    public RunTaskHelper(String username, HttpServletRequest request) throws IOException, FileUploadException {

        this.username = username;

        ServletFileUpload fub = new ServletFileUpload(new DiskFileItemFactory());

        HashMap<String, FileItem> nameToFileItemMap = new HashMap<String, FileItem>();

        // prefix is used to restrict access to input files based on username
        String prefix = username.hashCode() + "_";
        tempDir = File.createTempFile(prefix + "runTask", null);
        tempDir.delete();
        tempDir.mkdir();

        if (ServletFileUpload.isMultipartContent(request)) {
            List params = fub.parseRequest(request);

            for (Iterator iter = params.iterator(); iter.hasNext();) {
                FileItem fi = (FileItem) iter.next();
                nameToFileItemMap.put(fi.getFieldName(), fi);
            }

            for (Iterator iter = params.iterator(); iter.hasNext();) {
                FileItem fi = (FileItem) iter.next();
                String fieldName = fi.getFieldName();

                if (!fi.isFormField()) {
                    FileItem cbItem = (FileItem) nameToFileItemMap.get(fieldName + "_cb");
                    boolean urlChecked = cbItem != null ? "url".equals(cbItem.getString()) : false;
                    String fileName = fi.getName();
                    if (urlChecked || fileName == null || fileName.trim().equals("")) {
                        FileItem urlInput = nameToFileItemMap.get(fieldName + "_url");
                        if (urlInput != null) {
                            fileName = urlInput.getString();
                        }
                    }

                    if (fileName != null && !fileName.trim().equals("")) {
                        try {
                            new URL(fileName);
                            // don't bother trying to save a file that is a URL,
                            // retrieve it at execution time instead
                            inputFileParameters.put(fieldName, fileName);
                        } catch (MalformedURLException mfe) {
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
                                // deal with reload files that are not uploaded
                                // and so for which
                                // the write leaves an empty file
                                if (file.length() == 0) {
                                    file = oldFile;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            inputFileParameters.put(fieldName, file.getCanonicalPath());
                        }
                    }
                } else {
                    int endIndex = Math.max(fieldName.length() - "_url".length(), 1);
                    String parameterName = fieldName.substring(0, endIndex);

                    FileItem cbItem = (FileItem) nameToFileItemMap.get(parameterName + "_cb");
                    boolean urlChecked = cbItem != null ? "url".equals(cbItem.getString()) : false;
                    if (urlChecked) {
                        inputFileParameters.put(parameterName, fi.getString());
                    } else {
                        requestParameters.put(fieldName, fi.getString());
                    }
                }
            } // loop over files
        } else {
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
            } catch (WebServiceException e) {
                e.printStackTrace();
            }
            this.visualizer = "visualizer".equalsIgnoreCase((String) taskInfo.getTaskInfoAttributes().get(
                    GPConstants.TASK_TYPE));
            parameterInfoArray = taskInfo.getParameterInfoArray();
            if (parameterInfoArray == null) {
                parameterInfoArray = new ParameterInfo[0];
            }

        } else {
            parameterInfoArray = new ParameterInfo[0];
        }
        setParameterValues(request);

    }

    public ParameterInfo[] getParameterInfoArray() {
        return parameterInfoArray;

    }

    private void setParameterValues(HttpServletRequest request) throws IOException {
        String server = request.getScheme() + "://" + InetAddress.getLocalHost().getCanonicalHostName() + ":"
                + System.getProperty("GENEPATTERN_PORT");
        missingParameters = new ArrayList<ParameterInfo>();
        for (int i = 0; i < parameterInfoArray.length; i++) {
            ParameterInfo pinfo = parameterInfoArray[i];
            String value;
            if (pinfo.isInputFile()) {
                value = inputFileParameters.get(pinfo.getName());
                if (value == null) {
                    pinfo.getAttributes().put(ParameterInfo.TYPE, "");
                }
                if (value != null) {
                    try {
                        new URL(value);
                        HashMap attrs = pinfo.getAttributes();
                        attrs.put(ParameterInfo.MODE, ParameterInfo.URL_INPUT_MODE);
                        attrs.remove(ParameterInfo.TYPE);
                    } catch (MalformedURLException mfe) {
                        if (visualizer) {
                            File file = new File(value);
                            value = server + request.getContextPath() + "/getFile.jsp?task=&file="
                                    + file.getParentFile().getName() + File.separator + file.getName();
                        }
                    }
                }
            } else {
                value = requestParameters.get(pinfo.getName());
            }

            // look for missing required params

            if ((value == null) || (value.trim().length() == 0)) {
                HashMap pia = pinfo.getAttributes();
                boolean isOptional = ((String) pia
                        .get(GPConstants.PARAM_INFO_OPTIONAL[GPConstants.PARAM_INFO_NAME_OFFSET])).length() > 0;
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
