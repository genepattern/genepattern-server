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
import javax.swing.JSeparator;
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
import org.genepattern.gpge.ui.tasks.ParameterChoice;
import org.genepattern.gpge.ui.tasks.TaskHelpActionListener;
import org.genepattern.gpge.ui.tasks.TaskNamePanel;
import org.genepattern.gpge.ui.util.GUIUtil;
import org.genepattern.gpge.util.PostData;
import org.genepattern.util.BrowserLauncher;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.ParameterInfo;

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

	private JPanel createButtonPanel(final AnalysisService svc) {
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
					ParameterInfo[] p = svc.getTaskInfo().getParameterInfoArray();
					if(p==null || p.length==0) {
						AnalysisServiceDisplay.doSubmit(runButton, new ParameterInfo[0], svc);
					} else {
						MessageManager
							.notifyListeners(new ChangeViewMessageRequest(
									this,
									ChangeViewMessageRequest.SHOW_RUN_TASK_REQUEST,
									svc));
					}
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
		return bottomPanel;
	}
	
	private void setPipeline(final AnalysisService svc,
			PipelineModel pipelineModel) {
		setMinimumSize(new java.awt.Dimension(100, 100));
		setLayout(new BorderLayout());

		// show edit link when task has local authority and either belongs to
		// current user or is public
		
		model = new PipelineEditorModel(svc, pipelineModel);
		
		tasksLayout = new FormLayout("right:pref, 3dlu, default:grow", "");
		tasksPanel = new AlternatingRowColorPanel(tasksLayout);
		tasksPanel.setBackground(getBackground());
		for (int i = 0; i < model.getTaskCount(); i++) {
			layoutTask(i);
		}
		if (model.getMissingJobSubmissions().size() == 0) {
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

			add(createButtonPanel(svc), BorderLayout.SOUTH);
		} else {
			MissingTasksDisplay mtd = new MissingTasksDisplay(model.getMissingJobSubmissions(), svc);
			taskNamePanel = new TaskNamePanel(svc.getTaskInfo(),
					ChangeViewMessageRequest.SHOW_VIEW_PIPELINE_REQUEST, mtd
							.getErrorPanel());
			add(taskNamePanel, BorderLayout.NORTH);
			JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
					mtd.getScrollPane(), new JScrollPane(tasksPanel));
			add(splitPane, BorderLayout.CENTER);
			splitPane.setDividerLocation(200);
			add(createButtonPanel(svc), BorderLayout.SOUTH);
		}
		invalidate();
		validate();

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
		

		togglePanel.setBackground(getBackground());
		int taskStart = tasksLayout.getRowCount();
		addTaskParameters(taskIndex, togglePanel);
		int taskEnd = tasksLayout.getRowCount();
		int parameterCount = taskEnd - taskStart;
		togglePanel.setExpanded(true);
		tasksPanel.addTask(togglePanel, taskStart, parameterCount);
		
		// add separator between tasks
		tasksLayout.appendRow(new RowSpec("1dlu"));
		tasksLayout.appendRow(new RowSpec("pref"));
		tasksPanel.add(new JSeparator(), cc.xyw(1, tasksLayout.getRowCount(), tasksLayout.getColumnCount()));
		tasksLayout.appendRow(new RowSpec("5dlu"));
		
		

	}

	private void addTaskParameters(final int taskIndex, GroupPanel togglePanel) {
		for (int i = 0; i < model.getParameterCount(taskIndex); i++) {

			CellConstraints cc = new CellConstraints();
			String value = model.getValue(taskIndex, i);
			if(value !=null && value.startsWith("<GenePatternURL>getFile.jsp?task=<LSID>&file=")) {
				value = value.substring("<GenePatternURL>getFile.jsp?task=<LSID>&file=".length(), value.length());
			}
			JLabel field = new JLabel(value);
			JLabel label = new JLabel(AnalysisServiceDisplay
					.getDisplayString(model.getParameterName(taskIndex, i))
					+ ":");
			if(model.isPromptWhenRun(taskIndex, i)) {
				field.setText("Prompt when run");
			} else if (model.getInheritedTaskIndex(taskIndex, i) != -1) {
				final int parameterIndex = i;
				int inheritedTaskIndex = model.getInheritedTaskIndex(taskIndex, i);
				value = "<html>Use <b>"
						+ model.getInheritedFile(taskIndex, i)
						+ "</b> from <b>"
						+ (1 + model.getInheritedTaskIndex(taskIndex, i))
						+ ". " + model.getTaskName(inheritedTaskIndex) + "</b>";
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

			} else if(model.isChoiceList(taskIndex, i)) {
				ParameterChoice[] choiceItems = model.getChoices(taskIndex, i);
				if(choiceItems!=null) {
					for (int j = 0; j < choiceItems.length; j++) {
						if (choiceItems[j]
							.equalsCmdLineOrUIValue(value)) {
							field.setText(choiceItems[j].getUIValue());
							break;
						}
					}
				}
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
