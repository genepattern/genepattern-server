package org.genepattern.gpge.ui.tasks;

import org.genepattern.webservice.AnalysisJob;

public class JobEvent extends java.util.EventObject {
	private AnalysisJob job;

	public JobEvent(Object source, AnalysisJob job) {
		super(source);
		this.job = job;
	}

	public AnalysisJob getJob() {
		return job;
	}
}