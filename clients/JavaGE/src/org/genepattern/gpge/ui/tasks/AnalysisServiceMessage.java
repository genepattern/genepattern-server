package org.genepattern.gpge.ui.tasks;

import org.genepattern.gpge.message.AbstractGPGEMessage;
import org.genepattern.gpge.message.AbstractTypedGPGEMessage;
import org.genepattern.webservice.AnalysisService;

/**
 * A message that indicates an <tt>AnalysisService</tt> has been selected
 * 
 * @author Joshua Gould
 */
public class AnalysisServiceMessage extends AbstractTypedGPGEMessage {
	private AnalysisService svc;

	public static final int RUN_TASK = 0;

	public static final int EDIT_PIPELINE = 1;

	public static final int VIEW_PIPELINE = 2;

	public AnalysisServiceMessage(Object source, int type, AnalysisService svc) {
		super(source, type);
		this.svc = svc;
	}

	/**
	 * Gets the analysis service that is selected or <tt>null</tt> if no analysis service is selected
	 * @return the analysis service
	 */
	public AnalysisService getAnalysisService() {
		return svc;
	}
}