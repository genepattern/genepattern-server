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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.user.UserProp;
import org.genepattern.server.util.AuthorizationManagerFactoryImpl;
import org.genepattern.server.util.IAuthorizationManager;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.WebServiceException;

public class JobResultsBean extends JobBean {
    private static Logger log = Logger.getLogger(JobResultsBean.class);

    /**
     * File sort direction (true for ascending, false for descending)
     */
    private boolean fileSortAscending = true;

    /**
     * Specifies file column to sort on. Possible values are name size
     * lastModified
     */
    private String fileSortColumn = "name";

    private boolean showEveryonesJobs = true;

    /**
     * Specifies job column to sort on. Possible values are jobNumber taskName
     * dateSubmitted dateCompleted status
     */
    private String jobSortColumn = "jobNumber";

    /**
     * Job sort direction (true for ascending, false for descending)
     */
    private boolean jobSortAscending = true;

    public JobResultsBean() {
        String userId = UIBeanHelper.getUserId();
        this.fileSortAscending = Boolean.valueOf(new UserDAO().getPropertyValue(userId, "fileSortAscending", String
                .valueOf(fileSortAscending)));
        this.fileSortColumn = new UserDAO().getPropertyValue(userId, "fileSortColumn", fileSortColumn);
        this.showEveryonesJobs = Boolean.valueOf(new UserDAO().getPropertyValue(userId, "showEveryonesJobs", String
                .valueOf(showEveryonesJobs)));
        if (showEveryonesJobs
                && !new AuthorizationManagerFactoryImpl().getAuthorizationManager().checkPermission(
                        "administrateServer", UIBeanHelper.getUserId())) {
            showEveryonesJobs = false;

        }
        this.jobSortColumn = new UserDAO().getPropertyValue(userId, "jobSortColumn", jobSortColumn);
        this.jobSortAscending = Boolean.valueOf(new UserDAO().getPropertyValue(userId, "jobSortAscending", String
                .valueOf(jobSortAscending)));

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
        if (selectedFiles != null) {
            for (String jobFileName : selectedFiles) {
                deleteFile(jobFileName);
            }
        }
        this.updateJobs();
        return null;

    }

    public String getFileSortColumn() {
        return fileSortColumn;
    }

    public String getJobSortColumn() {
        return jobSortColumn;
    }

    public boolean isFileSortAscending() {
        return fileSortAscending;
    }

    public boolean isShowEveryonesJobs() {
        return showEveryonesJobs;
    }

    public void setFileSortAscending(boolean fileSortAscending) {
        this.fileSortAscending = fileSortAscending;
        new UserDAO().setProperty(UIBeanHelper.getUserId(), "fileSortAscending", String.valueOf(fileSortAscending));
    }

    public void setFileSortColumn(String fileSortField) {
        this.fileSortColumn = fileSortField;
        new UserDAO().setProperty(UIBeanHelper.getUserId(), "fileSortColumn", String.valueOf(fileSortField));

    }

    public void setJobSortAscending(boolean jobSortAscending) {
        this.jobSortAscending = jobSortAscending;
        new UserDAO().setProperty(UIBeanHelper.getUserId(), "jobSortAscending", String.valueOf(jobSortAscending));

    }

    public void setJobSortColumn(String jobSortField) {
        this.jobSortColumn = jobSortField;
        new UserDAO().setProperty(UIBeanHelper.getUserId(), "jobSortColumn", String.valueOf(jobSortColumn));
    }

    public void setShowEveryonesJobs(boolean showEveryonesJobs) {
        if (showEveryonesJobs
                && !new AuthorizationManagerFactoryImpl().getAuthorizationManager().checkPermission(
                        "administrateServer", UIBeanHelper.getUserId())) {
            showEveryonesJobs = false;

        }
        this.showEveryonesJobs = showEveryonesJobs;
        new UserDAO().setProperty(UIBeanHelper.getUserId(), "showEveryonesJobs", String.valueOf(showEveryonesJobs));
        this.updateJobs();
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
        } catch (WebServiceException wse) {
            log.error(wse);
            return new JobInfo[0];
        }

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
                } catch (NumberFormatException e) {
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

    private void sortFiles() {
        final String column = getFileSortColumn();
        Comparator comparator = new Comparator() {

            public int compare(Object o1, Object o2) {
                OutputFileInfo c1 = (OutputFileInfo) o1;
                OutputFileInfo c2 = (OutputFileInfo) o2;
                if (column == null) {
                    return 0;
                } else if (column.equals("name")) {
                    return fileSortAscending ? c1.getName().compareTo(c2.getName()) : c2.getName().compareTo(
                            c1.getName());
                } else if (column.equals("size")) {
                    return fileSortAscending ? new Long(c1.getSize()).compareTo(c2.getSize()) : new Long(c2.getSize())
                            .compareTo(c1.getSize());
                } else if (column.equals("lastModified")) {
                    return fileSortAscending ? c1.getLastModified().compareTo(c2.getLastModified()) : c2
                            .getLastModified().compareTo(c1.getLastModified());
                }

                else {
                    return 0;
                }
            }
        };

        for (JobResultsWrapper jobResult : getJobs()) {
            sortFilesRecursive(jobResult, comparator);
        }

    }

    private void sortFilesRecursive(JobResultsWrapper jobResult, Comparator comparator) {
        Collections.sort(jobResult.getOutputFileParameterInfos(), comparator);
        for (JobResultsWrapper child : jobResult.getChildJobs()) {
            sortFilesRecursive(child, comparator);
        }

    }

    private void sortJobs() {
        final String column = getJobSortColumn();
        Comparator comparator = new Comparator() {

            public int compare(Object o1, Object o2) {
                JobResultsWrapper c1 = ((JobResultsWrapper) o1);
                JobResultsWrapper c2 = ((JobResultsWrapper) o2);
                if (column == null) {
                    return 0;
                } else if (column.equals("jobNumber")) {
                    return jobSortAscending ? new Integer(c1.getJobNumber()).compareTo(c2.getJobNumber())
                            : new Integer(c2.getJobNumber()).compareTo(c1.getJobNumber());
                } else if (column.equals("taskName")) {
                    return jobSortAscending ? c1.getTaskName().compareTo(c2.getTaskName()) : c2.getTaskName()
                            .compareTo(c1.getTaskName());
                } else if (column.equals("status")) {
                    return jobSortAscending ? c1.getStatus().compareTo(c2.getStatus()) : c2.getStatus().compareTo(
                            c1.getStatus());
                } else if (column.equals("dateCompleted")) {
                    return jobSortAscending ? c1.getDateCompleted().compareTo(c2.getDateCompleted()) : c2
                            .getDateCompleted().compareTo(c1.getDateCompleted());
                } else if (column.equals("dateSubmitted")) {
                    return jobSortAscending ? c1.getDateSubmitted().compareTo(c2.getDateSubmitted()) : c2
                            .getDateSubmitted().compareTo(c1.getDateSubmitted());
                } else {
                    return 0;
                }
            }
        };
        Collections.sort(getJobs(), comparator);
    }

    public boolean isJobSortAscending() {
        return jobSortAscending;
    }

}
