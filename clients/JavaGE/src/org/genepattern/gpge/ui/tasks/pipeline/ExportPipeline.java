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


package org.genepattern.gpge.ui.tasks.pipeline;

import java.io.File;

import javax.swing.JOptionPane;

import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.ui.tasks.AnalysisServiceManager;
import org.genepattern.gpge.ui.util.GUIUtil;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.TaskIntegratorProxy;
import org.genepattern.webservice.WebServiceException;

/**
 * 
 * @author Joshua Gould
 * 
 */
public class ExportPipeline {

	/**
	 * Displays a GUI for exporting the given analysis service
	 * 
	 * @param selectedService
	 */
	public ExportPipeline(final AnalysisService selectedService) {

		try {
			final TaskIntegratorProxy proxy = new TaskIntegratorProxy(
					AnalysisServiceManager.getInstance().getServer(),
					AnalysisServiceManager.getInstance().getUsername());

			String[] text = { "Include Tasks", "Pipeline Only", "Cancel Export" };
			String message = "Press 'Include tasks' to include all tasks used by "
					+ selectedService.getName()
					+ " in the exported zip file.\nPress 'Pipeline only' to include only the "
					+ selectedService.getName() + " definition itself.";
			int result = GUIUtil.showYesNoCancelDialog(GenePattern
					.getDialogParent(), "GenePattern", message, text);
			boolean _recursive = true;
			if (result == JOptionPane.YES_OPTION) {
				_recursive = true;
			} else if (result == JOptionPane.NO_OPTION) {
				_recursive = false;
			} else { // cancel
				return;
			}
			final boolean recursive = _recursive;
			final File destination = GUIUtil.showSaveDialog(new File(selectedService.getName() + ".zip"),
					"Select destination zip file");
			if (destination == null) {
				return;
			}
			new Thread() {
				public void run() {
					try {
						proxy.exportToZip(selectedService.getLsid(), recursive,
								destination);
					} catch (WebServiceException e) {
						e.printStackTrace();
						if (!GenePattern.disconnectedFromServer(e)) {
							GenePattern
									.showErrorDialog("An error occurred while saving the file "
											+ destination.getName() + ".");
						}
					}
				}
			}.start();

		} catch (WebServiceException e1) {
			e1.printStackTrace();
			if (!GenePattern.disconnectedFromServer(e1)) {
				GenePattern
						.showErrorDialog("An error occurred while connecting to the server.");
			}
		}
	}

}
