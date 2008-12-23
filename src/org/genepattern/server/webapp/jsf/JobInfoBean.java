/*******************************************************************************
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright (2003-2008) by the
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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.FacesException;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;
import org.genepattern.server.PermissionsManager;
import org.genepattern.server.auth.GroupPermission;
import org.genepattern.server.auth.GroupPermission.Permission;
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
    private JobInfoWrapper jobInfoWrapper;
    private JobInfoWrapper[] childJobs; // for pipelines
    private static Logger log = Logger.getLogger(JobInfoBean.class);
    private String genePatternUrl;

    private int requestedJobNumber = -1;

    public static class JobInfoWrapper {
        private JobInfo jobInfo = null;
        private int jobNumber;
        private String taskName;
        private String status;
        private Date dateSubmitted;
        private Date dateCompleted;
        private List<InputParameter> inputParameters;
        private List<OutputParameter> outputFiles;
        
        private TaskInfo taskInfo = null;
        
        public int getJobNumber() {
            return jobNumber;
        }
         
        public String getTaskName() {
            return taskName;
        }

        public String getStatus() {
            return status;
        }

        public Date getDateSubmitted() {
            return dateSubmitted;
        }
        
        public Date getDateCompleted() {
            return dateCompleted;
        }

        public List<InputParameter> getInputParameters() {
            return inputParameters;
        }

        public List<OutputParameter> getOutputFiles() {
            return outputFiles;
        }

        public long getElapsedTimeMillis() {
            if (dateSubmitted == null) return 0;
            else if (dateCompleted != null) return dateCompleted.getTime() - dateSubmitted.getTime();
            else if (!"finished".equals(getStatus())) return new Date().getTime() - dateSubmitted.getTime();
            else return 0;
        }
        
        public long getRefreshInterval() {
            long elapsedTimeMillis = getElapsedTimeMillis();
            if (elapsedTimeMillis < 10000) {
                return 1000;
            }
            else if (elapsedTimeMillis < 60000) {
                return 5000;
            }
            else {
                return 10000;
            }
        }
        
        public String getRefreshIntervalLabel() {
            long millis = getRefreshInterval();
            long sec = millis / 1000L;
            if (sec == 1) {
                return "1 second";
            }
            else {
                return sec + " seconds";
            }
        }
        
        public List<GroupPermission> getGroupPermissions() {
            PermissionsManager pm = new PermissionsManager(jobInfo.getUserId());
            return pm.getJobResultPermissions(jobNumber);
        }
        
        public boolean getSetJobPermissionsAllowed() {
            String userId = UIBeanHelper.getUserId();
            PermissionsManager pm = new PermissionsManager(userId);
            return pm.canSetJobPermissions(jobInfo);
        }
        
        /**
         * Process request parameters (from form submission) and update the access permissions for the current job.
         * Only the owner of a job is allowed to change its permissions.
         */
        public String saveGroupPermissions() { 
            List<GroupPermission> permissions = getGroupPermissions();
            /* 
             JSF auto generated parameter names from jobResult.xhtml, e.g.
               permForm:   permForm
               permForm:permTable:0:RW:    true
               permForm:permTable:0:R:     true
               permForm:permTable:1:R:     true
             Note: only selected checkboxes are submitted.
            */
            
            //not sure this is the best approach, but for now, 
            //    regenerate the table in the exact order as was done to present the input form
            
            //NOTE: don't edit the jobResult.xhtml without also editing this page 
            //    in other words, DON'T REUSE THIS CODE in another page unless you know what you are doing
            Set<GroupPermission> updatedPermissions = new HashSet<GroupPermission>();
            Map<String,String[]> requestParameters = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterValuesMap();
            for(String name : requestParameters.keySet()) {
                //System.out.println("\t"+val);
                if (name.endsWith("R") || name.endsWith("RW")) {
                    int gin = -1;
                    String[] splits = name.split(":");
                    String permFlag = splits[ splits.length - 1 ];
                    int sin = splits.length - 2;
                    if (sin > 0) {
                        try {
                            gin = Integer.parseInt( splits[sin] );
                            String groupId = permissions.get(gin).getGroupId();
                            System.out.println("set "+permFlag+" permission for group: " + groupId);
                            
                            Permission p = null;
                            if (permFlag.equalsIgnoreCase("R")) {
                                p = GroupPermission.Permission.READ;
                            }
                            else if (permFlag.equalsIgnoreCase("RW")) {
                                p = GroupPermission.Permission.READ_WRITE;                                
                            }
                            else {
                                handleException("Ignoring permissions flag: "+permFlag);
                                return "error";
                            }
                            GroupPermission gp = new GroupPermission(groupId, p);
                            updatedPermissions.add(gp);
                        }
                        catch (NumberFormatException e) {
                            handleException("Can't parse input form", e);
                            return "error";
                        }
                    }
                }
            }
            
            String userId = UIBeanHelper.getUserId();
            PermissionsManager pm = new PermissionsManager(userId);
            try {
                pm.setPermissions(jobInfo, updatedPermissions);
                return "success";
            }
            catch (Exception e) {
                handleException("You are not authorized to change the permissions for this job", e);
                return "error";
            }
        }
        
        private void handleException(String message) {
            log.error(message);
            UIBeanHelper.setErrorMessage(message);
        }

        private void handleException(String message, Exception e) {
            log.error(message, e);
            UIBeanHelper.setErrorMessage(message);
        }

        public String getPermissionsLabel() {
            List<GroupPermission> groups = getGroupPermissions();
            if (groups == null || groups.size() == 0) {
                return "";
            }
            String rval = "";
            for (GroupPermission gp : groups) {
                rval += gp.getGroupId() + " " + gp.getPermission() + ", ";
            }
            int idx = rval.lastIndexOf(", ");
            return rval.substring(0, idx) + "";
        }
    }

    public JobInfoBean() {
        genePatternUrl = UIBeanHelper.getServer();
        try {
            requestedJobNumber = Integer.parseInt(UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("jobNumber")));
        }
        catch (NumberFormatException e1) {
            log.error(e1);
            //throw new FacesException("Requested job not found.");
            return;
        }
        LocalAnalysisClient client = new LocalAnalysisClient(UIBeanHelper.getUserId());
        try {
            JobInfo jobInfo = client.getJob(requestedJobNumber);
            jobInfoWrapper = createJobInfoWrapper(jobInfo);
            JobInfo[] children = new JobInfo[0];
            try {
                children = client.getChildren(jobInfo.getJobNumber());
            } 
            catch (Exception e) {
                log.error(e.getMessage(), e);
            }
    
            childJobs = new JobInfoWrapper[children != null ? children.length : 0];
            if (children != null) {
                for (int i = 0, length = children.length; i < length; i++) {
                    childJobs[i] = createJobInfoWrapper(children[i]);
                }
            }
        } 
        catch (WebServiceException e) {
            log.error(e);
            throw new FacesException("Job " + requestedJobNumber + " not found.");
        }
    }

    private JobInfoWrapper createJobInfoWrapper(JobInfo jobInfo) {
        //TODO: shouldn't have to check for permissions here, should require permissions in order to create the JobInfo instance
        String userId = UIBeanHelper.getUserId();
        PermissionsManager pm = new PermissionsManager(userId);
        if (!pm.canReadJob(userId, jobInfo)) {
            throw new FacesException("You don't have the required permissions to access the requested job."); 
        }

        Map<String, ParameterInfo> parameterMap = new HashMap<String, ParameterInfo>();
        ParameterInfo[] formalParameters = null;
        TaskInfo taskInfo = null;
        try {
            taskInfo = new LocalAdminClient(jobInfo.getUserId()).getTask(jobInfo.getTaskLSID());
            formalParameters = taskInfo.getParameterInfoArray();
        } 
        catch (WebServiceException e) {
            log.error(e);
        }

        List<OutputParameter> outputFiles = new ArrayList<OutputParameter>();
        List<InputParameter> inputs = new ArrayList<InputParameter>();
        ParameterInfo[] parameterInfoArray = jobInfo.getParameterInfoArray();
        if (parameterInfoArray != null) {
            for (ParameterInfo p : parameterInfoArray) {
                parameterMap.put(p.getName(), p);
            }
        }

        if (formalParameters != null) {
            for (ParameterInfo formalParameter : formalParameters) {
                ParameterInfo param = parameterMap.get(formalParameter.getName());
                if (param == null) {
                    continue;
                }
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
                        URL url = new URL(value);
                        // bug 2026 - file:// URLs should not be treated as a URL
                        isUrl = true;
                        if ("file".equals(url.getProtocol())){
                            isUrl = false;
                            value = value.substring(5);// strip off the file: part for the next step
                        }
                        if (displayValue.startsWith(genePatternUrl)) {          
                            int lastNameIdx = value.lastIndexOf("/");
                            displayValue = value.substring(lastNameIdx+1);      
                            isUrl = true;
                        }
                    } 
                    catch (MalformedURLException e) {
                        if (displayValue.startsWith("<GenePatternURL>")) {          
                            int lastNameIdx = value.lastIndexOf("/");
                            if (lastNameIdx == -1) {
                                lastNameIdx = value.lastIndexOf("file=");
                                if (lastNameIdx != -1) {
                                    lastNameIdx += 5;
                                }
                            }
                            if (lastNameIdx != -1) { 
                                displayValue = value.substring(lastNameIdx);        
                            } 
                            else {
                                displayValue = value;
                            }
                            value = genePatternUrl + value.substring("<GenePatternURL>".length());
                            isUrl = true;
                        } 
                    }

                    if (!isUrl) {
                        File f = new File(value);
                        exists = f.exists();
                        value = f.getName();
                        displayValue = value;
                        if (displayValue.startsWith("Axis")) {
                            displayValue = displayValue.substring(displayValue.indexOf('_') + 1);
                        }
                        if (exists) {
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
                if (formalParameter.isPassword()) {
                    p.setDisplayValue(displayValue != null ? "*****" : null);
                } 
                else {
                    p.setDisplayValue(displayValue);
                }
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
        JobInfoWrapper jobInfoWrapper = new JobInfoWrapper();
        jobInfoWrapper.jobInfo = jobInfo;
        jobInfoWrapper.jobNumber = jobInfo.getJobNumber();
        jobInfoWrapper.taskName = jobInfo.getTaskName();
        jobInfoWrapper.status = jobInfo.getStatus();
        jobInfoWrapper.dateSubmitted = jobInfo.getDateSubmitted();
        jobInfoWrapper.dateCompleted = jobInfo.getDateCompleted();
        jobInfoWrapper.outputFiles = outputFiles;
        jobInfoWrapper.inputParameters = inputs;
        //jobInfoWrapper.isVisualizer = taskInfo != null && taskInfo.isVisualizer();
        jobInfoWrapper.taskInfo = taskInfo;
        return jobInfoWrapper;
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

    public Date getDateSubmitted() {
        return jobInfoWrapper.getDateSubmitted();
    }

    public Date getDateCompleted() {
        return jobInfoWrapper.getDateCompleted();
    }

    public JobInfoWrapper getJobInfoWrapper() {
    return jobInfoWrapper;
    }

    public void setJobInfoWrapper(JobInfoWrapper jobInfoWrapper) {
    this.jobInfoWrapper = jobInfoWrapper;
    }

    public JobInfoWrapper[] getChildJobs() {
    return childJobs;
    }

    public void setChildJobs(JobInfoWrapper[] childJobs) {
    this.childJobs = childJobs;
    }

}
