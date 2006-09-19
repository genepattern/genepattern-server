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

import java.util.Arrays;
import java.util.Comparator;

import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.WebServiceException;

public class JobResultsBean extends AbstractUIBean {
    private JobInfo[] jobs;
    private boolean empty;
    private boolean ascending;
    private String sort;

    public JobResultsBean() {
        updateJobs();
        sort = "Job Number";
        ascending = false;
    }

    private void updateJobs() {
        String userId = getUserId();
        LocalAnalysisClient analysisClient = new LocalAnalysisClient(userId);
        try {
            jobs = analysisClient.getJobs(userId, -1, Integer.MAX_VALUE, false);
        }
        catch (WebServiceException wse) {
            wse.printStackTrace();
        }
        empty = jobs == null || jobs.length == 0;
    }

    public String deleteJobs() {
        String[] deleteJob = getRequest().getParameterValues("deleteJobId");
        LocalAnalysisClient analysisClient = new LocalAnalysisClient(getUserId());
        if (deleteJob != null) {
            for (String job : deleteJob) {
                try {
                    System.out.println("Deleted job " + job);
                    analysisClient.deleteJob(Integer.parseInt(job));
                }
                catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                catch (WebServiceException e) {
                    e.printStackTrace();
                }
            }
        }

        return "Success";
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public boolean isAscending() {
        return ascending;
    }

    public void setAscending(boolean ascending) {
        this.ascending = ascending;

    }

    public void sort(final String column) {
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
        Arrays.sort(jobs, comparator);
    }

    public JobInfo[] getJobs() {
        sort(getSort());
        return jobs;
    }

    public void setJobs(JobInfo[] jobs) {
        this.jobs = jobs;
    }

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }
}
