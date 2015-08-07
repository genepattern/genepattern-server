/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.SimpleTimeZone;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.JobInfoWrapper;
import org.genepattern.server.JobInfoWrapper.OutputFile;
import org.genepattern.server.JobInfoWrapper.ParameterInfoWrapper;
import org.genepattern.server.config.ServerConfigurationFactory;

/**
 * 
 */
public class WritePipelineExecutionLog {
    private static Logger log = Logger.getLogger(WritePipelineExecutionLog.class);

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd-MM-yy");
    private static SimpleDateFormat elapsedDateFormat = new SimpleDateFormat("HH' hrs' mm' mins' ss' secs'");
    private static SimpleDateFormat titleDateFormat = new SimpleDateFormat("h:mm a EEE MMM d, ''yy");
    
    private static String htmlEncode(String str) {
        return HtmlEncoder.htmlEncode(str);
    }

    private JobInfoWrapper pipelineJobInfo = null;
    private File logFile = null;
    private String genepatternUrl = null;
    
    public WritePipelineExecutionLog(File logFile, JobInfoWrapper jobInfo) {
        this.logFile = logFile;
        this.pipelineJobInfo = jobInfo;
        this.genepatternUrl = ServerConfigurationFactory.instance().getGpUrl();
    }
    
    public void writeLogFile() {
        PrintWriter logWriter = null;
        try {
            logWriter = new PrintWriter(new FileWriter(logFile));
        }
        catch (IOException e) {
            log.error("Didn't write log file: "+e.getLocalizedMessage(), e);
            return;
        }
        writeHtml(logWriter);
    }

    private void writeHtml(PrintWriter logWriter) {
        includeStyleSheet(logWriter);

        //before
        Date  pipelineStart = pipelineJobInfo.getDateSubmitted();
        if (pipelineStart == null) {
            log.error("Missing jobInfo.getDateSubmitted");
        }
        logWriter.println("<link rel=\"SHORTCUT ICON\" href=\"" + genepatternUrl + "/gp/favicon.ico\" >");
        logWriter.println("<title>" + pipelineJobInfo.getTaskName() + "</title></head><body><h2>" + pipelineJobInfo.getTaskName()
                + " Execution Log " + titleDateFormat.format(pipelineStart) + "</h2>");
        logWriter.println("Running as user: <b>" + pipelineJobInfo.getUserId() + "</b><p>");
        String displayName = pipelineJobInfo.getTaskName();
        if (displayName.endsWith(".pipeline")) {
            displayName = displayName.substring(0, displayName.length() - ".pipeline".length());
        }
        logWriter.flush();
        
        recordInputParameters(logWriter, pipelineJobInfo);
        recordTaskCompletion(logWriter, pipelineJobInfo);
        
        int stepNum = 0;
        //for each step
        for(JobInfoWrapper step : pipelineJobInfo.getAllSteps()) {
            ++stepNum;
            recordTaskExecution(logWriter, step);
            recordTaskCompletion(logWriter, step);
        }
    } 

    private void includeStyleSheet(PrintWriter logWriter) {
        logWriter.println("<link  rel=\"stylesheet\" type=\"text/css\" href=\"/gp/skin/stylesheet.css\" />");
    }

    private void recordTaskExecution(PrintWriter logWriter, JobInfoWrapper jobInfo) {
        logWriter.println("<font size='+1'>step " + jobInfo.getStepPath()  + ". <a href=\"../../addTask.jsp?view=1&name=" + htmlEncode(jobInfo.getTaskLSID()) + "\">" + 
                htmlEncode(jobInfo.getTaskName()) + "</a>  [id: "+jobInfo.getJobNumber()+"]</font>  " + 
                htmlEncode(jobInfo.getTaskDescription()));
        logWriter.print("<div id=\"lsid" + jobInfo.getCurrentStepInPipeline() + "\" style=\"display:block;\">");
        logWriter.print("<pre>    " + jobInfo.getTaskLSID() + "</pre>");
        logWriter.print("</div>");
        logWriter.println("<div id=\"id" + jobInfo.getCurrentStepInPipeline() + "\" style=\"display:block;\">");
        logWriter.flush();
        recordInputParameters(logWriter, jobInfo);
    }
    
    private void recordInputParameters(PrintWriter logWriter, JobInfoWrapper jobInfo) {
        List<ParameterInfoWrapper> params = jobInfo.getInputParameters();
        logWriter.println("<table width='100%' frame='box' cellspacing='0'>");
        boolean odd = false;
        for(ParameterInfoWrapper aParam : params) {
            if (odd) {
                logWriter.println("<tr>");
            } 
            else {
                logWriter.println("<tr  bgcolor='#EFEFFF'>");
            }
            odd = !odd;
            logWriter.println("<td WIDTH='25%'>" + htmlEncode(aParam.getDisplayName()));
            logWriter.println("</td>");

            logWriter.println("<td>");
            if (aParam.getLink() != null) {
                logWriter.print("<a href=\"");
                logWriter.print(htmlEncode(aParam.getLink()));
                logWriter.print("\">");
                logWriter.print(htmlEncode(aParam.getDisplayValue()));
                logWriter.print("</a>");
            }
            else {
                logWriter.print(htmlEncode(aParam.getDisplayValue()));
            }
            logWriter.println("</td>");
            logWriter.println("</tr>");
        }
        logWriter.println("</table>");
        logWriter.flush();
    }

    public void recordTaskCompletion(PrintWriter logWriter, JobInfoWrapper jobInfo) {
        boolean hasOutput = jobInfo.getOutputFiles().size() > 0;
        if (hasOutput) {
            logWriter.println("<table width='100%' >");
            logWriter.println("<tr><td colspan=2 align='left'><b>Output Files:</b></td></tr>");
            for(OutputFile outputFile : jobInfo.getOutputFiles()) {
                logWriter.write("<tr><td width='25%'>&nbsp;</td><td><a target=\"_blank\" href=\"");
                logWriter.write(outputFile.getLink());
                logWriter.write("\">" + htmlEncode(outputFile.getDisplayName()) + "</a></td></tr>");
            }
            logWriter.println("</table>");
        }

        // ============================================================================
        String formattedElapsedTime = getElapsedTime(jobInfo.getDateSubmitted(), jobInfo.getDateCompleted());
        String formattedDateSubmitted = "";
        String formattedDateCompleted = "";
        Date dateSubmitted = jobInfo.getDateSubmitted();
        Date dateCompleted = jobInfo.getDateCompleted();
        if (dateSubmitted != null) {
            formattedDateSubmitted = dateFormat.format(dateSubmitted);
        }
        if (dateCompleted != null) {
            formattedDateCompleted = dateFormat.format(dateCompleted);
        }
        logWriter.println("<table>");
        logWriter.println("<tr colspan='2'><td><b>Execution Times:</b></td></tr>");
        logWriter.println("<tr><td width='25%'>Submitted: </td><td>" + formattedDateSubmitted);
        logWriter.println("</td></tr>");
        logWriter.println("<tr><td width='25%'>Completed: </td><td>" + formattedDateCompleted);
        logWriter.println("</td></tr>");
        logWriter.println("<tr><td width='25%'>Elapsed: </td><td>" + formattedElapsedTime);
        logWriter.println("</td></tr>");
        logWriter.println("</table><p>");
        logWriter.flush();
    }

    public void afterPipelineRan(PrintWriter logWriter, PipelineModel model) {
        String formattedElapsedTime = getElapsedTime(pipelineJobInfo.getDateSubmitted(), pipelineJobInfo.getDateCompleted());
        String formattedDateSubmitted = "";
        String formattedDateCompleted = "";
        Date dateSubmitted = pipelineJobInfo.getDateSubmitted();
        Date dateCompleted = pipelineJobInfo.getDateCompleted();
        if (dateSubmitted != null) {
            formattedDateSubmitted = dateFormat.format(dateSubmitted);
        }
        if (dateCompleted != null) {
            formattedDateCompleted = dateFormat.format(dateCompleted);
        }

        logWriter.println("<table>");

        logWriter.println("<tr colspan='2'><td><h2>Pipeline Execution Times:</h2></td></tr>");
        logWriter.println("<tr><td width='25%'>Submitted: </td><td>" + formattedDateSubmitted);
        logWriter.println("</td></tr>");
        logWriter.println("<tr><td width='25%'>Completed: </td><td>" + formattedDateCompleted);
        logWriter.println("</td></tr>");
        logWriter.println("<tr><td width='25%'>Elapsed: </td><td>" + formattedElapsedTime);
        logWriter.println("</td></tr>");
        logWriter.println("</table><p>");

        // add the link to the execution log
        logWriter.flush();
        logWriter.close();        
    }

    public String getElapsedTime(Date startTime, Date endTime) {
        if (startTime == null) {
            return "";
        }
        if (endTime == null) {
            endTime = new Date();
        }
        long deltaMillis = endTime.getTime() - startTime.getTime();
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(deltaMillis);
        cal.setTimeZone(new SimpleTimeZone(0, ""));
        elapsedDateFormat.setTimeZone(new SimpleTimeZone(0, ""));// set to
        // GMT for the calculation
        return elapsedDateFormat.format(cal.getTime());
    }

}
