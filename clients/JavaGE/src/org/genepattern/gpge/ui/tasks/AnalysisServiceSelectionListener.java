package org.genepattern.gpge.ui.tasks;

/**
 * A listener for receiving analysis service selection events.
 * 
 * @author Joshua Gould
 */
public interface AnalysisServiceSelectionListener extends
		java.util.EventListener {
	public void valueChanged(AnalysisServiceSelectionEvent e);
}