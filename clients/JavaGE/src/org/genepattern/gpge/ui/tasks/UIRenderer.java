/*
 * UIRenderer.java
 *
 * Created on March 26, 2003, 9:36 AM
 */

package org.genepattern.gpge.ui.tasks;

import java.util.List;
import java.util.Map;

import javax.swing.JComponent;

import org.genepattern.webservice.AnalysisService;

/**
 * Defines methods for creating a UI for tasks.
 * 
 * @author kohm
 */
public interface UIRenderer {
	/**
	 * renders as many parameters as it can on the supplied JComponent The input
	 * List of ParameterInfo objects will contain those that were not processed.
	 * The specified Map will map parameter names to ParamRetrievor instances.
	 * 
	 * @param container
	 *            where to put most of the graphical components
	 * @param service
	 *            the analysis service that defines a task
	 * @param params
	 *            the input parameters
	 * @param name_retriever
	 *            a map of the parameter names to a parameter value retrievor
	 *            object
	 */
	public void render(final JComponent container,
			final AnalysisService service, final List params,
			final Map name_retriever);

	/**
	 * creates a component that is the label for the analysis service.
	 * 
	 * @param service
	 *            the analysis service that defines a task
	 * @return JComponent the label or name of this task
	 */
	public JComponent createTaskLabel(final AnalysisService service);

	/**
	 * creates a component that is or contains another component that the user
	 * can press (a JButton) to submit the job
	 * 
	 * @param listener
	 *            the listener for the submit button to be pressed
	 * @return JComponent that is or contains some component like a submit
	 *         button.
	 */
	public JComponent createSubmitPanel(final AnalysisService service,
			final java.awt.event.ActionListener listener);

	/**
	 * creates a component that is or contains another component that the user
	 * can press (a JButton) to submit the job
	 * 
	 * @param submitListener
	 *            the listener for the submit button to be pressed
	 * @param resetListener
	 *            the listener for the reset button to be pressed
	 * @return JComponent that is or contains some component like a submit
	 *         button.
	 */
	public JComponent createSubmitPanel(final AnalysisService service,
			final java.awt.event.ActionListener submitListener,
			java.awt.event.ActionListener resetListener);
}