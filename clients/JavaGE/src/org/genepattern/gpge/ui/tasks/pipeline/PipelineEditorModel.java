package org.genepattern.gpge.ui.tasks.pipeline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.EventListenerList;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.gpge.ui.tasks.AnalysisServiceManager;
import org.genepattern.gpge.ui.tasks.ParameterChoice;
import org.genepattern.gpge.ui.tasks.TaskLauncher;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

public class PipelineEditorModel {

	/** list of MyTask objects */
	private List tasks;

	/** pipeline name */
	private String pipelineName;

	/** pipeline description */
	private String description;

	private AnalysisService pipelineAnalysisService;

	private EventListenerList listenerList;

	public void addPipelineListener(PipelineListener l) {
		listenerList.add(PipelineListener.class, l);
	}

	public void removePipelineListener(PipelineListener l) {
		listenerList.remove(PipelineListener.class, l);
	}

	public PipelineEditorModel(AnalysisService svc, PipelineModel model)
			throws JobSubmissionsNotFoundException {
		this.pipelineAnalysisService = svc;
		listenerList = new EventListenerList();
		pipelineName = model.getName();
		description = model.getDescription();

		if (pipelineName.endsWith(".pipeline")) {
			pipelineName = pipelineName.substring(0, pipelineName.length()
					- ".pipeline".length());
		}

		tasks = new ArrayList();
		AnalysisServiceManager asm = AnalysisServiceManager.getInstance();
		List jobSubmissions = model.getTasks();
		List missingTasks = new ArrayList();
		for (int i = 0; i < jobSubmissions.size(); i++) {
			JobSubmission js = (JobSubmission) jobSubmissions.get(i);
			TaskInfo formalTask = asm.getAnalysisService(js.getLSID())
					.getTaskInfo();

			if (formalTask == null) {
				missingTasks.add(js);
				continue;
			}
			MyTask myTask = new MyTask(formalTask, js.getDescription());
			tasks.add(myTask);
			Map paramName2ParamIndex = new HashMap();
			List jsParams = js.getParameters();
			for (int j = 0; j < jsParams.size(); j++) {
				ParameterInfo p = (ParameterInfo) jsParams.get(j);
				paramName2ParamIndex.put(p.getName(), new Integer(j));
			}
			ParameterInfo[] formalParams = formalTask.getParameterInfoArray();
			if (formalParams != null) {
				for (int j = 0; j < formalParams.length; j++) {
					ParameterInfo formalParam = formalParams[j];

					Integer index = (Integer) paramName2ParamIndex
							.get(formalParam.getName());
					if (index == null) {
						throw new IllegalArgumentException((i + 1) + ". "
								+ js.getName() + " is missing parameter "
								+ formalParam.getName());
					}
					int indexInJobSubmission = index.intValue();
					myTask.addParameter(new MyParameter(formalParam, js,
							indexInJobSubmission));
				}
			}
		}
		if (missingTasks.size() > 0) {
			throw new JobSubmissionsNotFoundException(missingTasks);
		}
	}

	public void move(int from, int to) {
		if (from < to) {
			moveDown(from, to);
		} else if (from > to) {
			moveUp(from, to);
		}
	}

	void moveDown(int from, int to) {
		for (int i = from + 1; i < getTaskCount(); i++) {
			MyTask task = (MyTask) tasks.get(i);
			for (int j = 0; j < getParameterCount(i); j++) {
				MyParameter p = (MyParameter) task.parameters.get(j);
				if (p.inheritedTaskIndex == from && i < to) { // task
					// lost
					// inheritance
					p.inheritedTaskIndex = -1;
					p.inheritedOutputFileName = null;
					p.value = null;
				} else if (p.inheritedTaskIndex == from) {
					p.inheritedTaskIndex = to;
				} else if (p.inheritedTaskIndex > from
						&& p.inheritedTaskIndex <= to) { // tasks > from
					// and <= to
					// have task
					// number
					// decreased by
					// 1
					p.inheritedTaskIndex--;
				}
			}
		}
		MyTask task = (MyTask) tasks.remove(from);
		tasks.add(to, task);
		notifyListeners();
	}

	void moveUp(int from, int to) {
		for (int i = from + 1; i < getTaskCount(); i++) {
			MyTask task = (MyTask) tasks.get(i);
			for (int j = 0; j < getParameterCount(i); j++) {
				MyParameter p = (MyParameter) task.parameters.get(j);
				if (p.inheritedTaskIndex == from) { // task inherits
					p.inheritedTaskIndex = to;
				} else if (p.inheritedTaskIndex >= to
						&& p.inheritedTaskIndex < from) { // tasks >= to
					p.inheritedTaskIndex++;
					// and < from
					// have task
					// number
					// increased by
					// 1

				}
			}
		}
		MyTask movedTask = (MyTask) tasks.get(from);
		for (int j = 0; j < getParameterCount(from); j++) {
			MyParameter p = (MyParameter) movedTask.parameters.get(j);
			if (p.inheritedTaskIndex >= to) {
				p.inheritedTaskIndex = -1;
				p.inheritedOutputFileName = null;
				p.value = null;
				// moved
				// task
				// inherits
				// from
				// a
				// task that is > to then moved
				// task loses inheritance
			}
		}
		for (int i = to; i < from; i++) {
			MyTask task = (MyTask) tasks.get(i);
			for (int j = 0; j < getParameterCount(i); j++) {
				MyParameter p = (MyParameter) task.parameters.get(j);
				if (p.inheritedTaskIndex >= to) {
					p.inheritedTaskIndex++;
				}
			}
		}
		MyTask task = (MyTask) tasks.remove(from);
		tasks.add(to, task);
		notifyListeners();
	}

	public void add(final int taskIndex, TaskInfo t) {
		for (int i = taskIndex + 1; i < getTaskCount(); i++) {
			MyTask task = (MyTask) tasks.get(i);
			for (int j = 0; j < getParameterCount(i); j++) {
				MyParameter p = (MyParameter) task.parameters.get(j);
				if (p.inheritedTaskIndex >= taskIndex) { // task inherits
					// from a task that
					// was at index or
					// later
					p.inheritedTaskIndex++;
					// increase
					// task
					// number
					// by
					// one
				}
			}
		}

		ParameterInfo[] formalParams = t.getParameterInfoArray();
		MyTask myTask = new MyTask(t, t.getDescription());
		if (formalParams != null) {
			for (int j = 0; j < formalParams.length; j++) {
				ParameterInfo formalParam = formalParams[j];
				myTask.addParameter(new MyParameter(formalParam));
			}
		}

		tasks.add(taskIndex, myTask);
		notifyListeners();
	}

	protected void notifyListeners() {
		Object[] listeners = listenerList.getListenerList();
		PipelineEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == PipelineListener.class) {
				// Lazily create the event:
				if (e == null) {
					e = new PipelineEvent(this);
				}

				((PipelineListener) listeners[i + 1]).pipelineChanged(e);
			}
		}
	}

	public void remove(final int taskIndex) {
		// check if subsequent tasks inherit from removed task or subsequent
		// tasks
		for (int i = taskIndex + 1; i < getTaskCount(); i++) {
			MyTask task = (MyTask) tasks.get(i);
			for (int j = 0; j < getParameterCount(i); j++) {
				MyParameter p = (MyParameter) task.parameters.get(j);
				if (p.inheritedTaskIndex == taskIndex) { // lost inheritance
					p.inheritedTaskIndex = -1;
					p.inheritedOutputFileName = null;
					p.value = null;
				} else if (p.inheritedTaskIndex > taskIndex) { // inherits
					// from
					// a task that
					// comes after
					// deleted task
					p.inheritedTaskIndex--; // decrease
					// task
					// number
					// by
					// one

				}
			}
		}
		tasks.remove(taskIndex);
		notifyListeners();
	}

	public TaskInfo getPipelineTaskInfo() {
		return pipelineAnalysisService.getTaskInfo();
	}

	public AnalysisService getPipelineAnalysisService() {
		return pipelineAnalysisService;
	}

	void print() {
		for (int i = 0; i < tasks.size(); i++) {
			System.out.println(tasks.get(i));
		}
	}

	public TaskInfo toTaskInfo() {
		PipelineModel pipelineModel = new PipelineModel();
		pipelineModel.setName(pipelineName);
		pipelineModel.setDescription(description);
		pipelineModel.setAuthor("");
		pipelineModel.setUserid("");

		List pipelineParameterInfoList = new ArrayList();
		for (int i = 0; i < getTaskCount(); i++) {
			ParameterInfo[] jsParameterInfoArray = new ParameterInfo[getParameterCount(i)];
			for (int j = 0; j < getParameterCount(i); j++) {
				ParameterInfo p = new ParameterInfo(getParameterName(i, j),
						getValue(i, j), "");
				if (isPromptWhenRun(i, j)) {
					HashMap attrs = new HashMap();
					attrs.put("runTimePrompt", "1");
					p.setAttributes(attrs);

					ParameterInfo pipelineParam = new ParameterInfo(
							getParameterName(i, j), getValue(i, j), ""); // FIXME
					pipelineParameterInfoList.add(pipelineParam);
				}
			}
			TaskInfo taskInfo = ((MyTask) tasks.get(i)).formalTaskInfo;
			JobSubmission js = new JobSubmission(getTaskName(i),
					getTaskDescription(i), (String) taskInfo
							.getTaskInfoAttributes().get(GPConstants.LSID),
					jsParameterInfoArray, null, TaskLauncher
							.isVisualizer(taskInfo), null);
			pipelineModel.addTask(js);

		}
		TaskInfo pipelineTaskInfo = new TaskInfo();
		pipelineTaskInfo.setName(pipelineName);
		pipelineTaskInfo.setDescription(description);
		pipelineTaskInfo.setUserId("");
		pipelineTaskInfo.setAccessId(GPConstants.ACCESS_PUBLIC);
		pipelineTaskInfo
				.setParameterInfoArray((ParameterInfo[]) pipelineParameterInfoList
						.toArray(new ParameterInfo[0]));
		HashMap taskInfoAttrs = new HashMap();
		taskInfoAttrs.put("version", "");
		taskInfoAttrs.put("taskType", "pipeline");
		taskInfoAttrs.put("os", "any");
		taskInfoAttrs.put("cpuType", "any");
		taskInfoAttrs
				.put(
						"commandLine",
						"<java> -cp <pipeline.cp> -Ddecorator=<pipeline.decorator> -Dgenepattern.properties=<resources> -DLSID=<LSID> <pipeline.main> <GenePatternURL>getPipelineModel.jsp?name=<LSID>&userid=<userid> <userid>");
		taskInfoAttrs.put("JVMLevel", "1.4");
		taskInfoAttrs.put("LSID", "");
		taskInfoAttrs.put("serializedModel", pipelineModel.toXML());
		taskInfoAttrs.put("language", "Java");
		pipelineTaskInfo.setTaskInfoAttributes(taskInfoAttrs);
		/**
		 * userid=gp-help@broad.mit.edu author=GenePattern
		 */
		return pipelineTaskInfo;
	}

	public int getTaskCount() {
		return tasks.size();
	}

	/**
	 * 
	 * @param taskIndex
	 * @return
	 */
	public int getParameterCount(int taskIndex) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		TaskInfo formalTask = task.getTaskInfo();
		return formalTask.getParameterInfoArray().length;
	}

	public String getTaskDescription(int taskIndex) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		return task.description;
	}

	public List getOutputFileTypes(int taskIndex) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		return task.getOutputFileTypes();
	}

	public String getParameterName(int taskIndex, int parameterIndex) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		TaskInfo formalTask = task.getTaskInfo();
		return formalTask.getParameterInfoArray()[parameterIndex].getName();
	}

	public int getInheritedTaskIndex(int taskIndex, int parameterIndex) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		MyParameter p = task.getParameter(parameterIndex);
		return p.inheritedTaskIndex;
	}

	public String getInheritedFile(int taskIndex, int parameterIndex) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		MyParameter p = task.getParameter(parameterIndex);
		return p.getUIInheritedOutputFileName();
	}

	public boolean isPromptWhenRun(int taskIndex, int parameterIndex) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		MyParameter p = task.getParameter(parameterIndex);
		return p.isPromptWhenRun;
	}

	public boolean isChoiceList(int taskIndex, int parameterIndex) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		MyParameter p = task.getParameter(parameterIndex);
		return p.choiceItems != null;
	}

	public boolean isInputFile(int taskIndex, int parameterIndex) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		MyParameter p = task.getParameter(parameterIndex);
		return p.isInputFile;
	}

	/**
	 * Gets the UI values of the choices for the specified parameter in the
	 * given task
	 * 
	 * @param taskIndex
	 * @param parameterIndex
	 * @return
	 */
	public ParameterChoice[] getChoices(int taskIndex, int parameterIndex) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		MyParameter p = task.getParameter(parameterIndex);
		return p.choiceItems;
	}

	public String getValue(int taskIndex, int parameterIndex) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		MyParameter p = task.getParameter(parameterIndex);
		return p.value;
	}

	public String getPipelineName() {
		return pipelineName;
	}

	public String getTaskName(int taskIndex) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		TaskInfo formalTask = task.getTaskInfo();
		return formalTask.getName();
	}

	private static class MyParameter {
		boolean isPromptWhenRun;

		ParameterChoice[] choiceItems;

		String name;

		String value;

		int inheritedTaskIndex = -1;

		String inheritedOutputFileName;

		boolean isInputFile;

		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append("name: " + name);
			sb.append(", value: " + value);
			sb.append(", prompt when run: " + isPromptWhenRun);
			sb.append(", inherited index: " + inheritedTaskIndex);
			sb.append(", inherited name: " + inheritedOutputFileName);
			return sb.toString();
		}

		public String getUIInheritedOutputFileName() {
			if (inheritedOutputFileName.equals("1")) {
				return "1st output";
			} else if (inheritedOutputFileName.equals("2")) {
				return "2nd output";
			} else if (inheritedOutputFileName.equals("3")) {
				return "3rd output";
			} else if (inheritedOutputFileName.equals("4")) {
				return "4th output";
			} else if (inheritedOutputFileName.equals("stdout")) {
				return "standard output";
			} else if (inheritedOutputFileName.equals("stderr")) {
				return "standard error";
			} else {
				return inheritedOutputFileName;
			}
		}

		public ParameterInfo createJobSubmissionParameter() {
			return null;
		}

		public MyParameter(ParameterInfo formalParam) {
			name = formalParam.getName();
			value = (String) formalParam.getAttributes().get(
					ParameterInfo.DEFAULT);
			isInputFile = formalParam.isInputFile();
			String[] choices = formalParam.getValue().split(
					GPConstants.PARAM_INFO_CHOICE_DELIMITER);
			if (choices.length > 1) {
				choiceItems = new ParameterChoice[choices.length];
				for (int i = 0; i < choices.length; i++) {
					choiceItems[i] = ParameterChoice.createChoice(choices[i]);
				}
			}
		}

		public MyParameter(ParameterInfo formalParam, JobSubmission js,
				int indexInJobSubmission) {
			this.isInputFile = formalParam.isInputFile();
			ParameterInfo jobSubmissionParam = (ParameterInfo) js
					.getParameters().get(indexInJobSubmission);
			this.name = jobSubmissionParam.getName();
			if (indexInJobSubmission == -1) {
				/*
				 * value = (String) formalParam.getAttributes().get(
				 * GPConstants.PARAM_INFO_DEFAULT_VALUE[0]); if (value == null) {
				 * value = ""; } js.getParameters().add(new ParameterInfo(name,
				 * value, "")); boolean[] newRuntimePrompt = new
				 * boolean[runtimePrompt.length + 1]; for (int k = 0; k <
				 * runtimePrompt.length; k++) { newRuntimePrompt[k] =
				 * runtimePrompt[k]; } newRuntimePrompt[newRuntimePrompt.length -
				 * 1] = false; js.setRuntimePrompt(newRuntimePrompt);
				 */
			} else {

				boolean[] runtimePrompt = js.getRuntimePrompt();
				if (formalParam.isInputFile()) {
					java.util.Map pipelineAttributes = jobSubmissionParam
							.getAttributes();

					String taskNumberString = null;
					if (pipelineAttributes != null) {
						taskNumberString = (String) pipelineAttributes
								.get(PipelineModel.INHERIT_TASKNAME);
					}
					if ((indexInJobSubmission < runtimePrompt.length)
							&& (runtimePrompt[indexInJobSubmission])) {
						isPromptWhenRun = true;
						value = "Prompt when run";
					} else if (taskNumberString != null) {
						inheritedTaskIndex = Integer.parseInt(taskNumberString
								.trim());
						String outputFileNumber = (String) pipelineAttributes
								.get(PipelineModel.INHERIT_FILENAME);

						inheritedOutputFileName = outputFileNumber;

					} else {
						value = jobSubmissionParam.getValue();
						if (value
								.startsWith("<GenePatternURL>getFile.jsp?task=<LSID>&file=")) {
							value = value.substring(
									"<GenePatternURL>getFile.jsp?task=<LSID>&file="
											.length(), value.length());
						}
					}

				} else {
					value = jobSubmissionParam.getValue(); // can be command
					// line value
					// can be command line value
					// instead of UI value
					String[] choices = formalParam.getValue().split(
							GPConstants.PARAM_INFO_CHOICE_DELIMITER);
					if (choices.length > 1) {
						choiceItems = new ParameterChoice[choices.length];
						for (int i = 0; i < choices.length; i++) {
							choiceItems[i] = ParameterChoice
									.createChoice(choices[i]);
						}
					}

				}
			}
		}
	}

	private static class MyTask {
		private List parameters;

		private TaskInfo formalTaskInfo;

		private String description;

		public MyTask(TaskInfo formalTask, String description) {
			this.formalTaskInfo = formalTask;
			this.description = description;
			parameters = new ArrayList();
		}

		public TaskInfo getTaskInfo() {
			return formalTaskInfo;
		}

		public List getOutputFileTypes() {
			String fileFormat = (String) formalTaskInfo.getTaskInfoAttributes()
					.get("fileFormat");
			if (fileFormat == null) {
				return Collections.EMPTY_LIST;
			}
			List outputs = Arrays.asList(fileFormat.split(";"));
			Collections.sort(outputs, String.CASE_INSENSITIVE_ORDER);
			return outputs;
		}

		public void addParameter(MyParameter p) {
			parameters.add(p);
		}

		public MyParameter getParameter(int index) {
			return (MyParameter) parameters.get(index);
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < parameters.size(); i++) {
				sb.append(parameters.get(i).toString() + "\n");
			}
			return sb.toString();
		}

	}

}
