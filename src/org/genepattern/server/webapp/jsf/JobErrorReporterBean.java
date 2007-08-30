
package org.genepattern.server.webapp.jsf;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.WebServiceException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


public class JobErrorReporterBean {
	private int jobNumber = 0;
	File errorFile = null;
	String fileJobDirAndName = null;
	private static Logger log = Logger.getLogger(JobErrorReporterBean.class);

	public JobErrorReporterBean(){
		super();
		String job = UIBeanHelper.getRequest().getParameter("jobNumber");
        jobNumber = new Integer(job);
        
        try {
        LocalAnalysisClient ac = new LocalAnalysisClient(UIBeanHelper.getUserId());
	    JobInfo jobInfo;
		
			jobInfo = ac.getJob(jobNumber);
		
		ParameterInfo[] jobParams = jobInfo.getParameterInfoArray();
		String webAppDir = System.getProperty("webappDir");
	    for (int j = 0; j < jobParams.length; j++) {
            if (!jobParams[j].isOutputFile()) {
                continue;
            }
            
            File outFile = new File(webAppDir+ "/jobResults/" + jobParams[j].getValue());
            String fileName = outFile.getName();
            if ("stderr.txt".equals(fileName)){
            	fileJobDirAndName = jobParams[j].getValue();
                errorFile = outFile;
            	break;
            }
        }
	    
        } catch (WebServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
		System.out.println("New JobErrorReporterBean for " + jobNumber);
	}
	
	public int getJobNumber(){
		
		
		return jobNumber;
		
	}
	
	public String getSomething(){
		return "Something";
	}
	
	public String getErrorFileContentHead(){
		if (errorFile == null) return "Error file not found ";
		
		try {
		StringBuffer buff = new StringBuffer();
		BufferedReader br = new BufferedReader(new FileReader(errorFile));
		//log.warn("READ: "+br.readLine());
		
		//if (true) return "FOOFOOFOO";
		
		String line = null;
		int i=0;
		while ((line = br.readLine())!=null){
			System.out.println("READ: "+line);
			buff.append(line);
			i++;
			if (i > 9) break;
		}
		return buff.toString();
		} catch (IOException e){
			return "Error reading error file " + errorFile.getAbsolutePath();
		}
		
	}
	
	public String getErrorFileURL(){
		
		return fileJobDirAndName;
	}
	
	public String getErrorFilePath(){
		
		return errorFile.getAbsolutePath();
	}
	
	
	
	
}
