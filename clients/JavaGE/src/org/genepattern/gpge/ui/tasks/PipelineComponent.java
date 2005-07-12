package org.genepattern.gpge.ui.tasks;

import java.awt.BorderLayout;
import java.awt.Color;
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
	private TaskInfoAttributes pipelineTaskInfoAttributes;

	private PipelineModel pipelineModel;

	private List jobSubmissions;

	private String userID;

	private TaskInfo pipelineTaskInfo;

	public PipelineComponent() {
		setBackground(Color.white);
	}
	
	private static TaskInfo cloneTaskInfo(TaskInfo taskInfo) {
		TaskInfo c = new TaskInfo(taskInfo.getID(), taskInfo.getName(), taskInfo.getDescription(),
				taskInfo.getParameterInfo(), (TaskInfoAttributes) taskInfo.getTaskInfoAttributes());
		return c;
	}
	
	private static ParameterInfo createRTPromptParameter(ParameterInfo p) {
		ParameterInfo rtParam = new ParameterInfo(p.getName(), "", p.getDescription());
		HashMap attrs = new HashMap();
		attrs.put(PipelineModel.RUNTIME_PARAM, "1");
		rtParam.setAttributes(attrs);
		return rtParam;
	}
	
	
	public void moveUp(int index) {
		JobSubmission js = (JobSubmission) pipelineModel.getTasks().get(index);
		delete(index, false);
		addTask(index - 1, js, false, false);
		// moved from index to index - 1
		List parameterInfo = js.getParameters();
		for (int i = 0; i < parameterInfo.size(); i++) { // update inherited files
			ParameterInfo p = (ParameterInfo) parameterInfo.get(i);
			Map parameterAttributes = p.getAttributes();
			if (parameterAttributes != null) {
				String taskNumberString = (String) parameterAttributes
						.get(PipelineModel.INHERIT_TASKNAME);
				if (taskNumberString != null) { // only can lose inheritance if the task inherits from the one before
					int taskNumber = Integer.parseInt(taskNumberString);// 
					if(taskNumber==(index-1)) { // lost inheritance
						parameterAttributes
						.remove(PipelineModel.INHERIT_TASKNAME);
						parameterAttributes
						.remove(PipelineModel.INHERIT_FILENAME);
						System.out
						.println("lost inheritance " + taskNumber);
					} 
				}
			}
		}
		setPipeline(pipelineTaskInfo, pipelineModel);
	}
	
	public void moveDown(int index) {
		
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
		for (int i = (index+1); i < currentTasks.size(); i++) {
			JobSubmission js = (JobSubmission) currentTasks.get(i);
			List parameterInfo = js.getParameters();
		
			for (int j = 0; j < parameterInfo.size(); j++) {
				ParameterInfo p = (ParameterInfo) parameterInfo.get(j);
				Map parameterAttributes = p.getAttributes();
				if (updateInheritedFiles && parameterAttributes != null) {
					String taskNumberString = (String) parameterAttributes
							.get(PipelineModel.INHERIT_TASKNAME);
					
					if (taskNumberString != null) {
						int taskNumber = Integer.parseInt(taskNumberString);
						if (taskNumber >= index) { // task inherits from a task that was at index or later
							parameterAttributes.put(
									PipelineModel.INHERIT_TASKNAME, String
											.valueOf(taskNumber+1)); // increase task number by one 
						}
					} 
				}
				if(js.getRuntimePrompt()[j]){
					ParameterInfo rtParam = createRTPromptParameter(p);
					promptWhenRunParameters.add(rtParam);
					inputParamsMap.put(js.getName() + (i+1) + "." + rtParam.getName(), rtParam);
				}
			}
		}
		
		
		if (pipelineTaskInfo.getParameterInfoArray() != null) {
			for (int i = 0; i < index; i++) {
				JobSubmission js = (JobSubmission) currentTasks.get(i);
				List parameterInfo = js.getParameters();
			
				for (int j = 0; j < parameterInfo.size(); j++) {
					ParameterInfo p = (ParameterInfo) parameterInfo.get(j);
					if(js.getRuntimePrompt()[j]){
						ParameterInfo rtParam = createRTPromptParameter(p);
						promptWhenRunParameters.add(rtParam);
						inputParamsMap.put(js.getName() + (i+1) + "." + rtParam.getName(), rtParam);
					}
				}
				
			}
			pipelineTaskInfo.setParameterInfoArray((ParameterInfo[]) promptWhenRunParameters.toArray(new ParameterInfo[0]));
			
		}
		if(doLayout) {
			setPipeline(pipelineTaskInfo, pipelineModel);
		}
	}
	
	void print() {
		ParameterInfo[] pi = pipelineTaskInfo.getParameterInfoArray();
		if(pi!=null)
		System.out.println(Arrays.asList(pi));
		List currentTasks = pipelineModel.getTasks();
		for (int i = 0; i < currentTasks.size(); i++) {
			JobSubmission js = (JobSubmission) currentTasks.get(i);
			List parameterInfo = js.getParameters();
			System.out.print("task number" + i);
			System.out.println(parameterInfo);
		}
		System.out.println();
	}

	public void delete(int index, boolean doLayout) {
		List currentTasks = pipelineModel.getTasks();
		TaskInfo pipelineTaskInfo = cloneTaskInfo(this.pipelineTaskInfo);
		JobSubmission removedJob = (JobSubmission) currentTasks.remove(index);
		// check if subsequent tasks inherit from removed task or subsequent tasks
		for (int i = index; i < currentTasks.size(); i++) {
			JobSubmission js = (JobSubmission) currentTasks.get(i);
			List parameterInfo = js.getParameters();
			for (int j = 0; j < parameterInfo.size(); j++) {
				ParameterInfo p = (ParameterInfo) parameterInfo.get(j);
				Map parameterAttributes = p.getAttributes();
				if (parameterAttributes != null) {
					String taskNumberString = (String) parameterAttributes
							.get(PipelineModel.INHERIT_TASKNAME);
					if (taskNumberString != null) {
						int taskNumber = Integer.parseInt(taskNumberString);
						if (taskNumber == index) { // lost inheritance
							parameterAttributes
									.remove(PipelineModel.INHERIT_TASKNAME);
							parameterAttributes
									.remove(PipelineModel.INHERIT_FILENAME);
							System.out
									.println("lost inheritance " + taskNumber);
						} else if (taskNumber > index) { // inherits from a task that comes after deleted task
							parameterAttributes.put(
									PipelineModel.INHERIT_TASKNAME, String
											.valueOf(taskNumber-1)); // decrease task number by one
						}
					}
				}
			}
		}
		
		String removedJobName = removedJob.getName();
		// remove parameters that start with removed task name + removed task number + .
		TreeMap currentInputParameters = pipelineModel.getInputParameters();
		String removeParameter = removedJobName + (index+1) + ".";
		for (Iterator it = currentInputParameters.keySet().iterator(); it
				.hasNext();) {
			String key = (String) it.next();
			if (key.startsWith(removeParameter)) {
				currentInputParameters.remove(key);
			}
		}
		if (pipelineTaskInfo.getParameterInfoArray() != null) {
			List parameters = new ArrayList(Arrays.asList(pipelineTaskInfo
					.getParameterInfoArray()));
			for (int i = 0; i < parameters.size(); i++) {
				ParameterInfo p = (ParameterInfo) parameters.get(i);
				if (p.getName().startsWith(removeParameter)) {
					parameters.remove(i);
					i--;
				}
			}
			pipelineTaskInfo.setParameterInfoArray((ParameterInfo[]) parameters
					.toArray(new ParameterInfo[0]));
		}

		if(doLayout) {
			setPipeline(pipelineTaskInfo, pipelineModel);
		}
	}

	public void setTaskInfo(TaskInfo info) {

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

	public void setPipeline(TaskInfo pipelineTaskInfo, PipelineModel model) {
		
		removeAll();

		this.pipelineTaskInfo = pipelineTaskInfo;
		this.pipelineModel = model;
		this.pipelineTaskInfoAttributes = pipelineTaskInfo
				.giveTaskInfoAttributes();
		this.jobSubmissions = pipelineModel.getTasks();
		this.userID = pipelineTaskInfo.getUserId();
		
		print();
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

	protected TaskInfo getTaskInfo(String lsid, String userID) {
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
			TaskInfo formalTaskInfo = getTaskInfo(js.getLSID(), userID);
			ParameterInfo[] formalParams = formalTaskInfo != null ? formalTaskInfo
					.getParameterInfoArray()
					: null;
			if (formalParams == null) {
				formalParams = new ParameterInfo[0];
			}
			int maxLabelWidth = 0;
			ParameterInfoPanel parameterInfoPanel = new ParameterInfoPanel(js
					.getName(), formalParams);
			parameterInfoPanel.setUseInputFromPreviousTask(0,
					new String[] { "a" }, new String[] { "b" });
			maxLabelWidth = Math.max(maxLabelWidth, parameterInfoPanel
					.getLabelWidth());
			JTextField description = new JTextField(js.getDescription(), 80);

			JButton docBtn = new JButton("Documentation");
			docBtn.setBackground(getBackground());

			TogglePanel togglePanel = new TogglePanel((index + 1) + ". "
					+ formalTaskInfo.getName(), description, parameterInfoPanel);
			togglePanel.setBackground(parameterInfoPanel.getBackground());
			togglePanel.setExpanded(true);

			setValues(js, formalParams, parameterInfoPanel);
			setLayout(new BorderLayout());
			add(togglePanel, BorderLayout.CENTER);

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
					if(source==deleteButton) {
						delete(index, true);
					} else if(source==addButton) {
						AnalysisService temp = AnalysisServiceManager.getInstance().getAnalysisService("ConvertLineEndings");
						addTask(index+1, temp.getTaskInfo());
					} else if(source==moveUpButton) {
						moveUp(index);
					} else if(source==moveDownButton) {
						moveDown(index);
					} else if(source==addBeforeButton) {
						AnalysisService temp = AnalysisServiceManager.getInstance().getAnalysisService("ConvertLineEndings");
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
			ParameterInfo actualParam = null;
			if (index != null) {
				actualParam = actualParameters[index.intValue()];
			}

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
					String outputFileNumber = (String) pipelineAttributes
							.get(PipelineModel.INHERIT_FILENAME);
					int taskNumberInt = Integer.parseInt(taskNumber.trim());
					String inheritedOutputFileName = null;
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

					value = "Use " + inheritedOutputFileName + "from "
							+ displayTaskNumber + ". "
							+ inheritedTask.getName();
				} else {
					value = actualParam.getValue();

					try {
						new java.net.URL(value);// see if parameter is a URL
					} catch (java.net.MalformedURLException x) {

					}
				}

			} else {
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
			parameterInfoPanel.setValue(paramName, value);
		}
	}
}
