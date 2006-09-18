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

import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.WebServiceException;

public class JobResultsBean extends AbstractUIBean {
    private JobInfo[] jobs;
    private boolean empty;

    public JobResultsBean() {
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

    public JobInfo[] getJobs() {
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
