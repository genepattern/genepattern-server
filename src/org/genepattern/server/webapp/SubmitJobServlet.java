package org.genepattern.server.webapp;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.FileUtils;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.executor.JobSubmissionException;
import org.genepattern.server.rest.GpServerException;
import org.genepattern.server.rest.JobInput;
import org.genepattern.server.rest.JobInput.Param;
import org.genepattern.server.rest.JobInput.ParamId;
import org.genepattern.server.rest.JobInput.ParamValue;
import org.genepattern.server.rest.JobInputApiImpl;
import org.genepattern.server.rest.JobInputFileUtil;
import org.genepattern.server.webapp.jsf.JobBean;
import org.genepattern.server.webapp.jsf.PageMessages;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

public class SubmitJobServlet extends HttpServlet {
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
 
        //TODO: set this to true to test the new API from the old job submit form
        final boolean newApi=true;
        if (runTaskHelper.isBatchJob()) {
            request.getSession().setAttribute(JobBean.DISPLAY_BATCH, runTaskHelper.getBatchJob().getId());
            response.sendRedirect("/gp/jobResults");
        }
        else if (newApi) {
            //prototype code for testing the new RESTful API for adding a job to the server
            //1) [throwaway code ...] parse the legacy job input form, generate an object for using the new API
            JobInput jobInput=initJobInput(runTaskHelper);
            
            //2) [throwaway code ...] hard-code a filelist value so that I can test this part of the API
            Param param=jobInput.getParams().get(new ParamId("input.filename"));
            if (param != null) {
                if ("admin".equals(userID)) {
                    param.addValue(new ParamValue("http://127.0.0.1:8080/gp/users/admin/all_aml%20test.cls"));
                    param.addValue(new ParamValue("http://127.0.0.1:8080/gp/users/admin/all_aml%20test.gct"));
                    param.addValue(new ParamValue("<GenePatternURL>users/admin/all_aml%20test.cls"));
                    param.addValue(new ParamValue("<GenePatternURL>users/admin/all_aml%20test.gct"));
                    param.addValue(new ParamValue("<GenePatternURL>/users/admin/all_aml%20test.cls"));
                    param.addValue(new ParamValue("<GenePatternURL>/users/admin/all_aml%20test.gct"));
                }
            }
            
            //3) [throwaway code ...] add a web upload file to the list of values
            Context jobContext=ServerConfiguration.Context.getContextForUser(userID);
            try {
                JobInputFileUtil fileUtil = new JobInputFileUtil(jobContext);
                GpFilePath gpFilePath=fileUtil.initUploadFileForInputParam(8, "input.filename", "build.properties");
                
                File srcFile=new File("../resources/build.properties");
                File destFile=gpFilePath.getServerFile();
                FileUtils.copyFile(srcFile, destFile);
                
                fileUtil.updateUploadsDb(gpFilePath);
                                
                param.addValue(new ParamValue(gpFilePath.getUrl().toExternalForm()));
            }
            catch (Exception e) {
                setErrorMessage(request, "Error submitting job: "+e.getLocalizedMessage());
                redirectToHome(response);
            }
            JobInputApiImpl impl = new JobInputApiImpl();
            String jobId;
            try {
                jobId = impl.postJob(jobContext, jobInput);
            }
            catch (GpServerException e) {
                setErrorMessage(request, "Error submitting job: "+e.getLocalizedMessage());
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
    
    private JobInput initJobInput(final RunTaskHelper runTaskHelper) {
        //initialize a map of input parameter info
        Map<String,ParameterInfo> pinfomap=new HashMap<String,ParameterInfo>();
        for(ParameterInfo pinfo : runTaskHelper.getTaskInfo().getParameterInfoArray()) {
            pinfo.getName();
            pinfomap.put(pinfo.getName(), pinfo);
        }
        
        final String lsid=runTaskHelper.getTaskLsid();
        JobInput jobInput = new JobInput();
        jobInput.setLsid(lsid);
        
        for(Entry<?,?> entry : runTaskHelper.getRequestParameters().entrySet()) {
            String key = (String) entry.getKey();
            String val = (String) entry.getValue();            
            ParameterInfo pinfo=pinfomap.get(key);
            if (pinfo != null) {
                jobInput.addValue(key, val);
            }
        }
        for(Entry<?,?> entry : runTaskHelper.getInputFileParameters().entrySet()) {
            String key = (String) entry.getKey();
            String val = (String) entry.getValue();            
            ParameterInfo pinfo=pinfomap.get(key);
            if (pinfo != null) {
                jobInput.addValue(key, val);
            }
        }
        return jobInput;
    }
}
