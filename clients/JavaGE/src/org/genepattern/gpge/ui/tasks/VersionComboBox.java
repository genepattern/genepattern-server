package org.genepattern.gpge.ui.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.util.List;

import javax.swing.JComboBox;

import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.message.ChangeViewMessageRequest;
import org.genepattern.gpge.message.MessageManager;
import org.genepattern.gpge.ui.maindisplay.GPGE;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.TaskInfo;

/**
 * A combo box that allows the user to select a version for a task
 * 
 * @author jgould
 *
 */
public class VersionComboBox extends JComboBox {

	/**
	 * @param taskInfo
	 * @param type
	 */
	public VersionComboBox(String lsidString, final int type) {
		try {
			final LSID lsid = new LSID(lsidString);

			final String lsidNoVersion = lsid.toStringNoVersion();
			List versions = (List) AnalysisServiceManager.getInstance()
					.getLSIDToVersionsMap().get(lsidNoVersion);
			
			if (versions != null) {
				String version = lsid.getVersion();
				for(int i = 0; i < versions.size(); i++) {
					String s = (String) versions.get(i);
					addItem(s);
				}
				setSelectedItem(version);
			}

			if (getItemCount() > 1) {

				if (!GPGE.RUNNING_ON_MAC) {
					this.setBackground(java.awt.Color.white);
				}
				this.setSelectedItem(lsid.getVersion());

				this.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String selectedItem = (String) ((JComboBox) e
								.getSource()).getSelectedItem();
						if (selectedItem.equals("")) {
							return;
						}

						String selectedLSID = lsidNoVersion + ":"
								+ selectedItem;
						AnalysisService svc = AnalysisServiceManager
								.getInstance().getAnalysisService(selectedLSID);
						if (svc == null) {
							GenePattern
									.showMessageDialog("The task was not found.");
						} else {
							MessageManager
									.notifyListeners(new ChangeViewMessageRequest(
											this, type, svc));
						}
					}
				});
			}
		} catch (MalformedURLException mfe) {
			mfe.printStackTrace();
		}
	}

}
