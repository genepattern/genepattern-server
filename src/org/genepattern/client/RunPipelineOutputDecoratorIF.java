package org.genepattern.client;


import java.io.PrintStream;

import org.genepattern.analysis.JobInfo;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;

public interface RunPipelineOutputDecoratorIF {

	public void setOutputStream(PrintStream outstr);

	public void beforePipelineRuns(PipelineModel model);

	public void recordTaskExecution(JobSubmission jobSubmission, int idx, int numSteps);	

	public void recordTaskCompletion(JobInfo jobInfo, String name);

	public void afterPipelineRan(PipelineModel model);
}