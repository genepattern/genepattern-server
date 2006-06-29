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


package org.genepattern.gpge.ui.tasks;

import java.awt.event.ActionListener;

import org.genepattern.gpge.CLThread;
import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.ui.maindisplay.LSIDUtil;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskIntegratorProxy;
import org.genepattern.webservice.WebServiceException;

public class TaskHelpActionListener implements ActionListener {
	private TaskInfo taskInfo;

	/** whether the <tt>selectedService</tt> has documentation */
	private volatile boolean hasDocumentation;

	
	public void setTaskInfo(TaskInfo t) {
		this.taskInfo = t;
		hasDocumentation = true;
		if (taskInfo != null) {
			new CLThread() {
				public void run() {
					try {

						String username = AnalysisServiceManager.getInstance()
								.getUsername();
						String server = AnalysisServiceManager.getInstance()
						.getServer();
						String lsid = LSIDUtil.getTaskId(taskInfo);
						String[] supportFileNames = new TaskIntegratorProxy(
								server, username).getDocFileNames(lsid);
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
			String server = AnalysisServiceManager.getInstance()
			.getServer();
			String lsid = LSIDUtil.getTaskId(taskInfo);

			if (hasDocumentation) {
				String docURL = server + "/gp/getTaskDoc.jsp?name=" + lsid
						+ "&" + GPConstants.USERID + "="
						+ java.net.URLEncoder.encode(username, "UTF-8");
				org.genepattern.util.BrowserLauncher.openURL(docURL);
			} else {
				GenePattern.showMessageDialog(taskInfo
						.getName()
						+ "has no documentation");
			}
		} catch (java.io.IOException ex) {
			System.err.println(ex);
		}
	}
}