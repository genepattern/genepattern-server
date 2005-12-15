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

import java.io.PrintStream;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.webservice.JobInfo;

public interface RunPipelineOutputDecoratorIF {

	public void setOutputStream(PrintStream outstr);

	public void beforePipelineRuns(PipelineModel model);

	public void recordTaskExecution(JobSubmission jobSubmission, int idx,
			int numSteps);

	public void recordTaskCompletion(JobInfo jobInfo, String name);

	public void afterPipelineRan(PipelineModel model);

	public void error(PipelineModel model, String message);
}