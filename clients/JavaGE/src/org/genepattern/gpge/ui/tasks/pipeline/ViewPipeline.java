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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

/**
 * A panel for viewing a pipeline
 * @author Joshua Gould
 *
 */
// inherited task numbers start at 0, output files start at one,, names for params start at one
public class ViewPipeline extends JPanel {
	private PipelineModel pipelineModel;

	private List jobSubmissions;

	private String userID;

	private TaskInfo pipelineTaskInfo;

	private AlternatingRowColorPanel tasksPanel;

	private FormLayout tasksLayout;

	private ArrayList togglePanelList = new ArrayList();

	private AnalysisService analysisService;

	private TaskNamePanel taskNamePanel;

	/**
	 * Currently only one instance should be created by the ViewManager
	 *
	 */
	public ViewPipeline() {
		setBackground(Color.white);
	}

	void clear() {
		togglePanelList.clear();
		removeAll();
	}

	public void display(AnalysisService svc) {
		clear();
		TaskInfo info = svc.getTaskInfo();
		this.analysisService = svc;
		try {
			PipelineModel pipelineModel = PipelineModel
					.toPipelineModel((String) info.getTaskInfoAttributes().get(
							GPConstants.SERIALIZED_MODEL));
			setPipeline(info, pipelineModel);

		} catch (Exception e1) {
			e1.printStackTrace();
			GenePattern
					.showErrorDialog("An error occurred while loading the pipeline");
			return;
		}
	}

	protected List getMissingTasks(List jobSubmissions) {
		List missingTasks = new ArrayList();
		for (int i = 0; i < jobSubmissions.size(); i++) {
			JobSubmission js = (JobSubmission) jobSubmissions.get(i);
			if (getTaskInfo(js.getLSID()) == null) {
				missingTasks.add(js);
			}
		}
		return missingTasks;
	}

	private void setPipeline(TaskInfo _pipelineTaskInfo, PipelineModel model) {
		this.pipelineTaskInfo = _pipelineTaskInfo;
		this.pipelineModel = model;
		this.jobSubmissions = pipelineModel.getTasks();
		this.userID = pipelineTaskInfo.getUserId();

		String displayName = pipelineModel.getName();
		if (displayName.endsWith(".pipeline")) {
			displayName = displayName.substring(0, displayName.length()
					- ".pipeline".length());
		}

		setMinimumSize(new java.awt.Dimension(100, 100));
		setLayout(new BorderLayout());

		// show edit link when task has local authority and either belongs to
		// current user or is public

		List tasks = pipelineModel.getTasks();
		final List missingTasks = getMissingTasks(tasks);
		tasksLayout = new FormLayout("right:pref, 3dlu, default:grow", "");
		tasksPanel = new AlternatingRowColorPanel(tasksLayout);
		tasksPanel.setBackground(getBackground());
		for (int i = 0; i < tasks.size(); i++) {
			layoutTask(i, (JobSubmission) tasks.get(i));
		}

		if (missingTasks.size() > 0) {

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
			//		buttonPanel.add(importZipBtn);
			installFromCatalogBtn.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent arg0) {
					PostData postData = new PostData(AnalysisServiceManager
							.getInstance().getServer()
							+ "/gp/taskCatalog.jsp");
					try {
						for (int i = 0; i < missingTasks.size(); i++) {
							JobSubmission js = (JobSubmission) missingTasks
									.get(i);
							postData.addPostData("LSID", js.getLSID());
						}
						BrowserLauncher.openURL(postData.toString());
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}

			});

			JScrollPane sp = new JScrollPane(table);

			JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
					sp, new JScrollPane(tasksPanel));
			//table.getColumnModel().getColumn(1).setPreferredWidth(10);
			JTextArea errorLabel = GUIUtil
					.createWrappedLabel("The following modules do not exist on the server. Please install the missing modules from the Module Repository or import them from a zip file (File > Import Module).");
			errorLabel.setBackground(Color.red);
			Border b = sp.getBorder();
			sp.setBorder(GUIUtil.createBorder(b, 0, -1, -1, -1));
			JPanel topPanel = new JPanel(new BorderLayout());
			topPanel.add(errorLabel, BorderLayout.NORTH);
			topPanel.add(buttonPanel, BorderLayout.CENTER);
			taskNamePanel = new TaskNamePanel(pipelineTaskInfo,
					ChangeViewMessageRequest.SHOW_VIEW_PIPELINE_REQUEST,
					topPanel);

			add(taskNamePanel, BorderLayout.NORTH);
			add(splitPane, BorderLayout.CENTER);

			return;
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

		taskNamePanel = new TaskNamePanel(pipelineTaskInfo,
				ChangeViewMessageRequest.SHOW_VIEW_PIPELINE_REQUEST,
				expandPanel);

		add(taskNamePanel, BorderLayout.NORTH);

		JPanel bottomPanel = new JPanel();
		final JButton runButton = new JButton("Run");
		final JButton editButton = new JButton("Edit");
		final JButton helpButton = new JButton("Help");
		helpButton
				.addActionListener(new TaskHelpActionListener(analysisService));

		ActionListener btnListener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == runButton) {
					MessageManager
							.notifyListeners(new ChangeViewMessageRequest(
									this,
									ChangeViewMessageRequest.SHOW_RUN_TASK_REQUEST,
									analysisService));
				} else if (e.getSource() == editButton) {
					MessageManager
							.notifyListeners(new ChangeViewMessageRequest(
									this,
									ChangeViewMessageRequest.SHOW_EDIT_PIPELINE_REQUEST,
									analysisService));
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

	}

	private void layoutTask(final int index, JobSubmission js) {

		TaskInfo formalTaskInfo = getTaskInfo(js.getLSID());
		ParameterInfo[] formalParams = formalTaskInfo != null ? formalTaskInfo
				.getParameterInfoArray() : null;
		if (formalParams == null) {
			formalParams = new ParameterInfo[0];
		}
		GroupPanel togglePanel = new GroupPanel((index + 1) + ". "
				+ js.getName(), js.getDescription());
		togglePanelList.add(togglePanel);
		CellConstraints cc = new CellConstraints();

		tasksLayout.appendRow(new RowSpec("pref"));

		tasksPanel.add(togglePanel, cc.xywh(1, tasksLayout.getRowCount(), 2, 1,
				CellConstraints.LEFT, CellConstraints.BOTTOM));
		togglePanel.setBackground(getBackground());
		addTaskComponents(js, formalParams, togglePanel);
		togglePanel.setExpanded(true);

		tasksPanel.addTask(togglePanel);

	}

	private TaskInfo getTaskInfo(String lsid) {
		AnalysisService svc = AnalysisServiceManager.getInstance()
				.getAnalysisService(lsid);
		if (svc == null) {
			return null;
		}
		return svc.getTaskInfo();
	}

	private void addTaskComponents(JobSubmission js,
			ParameterInfo[] formalParams, GroupPanel togglePanel) {

		ParameterInfo[] actualParameters = js.giveParameterInfoArray();

		boolean[] runtimePrompt = js.getRuntimePrompt();

		Map paramName2ActualParamIndexMap = new HashMap();
		for (int j = 0; j < actualParameters.length; j++) {
			paramName2ActualParamIndexMap.put(actualParameters[j].getName(),
					new Integer(j));
		}

		for (int j = 0; j < formalParams.length; j++) {
			int inheritedTaskNumber = -1;
			String paramName = formalParams[j].getName();

			ParameterInfo formalParam = formalParams[j];
			Integer index = (Integer) paramName2ActualParamIndexMap
					.get(paramName);
			if (index == null) {
				continue;
			}
			ParameterInfo actualParam = actualParameters[index.intValue()];

			String value = null;
			if (formalParam.isInputFile()) {
				java.util.Map pipelineAttributes = actualParam.getAttributes();

				String taskNumberString = null;
				if (pipelineAttributes != null) {
					taskNumberString = (String) pipelineAttributes
							.get(PipelineModel.INHERIT_TASKNAME);
				}
				int k = index.intValue();
				if ((k < runtimePrompt.length) && (runtimePrompt[k])) {
					value = "Prompt when run";
				} else if (taskNumberString != null) {
					inheritedTaskNumber = Integer.parseInt(taskNumberString
							.trim());
					String outputFileNumber = (String) pipelineAttributes
							.get(PipelineModel.INHERIT_FILENAME);

					String inheritedOutputFileName = outputFileNumber;
					if (outputFileNumber.equals("1")) {
						inheritedOutputFileName = "1st output";
					} else if (outputFileNumber.equals("2")) {
						inheritedOutputFileName = "2nd output";
					} else if (outputFileNumber.equals("3")) {
						inheritedOutputFileName = "3rd output";
					} else if (outputFileNumber.equals("stdout")) {
						inheritedOutputFileName = "standard output";
					} else if (outputFileNumber.equals("stderr")) {
						inheritedOutputFileName = "standard error";
					}
					JobSubmission inheritedTask = (JobSubmission) jobSubmissions
							.get(inheritedTaskNumber);
					int displayTaskNumber = inheritedTaskNumber + 1;

					value = "<html>Use <b>" + inheritedOutputFileName
							+ "</b> from <b>" + displayTaskNumber + ". "
							+ inheritedTask.getName() + "</b></html>";

				} else {
					value = actualParam.getValue();
					if (value
							.startsWith("<GenePatternURL>getFile.jsp?task=<LSID>&file=")) {
						value = value.substring(
								"<GenePatternURL>getFile.jsp?task=<LSID>&file="
										.length(), value.length());
					}
				}

			} else {
				value = actualParam.getValue(); // can be command line value instead of UI value  
				String[] choices = formalParam.getValue().split(
						GPConstants.PARAM_INFO_CHOICE_DELIMITER);
				String[] eachValue;
				value = actualParam.getValue();
				for (int v = 0; v < choices.length; v++) {
					eachValue = choices[v]
							.split(GPConstants.PARAM_INFO_TYPE_SEPARATOR);
					if (value.equals(eachValue[0])) {
						if (eachValue.length == 2) {
							value = eachValue[1];
						}
						break;
					}
				}
			}

			CellConstraints cc = new CellConstraints();
			JLabel field = new JLabel(value);
			JLabel label = new JLabel(AnalysisServiceDisplay
					.getDisplayString(paramName)
					+ ":");
			if (inheritedTaskNumber != -1) {
				final int inheritedTaskIndex = inheritedTaskNumber;
				field.addMouseListener(new MouseAdapter() {

					public void mouseEntered(MouseEvent e) {
						GroupPanel p = (GroupPanel) togglePanelList
								.get(inheritedTaskIndex);
						p.setMajorLabelForeground(Color.red);
					}

					public void mouseExited(MouseEvent e) {
						GroupPanel p = (GroupPanel) togglePanelList
								.get(inheritedTaskIndex);
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
