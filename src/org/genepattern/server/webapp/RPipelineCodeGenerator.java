package org.genepattern.server.webapp;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.analysis.genepattern.GenePatternAnalysisTask;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

/**
 * Generate R code to form a pipeline of GenePatternAnalysis tasks, complete with 
 * <ul>
 * <li>server/interactive execution checking and differential support for runtime prompting for missing parameters,</li>
 * <li>task existence checking,</li>
 * <li>inheritance of output files from a previous pipeline stage as input for a subsequent stage,</li>
 * <li>intermediate output file results reporting, </li>
 * <li>final output file selection and download, </li>
 * <li>script creation for pipelineDesigner.jsp</li>
 * </ul>
 *
 * @author Jim Lerner
 */
public class RPipelineCodeGenerator extends AbstractPipelineCodeGenerator {

	/** vector of tasks making up the pipeline */
	Vector vTaskNames = new Vector();
	/** array of parameters for a task */
	ParameterInfo[] pia = null;
	/** number of tasks in the pipeline */
	int numTasks = 0;

	/** create a new pipeline using the given PipelineModel, server, and URL */
	public RPipelineCodeGenerator(PipelineModel model, String serverName, int serverPort, String invokingURL, Collection tmTasks) {
		super(model, serverName, serverPort, invokingURL, tmTasks);
	}

	public String emitUserInstructions() {
		String version = "";
		try {
			version = new LSID(model.getLsid()).getVersion();
		} catch (MalformedURLException mue) {
		}
		return model.getName() + "." + GPConstants.TASK_TYPE_PIPELINE + " version " + version + " has been saved on " + serverName + ".";
	}

	/**
	 * generate the R source code that documents the pipeline, prompts for runtime parameter inputs,
	 * and offers download of output results
	 *
	 * @return String	R code
	 * @author Jim Lerner
	 */
	public String emitProlog() throws GenePatternException {
		StringBuffer prolog = new StringBuffer();
		Vector vProblems = new Vector();

		prolog.append("library(GenePattern)\n\n");
//		prolog.append("source(url(\"" + getBaseURL() + "GenePattern.R\"))\n\n");


		prolog.append("gpLogin(\"" + model.getUserID() + "\")\n\n");

		prolog.append(model.getName());
		if (!model.getName().endsWith(".pipeline")) {
			prolog.append(".pipeline");
		}
		prolog.append(" <-\n");
		prolog.append("# ");
		prolog.append(model.getName());
		if (model.getDescription().length() > 0) {
			prolog.append(" - ");
			prolog.append(model.getDescription());
		}
		if (model.getVersion() != null) {
			prolog.append(" - ");
			prolog.append(model.getVersion());
		}
		prolog.append("\n# generated: ");
		prolog.append(new Date().toString());
		prolog.append("\n# regenerate with: ");
		prolog.append(getBaseURL() + "pipelineDesigner.jsp?" + GPConstants.NAME + "=" + model.getLsid() + "&language=" + getLanguage() + "\n");
		prolog.append("# Author: ");
		prolog.append(model.getAuthor());
		prolog.append("\n# LSID: ");
		prolog.append(model.getLsid());
		prolog.append("\n\n");

		prolog.append("function(");
		
		boolean first = true;
		int taskNum = 1;
		ParameterInfo[] parameterInfo = null;
		for (Enumeration eTasks = model.getTasks().elements(); eTasks.hasMoreElements(); taskNum++) {
			numTasks++;
			JobSubmission jobSubmission = (JobSubmission)eTasks.nextElement();
			String taskName = jobSubmission.getName();
			String tName = taskName.replace('-', '.').replace('_','.').replace(' ','.');
			String paramName = null;
		        parameterInfo = jobSubmission.giveParameterInfoArray();
			if (parameterInfo != null) {
			      for (int i = 0; i < parameterInfo.length; i++) {
				HashMap pia = parameterInfo[i].getAttributes();
				if (pia == null) pia = new HashMap();

				// check that all required parameters have been supplied
				if (pia.size() > 0 && pia.get(GPConstants.PARAM_INFO_OPTIONAL[GPConstants.PARAM_INFO_NAME_OFFSET]) == null && !jobSubmission.getRuntimePrompt()[i] && 
				    parameterInfo[i].getValue().equals("") &&
				    pia.get(INHERIT_TASKNAME) == null &&
				    pia.get(GPConstants.PARAM_INFO_PREFIX[GPConstants.PARAM_INFO_NAME_OFFSET]) == null) {
					vProblems.add("Missing required parameter " + parameterInfo[i].getName() + " for task " + taskName + numTasks + "\npi: " + parameterInfo[i]);
				}
				
				if (jobSubmission.getRuntimePrompt()[i] && !parameterInfo[i].isOutputFile()) {
					if (!first) {
						prolog.append(", ");
					}
					first = false;
					paramName = tName + taskNum + "." + parameterInfo[i].getName().replace('_','.');
					prolog.append(paramName);
					String defaultValue = (String)pia.get(GPConstants.PARAM_INFO_DEFAULT_VALUE[GPConstants.PARAM_INFO_NAME_OFFSET]);
					if (defaultValue == null) defaultValue = "";
//					prolog.append("=askUser(\"" + paramName + "\", default=\"" + defaultValue + "\", TRUE)");

				}
			      }
			}
		}
		if (!first) {
			prolog.append(", ");
		}
		prolog.append("server=defaultServer)\n{\n");

		// check for input parameters that must be specified at runtime
		taskNum = 1;
		for (Enumeration eTasks = model.getTasks().elements(); eTasks.hasMoreElements(); taskNum++) {
			JobSubmission jobSubmission = (JobSubmission)eTasks.nextElement();
			String taskName = jobSubmission.getName();
			String tName = taskName.replace('-', '.').replace('_','.').replace(' ','.');
			String paramName = null;
		        parameterInfo = jobSubmission.giveParameterInfoArray();
			if (parameterInfo != null) {
			      for (int i = 0; i < parameterInfo.length; i++) {
				if (jobSubmission.getRuntimePrompt()[i] && !parameterInfo[i].isOutputFile()) {
					paramName = tName + taskNum + "." + parameterInfo[i].getName().replace('_','.');
					prolog.append("\tif (missing(" + paramName + ")) ");
					if (parameterInfo[i].isInputFile()) {
						prolog.append(paramName + " <- ");
						prolog.append("askUserFileChoice(\"Using the next dialog, please choose an input file for " + taskName + "'s " + parameterInfo[i].getName() + "\")\n");
					} else {
						prolog.append(paramName + " <- askUser(\"" + taskName + ": " + parameterInfo[i].getName().replace('.',' '));
						if (parameterInfo[i].getDescription() != null && !parameterInfo[i].getDescription().equals(paramName)) {
							prolog.append(" (" + parameterInfo[i].getDescription() + ")\")");
						}
						prolog.append("\n");
						prolog.append("	stopifnot(" + paramName + "!=\"CANCEL\")\n");
					}
//					prolog.append("	cat(\"" + paramName + "=\", " + paramName + ", \"\\n\", sep=\"\")\n");
				}
			      }
			}
		}

		// TODO: add userID parameter
		// this has to be a fully-qualified URL so that it can be emailed and still have meaning when referenced
	
		prolog.append("\n\t# verify that each task defined in the pipeline actually exists on this server\n");
		prolog.append("\tgpBeginPipeline(name=\"" + model.getName() + "\", server=server");
		for (Enumeration eTasks = model.getTasks().elements(); eTasks.hasMoreElements(); ) {
			JobSubmission jobSubmission = (JobSubmission)eTasks.nextElement();
			String taskName = jobSubmission.getName();
			prolog.append(",\n\t\t\t\"" + taskName + "\"=\"" + jobSubmission.getLSID() + "\"");
		}
		prolog.append(")\n");

		// prolog.append("\n\t#run each task and collect output\n");
		if (vProblems.size() > 0) {
			throw new GenePatternException("Cannot create pipeline", vProblems);
		}
		return prolog.toString();
	}

	/**
	 * generate R code for a particular task to execute within the pipeline, including a bit of documentation for
	 * the task.  Generate file input inheritance code for input from previously-run pipeline stages.  At end of
	 * task, generate links for downloading output files individually.  Invoked once for each task in the pipeline.
	 *
	 * @param jobSubmission		description of task
	 * @param taskInfo		TaskInfo for this task
	 * @param parameterInfo		ParameterInfo array for this task
	 * @param taskNum		task number (indexed from zero)
	 * @return String		generated R code
	 * @author Jim Lerner
	 * 
	 */
	public String emitTask(JobSubmission jobSubmission, TaskInfo taskInfo, ParameterInfo[] parameterInfo, int taskNum) throws GenePatternException {
		StringBuffer out = new StringBuffer();
		String tName = jobSubmission.getName().replace('-', '.').replace('_','.').replace(' ','.');
		vTaskNames.addElement(tName);
		out.append("\n	# " + jobSubmission.getName() + " - " + jobSubmission.getDescription());
		out.append("\n	# LSID: " + jobSubmission.getLSID());
		TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
		String author = tia.get(GPConstants.AUTHOR);
		if (author.length() > 0) {
			out.append("\n	# author: " + author + "\n");
		}
		out.append("\n");

		StringBuffer invocation = new StringBuffer();
		invocation.append(rEncode(jobSubmission.getName()) + "(");
		boolean hasServerParameter = false;
		if (parameterInfo != null) {
		      for (int i = 0; i < parameterInfo.length; i++) {
			ParameterInfo p = parameterInfo[i];
			HashMap pia = p.getAttributes();
			if (pia == null) pia = new HashMap();
			if (p.getName().equals("server")) hasServerParameter = true;
		      	if (i > 0) invocation.append(", ");
			invocation.append(rEncode(p.getName().replace('_','.')) + "=");
			if (jobSubmission.getRuntimePrompt()[i]) {
				invocation.append(tName + (taskNum+1) + "." + p.getName().replace('_','.'));
			} else if (pia.get(INHERIT_FILENAME) != null && !"NOT SET".equals((String)pia.get(INHERIT_TASKNAME))) {
				int inheritedTaskNum = Integer.parseInt((String)pia.get(INHERIT_TASKNAME));
				invocation.append("gpUseResult(");
				invocation.append(vTaskNames.elementAt(inheritedTaskNum));
				invocation.append(inheritedTaskNum+1);
				String fname = (String)pia.get(INHERIT_FILENAME);
				if (!Character.isDigit(fname.charAt(0))) {
					fname = "\"" + fname + "\"";
				}
				invocation.append(".results[" + fname + "]");
				invocation.append(")");
			} else {
				String val = p.getValue();
				if (val == null) {
					if (pia != null) {
						val = (String)pia.get((String)GPConstants.PARAM_INFO_DEFAULT_VALUE[0]);
					}
					if (val == null) {
						val = "";
					}
				}
				val = GenePatternAnalysisTask.replace(val, GPConstants.LEFT_DELIMITER + GPConstants.LSID + GPConstants.RIGHT_DELIMITER, model.getLsid());
				val = val.replace('\\', '/');

				// if this is a taskLib-based URL, convert it so that the server and 
				// port are evaluated at runtime, allowing the pipeline to be moved 
				// to another server without code changes

				// BUG: bug 116: this makes the pipeline non-portable to systems that can't resolve the server name or the port number!!!
				if (val.indexOf(System.getProperty("GenePatternURL")) == 0) {
					val = GenePatternAnalysisTask.replace(val, 
						"http://" + serverName + ":" + serverPort, 
						"paste(server, \"");
					val = val + "\", sep=\"\")";
					invocation.append(val);
				} else {
					invocation.append("\"" + val + "\"");
				}
			}
		      }
		}
/*
		if (!hasServerParameter) {
			if (parameterInfo != null) {
			      invocation.append(", ");
			}
			invocation.append("server=server");
		}
*/
		invocation.append(", LSID=\"" + jobSubmission.getLSID() + "\"");
		invocation.append(")");
		out.append("\t" + tName + (taskNum+1) + ".results <- " + invocation.toString() + "\n");

		return out.toString();
	}

	/**
	 * Performs housekeeping to finish off pipeline execution.  
	 * This consists of creating links for downloading some or all output files
	 * from the pipeline's tasks.
	 *
	 * @return String	R code
	 * @author Jim Lerner
	 */
	public String emitEpilog() {
		StringBuffer out = new StringBuffer();
		Vector vTasks = model.getTasks();
		String finalOutputFilename = ((JobSubmission)vTasks.lastElement()).getName().replace('-', '.').replace('_','.').replace(' ','.') + vTasks.size() + ".results";
		out.append("\n\t# return output as either visualization or raw files\n");
//		out.append("	if (!runningOnServer) {\n");
		// TODO: ??? should I return all result files?  And should I say that the pipeline result files are here?

//		out.append("		return (" + ((JobSubmission)vTasks.lastElement()).getName().replace('-', '.').replace('_','.').replace(' ','.') + vTasks.size() + ".results)\n");
//		out.append("	}\n");
		out.append("	return (c(");
		int taskNum = 1;
		for (Enumeration eTasks = model.getTasks().elements(); eTasks.hasMoreElements(); taskNum++) {
			JobSubmission jobSubmission = (JobSubmission)eTasks.nextElement();
			String taskName = jobSubmission.getName();
			if (taskNum > 1) out.append(", ");
			out.append(taskName + taskNum + ".results");
		}
		out.append("))\n");
		out.append("}\n");
		return out.toString();
	}
	
	/**
	 * Creates a String of R code that invokes the pipeline as an R method (like any task would in R)
	 *
	 * @return String R code that invokes the pipeline
	 * @author Jim Lerner
	 *
	 */
	public String invoke() {
		StringBuffer out = new StringBuffer();
		out.append("results <- " + rEncode(model.getName() + "." + GPConstants.TASK_TYPE_PIPELINE) + "(");
		boolean first = true;
		int taskNum = 1;
		ParameterInfo[] parameterInfo = null;
		for (Enumeration eTasks = model.getTasks().elements(); eTasks.hasMoreElements(); taskNum++) {
			JobSubmission jobSubmission = (JobSubmission)eTasks.nextElement();
			String taskName = jobSubmission.getName();
			String tName = taskName.replace('-', '.').replace('_','.').replace(' ','.');
			String paramName = null;
		        parameterInfo = jobSubmission.giveParameterInfoArray();
			if (parameterInfo != null) {
			      for (int i = 0; i < parameterInfo.length; i++) {
				if (jobSubmission.getRuntimePrompt()[i] && !parameterInfo[i].isOutputFile()) {
					if (!first) {
						out.append(", ");
					}
					first = false;
					paramName = tName + taskNum + "." + parameterInfo[i].getName().replace('_','.');
					out.append(paramName);
				}
			      }
			}
		}
/*
		if (!first) {
			out.append(", ");
		}
		out.append("server=defaultServer");
*/
		out.append(")\n");
		return out.toString();
	}

	/**
	 * concrete method for AbstractPipelineCodeGenerator answering: "what language is this?"
	 *
	 * @return String language (R)
	 * @author Jim Lerner
	 */
	public String getLanguage() {
		return "R";
	}	

	/**
	 * return an R variable or function name in quotes if it needs it, otherwise unquoted
	 * 
	 * @param varName name of R variable/method
	 * @return String same name, with quotes surrounding if necessary	
	 * @author Jim Lerner
	 */
	protected String rEncode(String varName) {
		// anything but letters, digits, and period is an invalid R identifier that must be quoted
		if (varName.indexOf("_") == -1) {
			return varName;
		} else {
			return "\"" + varName + "\"";
		}
	}
}
