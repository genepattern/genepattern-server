package org.genepattern.gpge.message;

import org.genepattern.webservice.AnalysisService;

public class ChangeViewMessageRequest extends AbstractTypedGPGEMessage {
	
	public static final int SHOW_RUN_TASK_REQUEST = 0;
	public static final int SHOW_EDIT_PIPELINE_REQUEST = 1;
	public static final int SHOW_VIEW_PIPELINE_REQUEST = 2;
	public static final int SHOW_GETTING_STARTED_REQUEST = 3;
	private AnalysisService svc;
	
	public ChangeViewMessageRequest(Object source, int type) {
		super(source, type);
	}
	
	public ChangeViewMessageRequest(Object source, int type, AnalysisService svc) {
		super(source, type);
		this.svc = svc;
	}

	public AnalysisService getAnalysisService() {
		return svc;
	}

}
