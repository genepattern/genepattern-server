package org.genepattern.gpge.ui.tasks.pipeline;

import java.io.File;
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
import org.genepattern.webservice.TaskIntegratorProxy;
import org.genepattern.webservice.WebServiceException;

public class PipelineEditorModel {

	public static final int CHOOSE_TASK_INDEX = Integer.MIN_VALUE;
	
	/** list of MyTask objects */
	private List tasks;

	/** pipeline name */
	private String pipelineName;

	/** pipeline description */
	private String description;

	private String owner;

	private String author;

	private String versionComment;

	private int privacy;

	private String lsid;

	private EventListenerList listenerList;

	/** list of doc file names that have already been uploaded to the server for this task */
	private List serverDocFiles = new ArrayList();
	
	/** list of doc file names that have already been uploaded to the server for this task */
	private List localDocFiles = new ArrayList();

	private List missingJobSubmissions;
	
	/**
	 * Gets a list containing <tt>String</tt> instances of existing doc file names
	 * @return the doc files
	 */
	public List getServerDocFiles() {
		return serverDocFiles;
	}
	
	/**
	 * Gets a list containing <tt>File</tt> instances of new uploaded doc files
	 * @return the doc files
	 */
	public List getLocalDocFiles() {
		return localDocFiles;
	}
	
	public void addLocalDocFile(File file) {
		localDocFiles.add(file);
	}
	
	public void removeLocalDocFile(File file) {
		localDocFiles.remove(file);
	}
	
	public void removeServerDocFile(String s) {
		serverDocFiles.remove(s);
	}
	
	public void addPipelineListener(PipelineListener l) {
		listenerList.add(PipelineListener.class, l);
	}

	public void removePipelineListener(PipelineListener l) {
		listenerList.remove(PipelineListener.class, l);
	}

	public PipelineEditorModel() {
		tasks = new ArrayList();
		listenerList = new EventListenerList();
		this.missingJobSubmissions = new ArrayList();
	}
	
	public void resetDocFiles() {
		try {
			String[]  fileNames = new TaskIntegratorProxy(AnalysisServiceManager.getInstance()
					.getServer(), AnalysisServiceManager.getInstance()
					.getUsername()).getDocFileNames(lsid);
			serverDocFiles.clear();
			serverDocFiles.addAll(Arrays.asList(fileNames));
			localDocFiles.clear();
		} catch (WebServiceException e) {
			e.printStackTrace();
		}
	}

	public PipelineEditorModel(AnalysisService svc, PipelineModel model) {
		Map attrs = svc.getTaskInfo().getTaskInfoAttributes();
		this.lsid = (String) attrs.get(GPConstants.LSID);
		resetDocFiles();
		
		this.owner = svc.getTaskInfo().getUserId();
		this.author = model.getAuthor();
		this.privacy = svc.getTaskInfo().getAccessId();
		this.versionComment = (String) attrs.get(GPConstants.VERSION);

		this.description = model.getDescription();

		this.pipelineName = svc.getTaskInfo().getName();
		if (pipelineName.endsWith(".pipeline")) {
			pipelineName = pipelineName.substring(0, pipelineName.length()
					- ".pipeline".length());
		}
		listenerList = new EventListenerList();

		tasks = new ArrayList();
		AnalysisServiceManager asm = AnalysisServiceManager.getInstance();
		List jobSubmissions = model.getTasks();
		missingJobSubmissions = new ArrayList();
		List missingLSIDs = new ArrayList();
		for (int i = 0; i < jobSubmissions.size(); i++) {
			JobSubmission js = (JobSubmission) jobSubmissions.get(i);
			 AnalysisService formalAnalysisService = asm.getAnalysisService(js.getLSID());
			 
			if (formalAnalysisService == null) {
				if(!missingLSIDs.contains(js.getLSID())) {
					missingLSIDs.add(js.getLSID());
					missingJobSubmissions.add(js);
				}
			}
			MyTask myTask;
			if(formalAnalysisService!=null) {
				myTask = new MyTask(formalAnalysisService.getTaskInfo(), js.getDescription());
			} else {
				myTask = new MyTask(js.getName(), js.getDescription());
			}
			tasks.add(myTask);
			Map paramName2ParamIndex = new HashMap();
			List jsParams = js.getParameters();
			for (int j = 0; j < jsParams.size(); j++) {
				ParameterInfo p = (ParameterInfo) jsParams.get(j);
				paramName2ParamIndex.put(p.getName(), new Integer(j));
			}
			ParameterInfo[] formalParams = formalAnalysisService!=null?formalAnalysisService.getTaskInfo().getParameterInfoArray():null;
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
			} else {
				for (int j = 0; j < jsParams.size(); j++) {
					ParameterInfo p = (ParameterInfo) jsParams.get(j);
					myTask.addParameter(MyParameter.createMissingFormalParam(p));
				}
			}
		}
	}

	public List getMissingJobSubmissions() {
		return missingJobSubmissions;
	}
	
	public void move(int from, int to) {
		if (from < to) {
			moveDown(from, to);
		} else if (from > to) {
			moveUp(from, to);
		}
		notifyListeners(new PipelineEvent(this, PipelineEvent.MOVE, from, to));
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
	}
	
	public void replace(final int taskIndex, TaskInfo t) {
		MyTask removedTask = (MyTask) tasks.get(taskIndex);
		remove(taskIndex, false);
		add(taskIndex, t, false);
	//	MyTask addedTask = (MyTask) tasks.get(taskIndex);
		List removedParameters = removedTask.parameters;
		
		for(int i = 0; i < removedParameters.size(); i++) {
			MyParameter p = (MyParameter) removedParameters.get(i);
			for(int j = 0; j < this.getParameterCount(taskIndex); j++) {
				if(p.name.equals(getParameterName(taskIndex, j))) {
					if(p.inheritedTaskIndex!=-1) {
						setInheritedFile(taskIndex, j, p.inheritedTaskIndex, p.inheritedOutputFileName);
					} else if(p.isPromptWhenRun) {
						setPromptWhenRun(taskIndex, j);
					} else {
						setValue(taskIndex, j, p.value);
					}
					break;
				}
			}
		}
		notifyListeners(new PipelineEvent(this, PipelineEvent.REPLACE, taskIndex));
	}

	public void add(final int taskIndex, TaskInfo t) {
		add(taskIndex, t, true);
	}
	
	private void add(final int taskIndex, TaskInfo t, boolean notify) {
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
		
		tasks.add(taskIndex, myTask);
		
		if (formalParams != null) {
			for (int j = 0; j < formalParams.length; j++) {
				ParameterInfo formalParam = formalParams[j];
				MyParameter myParam = new MyParameter(formalParam);
				myTask.addParameter(myParam);
				if(myParam.isInputFile && myParam.inputTypes!=null) {
					String inheritedFileName = null;;
					int inheritedTaskIndex = -1;
					boolean unique = true;
					for(int i = 0; i < taskIndex; i++) {
						List outputs = getOutputFileTypes(i);
						for(int inputIndex = 0; inputIndex < myParam.inputTypes.length && unique; inputIndex++) {
							if(outputs.contains(myParam.inputTypes[inputIndex])) {
								if(inheritedFileName==null) {
									inheritedTaskIndex = i;
									inheritedFileName = myParam.inputTypes[inputIndex];
								} else { // inherited output is not unique
									unique = false;
									break;
								}
							}
						}
					}
					if(unique && inheritedFileName!=null) {
						setInheritedFile(taskIndex, j, inheritedTaskIndex, inheritedFileName);
					}
				}
			}
		}

		if(notify) {
			
			notifyListeners(new PipelineEvent(this, PipelineEvent.INSERT, taskIndex));
		}
	}
	
	protected void notifyListeners(PipelineEvent e) {
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == PipelineListener.class) {
				((PipelineListener) listeners[i + 1]).pipelineChanged(e);
			}
		}
	}

	public void remove(final int taskIndex) {
		remove(taskIndex, true);
	}
	
	private void remove(final int taskIndex, boolean notify) {
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
		if(notify) {
			notifyListeners(new PipelineEvent(this, PipelineEvent.DELETE, taskIndex));
		}
	}

	public TaskInfo toTaskInfo() {
		PipelineModel pipelineModel = new PipelineModel();
		pipelineModel.setName(pipelineName);
		pipelineModel.setDescription(description);
		pipelineModel.setAuthor(author);
		pipelineModel.setUserid(owner);
		final HashMap promptWhenRunAttrs = new HashMap();
		promptWhenRunAttrs.put("runTimePrompt", "1");

		
		HashMap emptyAttrs = new HashMap();
	
		List pipelineParameterInfoList = new ArrayList();
		for (int i = 0; i < getTaskCount(); i++) {
			TaskInfo taskInfo = ((MyTask) tasks.get(i)).formalTaskInfo;

			JobSubmission js = new JobSubmission(getTaskName(i),
					getTaskDescription(i), (String) taskInfo
							.getTaskInfoAttributes().get(GPConstants.LSID),
					null, null, TaskLauncher.isVisualizer(taskInfo), null);

			for (int j = 0; j < getParameterCount(i); j++) {
				String value = getValue(i, j);
				ParameterInfo p = new ParameterInfo(getParameterName(i, j), "",
						"");

				int inheritedTaskIndex = getInheritedTaskIndex(i, j);
				if (inheritedTaskIndex != -1) {
					HashMap attrs = new HashMap();
					String inheritedFile = getInheritedFile(i, j);
					if ("1st output".equals(inheritedFile)) {
						inheritedFile = "1";
					} else if ("2nd output".equals(inheritedFile)) {
						inheritedFile = "2";
					} else if ("3rd output".equals(inheritedFile)) {
						inheritedFile = "3";
					} else if ("4th output".equals(inheritedFile)) {
						inheritedFile = "4";
					}
					attrs.put(PipelineModel.INHERIT_FILENAME, inheritedFile);
					attrs.put(PipelineModel.INHERIT_TASKNAME, String
							.valueOf(inheritedTaskIndex)); // gpUseResult indices start at 1
					p.setAttributes(attrs);
				} else if (isPromptWhenRun(i, j)) {
					p.setAttributes(promptWhenRunAttrs);
					ParameterInfo pipelineParam = new ParameterInfo(
							getTaskName(i) + (i + 1) + "."
									+ getParameterName(i, j), getValue(i, j),
							"");
					HashMap attributes = new HashMap(promptWhenRunAttrs);
					attributes.putAll(getParameterAttributes(i, j));
					
					pipelineParam.setAttributes(attributes);
					pipelineParameterInfoList.add(pipelineParam);
					pipelineModel.addInputParameter(pipelineParam.getName(), p);
				} else if (isInputFile(i, j)) {
					File file = new File(value);
					if (file.exists()) {
						value = "<GenePatternURL>getFile.jsp?task=<LSID>&file="
								+ file.getName();
					} else if(value.startsWith("job #")) {
						String jobNumber = value.substring(value.indexOf("#")+1, value.indexOf(",")).trim();
						String fileName = value.substring(value.indexOf(",")+1, value.length()).trim();
						value = "<GenePatternURL>getFile.jsp?task=<LSID>&file=" + fileName;
					}
					
					p.setValue(value);
					p.setAttributes(emptyAttrs);
				} else {
					p.setValue(value);
					p.setAttributes(emptyAttrs);
				}
				js.addParameter(p);
			}

			pipelineModel.addTask(js);

		}
		TaskInfo pipelineTaskInfo = new TaskInfo();
		pipelineTaskInfo.setName(pipelineName);
		pipelineTaskInfo.setDescription(description);
		pipelineTaskInfo.setUserId(owner);
		pipelineTaskInfo.setAccessId(privacy);
		
		
		pipelineTaskInfo
				.setParameterInfoArray((ParameterInfo[]) pipelineParameterInfoList
						.toArray(new ParameterInfo[0]));
		HashMap taskInfoAttrs = new HashMap();
		taskInfoAttrs.put("version", versionComment);
		taskInfoAttrs.put("taskType", "pipeline");
		taskInfoAttrs.put("os", "any");
		taskInfoAttrs.put("cpuType", "any");
		taskInfoAttrs.put("author", author);
		StringBuffer baseCmdLine = new StringBuffer(
				"<java> -cp <pipeline.cp> -Ddecorator=<pipeline.decorator> -Dgenepattern.properties=<resources> -DLSID=<LSID> <pipeline.main> <GenePatternURL>getPipelineModel.jsp?name=<LSID>&userid=<userid> <userid>");
		for (int i = 0; i < pipelineParameterInfoList.size(); i++) {
			ParameterInfo p = (ParameterInfo) pipelineParameterInfoList.get(i);
			baseCmdLine.append(" " + p.getName() + "=<" + p.getName() + ">");
		}
		taskInfoAttrs.put("commandLine", baseCmdLine.toString());
		taskInfoAttrs.put("JVMLevel", "1.4");
		taskInfoAttrs.put("LSID", lsid);
		taskInfoAttrs.put("serializedModel", pipelineModel.toXML());
		taskInfoAttrs.put("language", "Java");
		taskInfoAttrs.put(GPConstants.USERID, owner);
		pipelineTaskInfo.setTaskInfoAttributes(taskInfoAttrs);
	//	System.out.println(taskInfoAttrs);
		//System.out.print(pipelineParameterInfoList);
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
		return task.parameters.size();
	}

	public String getTaskLSID(int taskIndex) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		TaskInfo ti = task.getTaskInfo();
		return ti !=null ? (String) ti.getTaskInfoAttributes().get(GPConstants.LSID) : "";
	}
	public String getTaskDescription(int taskIndex) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		return task.description;
	}
	
	public void setTaskDescription(int taskIndex, String s) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		task.description = s;
	}

	public List getOutputFileTypes(int taskIndex) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		return task.getOutputFileTypes();
	}

	public String[] getParameterInputTypes(int taskIndex, int parameterIndex) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		MyParameter p = task.getParameter(parameterIndex);
		return p.inputTypes;
	}
	
	public String getParameterName(int taskIndex, int parameterIndex) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		return ((MyParameter) task.parameters.get(parameterIndex)).name;
	}
	
	public HashMap getParameterAttributes(int taskIndex, int parameterIndex) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		MyParameter p = task.getParameter(parameterIndex);
		return p.attributes;
	}
	

	public int getInheritedTaskIndex(int taskIndex, int parameterIndex) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		MyParameter p = task.getParameter(parameterIndex);
		return p.inheritedTaskIndex;
	}

	public String getInheritedFile(int taskIndex, int parameterIndex) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		MyParameter p = task.getParameter(parameterIndex);
		return p.inheritedOutputFileName;
	}

	public boolean isRequired(int taskIndex, int parameterIndex) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		MyParameter p = task.getParameter(parameterIndex);
		return p.isRequired;
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

	public void setValue(int taskIndex, int parameterIndex, String value) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		MyParameter p = task.getParameter(parameterIndex);
		p.setValue(value);
	}

	public void setPromptWhenRun(int taskIndex, int parameterIndex) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		MyParameter p = task.getParameter(parameterIndex);
		p.setPromptWhenRun();
	}

	public void setInheritedFile(int taskIndex, int parameterIndex,
			int inheritedTaskIndex, String inheritedName) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		MyParameter p = task.getParameter(parameterIndex);
		p.setInheritOutput(inheritedTaskIndex, inheritedName);
	}

	public String getPipelineName() {
		return pipelineName;
	}

	public String getTaskName(int taskIndex) {
		MyTask task = (MyTask) tasks.get(taskIndex);
		return task.taskName;
	}

	private static class MyParameter {
		private boolean isPromptWhenRun;

		private final ParameterChoice[] choiceItems;

		private final String name;

		private String value;

		private int inheritedTaskIndex = -1;

		private String inheritedOutputFileName;

		private final boolean isInputFile;

		private final boolean isRequired;

		private final String[] inputTypes;

		private final HashMap attributes;
		
		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append("name: " + name);
			sb.append(", value: " + value);
			sb.append(", prompt when run: " + isPromptWhenRun);
			sb.append(", inherited index: " + inheritedTaskIndex);
			sb.append(", inherited name: " + inheritedOutputFileName);
			return sb.toString();
		}

		public void setPromptWhenRun() {
			isPromptWhenRun = true;
			value = null;
			inheritedTaskIndex = -1;
			inheritedOutputFileName = null;
		}

		public void setValue(String s) {
			value = s;
			/*
			 * if (value .startsWith("<GenePatternURL>getFile.jsp?task=<LSID>&file=")) {
			 * value = value.substring( "<GenePatternURL>getFile.jsp?task=<LSID>&file="
			 * .length(), value.length()); }
			 */
			inheritedOutputFileName = null;
			inheritedTaskIndex = -1;
			isPromptWhenRun = false;
		}

		public void setInheritOutput(int taskIndex,
				String _inheritedOutputFileName) {
			this.inheritedTaskIndex = taskIndex;
			value = null;
			isPromptWhenRun = false;
			if(_inheritedOutputFileName==null) {
				inheritedOutputFileName = _inheritedOutputFileName; // user has not selected a value
			} else if (_inheritedOutputFileName.equals("1")) {
				inheritedOutputFileName = "1st output";
			} else if (_inheritedOutputFileName.equals("2")) {
				inheritedOutputFileName = "2nd output";
			} else if (_inheritedOutputFileName.equals("3")) {
				inheritedOutputFileName = "3rd output";
			} else if (_inheritedOutputFileName.equals("4")) {
				inheritedOutputFileName = "4th output";
			} else if (_inheritedOutputFileName.equals("stdout")) {
				inheritedOutputFileName = "standard output";
			} else if (_inheritedOutputFileName.equals("stderr")) {
				inheritedOutputFileName = "standard error";
			} else {
				inheritedOutputFileName = _inheritedOutputFileName;
			}

		}

		
		private MyParameter(String name) {
			isRequired = false;
			isInputFile = false;
			choiceItems = null;
			this.name = name;
			this.inputTypes = null;
			this.attributes = new HashMap();
		}
		
		/**
		 * Creates a new instance. Use when missing formal parameter
		 * @param jobSubmissionParam ParameterInfo returned from JobSubmission
		 */
		public static MyParameter createMissingFormalParam(ParameterInfo jobSubmissionParam) {
			MyParameter p = new MyParameter(jobSubmissionParam.getName());

			java.util.Map pipelineAttributes = jobSubmissionParam
			.getAttributes();

			String taskNumberString = null;
			if (pipelineAttributes != null) {
				taskNumberString = (String) pipelineAttributes
				.get(PipelineModel.INHERIT_TASKNAME);
				if (taskNumberString != null) {
					int inheritedTaskIndex = Integer
					.parseInt(taskNumberString.trim());
					String outputFileNumber = (String) pipelineAttributes
					.get(PipelineModel.INHERIT_FILENAME);
					p.setInheritOutput(inheritedTaskIndex, outputFileNumber);

				} else {
					p.setValue(jobSubmissionParam.getValue());
				}
			}
			return p;
		}
		
		public MyParameter(ParameterInfo formalParam) {
			name = formalParam.getName();
			value = (String) formalParam.getAttributes().get(
					GPConstants.PARAM_INFO_DEFAULT_VALUE[0]);
			isInputFile = formalParam.isInputFile();
			this.attributes = formalParam.getAttributes();
			ParameterChoice[] _choiceItems = null;
			if (!isInputFile) {
				String[] choices = formalParam.getValue().split(
						GPConstants.PARAM_INFO_CHOICE_DELIMITER);
				if (choices.length > 1) {
					_choiceItems = new ParameterChoice[choices.length];
					for (int i = 0; i < choices.length; i++) {
						_choiceItems[i] = ParameterChoice
								.createChoice(choices[i]);
					}
				}
			}
			String fileFormatString = (String) formalParam.getAttributes().get("fileFormat");
			if(fileFormatString!=null) {
				inputTypes = fileFormatString.split(";");
			} else {
				inputTypes = null;
			}
			choiceItems = _choiceItems;
			String optional = (String) formalParam.getAttributes().get(
					GPConstants.PARAM_INFO_OPTIONAL[0]);
			isRequired = !"on".equalsIgnoreCase(optional);

		}

		public MyParameter(ParameterInfo formalParam, JobSubmission js,
				int indexInJobSubmission) {
			this.isInputFile = formalParam.isInputFile();
			String optional = (String) formalParam.getAttributes().get(
					GPConstants.PARAM_INFO_OPTIONAL[0]);
			isRequired = !"on".equalsIgnoreCase(optional);
			ParameterInfo jobSubmissionParam = (ParameterInfo) js
					.getParameters().get(indexInJobSubmission);
			this.name = jobSubmissionParam.getName();
			this.attributes = formalParam.getAttributes();
			ParameterChoice[] _choiceItems = null;
			String fileFormatString = (String) formalParam.getAttributes().get("fileFormat");
			if(fileFormatString!=null) {
				inputTypes = fileFormatString.split(";");
			} else {
				inputTypes = null;
			}
			
			if (formalParam.isInputFile()) {
				java.util.Map pipelineAttributes = jobSubmissionParam
						.getAttributes();

				String taskNumberString = null;
				if (pipelineAttributes != null) {
					taskNumberString = (String) pipelineAttributes
							.get(PipelineModel.INHERIT_TASKNAME);
					if (taskNumberString != null) {
						int inheritedTaskIndex = Integer
								.parseInt(taskNumberString.trim());
						String outputFileNumber = (String) pipelineAttributes
								.get(PipelineModel.INHERIT_FILENAME);
						setInheritOutput(inheritedTaskIndex, outputFileNumber);

					} else {
						setValue(jobSubmissionParam.getValue());
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
					_choiceItems = new ParameterChoice[choices.length];
					for (int i = 0; i < choices.length; i++) {
						_choiceItems[i] = ParameterChoice
								.createChoice(choices[i]);
					}
				}

			}
			boolean[] runtimePrompt = js.getRuntimePrompt();

			if ((indexInJobSubmission < runtimePrompt.length)
					&& (runtimePrompt[indexInJobSubmission])) {
				setPromptWhenRun();
			}

			choiceItems = _choiceItems;
		}
	}

	private static class MyTask {
		private List parameters;

		private TaskInfo formalTaskInfo;

		private String description;

		private String taskName;

		public MyTask(String taskName, String description) {
			this.taskName = taskName;
			this.description = description;
			parameters = new ArrayList();
		}
		
		public MyTask(TaskInfo formalTask, String description) {
			this.formalTaskInfo = formalTask;
			this.taskName = formalTaskInfo.getName();
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

	public String getPipelineDescription() {
		return description;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getLSID() {
		return lsid;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public int getPrivacy() {
		return privacy;
	}

	public void setPrivacy(int privacy) {
		this.privacy = privacy;
	}

	public String getVersionComment() {
		return versionComment;
	}

	public void setVersionComment(String versionComment) {
		this.versionComment = versionComment;
	}

	public void setPipelineDescription(String text) {
		this.description = text;
	}

	public void setPipelineName(String text) {
		this.pipelineName = text;
	}

	public void setLSID(String lsid2) {
		this.lsid = lsid2;
	}

	
}
