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


package org.genepattern.gpge.ui.preferences;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;

import org.genepattern.gpge.ui.maindisplay.CenteredDialog;
import org.genepattern.gpge.ui.maindisplay.GPGE;
import org.genepattern.gpge.ui.tasks.AnalysisServiceManager;
import org.genepattern.webservice.AdminProxy;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.WebServiceException;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

/**
 * Allows users to change visible suites
 * 
 * @author Joshua Gould
 * 
 */
public class SuitesPreferences {

	private JCheckBox[] checkBoxes;

	private JRadioButton noFilterBtn;

	private JRadioButton filterBtn;

	private SuiteInfo[] suites;

	private boolean isFilteringInitially;

	private JDialog dialog;

	public SuitesPreferences(Frame parent) {
		dialog = new CenteredDialog(parent);
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
	}

	public void show() {
		if (dialog.isShowing()) {
			return;
		}
		dialog.getContentPane().removeAll();
		dialog.getContentPane().add(createSuitePanel());
		dialog.pack();
		dialog.show();
	}

	private void save() {
		List selectedSuites = new ArrayList();
		if (filterBtn.isSelected()) {
			for (int i = 0; i < checkBoxes.length; i++) {
				if (checkBoxes[i].isSelected()) {
					selectedSuites.add(suites[i]);
				}
			}
			if (selectedSuites.size() == 0) {
				selectedSuites = null;
			}
			AnalysisServiceManager.getInstance().setVisibleSuites(
					selectedSuites);
			GPGE.getInstance().rebuildTasksUI();
		} else {
			if (isFilteringInitially) { // only update if initially filtering
				// and now not filtering
				AnalysisServiceManager.getInstance().setVisibleSuites(null);
				GPGE.getInstance().rebuildTasksUI();
			}
		}
		StringBuffer suiteValue = new StringBuffer();
		if (selectedSuites != null) {
			for (int i = 0; i < selectedSuites.size(); i++) {
				SuiteInfo s = (SuiteInfo) selectedSuites.get(i);
				String lsid = s.getLsid();
				if (i > 0) {
					suiteValue.append(";");
				}
				suiteValue.append(lsid);
			}
		}
		// PropertyManager.setProperty(PreferenceKeys.SUITES,
		// suiteValue.toString());
		// try {
		// PropertyManager.saveProperties();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
	}

	private JPanel createSuitePanel() {
		noFilterBtn = new JRadioButton("No Filtering (show all tasks)");
		filterBtn = new JRadioButton(
				"Filter (show only tasks from selected suites)");
		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(noFilterBtn);
		buttonGroup.add(filterBtn);

		CellConstraints cc = new CellConstraints();

		FormLayout headerPanelLayout = new FormLayout("left:pref, left:pref",
				"");
		JPanel headerPanel = new JPanel(headerPanelLayout);
		headerPanelLayout.appendRow(new RowSpec("pref"));
		headerPanel.add(noFilterBtn, cc.xy(1, headerPanelLayout.getRowCount()));
		headerPanelLayout.appendRow(new RowSpec("pref"));
		headerPanel.add(filterBtn, cc.xy(1, headerPanelLayout.getRowCount()));
		headerPanelLayout.appendRow(new RowSpec("pref"));
		headerPanel.add(new JSeparator(), cc.xyw(1, headerPanelLayout
				.getRowCount(), 2));

		FormLayout formLayout = new FormLayout("left:pref, left:pref", "");
		final JPanel suitePanel = new JPanel(formLayout);

		JPanel buttonPanel = new JPanel();
		final JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		ActionListener saveListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.setVisible(false);
				if (e.getSource() == okBtn) {
					new Thread() {
						public void run() {
							save();
						}
					}.start();

				}

			}
		};
		okBtn.addActionListener(saveListener);
		cancelBtn.addActionListener(saveListener);
		buttonPanel.add(cancelBtn);
		buttonPanel.add(okBtn);

		ActionListener btnListener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == noFilterBtn) {
					suitePanel.setVisible(false);
				} else {
					suitePanel.setVisible(true);
					dialog.pack();
				}
			}

		};
		noFilterBtn.addActionListener(btnListener);
		filterBtn.addActionListener(btnListener);

		List selectedSuites = AnalysisServiceManager.getInstance()
				.getVisibleSuites();
		if (selectedSuites != null) {
			Collections.sort(selectedSuites, new Comparator() {

				public int compare(Object o1, Object o2) {
					SuiteInfo suite1 = (SuiteInfo) o1;
					SuiteInfo suite2 = (SuiteInfo) o2;
					return suite1.getName().compareToIgnoreCase(
							suite2.getName());
				}

			});
		}
		isFilteringInitially = selectedSuites != null;
		Set selectedSuiteLsids = new HashSet();

		if (selectedSuites != null) {
			filterBtn.setSelected(true);

			for (int i = 0; i < selectedSuites.size(); i++) {
				selectedSuiteLsids.add(((SuiteInfo) selectedSuites.get(i))
						.getLsid());
			}
		} else {
			noFilterBtn.setSelected(true);
		}
		try {
			AdminProxy proxy = new AdminProxy(AnalysisServiceManager
					.getInstance().getServer(), AnalysisServiceManager
					.getInstance().getUsername());

			suites = proxy.getLatestSuites();
			JLabel showSuiteLabel = null;
			if (suites == null || suites.length == 0) {
				showSuiteLabel = new JLabel("No suites available");
			} else {
				showSuiteLabel = new JLabel("Show suite");
			}

			showSuiteLabel.setFont(showSuiteLabel.getFont().deriveFont(
					showSuiteLabel.getFont().getSize2D() - 2));

			formLayout.appendRow(new RowSpec("pref"));
			suitePanel.add(showSuiteLabel, cc.xy(1, formLayout.getRowCount()));

			checkBoxes = new JCheckBox[suites.length];
			for (int i = 0; i < suites.length; i++) {
				SuiteInfo suiteInfo = suites[i];
				// suiteInfo.getDocumentationFiles()
				// suiteInfo.getDescription()

				JCheckBox showCheckBox = new JCheckBox();
				showCheckBox.setSelected(selectedSuiteLsids.contains(suiteInfo
						.getLsid()));
				checkBoxes[i] = showCheckBox;
				formLayout.appendRow(new RowSpec("pref"));
				suitePanel
						.add(showCheckBox, cc.xy(1, formLayout.getRowCount()));
				suitePanel.add(new JLabel(suiteInfo.getName()), cc.xy(2,
						formLayout.getRowCount()));
			}

		} catch (WebServiceException e) {
			e.printStackTrace();
		}

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(headerPanel, BorderLayout.NORTH);
		panel.add(suitePanel, BorderLayout.CENTER);
		panel.add(buttonPanel, BorderLayout.SOUTH);
		suitePanel.setVisible(isFilteringInitially);
		return panel;

	}

}
