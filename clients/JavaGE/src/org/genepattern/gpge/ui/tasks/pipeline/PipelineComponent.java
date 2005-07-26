package org.genepattern.gpge.ui.tasks.pipeline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.message.ChangeViewMessageRequest;
import org.genepattern.gpge.message.MessageManager;
import org.genepattern.gpge.ui.maindisplay.GroupPanel;
import org.genepattern.gpge.ui.tasks.AnalysisServiceDisplay;
import org.genepattern.gpge.ui.tasks.AnalysisServiceManager;
import org.genepattern.gpge.ui.tasks.TaskDisplay;
import org.genepattern.gpge.ui.tasks.TaskHelpActionListener;
import org.genepattern.gpge.ui.tasks.TaskNamePanel;
import org.genepattern.gpge.ui.util.GUIUtil;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskIntegratorProxy;
import org.genepattern.webservice.WebServiceException;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

// inherited task numbers start at 0, output files start at one,, names for params start at one
public class PipelineComponent extends JPanel implements TaskDisplay {
	private PipelineModel pipelineModel;

	private List jobSubmissions;

	private String userID;

	private TaskInfo pipelineTaskInfo;

	private JPanel tasksPanel;

	private FormLayout tasksLayout;

	private ArrayList togglePanelList = new ArrayList();

	private PipelineEditor editor;

	private AnalysisService analysisService;

	private JPanel buttonPanel;

	private TaskNamePanel taskNamePanel;

	private Map parameterName2ComponentMap = new HashMap();

	private List inputFileParameters = new ArrayList();

	private JComboBox tasksInPipelineComboBox;

	/**
	 * Currently only one instance should be created by the ViewManager
	 * 
	 */
	public PipelineComponent() {
		setBackground(Color.white);
		setMinimumSize(new java.awt.Dimension(100, 100));
		setLayout(new BorderLayout());
		tasksLayout = new FormLayout("right:pref, 3dlu, default:grow", "");

		tasksPanel = new JPanel(tasksLayout);
		tasksPanel.setBackground(getBackground());
		JScrollPane sp = new JScrollPane(tasksPanel);
		Border b = sp.getBorder();
		sp.setBorder(GUIUtil.createBorder(b, 0, -1, -1, -1));
		add(sp, BorderLayout.CENTER);

		tasksInPipelineComboBox = new JComboBox();

		final JButton addButton = new JButton("Add Task After");

		final JButton addBeforeButton = new JButton("Add Task Before");

		final JButton deleteButton = new JButton("Delete");

		final JButton moveUpButton = new JButton("Move Up");

		final JButton moveDownButton = new JButton("Move Down");

		final JButton expandAllButton = new JButton("Expand All");
		final JButton collapseAllButton = new JButton("Collapse All");

		ActionListener taskBtnListener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				int index = tasksInPipelineComboBox.getSelectedIndex();
				if (e.getSource() == addButton) {
					editor.addTask(index + 1, null); // FIXME
				} else if (e.getSource() == addBeforeButton) {
					editor.addTask(index - 1, null); // FIXME
				} else if (e.getSource() == deleteButton) {
					editor.delete(index);
				} else if (e.getSource() == moveUpButton) {
					editor.moveUp(index, index - 1);
				} else if (e.getSource() == moveDownButton) {
					editor.moveDown(index, index + 1);
				}
				setPipeline(editor.getTaskInfo(), editor.getPipelineModel());
			}
		};

		addButton.addActionListener(taskBtnListener);
		addBeforeButton.addActionListener(taskBtnListener);
		deleteButton.addActionListener(taskBtnListener);
		moveUpButton.addActionListener(taskBtnListener);
		moveDownButton.addActionListener(taskBtnListener);

		buttonPanel = new JPanel(new BorderLayout());
		JPanel topPanel = new JPanel();
		topPanel.add(tasksInPipelineComboBox);
		topPanel.add(addButton);
		topPanel.add(addBeforeButton);
		topPanel.add(deleteButton);
		topPanel.add(moveUpButton);
		topPanel.add(moveDownButton);
		JPanel bottomPanel = new JPanel();
		bottomPanel.add(collapseAllButton);
		bottomPanel.add(expandAllButton);
		buttonPanel.add(topPanel, BorderLayout.CENTER);
		buttonPanel.add(bottomPanel, BorderLayout.SOUTH);

		ActionListener expandListener = new ActionListener() {

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

		expandAllButton.addActionListener(expandListener);
		collapseAllButton.addActionListener(expandListener);

		JPanel bottomBtnPanel = new JPanel();
		final JButton saveButton = new JButton("Save");
		final JButton runButton = new JButton("Run");
		final JButton viewButton = new JButton("View");
		final JButton helpButton = new JButton("Help");
		helpButton
				.addActionListener(new TaskHelpActionListener(analysisService));

		ActionListener btnListener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == saveButton) {
					for (int i = 0; i < jobSubmissions.size(); i++) {
						JobSubmission js = (JobSubmission) jobSubmissions
								.get(i);
						TaskInfo task = getTaskInfo(js.getLSID());
						ParameterInfo[] formalParams = task
								.getParameterInfoArray();
						if (formalParams == null) {
							formalParams = new ParameterInfo[0];
						}
						for (int j = 0; j < formalParams.length; j++) {
							ParameterInfo p = formalParams[j];

						}

					}
					// FIXME update values in pipeline model, existing files not
					// copied over to new task
					pipelineTaskInfo.getTaskInfoAttributes()
							.put(PipelineModel.PIPELINE_MODEL,
									pipelineModel.toXML());

					try {
						new TaskIntegratorProxy(AnalysisServiceManager
								.getInstance().getServer(), userID, false)
								.modifyTask(GPConstants.ACCESS_PUBLIC,
										pipelineTaskInfo.getName(),
										pipelineTaskInfo.getDescription(),
										pipelineTaskInfo
												.getParameterInfoArray(),
										(HashMap) pipelineTaskInfo
												.getTaskInfoAttributes(),
										new File[] {});
					} catch (WebServiceException e1) {
						e1.printStackTrace();
						if (!GenePattern.disconnectedFromServer(e1,
								AnalysisServiceManager.getInstance()
										.getServer())) {
							GenePattern
									.showErrorDialog("An error occurred while saving the pipeline.");
						}
					}
				}
				if (e.getSource() == runButton) {
					MessageManager
							.notifyListeners(new ChangeViewMessageRequest(
									this,
									ChangeViewMessageRequest.SHOW_RUN_TASK_REQUEST,
									analysisService));
				} else if (e.getSource() == viewButton) {
					MessageManager
							.notifyListeners(new ChangeViewMessageRequest(
									this,
									ChangeViewMessageRequest.SHOW_VIEW_PIPELINE_REQUEST,
									analysisService));
				}
			}

		};
		saveButton.addActionListener(btnListener);
		runButton.addActionListener(btnListener);
		viewButton.addActionListener(btnListener);
		bottomBtnPanel.add(saveButton);
		bottomBtnPanel.add(runButton);
		bottomBtnPanel.add(viewButton);
		bottomBtnPanel.add(helpButton);
		add(bottomBtnPanel, BorderLayout.SOUTH);
	}

	void reset() {
		if (taskNamePanel != null) {
			remove(taskNamePanel);
		}
		togglePanelList.clear();
		tasksPanel.removeAll();
		parameterName2ComponentMap.clear();
		inputFileParameters.clear();
		tasksInPipelineComboBox.removeAllItems();
	}

	/**
	 * Gets an iterator of input file parameters
	 * 
	 * @return the input file names
	 */
	public Iterator getInputFileParameters() {
		return inputFileParameters.iterator();
	}

	/**
	 * Sets the value of the given parameter to the given node
	 * 
	 * @param parameterName
	 *            the parmeter name
	 * @param node
	 *            a tree node
	 */
	public void setInputFile(String parameterName,
			javax.swing.tree.TreeNode node) {
		JLabel label = (JLabel) parameterName2ComponentMap.get(parameterName);
		if (label != null) {
			label.setText(node.toString()); // FIXME
		}
	}

	public void display(AnalysisService svc) {
		this.analysisService = svc;
		TaskInfo info = svc.getTaskInfo();
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

	private void setPipeline(TaskInfo _pipelineTaskInfo, PipelineModel model) {
		reset();

		this.pipelineTaskInfo = _pipelineTaskInfo;
		this.pipelineModel = model;
		editor = new PipelineEditor(pipelineTaskInfo, pipelineModel);
		editor.print();
		this.jobSubmissions = pipelineModel.getTasks();
		this.userID = pipelineTaskInfo.getUserId();

		String displayName = pipelineModel.getName();
		if (displayName.endsWith(".pipeline")) {
			displayName = displayName.substring(0, displayName.length()
					- ".pipeline".length());
		}

		// show edit link when task has local authority and either belongs to
		// current user or is public

		List tasks = pipelineModel.getTasks();
		for (int i = 0; i < tasks.size(); i++) {
			JobSubmission js = (JobSubmission) tasks.get(i);
			layoutTask(i, js);
			tasksInPipelineComboBox.addItem((i + 1) + ". " + js.getName());
		}

		taskNamePanel = new TaskNamePanel(pipelineTaskInfo,
				ChangeViewMessageRequest.SHOW_EDIT_PIPELINE_REQUEST,
				buttonPanel);

		add(taskNamePanel, BorderLayout.NORTH);
		invalidate();
		validate();
		tasksPanel.invalidate();
		tasksPanel.validate();
	}

	private void layoutTask(final int index, JobSubmission js) {

		TaskInfo formalTaskInfo = getTaskInfo(js.getLSID());
		ParameterInfo[] formalParams = formalTaskInfo != null ? formalTaskInfo
				.getParameterInfoArray() : null;
		if (formalParams == null) {
			formalParams = new ParameterInfo[0];
		}

		GroupPanel togglePanel = new GroupPanel((index + 1) + ". "
				+ formalTaskInfo.getName(), new JTextField(formalTaskInfo
				.getDescription(), 60));
		final JPopupMenu popupMenu = new JPopupMenu();

		final JMenuItem addTaskAfterItem = new JMenuItem("Add Task After");
		popupMenu.add(addTaskAfterItem);
		final JMenuItem addBeforeItem = new JMenuItem("Add Task Before");
		popupMenu.add(addBeforeItem);
		final JMenuItem moveUpItem = new JMenuItem("Move Up");
		popupMenu.add(moveUpItem);
		final JMenuItem moveDownItem = new JMenuItem("Move Down");
		popupMenu.add(moveDownItem);
		final JMenuItem deleteItem = new JMenuItem("Delete");
		popupMenu.add(deleteItem);

		ActionListener listener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == addTaskAfterItem) {
					editor.addTask(index + 1, null);
				} else if (e.getSource() == addBeforeItem) {
					editor.addTask(index - 1, null);
				} else if (e.getSource() == moveUpItem) {
					editor.moveUp(index, index - 1);
				} else if (e.getSource() == moveDownItem) {
					editor.moveDown(index, index + 1);
				} else if (e.getSource() == deleteItem) {
					editor.delete(index);
				}

				setPipeline(editor.getTaskInfo(), editor.getPipelineModel());
			}

		};
		addTaskAfterItem.addActionListener(listener);
		addBeforeItem.addActionListener(listener);
		moveUpItem.addActionListener(listener);
		moveDownItem.addActionListener(listener);
		deleteItem.addActionListener(listener);

		togglePanel.getMajorLabel().addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				moveDownItem.setEnabled((index + 1) != jobSubmissions.size());
				moveUpItem.setEnabled(index > 0);
				if (e.isPopupTrigger()
						|| e.getModifiers() == MouseEvent.BUTTON3_MASK) {
					popupMenu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
		togglePanelList.add(togglePanel);
		CellConstraints cc = new CellConstraints();

		tasksLayout.appendRow(new RowSpec("pref"));

		// tasksPanel.addTask(togglePanel);
		tasksPanel.add(togglePanel, cc.xywh(1, tasksLayout.getRowCount(), 2, 1,
				CellConstraints.LEFT, CellConstraints.BOTTOM));
		togglePanel.setBackground(getBackground());
		addTaskComponents(index, js, formalParams, togglePanel);
		togglePanel.setExpanded(true);

	}

	private TaskInfo getTaskInfo(String lsid) {
		AnalysisService svc = AnalysisServiceManager.getInstance()
				.getAnalysisService(lsid);
		if (svc == null) {
			return null;
		}
		return svc.getTaskInfo();
	}

	private void addTaskComponents(int jobSubmissionIndex, JobSubmission js,
			ParameterInfo[] formalParams, GroupPanel togglePanel) {

		ParameterInfo[] actualParameters = js.giveParameterInfoArray();

		boolean[] runtimePrompt = js.getRuntimePrompt();

		Map paramName2ActualParamIndexMap = new HashMap();
		for (int j = 0; j < actualParameters.length; j++) {
			paramName2ActualParamIndexMap.put(actualParameters[j].getName(),
					new Integer(j));
		}

		for (int j = 0; j < formalParams.length; j++) {
			String paramName = formalParams[j].getName();
			ParameterInfo formalParam = formalParams[j];
			Integer index = (Integer) paramName2ActualParamIndexMap
					.get(paramName);

			String value = "";
			String inheritedOutputFileName = null;
			int inheritedTaskNumber = -1;
			if (index == null) {
				value = (String) formalParam.getAttributes().get(
						ParameterInfo.DEFAULT);
				if (value == null) {
					value = "";
				}
				js.getParameters().add(new ParameterInfo(paramName, value, ""));
				boolean[] newRuntimePrompt = new boolean[runtimePrompt.length + 1];
				for (int k = 0; k < runtimePrompt.length; k++) {
					newRuntimePrompt[k] = runtimePrompt[k];
				}
				newRuntimePrompt[newRuntimePrompt.length - 1] = false;
				js.setRuntimePrompt(newRuntimePrompt);
			} else {
				ParameterInfo actualParam = actualParameters[index.intValue()];
				if (formalParam.isInputFile()) {
					java.util.Map pipelineAttributes = actualParam
							.getAttributes();

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

						inheritedOutputFileName = outputFileNumber;
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
					value = actualParam.getValue(); // can be command line value
					// instead of UI value
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
			}
			CellConstraints cc = new CellConstraints();
			JLabel label = new JLabel(AnalysisServiceDisplay
					.getDisplayString(paramName)
					+ ":");
			togglePanel.addToggleComponent(label);
			tasksLayout.appendRow(new RowSpec("pref"));
			tasksPanel.add(label, cc.xy(1, tasksLayout.getRowCount(),
					CellConstraints.RIGHT, CellConstraints.BOTTOM));
			final JTextField inputField = new JTextField(value, 20);
			if (formalParam.isInputFile()) {
				JPanel inputPanel = new JPanel();
				inputPanel.setBackground(getBackground());
				FormLayout inputPanelLayout = new FormLayout(
						"left:pref:none, left:pref:none, left:pref:none, left:pref:none, left:default:none",
						"pref"); // input field, browse, task drop down, output file drop down, checkbox
				inputPanel.setLayout(	inputPanelLayout);

				final JButton browseBtn = new JButton("Browse...");
				browseBtn.setBackground(getBackground());
				browseBtn.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						File f = GUIUtil.showOpenDialog();
						if (f != null) {
							inputField.setText(f.getPath());
						}
					}
				});

				final JCheckBox usePreviousOutput = new JCheckBox(
						"Use Output From Previous Task");
				usePreviousOutput.setBackground(getBackground());
				

				inputPanel.add(inputField, cc.xy(1, 1));
				inputPanel.add(browseBtn, cc.xy(2, 1));
				inputPanel.add(usePreviousOutput, cc.xy(5, 1));

				togglePanel.addToggleComponent(inputPanel);
				tasksPanel.add(inputPanel, cc.xy(3, tasksLayout.getRowCount(),
						CellConstraints.LEFT, CellConstraints.BOTTOM));

				parameterName2ComponentMap.put(paramName, inputField); // FIXME
				inputFileParameters.add(formalParams[j]);

				Vector previousTaskNames = new Vector();
				previousTaskNames.add("Choose Task");
				for (int k = 0; k < jobSubmissionIndex; k++) {
					JobSubmission previousJS = (JobSubmission) jobSubmissions
							.get(k);
					previousTaskNames
							.add((k + 1) + ". " + previousJS.getName());
				}
				final JComboBox tasksComboBox = new JComboBox(previousTaskNames);

				final JComboBox outputFilesComboBox = new JComboBox();
				inputPanel.add(tasksComboBox, cc.xy(3, 1));
				inputPanel.add(outputFilesComboBox, cc.xy(4, 1));
				
				usePreviousOutput.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						browseBtn.setVisible(!usePreviousOutput.isSelected());
						inputField.setVisible(!usePreviousOutput.isSelected());
						tasksComboBox.setVisible(usePreviousOutput.isSelected());
						outputFilesComboBox.setVisible(usePreviousOutput.isSelected());
					}
				});
				
				browseBtn.setVisible(inheritedTaskNumber == -1);
				inputField.setVisible(inheritedTaskNumber == -1);
				tasksComboBox.setVisible(inheritedTaskNumber != -1);
				outputFilesComboBox.setVisible(inheritedTaskNumber != -1);
				usePreviousOutput.setSelected(inheritedTaskNumber != -1);
				tasksComboBox.addItemListener(new ItemListener() {

					public void itemStateChanged(ItemEvent e) {
						if (e.getStateChange() != ItemEvent.SELECTED) {
							return;
						}
						String item = (String) tasksComboBox.getSelectedItem();
						if (item.equals("Choose Task")) {
							outputFilesComboBox.removeAllItems();
							outputFilesComboBox.setVisible(false);
						} else {
							outputFilesComboBox.setVisible(true);
							outputFilesComboBox.removeAllItems();
							int index = Integer.parseInt(item.substring(0, item
									.indexOf("."))) - 1;
							JobSubmission js = (JobSubmission) jobSubmissions
									.get(index);
							TaskInfo taskInfo = getTaskInfo(js.getLSID());

							List outputs = Arrays.asList(((String) taskInfo
									.getTaskInfoAttributes().get("fileFormat"))
									.split(";"));
							Collections.sort(outputs,
									String.CASE_INSENSITIVE_ORDER);
							for (int i = 0; i < outputs.size(); i++) {
								outputFilesComboBox.addItem(outputs.get(i));
							}
						}
					}

				});
				tasksComboBox.setBackground(getBackground());
				outputFilesComboBox.setBackground(getBackground());

				if (inheritedTaskNumber != -1) {
					
					JobSubmission inheritedTask = (JobSubmission) jobSubmissions
							.get(inheritedTaskNumber);

					tasksComboBox.setSelectedItem((inheritedTaskNumber + 1)
							+ ". " + inheritedTask.getName());
					outputFilesComboBox
							.setSelectedItem(inheritedOutputFileName);
				} 
			

			} else {
				tasksPanel.add(inputField, cc.xy(3, tasksLayout.getRowCount(),
						CellConstraints.LEFT, CellConstraints.BOTTOM));
				togglePanel.addToggleComponent(inputField);

			}

		}
	}
}
