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


package org.genepattern.gpge.message;

import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.SuiteInfo;

public class ChangeViewMessageRequest extends AbstractTypedGPGEMessage {
	
	public static final int SHOW_RUN_TASK_REQUEST = 0;
	public static final int SHOW_EDIT_PIPELINE_REQUEST = 1;
	public static final int SHOW_VIEW_PIPELINE_REQUEST = 2;
	public static final int SHOW_GETTING_STARTED_REQUEST = 3;
	public static final int SHOW_EDIT_SUITE_REQUEST = 4;
	
	private Object objToDisplay;
	
	public ChangeViewMessageRequest(Object source, int type) {
		super(source, type);
	}
	
	public ChangeViewMessageRequest(Object source, int type, Object objToDisplay) {
		super(source, type);
		this.objToDisplay = objToDisplay;
	}

	public AnalysisService getAnalysisService() {
		return (AnalysisService) objToDisplay;
	}

	public SuiteInfo getSuiteInfo() {
		return (SuiteInfo) objToDisplay;
	}

}
