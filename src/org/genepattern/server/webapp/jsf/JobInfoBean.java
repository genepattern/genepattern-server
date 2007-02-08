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
package org.genepattern.server.webapp.jsf;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.faces.FacesException;

import org.apache.log4j.Logger;
import org.genepattern.server.util.AuthorizationManagerFactory;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.StringUtils;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * 
 * @author Joshua Gould
 * 
 */
public class JobInfoBean {
    private ArrayList<OutputParameter> outputFiles;

    private ArrayList<InputParameter> inputs;

    private static Logger log = Logger.getLogger(JobInfoBean.class);

    int requestedJobNumber = -1;

    private String taskName;

    public JobInfoBean() {
        try {
            requestedJobNumber = Integer.parseInt(UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter(
                    "jobNumber")));
        } catch (NumberFormatException e1) {
            log.error(e1);
            throw new FacesException("Requested job not found.");
        }
        LocalAnalysisClient client = new LocalAnalysisClient(UIBeanHelper.getUserId());
        JobInfo job = null;
        try {
            job = client.getJob(requestedJobNumber);
            this.taskName = job.getTaskName();
        } catch (WebServiceException e) {
            log.error(e);
            throw new FacesException("Job " + requestedJobNumber + " not found.");
        }

        if (!AuthorizationManagerFactory.getAuthorizationManager().checkPermission("adminJobs",
                UIBeanHelper.getUserId())
                && !job.getUserId().equals(UIBeanHelper.getUserId())) {
            throw new FacesException("You don' have the required permissions to access the requested job.");
        }

        Map<String, ParameterInfo> parameterMap = new HashMap<String, ParameterInfo>();
        ParameterInfo[] formalParameters = null;
        try {
            TaskInfo task = new LocalAdminClient(job.getUserId()).getTask(job.getTaskLSID());

            formalParameters = task.getParameterInfoArray();
        } catch (WebServiceException e) {
            log.error(e);
        }
        outputFiles = new ArrayList<OutputParameter>();
        inputs = new ArrayList<InputParameter>();
        ParameterInfo[] parameterInfoArray = job.getParameterInfoArray();
        if (parameterInfoArray != null) {
            for (ParameterInfo p : parameterInfoArray) {
                parameterMap.put(p.getName(), p);
            }
        }

        if (formalParameters != null) {
            for (ParameterInfo formalParameter : formalParameters) {
                ParameterInfo param = parameterMap.get(formalParameter.getName());
                String value = param.getUIValue(formalParameter);
                // skip parameters that the user did not give a value for
                if (value == null || value.equals("")) {
                    continue;
                }
                String displayValue = value;

                boolean isUrl = false;
                boolean exists = false;
                String directory = null;
                if (formalParameter.isInputFile()) {
                    try {
                        // see if a URL was passed in
                        new URL(value);
                        isUrl = true;
                    } catch (MalformedURLException e) {

                    }
                    if (!isUrl) {
                        File f = new File(value);
                        exists = f.exists();
                        value = f.getName();
                        displayValue = value;
                        if (displayValue.startsWith("Axis")) {
                            displayValue = displayValue.substring(displayValue.indexOf('_') + 1);
                        } else {
                            directory = f.getParentFile().getName();
                        }
                    }
                }
                InputParameter p = new InputParameter();
                String name = (String) formalParameter.getAttributes().get("altName");
                if (name == null) {
                    name = formalParameter.getName();
                }
                name = name.replaceAll("\\.", " ");
                p.setDirectory(directory);
                p.setName(name);
                p.setDisplayValue(displayValue);
                p.setValue(value);
                p.setUrl(isUrl);
                p.setExists(exists);
                inputs.add(p);
            }
        }

        if (parameterInfoArray != null) {
            for (ParameterInfo param : parameterInfoArray) {
                if (param.isOutputFile() && !param.getName().equals(GPConstants.TASKLOG)) {
                    String value = param.getValue();
                    int index = StringUtils.lastIndexOfFileSeparator(value);
                    int jobNumber = Integer.parseInt(value.substring(0, index));
                    String filename = value.substring(index + 1);
                    OutputParameter p = new OutputParameter();
                    p.setJobNumber(jobNumber);
                    p.setName(filename);
                    outputFiles.add(p);
                }
            }
        }
    }

    public ArrayList<OutputParameter> getOutputFiles() {
        return outputFiles;
    }

    public ArrayList<InputParameter> getInputParameters() {
        return inputs;
    }

    public static class OutputParameter {
        private String name;

        private int jobNumber;

        public int getJobNumber() {
            return jobNumber;
        }

        public void setJobNumber(int jobNumber) {
            this.jobNumber = jobNumber;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    public static class InputParameter {
        private boolean url;

        private String name;

        private String value;

        private String displayValue;

        private boolean exists;

        private String directory;

        public String getName() {
            return name;
        }

        public void setDisplayValue(String displayValue) {
            this.displayValue = displayValue;
        }

        public String getDisplayValue() {
            return this.displayValue;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isUrl() {
            return url;
        }

        public void setUrl(boolean url) {
            this.url = url;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public boolean isExists() {
            return exists;
        }

        public void setExists(boolean exists) {
            this.exists = exists;
        }

        public String getDirectory() {
            return directory;
        }

        public void setDirectory(String directory) {
            this.directory = directory;
        }
    }

    public int getJobNumber() {
        return requestedJobNumber;
    }

    public String getTaskName() {
        return taskName;
    }
}
