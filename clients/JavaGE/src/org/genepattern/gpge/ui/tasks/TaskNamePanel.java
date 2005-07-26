package org.genepattern.gpge.ui.tasks;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.PropertyManager;
import org.genepattern.gpge.message.ChangeViewMessageRequest;
import org.genepattern.gpge.message.MessageManager;
import org.genepattern.gpge.message.PreferenceChangeMessage;
import org.genepattern.gpge.ui.maindisplay.GPGE;
import org.genepattern.gpge.ui.preferences.PreferenceKeys;
import org.genepattern.gpge.ui.util.GUIUtil;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.TaskInfo;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class TaskNamePanel extends JPanel {
	
	public static class LSIDVersionComparator implements Comparator{

		public int compare(Object arg0, Object arg1) {
			String s0 = (String) arg0;
			String s1 = (String) arg1;
			String[] s0Tokens = s0.split(".");
			String[] s1Tokens = s1.split(".");
			int min = Math.min(s0Tokens.length, s1Tokens.length);
			for(int i = 0; i < min; i++) {
				int s0Int = Integer.parseInt(s0Tokens[i]);
				int s1Int = Integer.parseInt(s1Tokens[i]);
				if(s0Int < s1Int) {
					return -1;
				} else if(s0Int > s1Int) {
					return 1;
				}
			}
			if(s0Tokens.length > s1Tokens.length) {
				return 1;
			}
			return -1;
		}

		
	}
	public TaskNamePanel(TaskInfo taskInfo, final int type) {
		this(taskInfo, type, null);
	}

	/**
	 * 
	 * @param taskInfo
	 * @param type
	 *            the type of AnalysisServiceMessage to fire when the user
	 *            selects a version from the versions combo box
	 * @param bottomComponent the component to display at the bttom of this panel
	 */
	public TaskNamePanel(TaskInfo taskInfo, final int type, Component bottomComponent) {
		super();
		String latestVersion = null;
		String taskName = taskInfo.getName();
		if (taskName.endsWith(".pipeline")) {
			taskName = taskName.substring(0, taskName.length()
					- ".pipeline".length());
		}
		
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
				versionsCopy.addAll(versions);
				versionsCopy.remove(lsid.getVersion());
			}
			Collections.sort(versionsCopy, new LSIDVersionComparator());
			versionsCopy.add(0, "");
			
			if (versionsCopy.size() > 1) {
				versionComboBox = new JComboBox(versionsCopy);
				if (!GPGE.RUNNING_ON_MAC) {
					versionComboBox.setBackground(java.awt.Color.white);
				}
				versionComboBox.setSelectedItem(lsid.getVersion());
				
				versionComboBox.addActionListener(new ActionListener() {
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
		Component taskNameComponent = new JLabel(taskName);
		taskNameComponent.setFont(taskNameComponent.getFont().deriveFont(
				java.awt.Font.BOLD));

		JLabel taskVersionLabel = new JLabel(taskVersionDisplay);

		Component description = GUIUtil
				.createWrappedLabel(taskInfo.getDescription());
		if(bottomComponent==null) {
			bottomComponent = createBottomComponent();
		
		}
		if (versionComboBox != null) {
			FormLayout formLayout = new FormLayout(
					"left:pref:none, left:pref:none, 12px, left:pref:none, 6px, left:pref:none",
					"pref"); // title, task version, version
									// label, version combo box, show
					
			JPanel temp = new JPanel(formLayout);
					
			CellConstraints cc = new CellConstraints();
			JLabel versionLabel = new JLabel("Choose Version:");
			temp.add(taskNameComponent, cc.xy(1, 1));
			temp.add(taskVersionLabel, cc.xy(2, 1));
			temp.add(versionLabel, cc.xy(4, 1));
			temp.add(versionComboBox, cc.xy(6, 1));
			JPanel apu = new JPanel(new BorderLayout());
			setLayout(new BorderLayout());
			apu.add(temp, BorderLayout.NORTH);
			apu.add(description, BorderLayout.SOUTH);
			add(apu, BorderLayout.CENTER);
			add(bottomComponent, BorderLayout.SOUTH);
			
		} else {
			CellConstraints cc = new CellConstraints();
			JPanel temp = new JPanel(
					new FormLayout(
							"left:pref:none, left:pref:none",
							"pref")); // title, task version

			temp.add(taskNameComponent, cc.xy(1, 1));
			temp.add(taskVersionLabel, cc.xy(2, 1));
			
			JPanel apu = new JPanel(new BorderLayout());
			setLayout(new BorderLayout());
			apu.add(temp, BorderLayout.NORTH);
			apu.add(description, BorderLayout.SOUTH);
			add(apu, BorderLayout.CENTER);
			add(bottomComponent, BorderLayout.SOUTH);
			
		}
		setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

	}

	protected Component createBottomComponent() {
		final JCheckBox showDescriptionsCheckBox = new JCheckBox(
				"Show Parameter Descriptions");
		boolean showDescriptions = Boolean
				.valueOf(
						PropertyManager
								.getProperty(PreferenceKeys.SHOW_PARAMETER_DESCRIPTIONS))
				.booleanValue();

		showDescriptionsCheckBox.setSelected(showDescriptions);
		showDescriptionsCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean showDescriptions = showDescriptionsCheckBox
						.isSelected();
				PropertyManager.setProperty(
						PreferenceKeys.SHOW_PARAMETER_DESCRIPTIONS, String
								.valueOf(showDescriptions));
				MessageManager.notifyListeners(new PreferenceChangeMessage(
						this,
						PreferenceChangeMessage.SHOW_PARAMETER_DESCRIPTIONS,
						showDescriptions));
			}
		});
		return showDescriptionsCheckBox;

	}

}
