package org.genepattern.gpge.ui.analysis;


import java.io.PrintStream;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.analysis.JobInfo;

public interface RunPipelineOutputDecoratorIF {

	public void setOutputStream(PrintStream outstr);

	public void beforePipelineRuns(PipelineModel model);

	public void recordTaskExecution(JobSubmission jobSubmission, int idx, int numSteps);	

	public void recordTaskCompletion(JobInfo jobInfo, String name);

	public void afterPipelineRan(PipelineModel model);
}