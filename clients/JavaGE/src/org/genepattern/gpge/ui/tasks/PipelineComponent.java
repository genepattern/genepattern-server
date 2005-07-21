package org.genepattern.gpge.ui.tasks;

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
import org.genepattern.gpge.ui.maindisplay.TogglePanel2;
import org.genepattern.gpge.ui.table.AlternatingColorTable;
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
	
	private JPanel tasksPanel;

	private FormLayout tasksLayout;
	
	private ArrayList togglePanelList = new ArrayList();
	
	/**
	 * Currently only one instance should be created by the ViewManager
	 *
	 */
	public PipelineComponent() {
		setBackground(Color.white);
	}
	
	private static TaskInfo cloneTaskInfo(TaskInfo taskInfo) {
		TaskInfo c = new TaskInfo(taskInfo.getID(), taskInfo.getName(), taskInfo.getDescription(),
				taskInfo.getParameterInfo(), (TaskInfoAttributes) taskInfo.getTaskInfoAttributes());
		return c;
	}
	
	private static ParameterInfo createRTPromptParameterForMap(ParameterInfo p) {
		ParameterInfo rtParam = new ParameterInfo(p.getName(), "", "");
		HashMap attrs = new HashMap();
		attrs.put(PipelineModel.RUNTIME_PARAM, "1");
		rtParam.setAttributes(attrs);
		return rtParam;
		// in PipelineModel.getInputParameters {ConvertLineEndings1.input.filename maps to name=input.filename value= Description Attribute:{runTimePrompt=1}}
		// in TaskInfo for pipeline [name=ConvertLineEndings1.input.filename runTimePrompt=1]
	}
	
	private ParameterInfo createRTPromptParameterForTaskInfo(JobSubmission js, int index, ParameterInfo p) {
		TaskInfo task = getTaskInfo(js.getLSID());
		ParameterInfo[] taskInfoParams = task.getParameterInfoArray();
		ParameterInfo formalParam = null;
		for(int i = 0; i < taskInfoParams.length; i++) {
			if(taskInfoParams[i].getName().equals(p.getName())) {
				formalParam = taskInfoParams[i];
			}
		}
		ParameterInfo rtParam = new ParameterInfo(js.getName() + (index+1) + "." + p.getName(), "", formalParam.getDescription());
		HashMap attrs = new HashMap(formalParam.getAttributes());
		attrs.put(PipelineModel.RUNTIME_PARAM, "1");
		rtParam.setAttributes(attrs);
		return rtParam;
		// in PipelineModel.getInputParameters {ConvertLineEndings1.input.filename maps to name=input.filename value= Description Attribute:{runTimePrompt=1}}
		// in TaskInfo for pipeline [name=ConvertLineEndings1.input.filename runTimePrompt=1]
	}
	
	private 	void moveTask(int from, int to) {
		if(to < from) {
			moveUp(from, to);
		} else if(to > from) {
			moveDown(from, to);
		}
	}
	
	private static interface ParameterItereratorCallBack {
		void param(JobSubmission js, int index, ParameterInfo p, Map parameterAttributes, int inheritTaskNumber);
	}
	
	private void iterate(int from, int to, Map inputParamsMap, List promptWhenRunParameters, ParameterItereratorCallBack cb) {
		List currentTasks = pipelineModel.getTasks();
		for (int i = from; i < to; i++) {
			JobSubmission js = (JobSubmission) currentTasks.get(i);
			List parameterInfo = js.getParameters();

			for (int j = 0; j < parameterInfo.size(); j++) {
				ParameterInfo p = (ParameterInfo) parameterInfo.get(j);
				Map parameterAttributes = p.getAttributes();
				int taskNumber = -1;
				if (parameterAttributes != null) {
					String taskNumberString = (String) parameterAttributes
					.get(PipelineModel.INHERIT_TASKNAME);

					if (taskNumberString != null) {
						taskNumber = Integer.parseInt(taskNumberString);
					}
				}
				cb.param(js, i, p, parameterAttributes, taskNumber);
				if(js.getRuntimePrompt()[j]){
					ParameterInfo mapParam = createRTPromptParameterForMap(p);
					ParameterInfo taskInfoParam = createRTPromptParameterForTaskInfo(js, i,  p);
					promptWhenRunParameters.add(taskInfoParam);
					inputParamsMap.put(taskInfoParam.getName(), mapParam);
				}
			}
		}
	}
	
	/**
	 * @param from 
	 * @param to
	 */
	private void moveUp(final int from, final int to) {
		if(from < to) {
			throw new IllegalArgumentException();
		}
		List currentTasks = pipelineModel.getTasks();
		
		JobSubmission movedTask = (JobSubmission) currentTasks.remove(from);
		currentTasks.add(to, movedTask);
		
		TaskInfo pipelineTaskInfo = cloneTaskInfo(this.pipelineTaskInfo);
		List promptWhenRunParameters = new ArrayList();
		TreeMap inputParamsMap = pipelineModel.getInputParameters();	
		inputParamsMap.clear();
		
		iterate(from, currentTasks.size(), inputParamsMap, promptWhenRunParameters, 
				new ParameterItereratorCallBack() {

					public void param(JobSubmission js, int index,
							ParameterInfo p, Map parameterAttributes,
							int inheritTaskNumber) {
						if (inheritTaskNumber == from) { // task inherited
							// from moved
							// task
							parameterAttributes.put(
									PipelineModel.INHERIT_TASKNAME, String
											.valueOf(to));
						} else if(inheritTaskNumber >= to && inheritTaskNumber < from) { // tasks >= to and < from have task number increased by 1
							parameterAttributes.put(
									PipelineModel.INHERIT_TASKNAME, String
											.valueOf(inheritTaskNumber+1));
						}
					}

		});

		iterate(to, from, inputParamsMap, promptWhenRunParameters, 
				new ParameterItereratorCallBack() {

					public void param(JobSubmission js, int index,
							ParameterInfo p, Map parameterAttributes,
							int inheritTaskNumber) {
		
						if (index==to && inheritTaskNumber > to) { // if moved task inherits from a
							// task that is > to then moved
							// task loses inheritance
							parameterAttributes.remove(PipelineModel.INHERIT_TASKNAME);
							parameterAttributes.remove(PipelineModel.INHERIT_FILENAME);
						} else if (inheritTaskNumber >= to) {
							parameterAttributes.put(
									PipelineModel.INHERIT_TASKNAME, String
									.valueOf(index + 1));
						}
					}
		});
	
		// update prompt when run parameters
		iterate(0, to,  inputParamsMap, promptWhenRunParameters, new ParameterItereratorCallBack() {
			public void param(JobSubmission js, int index, ParameterInfo p,
					Map parameterAttributes, int inheritTaskNumber) {
				
			}
		});
		pipelineTaskInfo.setParameterInfoArray((ParameterInfo[]) promptWhenRunParameters.toArray(new ParameterInfo[0]));
		setPipeline(pipelineTaskInfo, pipelineModel);
	}
	
	
	/**
	 * Increases the position of a task in a pipeline
	 * @param from
	 * @param to
	 */
	private void moveDown(final int from, final int to) {
		List currentTasks = pipelineModel.getTasks();
		
		// notes: moved task can't lose inheritance, tasks that inherited from moved task can lose inheritance if task is moved beyond them
		
		JobSubmission movedTask = (JobSubmission) currentTasks.remove(from);
		currentTasks.add(to, movedTask);
		
		TaskInfo pipelineTaskInfo = cloneTaskInfo(this.pipelineTaskInfo);
		List promptWhenRunParameters = new ArrayList();
		TreeMap inputParamsMap = pipelineModel.getInputParameters();	
		inputParamsMap.clear();
		
		iterate(0, from, inputParamsMap, promptWhenRunParameters, 
				new ParameterItereratorCallBack() {
					public void param(JobSubmission js, int index, ParameterInfo p,
							Map parameterAttributes, int inheritTaskNumber) {
						// update prompt when run parameters
					}
					

				});

		iterate(from, currentTasks.size(),  inputParamsMap, promptWhenRunParameters, new ParameterItereratorCallBack() {
			public void param(JobSubmission js, int index, ParameterInfo p,
					Map parameterAttributes, int inheritTaskNumber) {
				if (inheritTaskNumber == from || index < to) { // task lost inheritance
					parameterAttributes.remove(PipelineModel.INHERIT_TASKNAME);
					parameterAttributes.remove(PipelineModel.INHERIT_FILENAME);
				} else if(inheritTaskNumber == from) {
					parameterAttributes.put(
							PipelineModel.INHERIT_TASKNAME, String
									.valueOf(to));
				} else if(inheritTaskNumber > from && inheritTaskNumber <= to) { // tasks > from and <= to have task number decreased by 1
					parameterAttributes.put(
							PipelineModel.INHERIT_TASKNAME, String
									.valueOf(index - 1));
				}
				
			}
		});
		pipelineTaskInfo.setParameterInfoArray((ParameterInfo[]) promptWhenRunParameters.toArray(new ParameterInfo[0]));
		setPipeline(pipelineTaskInfo, pipelineModel);
		
	}
	
	
	/**
	 * Adds a new task
	 * @param index
	 * @param task
	 */
	private void addTask(int index, TaskInfo task) {
		JobSubmission addedJob = new  JobSubmission(pipelineTaskInfo.getName(), pipelineTaskInfo.getDescription(), (String) task.getTaskInfoAttributes().get(GPConstants.LSID),
				task.getParameterInfoArray(), new boolean[task.getParameterInfoArray().length],
				TaskLauncher.isVisualizer(task), pipelineTaskInfo);
		addTask(index, addedJob, true, true);
	}

	/**
	 *  Inserts the task at the given index. 
	 * @param index
	 * @param jobToAdd
	 * @param updateInheritedFiles
	 * @param doLayout
	 */
	private void addTask(int index, JobSubmission jobToAdd, boolean updateInheritedFiles, boolean doLayout) {
		List currentTasks = pipelineModel.getTasks();
		currentTasks.add(index, jobToAdd);
		List promptWhenRunParameters = new ArrayList();
		TreeMap inputParamsMap = pipelineModel.getInputParameters();	
		inputParamsMap.clear();
		TaskInfo pipelineTaskInfo = cloneTaskInfo(this.pipelineTaskInfo);
		// check if other tasks inherit from this task or subsequent tasks
		iterate(index+1, currentTasks.size(), inputParamsMap, promptWhenRunParameters, 
				new ParameterItereratorCallBack() {

					public void param(JobSubmission js, int index,
							ParameterInfo p, Map parameterAttributes,
							int inheritTaskNumber) {
						if (inheritTaskNumber >= index) { // task inherits from a task that was at index or later
							parameterAttributes.put(
									PipelineModel.INHERIT_TASKNAME, String
											.valueOf(inheritTaskNumber+1)); // increase task number by one 
						}
						
					}
				});
		
		iterate(0, index, inputParamsMap, promptWhenRunParameters, 
				new ParameterItereratorCallBack() {

					public void param(JobSubmission js, int index,
							ParameterInfo p, Map parameterAttributes,
							int inheritTaskNumber) {
						
						
					}
		});
		pipelineTaskInfo.setParameterInfoArray((ParameterInfo[]) promptWhenRunParameters.toArray(new ParameterInfo[0]));
			
		if(doLayout) {
			setPipeline(pipelineTaskInfo, pipelineModel);
		}
	}
	
	void print() {
		ParameterInfo[] pi = pipelineTaskInfo.getParameterInfoArray();
		if(pi!=null) {
			System.out.print("TaskInfo: ");
			System.out.println(Arrays.asList(pi));
		}
		System.out.println();
		System.out.print("input params: ");
		System.out.println(pipelineModel.getInputParameters());
		System.out.println();
		
		System.out.println("Job submissions: ");
		List currentTasks = pipelineModel.getTasks();
		for (int i = 0; i < currentTasks.size(); i++) {
			JobSubmission js = (JobSubmission) currentTasks.get(i);
			List parameterInfo = js.getParameters();
			System.out.print("task number" + i);
			System.out.println(parameterInfo);
		}
		System.out.println();
		System.out.println();
	}

	private void delete(final int index, boolean doLayout) {
		List currentTasks = pipelineModel.getTasks();
		List promptWhenRunParameters = new ArrayList();
		TreeMap inputParamsMap = pipelineModel.getInputParameters();	
		inputParamsMap.clear();
		TaskInfo pipelineTaskInfo = cloneTaskInfo(this.pipelineTaskInfo);
		currentTasks.remove(index);
		// check if subsequent tasks inherit from removed task or subsequent tasks
		iterate(index, currentTasks.size(), inputParamsMap, promptWhenRunParameters, 
				new ParameterItereratorCallBack() {

					public void param(JobSubmission js, int index,
							ParameterInfo p, Map parameterAttributes,
							int inheritTaskNumber) {
						if (inheritTaskNumber == index) { // lost inheritance
							parameterAttributes
									.remove(PipelineModel.INHERIT_TASKNAME);
							parameterAttributes
									.remove(PipelineModel.INHERIT_FILENAME);
							System.out
									.println("lost inheritance " + inheritTaskNumber);
						} else if (inheritTaskNumber > index) { // inherits from a task that comes after deleted task
							parameterAttributes.put(
									PipelineModel.INHERIT_TASKNAME, String
											.valueOf(inheritTaskNumber-1)); // decrease task number by one
						}
					}
		});
		pipelineTaskInfo.setParameterInfoArray((ParameterInfo[]) promptWhenRunParameters.toArray(new ParameterInfo[0]));
		
		if(doLayout) {
			setPipeline(pipelineTaskInfo, pipelineModel);
		}
	}

	public void setTaskInfo(TaskInfo info, boolean viewOnly) {
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
		removeAll();
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
		List tasks = pipelineModel.getTasks();
		
		
		tasksLayout = new FormLayout(
	             "right:pref, 3dlu, default:grow", 
	             "");
	
		
		tasksPanel = new AlternatingRowColorPanel(tasksLayout);
		tasksPanel.setBackground(getBackground());
		for (int i = 0; i < tasks.size(); i++) {
			layoutTask(i, (JobSubmission) tasks.get(i));
		}
		
		setLayout(new BorderLayout());
		add(new JScrollPane(tasksPanel), BorderLayout.CENTER);
		JPanel taskNamePanel = new TaskNamePanel(pipelineTaskInfo,
				ChangeViewMessageRequest.SHOW_EDIT_PIPELINE_REQUEST);
		add(taskNamePanel, BorderLayout.NORTH);

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
			
			TogglePanel2 togglePanel = new TogglePanel2((index + 1) + ". "
					+ formalTaskInfo.getName());
			togglePanelList.add(togglePanel);
			CellConstraints cc = new CellConstraints();
			tasksLayout.appendRow(new RowSpec("pref"));
			
			tasksPanel.add(togglePanel, cc.xy(1, tasksLayout.getRowCount(), CellConstraints.LEFT, CellConstraints.BOTTOM));
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
							delete(index, true);
						} else if (source == addButton) {
							AnalysisService temp = AnalysisServiceManager
									.getInstance().getAnalysisService(
											"ConvertLineEndings");
							addTask(index + 1, temp.getTaskInfo());
						} else if (source == moveUpButton) {
							moveUp(index, index - 1);
						} else if (source == moveDownButton) {
							moveDown(index, index + 1);
						} else if (source == addBeforeButton) {
							AnalysisService temp = AnalysisServiceManager
									.getInstance().getAnalysisService(
											"ConvertLineEndings");
							addTask(index, temp.getTaskInfo());
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
	
	private void addTaskComponents(JobSubmission js, ParameterInfo[] formalParams, TogglePanel2 togglePanel) {

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
				value = actualParam.getValue(); // can be command  
				/*String[] choices = formalParam.getValue().split(
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
				}*/
			}
			if(viewOnly) {
				CellConstraints cc = new CellConstraints();
				JLabel field = new JLabel(value);
				JLabel label = new JLabel(AnalysisServiceDisplay.getDisplayString(paramName) + ":");
				if(inheritedTaskNumber!=-1) {
					final int inheritedTaskIndex = inheritedTaskNumber;
					
					//field.setEditable(false);
					field.addMouseListener(new MouseAdapter() {

						public void mouseEntered(MouseEvent e) {
							
							
								TogglePanel2 p = (TogglePanel2) togglePanelList.get(inheritedTaskIndex);
								p.setForeground(Color.red);
						}
						public void mouseExited(MouseEvent e) {
								
								TogglePanel2 p = (TogglePanel2) togglePanelList.get(inheritedTaskIndex);
								p.setForeground(Color.black);
								p.repaint();

							
						}
						
					});
				
				} 
				
				tasksLayout.appendRow(new RowSpec("pref"));
				tasksPanel.add(label, cc.xy(1, tasksLayout.getRowCount(), CellConstraints.RIGHT, CellConstraints.BOTTOM));
				tasksPanel.add(field, cc.xy(3, tasksLayout.getRowCount(), CellConstraints.LEFT, CellConstraints.BOTTOM));
				
				togglePanel.addComponent(field);
				togglePanel.addComponent(label);
			} else {
			//	JLabel field = new JLabel(value);
			//	JLabel label = builder.append(AnalysisServiceDisplay.getDisplayString(paramName), field);
				//togglePanel.addComponent(field);
				//togglePanel.addComponent(label);
			}
			
		}
	}
}
