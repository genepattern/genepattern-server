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


package edu.mit.genome.gp.ui.analysis;

import java.io.PrintStream;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.webservice.JobInfo;
import org.genepattern.server.webapp.RunPipelineOutputDecoratorIF;
import org.genepattern.server.webapp.*;

/**
 * Forwards all methods to
 * org.genepattern.server.webapp.RunPipelineNullDecorator
 * 
 * @author Joshua Gould
 */
public class RunPipelineNullDecorator implements RunPipelineOutputDecoratorIF {
	org.genepattern.server.webapp.RunPipelineNullDecorator decorator = new org.genepattern.server.webapp.RunPipelineNullDecorator();

	public void error(PipelineModel model, String message) {
		decorator.error(model, message);
	}

	public void beforePipelineRuns(PipelineModel model) {
		decorator.beforePipelineRuns(model);
	}

	public void recordTaskExecution(JobSubmission jobSubmission, int idx,
			int numSteps) {
		decorator.recordTaskExecution(jobSubmission, idx, numSteps);
	}

	public void recordTaskCompletion(JobInfo jobInfo, String name) {
		decorator.recordTaskCompletion(jobInfo, name);
	}

	public void afterPipelineRan(PipelineModel model) {
		decorator.afterPipelineRan(model);
	}

	public void setOutputStream(PrintStream outstr) {
		decorator.setOutputStream(outstr);
	}

}