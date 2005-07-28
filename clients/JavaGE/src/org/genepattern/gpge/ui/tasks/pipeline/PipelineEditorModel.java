package org.genepattern.gpge.ui.tasks.pipeline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.gpge.ui.tasks.AnalysisServiceManager;
import org.genepattern.gpge.ui.tasks.ParameterChoice;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

public class PipelineEditorModel {

	/** list of MyTask objects*/
	private List tasks;

	/** pipeline name */
	private String pipelineName;

	/** pipeline description */
	private String description;

	public PipelineEditorModel(TaskInfo _pipelineTaskInfo, PipelineModel model) {

		pipelineName = model.getName();
		description = model.getDescription();

		if (pipelineName.endsWith(".pipeline")) {
			pipelineName = pipelineName.substring(0, pipelineName.length()
					- ".pipeline".length());
		}

		tasks = new ArrayList();
		AnalysisServiceManager asm = AnalysisServiceManager.getInstance();
		List jobSubmissions = model.getTasks();
		for (int i = 0; i < jobSubmissions.size(); i++) {
			JobSubmission js = (JobSubmission) jobSubmissions.get(i);
			TaskInfo formalTask = asm.getAnalysisService(js.getLSID())
					.getTaskInfo();

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
						throw new IllegalArgumentException("Missing parameter "
								+ formalParam.getName());
					}
					int indexInJobSubmission = index.intValue();
					myTask.addParameter(new MyParameter(formalParam, js,
							indexInJobSubmission));
				}
			}

		}
	}

	
	public void delete(int index) {
	}

	public void move(int from, int to) {
	}

	void print() {
		for (int i = 0; i < tasks.size(); i++) {
			System.out.println(tasks.get(i));
		}
	}

	public void addTask(int index, TaskInfo taskInfo) {
	}
	
	public String toXML() {
		return null;
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
			sb.append("name:" + name);
			sb.append("value:" + value);
			sb.append("prompt when run:" + isPromptWhenRun);
			sb.append("inherited index:" + inheritedTaskIndex);
			sb.append("inherited name:" + inheritedOutputFileName);
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
			List outputs = Arrays.asList(((String) formalTaskInfo
					.getTaskInfoAttributes().get("fileFormat")).split(";"));
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
