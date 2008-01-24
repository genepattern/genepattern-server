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

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.webservice.JobInfo;

/**
 * This is the decorator for output from running a pipeline from the web
 * environment. It should generate the html for the runPipeline.jsp page as it
 * runs and also record a log file that will allow users to see when this
 * pipeline was run, execution times and output files
 */
public class RunPipelineLoggingHTMLDecorator extends RunPipelineHTMLDecorator {

	RunPipelineExecutionLogger logger =  new RunPipelineExecutionLogger();

	public void error(PipelineModel model, String message) {
		super.error(model, message);
		logger.error(model, message);
	}

	public void beforePipelineRuns(PipelineModel model) {
		super.beforePipelineRuns(model);
		logger.beforePipelineRuns(model);
		String divId = "executionLogDiv0";
		String rowId = "executionLogRow0" ;
	
		String jobID = System.getProperty("jobID");
		
		String fileName = model.getName() + "_execution_log.html";
		out.println("<tr id=\"executionLogRow0\" class=\"task-title\"><td>&nbsp;</td><td><div align=\"center\" id=\""+rowId+"\"><input type=\"checkbox\" value=\"" + fileName + "="
				+ jobID + "/" + fileName + "\" name=\"dl\" checked></td><td>&nbsp;</td><td>");
		out.println("<a target=\"_blank\" href=\"" + URL + GET_TASK_FILE
				+ "/" + jobID + "/" + fileName + "\">" + fileName
				+ "</a></div><div id=\""+divId+"\"></div></td></tr>");

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
