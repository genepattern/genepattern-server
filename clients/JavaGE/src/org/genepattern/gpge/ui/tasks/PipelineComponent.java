package org.genepattern.gpge.ui.tasks;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.message.ChangeViewMessageRequest;
import org.genepattern.gpge.message.GPGEMessage;
import org.genepattern.gpge.message.GPGEMessageListener;
import org.genepattern.gpge.message.MessageManager;
import org.genepattern.gpge.ui.graphics.draggable.ObjectTextField;
import org.genepattern.gpge.ui.maindisplay.TogglePanel;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.util.LSIDUtil;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

// inherited task numbers start at 0, output files start at one,, names for params start at one
public class PipelineComponent extends JPanel {
	private PipelineModel pipelineModel;

	private List jobSubmissions;

	private String userID;

	private TaskInfo pipelineTaskInfo;

	private boolean viewOnly;
	
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
	
	void moveTask(int from, int to) {
		if(to < from) {
			moveUp(from, to);
		} else if(to > from) {
			moveDown(from, to);
		}
	}
	
	static interface ParameterItereratorCallBack {
		void param(JobSubmission js, int index, ParameterInfo p, Map parameterAttributes, int inheritTaskNumber);
	}
	
	void iterate(int from, int to, Map inputParamsMap, List promptWhenRunParameters, ParameterItereratorCallBack cb) {
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
	void moveUp(final int from, final int to) {
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
		GenePattern.DEBUG = true;
		setPipeline(pipelineTaskInfo, pipelineModel);
	}
	
	
	/**
	 * Increases the position of a task in a pipeline
	 * @param from
	 * @param to
	 */
	void moveDown(final int from, final int to) {
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
		GenePattern.DEBUG = true;
		pipelineTaskInfo.setParameterInfoArray((ParameterInfo[]) promptWhenRunParameters.toArray(new ParameterInfo[0]));
		setPipeline(pipelineTaskInfo, pipelineModel);
		
	}
	
	
	/**
	 * Adds a new task
	 * @param index
	 * @param task
	 */
	public void addTask(int index, TaskInfo task) {
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
	void addTask(int index, JobSubmission jobToAdd, boolean updateInheritedFiles, boolean doLayout) {
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

	public void delete(final int index, boolean doLayout) {
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

	private void setPipeline(TaskInfo pipelineTaskInfo, PipelineModel model) {
		
		removeAll();

		this.pipelineTaskInfo = pipelineTaskInfo;
		this.pipelineModel = model;
		this.jobSubmissions = pipelineModel.getTasks();
		this.userID = pipelineTaskInfo.getUserId();
	
		if(GenePattern.DEBUG) {
			print();
		}
		String displayName = pipelineModel.getName();
		if (displayName.endsWith(".pipeline")) {
			displayName = displayName.substring(0, displayName.length()
					- ".pipeline".length());
		}

		// show edit link when task has local authority and either belongs to
		// current user or is public
		List tasks = pipelineModel.getTasks();
		StringBuffer rowSpec = new StringBuffer();
		for (int i = 0; i < tasks.size(); i++) {
			if (i > 0) {
				rowSpec.append(", ");
			}
			rowSpec.append("pref");// input, description space

		}

		JPanel tasksPanel = new JPanel();
		FormLayout formLayout = new FormLayout( // 
				"left:pref:none", rowSpec.toString());

		tasksPanel.setLayout(formLayout);
		CellConstraints cc = new CellConstraints();
		for (int i = 0; i < tasks.size(); i++) {
			tasksPanel.add(new PipelineTask(i, (JobSubmission) tasks.get(i)),
					cc.xy(1, (i + 1)));
		}
		setLayout(new BorderLayout());
		add(new JScrollPane(tasksPanel), BorderLayout.CENTER);
		JPanel taskNamePanel = new TaskNamePanel(pipelineTaskInfo,
				ChangeViewMessageRequest.SHOW_EDIT_PIPELINE_REQUEST);
		add(taskNamePanel, BorderLayout.NORTH);

		invalidate();
		validate();

	}

	protected TaskInfo getTaskInfo(String lsid) {
		AnalysisService svc = AnalysisServiceManager.getInstance()
				.getAnalysisService(lsid);
		if (svc == null) {
			return null;
		}
		return svc.getTaskInfo();
	}

	private class PipelineTask extends JPanel {

		public PipelineTask(final int index, JobSubmission js) {
			setBackground(Color.white);
			TaskInfo formalTaskInfo = getTaskInfo(js.getLSID());
			ParameterInfo[] formalParams = formalTaskInfo != null ? formalTaskInfo
					.getParameterInfoArray()
					: null;
			if (formalParams == null) {
				formalParams = new ParameterInfo[0];
			}
			int maxLabelWidth = 0;
			ParameterInfoPanel parameterInfoPanel = new ParameterInfoPanel(js
					.getName(), formalParams, viewOnly);
			
			//parameterInfoPanel.setUseInputFromPreviousTask(0,
			//		new String[] { "a" }, new String[] { "b" });
			maxLabelWidth = Math.max(maxLabelWidth, parameterInfoPanel
					.getLabelWidth());
			JComponent descriptionComponent;
			if(viewOnly) {
				descriptionComponent = new JLabel(js.getDescription());
			} else {
				descriptionComponent = new JTextField(js.getDescription(), 80);
			}
			
			TogglePanel togglePanel = new TogglePanel((index + 1) + ". "
					+ formalTaskInfo.getName(), descriptionComponent, parameterInfoPanel);
			togglePanel.setBackground(parameterInfoPanel.getBackground());
			togglePanel.setExpanded(true);

			setValues(js, formalParams, parameterInfoPanel);
			setLayout(new BorderLayout());
			add(togglePanel, BorderLayout.CENTER);

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
				CellConstraints cc = new CellConstraints();
				bottomPanel.add(addButton, cc.xy(1, 1));
				bottomPanel.add(addBeforeButton, cc.xy(2, 1));
				bottomPanel.add(deleteButton, cc.xy(3, 1));
				bottomPanel.add(moveUpButton, cc.xy(4, 1));
				bottomPanel.add(moveDownButton, cc.xy(5, 1));
				add(bottomPanel, BorderLayout.SOUTH);
			}
		}

	}

	private void setValues(JobSubmission js, ParameterInfo[] formalParams,
			ParameterInfoPanel parameterInfoPanel) {

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
			if (index == null) {
				continue;
			}
			ParameterInfo actualParam = actualParameters[index.intValue()];
			

			String value = null;
			if (formalParam.isInputFile()) {
				java.util.Map pipelineAttributes = actualParam.getAttributes();

				String taskNumber = null;
				if (pipelineAttributes != null) {
					taskNumber = (String) pipelineAttributes
							.get(PipelineModel.INHERIT_TASKNAME);
				}
				int k = index.intValue();
				if ((k < runtimePrompt.length) && (runtimePrompt[k])) {
					value = "Prompt when run";
				} else if (taskNumber != null) {
					int taskNumberInt = Integer.parseInt(taskNumber.trim());
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
							.get(taskNumberInt);
					int displayTaskNumber = taskNumberInt + 1;

					value = "Use " + inheritedOutputFileName + " from "
							+ displayTaskNumber + ". "
							+ inheritedTask.getName();
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
			parameterInfoPanel.setValue(paramName, value);
		}
	}
}
