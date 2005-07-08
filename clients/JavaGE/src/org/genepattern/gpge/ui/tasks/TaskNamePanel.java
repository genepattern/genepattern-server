package org.genepattern.gpge.ui.tasks;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.PropertyManager;
import org.genepattern.gpge.message.MessageManager;
import org.genepattern.gpge.message.PreferenceChangeMessage;
import org.genepattern.gpge.ui.maindisplay.GPGE;
import org.genepattern.gpge.ui.preferences.PreferenceKeys;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.TaskInfo;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class TaskNamePanel extends JPanel {

	/**
	 * 
	 * @param taskInfo
	 * @param type the type of AnalysisServiceMessage to fire when the user selects a version from the versions combo box
	 */
	public TaskNamePanel(TaskInfo taskInfo, final int type) {
		super();
		String latestVersion = null;
		String taskName = taskInfo.getName();
		String taskVersionDisplay = "";
		JComboBox versionComboBox = null;
		try {
			final LSID lsid = new LSID((String) taskInfo
					.getTaskInfoAttributes().get(GPConstants.LSID));
			if (!org.genepattern.gpge.ui.maindisplay.LSIDUtil.isBroadTask(lsid)) {
				String authority = lsid.getAuthority();
				taskVersionDisplay += " (" + authority + ")";
			}

			final String lsidNoVersion = lsid.toStringNoVersion();
			List versions = (List) AnalysisServiceManager.getInstance()
					.getLSIDToVersionsMap().get(lsidNoVersion);
			Vector versionsCopy = new Vector();
			versionsCopy.add("");
			latestVersion = lsid.getVersion();
			if (versions != null) {
				for (int i = 0; i < versions.size(); i++) {
					String version = (String) versions.get(i);
					if (version.compareTo(latestVersion) > 0) {
						latestVersion = version;
					}
				}
			}
			if (lsid.getVersion().equals(latestVersion)) {
				taskVersionDisplay += ", version " + lsid.getVersion()
						+ " (latest)";
			} else {
				taskVersionDisplay += ", version " + lsid.getVersion();
			}

			if (versions != null) {
				for (int i = 0; i < versions.size(); i++) {
					String version = (String) versions.get(i);
					if (version.equals(lsid.getVersion())) {
						continue;
					}

					if (version.equals(latestVersion)) {
						version += " (latest)";
					}

					versionsCopy.add(version);
				}
			}
			Collections.sort(versionsCopy, String.CASE_INSENSITIVE_ORDER);

			if (versionsCopy.size() > 1) {
				versionComboBox = new JComboBox(versionsCopy);
				if (!GPGE.RUNNING_ON_MAC) {
					versionComboBox.setBackground(java.awt.Color.white);
				}
				if (lsid.getVersion().equals(latestVersion)) {
					versionComboBox.setSelectedItem(lsid.getVersion()
							+ " (latest)");
				} else {
					versionComboBox.setSelectedItem(lsid.getVersion());
				}

				versionComboBox.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String selectedItem = (String) ((JComboBox) e
								.getSource()).getSelectedItem();
						if (selectedItem.equals("")) {
							return;
						}
						int index = selectedItem.indexOf(" (latest");

						if (index > 0) {
							selectedItem = selectedItem.substring(0, index);
						}
						if (selectedItem.equals(lsid.getVersion())) {
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
							MessageManager.notifyListeners(new AnalysisServiceMessage(this, type, svc)); 
						}
					}
				});
			}
		} catch (MalformedURLException mfe) {
			mfe.printStackTrace();
		}
		Component taskNameComponent = new JLabel(taskName);
		taskNameComponent.setFont(taskNameComponent.getFont().deriveFont(
				java.awt.Font.BOLD));

		JLabel taskVersionLabel = new JLabel(taskVersionDisplay);

		Component description = AnalysisServiceDisplay.createWrappedLabel(taskInfo.getDescription());

		
		final JCheckBox showDescriptionsCheckBox = new JCheckBox(
				"Show Parameter Descriptions");
		boolean showDescriptions = Boolean.valueOf(PropertyManager
					.getProperty(PreferenceKeys.SHOW_PARAMETER_DESCRIPTIONS)).booleanValue();
	    
		showDescriptionsCheckBox.setSelected(showDescriptions);
		showDescriptionsCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean showDescriptions = showDescriptionsCheckBox
						.isSelected();
				PropertyManager.setProperty(PreferenceKeys.SHOW_PARAMETER_DESCRIPTIONS, String.valueOf(showDescriptions));
				MessageManager.notifyListeners(new PreferenceChangeMessage(this, PreferenceChangeMessage.SHOW_PARAMETER_DESCRIPTIONS, showDescriptions));
			}
		});
		if (versionComboBox != null) {
			JPanel temp = new JPanel(
					new FormLayout(
							"left:pref:none, left:pref:none, 12px, left:pref:none, 6px, left:pref:none, right:pref:g",
							"pref, 6px")); // title, task version, version label, version combo box, show parameter desc checkbox   
			CellConstraints cc = new CellConstraints();
			JLabel versionLabel = new JLabel("Choose Version:");
			temp.add(taskNameComponent, cc.xy(1, 1));
			temp.add(taskVersionLabel, cc.xy(2, 1));
			temp.add(versionLabel, cc.xy(4, 1));
			temp.add(versionComboBox, cc.xy(6, 1));
			temp.add(showDescriptionsCheckBox, cc.xy(7, 1));
			setLayout(new BorderLayout());
			add(temp, BorderLayout.NORTH);
			add(description, BorderLayout.SOUTH);
		} else {
			CellConstraints cc = new CellConstraints();
			JPanel temp = new JPanel(
					new FormLayout(
							"left:pref:none, left:pref:none, right:pref:g",
							"pref, 6px")); // title, task version, show parameter desc checkbox 

			temp.add(taskNameComponent, cc.xy(1, 1));
			temp.add(taskVersionLabel, cc.xy(2, 1));
			temp.add(showDescriptionsCheckBox, cc.xy(3, 1));
			setLayout(new BorderLayout());
			add(temp, BorderLayout.NORTH);
			add(description, BorderLayout.SOUTH);
		}
		setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

	}

}
