package org.genepattern.server.webapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadException;
import org.genepattern.server.executor.JobSubmissionException;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

public class SubmitJobServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doPost(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String userID = (String) request.getAttribute(GPConstants.USERID);
        
        if (userID == null) {
            response.sendRedirect("/gp/pages/notFound.jsf");
            return;
        }
        
        RunTaskHelper runTaskHelper = null;
        TaskInfo task = null;
        List<ParameterInfo> missingReqParams = new ArrayList<ParameterInfo>();
        FileUploadException fileUploadException = null;
        try {
            runTaskHelper = new RunTaskHelper(userID, request);
            task = runTaskHelper.getTaskInfo();
            if (task == null) {
                UIBeanHelper.setErrorMessage("Unable to find module");
                response.sendRedirect("/gp/");
                return;
            }
            missingReqParams = runTaskHelper.getMissingParameters();
            // Check for unmatched batch params here then add to missingReqParams if true
        }
        catch (FileUploadException e) {
            fileUploadException = e;
        }
        
        if (fileUploadException != null || missingReqParams.size() > 0) {
            if (missingReqParams.size() > 0) {
                String params = "";
                for (int i=0; i < missingReqParams.size(); i++){
                    ParameterInfo pinfo = missingReqParams.get(i);
                    params += pinfo.getName().replaceAll(".", " ") + " ";
                }
                UIBeanHelper.setErrorMessage("The module could not be run. The following required parameters need to have values provided: " + params);
                response.sendRedirect("/gp/");
                return;
            }
            else { 
                UIBeanHelper.setErrorMessage(fileUploadException.getLocalizedMessage() + " Hit the back button to resubmit the job.");
                response.sendRedirect("/gp/");
                return;
            }
        }
 
        if (runTaskHelper.isBatchJob()) {
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
                UIBeanHelper.setErrorMessage("Error submitting job");
                response.sendRedirect("/gp/");
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
