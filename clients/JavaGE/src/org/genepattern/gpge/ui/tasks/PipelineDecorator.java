package org.genepattern.gpge.ui.tasks;

import java.io.PrintStream;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.gpge.GenePattern;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.AnalysisWebServiceProxy;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.JobStatus;
import org.genepattern.webservice.WebServiceException;
import org.genepattern.pipeline.RunPipelineOutputDecoratorIF;

public class PipelineDecorator implements RunPipelineOutputDecoratorIF{
    AnalysisWebServiceProxy proxy;
    int jobNumber;
    AnalysisJob analysisJob;
    
    public PipelineDecorator(AnalysisWebServiceProxy proxy, AnalysisJob analysisJob) {
        this.proxy = proxy;
        this.jobNumber = analysisJob.getJobInfo().getJobNumber();
        this.analysisJob = analysisJob;
    }
	public void setOutputStream(PrintStream outstr) {}

	public void beforePipelineRuns(PipelineModel model){}

	public void recordTaskExecution(JobSubmission jobSubmission, int idx,
			int numSteps) {
	}

	public void recordTaskCompletion(JobInfo jobInfo, String name) {
	    
	   ;//  JobModel.getInstance().jobCompleted(new AnalysisJob(server, jobInfo, false));
	}

	public void afterPipelineRan(PipelineModel model) {
	    JobInfo job = null;
        try {
            job = proxy.checkStatus(jobNumber);
            analysisJob.setJobInfo(job);
            JobModel.getInstance().jobCompleted(analysisJob);
        } catch (WebServiceException e) {
            e.printStackTrace();
        }
        
	}

	public void error(PipelineModel model, String message) {
	    JobInfo job = null;
        try {
            job = proxy.checkStatus(jobNumber);
            analysisJob.setJobInfo(job);
            JobModel.getInstance().jobStatusChanged(analysisJob);
        } catch (WebServiceException e) {
            e.printStackTrace();
        }
        
	    GenePattern.showErrorDialog(message);
	}
}