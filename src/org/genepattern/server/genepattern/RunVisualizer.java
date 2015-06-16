/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.genepattern;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.Properties;

import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.util.GPConstants;
import org.genepattern.visualizer.RunVisualizerConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfoAttributes;

public class RunVisualizer {
    private String userID = null;
    private JobInfo jobInfo = null;
    private TaskInfoAttributes taskInfoAttributes = null;
    private String contextPath = "/gp";
    private String documentCookie = "";
    private String javaFlags = null;
    private Properties requestParameters = new Properties();

    public RunVisualizer() {
    }
    
    public void setJobInfo(JobInfo ji) {
        this.jobInfo = ji;
    }
    
    public void setTaskInfoAttributes(TaskInfoAttributes tia) {
        this.taskInfoAttributes = tia;
    }

    /**
     * <code>request.getContextPath()</code>
     * @param str
     */
    public void setContextPath(String str) {
        this.contextPath = str;
    }
    
    public void setDocumentCookie(String documentCookie) {
        this.documentCookie = documentCookie;
    }
    
    public void setJavaFlags(String str) {
        this.javaFlags = str;
    }
    
    public void setRequestParameters(Properties props) {
        this.requestParameters.putAll(props);
    }
    
    public void writeVisualizerAppletTag(Writer out) throws IOException, UnsupportedEncodingException, MalformedURLException {
        String name = jobInfo.getTaskName();
        ParameterInfo[] parameterInfoArray = jobInfo.getParameterInfoArray();

        if(javaFlags==null) {
            javaFlags = System.getProperty(RunVisualizerConstants.JAVA_FLAGS_VALUE);
        }

        StringWriter app = new StringWriter();
        app.append("<applet code=\"" 
                + org.genepattern.visualizer.RunVisualizerApplet.class.getName() 
                + "\" archive=\"runVisualizer.jar,commons-httpclient.jar,commons-codec-1.6.jar\" codebase=\"/gp/downloads\" width=\"1\" height=\"1\" alt=\"Your browser can not run applets\">");

        app.append("<param name=\"" + RunVisualizerConstants.NAME + "\" value=\"" + URLEncoder.encode(name, "UTF-8") + "\" >");
        app.append("<param name=\"" + RunVisualizerConstants.OS + "\" value=\"" + URLEncoder.encode(taskInfoAttributes.get(GPConstants.OS), "UTF-8") + "\">");
        app.append("<param name=\"" + RunVisualizerConstants.CPU_TYPE + "\" value=\"" + URLEncoder.encode(taskInfoAttributes.get(GPConstants.CPU_TYPE), "UTF-8") + "\">");
        app.append("<param name=\"" + RunVisualizerConstants.JAVA_FLAGS_VALUE + "\" value=\"" + URLEncoder.encode(javaFlags, "UTF-8") + "\">");
        app.append("<param name=\"" + RunVisualizerConstants.CONTEXT_PATH + "\" value=\"" + URLEncoder.encode(contextPath, "UTF-8") + "\">");
        StringBuffer paramNameList = new StringBuffer();
        for (int i = 0; i < parameterInfoArray.length; i++) {
            if (i > 0) {
                paramNameList.append(",");
            }
            paramNameList.append(parameterInfoArray[i].getName());
        }
        app.append("<param name=\"" + RunVisualizerConstants.PARAM_NAMES + "\" value=\"" + paramNameList.toString() + "\" >");

        for (int i = 0; i < parameterInfoArray.length; i++) {
            String paramName = parameterInfoArray[i].getName();
            String paramValue = (String) parameterInfoArray[i].getValue();
            if (paramValue != null) {
                paramValue = paramValue.replace("\\", "\\\\");
            } 
            else {
                paramValue = "";
            }
            app.append("<param name=\"" + paramName + "\" value=\"" + paramValue + "\">");
        }

        StringBuffer vis = new StringBuffer();

        int numToDownload = 0;
        for (int i = 0; i < parameterInfoArray.length; i++) {
            String paramValue = parameterInfoArray[i].getValue();
            
            //HACK: don't know what is supposed to happen here, parameterInfo.isInputFile() no longer works!
            //      After converting from .jsp pages to run visualizers.
            ParameterInfo parameterInfo = parameterInfoArray[i];
            String mode = (String) parameterInfo.getAttributes().get("MODE");
            if (mode != null && ( mode.equals("IN") || mode.equals("URL_IN") ) && paramValue != null) {
                try {
                    new java.net.URL(paramValue);
                    // note that this parameter is a URL that must be downloaded by adding it to the CSV list for the applet
                    if (numToDownload > 0) {
                        vis.append(",");
                    }
                    vis.append(parameterInfoArray[i].getName());
                    numToDownload++;
                }
                catch(Exception x){
                }
            }
        }
        app.append("<param name=\"" + RunVisualizerConstants.DOWNLOAD_FILES + "\" value=\"" + URLEncoder.encode(vis.toString(), "UTF-8") + "\">");
        app.append("<param name=\"" + RunVisualizerConstants.COMMAND_LINE + "\" value=\"" + URLEncoder.encode(taskInfoAttributes.get(GPConstants.COMMAND_LINE), "UTF-8") + "\">");
        app.append("<param name=\"" + RunVisualizerConstants.DEBUG + "\" value=\"1\">");

        StringBuffer fileNamesBuf = new StringBuffer();
        String lsid = jobInfo.getTaskLSID();
        String libdir = DirectoryManager.getTaskLibDir(null, lsid, userID);
        File[] supportFiles = new File(libdir).listFiles();
        for (int i = 0; i < supportFiles.length; i++) {
            if (i > 0) fileNamesBuf.append(",");
            fileNamesBuf.append(supportFiles[i].getName());
        }
        app.append("<param name=\"" + RunVisualizerConstants.SUPPORT_FILE_NAMES + "\" value=\"" + URLEncoder.encode(fileNamesBuf.toString(), "UTF-8") + "\" >");

        StringBuffer fileDatesBuf = new StringBuffer();
        for (int i = 0; i < supportFiles.length; i++) {
            if (i > 0) {
                fileDatesBuf.append(",");
            }
            fileDatesBuf.append(supportFiles[i].lastModified());
        }
        app.append("<param name=\"" + RunVisualizerConstants.SUPPORT_FILE_DATES + "\" value=\"" + URLEncoder.encode(fileDatesBuf.toString(), "UTF-8") + "\" >");
        app.append("<param name=\"" + RunVisualizerConstants.LSID + "\" value=\"" + URLEncoder.encode(lsid, "UTF-8") + "\" >");
        if (documentCookie != null && documentCookie.trim() != "") {
            app.append("<param name=\"browserCookie\" value=\""+documentCookie+"\">");
        }
        app.append("</applet>");
        
        out.write(app.toString());
    }
}
