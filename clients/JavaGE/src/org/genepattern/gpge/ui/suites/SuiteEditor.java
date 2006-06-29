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


package org.genepattern.gpge.ui.suites;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.genepattern.gpge.CLThread;
import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.message.ChangeViewMessageRequest;
import org.genepattern.gpge.message.GPGEMessage;
import org.genepattern.gpge.message.GPGEMessageListener;
import org.genepattern.gpge.message.MessageManager;
import org.genepattern.gpge.message.RefreshMessage;
import org.genepattern.gpge.message.SuiteInstallMessage;
import org.genepattern.gpge.ui.maindisplay.TogglePanel;
import org.genepattern.gpge.ui.tasks.AnalysisServiceManager;
import org.genepattern.gpge.ui.tasks.AnalysisServiceUtil;
import org.genepattern.gpge.ui.tasks.VersionComboBox;
import org.genepattern.gpge.ui.util.GUIUtil;
import org.genepattern.util.BrowserLauncher;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.AdminProxy;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskIntegratorProxy;
import org.genepattern.webservice.WebServiceException;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Used to create/edit a suite
 * 
 * @author Joshua Gould
 * 
 */
public class SuiteEditor extends JPanel {

	public HeaderPanel headerPanel;

	private ArrayList checkBoxes = new ArrayList();

	private SuiteInfo suiteInfo;

	public SuiteEditor() {
		MessageManager.addGPGEMessageListener(new GPGEMessageListener() {

			public void receiveMessage(GPGEMessage message) {
				if (message instanceof RefreshMessage && isShowing()) {
					String lsid = null;
					if (suiteInfo != null) {
						lsid = suiteInfo.getLsid();
					}
					if (lsid != null && !lsid.equals("")) {
						try {
							final AdminProxy proxy = new AdminProxy(
									AnalysisServiceManager.getInstance()
											.getServer(),
									AnalysisServiceManager.getInstance()
											.getUsername());
							proxy.getSuite(lsid);

						} catch (WebServiceException e) {
							GenePattern.showMessageDialog(suiteInfo.getName()
									+ " has been deleted from the server.");
						}
					}
				}
			}

		});

	}

	private static String createRowSpec(int rows) {
		StringBuffer rowBuff = new StringBuffer();
		for (int i = 0; i < rows; i++) {
			if (i > 0) {
				rowBuff.append(", ");
			}
			rowBuff.append("pref");

		}
		return rowBuff.toString();
	}

	static class TaskSelector {

		private JCheckBox cb;

		private LSID lsid;

		private JComboBox versionsComboBox;

		public TaskSelector(JCheckBox cb, LSID lsid, JComboBox versionsComboBox) {
			this.cb = cb;
			this.lsid = lsid;
			this.versionsComboBox = versionsComboBox;
		}

	}

	public void display(SuiteInfo _suiteInfo) {
		checkBoxes.clear();
		removeAll();
		if (_suiteInfo == null) {
			_suiteInfo = new SuiteInfo();
			_suiteInfo.setLsid(null);
			AnalysisServiceManager asm = AnalysisServiceManager.getInstance();
			_suiteInfo.setAuthor(asm.getUsername());
			_suiteInfo.setOwner(asm.getUsername());
		}
		this.suiteInfo = _suiteInfo;

		Set moduleLsids = new HashSet();
		if (suiteInfo.getModuleLsids() != null) {
			moduleLsids.addAll(Arrays.asList(suiteInfo.getModuleLsids()));
		}
		Map categoryToAnalysisServices = AnalysisServiceUtil
				.getCategoryToAnalysisServicesMap(AnalysisServiceManager
						.getInstance().getLatestAnalysisServices(true));
		Map lsid2VersionsMap = AnalysisServiceManager.getInstance()
				.getLSIDToVersionsMap();
		CellConstraints cc = new CellConstraints();

		int[] totals = new int[categoryToAnalysisServices.size()];
		int index = 0;
		int numTasks = 0;
		for (Iterator keys = categoryToAnalysisServices.keySet().iterator(); keys
				.hasNext();) {
			String category = (String) keys.next();
			List services = (List) categoryToAnalysisServices.get(category);
			numTasks += services.size();
			if (index > 0) {
				totals[index] = totals[index - 1] + services.size();
			} else {
				totals[index] = services.size();
			}
			index++;

		}
		int halfway = numTasks / 2;
		int categoryIndex = -1;
		for (int i = 0; i < totals.length; i++) {
			if (totals[i] > halfway) {
				categoryIndex = i;
				break;
			}
		}

		FormLayout leftLayout = new FormLayout("pref",
				createRowSpec(categoryIndex + 1));
		FormLayout rightLayout = new FormLayout("pref",
				createRowSpec(categoryToAnalysisServices.size() - categoryIndex
						+ 1));

		JPanel leftTasksPanel = new JPanel(leftLayout);
		JPanel rightTasksPanel = new JPanel(rightLayout);

		int row = 1;
		JPanel panel = leftTasksPanel;
		for (Iterator keys = categoryToAnalysisServices.keySet().iterator(); keys
				.hasNext();) {

			String category = (String) keys.next();
			List services = (List) categoryToAnalysisServices.get(category);

			JPanel categoryPanel = new JPanel(new FormLayout(
					"left:pref:g(0), left:pref:g(1)", createRowSpec(services
							.size() + 1)));

			categoryPanel
					.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
			JLabel categoryLabel = new JLabel(category);
			categoryLabel
					.setFont(categoryLabel.getFont().deriveFont(Font.BOLD));

			categoryPanel.add(categoryLabel, cc.xyw(1, 1, 2));

			for (int i = 0; i < services.size(); i++) {
				JCheckBox cb = new JCheckBox();

				AnalysisService svc = (AnalysisService) services.get(i);

				try {
					LSID lsid = new LSID((String) svc.getTaskInfo()
							.getTaskInfoAttributes().get(GPConstants.LSID));

					List versions = (List) lsid2VersionsMap.get(lsid
							.toStringNoVersion());

					categoryPanel.add(cb, cc.xy(1, i + 2));

					JComboBox versionsComboBox = null;
					JPanel temp = new JPanel(new FlowLayout(FlowLayout.LEFT));
					if (versions != null && versions.size() > 1) {
						versionsComboBox = new JComboBox();
						for (int j = 0; j < versions.size(); j++) {
							String moduleLsid = lsid.toStringNoVersion() + ":"
									+ versions.get(j);
							versionsComboBox.addItem(versions.get(j));
							if (moduleLsids.contains(moduleLsid)) {
								cb.setSelected(true);
								versionsComboBox.setSelectedItem(moduleLsid);
							}
						}
						if (versionsComboBox.getSelectedItem() == null) {
							versionsComboBox.setSelectedItem(lsid.getVersion());
						}
						JLabel taskNameLabel = new JLabel(svc.getName());
						temp.add(taskNameLabel);
						temp.add(versionsComboBox);

					} else {
						JLabel taskNameLabel = new JLabel(svc.getName() + " ("
								+ lsid.getVersion() + ")");
						temp.add(taskNameLabel);
						if (moduleLsids.contains(lsid.toString())) {
							cb.setSelected(true);
						}
					}
					categoryPanel.add(temp, cc.xy(2, i + 2));
					checkBoxes
							.add(new TaskSelector(cb, lsid, versionsComboBox));

				} catch (MalformedURLException e) {
					e.printStackTrace();
				}

			}
			if (row == (categoryIndex + 1)) {
				panel = rightTasksPanel;
				row = 1;
			}
			panel.add(categoryPanel, cc.xy(1, row++));
		}

		JPanel tasksPanel = new JPanel(new FormLayout("pref, 10dlu, pref",
				"pref"));
		tasksPanel.add(leftTasksPanel, cc.xy(1, 1, "left, top"));
		tasksPanel.add(rightTasksPanel, cc.xy(3, 1, "left, top"));

		headerPanel = new HeaderPanel(suiteInfo);

		JPanel bottomBtnPanel = new JPanel();
		final JButton saveButton = new JButton("Save");
		// helpButton = new JButton("Help");
		final JButton exportButton = new JButton("Export");
		bottomBtnPanel.add(saveButton);
		bottomBtnPanel.add(exportButton);

		ActionListener btnListener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == exportButton) {
					new CLThread() {
						public void run() {

							try {
								AnalysisServiceManager asm = AnalysisServiceManager
										.getInstance();
								final File destination = GUIUtil
										.showSaveDialog(new File(suiteInfo
												.getName()
												+ ".zip"),
												"Select destination zip file");
								if (destination == null) {
									return;
								}
								new TaskIntegratorProxy(asm.getServer(), asm
										.getUsername()).exportSuiteToZip(
										suiteInfo.getLsid(), destination);

							} catch (WebServiceException e1) {
								e1.printStackTrace();
								GenePattern
										.showErrorDialog("An error occurred while exporting the suite "
												+ suiteInfo.getName());
							}
						}
					}.start();

				} else if (e.getSource() == saveButton) {
					int accessId = headerPanel.privacyComboBox
							.getSelectedItem().equals("Public") ? GPConstants.ACCESS_PUBLIC
							: GPConstants.ACCESS_PRIVATE;
					suiteInfo.setAccessId(accessId);
					suiteInfo.setName(headerPanel.nameField.getText());
					if (suiteInfo.getName().trim().equals("")) {
						GenePattern
								.showErrorDialog("Please supply a suite name.");
						return;
					}
					suiteInfo.setAuthor(headerPanel.authorField.getText());
					if (suiteInfo.getAuthor().trim().equals("")) {
						GenePattern.showErrorDialog("Please supply an author.");
						return;
					}
					suiteInfo.setOwner(headerPanel.ownerField.getText());
					if (suiteInfo.getOwner().trim().equals("")) {
						GenePattern.showErrorDialog("Please supply an owner.");
						return;
					}

					suiteInfo.setDescription(headerPanel.descriptionField
							.getText());
					JComboBox docComboBox = headerPanel.docComboBox;
					final List localDocFiles = new ArrayList();
					final List serverDocFiles = new ArrayList();

					for (int i = 0; i < docComboBox.getItemCount(); i++) {
						Object obj = docComboBox.getItemAt(i);
						if (obj instanceof String) {
							String s = (String) obj;
							String url = AnalysisServiceManager.getInstance()
									.getServer()
									+ "/gp/getFile.jsp?task="
									+ suiteInfo.getLSID() + "&file=" + s;
							serverDocFiles.add(s);
						} else {
							LocalFileWrapper fw = (LocalFileWrapper) obj;
							localDocFiles.add(fw.file);
						}
					}

					suiteInfo.setDocumentationFiles((String[]) serverDocFiles
							.toArray(new String[0]));
					List moduleLsids = new ArrayList();
					for (int i = 0; i < checkBoxes.size(); i++) {
						TaskSelector ts = (TaskSelector) checkBoxes.get(i);
						if (ts.cb.isSelected()) {
							if (ts.versionsComboBox != null) {
								moduleLsids
										.add(ts.lsid.toStringNoVersion()
												+ ":"
												+ ts.versionsComboBox
														.getSelectedItem());
							} else {
								moduleLsids.add(ts.lsid.toString());
							}
						}
					}
					suiteInfo.setModuleLSIDs((String[]) moduleLsids
							.toArray(new String[0]));

					final AnalysisServiceManager asm = AnalysisServiceManager
							.getInstance();
					new CLThread() {
						public void run() {
							try {

								String lsid = new TaskIntegratorProxy(asm
										.getServer(), asm.getUsername())
										.modifySuite(suiteInfo,
												(File[]) localDocFiles
														.toArray(new File[0]),
												(String[]) serverDocFiles
														.toArray(new String[0]));
								suiteInfo.setLsid(lsid);
								headerPanel.lsidField.setText(lsid);
								GenePattern.showMessageDialog("Saved "
										+ suiteInfo.getName());
								try {
									MessageManager
											.notifyListeners(new SuiteInstallMessage(
													SuiteEditor.this, lsid));
								} catch (MalformedURLException e) {
									e.printStackTrace();
								}
								MessageManager
										.notifyListeners(new ChangeViewMessageRequest(
												SuiteEditor.this,
												ChangeViewMessageRequest.SHOW_EDIT_SUITE_REQUEST,
												new AdminProxy(asm.getServer(),
														asm.getUsername())
														.getSuite(lsid)));
							} catch (WebServiceException e1) {
								e1.printStackTrace();
								GenePattern
										.showErrorDialog("An error occurred while saving the suite "
												+ suiteInfo.getName());
							}
						}
					}.start();
				}
			}
		};
		saveButton.addActionListener(btnListener);
		exportButton.addActionListener(btnListener);

		setLayout(new BorderLayout());
		JScrollPane sp = new JScrollPane(tasksPanel);
		Border b = sp.getBorder();
		sp.setBorder(GUIUtil.createBorder(b, 0, -1, -1, -1));

		add(sp, BorderLayout.CENTER);

		add(headerPanel, BorderLayout.NORTH);

		add(bottomBtnPanel, BorderLayout.SOUTH);
		invalidate();
		validate();
	}

	static class HeaderPanel extends JPanel {

		private JTextField nameField;

		private JTextField descriptionField;

		private JTextField authorField;

		private JTextField ownerField;

		private JComboBox privacyComboBox;

		private JComboBox docComboBox;

		private JLabel lsidField;

		public HeaderPanel(final SuiteInfo suiteInfo) {
			boolean view = false;
			setLayout(new BorderLayout());

			String name = suiteInfo.getName();
			JLabel nameLabel = new JLabel("Name:");
			nameField = view ? GUIUtil.createLabelLikeTextField(name, 40)
					: new JTextField(name, 40);

			JLabel descriptionLabel = new JLabel("Description:");
			descriptionField = view ? GUIUtil.createLabelLikeTextField(
					suiteInfo.getDescription(), 40) : new JTextField(suiteInfo
					.getDescription(), 40);
			JComboBox versionComboBox = null;
			if (name != null && !name.equals("")) {
				versionComboBox = new VersionComboBox(suiteInfo.getLSID(),
						ChangeViewMessageRequest.SHOW_EDIT_SUITE_REQUEST, true);
			}
			CellConstraints cc = new CellConstraints();
			JPanel temp = new JPanel(new FormLayout(
					"right:pref:none, 3dlu, pref, pref",
					"pref, 3dlu, pref, pref"));

			temp.add(nameLabel, cc.xy(1, 1));
			temp.add(nameField, cc.xy(3, 1));

			if (versionComboBox != null) {
				temp.add(versionComboBox, cc.xy(4, 1));
			}

			temp.add(descriptionLabel, cc.xy(1, 3));
			temp.add(descriptionField, cc.xy(3, 3));

			StringBuffer rowSpec = new StringBuffer();
			for (int i = 0; i < 6; i++) {
				if (i > 0) {
					rowSpec.append(", ");
				}
				rowSpec.append("pref, ");
				rowSpec.append("3dlu");
			}
			JPanel detailsPanel = new JPanel(new FormLayout(
					"right:pref:none, 3dlu, left:pref", rowSpec.toString()));
			// detailsPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			JLabel authorLabel = new JLabel("Author:");
			authorField = view ? GUIUtil.createLabelLikeTextField(suiteInfo
					.getAuthor(), 40) : new JTextField(suiteInfo.getAuthor(),
					40);
			detailsPanel.add(authorLabel, cc.xy(1, 1));
			detailsPanel.add(authorField, cc.xy(3, 1));

			JLabel ownerLabel = new JLabel("Owner:");
			ownerField = view ? GUIUtil.createLabelLikeTextField(suiteInfo
					.getOwner(), 40) : new JTextField(suiteInfo.getOwner(), 40);
			detailsPanel.add(ownerLabel, cc.xy(1, 3));
			detailsPanel.add(ownerField, cc.xy(3, 3));

			JLabel privacyLabel = new JLabel("Privacy:");

			privacyComboBox = new JComboBox(
					new String[] { "Public", "Private" });
			if (suiteInfo.getAccessId() == GPConstants.ACCESS_PUBLIC) {
				privacyComboBox.setSelectedIndex(0);
			} else {
				privacyComboBox.setSelectedIndex(1);
			}

			detailsPanel.add(privacyLabel, cc.xy(1, 5));
			if (!view) {
				detailsPanel.add(privacyComboBox, cc.xy(3, 5));
			} else {
				String privacyString = suiteInfo.getAccessId() == GPConstants.ACCESS_PRIVATE ? "Private"
						: "Public";
				detailsPanel.add(GUIUtil
						.createLabelLikeTextField(privacyString), cc.xy(3, 5));
			}

			JLabel documentationLabel = new JLabel("Documentation:");
			docComboBox = new JComboBox();
			if (name != null) {
				String[] docFiles = suiteInfo.getDocumentationFiles();
				for (int i = 0; i < docFiles.length; i++) {
					docComboBox.addItem(docFiles[i]);
				}
			}

			JButton deleteDocBtn = new JButton("Delete");
			deleteDocBtn.setVisible(!view);
			deleteDocBtn.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					if (docComboBox.getSelectedItem() == null) {
						return;
					}
					if (GUIUtil
							.showConfirmDialog("Are you sure you want to delete "
									+ docComboBox.getSelectedItem() + "?")) {
						docComboBox
								.removeItemAt(docComboBox.getSelectedIndex());
					}
				}
			});

			JButton addDocBtn = new JButton("Add...");
			addDocBtn.setVisible(!view);
			addDocBtn.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					File f = GUIUtil.showOpenDialog();
					if (f != null) {
						docComboBox.addItem(new LocalFileWrapper(f));
					}
				}
			});

			JButton viewBtn = new JButton("View");
			viewBtn.setVisible(view && docComboBox.getItemCount() > 0);
			viewBtn.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					String s = (String) docComboBox.getSelectedItem();
					String url = AnalysisServiceManager.getInstance()
							.getServer()
							+ "/gp/getFile.jsp?task="
							+ suiteInfo.getLSID()
							+ "&file=" + s;

					try {
						BrowserLauncher.openURL(url);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			});

			detailsPanel.add(documentationLabel, cc.xy(1, 9));
			JPanel docPanel = new JPanel(new FormLayout(
					"pref, 3dlu, pref, 3dlu, pref, 3dlu, pref", "pref"));
			docPanel.add(docComboBox, cc.xy(1, 1));
			docPanel.add(deleteDocBtn, cc.xy(3, 1));
			docPanel.add(addDocBtn, cc.xy(5, 1));
			docPanel.add(viewBtn, cc.xy(7, 1));

			detailsPanel.add(docPanel, cc.xy(3, 9));

			if (name != null) {
				JLabel lsidLabel = new JLabel("LSID:");
				lsidField = new JLabel(suiteInfo.getLSID());
				detailsPanel.add(lsidLabel, cc.xy(1, 11));
				detailsPanel.add(lsidField, cc.xy(3, 11));
			}
			TogglePanel detailsToggle = new TogglePanel("Details", detailsPanel);

			JPanel bottom = new JPanel(new BorderLayout());
			bottom.add(detailsToggle, BorderLayout.NORTH);
			// bottom.add(buttonPanel, BorderLayout.SOUTH);
			add(temp, BorderLayout.CENTER);
			add(bottom, BorderLayout.SOUTH);
		}
	}

	static class LocalFileWrapper {
		File file;

		public LocalFileWrapper(File f) {
			file = f;
		}

		public String toString() {
			return file.getName();
		}
	}

}
