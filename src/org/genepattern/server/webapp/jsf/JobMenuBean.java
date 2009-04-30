/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2009) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp.jsf;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.codegenerator.CodeGeneratorUtil;
import org.genepattern.server.JobInfoWrapper;
import org.genepattern.server.JobInfoWrapper.OutputFile;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;

import org.genepattern.server.webapp.jsf.jobinfo.JobStatusBean;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;

import org.genepattern.webservice.WebServiceException;

public class JobMenuBean { 
    private static Logger log = Logger.getLogger(JobMenuBean.class);
   
  
    

    public JobMenuBean() {
      
    }

    public void createPipeline(ActionEvent e) {
	try {
		HttpServletRequest request = UIBeanHelper.getRequest();
		
		String jobNumber = request.getParameter("jobMenuNumber");
	       
	    System.out.println("CreatePipeline on job #" + jobNumber + "  ");
		
	    if (jobNumber == null) {
		UIBeanHelper.setErrorMessage("No job specified.");
		return;
	    }
        // TODO prompt user for name
	    String pipelineName = "job" + jobNumber; 
	    String lsid = new LocalAnalysisClient(UIBeanHelper.getUserId()).createProvenancePipeline(jobNumber, pipelineName);

	    if (lsid == null) {
		UIBeanHelper.setErrorMessage("Unable to create pipeline.");
		return;
	    }
	    UIBeanHelper.getResponse().sendRedirect(
		    UIBeanHelper.getRequest().getContextPath() + "/pipelineDesigner.jsp?name="
			    + UIBeanHelper.encode(lsid));
	} catch (WebServiceException wse) {
	    log.error("Error creating pipeline.", wse);
	} catch (IOException e1) {
	    log.error("Error creating pipeline.", e1);
	}
    }

      

    public void saveFile(ActionEvent event) {
        HttpServletRequest request = UIBeanHelper.getRequest();
        
        String jobFileName = request.getParameter("jobFileName");
        
        jobFileName = UIBeanHelper.decode(jobFileName);
        if (jobFileName == null || "".equals(jobFileName.trim())) {
            log.error("Error saving file, missing required parameter, 'jobFileName'.");
            return;
        }
	    
        //parse jobFileName for <jobNumber> and <filename>, add support for directories
        //from Job Summary page jobFileName="1/all_aml_test.preprocessed.gct"
        //from Job Status page jobFileName="/gp/jobResults/1/all_aml_test.preprocessed.gct"
        String contextPath = request.getContextPath();
        String pathToJobResults = contextPath + "/jobResults/";
        if (jobFileName.startsWith(pathToJobResults)) {
            jobFileName = jobFileName.substring(pathToJobResults.length());
        }

        int idx = jobFileName.indexOf('/');
        if (idx <= 0) {
            log.error("Error saving file, invalid parameter, jobFileName="+jobFileName);
            return;
        }
        String jobNumber = jobFileName.substring(0, idx);
        String filename = jobFileName.substring(idx+1);
        File in = new File(GenePatternAnalysisTask.getJobDir(jobNumber), filename);
        if (!in.exists()) {
            UIBeanHelper.setInfoMessage("File " + filename + " does not exist.");
            return;
        }
        InputStream is = null;
        try {
            HttpServletResponse response = UIBeanHelper.getResponse();
            response.setHeader("Content-Disposition", "attachment; filename=" + in.getName() + ";");
            response.setHeader("Content-Type", "application/octet-stream");
            response.setHeader("Cache-Control", "no-store"); // HTTP 1.1
            response.setHeader("Pragma", "no-cache"); // HTTP 1.0 cache
            response.setDateHeader("Expires", 0);

            OutputStream os = response.getOutputStream();
            is = new BufferedInputStream(new FileInputStream(in));
            byte[] b = new byte[10000];
            int bytesRead;
            while ((bytesRead = is.read(b)) != -1) {
                os.write(b, 0, bytesRead);
            }
            os.flush();
            os.close();
            UIBeanHelper.getFacesContext().responseComplete();
        } 
        catch (IOException e) {
            log.error("Error saving file.", e);
        } 
        finally {
            if (is != null) {
                try {
                    is.close();
                } 
                catch (IOException e) {
                }
            }
        }
    }

  
    public void deleteFile(ActionEvent event) {
    	String value = UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("jobFile"));
    	deleteFile(value);
    
    	/**
    	 * Force the JobStatusBean to be refreshed. While it is used as a JSF bean it's lifecycle is not managed
    	 * via JSF so we need to manually update it after the transaction to ensure the right files are displayed
    	 * after the delete - JTL 4/30/09
    	 */
    	HibernateUtil.commitTransaction();
    	HibernateUtil.beginTransaction();
    	JobStatusBean jsb = (JobStatusBean)UIBeanHelper.getManagedBean("#{jobStatusBean}");
    	
    	if (jsb != null) jsb.init();
    
    }
    
    public String deleteFileAction() {
    	deleteFile((ActionEvent)null);
    	
    	
    	return "deleteSuccess";
    }
    
    protected void deleteFile(String jobFileName) {
     
    	String contextPath = UIBeanHelper.getRequest().getContextPath();
        String pathToJobResults = contextPath + "/jobResults/";
        if (jobFileName.startsWith(pathToJobResults)) {
            jobFileName = jobFileName.substring(pathToJobResults.length());
        }
        int idx = jobFileName.indexOf('/');
        if (idx <= 0) {
            UIBeanHelper.setErrorMessage("Error deleting file: "+jobFileName);
            return;
        }
        int jobNumber = -1;
        String jobId = jobFileName.substring(0, idx);
        try {
            jobNumber = Integer.parseInt(jobId);
        }
        catch (NumberFormatException e) {
            UIBeanHelper.setErrorMessage("Error deleting file: "+jobFileName+", "+e.getMessage());
            return;
        }
        try {
            // String filename = encodedJobFileName.substring(index + 1);
            String currentUserId = UIBeanHelper.getUserId();
            LocalAnalysisClient analysisClient = new LocalAnalysisClient(currentUserId);
            analysisClient.deleteJobResultFile(jobNumber, jobFileName);
        } 
        catch (WebServiceException e) {
            UIBeanHelper.setErrorMessage("Error deleting file: "+jobFileName+", "+e.getMessage());
            return;
        }
    }

    /**
	 * Loads a module from an output file.
	 * 
	 * @return
	 */
	public String loadTask() {
		String lsid = UIBeanHelper.decode(UIBeanHelper.getRequest()
				.getParameter("module"));
		UIBeanHelper.getRequest().setAttribute(
				"matchJob",
				UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter(
						"jobMenuNumber")));
		UIBeanHelper.getRequest().setAttribute(
				"outputFileName",
				UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter(
						"name")));
		RunTaskBean runTaskBean = (RunTaskBean) UIBeanHelper
				.getManagedBean("#{runTaskBean}");
		assert runTaskBean != null;
		runTaskBean.setTask(lsid);
		return "run task";
	}
}
