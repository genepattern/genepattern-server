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
		super(source);
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