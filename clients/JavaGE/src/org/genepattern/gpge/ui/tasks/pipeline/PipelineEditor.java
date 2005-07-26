package org.genepattern.gpge.ui.tasks.pipeline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.ui.tasks.AnalysisServiceDisplay;
import org.genepattern.gpge.ui.tasks.AnalysisServiceManager;
import org.genepattern.gpge.ui.tasks.TaskLauncher;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

public class PipelineEditor {
	private TaskInfo pipelineTaskInfo;
	private PipelineModel pipelineModel;
	
	public PipelineEditor(TaskInfo taskInfo, PipelineModel m) {
		this.pipelineTaskInfo = taskInfo;
		this.pipelineModel = m;
	}
	
	private static TaskInfo cloneTaskInfo(TaskInfo taskInfo) {
		TaskInfo c = new TaskInfo(taskInfo.getID(), taskInfo.getName(),
				taskInfo.getDescription(), taskInfo.getParameterInfo(),
				(TaskInfoAttributes) taskInfo.getTaskInfoAttributes());
		return c;
	}

	private static ParameterInfo createRTPromptParameterForMap(ParameterInfo p) {
		ParameterInfo rtParam = new ParameterInfo(p.getName(), "", "");
		HashMap attrs = new HashMap();
		attrs.put(PipelineModel.RUNTIME_PARAM, "1");
		rtParam.setAttributes(attrs);
		return rtParam;
		// in PipelineModel.getInputParameters
		// {ConvertLineEndings1.input.filename maps to name=input.filename
		// value= Description Attribute:{runTimePrompt=1}}
		// in TaskInfo for pipeline [name=ConvertLineEndings1.input.filename
		// runTimePrompt=1]
	}


	private ParameterInfo createRTPromptParameterForTaskInfo(JobSubmission js,
			int index, ParameterInfo p) {
		AnalysisService svc = AnalysisServiceManager.getInstance()
		.getAnalysisService(js.getLSID());
		TaskInfo task = null;
		if(svc!=null) {
			task = svc.getTaskInfo();
		} else {
			throw new RuntimeException(); // FIXME
		}
	
		ParameterInfo[] taskInfoParams = task.getParameterInfoArray();
		ParameterInfo formalParam = null;
		for (int i = 0; i < taskInfoParams.length; i++) {
			if (taskInfoParams[i].getName().equals(p.getName())) {
				formalParam = taskInfoParams[i];
			}
		}
		ParameterInfo rtParam = new ParameterInfo(js.getName() + (index + 1)
				+ "." + p.getName(), "", formalParam.getDescription());
		HashMap attrs = new HashMap(formalParam.getAttributes());
		attrs.put(PipelineModel.RUNTIME_PARAM, "1");
		rtParam.setAttributes(attrs);
		return rtParam;
		// in PipelineModel.getInputParameters
		// {ConvertLineEndings1.input.filename maps to name=input.filename
		// value= Description Attribute:{runTimePrompt=1}}
		// in TaskInfo for pipeline [name=ConvertLineEndings1.input.filename
		// runTimePrompt=1]
	}

	private void moveTask(int from, int to) {
		if (to < from) {
			moveUp(from, to);
		} else if (to > from) {
			moveDown(from, to);
		}
	}

	private static interface ParameterItereratorCallBack {
		void param(JobSubmission js, int index, ParameterInfo p,
				Map parameterAttributes, int inheritTaskNumber);
	}

	private void iterate(int from, int to, Map inputParamsMap,
			List promptWhenRunParameters, ParameterItereratorCallBack cb) {
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
				if (js.getRuntimePrompt()[j]) {
					ParameterInfo mapParam = createRTPromptParameterForMap(p);
					ParameterInfo taskInfoParam = createRTPromptParameterForTaskInfo(
							js, i, p);
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
		final StringBuffer errors = new StringBuffer();
		if (from < to) {
			throw new IllegalArgumentException();
		}
		List currentTasks = pipelineModel.getTasks();

		JobSubmission movedTask = (JobSubmission) currentTasks.remove(from);
		currentTasks.add(to, movedTask);

		this.pipelineTaskInfo = cloneTaskInfo(this.pipelineTaskInfo);
		List promptWhenRunParameters = new ArrayList();
		TreeMap inputParamsMap = pipelineModel.getInputParameters();
		inputParamsMap.clear();

		iterate(from, currentTasks.size(), inputParamsMap,
				promptWhenRunParameters, new ParameterItereratorCallBack() {

					public void param(JobSubmission js, int index,
							ParameterInfo p, Map parameterAttributes,
							int inheritTaskNumber) {
						if (inheritTaskNumber == from) { // task inherited
							// from moved
							// task
							parameterAttributes.put(
									PipelineModel.INHERIT_TASKNAME, String
											.valueOf(to));
						} else if (inheritTaskNumber >= to
								&& inheritTaskNumber < from) { // tasks >= to
																// and < from
																// have task
																// number
																// increased by
																// 1
							parameterAttributes.put(
									PipelineModel.INHERIT_TASKNAME, String
											.valueOf(inheritTaskNumber + 1));
						}
					}

				});

		iterate(to, from, inputParamsMap, promptWhenRunParameters,
				new ParameterItereratorCallBack() {

					public void param(JobSubmission js, int index,
							ParameterInfo p, Map parameterAttributes,
							int inheritTaskNumber) {

						if (index == to && inheritTaskNumber >= to) { // if
																		// moved
																		// task
																		// inherits
																		// from
																		// a
							// task that is > to then moved
							// task loses inheritance
							parameterAttributes
									.remove(PipelineModel.INHERIT_TASKNAME);
							parameterAttributes
									.remove(PipelineModel.INHERIT_FILENAME);
							errors.append((index+1) + ". " + js.getName() + " lost input for " + AnalysisServiceDisplay.getDisplayString(p.getName()) + "\n");
						} else if (inheritTaskNumber >= to) {
							parameterAttributes.put(
									PipelineModel.INHERIT_TASKNAME, String
											.valueOf(index + 1));
						}
					}
				});

		// update prompt when run parameters
		iterate(0, to, inputParamsMap, promptWhenRunParameters,
				new ParameterItereratorCallBack() {
					public void param(JobSubmission js, int index,
							ParameterInfo p, Map parameterAttributes,
							int inheritTaskNumber) {

					}
				});
		pipelineTaskInfo
				.setParameterInfoArray((ParameterInfo[]) promptWhenRunParameters
						.toArray(new ParameterInfo[0]));
		if(errors.length() > 0) {
			GenePattern.showErrorDialog(errors.toString());
		}

	}

	public TaskInfo getTaskInfo() {
		return pipelineTaskInfo;
	}

	public PipelineModel getPipelineModel() {
		return pipelineModel;
	}

	/**
	 * Increases the position of a task in a pipeline
	 * 
	 * @param from
	 * @param to
	 */
	void moveDown(final int from, final int to) {
		List currentTasks = pipelineModel.getTasks();
		final StringBuffer errors = new StringBuffer();
		// notes: moved task can't lose inheritance, tasks that inherited from
		// moved task can lose inheritance if task is moved beyond them

		JobSubmission movedTask = (JobSubmission) currentTasks.remove(from);
		currentTasks.add(to, movedTask);

		this.pipelineTaskInfo = cloneTaskInfo(this.pipelineTaskInfo);
		List promptWhenRunParameters = new ArrayList();
		TreeMap inputParamsMap = pipelineModel.getInputParameters();
		inputParamsMap.clear();

		iterate(0, from, inputParamsMap, promptWhenRunParameters,
				new ParameterItereratorCallBack() {
					public void param(JobSubmission js, int index,
							ParameterInfo p, Map parameterAttributes,
							int inheritTaskNumber) {
						// update prompt when run parameters
					}

				});

		iterate(from, currentTasks.size(), inputParamsMap,
				promptWhenRunParameters, new ParameterItereratorCallBack() {
					public void param(JobSubmission js, int index,
							ParameterInfo p, Map parameterAttributes,
							int inheritTaskNumber) {
						if (inheritTaskNumber == from && index < to) { // task
																		// lost
																		// inheritance
							
							parameterAttributes
									.remove(PipelineModel.INHERIT_TASKNAME);
							parameterAttributes
									.remove(PipelineModel.INHERIT_FILENAME);
							errors.append((index+1) + ". " + js.getName() + " lost input for " + AnalysisServiceDisplay.getDisplayString(p.getName()) + "\n");
						} else if (inheritTaskNumber == from) {
							parameterAttributes.put(
									PipelineModel.INHERIT_TASKNAME, String
											.valueOf(to));
						} else if (inheritTaskNumber > from
								&& inheritTaskNumber <= to) { // tasks > from
																// and <= to
																// have task
																// number
																// decreased by
																// 1
							parameterAttributes.put(
									PipelineModel.INHERIT_TASKNAME, String
											.valueOf(index - 1));
						}

					}
				});
		pipelineTaskInfo
				.setParameterInfoArray((ParameterInfo[]) promptWhenRunParameters
						.toArray(new ParameterInfo[0]));
		if(errors.length()>0) {
			GenePattern.showErrorDialog(errors.toString());
		}
	}

	/**
	 * Adds a new task
	 * 
	 * @param index
	 * @param task
	 */
	void addTask(int index, TaskInfo task) {
		JobSubmission addedJob = new JobSubmission(pipelineTaskInfo.getName(),
				pipelineTaskInfo.getDescription(), (String) task
						.getTaskInfoAttributes().get(GPConstants.LSID), task
						.getParameterInfoArray(), new boolean[task
						.getParameterInfoArray().length], TaskLauncher
						.isVisualizer(task), pipelineTaskInfo);
		addTask(index, addedJob, true);
	}

	/**
	 * Inserts the task at the given index.
	 * 
	 * @param index
	 * @param jobToAdd
	 * @param updateInheritedFiles
	 * @param doLayout
	 */
	private void addTask(final int taskIndex, JobSubmission jobToAdd,
			boolean updateInheritedFiles) {
		List currentTasks = pipelineModel.getTasks();
		currentTasks.add(taskIndex, jobToAdd);
		List promptWhenRunParameters = new ArrayList();
		TreeMap inputParamsMap = pipelineModel.getInputParameters();
		inputParamsMap.clear();
		this.pipelineTaskInfo = cloneTaskInfo(this.pipelineTaskInfo);
		// check if other tasks inherit from this task or subsequent tasks
		iterate(taskIndex + 1, currentTasks.size(), inputParamsMap,
				promptWhenRunParameters, new ParameterItereratorCallBack() {

					public void param(JobSubmission js, int index,
							ParameterInfo p, Map parameterAttributes,
							int inheritTaskNumber) {
						if (inheritTaskNumber >= taskIndex) { // task inherits
															// from a task that
															// was at index or
															// later
							parameterAttributes.put(
									PipelineModel.INHERIT_TASKNAME, String
											.valueOf(inheritTaskNumber + 1)); // increase
																				// task
																				// number
																				// by
																				// one
						}

					}
				});

		iterate(0, taskIndex, inputParamsMap, promptWhenRunParameters,
				new ParameterItereratorCallBack() {

					public void param(JobSubmission js, int index,
							ParameterInfo p, Map parameterAttributes,
							int inheritTaskNumber) {

					}
				});
		pipelineTaskInfo
				.setParameterInfoArray((ParameterInfo[]) promptWhenRunParameters
						.toArray(new ParameterInfo[0]));
	}

	void print() {
		ParameterInfo[] pi = pipelineTaskInfo.getParameterInfoArray();
		if (pi != null) {
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

	void delete(final int taskIndex) {
		final StringBuffer errors = new StringBuffer();
		List currentTasks = pipelineModel.getTasks();
		List promptWhenRunParameters = new ArrayList();
		TreeMap inputParamsMap = pipelineModel.getInputParameters();
		inputParamsMap.clear();
		this.pipelineTaskInfo = cloneTaskInfo(this.pipelineTaskInfo);
		currentTasks.remove(taskIndex);
		// check if subsequent tasks inherit from removed task or subsequent
		// tasks
		iterate(taskIndex, currentTasks.size(), inputParamsMap,
				promptWhenRunParameters, new ParameterItereratorCallBack() {

					public void param(JobSubmission js, int index,
							ParameterInfo p, Map parameterAttributes,
							int inheritTaskNumber) {
						if (inheritTaskNumber == taskIndex) { // lost inheritance
							parameterAttributes
									.remove(PipelineModel.INHERIT_TASKNAME);
							parameterAttributes
									.remove(PipelineModel.INHERIT_FILENAME);
							errors.append((index+1) + ". " + js.getName() + " lost input for " + AnalysisServiceDisplay.getDisplayString(p.getName()) + "\n");
						} else if (inheritTaskNumber > taskIndex) { // inherits from
																// a task that
																// comes after
																// deleted task
							parameterAttributes.put(
									PipelineModel.INHERIT_TASKNAME, String
											.valueOf(inheritTaskNumber - 1)); // decrease
																				// task
																				// number
																				// by
																				// one
						}
					}
				});
		pipelineTaskInfo
				.setParameterInfoArray((ParameterInfo[]) promptWhenRunParameters
						.toArray(new ParameterInfo[0]));
		if(errors.length()>0) {
			GenePattern.showErrorDialog(errors.toString());
		}
	}
}