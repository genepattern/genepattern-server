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

import org.apache.log4j.Logger;
import org.genepattern.server.user.UserPropKey;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.WebServiceException;

public class RecentJobsBean {
    private JobInfo[] jobs;

    private static Logger log = Logger.getLogger(RecentJobsBean.class);

    public RecentJobsBean() {
        String userId = UIBeanHelper.getUserId();
        int recentJobsToShow = Integer.parseInt(UserPrefsBean.getProp(
                UserPropKey.RECENT_JOBS_TO_SHOW, "4").getValue());
        LocalAnalysisClient analysisClient = new LocalAnalysisClient(userId);
        try {
            jobs = analysisClient.getJobs(userId, -1, recentJobsToShow, false);
        } catch (WebServiceException wse) {
            log.error(wse);
        }
    }

    public int getSize() {
        return jobs == null ? 0 : jobs.length;
    }

    public JobInfo[] getJobs() {
        return jobs;
    }

}
