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
import java.util.Date;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.apache.log4j.Logger;
import org.apache.myfaces.custom.navmenu.jscookmenu.HtmlCommandJSCookMenu;
import org.genepattern.codegenerator.CodeGeneratorUtil;
import org.genepattern.server.user.UserPropKey;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.WebServiceException;

public class RecentJobsBean {
    private MyJobInfo[] jobs;

    private static Logger log = Logger.getLogger(RecentJobsBean.class);

    public RecentJobsBean() {
        updateJobs();
    }

    private void updateJobs() {
        String userId = UIBeanHelper.getUserId();
        assert userId != null;
        int recentJobsToShow = Integer.parseInt(UserPrefsBean.getProp(
                UserPropKey.RECENT_JOBS_TO_SHOW, "4").getValue());
        LocalAnalysisClient analysisClient = new LocalAnalysisClient(userId);
        try {
            // jobs = analysisClient.getJobs(userId, -1, recentJobsToShow,
            // false); FIXME uncomment
            JobInfo[] temp = analysisClient.getJobs(null, -1, recentJobsToShow,
                    true);
            jobs = new MyJobInfo[temp.length];
            for (int i = 0; i < jobs.length; i++) {
                jobs[i] = new MyJobInfo(temp[i]);
            }
        } catch (WebServiceException wse) {
            log.error(wse);
        }
    }

    public int getSize() {
        return jobs == null ? 0 : jobs.length;
    }

    public MyJobInfo[] getJobs() {
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
            // FIXME update default parameters
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
            updateJobs(); // TODO don't retrieve jobs twice from the database
        } catch (WebServiceException e) {
            log.error(e);
        }
        return "run task";

    }

    public void viewCode(ActionEvent event) {
        HtmlCommandJSCookMenu m = (HtmlCommandJSCookMenu) event.getSource();
        int jobNumber = Integer.parseInt(m.getValue().toString());
        try {
            String code = CodeGeneratorUtil.getCode(CodeGeneratorUtil.LANGUAGE.JAVA,
                    new AnalysisJob(UIBeanHelper.getUserId(),
                            new LocalAnalysisClient(UIBeanHelper.getUserId())
                                    .getJob(jobNumber)));
        } catch (Exception e) {
            log.error(e);
        }

    }

    public static class MyJobInfo {
        private JobInfo jobInfo;

        private List<ParameterInfo> outputFiles;

        public MyJobInfo(JobInfo jobInfo) {
            this.jobInfo = jobInfo;
            outputFiles = new ArrayList<ParameterInfo>();
            ParameterInfo[] parameterInfoArray = jobInfo
                    .getParameterInfoArray();

            for (int i = 0; i < parameterInfoArray.length; i++) {
                if (parameterInfoArray[i].isOutputFile()) {
                    outputFiles.add(parameterInfoArray[i]);
                    // get modules for output file
                }
            }
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

        public List<ParameterInfo> getOutputFileParameterInfos() {
            return outputFiles;
        }

        public ParameterInfo[] getParameterInfoArray() {
            return jobInfo.getParameterInfoArray();
        }

        public String getStatus() {
            return jobInfo.getStatus();
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
    }

}
