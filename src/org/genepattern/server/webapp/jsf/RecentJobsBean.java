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

import javax.faces.event.ActionEvent;

import org.apache.log4j.Logger;
import org.apache.myfaces.custom.navmenu.jscookmenu.HtmlCommandJSCookMenu;
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
//            jobs = analysisClient.getJobs(userId, -1, recentJobsToShow, false);
            jobs = analysisClient.getJobs(null, -1, recentJobsToShow, true);
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

    public String reload(ActionEvent event) {
        LocalAnalysisClient ac = new LocalAnalysisClient(UIBeanHelper
                .getUserId());
        HtmlCommandJSCookMenu m = (HtmlCommandJSCookMenu) event.getSource();
        int jobNumber = Integer.parseInt(m.getValue().toString());
        try {
            JobInfo reloadJob = ac.getJob(jobNumber);
            ModuleChooserBean chooser = (ModuleChooserBean) UIBeanHelper
            .getManagedBean("#{moduleChooserBean}");
            assert chooser != null;
            chooser.setSelectedModule(reloadJob.getTaskLSID());  
        } catch (WebServiceException e) {
            log.error(e);
        }
        return "run task";
    }
    
    public String delete(ActionEvent event) {
        HtmlCommandJSCookMenu m = (HtmlCommandJSCookMenu) event.getSource();
        int jobNumber = Integer.parseInt(m.getValue().toString());
        LocalAnalysisClient ac = new LocalAnalysisClient(UIBeanHelper
                .getUserId());
        try {
            ac.deleteJob(jobNumber);
        } catch (WebServiceException e) {
            log.error(e);
        }
        return "run task";

    }
    
    public void viewCode(ActionEvent event) {
        HtmlCommandJSCookMenu m = (HtmlCommandJSCookMenu) event.getSource();
        int jobNumber = Integer.parseInt(m.getValue().toString());
        LocalAnalysisClient ac = new LocalAnalysisClient(UIBeanHelper
                .getUserId());

    }

}
