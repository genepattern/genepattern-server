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
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.user.UserPropKey;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.WebServiceException;

public class RecentJobsBean extends JobBean {

    private static Logger log = Logger.getLogger(RecentJobsBean.class);

    protected JobInfo[] getJobInfos() {
        String userId = UIBeanHelper.getUserId();
        assert userId != null;
        int recentJobsToShow = Integer.parseInt(new UserDAO().getPropertyValue(userId, UserPropKey.RECENT_JOBS_TO_SHOW,
                "4"));
        LocalAnalysisClient analysisClient = new LocalAnalysisClient(userId);
        try {
            return analysisClient.getJobs(userId, -1, recentJobsToShow, false);
        } catch (WebServiceException wse) {
            log.error(wse);
            return new JobInfo[0];
        }
    }

}
