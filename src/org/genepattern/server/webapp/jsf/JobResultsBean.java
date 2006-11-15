/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp.jsf;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.WebServiceException;

public class JobResultsBean {
    private JobResultInfo[] jobResults;
    private boolean ascending;
    private boolean warnBeforeDeletingJobs;
    private boolean showAllJobs = true;
    
    List checkbox = new ArrayList();
    
    /** name of column we're sorting on */
    private String sort;
    /** column that was last used for sorting */
    private String lastSort;

    public JobResultsBean() {
        updateJobs();
        warnBeforeDeletingJobs = true; // TODO get from property
        showAllJobs = false; // TODO get from property

    }

    private void updateJobs() {
        String userId = UIBeanHelper.getUserId();
        LocalAnalysisClient analysisClient = new LocalAnalysisClient(userId);
        try {
            JobInfo[] jobs = analysisClient.getJobs(showAllJobs ? null : userId, -1, Integer.MAX_VALUE, false);
            jobResults = new JobResultInfo[jobs.length];
            for(int i=0; i<jobs.length; i++) {
                jobResults[i] = new JobResultInfo(jobs[i]);
            }
        }
        catch (WebServiceException wse) {
            wse.printStackTrace();
        }
    }
    
    public String delete() {
    	String [] selectedJobs = UIBeanHelper.getRequest().getParameterValues("selectedJobs");
    	String [] selectedFiles = UIBeanHelper.getRequest().getParameterValues("selectedFiles");
    	return null;
    }
    
    public String download() {
    	String [] selectedJobs = UIBeanHelper.getRequest().getParameterValues("selectedJobs");
    	String [] selectedFiles = UIBeanHelper.getRequest().getParameterValues("selectedFiles");
    	return null;
    }

    public void deleteJobs() {
        String[] deleteJob = UIBeanHelper.getRequest().getParameterValues("deleteJobId");
        LocalAnalysisClient analysisClient = new LocalAnalysisClient(UIBeanHelper.getUserId());
        List<Integer> jobErrors = new ArrayList<Integer>();

        if (deleteJob != null) {
            for (String job : deleteJob) {
                int jobNumber = Integer.parseInt(job);
                try {
                    analysisClient.deleteJob(jobNumber);
                    System.out.println("deleted job " + jobNumber);
                }
                catch (NumberFormatException e) {
                    e.printStackTrace(); // ignore
                }
                catch (WebServiceException e) {
                    jobErrors.add(jobNumber);
                }
            }
        }
        StringBuffer sb = new StringBuffer();

        for (int i = 0, size = jobErrors.size(); i < size; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(jobErrors.get(i));
        }
        if (jobErrors.size() > 0) {
            String msg = "An error occurred while deleting job(s) " + sb.toString() + ".";
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, msg));
        }
        updateJobs();
        lastSort = null;
        sort();

    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
        sort();
    }

    public boolean isAscending() {
        return ascending;
    }

    public void setAscending(boolean ascending) {
        this.ascending = ascending;

    }

    public void sort() {
        final String column = getSort();
        Comparator comparator = new Comparator() {

            public int compare(Object o1, Object o2) {
                JobInfo c1 = (JobInfo) o1;
                JobInfo c2 = (JobInfo) o2;
                if (column == null) {
                    return 0;
                }
                else if (column.equals("Job Number")) {
                    return ascending ? new Integer(c1.getJobNumber()).compareTo(c2.getJobNumber()) : new Integer(c2
                            .getJobNumber()).compareTo(c1.getJobNumber());
                }
                else if (column.equals("Name")) {
                    return ascending ? c1.getTaskName().compareTo(c2.getTaskName()) : c2.getTaskName().compareTo(
                            c1.getTaskName());
                }
                else if (column.equals("Status")) {
                    return ascending ? c1.getStatus().compareTo(c2.getStatus()) : c2.getStatus().compareTo(
                            c1.getStatus());
                }
                else if (column.equals("Completed")) {
                    return ascending ? c1.getDateCompleted().compareTo(c2.getDateCompleted()) : c2.getDateCompleted()
                            .compareTo(c1.getDateCompleted());
                }
                else if (column.equals("Submitted")) {
                    return ascending ? c1.getDateSubmitted().compareTo(c2.getDateSubmitted()) : c2.getDateSubmitted()
                            .compareTo(c1.getDateSubmitted());
                }
                else {
                    return 0;
                }
            }
        };
        if (column != null && !column.equals(lastSort)) {
            Arrays.sort(jobResults, comparator);
        }
        lastSort = column;

    }

    public JobResultInfo[] getJobResults() {
        return jobResults;
    }

    public boolean isShowAllJobs() {
        return showAllJobs;
    }

    public void setShowAllJobs(boolean showAllJobs) {
        this.showAllJobs = showAllJobs;
    }

    public boolean isWarnBeforeDeletingJobs() {
        return warnBeforeDeletingJobs;
    }

    public void setWarnBeforeDeletingJobs(boolean warnBeforeDeletingJobs) {
        this.warnBeforeDeletingJobs = warnBeforeDeletingJobs;
    }
    
    public static class JobResultInfo {
        JobInfo jobInfo;
        List<FileInfo> outputFiles;
        boolean expanded = true;
        
        public JobResultInfo(JobInfo jobInfo) {
            this.jobInfo = jobInfo;
            List<File> files = JobHelper.getOutputFiles(jobInfo);
            outputFiles = new ArrayList<FileInfo>(files.size());
            for(File file : files) {
                outputFiles.add(new FileInfo(file));
            }
        }
        
        public JobInfo getJobInfo() {
            return jobInfo;
        }
        
        public List<FileInfo> getOutputFiles() {
            return outputFiles;
        }  
        
        public boolean isExpanded() {
            return expanded;
        }
    }
    
    public static class FileInfo {
        String name;
        String absolutePath;
        long size;
        Date lastModified;
        boolean exists;
        
        public FileInfo(File file) {
            this.name = file.getName();
            this.size = file.length();
            this.exists = file.exists();
            this.absolutePath = file.getAbsolutePath();           
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(file.lastModified());
            this.lastModified = cal.getTime();
        }
        
        public String getName() {
            return name;
        }
        
        public String getFormattedSize() {            
            NumberFormat nf = NumberFormat.getInstance();
            return nf.format(Math.max(1, size / 1000)) + "k";
        }
        
        public Date getLastModified() {
            return lastModified;
        }
        
        public String getAbsolutePath() {
        	return absolutePath;
        }
        
        
        
        
        
    }

    public List getCheckbox() {
        return checkbox;
    }

    public void setCheckbox(List checkboxList) {
        this.checkbox = checkboxList;
    }
}
