package org.genepattern.server.webapp;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadException;
import org.genepattern.server.executor.JobSubmissionException;
import org.genepattern.server.webapp.jsf.JobBean;
import org.genepattern.server.webapp.jsf.PageMessages;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * 
 * @author pcarr
 * @deprecated - in favor of newer job submit form.
 */
class SubmitJobServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private String lsid = null;
    
    private void redirectToHome(HttpServletResponse response) throws IOException {
        if (lsid != null) {
            response.sendRedirect("/gp/pages/index.jsf?lsid=" + URLEncoder.encode(lsid, "UTF-8"));
        }
        else {
            response.sendRedirect("/gp/");
        }
    }
    
    private void setErrorMessage(HttpServletRequest request, String message) {
        request.getSession().setAttribute(PageMessages.ERROR_MESSAGE_KEY, message);
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doPost(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String userID = (String) request.getSession().getAttribute(GPConstants.USERID);
        lsid = (String) request.getSession().getAttribute(GPConstants.LSID);
        
        if (userID == null) {
            response.sendRedirect("/gp/pages/notFound.jsf");
            return;
        }
        
        RunTaskHelper runTaskHelper = null;
        TaskInfo task = null;
        List<ParameterInfo> missingReqParams = new ArrayList<ParameterInfo>();
        try {
            runTaskHelper = new RunTaskHelper(userID, request);
            task = runTaskHelper.getTaskInfo();
            if (task == null) {
                setErrorMessage(request, "Unable to find module");
                redirectToHome(response);
                return;
            }
            missingReqParams = runTaskHelper.getMissingParameters();
            // Check for unmatched batch params here then add to missingReqParams if true
        }
        catch (FileUploadException e) {
            setErrorMessage(request, e.getMessage() + " Please resubmit the job.");
            redirectToHome(response);
            return;
        }
        
        if (missingReqParams.size() > 0) {
            String params = "";
            for (int i=0; i < missingReqParams.size(); i++){
                ParameterInfo pinfo = missingReqParams.get(i);
                params += pinfo.getName().replaceAll(".", " ") + " ";
            }
            setErrorMessage(request, "The module could not be run. The following required parameters need to have values provided: " + params);
            redirectToHome(response);
            return;
        }
 
        if (runTaskHelper.isBatchJob()) {
            request.getSession().setAttribute(JobBean.DISPLAY_BATCH, runTaskHelper.getBatchJob().getId());
            response.sendRedirect("/gp/jobResults");
        }
        else {
            RunJobFromJsp runner = new RunJobFromJsp();
            runner.setUserId(userID);
            runner.setTaskInfo(task);
            String jobId;
            try {
                jobId = runner.submitJob();
            }
            catch (JobSubmissionException e) {
                setErrorMessage(request, "Error submitting job");
                redirectToHome(response);
                return;
            }
            if (jobId == null) {
                response.sendRedirect("/gp/jobResults");
            }
            else {
                response.sendRedirect("/gp/jobResults/"+jobId + "?openVisualizers=true");
            }
        }
    }
    
}
