package org.genepattern.gpge.ui.tasks;

import org.genepattern.webservice.AnalysisService;

/**
 * An event emitted by <tt>AnalysisServicePanel</tt> that indicates an <tt>
 *  AnalysisService</tt>
 * has been selected
 * 
 * @author Joshua Gould
 */
public class AnalysisServiceSelectionEvent extends java.util.EventObject {
	private AnalysisService svc;

	public AnalysisServiceSelectionEvent(Object source, AnalysisService svc) {
		super(svc);
		this.svc = svc;
	}

	public AnalysisService getAnalysisService() {
		return svc;
	}
}