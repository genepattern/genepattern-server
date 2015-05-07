/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp.jsf;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.webapp.FileDownloader;
import org.genepattern.server.webapp.jsf.jobinfo.JobStatusBean;
import org.genepattern.server.webservice.server.ProvenanceFinder.ProvenancePipelineResult;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.WebServiceException;

public class JobMenuBean {
    private static Logger log = Logger.getLogger(JobMenuBean.class);

    public JobMenuBean() {
    }
    
    public String createPipelineMessage(List<ParameterInfo> params) throws UnsupportedEncodingException {
        String toReturn = "";
        GpContext userContext = GpContext.getContextForUser(UIBeanHelper.getUserId());
        long maxFileSize = ServerConfigurationFactory.instance().getGPLongProperty(userContext, "pipeline.max.file.size", 250L * 1000L * 1024L);
        for (ParameterInfo i : params) {
            toReturn += "Changed parameter " + i.getName() + " to 'Prompt When Run' because it exceeded maximum file size of " + JobHelper.getFormattedSize(maxFileSize) + " for pipelines.  ";
        }
        if (toReturn.length() != 0) {
            toReturn = "&message=" + URLEncoder.encode(toReturn, "UTF-8");
        }
        return toReturn;
    }

    public void createPipeline(ActionEvent e) {
        try {
            HttpServletRequest request = UIBeanHelper.getRequest();
            String jobNumber = request.getParameter("jobMenuNumber");
            if (jobNumber == null) {
                UIBeanHelper.setErrorMessage("No job specified.");
                return;
            }
            // TODO prompt user for name
            String pipelineName = "job" + jobNumber;
            ProvenancePipelineResult pipelineResult = new LocalAnalysisClient(UIBeanHelper.getUserId()).createProvenancePipeline(jobNumber, pipelineName);
            String lsid = pipelineResult.getLsid();
            String message = createPipelineMessage(pipelineResult.getReplacedParams());
            if (lsid == null) {
                UIBeanHelper.setErrorMessage("Unable to create pipeline.");
                return;
            }
            UIBeanHelper.getResponse().sendRedirect(
                    UIBeanHelper.getRequest().getContextPath() + "/pipeline/index.jsf?lsid=" + UIBeanHelper.encode(lsid) + message);
        }
        catch (WebServiceException wse) {
            log.error("Error creating pipeline.", wse);
        }
        catch (IOException e1) {
            log.error("Error creating pipeline.", e1);
        }
    }
    
    private String prepJobFileName(String name) {
        return name.substring(3);
    }

    public void saveFile(ActionEvent event) {
        HttpServletRequest request = UIBeanHelper.getRequest();

        String jobFileName = request.getParameter("jobFileName");
        
        if (jobFileName.contains("/gp/getFile.jsp")) {
            HttpServletResponse response = UIBeanHelper.getResponse();
            try {
                response.sendRedirect(UIBeanHelper.getServer() + prepJobFileName(jobFileName));
            }
            catch (IOException e) {
                log.error("Problem redirecting to saved file");
                e.printStackTrace();
            }
            return;
        }

        jobFileName = UIBeanHelper.decode(jobFileName);
        if (jobFileName == null || "".equals(jobFileName.trim())) {
            log.error("Error saving file, missing required parameter, 'jobFileName'.");
            return;
        }

        // parse jobFileName for <jobNumber> and <filename>, add support for directories
        // from Job Summary page jobFileName="1/all_aml_test.preprocessed.gct"
        // from Job Status page jobFileName="/gp/jobResults/1/all_aml_test.preprocessed.gct"
        String contextPath = request.getContextPath();
        String pathToJobResults = contextPath + "/jobResults/";
        if (jobFileName.startsWith(pathToJobResults)) {
            jobFileName = jobFileName.substring(pathToJobResults.length());
        }

        int idx = jobFileName.indexOf('/');
        if (idx <= 0) {
            log.error("Error saving file, invalid parameter, jobFileName=" + jobFileName);
            return;
        }
        String jobNumber = jobFileName.substring(0, idx);
        String filename = jobFileName.substring(idx + 1);
        File fileObj = new File(GenePatternAnalysisTask.getJobDir(jobNumber), filename);
        if (!fileObj.exists()) {
            UIBeanHelper.setInfoMessage("File " + filename + " does not exist.");
            return;
        }


        boolean serveContent = true;
        try {
            //TODO: Hack, based on comments in http://seamframework.org/Community/LargeFileDownload
            ServletContext servletContext = UIBeanHelper.getServletContext();
            HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();        
            if (response instanceof HttpServletResponseWrapper) {
                response = (HttpServletResponse) ((HttpServletResponseWrapper) response).getResponse();
            }
            FileDownloader.serveFile(servletContext, request, response, serveContent, FileDownloader.ContentDisposition.ATTACHMENT, fileObj);
        }
        catch (Throwable t) {
            log.error("Error downloading "+jobFileName+" for user "+UIBeanHelper.getUserId(), t);
            UIBeanHelper.setErrorMessage("Error downloading "+jobFileName+": "+t.getLocalizedMessage());
        }
        FacesContext.getCurrentInstance().responseComplete(); 
    }

    public void deleteFile(ActionEvent event) {
        String value = UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("jobFile"));
        deleteFile(value);

        /**
         * Force the JobStatusBean to be refreshed. While it is used as a JSF
         * bean it's lifecycle is not managed via JSF so we need to manually
         * update it after the transaction to ensure the right files are
         * displayed after the delete - JTL 4/30/09
         */
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();
        JobStatusBean jsb = (JobStatusBean) UIBeanHelper.getManagedBean("#{jobStatusBean}");

        if (jsb != null) {
            jsb.init();
        }
    }

    public String deleteFileAction() {
        deleteFile((ActionEvent) null);
        return "deleteSuccess";
    }

    private void deleteFile(String jobFileName) {
        String contextPath = UIBeanHelper.getRequest().getContextPath();
        String pathToJobResults = contextPath + "/jobResults/";
        if (jobFileName.startsWith(pathToJobResults)) {
            jobFileName = jobFileName.substring(pathToJobResults.length());
        }
        int idx = jobFileName.indexOf('/');
        if (idx <= 0) {
            UIBeanHelper.setErrorMessage("Error deleting file: " + jobFileName);
            return;
        }
        int jobNumber = -1;
        String jobId = jobFileName.substring(0, idx);
        try {
            jobNumber = Integer.parseInt(jobId);
        }
        catch (NumberFormatException e) {
            UIBeanHelper.setErrorMessage("Error deleting file: " + jobFileName + ", " + e.getMessage());
            return;
        }
        try {
            String currentUserId = UIBeanHelper.getUserId();
            LocalAnalysisClient analysisClient = new LocalAnalysisClient(currentUserId);
            analysisClient.deleteJobResultFile(jobNumber, jobFileName);
            UIBeanHelper.setErrorMessage("Deleted file '" + jobFileName + "' from job #"+jobId);
        }
        catch (Throwable t) {
            UIBeanHelper.setErrorMessage("Error deleting file '" + jobFileName + "' from job #"+jobId+": " + t.getMessage());
            return;
        }
    }

    /**
     * Loads a module from an output file.
     * 
     * @return
     */
    public String loadTask() {
        String lsid = UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("module"));
        UIBeanHelper.getRequest().setAttribute("matchJob",
                UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("jobMenuNumber")));
        UIBeanHelper.getRequest().setAttribute("outputFileName",
                UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("name")));
        RunTaskBean runTaskBean = (RunTaskBean) UIBeanHelper.getManagedBean("#{runTaskBean}");
        assert runTaskBean != null;
        runTaskBean.setTask(lsid);
        return "run task";
    }
}
