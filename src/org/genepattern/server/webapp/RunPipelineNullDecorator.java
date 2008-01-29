/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2008) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp;

import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.util.GPConstants;
import org.genepattern.util.StringUtils;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;

public class RunPipelineNullDecorator extends RunPipelineBasicDecorator implements RunPipelineOutputDecoratorIF {
    RunPipelineExecutionLogger logger = new RunPipelineExecutionLogger();

    Properties genepatternProps = null;

    public void setOutputStream(PrintStream outstr) {
    }

    public void error(PipelineModel model, String message) {
        logger.error(model, message);
    }

    public void beforePipelineRuns(PipelineModel model) {
        this.model = model;
        try {
            String genePatternPropertiesFile = System.getProperty("genepattern.properties") + java.io.File.separator
                    + "genepattern.properties";
            java.io.FileInputStream fis = new java.io.FileInputStream(genePatternPropertiesFile);
            genepatternProps = new Properties();
            genepatternProps.load(fis);
            fis.close();
        } catch (Exception e) {
            genepatternProps = new Properties();
        }
        logger.setRegisterExecutionLog(false);
        logger.beforePipelineRuns(model);

    }

    public void recordTaskExecution(JobSubmission jobSubmission, int idx, int numSteps) {
        ParameterInfo[] parameterInfo = jobSubmission.giveParameterInfoArray();
        for (int i = 0; i < parameterInfo.length; i++) {
            ParameterInfo aParam = parameterInfo[i];
            boolean isInputFile = aParam.isInputFile();
            HashMap hmAttributes = aParam.getAttributes();
            String paramType = null;
            if (hmAttributes != null)
                paramType = (String) hmAttributes.get(ParameterInfo.TYPE);
            if (!isInputFile && !aParam.isOutputFile() && paramType != null
                    && paramType.equals(ParameterInfo.FILE_TYPE)) {
                isInputFile = true;
            }
            isInputFile = aParam.isInputFile();

            if (isInputFile) {
                // convert from "localhost" to the actual host name so that
                // it can be referenced from anywhere (eg. visualizer on
                // non-local client)
                aParam.setValue(localizeURL(aParam.getValue()));

            }
        }
        logger.recordTaskExecution(jobSubmission, idx, numSteps);

    }

    public void recordTaskCompletion(JobInfo jobInfo, String name) {
        logger.recordTaskCompletion(jobInfo, name);

    }

    public void afterPipelineRan(PipelineModel model) {
        logger.afterPipelineRan(model);

    }

    protected String localizeURL(String original) {
        if (original == null) {
            return "";
        }
        original = StringUtils.replaceAll(original, GPConstants.LEFT_DELIMITER + GPConstants.LSID
                + GPConstants.RIGHT_DELIMITER, model.getLsid());
        try {
            URL org = new URL(original);
            URL url = new URL(URL + org.getFile());
            return url.toString();
        } 
        catch (MalformedURLException mue) {
            return original;
        }
    }

}
