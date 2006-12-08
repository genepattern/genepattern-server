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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.component.UISelectBoolean;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

public class JobResultsBean extends JobBean {
    private static Logger log = Logger.getLogger(JobResultsBean.class);

    /**
     * Specifies job column to sort on. Possible values are jobNumber taskName
     * dateSubmitted dateCompleted status
     */
    private String jobSortColumn = "jobNumber";

    /**
     * Specifies file column to sort on. Possible values are name size
     * lastModified
     */
    private String fileSortColumn = "name";

    /**
     * File sort direction (true for ascending, false for descending)
     */
    private boolean fileSortAscending = true;

    private boolean showEveryonesJobs = false;

    private boolean showExecutionLogs = true;

    /**
     * Get the job infos to display. This might be called before the model is
     * updated.
     */
    protected JobInfo[] getJobInfos() {

        String userId = UIBeanHelper.getUserId();
        LocalAnalysisClient analysisClient = new LocalAnalysisClient(userId);
        try {
            JobInfo[] jobs = analysisClient.getJobs(showEveryonesJobs ? null : userId, -1, Integer.MAX_VALUE, false);
            return jobs;
        }
        catch (WebServiceException wse) {
            log.error(wse);
            return new JobInfo[0];
        }

    }

    /**
     * Delete the selected jobs and files.
     * 
     * @return
     */
    public String delete() {
        String[] selectedJobs = UIBeanHelper.getRequest().getParameterValues("selectedJobs");
        deleteJobs(selectedJobs);

        String[] selectedFiles = UIBeanHelper.getRequest().getParameterValues("selectedFiles");
        for (String jobFileName : selectedFiles) {
            deleteFile(jobFileName);
        }

        return null;

    }

    /**
     * Delete a list of jobs.
     * 
     * @param jobNumbers
     */
    private void deleteJobs(String[] jobNumbers) {
        LocalAnalysisClient analysisClient = new LocalAnalysisClient(UIBeanHelper.getUserId());
        List<Integer> jobErrors = new ArrayList<Integer>();

        if (jobNumbers != null) {
            for (String job : jobNumbers) {
                int jobNumber = Integer.parseInt(job);
                try {
                    deleteJob(jobNumber);
                }
                catch (NumberFormatException e) {
                    log.error(e);
                }
            }
        }

        // Create error messages
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

    }

    /**
     * Action method. First sorts jobs, then for each job sorts files.
     * 
     * @return
     */
    public String sort() {

        sortJobs();
        sortFiles();
        return null;
    }

    private void sortJobs() {
        final String column = getJobSortColumn();
        Comparator comparator = new Comparator() {

            public int compare(Object o1, Object o2) {
                MyJobInfo c1 = ((MyJobInfo) o1);
                MyJobInfo c2 = ((MyJobInfo) o2);
                if (column == null) {
                    return 0;
                }
                else if (column.equals("jobNumber")) {
                    return jobSortAscending ? new Integer(c1.getJobNumber()).compareTo(c2.getJobNumber())
                            : new Integer(c2.getJobNumber()).compareTo(c1.getJobNumber());
                }
                else if (column.equals("taskName")) {
                    return jobSortAscending ? c1.getTaskName().compareTo(c2.getTaskName()) : c2.getTaskName()
                            .compareTo(c1.getTaskName());
                }
                else if (column.equals("status")) {
                    return jobSortAscending ? c1.getStatus().compareTo(c2.getStatus()) : c2.getStatus().compareTo(
                            c1.getStatus());
                }
                else if (column.equals("dateCompleted")) {
                    return jobSortAscending ? c1.getDateCompleted().compareTo(c2.getDateCompleted()) : c2
                            .getDateCompleted().compareTo(c1.getDateCompleted());
                }
                else if (column.equals("dateSubmitted")) {
                    return jobSortAscending ? c1.getDateSubmitted().compareTo(c2.getDateSubmitted()) : c2
                            .getDateSubmitted().compareTo(c1.getDateSubmitted());
                }
                else {
                    return 0;
                }
            }
        };
        Collections.sort(getJobs(), comparator);
    }

    private void sortFiles() {
        final String column = getFileSortColumn();
        Comparator comparator = new Comparator() {

            public int compare(Object o1, Object o2) {
                MyParameterInfo c1 = (MyParameterInfo) o1;
                MyParameterInfo c2 = (MyParameterInfo) o2;
                if (column == null) {
                    return 0;
                }
                else if (column.equals("name")) {
                    return fileSortAscending ? c1.getName().compareTo(c2.getName()) : c2.getName().compareTo(
                            c1.getName());
                }
                else if (column.equals("size")) {
                    return fileSortAscending ? new Long(c1.getSize()).compareTo(c2.getSize()) : new Long(c2.getSize())
                            .compareTo(c1.getSize());
                }
                else if (column.equals("lastModified")) {
                    return fileSortAscending ? c1.getLastModified().compareTo(c2.getLastModified()) : c2
                            .getLastModified().compareTo(c1.getLastModified());
                }

                else {
                    return 0;
                }
            }
        };

        for (MyJobInfo jobResult : getJobs()) {
            Collections.sort(jobResult.getOutputFileParameterInfos(), comparator);
        }

    }

    public boolean isShowExecutionLogs() {
        return showExecutionLogs;
    }

    public String getJobSortColumn() {
        return jobSortColumn;
    }

    public void setJobSortColumn(String jobSortField) {
        this.jobSortColumn = jobSortField;
    }

    public String getFileSortColumn() {
        return fileSortColumn;
    }

    public void setFileSortColumn(String fileSortField) {
        this.fileSortColumn = fileSortField;
    }

    public boolean isFileSortAscending() {
        return fileSortAscending;
    }

    public void setFileSortAscending(boolean fileSortAscending) {
        this.fileSortAscending = fileSortAscending;
    }

    public boolean isJobSortAscending() {
        return jobSortAscending;
    }

    public void setJobSortAscending(boolean jobSortAscending) {
        this.jobSortAscending = jobSortAscending;
    }

    public void setShowExecutionLogs(boolean showExecutionLogs) {
        this.showExecutionLogs = showExecutionLogs;
    }

    public boolean isShowEveryonesJobs() {
        return showEveryonesJobs;
    }

    public void setShowEveryonesJobs(boolean showEveryonesJobs) {
        this.showEveryonesJobs = showEveryonesJobs;
        this.updateJobs();
    }

}
