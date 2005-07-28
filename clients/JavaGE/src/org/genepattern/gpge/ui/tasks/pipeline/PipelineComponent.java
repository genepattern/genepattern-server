package org.genepattern.gpge.ui.tasks.pipeline;

import gnu.trove.TIntArrayList;

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
import javax.swing.JComponent;
import javax.swing.JDialog;
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
import org.genepattern.gpge.ui.tasks.ParameterChoice;
import org.genepattern.gpge.ui.tasks.ParameterInfoPanel;
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
	private static final boolean DEBUG = false;

	private PipelineEditorModel model;

	private AlternatingRowColorPanel tasksPanel;

	private FormLayout tasksLayout;

	private ArrayList togglePanelList = new ArrayList();

	private AnalysisService analysisService;

	private JPanel buttonPanel;

	private TaskNamePanel taskNamePanel;

	private Map parameterName2ComponentMap = new HashMap();

	private List inputFileParameters = new ArrayList();

	private JComboBox tasksInPipelineComboBox;

	private JScrollPane scrollPane;

	/**
	 * Currently only one instance should be created by the ViewManager
	 * 
	 */
	public PipelineComponent() {
		setBackground(Color.white);
		setMinimumSize(new java.awt.Dimension(100, 100));
		setLayout(new BorderLayout());

		scrollPane = new JScrollPane();
		Border b = scrollPane.getBorder();
		scrollPane.setBorder(GUIUtil.createBorder(b, 0, -1, -1, -1));
		add(scrollPane, BorderLayout.CENTER);

		tasksInPipelineComboBox = new JComboBox();

		final JButton addButton = new JButton("Add Task After");

		final JButton addBeforeButton = new JButton("Add Task Before");

		final JButton deleteButton = new JButton("Delete");

		final JButton moveUpButton = new JButton("Move Up");

		final JButton moveDownButton = new JButton("Move Down");

		final JButton expandAllButton = new JButton("Expand All");
		final JButton collapseAllButton = new JButton("Collapse All");

		tasksInPipelineComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				int index = cb.getSelectedIndex();
				addBeforeButton.setEnabled(index != 0);
				addButton.setEnabled(index != cb.getItemCount() - 1
						|| cb.getItemCount() == 1);

			}
		});

		ActionListener taskBtnListener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				int index = tasksInPipelineComboBox.getSelectedIndex();
				if (e.getSource() == addButton) {
					showAddTask(index, true);
				} else if (e.getSource() == addBeforeButton) {
					showAddTask(index, false);
				} else if (e.getSource() == deleteButton) {
					model.delete(index);
					// editor.delete(index);
					// setPipeline(editor.getTaskInfo(),
					// editor.getPipelineModel());
				} else if (e.getSource() == moveUpButton) {
					model.move(index, index - 1);
					// editor.moveUp(index, index - 1);
					// setPipeline(editor.getTaskInfo(),
					// editor.getPipelineModel());
				} else if (e.getSource() == moveDownButton) {
					model.move(index, index + 1);
					// editor.moveDown(index, index + 1);
					// setPipeline(editor.getTaskInfo(),
					// editor.getPipelineModel());
				}

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
					/*
					 * for (int i = 0; i < jobSubmissions.size(); i++) {
					 * JobSubmission js = (JobSubmission) jobSubmissions
					 * .get(i); TaskInfo task = getTaskInfo(js.getLSID());
					 * ParameterInfo[] formalParams = task
					 * .getParameterInfoArray(); if (formalParams == null) {
					 * formalParams = new ParameterInfo[0]; } for (int j = 0; j <
					 * formalParams.length; j++) { ParameterInfo p =
					 * formalParams[j];
					 *  }
					 *  }
					 * 
					 * pipelineTaskInfo.getTaskInfoAttributes()
					 * .put(PipelineModel.PIPELINE_MODEL,
					 * pipelineModel.toXML());
					 * 
					 * try { new TaskIntegratorProxy(AnalysisServiceManager
					 * .getInstance().getServer(), userID, false)
					 * .modifyTask(GPConstants.ACCESS_PUBLIC,
					 * pipelineTaskInfo.getName(),
					 * pipelineTaskInfo.getDescription(), pipelineTaskInfo
					 * .getParameterInfoArray(), (HashMap) pipelineTaskInfo
					 * .getTaskInfoAttributes(), new File[] {}); } catch
					 * (WebServiceException e1) { e1.printStackTrace(); if
					 * (!GenePattern.disconnectedFromServer(e1,
					 * AnalysisServiceManager.getInstance() .getServer())) {
					 * GenePattern .showErrorDialog("An error occurred while
					 * saving the pipeline."); } }
					 */
				} else if (e.getSource() == runButton) {
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
		parameterName2ComponentMap.clear();
		inputFileParameters.clear();
		tasksInPipelineComboBox.removeAllItems();
		tasksLayout = new FormLayout("right:pref, 3dlu, default:grow", "");
		tasksPanel = new AlternatingRowColorPanel(tasksLayout);
		tasksPanel.setBackground(getBackground());
		scrollPane.setViewportView(tasksPanel);
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

	private void setPipeline(TaskInfo _pipelineTaskInfo,
			PipelineModel pipelineModel) {
		reset();
		model = new PipelineEditorModel(_pipelineTaskInfo, pipelineModel);
		// editor = new PipelineEditor(pipelineTaskInfo, pipelineModel);
		if (DEBUG) {
			model.print();
		}

		// show edit link when task has local authority and either belongs to
		// current user or is public

		System.out.println(model.getTaskCount() + " Tasks");
		for (int i = 0; i < model.getTaskCount(); i++) {
			layoutTask(i);
			tasksInPipelineComboBox.addItem((i + 1) + ". "
					+ model.getTaskName(i));
		}

		taskNamePanel = new TaskNamePanel(_pipelineTaskInfo,
				ChangeViewMessageRequest.SHOW_EDIT_PIPELINE_REQUEST,
				buttonPanel);

		add(taskNamePanel, BorderLayout.NORTH);
		invalidate();
		validate();
		tasksPanel.invalidate();
		tasksPanel.validate();
	}

	private void layoutTask(final int taskIndex) {
		System.out.println("layoutTask" + taskIndex);
		GroupPanel togglePanel = new GroupPanel((taskIndex + 1) + ". "
				+ model.getTaskName(taskIndex), new JTextField(model
				.getTaskDescription(taskIndex), 80));
		togglePanel.setBackground(getBackground());
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
					showAddTask(taskIndex, true);
				} else if (e.getSource() == addBeforeItem) {
					showAddTask(taskIndex, false);
				} else if (e.getSource() == moveUpItem) {
					model.move(taskIndex, taskIndex - 1);
				} else if (e.getSource() == moveDownItem) {
					model.move(taskIndex, taskIndex + 1);
				} else if (e.getSource() == deleteItem) {
					model.delete(taskIndex);
				}
			}
		};

		addTaskAfterItem.addActionListener(listener);
		addBeforeItem.addActionListener(listener);
		moveUpItem.addActionListener(listener);
		moveDownItem.addActionListener(listener);
		deleteItem.addActionListener(listener);

		togglePanel.getMajorLabel().addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				moveDownItem
						.setEnabled((taskIndex + 1) != model.getTaskCount());
				moveUpItem.setEnabled(taskIndex > 0);
				if (e.isPopupTrigger()
						|| e.getModifiers() == MouseEvent.BUTTON3_MASK) {
					popupMenu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
		togglePanelList.add(togglePanel);
		CellConstraints cc = new CellConstraints();

		tasksLayout.appendRow(new RowSpec("pref"));

		tasksPanel.add(togglePanel, cc.xywh(1, tasksLayout.getRowCount(), 2, 1,
				CellConstraints.LEFT, CellConstraints.BOTTOM));
		int parameterStart = tasksLayout.getRowCount();
		addTaskParameters(taskIndex, togglePanel);
		int parameterEnd = tasksLayout.getRowCount();
		tasksPanel.addTask(togglePanel, parameterEnd - parameterStart);
		togglePanel.setExpanded(true);

	}

	void addTask(int index, AnalysisService svc) {
		model.addTask(index, svc.getTaskInfo());
	}

	private void showAddTask(int jobSubmissionIndex, boolean addAfter) {
		String title;
		int insertionIndex;
		if (addAfter) {
			title = "Add Task After " + (jobSubmissionIndex + 1) + ". "
					+ model.getTaskName(jobSubmissionIndex);
			insertionIndex = jobSubmissionIndex + 1;
		} else {
			title = "Add Task Before " + (jobSubmissionIndex + 1) + ". "
					+ model.getTaskName(jobSubmissionIndex);
			insertionIndex = jobSubmissionIndex + 1;
		}

		JDialog dialog = new TaskChooser(GenePattern.getDialogParent(), title,
				this, insertionIndex);
	}


	private void addTaskParameters(final int taskIndex, GroupPanel togglePanel) {

		int startParameterRow = tasksLayout.getRowCount() + 1;
		for (int i = 0; i < model.getParameterCount(taskIndex); i++) {
			String paramName = model.getParameterName(taskIndex, i);
			CellConstraints cc = new CellConstraints();
			JLabel label = new JLabel(AnalysisServiceDisplay
					.getDisplayString(paramName)
					+ ":");
			togglePanel.addToggleComponent(label);

			tasksLayout.appendRow(new RowSpec("pref"));
			tasksPanel.add(label, cc.xy(1, tasksLayout.getRowCount(),
					CellConstraints.RIGHT, CellConstraints.CENTER));

		
			if (model.isChoiceList(taskIndex, i)) {
				JComboBox comboBox = new JComboBox(model.getChoices(taskIndex, i));
				String value = model.getValue(taskIndex, i);
				for(int j = 0; j < comboBox.getItemCount(); j++) {
					if(((ParameterChoice) comboBox.getItemAt(j)).equalsCmdLineOrUIValue(value)) {
						comboBox.setSelectedIndex(j);
						break;
					}
				}
				comboBox.setSelectedItem(model.getValue(taskIndex, i));
				tasksPanel.add(comboBox, cc.xy(3, tasksLayout
						.getRowCount(), CellConstraints.LEFT,
						CellConstraints.BOTTOM));
				togglePanel.addToggleComponent(comboBox);
			} else if (model.isInputFile(taskIndex, i)) {
				final JTextField inputComponent = new JTextField(20);
				JPanel inputPanel = new JPanel();
				inputPanel.setOpaque(false);
				inputPanel.setBackground(getBackground());
				FormLayout inputPanelLayout = new FormLayout(
						"left:pref:none, left:pref:none, left:pref:none, left:pref:none, left:default:none",
						"pref"); // input field, browse, task drop down,
									// output file drop down, checkbox
				inputPanel.setLayout(inputPanelLayout);

				final JButton browseBtn = new JButton("Browse...");
				browseBtn.setOpaque(false);
				browseBtn.setBackground(getBackground());
				browseBtn.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						File f = GUIUtil.showOpenDialog();
						if (f != null) {
							inputComponent.setText(f.getPath());
						}
					}
				});

				final JCheckBox usePreviousOutput = new JCheckBox(
						"Use Output From Previous Task");
				usePreviousOutput.setOpaque(false);
				usePreviousOutput.setBackground(getBackground());

				inputPanel.add(inputComponent, cc.xy(1, 1));
				inputPanel.add(browseBtn, cc.xy(2, 1));
				inputPanel.add(usePreviousOutput, cc.xy(5, 1));

				togglePanel.addToggleComponent(inputPanel);
				tasksPanel.add(inputPanel, cc.xy(3, tasksLayout.getRowCount(),
						CellConstraints.LEFT, CellConstraints.BOTTOM));

				Vector previousTaskNames = new Vector();
				previousTaskNames.add("Choose Task");
				for (int k = 0; k < taskIndex; k++) {
					previousTaskNames
							.add((k + 1) + ". " + model.getTaskName(k));
				}
				final JComboBox tasksComboBox = new JComboBox(previousTaskNames);
				tasksComboBox.setOpaque(false);
				final JComboBox outputFilesComboBox = new JComboBox();
				outputFilesComboBox.setOpaque(false);
				inputPanel.add(tasksComboBox, cc.xy(3, 1));
				inputPanel.add(outputFilesComboBox, cc.xy(4, 1));

				usePreviousOutput.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						browseBtn.setVisible(!usePreviousOutput.isSelected());
						inputComponent.setVisible(!usePreviousOutput
								.isSelected());
						tasksComboBox
								.setVisible(usePreviousOutput.isSelected());
						outputFilesComboBox.setVisible(usePreviousOutput
								.isSelected()
								&& outputFilesComboBox.getItemCount() > 0);
					}
				});
				int inheritedTaskIndex = model.getInheritedTaskIndex(taskIndex,
						i);
				browseBtn.setVisible(inheritedTaskIndex == -1);
				inputComponent.setVisible(inheritedTaskIndex == -1);
				tasksComboBox.setVisible(inheritedTaskIndex != -1);
				outputFilesComboBox.setVisible(inheritedTaskIndex != -1);
				usePreviousOutput.setSelected(inheritedTaskIndex != -1);
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

							List outputFileTypes = model
									.getOutputFileTypes(taskIndex);
							for (int i = 0; i < outputFileTypes.size(); i++) {
								outputFilesComboBox.addItem(outputFileTypes
										.get(i));
							}
							outputFilesComboBox.addItem("1st output");
							outputFilesComboBox.addItem("2nd output");
							outputFilesComboBox.addItem("3rd output");
							outputFilesComboBox.addItem("4th output");
							outputFilesComboBox.addItem("stdout");
							outputFilesComboBox.addItem("stderr");
						}
					}

				});
				tasksComboBox.setBackground(getBackground());
				outputFilesComboBox.setBackground(getBackground());

				if (inheritedTaskIndex != -1) {
					tasksComboBox.setSelectedItem((inheritedTaskIndex + 1)
							+ ". " + model.getTaskName(inheritedTaskIndex));
					outputFilesComboBox.setSelectedItem(model.getInheritedFile(
							taskIndex, i));
				} else {
					inputComponent.setText(model.getValue(taskIndex, i));
				}

			} else {
				JTextField inputComponent = new JTextField(model.getValue(taskIndex, i), 20);
				tasksPanel.add(inputComponent, cc.xy(3, tasksLayout
						.getRowCount(), CellConstraints.LEFT,
						CellConstraints.BOTTOM));
				togglePanel.addToggleComponent(inputComponent);

			}
		}

		int endParameterRow = tasksLayout.getRowCount();
		int[] group = new int[endParameterRow - startParameterRow + 1];
		for (int i = startParameterRow, index = 0; i <= endParameterRow; i++, index++) {
			group[index] = i;
		}
		int[][] rowGroups = tasksLayout.getRowGroups();
		int[][] newRowGroups = new int[rowGroups.length + 1][];
		for (int i = 0; i < rowGroups.length; i++) {
			newRowGroups[i] = rowGroups[i];
		}
		newRowGroups[newRowGroups.length - 1] = group;
		tasksLayout.setRowGroups(newRowGroups);
	}
}
