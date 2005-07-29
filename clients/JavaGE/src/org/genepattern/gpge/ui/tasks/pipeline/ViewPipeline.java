package org.genepattern.gpge.ui.tasks.pipeline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.message.ChangeViewMessageRequest;
import org.genepattern.gpge.message.MessageManager;
import org.genepattern.gpge.ui.maindisplay.GroupPanel;
import org.genepattern.gpge.ui.table.AlternatingColorTable;
import org.genepattern.gpge.ui.tasks.AnalysisServiceDisplay;
import org.genepattern.gpge.ui.tasks.AnalysisServiceManager;
import org.genepattern.gpge.ui.tasks.TaskHelpActionListener;
import org.genepattern.gpge.ui.tasks.TaskNamePanel;
import org.genepattern.gpge.ui.util.GUIUtil;
import org.genepattern.gpge.util.PostData;
import org.genepattern.util.BrowserLauncher;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.AnalysisService;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

/**
 * A panel for viewing a pipeline
 * 
 * @author Joshua Gould
 * 
 */
// inherited task numbers start at 0, output files start at one,, names for
// params start at one
public class ViewPipeline extends JPanel {

	private PipelineEditorModel model;

	private List togglePanelList;

	private AlternatingRowColorPanel tasksPanel;

	private FormLayout tasksLayout;

	private TaskNamePanel taskNamePanel;

	/**
	 * Currently only one instance should be created by the ViewManager
	 * 
	 */
	public ViewPipeline() {
		setBackground(Color.white);
		togglePanelList = new ArrayList();
	}

	void clear() {
		togglePanelList.clear();
		removeAll();
	}

	private void showMissingTasks(AnalysisService svc, final List missingTasks) {

		final String[] columnNames = { "Name", "Version", "LSID" };
		TableModel tableModel = new AbstractTableModel() {

			public int getRowCount() {
				return missingTasks.size();
			}

			public int getColumnCount() {
				return columnNames.length;
			}

			public String getColumnName(int j) {
				return columnNames[j];
			}

			public Object getValueAt(int row, int col) {
				JobSubmission js = (JobSubmission) missingTasks.get(row);
				if (col == 0) {
					return js.getName();
				} else if (col == 1) {
					try {
						return new LSID(js.getLSID()).getVersion();
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
					return null;
				} else {
					return js.getLSID();
				}
			}
		};

		JTable table = new AlternatingColorTable(tableModel);

		JPanel buttonPanel = new JPanel();
		JButton installFromCatalogBtn = new JButton(
				"Install Missing Modules From Catalog");
		JButton importZipBtn = new JButton("Import Module From Zip File");
		importZipBtn.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {

			}

		});
		buttonPanel.add(installFromCatalogBtn);
		// buttonPanel.add(importZipBtn);
		installFromCatalogBtn.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				PostData postData = new PostData(AnalysisServiceManager
						.getInstance().getServer()
						+ "/gp/taskCatalog.jsp");
				try {
					for (int i = 0; i < missingTasks.size(); i++) {
						JobSubmission js = (JobSubmission) missingTasks.get(i);
						postData.addPostData("LSID", js.getLSID());
					}
					BrowserLauncher.openURL(postData.toString());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}

		});

		JScrollPane sp = new JScrollPane(table);

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sp,
				new JScrollPane(tasksPanel));
		// table.getColumnModel().getColumn(1).setPreferredWidth(10);
		JTextArea errorLabel = GUIUtil
				.createWrappedLabel("The following modules do not exist on the server. Please install the missing modules from the Module Repository or import them from a zip file (File > Import Module).");
		errorLabel.setBackground(Color.red);
		Border b = sp.getBorder();
		sp.setBorder(GUIUtil.createBorder(b, 0, -1, -1, -1));
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(errorLabel, BorderLayout.NORTH);
		topPanel.add(buttonPanel, BorderLayout.CENTER);
		taskNamePanel = new TaskNamePanel(svc.getTaskInfo(),
				ChangeViewMessageRequest.SHOW_VIEW_PIPELINE_REQUEST, topPanel);

		add(taskNamePanel, BorderLayout.NORTH);
		add(splitPane, BorderLayout.CENTER);

	}

	public void display(AnalysisService svc) {
		clear();
		try {
			PipelineModel pipelineModel = PipelineModel
					.toPipelineModel((String) svc.getTaskInfo()
							.getTaskInfoAttributes().get(
									GPConstants.SERIALIZED_MODEL));
			setPipeline(svc, pipelineModel);

		} catch (Exception e1) {
			e1.printStackTrace();
			GenePattern
					.showErrorDialog("An error occurred while loading the pipeline");
			return;
		}
	}

	private void setPipeline(final AnalysisService svc,
			PipelineModel pipelineModel) {
		setMinimumSize(new java.awt.Dimension(100, 100));
		setLayout(new BorderLayout());

		// show edit link when task has local authority and either belongs to
		// current user or is public
		try {
			model = new PipelineEditorModel(svc, pipelineModel);

			tasksLayout = new FormLayout("right:pref, 3dlu, default:grow", "");
			tasksPanel = new AlternatingRowColorPanel(tasksLayout);
			tasksPanel.setBackground(getBackground());
			for (int i = 0; i < model.getTaskCount(); i++) {
				layoutTask(i);
			}

			JScrollPane sp = new JScrollPane(tasksPanel);
			Border b = sp.getBorder();
			sp.setBorder(GUIUtil.createBorder(b, 0, -1, -1, -1));
			add(sp, BorderLayout.CENTER);
			final JButton expandAllButton = new JButton("Expand All");
			final JButton collapseAllButton = new JButton("Collapse All");
			JPanel expandPanel = new JPanel();
			expandPanel.add(collapseAllButton);
			expandPanel.add(expandAllButton);

			ActionListener expandCollapseListener = new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					if (e.getSource() == expandAllButton) {
						for (int i = 0; i < togglePanelList.size(); i++) {
							GroupPanel p = (GroupPanel) togglePanelList.get(i);
							p.setExpanded(true);
						}
					} else if (e.getSource() == collapseAllButton) {
						for (int i = 0; i < togglePanelList.size(); i++) {
							GroupPanel p = (GroupPanel) togglePanelList.get(i);
							p.setExpanded(false);
						}
					}
				}

			};

			expandAllButton.addActionListener(expandCollapseListener);
			collapseAllButton.addActionListener(expandCollapseListener);

			taskNamePanel = new TaskNamePanel(svc.getTaskInfo(),
					ChangeViewMessageRequest.SHOW_VIEW_PIPELINE_REQUEST,
					expandPanel);

			add(taskNamePanel, BorderLayout.NORTH);

			JPanel bottomPanel = new JPanel();
			final JButton runButton = new JButton("Run");
			final JButton editButton = new JButton("Edit");
			final JButton helpButton = new JButton("Help");
			TaskHelpActionListener taskHelpActionListener = new TaskHelpActionListener();
			taskHelpActionListener.setTaskInfo(svc.getTaskInfo());
			helpButton.addActionListener(taskHelpActionListener);

			ActionListener btnListener = new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					if (e.getSource() == runButton) {
						MessageManager
								.notifyListeners(new ChangeViewMessageRequest(
										this,
										ChangeViewMessageRequest.SHOW_RUN_TASK_REQUEST,
										svc));
					} else if (e.getSource() == editButton) {
						MessageManager
								.notifyListeners(new ChangeViewMessageRequest(
										this,
										ChangeViewMessageRequest.SHOW_EDIT_PIPELINE_REQUEST,
										svc));
					}
				}

			};
			runButton.addActionListener(btnListener);
			editButton.addActionListener(btnListener);
			bottomPanel.add(runButton);
			bottomPanel.add(editButton);
			bottomPanel.add(helpButton);
			add(bottomPanel, BorderLayout.SOUTH);
			invalidate();
			validate();
		} catch (JobSubmissionsNotFoundException e) {
			e.printStackTrace();
			showMissingTasks(svc, e.getjobSubmissions());

		}
	}

	private void layoutTask(final int taskIndex) {

		GroupPanel togglePanel = new GroupPanel((taskIndex + 1) + ". "
				+ model.getTaskName(taskIndex), model
				.getTaskDescription(taskIndex));
		togglePanelList.add(togglePanel);
		CellConstraints cc = new CellConstraints();

		tasksLayout.appendRow(new RowSpec("pref"));

		tasksPanel.setOpaque(false);
		tasksPanel.add(togglePanel, cc.xywh(1, tasksLayout.getRowCount(), 2, 1,
				CellConstraints.LEFT, CellConstraints.BOTTOM));
		int taskStart = tasksLayout.getRowCount();

		togglePanel.setBackground(getBackground());
		addTaskParameters(taskIndex, togglePanel);
		int taskEnd = tasksLayout.getRowCount();
		int parameterCount = taskEnd - taskStart;
		togglePanel.setExpanded(true);
		tasksPanel.addTask(togglePanel, parameterCount);

	}

	private void addTaskParameters(final int taskIndex, GroupPanel togglePanel) {
		for (int i = 0; i < model.getParameterCount(taskIndex); i++) {

			CellConstraints cc = new CellConstraints();
			JLabel field = new JLabel(model.getValue(taskIndex, i));
			JLabel label = new JLabel(AnalysisServiceDisplay
					.getDisplayString(model.getParameterName(taskIndex, i))
					+ ":");
			if (model.getInheritedTaskIndex(taskIndex, i) != -1) {
				final int parameterIndex = i;
				String value = "<html>Use <b>"
						+ model.getInheritedFile(taskIndex, i)
						+ "</b> from <b>"
						+ (1 + model.getInheritedTaskIndex(taskIndex, i))
						+ ". " + model.getTaskName(taskIndex) + "</b>";
				field.setText(value);
				field.addMouseListener(new MouseAdapter() {

					public void mouseEntered(MouseEvent e) {
						GroupPanel p = (GroupPanel) togglePanelList.get(model
								.getInheritedTaskIndex(taskIndex,
										parameterIndex));
						p.setMajorLabelForeground(Color.red);
					}

					public void mouseExited(MouseEvent e) {
						GroupPanel p = (GroupPanel) togglePanelList.get(model
								.getInheritedTaskIndex(taskIndex,
										parameterIndex));
						p.setMajorLabelForeground(Color.black);
					}

				});

			}

			tasksLayout.appendRow(new RowSpec("pref"));
			tasksPanel.add(label, cc.xy(1, tasksLayout.getRowCount(),
					CellConstraints.RIGHT, CellConstraints.BOTTOM));
			tasksPanel.add(field, cc.xy(3, tasksLayout.getRowCount(),
					CellConstraints.LEFT, CellConstraints.BOTTOM));

			togglePanel.addToggleComponent(field);
			togglePanel.addToggleComponent(label);

		}
	}
}
