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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.FacesException;

import org.apache.log4j.Logger;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.GroupPermission;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
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
        private String taskName;
        private String status;
        private Date dateSubmitted;
        private Date dateCompleted;
        private List<InputParameter> inputParameters;
        private List<OutputParameter> outputFiles;

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
        
    }

    public JobInfoBean() {

	genePatternUrl = UIBeanHelper.getServer();

	try {
	    requestedJobNumber = Integer.parseInt(UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter(
		    "jobNumber")));
	} catch (NumberFormatException e1) {
	    log.error(e1);
	    //throw new FacesException("Requested job not found.");
	    return;
	}
	LocalAnalysisClient client = new LocalAnalysisClient(UIBeanHelper.getUserId());
	try {
	    JobInfo job = client.getJob(requestedJobNumber);

	    jobInfoWrapper = createJobInfoWrapper(job);
	    JobInfo[] children = new JobInfo[0];
	    try {
		children = client.getChildren(job.getJobNumber());
	    } catch (Exception e) {

		log.error(e.getMessage(), e);

	    }

	    childJobs = new JobInfoWrapper[children != null ? children.length : 0];
	    if (children != null) {
		for (int i = 0, length = children.length; i < length; i++) {
		    childJobs[i] = createJobInfoWrapper(children[i]);
		}
	    }
	} catch (WebServiceException e) {
	    log.error(e);
	    throw new FacesException("Job " + requestedJobNumber + " not found.");
	}
    }

    //TODO: create helper function in group manager package
    private boolean canReadJob(String userId, JobInfo jobInfo) {
        return checkPermission(userId, jobInfo, false);
    }
    private boolean canWriteJob(String userId, JobInfo jobInfo) {
        return checkPermission(userId, jobInfo, true);
    }
    private boolean checkPermission(String userId, JobInfo jobInfo, boolean write) {
        if (AuthorizationHelper.adminJobs(userId)) {
            return true;
        }
        String jobOwner = jobInfo.getUserId();
        if (userId.equals(jobOwner)) {
            return true;
        }
        AnalysisDAO ds = new AnalysisDAO();
        Set<GroupPermission>  groupPermissions = ds.getGroupPermissions(jobInfo.getJobNumber());
        IGroupMembershipPlugin groupMembership = UserAccountManager.instance().getGroupMembership();
        for(GroupPermission gp : groupPermissions) {
            if (groupMembership.isMember(userId, gp.getGroupId())) {
                if (write && gp.getPermission().canWrite()) {
                    return true;
                }
                else if (gp.getPermission().canRead()) {
                    return true;
                }
            }
        }
        return false;
    }

    private JobInfoWrapper createJobInfoWrapper(JobInfo job) {
        String userId = UIBeanHelper.getUserId();
        if (!canReadJob(userId, job)) {
            throw new FacesException("You don't have the required permissions to access the requested job.");
        }

        Map<String, ParameterInfo> parameterMap = new HashMap<String, ParameterInfo>();
        ParameterInfo[] formalParameters = null;
        try {
            TaskInfo task = new LocalAdminClient(job.getUserId()).getTask(job.getTaskLSID());
            formalParameters = task.getParameterInfoArray();
        } 
        catch (WebServiceException e) {
            log.error(e);
        }

        List<OutputParameter> outputFiles = new ArrayList<OutputParameter>();
        List<InputParameter> inputs = new ArrayList<InputParameter>();
        ParameterInfo[] parameterInfoArray = job.getParameterInfoArray();
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
        jobInfoWrapper.taskName = job.getTaskName();
        jobInfoWrapper.status = job.getStatus();
        jobInfoWrapper.dateSubmitted = job.getDateSubmitted();
        jobInfoWrapper.dateCompleted = job.getDateCompleted();
        jobInfoWrapper.outputFiles = outputFiles;
        jobInfoWrapper.inputParameters = inputs;
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
