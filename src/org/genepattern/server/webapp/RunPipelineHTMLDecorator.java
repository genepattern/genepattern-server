package org.genepattern.server.webapp;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.util.StringUtils;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;

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

	protected static String GET_TASK_JSP = "addTask.jsp?view=1&name=";

	protected static String GET_TASK_FILE = "retrieveResults.jsp?";

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

		if ("false".equalsIgnoreCase(isSaved)) {
			out.print("running " + model.getName() + ".pipeline as ");
		} else {
			out.print("running ");
			if (model.getLsid().length() > 0) {
				out.print("<a href=\"" + URL + GET_PIPELINE_JSP
						+ model.getLsid() + "\">");
			}
			out.print(model.getName() + ".pipeline");
			out.print("</a>");
			out.print(" as ");
		}
		out.println("<a href=\"" + URL + GET_JOB_JSP + jobID + "\">job #"
				+ jobID + "</a> on " + (new Date()));
		out.println("<p>");

		out.print("Pipeline summary: ");
		int taskNum = 0;
		Vector vTasks = model.getTasks();
		for (Enumeration eTasks = vTasks.elements(); eTasks.hasMoreElements(); taskNum++) {
			JobSubmission jobSubmission = (JobSubmission) eTasks.nextElement();
			out.print("<a href=\"" + URL + GET_TASK_JSP
					+ jobSubmission.getLSID() + "\">");
			out.print(jobSubmission.getName());
			out.print("</a>");
			if (taskNum < (vTasks.size() - 1))
				out.print(", ");
		}
		out.println("<p>");

		// set up the form for zip results
		out.println("<form name=\"results\" action=\"" + URL
				+ "zipJobResults.jsp\">");
		out.println("<input type=\"hidden\" name=\"name\" value=\""
				+ model.getName() + "\">");
		out.println("<input type=\"hidden\" name=\"jobID\" value=\"" + jobID
				+ "\">");

		// set up the table for task reporting
		out
				.println("<table width=\"90%\"><tr><td><u>step</u></td><td><u>name and parameters</u></td></tr>");
		out.flush();
	}

	/**
	 * called before a task is executed
	 * 
	 * If this is for a visualizer, write out the applet code
	 */
	public void recordTaskExecution(JobSubmission jobSubmission, int idx,
			int numSteps) {

		out.print("<tr><td valign=top width=20><nobr>" + idx + " of "
				+ numSteps + "</nobr></td>");
		out.print("<td valign=top>" + jobSubmission.getName() + "(");
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
			isInputFile = (aParam.getName().indexOf("filename") != -1);

			out.print(aParam.getName().replace('.',' '));
			out.print("=");
			if (isInputFile) {
				// convert from "localhost" to the actual host name so that
				// it can be referenced from anywhere (eg. visualizer on
				// non-local client)
				aParam.setValue(localizeURL(aParam.getValue()));
				out.print("<a href=\"");
				out.print(aParam.getValue());
				out.print("\">");

			}
			out.print(htmlEncode(aParam.getValue()));

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

	// output the applet tag for a visdualizer
	public void writeVisualizerAppletTag(JobSubmission jobSubmission) {
		// PUT APPLET HERE
		StringBuffer buff = new StringBuffer();
		buff.append(URL);
		buff.append("runVisualizer.jsp?name=");
		buff.append(jobSubmission.getLSID());
		buff.append("&userid=");
		buff.append(System.getProperty("userID", ""));
		ParameterInfo[] parameterInfo = jobSubmission.giveParameterInfoArray();
		for (int i = 0; i < parameterInfo.length; i++) {
			buff.append("&");
			ParameterInfo aParam = parameterInfo[i];
			try {
				buff.append(URLEncoder.encode(aParam.getName(), "utf-8"));
				buff.append("=");
				buff.append(URLEncoder.encode(aParam.getValue(), "utf-8"));
			} catch (UnsupportedEncodingException uee) {
				// ignore
			}
		}

		try {
			URL url = new URL(buff.toString());
			Object appletTag = url.getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					(InputStream) appletTag));
			String line = null;
			while ((line = reader.readLine()) != null) {
				out.println(line);
			}

		} catch (Exception mue) {
			out.println("Could not create applet tag " + mue);
			mue.printStackTrace();
		}
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

		for (int j = 0; j < jobParams.length; j++) {
			if (!jobParams[j].isOutputFile()) {
				continue;
			}

			sbOut.setLength(0);
			String fileName = new File("../../" + jobParams[j].getValue())
					.getName();

			sbOut.append("<tr><td></td><td><input type=\"checkbox\" value=\"");
			sbOut.append(name + "/" + fileName + "=" + jobInfo.getJobNumber()
					+ "/" + fileName);
			sbOut.append("\" name=\"dl\" ");
			sbOut.append("checked><a target=\"_blank\" href=\"");

			String outFileUrl = null;
			try {
				outFileUrl = URL + GET_TASK_FILE + "job="
						+ jobInfo.getJobNumber() + "&filename="
						+ URLEncoder.encode(fileName, "utf-8");
			} catch (UnsupportedEncodingException uee) {
				outFileUrl = URL + GET_TASK_FILE + "job="
						+ jobInfo.getJobNumber() + "&filename=" + fileName;
			}

			sbOut.append(localizeURL(outFileUrl));
			try {
				fileName = URLDecoder.decode(fileName, "UTF-8");
			} catch (UnsupportedEncodingException uee) {
				// ignore
			}
			sbOut.append("\">" + htmlEncode(fileName) + "</a></td></tr>");
			out.println(sbOut.toString());
		}
		out.flush();

	}

	public void afterPipelineRan(PipelineModel model) {
		out.println("</table>"); // lead with this as the subclass expects it

		out
				.println("<center><input type=\"submit\" name=\"download\" value=\"download selected results\">&nbsp;&nbsp;");
		out
				.println("<a href=\"javascript:checkAll(this.form, true)\">check all</a> &nbsp;&nbsp;");
		out
				.println("<a href=\"javascript:checkAll(this.form, false)\">uncheck all</a></center><br><center>");
		out
				.println("<input type=\"submit\" name=\"delete\" value=\"delete selected results\"");
		out
				.println(" onclick=\"return confirm(\'Really delete the selected files?\')\">");
		out.println("</form>");
		out.flush();
	}

	public void showVisualizerApplet(JobSubmission jobSubmission, int idx,
			int numSteps) {
	}

	protected String localizeURL(String original) {
		if (original == null)
			return "";
		String GENEPATTERN_PORT = "GENEPATTERN_PORT";
		String GENEPATTERN_URL = "GenePatternURL";
		String port = genepatternProps.getProperty(GENEPATTERN_PORT);
		original = StringUtils.replaceAll(original, GPConstants.LEFT_DELIMITER
				+ GPConstants.LSID + GPConstants.RIGHT_DELIMITER, model
				.getLsid());
		original = StringUtils.replaceAll(original, GPConstants.LEFT_DELIMITER
				+ GENEPATTERN_PORT + GPConstants.RIGHT_DELIMITER, port);
		original = StringUtils.replaceAll(original, GPConstants.LEFT_DELIMITER
				+ GENEPATTERN_URL + GPConstants.RIGHT_DELIMITER, System
				.getProperty("GenePatternURL"));
		try {
			// one of ours?
			if (!original.startsWith("http://localhost:" + port)
					&& !original.startsWith("http://127.0.0.1:" + port)) {
				return original;
			}
			URL org = new URL(original);
			String localhost = InetAddress.getLocalHost()
					.getCanonicalHostName();
			if (localhost.equals("localhost")) {
				// MacOS X can't resolve localhost when unplugged from network
				localhost = "127.0.0.1";
			}
			URL url = new URL("http://" + localhost + ":" + port
					+ org.getFile());
			return url.toString();
		} catch (UnknownHostException uhe) {
			return original;
		} catch (MalformedURLException mue) {
			return original;
		}
	}

}