package org.genepattern.server.webapp;

import java.io.PrintStream;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.webservice.JobInfo;

/**
 * A RunPipelineOutputDecorator which does nothing.
 * Added for GP 3.2 because the pipeline execution log is generated from GenePatternAnalysisTask.
 * @author pcarr
 */
public class RunPipelineNoDecorator implements RunPipelineOutputDecoratorIF {
    public void afterPipelineRan(PipelineModel model) {
    }

    public void beforePipelineRuns(PipelineModel model) {
    }

    public void error(PipelineModel model, String message) {
    }

    public void recordTaskCompletion(JobInfo jobInfo, String name) {
    }

    public void recordTaskExecution(JobSubmission jobSubmission, int idx, int numSteps) {
   }

    public void setOutputStream(PrintStream outstr) {
    } 
}
