package edu.mit.genome.gp.ui.analysis;

import java.io.PrintStream;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.webservice.JobInfo;
import org.genepattern.server.webapp.RunPipelineOutputDecoratorIF;

/**
 * Forwards all methods to
 * org.genepattern.server.webapp.RunPipelineNullDecorator
 * 
 * @author Joshua Gould
 */
public class RunPipelineNullDecorator implements RunPipelineOutputDecoratorIF {
	RunPipelineNullDecorator decorator = new RunPipelineNullDecorator();

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