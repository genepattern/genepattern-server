/*******************************************************************************
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright (2003-2008) by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are
 * reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 *
 *******************************************************************************/
package org.genepattern.server.webapp.jsf.jobinfo;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.server.JobInfoManager;
import org.genepattern.server.JobInfoWrapper;
import org.genepattern.server.webapp.jsf.UIBeanHelper;

/**
 * Access job status for a single job result from a JSF page.
 * 
 * @author pcarr
 */
public class JobStatusBean {
    private static Logger log = Logger.getLogger(JobStatusBean.class);
    
    private int jobNumber = -1;
    private JobInfoWrapper jobInfoWrapper = null;
    private List<JobInfoWrapper> allSteps = null;

    public JobStatusBean() {
        String jobNumberParameter = null;

        try {
            jobNumberParameter = UIBeanHelper.getRequest().getParameter("jobNumber");
            jobNumberParameter = UIBeanHelper.decode(jobNumberParameter);
            jobNumber = Integer.parseInt(jobNumberParameter);
        }
        catch (NumberFormatException e1) {
            log.error("Invalid value for request parameter, 'jobNumber':  "+jobNumberParameter, e1);
            return;
        }
        
        String userId = UIBeanHelper.getUserId();

        HttpServletRequest request = UIBeanHelper.getRequest();
        String contextPath = request.getContextPath();
        String cookie = request.getHeader("Cookie");
        
        JobInfoManager jobInfoManager = new JobInfoManager();
        this.jobInfoWrapper = jobInfoManager.getJobInfo(cookie, contextPath, userId, jobNumber);
    }
    
    public JobInfoWrapper getJobInfo() {
        return jobInfoWrapper;
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
    

}
