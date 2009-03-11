/*******************************************************************************
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright (2003-2009) by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are
 * reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 *
 *******************************************************************************/
package org.genepattern.server.webapp.jsf.jobinfo;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.server.JobInfoManager;
import org.genepattern.server.JobInfoWrapper;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.util.EmailNotificationManager;
import org.genepattern.server.webapp.jsf.UIBeanHelper;

/**
 * Access job status for a single job result from a JSF page.
 * 
 * @author pcarr
 */
public class JobStatusBean {
    private static Logger log = Logger.getLogger(JobStatusBean.class);
    
    private JobInfoWrapper jobInfoWrapper = null;
    private List<JobInfoWrapper> allSteps = null;
    private String currentUserId = null;
    private String currentUserEmail = null;

    private boolean finished = false;
    private boolean sendEmailNotification = false;
    private boolean showExecutionLogs = false;
    private boolean openVisualizers = false;

    public JobStatusBean() {
        String jobNumberParameter = null;

        int jobNumber = -1;
        try {
            jobNumberParameter = UIBeanHelper.getRequest().getParameter("jobNumber");
            jobNumberParameter = UIBeanHelper.decode(jobNumberParameter);
            jobNumber = Integer.parseInt(jobNumberParameter);
            String openVisualizersParameter = UIBeanHelper.getRequest().getParameter("openVisualizers");
            setOpenVisualizers(openVisualizersParameter != null);
        }
        catch (NumberFormatException e1) {
            log.error("Invalid value for request parameter, 'jobNumber':  "+jobNumberParameter, e1);
            return;
        }
        
        currentUserId = UIBeanHelper.getUserId();
        try {
            UserDAO userDao = new UserDAO();
            User user = userDao.findById(currentUserId);
            if (user != null) {
                currentUserEmail = user.getEmail();
                showExecutionLogs = userDao.getPropertyShowExecutionLogs(currentUserId);
            }

            String key = getEmailNotificationPropKey();
            if (key != null) {
                String propValue = userDao.getPropertyValue(currentUserId, key, String.valueOf(sendEmailNotification));
                sendEmailNotification = Boolean.valueOf(propValue);
            }
        }
        catch (Exception e) {
            log.error("Unable to initialize email notification for user: '"+currentUserId+"': "+e.getLocalizedMessage(), e);
        }

        HttpServletRequest request = UIBeanHelper.getRequest();
        String contextPath = request.getContextPath();
        String cookie = request.getHeader("Cookie");
        
        JobInfoManager jobInfoManager = new JobInfoManager();
        this.jobInfoWrapper = jobInfoManager.getJobInfo(cookie, contextPath, currentUserId, jobNumber);
        this.finished = 
            JobStatus.FINISHED.equals(this.jobInfoWrapper.getStatus()) ||
            JobStatus.ERROR.equals(this.jobInfoWrapper.getStatus());
    }
    
    public JobInfoWrapper getJobInfo() {
        return jobInfoWrapper;
    }

	public boolean getOpenVisualizers() {
		return openVisualizers;
	}

	public void setOpenVisualizers(boolean openVisualizers) {
		this.openVisualizers = openVisualizers;
	}

    /**
     * @return the top level job info, including all steps if it is a pipeline.
     */
    public List<JobInfoWrapper> getAllSteps() {
        if (allSteps == null) {
            allSteps = jobInfoWrapper.getAllSteps();
            allSteps.add(0, jobInfoWrapper);
        }
        return allSteps;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isShowExecutionLogs() {
        return this.showExecutionLogs;
    }
    
    public boolean isSendEmailNotification() {
        return sendEmailNotification;
    }

    public void setSendEmailNotification(boolean b) {
        this.sendEmailNotification = b;
        //validate
        String key = getEmailNotificationPropKey();
        if (key == null) {
            return;
        }
        //save state
        String value = String.valueOf(this.sendEmailNotification);
        UserDAO userDao = new UserDAO();        
        userDao.setProperty(currentUserId, key, value);
        //send notification
        int jobNumber = this.getJobInfo().getJobNumber();
        if (sendEmailNotification) {
            EmailNotificationManager.getInstance().addWaitingUser(this.currentUserEmail, this.currentUserId, ""+jobNumber);
        } 
        else {
            EmailNotificationManager.getInstance().removeWaitingUser(this.currentUserEmail, this.currentUserId, ""+jobNumber);
        }
    }
    
    private String getEmailNotificationPropKey() {
        if (jobInfoWrapper == null) {
            log.error("JobStatusBean.setEmailNotification with null jobInfoWrapper");
            return null;
        }
        if (jobInfoWrapper.getJobNumber() < 0) {
            log.error("JobStatusBean.setEmailNotification with invalid jobNumber: "+jobInfoWrapper.getJobNumber());
            return null;
        }
        if (currentUserId == null) {
            log.error("JobStatusBean.setEmailNotification with null currentUserId");
            return null;
        }
        if (currentUserEmail == null) {
            //TODO: handle special case when the current user has not yet set an email address
            return null;
        }
        
        return "sendEmailNotification_" + this.getJobInfo().getJobNumber();
    }
    
    /**
     * @return the userId of the logged in user,
     *         not necessarily the same as the owner of the job.
     */
    public String getCurrentUserId() {
        return currentUserId;
    }
    
    /**
     * @return the email address of the logged in user,
     *         not necessarily the same as the owner of the job.
     */
    public String getCurrentUserEmail() {
        return currentUserEmail;        
    }
}
