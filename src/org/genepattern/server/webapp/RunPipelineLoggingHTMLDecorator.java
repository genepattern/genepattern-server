package org.genepattern.server.webapp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.SimpleTimeZone;
import java.util.Vector;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;

/**
 * This is the decorator for output from running a pipeline from the web
 * environment. It should generate the html for the runPipeline.jsp page as it
 * runs and also record a log file that will allow users to see when this
 * pipeline was run, execution times and output files
 */
public class RunPipelineLoggingHTMLDecorator extends RunPipelineHTMLDecorator {

	RunPipelineExecutionLogger logger =  new RunPipelineExecutionLogger();

	public void beforePipelineRuns(PipelineModel model) {
		super.beforePipelineRuns(model);
		logger.beforePipelineRuns(model);

		String jobID = System.getProperty("jobID");
		
		String fileName = model.getName() + "_execution_log.html";
		out.println("<p><input type=\"checkbox\" value=\"" + fileName + "="
				+ jobID + "/" + fileName + "\" name=\"dl\" checked>");
		out.println("<a target=\"_blank\" href=\"" + URL + GET_TASK_FILE
				+ "job=" + jobID + "&filename=" + fileName + "\">" + fileName
				+ "</a><p>");

	}

	public void recordTaskExecution(JobSubmission jobSubmission, int idx,
			int numSteps) {
		super.recordTaskExecution(jobSubmission, idx, numSteps);
		logger.recordTaskExecution(jobSubmission, idx, numSteps);
	}

	public void recordTaskCompletion(JobInfo jobInfo, String name) {
		logger.recordTaskCompletion(jobInfo, name);

		super.recordTaskCompletion(jobInfo, name);

	}

	public void afterPipelineRan(PipelineModel model) {
		super.afterPipelineRan(model);
		logger.afterPipelineRan(model);
	}

}