package org.genepattern.server.webapp;


import java.lang.reflect.Constructor;
import java.io.IOException;
import java.io.File;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import javax.servlet.http.HttpServletRequest;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.analysis.ParameterFormatConverter;
import org.genepattern.server.analysis.ParameterInfo;
import org.genepattern.server.analysis.TaskInfo;
import org.genepattern.server.analysis.TaskInfoAttributes;
import org.genepattern.server.analysis.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.analysis.genepattern.TaskInstallationException;
import org.genepattern.server.util.OmnigeneException;
import org.genepattern.util.GPConstants;

import java.net.URL;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;


public abstract class AbstractPipelineCodeGenerator {

	protected PipelineModel model;
	protected String serverName = null;
	protected int serverPort = 0;
	protected String invokingURL = null;
	protected String baseURL = null;
	protected Collection tmTasks = null;
	public static final String INHERIT_TASKNAME = "inheritTaskname";
	public static final String INHERIT_FILENAME = "inheritFilename";
	ParameterInfo[] pia = null;
	
	public AbstractPipelineCodeGenerator(PipelineModel model, String serverName, int serverPort, String invokingURL, Collection tmTasks) {
		this.model = model;
		this.serverName = serverName;
		this.serverPort = serverPort;
		this.invokingURL = invokingURL;
		this.tmTasks = tmTasks;
		int i = invokingURL.indexOf(".jsp");
		if (i == -1) i = invokingURL.indexOf(".htm");
		if (i != -1) {
			baseURL = invokingURL.substring(0, i);
			baseURL = baseURL.substring(0, baseURL.lastIndexOf("/")+1);
		}
	}

	public String generateCode() throws Exception {
		Vector vTasks = model.getTasks();
		StringBuffer out = new StringBuffer();
		JobSubmission jobSubmission = null;
		TaskInfo taskInfo = null;
		ParameterInfo[] parameterInfo = null;
		out.append(emitProlog());
		int taskNum = 0;
		for (Enumeration eTasks = vTasks.elements(); eTasks.hasMoreElements(); taskNum++) {
			jobSubmission = (JobSubmission)eTasks.nextElement();
			try {
				try {
					taskInfo = GenePatternAnalysisTask.getTaskInfo(jobSubmission.getName(), model.getUserID());
				} catch(OmnigeneException e){
			            //this is a new task, no taskID exists
			            // do nothing
				    throw new Exception("no such task: " + jobSubmission.getName());
			        }
			        parameterInfo = jobSubmission.giveParameterInfoArray();

				// emit the code
				out.append(emitTask(jobSubmission, taskInfo, parameterInfo, taskNum));
			} catch (Exception e) {
				System.err.println("code generation for " + jobSubmission.getName() + " task failed:");
				System.err.println(e.getMessage());
				e.printStackTrace();
				System.err.println("");
				throw e;
			}
		}
		out.append(emitEpilog());
		return out.toString();
	}
	
	public String generateTask(ParameterInfo[] params) throws TaskInstallationException {
		try {
			TaskInfoAttributes tia = getTaskInfoAttributes();
			tia.put(getLanguage() + GPConstants.INVOKE, invoke());	// save invocation string in TaskInfoAttributes
			tia.put(GPConstants.CPU_TYPE, GPConstants.ANY);
			tia.put(GPConstants.OS, GPConstants.ANY);
			tia.put(GPConstants.LANGUAGE, "Java");
			tia.put(GPConstants.SERIALIZED_MODEL, model.toXML());
			tia.put(GPConstants.USERID, model.getUserID());

			String lsid = (String)tia.get(GPConstants.LSID);
			if (lsid != null && lsid.length() > 0) {
				//System.out.println("AbstractPipelineCodeGenerator.generateTask: updating " + lsid);
				lsid = GenePatternAnalysisTask.updateTask(
				    					model.getName() + "." + GPConstants.TASK_TYPE_PIPELINE, 
									"" + model.getDescription(), 
									GenePatternAnalysisTask.class.getName(), 
				    					params, tia, model.getUserID(), 
									model.isPrivate() ? GPConstants.ACCESS_PRIVATE : GPConstants.ACCESS_PUBLIC);
			} else {
				lsid = GenePatternAnalysisTask.installNewTask(
				    					model.getName() + "." + GPConstants.TASK_TYPE_PIPELINE, 
									"" + model.getDescription(), 
									GenePatternAnalysisTask.class.getName(), 
				    					params, tia, model.getUserID(), 
									model.isPrivate() ? GPConstants.ACCESS_PRIVATE : GPConstants.ACCESS_PUBLIC);
			}
			return lsid;
		} catch (TaskInstallationException tie) {
			throw tie;
		} catch (Exception e) {
			Vector vProblems = new Vector();
			vProblems.add(e.getMessage() + " while generating task " + model.getName());
			throw new TaskInstallationException(vProblems);
		}
	}
	
	public TaskInfoAttributes getCommonTaskInfoAttributes() {
		TaskInfoAttributes tia = new TaskInfoAttributes();
		tia.put(GPConstants.TASK_TYPE, GPConstants.TASK_TYPE_PIPELINE);
		tia.put(GPConstants.CLASSNAME, GenePatternAnalysisTask.class.getName());
		tia.put(GPConstants.AUTHOR, model.getAuthor());
		tia.put(GPConstants.USERID, model.getUserID());
		tia.put(GPConstants.PRIVACY, model.isPrivate() ? GPConstants.PRIVATE : GPConstants.PUBLIC);
		tia.put(GPConstants.QUALITY, GPConstants.QUALITY_DEVELOPMENT);
		tia.put(GPConstants.LSID, model.getLsid());
		return tia;
	}

	public TaskInfoAttributes getTaskInfoAttributes() {
		TaskInfoAttributes tia = getCommonTaskInfoAttributes();
 		tia.put(GPConstants.LANGUAGE, "Java");
		pia = giveParameterInfoArray();
		StringBuffer commandLine = new StringBuffer("<java> -cp <pipeline.cp>");
      
		// System properties
		commandLine.append(" -Ddecorator=edu.mit.genome.gp.ui.analysis.RunPipelineNullDecorator");
		commandLine.append(" -Domnigene.conf=<resources>");
		commandLine.append(" -Dgenepattern.properties=<resources>");
		commandLine.append(" -D" + GPConstants.LSID + "=<LSID>");

		// class to run
		commandLine.append(" edu.mit.genome.gp.ui.analysis.RunPipeline");

		// script name
		commandLine.append(" <GenePatternURL>getPipelineModel.jsp?");
		commandLine.append(GPConstants.NAME + "=<LSID>");
		commandLine.append("&" + GPConstants.USERID + "=<userid> <userid>");

		// method name within script
		//commandLine.append(" ");
		//commandLine.append(model.getName() + "." + GPConstants.TASK_TYPE_PIPELINE);

		// parameters to script
		for (int i = 0; i < pia.length; i++) {
			commandLine.append(" ");
			commandLine.append(pia[i].getName());
			commandLine.append("=<");
			commandLine.append(pia[i].getName());
			commandLine.append(">");
		}
		tia.put(GPConstants.COMMAND_LINE, commandLine.toString());
		tia.put(GPConstants.JVM_LEVEL, "1.4");
		tia.put(GPConstants.VERSION, model.getVersion());
		tia.put(GPConstants.LSID, model.getLsid());
		return tia;
	}

	/**
	 * returns a ParameterInfo[] for all of the runtime-promptable parameters in all of the tasks in the pipeline
	 *
	 * @return ParameterInfo[] of the runtime-promptable parameters in all of the tasks in the pipeline
	 * @author Jim Lerner
	 *
	 */
	public ParameterInfo[] giveParameterInfoArray() {
		if (pia == null) {
			Vector vParams = new Vector();
			int taskNum = 1;
			for (Enumeration eTasks = model.getTasks().elements(); eTasks.hasMoreElements(); taskNum++) {
				JobSubmission jobSubmission = (JobSubmission)eTasks.nextElement();
			        ParameterInfo[] parameterInfo = jobSubmission.giveParameterInfoArray();
				if (parameterInfo != null) {
				      String taskName = jobSubmission.getName().replace('-', '.').replace('_','.').replace(' ','.') + taskNum;
				      for (int i = 0; i < parameterInfo.length; i++) {
					if (jobSubmission.getRuntimePrompt()[i] && !parameterInfo[i].isOutputFile()) {
						// TODO: make ParameterInfo Cloneable and then just .clone() it
					        ParameterInfo pi = new ParameterInfo(parameterInfo[i].getName(), parameterInfo[i].getValue(), parameterInfo[i].getDescription());
						pi.setAttributes(parameterInfo[i].getAttributes());
						pi.setName(taskName + "." + pi.getName());
						vParams.add(pi);
					}
				      }
				}
			}
			pia = (ParameterInfo[])vParams.toArray(new ParameterInfo[vParams.size()]);
		}
		return pia;
	}



	public static String getCode(String pipelineName, HttpServletRequest request, String language, String userID) throws Exception {
		TaskInfo taskInfo = GenePatternAnalysisTask.getTaskInfo(pipelineName, userID);
		if (taskInfo == null) throw new Exception("No such task: " + pipelineName);
		return getCode(taskInfo, request, language, userID);
	}

	public static String getCode(TaskInfo taskInfo, HttpServletRequest request, String language, String userID) throws IOException, OmnigeneException, ClassNotFoundException, NoSuchMethodException, InstantiationException, Exception {
		Map tia = taskInfo.getTaskInfoAttributes();
		String serializedModel = (String)tia.get(GPConstants.SERIALIZED_MODEL);
		if (language.equalsIgnoreCase("xml")) {
			return serializedModel;
		}
		PipelineModel model = PipelineModel.toPipelineModel(serializedModel);
		model.setLsid((String)tia.get(GPConstants.LSID));
		Class clsPipelineCodeGenerator = Class.forName(AbstractPipelineCodeGenerator.class.getPackage().getName() + "." + language + "PipelineCodeGenerator");
		Constructor consAbstractPipelineCodeGenerator = clsPipelineCodeGenerator.getConstructor(new Class[] {PipelineModel.class, String.class, int.class, String.class, Collection.class});
		AbstractPipelineCodeGenerator codeGenerator = (AbstractPipelineCodeGenerator)consAbstractPipelineCodeGenerator.newInstance(new Object[] {model, request.getServerName(), new Integer(request.getServerPort()), System.getProperty("GenePatternURL") + "makePipeline.jsp?" + request.getQueryString(), null});
		model.setUserID(userID); // set ownership to current user
		PipelineController controller = new PipelineController(codeGenerator, model);
		String code = controller.generateCode(); // R (or some other language) code
		return code;
	}
	
	public static PipelineModel getModel(String pipelineName, String userID) throws Exception {
		TaskInfo taskInfo = GenePatternAnalysisTask.getTaskInfo(pipelineName, userID);
		if (taskInfo == null) throw new Exception("No such task: " + pipelineName);
		Map tia = taskInfo.getTaskInfoAttributes();
		String serializedModel = (String)tia.get(GPConstants.SERIALIZED_MODEL);
		PipelineModel model = PipelineModel.toPipelineModel(serializedModel);
		model.setLsid((String)tia.get(GPConstants.LSID));
		return model;
	}

	public static Collection getLanguages() {
		Vector vLanguages = new Vector();
		vLanguages.add("Java");
		vLanguages.add("MATLAB");
		vLanguages.add("R");
/*
 // TODO: make this code work under BEA WebLogic

		URL u = AbstractPipelineCodeGenerator.class.getResource("AbstractPipelineCodeGenerator.class");
		String uString = u.toString().substring("jar:file:".length());
		String file = uString.substring(0, uString.indexOf("!"));
		String pkg = uString.substring(file.length()+2, uString.lastIndexOf("/")+1);
		String suffix = "PipelineCodeGenerator.class";
		JarFile jarFile = null;
		try {
			jarFile = new JarFile(file);
			for (Enumeration entries = jarFile.entries(); entries.hasMoreElements(); ) {
				ZipEntry zipEntry = (ZipEntry)entries.nextElement();
				if (zipEntry.getName().startsWith(pkg) && zipEntry.getName().endsWith(suffix)) {
					String name = zipEntry.getName();
					name = name.substring(name.lastIndexOf("/")+1);
					name = name.substring(0, name.indexOf(suffix));
					if (name.equals("Abstract")) continue;
					if (vLanguages.contains(name)) continue;
					vLanguages.add(name);
				}
			}
		} catch (IOException ioe) {
			System.err.println(ioe + " while determining language bindings");
		} finally {
			try {
				if (jarFile != null) jarFile.close();
			} catch (IOException ioe) {
				// ignore
			}
		}
*/
		return vLanguages;
	}

	public abstract String emitTask(JobSubmission jobSubmission, TaskInfo taskInfo, ParameterInfo[] parameterInfo, int taskNum) throws GenePatternException;
	public String emitProlog() throws GenePatternException { return ""; }
	public String emitEpilog() { return ""; }
	public String emitUserInstructions() { return ""; }
	public String getBaseURL() { return baseURL; } 
	public abstract String invoke();
	public abstract String getLanguage();
}
