package org.genepattern.server.webapp.jsf;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.webapp.jsf.JobBean.OutputFileInfo;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * Represents a job result. Wraps JobInfo and adds methods for getting the output files and the expansion state of
 * the associated UI panel
 */
public class JobResultsWrapper {
    private static Logger log = Logger.getLogger(JobResultsWrapper.class);

    private List<JobResultsWrapper> childJobs;
    private JobInfo jobInfo;
    private int level = 0;
    private List<OutputFileInfo> outputFiles;
    private boolean selected = false;
    private int sequence = 0;
    /**
     * Is current user allowed to delete this job?
     */
    private boolean deleteAllowed = false;

    public JobResultsWrapper(
            JobInfo jobInfo, 
            Map<String, 
            Collection<TaskInfo>> kindToModules,
            Set<String> selectedFiles, 
            Set<String> selectedJobs, 
            int level, 
            int sequence,
            Map<String, List<KeyValuePair>> kindToInputParameters, 
            boolean showExecutionLogs) 
    {
        this.jobInfo = jobInfo;
        this.selected = selectedJobs.contains(String.valueOf(jobInfo.getJobNumber()));
        this.level = level;
        this.sequence = sequence;

        deleteAllowed = jobInfo.getUserId().equals(UIBeanHelper.getUserId()) || AuthorizationHelper.adminJobs();

        // Build the list of output files from the parameter info array.

        outputFiles = new ArrayList<OutputFileInfo>();
        ParameterInfo[] parameterInfoArray = jobInfo.getParameterInfoArray();
        if (parameterInfoArray != null) {
            File outputDir = new File(GenePatternAnalysisTask.getJobDir("" + jobInfo.getJobNumber()));
            for (int i = 0; i < parameterInfoArray.length; i++) {
                if (parameterInfoArray[i].isOutputFile()) {
                    boolean isTaskLog = (parameterInfoArray[i].getName().equals(GPConstants.TASKLOG) || parameterInfoArray[i].getName().endsWith(GPConstants.PIPELINE_TASKLOG_ENDING));

                    if (showExecutionLogs || !isTaskLog) {
                        File file = new File(outputDir, parameterInfoArray[i].getName());
                        String kind = SemanticUtil.getKind(file);
                        Collection<TaskInfo> modules;

                        if (parameterInfoArray[i].getName().equals(GPConstants.TASKLOG)) {
                            modules = new ArrayList<TaskInfo>();
                        } 
                        else {
                            modules = kindToModules.get(kind);
                        }
                        OutputFileInfo pInfo = new OutputFileInfo(parameterInfoArray[i], file, modules, jobInfo.getJobNumber(), kind);
                        pInfo.setSelected(selectedFiles.contains(pInfo.getValue()));
                        outputFiles.add(pInfo);
                    }
                }
            }
        }

        // Child jobs
        childJobs = new ArrayList<JobResultsWrapper>();
        String userId = UIBeanHelper.getUserId();
        LocalAnalysisClient analysisClient = new LocalAnalysisClient(userId);
        try {
            JobInfo[] children = analysisClient.getChildren(jobInfo.getJobNumber());
            int seq = 1;
            int childLevel = getLevel() + 1;
            for (JobInfo child : children) {
                childJobs.add(new JobResultsWrapper(child, kindToModules, selectedFiles, selectedJobs, childLevel, seq,kindToInputParameters, showExecutionLogs));
                seq++;
            }
        } 
        catch (WebServiceException e) {
            log.error("Error getting child jobs", e);
        }
    }

    /**
     * @return The list all descendant jobs, basically a flattened tree.
     */
    public List<JobResultsWrapper> getDescendantJobs() {
        List<JobResultsWrapper> descendantJobs = new ArrayList<JobResultsWrapper>();
        descendantJobs.addAll(childJobs);
        for (JobResultsWrapper childJob : childJobs) {
            descendantJobs.addAll(childJob.getDescendantJobs());
        }
        return descendantJobs;
    }

    public List<OutputFileInfo> getAllFileInfos() {
        List<OutputFileInfo> allFiles = new ArrayList<OutputFileInfo>();
        allFiles.addAll(outputFiles);
        for (JobResultsWrapper child : childJobs) {
            allFiles.addAll(child.getAllFileInfos());
        }
        return allFiles;
    }

    public List<JobResultsWrapper> getChildJobs() {
        return childJobs;
    }

    public Date getDateCompleted() {
        return jobInfo.getDateCompleted();
    }

    public Date getDateSubmitted() {
        return jobInfo.getDateSubmitted();
    }

    public int getJobNumber() {
        return jobInfo.getJobNumber();
    }

    public int getLevel() {
        return level;
    }

    public List<OutputFileInfo> getOutputFileParameterInfos() {
        return outputFiles;
    }

    public int getSequence() {
        return sequence;
    }

    public String getStatus() {
        return jobInfo.getStatus().toLowerCase();
    }

    public int getTaskID() {
        return jobInfo.getTaskID();
    }

    public String getTaskLSID() {
        return jobInfo.getTaskLSID();
    }

    public String getTaskName() {
        return jobInfo.getTaskName();
    }

    public String getUserId() {
        return jobInfo.getUserId();
    }

    /**
     * boolean property used to conditionally render or enable some menu items.
     * 
     * @return Whether the job is complete.
     */
    public boolean isComplete() {
        String status = jobInfo.getStatus();
        return status.equalsIgnoreCase("Finished")
                || status.equalsIgnoreCase("Error");
    }

    /**
     * This property supports saving of the "expanded" state of the job across
     * requests. It is used to initialize display properties of rows associated
     * with this job.
     * 
     * @return
     */
    public boolean isExpanded() {
        String parameterName = "expansion_state_" + jobInfo.getJobNumber();
        String value = UIBeanHelper.getRequest().getParameter(parameterName);
        return (value == null || value.equals("true"));
    }

    public boolean isSelected() {
        return selected;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setSelected(boolean bool) {
        this.selected = bool;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public boolean isDeleteAllowed() {
        return deleteAllowed;
    }

}