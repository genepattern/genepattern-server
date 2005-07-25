package org.genepattern.gpge.ui.tasks.pipeline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.message.ChangeViewMessageRequest;
import org.genepattern.gpge.message.GPGEMessage;
import org.genepattern.gpge.message.GPGEMessageListener;
import org.genepattern.gpge.message.MessageManager;
import org.genepattern.gpge.ui.graphics.draggable.ObjectTextField;
import org.genepattern.gpge.ui.maindisplay.TogglePanel;
import org.genepattern.gpge.ui.maindisplay.GroupPanel;
import org.genepattern.gpge.ui.table.AlternatingColorTable;
import org.genepattern.gpge.ui.tasks.AnalysisServiceDisplay;
import org.genepattern.gpge.ui.tasks.AnalysisServiceManager;
import org.genepattern.gpge.ui.tasks.TaskHelpActionListener;
import org.genepattern.gpge.ui.tasks.TaskNamePanel;
import org.genepattern.gpge.ui.util.GUIUtil;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.util.LSIDUtil;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.debug.FormDebugPanel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

// inherited task numbers start at 0, output files start at one,, names for params start at one
public class PipelineComponent extends JPanel {
	private PipelineModel pipelineModel;
	
	private List jobSubmissions;

	private String userID;

	private TaskInfo pipelineTaskInfo;

	private boolean viewOnly;
	
	private AlternatingRowColorPanel tasksPanel;

	private FormLayout tasksLayout;
	
	private ArrayList togglePanelList = new ArrayList();

	private PipelineEditor editor;

	private AnalysisService svc;
	
	/**
	 * Currently only one instance should be created by the ViewManager
	 *
	 */
	public PipelineComponent() {
		setBackground(Color.white);
	}
	
	void clear() {
		togglePanelList.clear();
		removeAll();
	}
	
	public void display(AnalysisService svc, boolean viewOnly) {
		clear();
		this.svc = svc;
		TaskInfo info = svc.getTaskInfo();
		this.viewOnly = viewOnly;
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

	/**
     * 
     * @param parameterIndex the index of the parameter, starting at 0
     * @param previousTaskNames, the array of task names to display
     * @param choices the array of output type choices to display
     */
	private void setUseInputFromPreviousTask(int parameterIndex, String[] previousTaskNames, String[] choices) {
		int row = 0 ; //getRowIndex(parameterIndex) + USE_OUTPUT_ROW_OFFSET;
		JLabel tasksLabel = new JLabel("or use output from");
		JComboBox tasksComboBox = new JComboBox(previousTaskNames);
		tasksComboBox.setBackground(getBackground());
		
		JLabel outputFilesLabel = new JLabel("output file");
		JComboBox outputFilesComboBox = new JComboBox(choices);
		outputFilesComboBox.setBackground(getBackground());
		
		JPanel p = new JPanel();
		p.setBackground(getBackground());
		p.add(tasksLabel);
		p.add(tasksComboBox);
		p.add(outputFilesLabel);
		p.add(outputFilesComboBox);
		CellConstraints cc = new CellConstraints();
		; // this.add(p, cc.xy(PARAMETER_INPUT_FIELD_COLUMN, row));
	}
	
	
	private void setPipeline(TaskInfo pipelineTaskInfo, PipelineModel model) {
		
		this.pipelineTaskInfo = pipelineTaskInfo;
		this.pipelineModel = model;
		this.jobSubmissions = pipelineModel.getTasks();
		this.userID = pipelineTaskInfo.getUserId();
	
		String displayName = pipelineModel.getName();
		if (displayName.endsWith(".pipeline")) {
			displayName = displayName.substring(0, displayName.length()
					- ".pipeline".length());
		}

		// show edit link when task has local authority and either belongs to
		// current user or is public
		
		
		
		tasksLayout = new FormLayout(
	             "right:pref, 3dlu, default:grow", 
	             "");
	
		
		tasksPanel = new AlternatingRowColorPanel(tasksLayout);
		tasksPanel.setBackground(getBackground());
		
		List tasks = pipelineModel.getTasks();
		for (int i = 0; i < tasks.size(); i++) {
			layoutTask(i, (JobSubmission) tasks.get(i));
		}
		
		setMinimumSize(new java.awt.Dimension(100, 100));
		setLayout(new BorderLayout());
		JScrollPane sp = new JScrollPane(tasksPanel);
		Border b = sp.getBorder();
		sp.setBorder(GUIUtil.createBorder(b, 0, -1, -1, -1));
		add(sp, BorderLayout.CENTER);
		int messageType;
		if(viewOnly) {
			messageType = ChangeViewMessageRequest.SHOW_VIEW_PIPELINE_REQUEST;
		} else {
			messageType = ChangeViewMessageRequest.SHOW_EDIT_PIPELINE_REQUEST;
		}
		final JButton expandAllButton = new JButton("Expand All");
		final JButton collapseAllButton = new JButton("Collapse All");

		JPanel expandPanel = new JPanel();
		expandPanel.add(collapseAllButton);
		expandPanel.add(expandAllButton);
		
		ActionListener expandListener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if(e.getSource()==expandAllButton) {
					for(int i = 0; i < togglePanelList.size(); i++) {
						GroupPanel p = (GroupPanel) togglePanelList.get(i);
						p.setExpanded(true);
					}
				} else if(e.getSource()==collapseAllButton) {
					for(int i = 0; i < togglePanelList.size(); i++) {
						GroupPanel p = (GroupPanel) togglePanelList.get(i);
						p.setExpanded(false);
					}
				} 
			}
			
		};
		
		expandAllButton.addActionListener(expandListener);
		collapseAllButton.addActionListener(expandListener);
		
		JPanel taskNamePanel = new TaskNamePanel(pipelineTaskInfo,
				messageType, expandPanel);
		
		add(taskNamePanel, BorderLayout.NORTH);
		
		JPanel bottomPanel = new JPanel();
		final JButton runButton = new JButton("Run");
		final JButton editButton = new JButton("Edit");
		final JButton helpButton = new JButton("Help");
		helpButton.addActionListener(new TaskHelpActionListener(svc));
		
		ActionListener btnListener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if(e.getSource()==runButton) {
					MessageManager.notifyListeners(new ChangeViewMessageRequest(this, ChangeViewMessageRequest.SHOW_RUN_TASK_REQUEST, svc));
				} else if(e.getSource()==editButton) {
					MessageManager.notifyListeners(new ChangeViewMessageRequest(this, ChangeViewMessageRequest.SHOW_EDIT_PIPELINE_REQUEST, svc));
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
					.getParameterInfoArray()
					: null;
			if (formalParams == null) {
				formalParams = new ParameterInfo[0];
			}
			
			JComponent descriptionComponent;
			if(viewOnly) {
				descriptionComponent = new JLabel(js.getDescription());
			} else {
				descriptionComponent = new JTextField(js.getDescription(), 80);
			}
			
			GroupPanel togglePanel = new GroupPanel((index + 1) + ". "
					+ formalTaskInfo.getName(), formalTaskInfo.getDescription());
			togglePanelList.add(togglePanel);
			CellConstraints cc = new CellConstraints();
			
			tasksLayout.appendRow(new RowSpec("pref"));
			
			// FIXME tasksPanel.addTask(tasksLayout.getRowCount());
			tasksPanel.add(togglePanel, cc.xywh(1, tasksLayout.getRowCount(), 2, 1, CellConstraints.LEFT, CellConstraints.BOTTOM));
			togglePanel.setBackground(getBackground());
			addTaskComponents(js, formalParams, togglePanel);
			togglePanel.setExpanded(true);

			if (!viewOnly) {
				final JButton addButton = new JButton("Add Task After");
				addButton.setBackground(getBackground());
				final JButton addBeforeButton = new JButton("Add Task Before");
				addBeforeButton.setBackground(getBackground());
				final JButton deleteButton = new JButton("Delete");
				deleteButton.setBackground(getBackground());
				final JButton moveUpButton = new JButton("Move Up");
				moveUpButton.setBackground(getBackground());
				final JButton moveDownButton = new JButton("Move Down");
				moveDownButton.setBackground(getBackground());

				ActionListener listener = new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						Object source = e.getSource();
						if (source == deleteButton) {
							editor.delete(index);
						} else if (source == addButton) {
							AnalysisService temp = AnalysisServiceManager
									.getInstance().getAnalysisService(
											"ConvertLineEndings");
							editor.addTask(index + 1, temp.getTaskInfo());
						} else if (source == moveUpButton) {
							editor.moveUp(index, index - 1);
						} else if (source == moveDownButton) {
							editor.moveDown(index, index + 1);
						} else if (source == addBeforeButton) {
							AnalysisService temp = AnalysisServiceManager
									.getInstance().getAnalysisService(
											"ConvertLineEndings");
							editor.addTask(index, temp.getTaskInfo());
						}
					}
				};
				deleteButton.addActionListener(listener);
				addButton.addActionListener(listener);
				addBeforeButton.addActionListener(listener);
				moveUpButton.addActionListener(listener);
				moveDownButton.addActionListener(listener);

				JPanel bottomPanel = new JPanel();
				bottomPanel.setBackground(getBackground());
				FormLayout formLayout = new FormLayout(
						"left:pref, left:pref, left:pref, left:pref, left:pref",
						"pref");
				bottomPanel.setLayout(formLayout);
				cc = new CellConstraints();
				bottomPanel.add(addButton, cc.xy(1, 1));
				bottomPanel.add(addBeforeButton, cc.xy(2, 1));
				bottomPanel.add(deleteButton, cc.xy(3, 1));
				bottomPanel.add(moveUpButton, cc.xy(4, 1));
				bottomPanel.add(moveDownButton, cc.xy(5, 1));
				add(bottomPanel, BorderLayout.SOUTH);
			}
		}


	private TaskInfo getTaskInfo(String lsid) {
		AnalysisService svc = AnalysisServiceManager.getInstance()
				.getAnalysisService(lsid);
		if (svc == null) {
			return null;
		}
		return svc.getTaskInfo();
	}
	
	private void addTaskComponents(JobSubmission js, ParameterInfo[] formalParams, GroupPanel togglePanel) {

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
					inheritedTaskNumber = Integer.parseInt(taskNumberString.trim());
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
					
					value = "<html>Use <b>" + inheritedOutputFileName + "</b> from <b>"
							+ displayTaskNumber + ". "
							+ inheritedTask.getName() + "</b></html>";
					
				} else {
					value = actualParam.getValue();
					if(value.startsWith("<GenePatternURL>getFile.jsp?task=<LSID>&file=")) {
						value = value.substring("<GenePatternURL>getFile.jsp?task=<LSID>&file=".length(), value.length());
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
			if(viewOnly) {
				CellConstraints cc = new CellConstraints();
				JLabel field = new JLabel(value);
				JLabel label = new JLabel(AnalysisServiceDisplay.getDisplayString(paramName) + ":");
				if(inheritedTaskNumber!=-1) {
					final int inheritedTaskIndex = inheritedTaskNumber;
					field.addMouseListener(new MouseAdapter() {

						public void mouseEntered(MouseEvent e) {
								GroupPanel p = (GroupPanel) togglePanelList.get(inheritedTaskIndex);
								p.setMajorLabelForeground(Color.red);
						}
						public void mouseExited(MouseEvent e) {
								GroupPanel p = (GroupPanel) togglePanelList.get(inheritedTaskIndex);
								p.setMajorLabelForeground(Color.black);	
						}
						
					});
				
				} 
				
				tasksLayout.appendRow(new RowSpec("pref"));
				tasksPanel.add(label, cc.xy(1, tasksLayout.getRowCount(), CellConstraints.RIGHT, CellConstraints.BOTTOM));
				tasksPanel.add(field, cc.xy(3, tasksLayout.getRowCount(), CellConstraints.LEFT, CellConstraints.BOTTOM));
				
				togglePanel.addToggleComponent(field);
				togglePanel.addToggleComponent(label);
			} else {
			//	JLabel field = new JLabel(value);
			//	JLabel label = builder.append(AnalysisServiceDisplay.getDisplayString(paramName), field);
				//togglePanel.addComponent(field);
				//togglePanel.addComponent(label);
			}
			
		}
	}
}
