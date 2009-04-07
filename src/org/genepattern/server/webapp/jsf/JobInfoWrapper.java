package org.genepattern.server.webapp.jsf;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.PermissionsHelper;
import org.genepattern.server.auth.GroupPermission;
import org.genepattern.server.auth.GroupPermission.Permission;
import org.genepattern.server.webapp.jsf.JobInfoBean.InputParameter;
import org.genepattern.server.webapp.jsf.JobInfoBean.OutputParameter;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;

public class JobInfoWrapper {
    private static Logger log = Logger.getLogger(JobInfoWrapper.class);

    private JobInfo jobInfo = null;
    private int jobNumber;
    private String taskName;
    private String status;
    private Date dateSubmitted;
    private Date dateCompleted;
    private List<InputParameter> inputParameters;
    private List<OutputParameter> outputFiles;
    private TaskInfo taskInfo = null;
    private boolean isVisualizer = false;
    private boolean isPipeline = false;
    
    private PermissionsHelper permissionsHelper;
    
    public JobInfoWrapper(JobInfo jobInfo, TaskInfo taskInfo) {
        this.jobInfo = jobInfo;
        this.jobNumber = jobInfo.getJobNumber();
        this.taskName = jobInfo.getTaskName();
        this.status = jobInfo.getStatus();
        this.dateSubmitted = jobInfo.getDateSubmitted();
        this.dateCompleted = jobInfo.getDateCompleted();
        
        this.isVisualizer = taskInfo != null && taskInfo.isVisualizer();
        this.isPipeline = taskInfo != null && taskInfo.isPipeline();
        this.taskInfo = taskInfo;
    }
    
    public void setInputParameters(List<InputParameter> inputParameters) {
        this.inputParameters = inputParameters;
    }

    public void setOutputFiles(List<OutputParameter> outputFiles) {
        this.outputFiles = outputFiles;
    }
    
    public void setPermissionsHelper(PermissionsHelper ph) {
        this.permissionsHelper = ph;
    }
    
    public String getOwner() {
        return jobInfo.getUserId();
    }
    
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

    public List<GroupPermission> getNonPublicGroupPermissions() {
        return permissionsHelper.getNonPublicPermissions();
    }
    
    public Permission getPublicAccessPermission() {
        return permissionsHelper.getPublicAccessPermission();
    }
    
    public void setPublicAccessPermission(Permission p) {
        log.error("ignoring setPublicAccessPermission: "+p);
    }
    
    public boolean isPublicNone() {
        return permissionsHelper.getPublicAccessPermission().equals( Permission.NONE );
    }

    public boolean isPublicRead() {
        return permissionsHelper.getPublicAccessPermission().equals( Permission.READ );        
    }
    
    public boolean isPublicReadWrite() {
        return permissionsHelper.getPublicAccessPermission().equals( Permission.READ_WRITE );
    }

    /**
     * get the number of groups that this user is a member of, excluding public groups from the count.
     * @return
     */
    public int getNumNonPublicGroups() {
        return permissionsHelper.getNonPublicPermissions().size();
    }
    
    public List<GroupPermission> getGroupPermissions() {
        boolean includeUsersGroups = true;
        return permissionsHelper.getJobResultPermissions(includeUsersGroups);
    }
    
    public boolean getSetJobPermissionsAllowed() {
        return permissionsHelper.canSetJobPermissions();
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
    
    public boolean getIsVisualizer() {
        return isVisualizer;
    }
    public boolean getIsPipeline() {
        return isPipeline;
    }

}
