/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/


package org.genepattern.codegenerator;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

public abstract class AbstractPipelineCodeGenerator {

	protected PipelineModel model;

	protected String server = null;

	protected List jobSubmissionTaskInfos = null;

	public static final String INHERIT_TASKNAME = "inheritTaskname";

	public static final String INHERIT_FILENAME = "inheritFilename";

	ParameterInfo[] pia = null;

   protected AbstractPipelineCodeGenerator(){}
   
	public AbstractPipelineCodeGenerator(PipelineModel model,
			String server,
			List jobSubmissionTaskInfos) {
		this.model = model;
		this.server = server;
		this.jobSubmissionTaskInfos = jobSubmissionTaskInfos;
		
	}

	public String generateCode() throws Exception {
		Vector vTasks = model.getTasks();
		StringBuffer out = new StringBuffer();
		
		ParameterInfo[] parameterInfo = null;
		out.append(emitProlog());
		int taskNum = 0;
		for (Enumeration eTasks = vTasks.elements(); eTasks.hasMoreElements(); taskNum++) {
			JobSubmission jobSubmission = (JobSubmission) eTasks.nextElement();
			try {
				
				TaskInfo taskInfo = (TaskInfo) jobSubmissionTaskInfos.get(taskNum);
				
				parameterInfo = jobSubmission.giveParameterInfoArray();

				// emit the code
				out.append(emitTask(jobSubmission, taskInfo, parameterInfo,
						taskNum));
			} catch (Exception e) {
				System.err.println("code generation for "
						+ jobSubmission.getName() + " task failed:");
				System.err.println(e.getMessage());
				e.printStackTrace();
				System.err.println("");
				throw e;
			}
		}
		out.append(emitEpilog());
		return out.toString();
	}


	private static TaskInfoAttributes getCommonTaskInfoAttributes(PipelineModel model) {
		TaskInfoAttributes tia = new TaskInfoAttributes();
		tia.put(GPConstants.TASK_TYPE, GPConstants.TASK_TYPE_PIPELINE);
		tia.put(GPConstants.AUTHOR, model.getAuthor());
		tia.put(GPConstants.USERID, model.getUserID());
		tia.put(GPConstants.PRIVACY, model.isPrivate() ? GPConstants.PRIVATE
				: GPConstants.PUBLIC);
		tia.put(GPConstants.QUALITY, GPConstants.QUALITY_DEVELOPMENT);
		tia.put(GPConstants.LSID, model.getLsid());
		return tia;
	}

	public TaskInfoAttributes getTaskInfoAttributes() {
		return getTaskInfoAttributes(model);
	}
	
	public static TaskInfoAttributes getTaskInfoAttributes(PipelineModel model) {
		TaskInfoAttributes tia = getCommonTaskInfoAttributes(model);
		tia.put(GPConstants.LANGUAGE, "Java");
		ParameterInfo[] pia = giveParameterInfoArray(model);
		StringBuffer commandLine = new StringBuffer("<java> -cp <pipeline.cp>");

		// System properties
		commandLine
				.append(" -Ddecorator=<pipeline.decorator>");
		commandLine.append(" -Dgenepattern.properties=<resources>");
		commandLine.append(" -D" + GPConstants.LSID + "=<LSID>");

		// class to run
		commandLine.append(" <pipeline.main>");

		// script name
		commandLine.append(" <GenePatternURL>getPipelineModel.jsp?");
		commandLine.append(GPConstants.NAME + "=<LSID>");
		commandLine.append("&" + GPConstants.USERID + "=<userid> <userid>");

		// method name within script
		//commandLine.append(" ");
		//commandLine.append(model.getName() + "." +
		// GPConstants.TASK_TYPE_PIPELINE);

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
	
	public ParameterInfo[] giveParameterInfoArray() {
		if (pia == null) {
			pia = giveParameterInfoArray(model);
		}
		return pia;
	}

	/**
	 * returns a ParameterInfo[] for all of the runtime-promptable parameters in
	 * all of the tasks in the pipeline
	 * 
	 * @return ParameterInfo[] of the runtime-promptable parameters in all of
	 *         the tasks in the pipeline
	 * @author Jim Lerner
	 *  
	 */
	public static ParameterInfo[] giveParameterInfoArray(PipelineModel model) {
			Vector vParams = new Vector();
			int taskNum = 1;
			for (Enumeration eTasks = model.getTasks().elements(); eTasks
					.hasMoreElements(); taskNum++) {
				JobSubmission jobSubmission = (JobSubmission) eTasks
						.nextElement();
				ParameterInfo[] parameterInfo = jobSubmission
						.giveParameterInfoArray();
				if (parameterInfo != null) {
					String taskName = jobSubmission.getName().replace('-', '.')
							.replace('_', '.').replace(' ', '.')
							+ taskNum;
					for (int i = 0; i < parameterInfo.length; i++) {
						if (jobSubmission.getRuntimePrompt()[i]
								&& !parameterInfo[i].isOutputFile()) {
							// TODO: make ParameterInfo Cloneable and then just
							// .clone() it
							ParameterInfo pi = new ParameterInfo(
									parameterInfo[i].getName(),
									parameterInfo[i].getValue(),
									parameterInfo[i].getDescription());
							pi.setAttributes(parameterInfo[i].getAttributes());
							pi.setName(taskName + "." + pi.getName());
							vParams.add(pi);
						}
					}
				}
			}
			return  (ParameterInfo[]) vParams.toArray(new ParameterInfo[vParams
					.size()]);
		
	}


	/**
	* Gets the code for the given pipeline
	* 
	* @param model the pipeline model. The lsid and user id of the model should be set
	* @param pipelineTaskInfos a list of <tt>TaskInfo</tt> objects in the same order as the tasks in the pipeline
	* @param the server, e.g. 'http://localhost:8080'
	* @param language the language to generate the code in
	* 
	*/
	public static String getCode(PipelineModel model, List pipelineTaskInfos, String server, String language) throws Exception {
		
		Class clsPipelineCodeGenerator = Class
				.forName(AbstractPipelineCodeGenerator.class.getPackage()
						.getName()
						+ "." + language + "PipelineCodeGenerator");
		Constructor consAbstractPipelineCodeGenerator = clsPipelineCodeGenerator
				.getConstructor(new Class[] { PipelineModel.class,
						String.class, List.class });
		AbstractPipelineCodeGenerator codeGenerator = (AbstractPipelineCodeGenerator) consAbstractPipelineCodeGenerator
				.newInstance(new Object[] {
						model,
						server, pipelineTaskInfos });
		return codeGenerator.generateCode(); // R (or some other language)

	}


	public static Collection getLanguages() {
		Vector vLanguages = new Vector();
		vLanguages.add("Java");
		vLanguages.add("MATLAB");
		vLanguages.add("R");
		/*
		 * // TODO: make this code work under BEA WebLogic
		 * 
		 * URL u =
		 * AbstractPipelineCodeGenerator.class.getResource("AbstractPipelineCodeGenerator.class");
		 * String uString = u.toString().substring("jar:file:".length()); String
		 * file = uString.substring(0, uString.indexOf("!")); String pkg =
		 * uString.substring(file.length()+2, uString.lastIndexOf("/")+1);
		 * String suffix = "PipelineCodeGenerator.class"; JarFile jarFile =
		 * null; try { jarFile = new JarFile(file); for (Enumeration entries =
		 * jarFile.entries(); entries.hasMoreElements(); ) { ZipEntry zipEntry =
		 * (ZipEntry)entries.nextElement(); if
		 * (zipEntry.getName().startsWith(pkg) &&
		 * zipEntry.getName().endsWith(suffix)) { String name =
		 * zipEntry.getName(); name = name.substring(name.lastIndexOf("/")+1);
		 * name = name.substring(0, name.indexOf(suffix)); if
		 * (name.equals("Abstract")) continue; if (vLanguages.contains(name))
		 * continue; vLanguages.add(name); } } } catch (IOException ioe) {
		 * System.err.println(ioe + " while determining language bindings"); }
		 * finally { try { if (jarFile != null) jarFile.close(); } catch
		 * (IOException ioe) { // ignore } }
		 */
		return vLanguages;
	}

	public abstract String emitTask(JobSubmission jobSubmission,
			TaskInfo taskInfo, ParameterInfo[] parameterInfo, int taskNum)
			throws GenePatternException;

	public String emitProlog() throws GenePatternException {
		return "";
	}

	public String emitEpilog() {
		return "";
	}

	public String getFullServerURL() {
		String path = System.getProperty("GP_Path");	
		return path!=null?server + path + "/":server + "/gp/";
	}
	
	public String emitUserInstructions() {
		return "";
	}


	public abstract String getLanguage();
}