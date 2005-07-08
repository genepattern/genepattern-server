package org.genepattern.gpge.ui.tasks;

import org.genepattern.gpge.message.AbstractTypedGPGEMessage;
import org.genepattern.webservice.AnalysisJob;

/**
 * Fired when running jobs
 * 
 * @author jgould
 */
public class JobMessage extends AbstractTypedGPGEMessage {
	private AnalysisJob job;

	public static int JOB_STATUS_CHANGED = 0;

	public static int JOB_COMPLETED = 1;

	public static int JOB_SUBMITTED = 2;

	public JobMessage(Object source, int type, AnalysisJob job) {
		super(source, type);
		this.job = job;
	}

	public AnalysisJob getJob() {
		return job;
	}
}