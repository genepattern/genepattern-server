/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.jsf;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.server.JobInfoWrapper;
import org.genepattern.server.JobInfoWrapper.InputFile;
import org.genepattern.server.JobInfoWrapper.ParameterInfoWrapper;
import org.genepattern.server.auth.GroupPermission;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.webapp.jsf.JobBean.OutputFileInfo;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.util.GPConstants;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * Represents a job result. Wraps JobInfo and adds methods for getting the output files and the expansion state of
 * the associated UI panel
 */
public class JobResultsWrapper {
    private static Logger log = Logger.getLogger(JobResultsWrapper.class);

    private JobInfo jobInfo;
    private int level = 0;
    private boolean selected = false;
    private int sequence = 0;
    private long totalSize = 0l;
    private boolean showExecutionLogs = false;
    private Map<String,Set<TaskInfo>> kindToModules;
    private Set<String> selectedFiles;
    private Set<String> selectedJobs;
    private Map<String, List<KeyValuePair>> kindToInputParameters;
    private boolean showPipelineOutputFiles = false;

    private JobPermissionsBean _jobPermissionsBean = null;

    private boolean fileSortAscending = false;
    private String fileSortColumn = "";
    
    private JobInfoWrapper jobInfoWrapper;
    
    public JobInfoWrapper getJobInfoWrapper() {
        return jobInfoWrapper;
    }

    public void setJobInfoWrapper(JobInfoWrapper jobInfoWrapper) {
        this.jobInfoWrapper = jobInfoWrapper;
    }
    
    public List<InputFile> getInputFiles() {
        if (getJobInfoWrapper() == null) {
            log.debug("jobInfoWrapper is null in JobResultsWrapper; it should be populated!");
            return Collections.emptyList();
        }
        return getJobInfoWrapper().getInputFiles();
    }
    
    public List<ParameterInfoWrapper> getDirectoryInputs() {
        if (getJobInfoWrapper() == null) {
            log.debug("jobInfoWrapper is null in JobResultsWrapper; it should be populated!");
            return Collections.emptyList();
        }
        return getJobInfoWrapper().getDirectoryInputs();
    }

    public JobResultsWrapper(
            final JobPermissionsBean jobPermissionsBean,
            final JobInfo jobInfo, 
            final Map<String,Set<TaskInfo>> kindToModules,
            final Set<String> selectedFiles, 
            final Set<String> selectedJobs, 
            final int level, 
            final int sequence,
            final Map<String, List<KeyValuePair>> kindToInputParameters, 
            final boolean showExecutionLogs) 
    {
        this._jobPermissionsBean = jobPermissionsBean;
        this.jobInfo = jobInfo;
        this.kindToModules = kindToModules;
        this.selectedFiles = selectedFiles;
        this.selectedJobs = selectedJobs;
        this.kindToInputParameters = kindToInputParameters;
        this.selected = selectedJobs.contains(String.valueOf(jobInfo.getJobNumber()));
        this.level = level;
        this.sequence = sequence;
        this.showExecutionLogs = showExecutionLogs;
        
        _outputFiles = initOutputFiles();
    }
    
    public void setFileSortAscending(final boolean b) {
        this.fileSortAscending = b;
    }

    public void setFileSortColumn(final String s) {
        this.fileSortColumn = s;
    }
    
    private List<OutputFileInfo> _outputFiles = null;
    private List<OutputFileInfo> getOutputFiles() {
        return _outputFiles;
    }
    private List<OutputFileInfo> initOutputFiles() {
        // Build the list of output files from the parameter info array.
        List<OutputFileInfo> outputFiles = new ArrayList<OutputFileInfo>();
        ParameterInfo[] parameterInfoArray = jobInfo.getParameterInfoArray();
        if (parameterInfoArray != null) {
            File outputDir = new File(GenePatternAnalysisTask.getJobDir("" + jobInfo.getJobNumber()));
            for(ParameterInfo param : parameterInfoArray) {
                if (param.isOutputFile()) {
                    boolean isTaskLog = (param.getName().equals(GPConstants.TASKLOG) || param.getName().endsWith(GPConstants.PIPELINE_TASKLOG_ENDING));
                    // is this file an output result of this job, false if it's a result of a child job of this pipeline
                    boolean isChildOutput = true;

                    if (showExecutionLogs || !isTaskLog) {
                        boolean isDeleted = false;
                        File file = new File(outputDir.getParent(), param.getValue());
                        if (!file.exists()) {
                            file = new File(outputDir, param.getName());
                        }
                        
                        if (!file.exists()) {
                            //skip
                            isDeleted = true;
                        }
                        
                        if (!isDeleted) {
                            if (!showPipelineOutputFiles) {
                                File relativePath = GenePatternAnalysisTask.getRelativePath(outputDir, file);
                                if (relativePath == null) {
                                    isChildOutput = false;
                                }
                            }
                            if (isChildOutput || showPipelineOutputFiles) {
                                String kind = SemanticUtil.getKind(file);
                                Collection<TaskInfo> sendToModules = null;

                                if (param.getName().equals(GPConstants.TASKLOG)) {
                                    sendToModules = new ArrayList<TaskInfo>();
                                } 
                                else {
                                    sendToModules = kindToModules.get(kind);
                                }
                                OutputFileInfo pInfo = new OutputFileInfo(param, file, sendToModules, jobInfo.getJobNumber(), kind);
                                totalSize += pInfo.getSize();
                                pInfo.setSelected(selectedFiles.contains(pInfo.getValue()));
                                outputFiles.add(pInfo);
                            }
                        }
                    }
                }
            }
        }

        //sort the list
        Comparator<OutputFileInfo> comparator = getOutputFileComparator(fileSortColumn, fileSortAscending);
        if (comparator != null) {
            Collections.sort(outputFiles, comparator);
        }
        return outputFiles;
    }
    
    private static Comparator<OutputFileInfo> getOutputFileComparator(final String fileSortColumn, final boolean fileSortAscending) {
        if (fileSortColumn == null) {
            return null;
        } 
        else if (fileSortColumn.equals("name")) {
            return new FileNameComparator(fileSortAscending);
        } 
        else if (fileSortColumn.equals("size")) {
            return new FileSizeComparator(fileSortAscending);
        } 
        else if (fileSortColumn.equals("lastModified")) {
            return new FileLastModifiedComparator(fileSortAscending);
        }
        return null;
     }

    private static class FileNameComparator implements Comparator<OutputFileInfo> {
        private boolean fileSortAscending;
        public FileNameComparator(final boolean fileSortAscending) {
            this.fileSortAscending = fileSortAscending;
        }
        public int compare(OutputFileInfo c1, OutputFileInfo c2) {
            return fileSortAscending ? c1.getName().compareToIgnoreCase(c2.getName()) : c2.getName().compareToIgnoreCase(c1.getName());
        }
    }
    private static class FileSizeComparator implements Comparator<OutputFileInfo> {
        private boolean fileSortAscending;
        public FileSizeComparator(final boolean fileSortAscending) {
            this.fileSortAscending = fileSortAscending;
        }
        public int compare(OutputFileInfo c1, OutputFileInfo c2) {
            return fileSortAscending ? new Long(c1.getSize()).compareTo(c2.getSize()) : new Long(c2.getSize()).compareTo(c1.getSize());
        }
    }
    private static class FileLastModifiedComparator implements Comparator<OutputFileInfo> {
        private boolean fileSortAscending;
        public FileLastModifiedComparator(final boolean fileSortAscending) {
            this.fileSortAscending = fileSortAscending;
        }
        public int compare(OutputFileInfo c1, OutputFileInfo c2) {
            return fileSortAscending ? c1.getLastModified().compareTo(c2.getLastModified()) : c2.getLastModified().compareTo(c1.getLastModified());
        }
    }

    private List<JobResultsWrapper> _childJobs = null;
    private List<JobResultsWrapper> _descendantJobs = null;

    private List<JobResultsWrapper> initChildJobs() { 
        List<JobResultsWrapper> childJobs = new ArrayList<JobResultsWrapper>();
        AnalysisDAO ds = new AnalysisDAO();
        //TODO: eliminate recursive calls 
        JobInfo[] children = ds.getChildren(jobInfo.getJobNumber());
        int seq = 1;
        int childLevel = getLevel() + 1;
        for (JobInfo child : children) {
            JobResultsWrapper childJob = new JobResultsWrapper(_jobPermissionsBean, child, kindToModules, selectedFiles, selectedJobs, childLevel, seq, kindToInputParameters, showExecutionLogs);
            childJobs.add(childJob);
            totalSize += childJob.getTotalSize();
            seq++;
        }
        return childJobs;
    }

    public String getFormattedSize() {
	    return JobHelper.getFormattedSize(totalSize);
	}
    
    public long getTotalSize() {
    	return totalSize;
    }
    
    /**
     * @return The list all descendant jobs, basically a flattened tree.
     */
    public List<JobResultsWrapper> getDescendantJobs() {
        if (_descendantJobs == null) {
            _descendantJobs = initDescendantJobs();
        }
        return _descendantJobs;
    }
    
    private List<JobResultsWrapper> initDescendantJobs() {
        List<JobResultsWrapper> descendantJobs = new ArrayList<JobResultsWrapper>();
        List<JobResultsWrapper> childJobs = getChildJobs();
        for (JobResultsWrapper childJob : childJobs) {
            addDescendantJobs(descendantJobs, childJob);
        }
        return descendantJobs;
    }
    
    private void addDescendantJobs(List<JobResultsWrapper> jobList, JobResultsWrapper node) {
        jobList.add(node);
        for(JobResultsWrapper child : node.getChildJobs()) {
            addDescendantJobs(jobList, child);
        }
    }

    public List<OutputFileInfo> getAllFileInfos() {
        List<JobResultsWrapper> childJobs = getChildJobs();
        List<OutputFileInfo> allFiles = new ArrayList<OutputFileInfo>();
        allFiles.addAll(getOutputFiles());
        for (JobResultsWrapper child : childJobs) {
            allFiles.addAll(child.getAllFileInfos());
        }
        return allFiles;
    }

    public List<JobResultsWrapper> getChildJobs() {
        if (_childJobs == null) {
            _childJobs = initChildJobs();
        }
        return _childJobs;
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
        return getOutputFiles();
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

    public String getTruncatedTaskName() {
    	if (getTaskName() != null && getTaskName().length() > 70) {
    		return getTaskName().substring(0, 67)+"...";
    	} else {
    		return getTaskName();
    	}
    }
    
    public String getUserId() {
        return jobInfo.getUserId();
    }
    
    public String getPermissionsLabel() {
        return _jobPermissionsBean.getPermissionsLabel();
    }

    public List<String> getGroupPermissionsWrite() {
        return _jobPermissionsBean.getGroupsWithFullAcess();
    }

    public List<String> getGroupPermissionsReadOnly() {
        return _jobPermissionsBean.getGroupsWithReadOnlyAccess();
    }

    public int getNumGroupPermissionsWrite() {
        return _jobPermissionsBean.getNumGroupsWithFullAccess();
    }
    
    public int getNumGroupPermissionsReadOnly() {
        return _jobPermissionsBean.getNumGroupsWithReadOnlyAccess();
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

    /**
     * Is current user allowed to delete this job?
     */
    public boolean isDeleteAllowed() {
        return _jobPermissionsBean.isDeleteAllowed();
    }

    public String getPermissionInfo() {
    	String info = "Public: " + _jobPermissionsBean.getPublicAccessPermission() + "<br/>";
    	for (GroupPermission perm : _jobPermissionsBean.getGroupAccessPermissions()) {
    		info += perm.getGroupId() + ": " + perm.getPermission() + "<br/>";
    	}
    	return info;
    }

    private Boolean _hasOutputFiles = null;
    private boolean hasOutputFiles() {
        if (_hasOutputFiles != null) {
            return _hasOutputFiles;
        }
        List o = this.getOutputFiles();
        if (o.size() > 0) {
            _hasOutputFiles = true;
            return _hasOutputFiles;
        }
        for(JobResultsWrapper wrapper : getChildJobs()) {
            if (wrapper.hasOutputFiles()) {
                _hasOutputFiles = true;
                return _hasOutputFiles;
            }
        }
        _hasOutputFiles = false;
        return _hasOutputFiles;
    }
    
    /**
     * Flag indicating whether or not to enable the 'download' popup menu for this job.
     * @return
     */
    public boolean isDownloadAllowed() {
        //HACK: quick and dirty way to disable the download menu for a visualizer.
        boolean hasOutputFiles = hasOutputFiles();
        return hasOutputFiles;
    }
}