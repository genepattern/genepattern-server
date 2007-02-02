/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Properties;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.user.UserPropKey;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.StringUtils;
import org.genepattern.visualizer.RunVisualizerConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

/**
 * This is the decorator for output from running a pipeline from the web
 * environment. It should generate the html for the runPipeline.jsp page as it
 * runs and also record a log file that will allow users to see when this
 * pipeline was run, execution times and output files
 */
public class RunPipelineHTMLDecorator extends RunPipelineDecoratorBase implements RunPipelineOutputDecoratorIF {
    PrintStream out = System.out;

    protected static String GET_PIPELINE_JSP = "pipelineDesigner.jsp?name=";

    protected static String GET_JOB_JSP = "getJobResults.jsp?jobID=";

    protected static String GET_TASK_JSP = "viewTask.jsp?view=1&name=";

    protected static String GET_TASK_FILE = "jobResults";

    protected static String GET_FILE = "getFile.jsp?task=&file=";

    public static final String STDOUT = GPConstants.STDOUT;

    public static final String STDERR = GPConstants.STDERR;

    public void setOutputStream(PrintStream outstr) {
        out = outstr;
    }

    public void error(PipelineModel model, String message) {
        out.println(htmlEncode(message) + "<br>");
    }

    public void beforePipelineRuns(PipelineModel model) {
        this.model = model;
        super.init();

        String jobID = System.getProperty("jobID");
        String isSaved = System.getProperty("savedPipeline");
        // bug 592. Don't give link to pipeline if it is not saved

        out.println("<p>");

        // set up the form for zip results
        out.println("<form name=\"results\" action=\"" + URL + "zipJobResults.jsp\">");
        out.println("<input type=\"hidden\" name=\"name\" value=\"" + model.getName() + "\">");
        out.println("<input type=\"hidden\" name=\"jobID\" value=\"" + jobID + "\">");
        out.println("<input type=\"hidden\" name=\"cmdElement\" value=\"\"/>");

        // set up the table for task reporting
        // out.println("<table width=\"90%\"><tr><td><u>step</u></td><td><u>name
        // and parameters</u></td></tr>");

        out.println("<table width=\"100%\"  border=\"0\" cellspacing=\"0\" cellpadding=\"0\">");

        // out.println("<colgroup><col width='100'/><col width='100'/><col
        // width='100'/></colgroup>");

        out.println("<tr class=\"smalltype\">");
        out.println("  <td >&nbsp;</td>");
        out.println("  <td valign=\"top\"><nobr>");
        out
                .println("    <div align=\"center\" class=\"smalltype\"><a onclick=\"downloadCheckedFiles()\" href=\"#\">download</a> | <a onclick=\"deleteCheckedFiles()\" href=\"#\">delete</a> </div>");

        out.println("  </nobr></td>");
        out.println("  <td>&nbsp;</td>");
        out
                .println("  <td><div class=\"smalltype\"><a href=\"#\" onclick=\"openAllTasks()\">open all</a> | <a href=\"#\" onclick=\"closeAllTasks()\">close all</a></div>");
        out.println("<img src=\"images/spacer.gif\" height=\"0\" width=\"550\"/></td></tr>");
        out.println("<tr class=\"tableheader-row\">");
        out.println("  <td>step</td>");
        out.println("  <td><div align=\"center\" >");
        out
                .println("        <input name=\"checkbox\" type=\"checkbox\" value=\"checkbox\" checked=\"checked\" onclick=\"javascript:checkAll(this.form, this.checked)\" />");

        out.println("  </div></td>");
        out.println("  <td>&nbsp;</td>");
        out.println("  <td>name and parameters </td>");
        out.println("</tr>");

        out.flush();

    }

    /**
     * called before a task is executed
     * 
     * If this is for a visualizer, write out the applet code
     */
    int currentIndex = -1;

    public void recordTaskExecution(JobSubmission jobSubmission, int idx, int numSteps) {
        currentIndex = idx;

        out.print("<tr class=\"task-title\"><td valign=top><nobr>" + idx + " of " + numSteps + "</nobr></td>");
        out.println("<td><div align=\"center\"><input name=\"checkbox\" type=\"checkbox\" onclick=\"checkAllInTask("
                + currentIndex + ", this)\" value=\"checkbox\" checked=\"checked\" />");
        out.println("</div></td>");

        out.println("<td>");
        out.println("<div align=\"right\"><a href=\"#\">");
        out.println("<img id=\"downarrow" + idx
                + "\" visibility=\"true\" src=\"images/arrow-down-main2.gif\" onclick=\"toggleTask(" + currentIndex
                + ",true)\" alt=\"close task\" width=\"7\" height=\"7\" hspace=\"3\" vspace=\"3\" border=\"0\" />");
        out.println("<img style=\"display:none\" visibility=\"false\" id=\"rightarrow" + idx
                + "\" src=\"images/arrow-right-main2.gif\" onclick=\"toggleTask(" + currentIndex
                + ",false)\" alt=\"open task\" width=\"7\" height=\"7\" hspace=\"3\" vspace=\"3\" border=\"0\" />");

        out.println("</a></div></td>");

        out.println("<td valign=top>" + jobSubmission.getName() + "</td></tr>");
        out.println("<tr id=\"fileRow" + idx + "_0\"class=\"files\"><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td>");
        out.println("<td class=\"description\">(");
        ParameterInfo[] parameterInfo = jobSubmission.giveParameterInfoArray();
        for (int i = 0; i < parameterInfo.length; i++) {
            ParameterInfo aParam = parameterInfo[i];
            boolean isInputFile = aParam.isInputFile();
            HashMap hmAttributes = aParam.getAttributes();
            boolean isOptional = false;

            String paramType = null;
            if (hmAttributes != null) {
                paramType = (String) hmAttributes.get(ParameterInfo.TYPE);
                String optVal = (String) hmAttributes.get(GPConstants.PARAM_INFO_OPTIONAL[0]);
                if (optVal != null) {
                    isOptional = "on".equalsIgnoreCase(optVal);
                }
            }
            if (!isInputFile && !aParam.isOutputFile() && paramType != null
                    && paramType.equals(ParameterInfo.FILE_TYPE)) {
                isInputFile = true;
            }
            isInputFile = aParam.isInputFile();
            String value = aParam.getValue();

            if (isOptional && (value.trim().length() == 0)) {
                continue; // don't write the optional empty params
            }

            out.print(aParam.getName().replace('.', ' '));
            out.print("=");

            if (isInputFile) {
                // convert from "localhost" to the actual host name so that
                // it can be referenced from anywhere (eg. visualizer on
                // non-local client)
                value = localizeURL(value);
                out.print("<a href=\"");
                out.print(value);
                out.print("\">");

                value = getFileUrlDisplayValue(value);
            }

            out.print(htmlEncode(value));

            if (isInputFile) {
                out.println("</a>");
            }

            if (i != (parameterInfo.length - 1))
                out.print(", ");
        }
        out.println(")");
        if (jobSubmission.isVisualizer())
            writeVisualizerAppletTag(jobSubmission);

        out.print("</td></tr>");
        out.println(); // trigger output flush in runPipeline.jsp
        out.flush();
    }

    // output the applet tag for a visualizer

    public void writeVisualizerAppletTag(JobSubmission jobSubmission) {
        // PUT APPLET HERE

        String userId = System.getProperty("userId");
        LocalAdminClient adminClient = new LocalAdminClient(userId);
        TaskInfo taskInfo = null;
        try {
            taskInfo = adminClient.getTask(jobSubmission.getLSID());
        } catch (Exception e) {
            e.printStackTrace();
            return; // can't write the applet without this
        }
        ParameterInfo[] parameterInfoArray = jobSubmission.giveParameterInfoArray();
        Properties params = new Properties();
        for (int i = 0; i < parameterInfoArray.length; i++) {
            params.setProperty(parameterInfoArray[i].getName(), parameterInfoArray[i].getValue());
        }

        String name = jobSubmission.getName();
        TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
        String lsid = (String) tia.get(GPConstants.LSID);
        String libdir = "";
        try {
            libdir = DirectoryManager.getTaskLibDir(null, lsid, userId);
        } catch (Exception e) {
            e.printStackTrace(); // can't get the applet without this
            return;
        }
        if (parameterInfoArray == null)
            parameterInfoArray = new ParameterInfo[0];
        File[] supportFiles = new File(libdir).listFiles();
        int i;
        String appletName = "a" + ("" + Math.random()).substring(2); // unique
                                                                        // name
                                                                        // so
                                                                        // that
                                                                        // multiple
                                                                        // instances
                                                                        // of
                                                                        // applet
                                                                        // on a
                                                                        // single
                                                                        // page
                                                                        // will
                                                                        // not
                                                                        // collide
        String javaFlags = adminClient.getUserProperty(UserPropKey.VISUALIZER_JAVA_FLAGS);
        if (javaFlags == null) {
            javaFlags = System.getProperty(RunVisualizerConstants.JAVA_FLAGS_VALUE);
        }

        out
                .println("<applet code=\""
                        + org.genepattern.visualizer.RunVisualizerApplet.class.getName()
                        + "\" archive=\"runVisualizer.jar\" codebase=\"downloads\" width=\"1\" height=\"1\" alt=\"Your browser refuses to run applets\" name=\""
                        + appletName + "\">");
        out.println("<param name=\"" + RunVisualizerConstants.NAME + "\" value=\"" + name + "\">");
        out.println("<param name=\"" + RunVisualizerConstants.OS + "\" value=\""
                + StringUtils.htmlEncode(tia.get(GPConstants.OS)) + "\">");
        out.println("<param name=\"" + RunVisualizerConstants.CPU_TYPE + "\" value=\""
                + StringUtils.htmlEncode(tia.get(GPConstants.CPU_TYPE)) + "\">");
        out.println("<param name=\"" + RunVisualizerConstants.LIBDIR + "\" value=\"" + StringUtils.htmlEncode(libdir)
                + "\">");
        out.println("<param name=\"" + RunVisualizerConstants.JAVA_FLAGS_VALUE + "\" value=\""
                + StringUtils.htmlEncode(javaFlags) + "\">");

        out.println("<param name=\"" + RunVisualizerConstants.PARAM_NAMES + "\" value=\"");

        for (i = 0; i < parameterInfoArray.length; i++) {
            if (i > 0)
                out.print(",");

            out.print(StringUtils.htmlEncode(parameterInfoArray[i].getName()));
        }
        out.println("\">");

        for (i = 0; i < parameterInfoArray.length; i++) {
            String paramName = parameterInfoArray[i].getName();
            if (paramName.equals("className")) {
                out.println("<param name=\"" + paramName + "\" value=\""
                        + StringUtils.htmlEncode(parameterInfoArray[i].getDescription()) + "\">");
                continue;
            }
            boolean isInputFile = (paramName.indexOf("filename") != -1);
            String value;
            if (isInputFile)
                value = localizeURL(params.getProperty(paramName));
            else
                value = StringUtils.htmlEncode(params.getProperty(paramName));
            out.println("<param name=\"" + StringUtils.htmlEncode(paramName) + "\" value=\"" + value + "\">");
        }

        out.println("<param name=\"" + RunVisualizerConstants.DOWNLOAD_FILES + "\" value=\"");

        int numToDownload = 0;
        for (i = 0; i < parameterInfoArray.length; i++) {
            String paramName = parameterInfoArray[i].getName();
            boolean isInputFile = (paramName.indexOf("filename") != -1);

            if (isInputFile) {
                // note that this parameter is a URL that must be downloaded by
                // adding it to the CSV list for the applet
                if (numToDownload > 0)
                    out.print(",");
                out.print(StringUtils.htmlEncode(parameterInfoArray[i].getName()));
                numToDownload++;
            }
        }
        out.println("\">");

        out.println("<param name=\"" + RunVisualizerConstants.COMMAND_LINE + "\" value=\""
                + StringUtils.htmlEncode(tia.get(GPConstants.COMMAND_LINE)) + "\">");
        out.println("<param name=\"" + RunVisualizerConstants.DEBUG + "\" value=\"1\">");
        out.print("<param name=\"" + RunVisualizerConstants.SUPPORT_FILE_NAMES + "\" value=\"");
        for (i = 0; i < supportFiles.length; i++) {
            if (i > 0)
                out.print(",");
            out.print(StringUtils.htmlEncode(supportFiles[i].getName()));
        }
        out.println("\">");

        out.print("<param name=\"" + RunVisualizerConstants.SUPPORT_FILE_DATES + "\" value=\"");

        for (i = 0; i < supportFiles.length; i++) {
            if (i > 0)
                out.print(",");
            out.print(supportFiles[i].lastModified());
        }
        out.println("\">");

        out.println("<param name=\"" + RunVisualizerConstants.LSID + "\" value=\"" + lsid + "\">");
        out.println("Your browser is ignoring this applet.");

        out.println("</applet>");
        out.flush();
    }

    /**
     * called after a task execution is complete
     * 
     * If this is for a visualizer, do nothing
     */
    public void recordTaskCompletion(JobInfo jobInfo, String name) {

        ParameterInfo[] jobParams = jobInfo.getParameterInfoArray();
        StringBuffer sbOut = new StringBuffer();
        int numOutputFiles = 0;
        // out.println("<tr><td colspan=5><div id=\"taskDiv"+ currentIndex
        // +"\"><table>");

        for (int j = 0; j < jobParams.length; j++) {
            if (!jobParams[j].isOutputFile()) {
                continue;
            }
            numOutputFiles++;
            sbOut.setLength(0);
            String fileName = new File("../../" + jobParams[j].getValue()).getName();
            String fileJobDirAndName = jobParams[j].getValue();

            boolean executionLog = false;
            if (fileName.equals(GPConstants.TASKLOG))
                executionLog = true;

            int idx = fileJobDirAndName.indexOf("/");
            String realJobId = fileJobDirAndName.substring(0, idx);

            String jobNumber = realJobId; // jobInfo.getJobNumber()

            String divId = "outFileDiv" + currentIndex + "_" + j;
            String cbDivId = "outFileCBDiv" + currentIndex + "_" + j;
            String cbId = "outFileCB" + currentIndex + "_" + numOutputFiles;
            String rowId = "fileRow" + currentIndex + "_" + numOutputFiles;
            if (executionLog) {
                rowId = "executionLogRow" + currentIndex;
                divId = "executionLogDiv" + currentIndex;
                cbDivId = "executionLogCBDiv" + currentIndex;

            }

            sbOut.append("<tr id=\"" + rowId
                    + "\" class=\"files\"><td>&nbsp;</td><td align=\"center\">&nbsp;<span align=\"center\" id=\""
                    + cbDivId + "\"><input id=\"" + cbId + "\" type=\"checkbox\" value=\"");
            sbOut.append(name + "/" + fileName + "=" + jobNumber + "/" + fileName);
            sbOut.append("\" name=\"dl\" ");
            sbOut.append("checked ></span></td><td>&nbsp;</td><td>&nbsp;<span id=\"" + divId
                    + "\"><a target=\"_blank\" href=\"");

            String outFileUrl = null;
            try {
                outFileUrl = URL + GET_TASK_FILE + "/" + jobNumber + "/" + URLEncoder.encode(fileName, "utf-8");
            } catch (UnsupportedEncodingException uee) {
                outFileUrl = URL + GET_TASK_FILE + "/" + jobNumber + "/" + fileName;
            }

            sbOut.append(localizeURL(outFileUrl));
            try {
                fileName = URLDecoder.decode(fileName, "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                // ignore
            }
            sbOut.append("\">" + htmlEncode(fileName) + "</a></span></td></tr>");

            out.println(sbOut.toString());
        }
        out.append("<script language=\"Javascript\">");
        out.append(" outputFileCount[" + currentIndex + "] = " + numOutputFiles + ";");
        out.append("</script>");
        out.append("</td></tr>");

        out.flush();

    }

    public void afterPipelineRan(PipelineModel model) {

        out.println("   <tr class=\"smalltype\">");
        out.println("  <td >&nbsp;</td>");

        out.println("  <td valign=\"top\"><nobr>");
        out
                .println("   <div class=\"smalltype\" align=\"center\"><a onclick=\"downloadCheckedFiles()\" href=\"#\">download</a> | <a onclick=\"deleteCheckedFiles()\" href=\"#\">delete</a> </div>");

        out.println("   </nobr></td><td>&nbsp;</td> <td><div align=\"right\"></div></td></tr>");

        out.println("</table>"); // lead with this as the subclass expects it

        // out.println("<center><input type=\"submit\" name=\"download\"
        // value=\"download selected results\">&nbsp;&nbsp;");
        // out.println("<a href=\"javascript:checkAll(this.form, true)\">check
        // all</a> &nbsp;&nbsp;");
        // out.println("<a href=\"javascript:checkAll(this.form,
        // false)\">uncheck all</a></center><br><center>");
        // out.println("<input type=\"submit\" name=\"delete\" value=\"delete
        // selected results\"");
        // out.println(" onclick=\"return confirm(\'Really delete the selected
        // files?\')\">");
        out.println("</form>");
        out.flush();
    }

    public void showVisualizerApplet(JobSubmission jobSubmission, int idx, int numSteps) {
    }

    protected String localizeURL(String original) {
        if (original == null)
            return "";
        String GENEPATTERN_PORT = "GENEPATTERN_PORT";
        String GENEPATTERN_URL = "GenePatternURL";
        String port = genepatternProps.getProperty(GENEPATTERN_PORT);
        original = StringUtils.replaceAll(original, GPConstants.LEFT_DELIMITER + GPConstants.LSID
                + GPConstants.RIGHT_DELIMITER, model.getLsid());
        original = StringUtils.replaceAll(original, GPConstants.LEFT_DELIMITER + GENEPATTERN_PORT
                + GPConstants.RIGHT_DELIMITER, port);
        original = StringUtils.replaceAll(original, GPConstants.LEFT_DELIMITER + GENEPATTERN_URL
                + GPConstants.RIGHT_DELIMITER, System.getProperty("GenePatternURL"));
        try {
            File f = new File(original);
            if (f.exists()) {
                try {
                    return getFileURL(f);
                } catch (IOException ioe) {
                    return original;
                }
            }

            // one of ours?
            if (!original.startsWith("http://localhost:" + port) && !original.startsWith("http://127.0.0.1:" + port)) {
                return original;
            }
            URL org = new URL(original);
            String localhost = InetAddress.getLocalHost().getCanonicalHostName();
            if (localhost.equals("localhost")) {
                // MacOS X can't resolve localhost when unplugged from network
                localhost = "127.0.0.1";
            }
            URL url = new URL(org.getProtocol() + "://" + localhost + ":" + port + org.getFile());
            return url.toString();
        } catch (UnknownHostException uhe) {
            return original;
        } catch (MalformedURLException mue) {
            // check if it is local file and write the URL for it

            return original;
        }
    }

    public String getFileURL(File theFile) throws IOException {

        // if it exists and lives under /jobResults, the parent dir where we are
        // running, we can
        // create a getFile link for it
        File tempDir = new File(System.getProperty("user.dir"), "here");
        tempDir = tempDir.getParentFile().getParentFile();

        StringBuffer pathBuffer = new StringBuffer();
        if (findCommonParentFile(theFile, tempDir, pathBuffer))
            return GET_FILE + pathBuffer.substring(1); // strip leading slash

        // now look again for Tomcat/temp. We know we should be in
        // Tomcat/webapps/gp/jobResults
        // so look up the path for the right alternate parent
        tempDir = tempDir.getParentFile().getParentFile().getParentFile();
        tempDir = new File(tempDir, "temp");
        pathBuffer = new StringBuffer();
        if (findCommonParentFile(theFile, tempDir, pathBuffer))
            return GET_FILE + pathBuffer.substring(1); // strip leading slash

        else
            return theFile.getName();

    }

    private boolean findCommonParentFile(File theFile, File tempDir, StringBuffer pathBuffer) {
        File parent = theFile;
        boolean foundCommonParent = false;

        while ((parent != null) && (!foundCommonParent)) {
            if (parent.equals(tempDir)) {
                foundCommonParent = true;
                break;
            }
            pathBuffer.insert(0, "/" + parent.getName());
            parent = parent.getParentFile();
        }
        return foundCommonParent;
    }

    // look for the file name. If it is a genepattern local url, then there will
    // be a file=
    // in it somewhere, we want to return what the file name is but not any
    // parent dir
    // information which is most likely a tempDir

    protected String getFileUrlDisplayValue(String original) {
        if (original == null)
            return "";

        try {
            // this throws an exception if not a URL and then we just return it
            // as is

            String queryPart = original;
            int idx = queryPart.indexOf("file=");
            if (idx == -1)
                return original;

            idx = idx + 5;
            int endIdx = queryPart.indexOf("&", idx);
            if (endIdx == -1)
                endIdx = queryPart.length();

            String filePath = queryPart.substring(idx, endIdx);

            if ((idx = filePath.indexOf("/")) != -1) {
                filePath = filePath.substring(idx + 1);
            }
            return filePath;

        } catch (Exception mue) {
            return original;
        }
    }

}