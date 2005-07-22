package org.genepattern.gpge.ui.tasks;

import java.awt.event.ActionListener;

import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.ui.maindisplay.LSIDUtil;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.TaskIntegratorProxy;
import org.genepattern.webservice.WebServiceException;

public class TaskHelpActionListener implements ActionListener {
	private AnalysisService selectedService;

	/** whether the <tt>selectedService</tt> has documentation */
	private volatile boolean hasDocumentation;

	public TaskHelpActionListener(AnalysisService svc) {
		this.selectedService = svc;
		hasDocumentation = true;
		if (selectedService != null) {
			new Thread() {
				public void run() {
					try {

						String username = AnalysisServiceManager.getInstance()
								.getUsername();
						String server = selectedService.getServer();
						String lsid = LSIDUtil.getTaskId(selectedService
								.getTaskInfo());
						String[] supportFileNames = new TaskIntegratorProxy(
								server, username).getSupportFileNames(lsid);
						hasDocumentation = supportFileNames != null
								&& supportFileNames.length > 0;
					} catch (WebServiceException wse) {
						wse.printStackTrace();
					}
				}
			}.start();
		}
	}

	public final void actionPerformed(java.awt.event.ActionEvent ae) {
		try {
			String username = AnalysisServiceManager.getInstance()
					.getUsername();
			String server = selectedService.getServer();
			String lsid = LSIDUtil.getTaskId(selectedService.getTaskInfo());

			if (hasDocumentation) {
				String docURL = server + "/gp/getTaskDoc.jsp?name=" + lsid
						+ "&" + GPConstants.USERID + "="
						+ java.net.URLEncoder.encode(username, "UTF-8");
				org.genepattern.util.BrowserLauncher.openURL(docURL);
			} else {
				GenePattern.showMessageDialog(selectedService.getTaskInfo()
						.getName()
						+ "has no documentation");
			}
		} catch (java.io.IOException ex) {
			System.err.println(ex);
		}
	}
}