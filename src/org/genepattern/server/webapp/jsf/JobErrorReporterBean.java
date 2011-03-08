/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */


package org.genepattern.server.webapp.jsf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;


public class JobErrorReporterBean {
    private static Logger log = Logger.getLogger(JobErrorReporterBean.class);

    private int jobNumber = 0;
    File errorFile = null;
    String fileJobDirAndName = null;

    public JobErrorReporterBean() {
        String jobNumberParam = UIBeanHelper.getRequest().getParameter("jobNumber");
        try {
            jobNumber = new Integer(jobNumberParam);
        }
        catch (NumberFormatException e) {
            log.error("Error initializing JobErrorReporterBean for jobNumber="+jobNumberParam, e);
        }
        
        try {
            JobInfo jobInfo = new AnalysisDAO().getJobInfo(jobNumber);

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
        } 
        catch (Throwable t) {
            // TODO Auto-generated catch block
            log.error(t);
        }
        log.info("New JobErrorReporterBean for " + jobNumber);
	}

    public int getJobNumber() {	
        return jobNumber;
    }
	
    public String getSomething(){
        return "Something";
    }
	
    public String getErrorFileContentHead() {
        if (errorFile == null) {
            return "Error file not found ";
        }
        try {
            StringBuffer buff = new StringBuffer();
            BufferedReader br = new BufferedReader(new FileReader(errorFile));
            String line = null;
            int i=0;
            while ((line = br.readLine())!=null){
                System.out.println("READ: "+line);
                buff.append(line);
                i++;
                if (i > 9) break;
            }
            return buff.toString();
        } 
        catch (IOException e){
            return "Error reading error file " + errorFile.getAbsolutePath();
        }
	}

    public String getErrorFileURL() {
        return fileJobDirAndName;
    }

    public String getErrorFilePath() {
        return errorFile.getAbsolutePath();
    }
}
