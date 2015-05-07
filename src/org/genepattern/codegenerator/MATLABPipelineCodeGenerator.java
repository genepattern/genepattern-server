/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.codegenerator;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

/**
 * Generate MATLAB code to form a pipeline of GenePatternAnalysis tasks, complete with
 * <ul>
 * <li>server/interactive execution checking and differential support for runtime prompting for missing parameters,</li>
 * <li>task existence checking,</li>
 * <li>inheritance of output files from a previous pipeline stage as input for a subsequent stage,</li>
 * <li>intermediate output file results reporting,</li>
 * <li>final output file selection and download,</li>
 * <li>script creation for pipelineDesigner.jsp</li>
 * </ul>
 * 
 * 
 * @author Ted Liefeld
 * @created Sept 26, 2004
 */
public class MATLABPipelineCodeGenerator extends AbstractPipelineCodeGenerator implements TaskCodeGenerator {
    int numPrompts = 0;

    int numNonVisualizersSeen = 0;

    java.util.Map taskNum2ResultsArrayIndex = new java.util.HashMap();

    public MATLABPipelineCodeGenerator(PipelineModel model, String server, List jobSubmissionTaskInfos) {
	super(model, server, jobSubmissionTaskInfos);
    }

    public MATLABPipelineCodeGenerator(String server) {
        this.server = server;
    }

    public String emitUserInstructions() {
	return model.getName() + "." + GPConstants.TASK_TYPE_PIPELINE + " has been saved as a pipeline task on "
		+ server + ".";
    }

    /**
     * generate the R source code that documents the pipeline, prompts for runtime parameter inputs, and offers download
     * of output results
     * 
     * @return String R code
     * @exception GenePatternException
     *                    Description of the Exception
     * @author Jim Lerner
     */
    public String emitProlog() throws GenePatternException {
	StringBuffer prolog = new StringBuffer();
	Vector vProblems = new Vector();

	prolog.append("% " + model.getName());
	if (model.getDescription().length() > 0) {
	    prolog.append(" - ");
	    prolog.append(model.getDescription());
	}
	if (model.getVersion() != null && !model.getVersion().trim().equals("")) {
	    prolog.append(" - version ");
	    prolog.append(model.getVersion());
	}
	prolog.append("\n% generated: ");
	prolog.append(new Date().toString());
	if (model.getAuthor() != null && !model.getAuthor().trim().equals("")) {
	    prolog.append("\n% @author\t");
	    prolog.append(model.getAuthor());
	}
	prolog.append("\n%");
	prolog.append("\n% To run, first download the GenePattern library from the GenePattern home page.");
	prolog.append("\n% and add it to your MATLABPATH as described in the documentation");
	prolog.append("\n% \n");

	int numNonVisualizerTasks = 0;

	// check for input parameters that must be specified at runtime
	ArrayList prompts = new ArrayList();
	int taskNum = 0;
	for (Enumeration eTasks = model.getTasks().elements(); eTasks.hasMoreElements(); taskNum++) {
	    JobSubmission jobSubmission = (JobSubmission) eTasks.nextElement();
	    String taskName = jobSubmission.getName();
	    String tName = javaEncodeName(taskName);
	    ParameterInfo[] parameterInfo = jobSubmission.giveParameterInfoArray();
	    if (!jobSubmission.isVisualizer()) {
		taskNum2ResultsArrayIndex.put(new Integer(taskNum), new Integer(numNonVisualizerTasks));
		numNonVisualizerTasks++;
	    }
	    for (int i = 0; i < parameterInfo.length; i++) {
		if (parameterInfo != null) {
		    HashMap pia = parameterInfo[i].getAttributes();
		    if (pia == null) {
			pia = new HashMap();
		    }
		    // check that all required parameters have been supplied
		    if (pia.size() > 0
			    && pia.get(GPConstants.PARAM_INFO_OPTIONAL[GPConstants.PARAM_INFO_NAME_OFFSET]) == null
			    && !jobSubmission.getRuntimePrompt()[i] && parameterInfo[i].getValue().equals("")
			    && pia.get(INHERIT_TASKNAME) == null
			    && pia.get(GPConstants.PARAM_INFO_PREFIX[GPConstants.PARAM_INFO_NAME_OFFSET]) == null) {
			vProblems.add("Missing required parameter " + parameterInfo[i].getName() + " for task "
				+ taskName + taskNum + "\npi: " + parameterInfo[i]);
		    }
		    if (jobSubmission.getRuntimePrompt()[i]) {
			prompts.add("input('Enter " + parameterInfo[i].getName().replace('.', ' ') + " for task "
				+ taskName + ": ','s')");
		    }
		}
	    }
	}

	prolog.append("gpClient =  GenePatternServer('" + server + "', '" + model.getUserID() + "');\n");

	if (prompts.size() > 0) {
	    for (int i = 0, size = prompts.size(); i < size; i++) {
		prolog.append("prompts" + i + " = " + prompts.get(i) + ";\n");
	    }
	}

	if (vProblems.size() > 0) {
	    throw new GenePatternException("Cannot create pipeline", vProblems);
	}
	if (numNonVisualizerTasks > 0) {
	    // prolog.append("\t\tJobResult[] results = new JobResult[" +
	    // numNonVisualizerTasks + "];\n");
	}
	return prolog.toString();
    }

    public String generateTask(AnalysisJob analysisJob, ParameterInfo[] params) {
	JobInfo jobInfo = analysisJob.getJobInfo();
	boolean visualizer = analysisJob.isClientJob();
	String taskName = jobInfo.getTaskName();
	String lsid = jobInfo.getTaskLSID();
	StringBuffer sb = new StringBuffer();
	sb.append("params = getMethodParameters(gpClient,'" + lsid + "');\n");
	if (params != null) {
	    for (int i = 0; i < params.length; i++) {
		sb.append("params." + matlabEncodeName(params[i].getName()) + " = '"
			+ new File(params[i].getValue()).getName() + "';\n");
	    }
	}
	if (!visualizer) {
	    sb.append("results = ");
	}
	sb.append("runAnalysis(gpClient,'" + taskName + "', params, '" + lsid + "');");
	return sb.toString();
    }

    /**
     * generate Java code for a particular task to execute within the pipeline, including a bit of documentation for the
     * task. Generate file input inheritance code for input from previously-run pipeline stages. At end of task,
     * generate links for downloading output files individually. Invoked once for each task in the pipeline.
     * 
     * @param jobSubmission
     *                description of task
     * @param taskInfo
     *                TaskInfo for this task
     * @param parameterInfo
     *                ParameterInfo array for this task
     * @param taskNum
     *                task number (indexed from zero)
     * @return String generated R code
     * @exception GenePatternException
     *                    Description of the Exception
     * @author Jim Lerner
     */
    public String emitTask(JobSubmission jobSubmission, TaskInfo taskInfo, ParameterInfo[] actualParams, int taskNum)
	    throws GenePatternException {

	StringBuffer out = new StringBuffer();
	String tName = javaEncodeName(jobSubmission.getName());
	TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
	StringBuffer invocation = new StringBuffer();
	String lsid = tia.get(GPConstants.LSID);
	String paramVar = "params" + taskNum;
	invocation.append("\n" + paramVar + " = getMethodParameters(gpClient,'" + lsid + "');\n");

	HashMap paramName2ActaulParam = new HashMap();
	if (actualParams != null) {
	    for (int i = 0, length = actualParams.length; i < length; i++) {
		paramName2ActaulParam.put(actualParams[i].getName(), actualParams[i]);
	    }
	}
	// ParameterInfo[] formalParams = taskInfo.getParameterInfoArray(); //
	// doesn't matches order of JobSubmission.getRuntimePrompt()
	ParameterInfo[] formalParams = jobSubmission.giveParameterInfoArray();

	if (formalParams != null) {
	    for (int i = 0; i < formalParams.length; i++) {
		ParameterInfo formal = formalParams[i];
		ParameterInfo actual = (ParameterInfo) paramName2ActaulParam.get(formal.getName());

		HashMap actualAttributes = null;
		if (actual != null) {
		    actualAttributes = actual.getAttributes();
		}
		if (actualAttributes == null) {
		    actualAttributes = new HashMap();
		}

		if (jobSubmission.getRuntimePrompt()[i]) {
		    invocation.append("\n" + paramVar + "." + matlabEncodeName(formal.getName()) + "= prompts"
			    + numPrompts + ";\n");
		    numPrompts++;

		} else if (actualAttributes.get(INHERIT_FILENAME) != null) {

		    int inheritedTaskNum = Integer.parseInt((String) actualAttributes.get(INHERIT_TASKNAME)); // task
		    // number
		    // of
		    // task to get
		    // output from
		    actualAttributes.remove("TYPE");
		    actualAttributes.put(ParameterInfo.MODE, ParameterInfo.URL_INPUT_MODE);

		    invocation.append(paramVar + "." + matlabEncodeName(formal.getName()) + " = ");

		    String fname = (String) actualAttributes.get(INHERIT_FILENAME); // can either by a number of
		    // stdout, stderr
		    int resultsIndex = ((Integer) (taskNum2ResultsArrayIndex.get(new Integer(inheritedTaskNum))))
			    .intValue();
		    try {
			fname = String.valueOf(Integer.parseInt(fname)); // 1st
			// index
			// is
			// 1st
			// output
		    } catch (NumberFormatException nfe) {
			fname = "\"" + fname + "\"";
		    }
		    invocation.append(" results" + resultsIndex + ".fileURLs{" + fname + "};\n");

		} else {

		    String val = null;
		    if (actual != null) {
			val = actual.getValue();
		    }
		    if (val == null) {
			if (pia != null) {
			    val = (String) actualAttributes.get((String) GPConstants.PARAM_INFO_DEFAULT_VALUE[0]);
			}
			if (val == null) {
			    val = "";
			}
		    }
		    val = val.replace('\\', '/');

		    // if this is a taskLib-based URL, convert it so that the
		    // server and
		    // port are evaluated at runtime, allowing the pipeline to
		    // be moved
		    // to another server without code changes

		    // BUG: bug 116: this makes the pipeline non-portable to
		    // systems that can't resolve the server name or the port
		    // number!!!

		    String getFile = "getFile.jsp?";
		    int index = -1;
		    if ((index = val.indexOf(getFile)) > 0) {
			String query = val.substring(index + getFile.length(), val.length());
			String[] queries = query.split("&");
			String task = null;
			String file = null;
			for (int j = 0, length = queries.length; j < length; j++) {
			    String[] temp = queries[j].split("=");
			    try {
				if (temp[0].equals("file")) {
				    file = URLDecoder.decode(temp[1], "utf-8");
				}
				if (temp[0].equals("task")) {
				    task = URLDecoder.decode(temp[1], "utf-8");
				    if ("<LSID>".equalsIgnoreCase(task)) {
					task = model.getLsid();
				    }
				}
			    } catch (UnsupportedEncodingException uee) {
				// ignore
			    }
			}
			if (task != null && file != null) {

			    // XXX - file from tasklib
			    invocation.append(paramVar + "." + matlabEncodeName(formal.getName())
				    + "= getModuleFileUrl(gpClient, '" + task + "', '" + file + "');");
			} else {
			    invocation.append(paramVar + "." + matlabEncodeName(formal.getName()) + "= '" + val
				    + "';\n");
			}
		    } else {
			invocation.append(paramVar + "." + matlabEncodeName(formal.getName()) + "= '"
				+ new File(val).getName() + "';\n");

		    }
		}
	    }

	}

	invocation.append("\n");

	// make the actual call

	if (!jobSubmission.isVisualizer()) {

	    invocation.append("results" + numNonVisualizersSeen + " = runAnalysis(gpClient,'");
	    invocation.append(jobSubmission.getName() + "'," + paramVar + ", '" + lsid + "');  \n");
	    out.append(invocation.toString() + "\n");
	    numNonVisualizersSeen++;
	} else {
	    invocation.append("runAnalysis(gpClient,'");
	    invocation.append(jobSubmission.getName() + "'," + paramVar + ", '" + lsid + "');  \n");

	    out.append("\n\t\t" + invocation.toString() + "\n");
	}

	return out.toString();
    }

    /**
     * Performs housekeeping to finish off pipeline execution. This consists of creating links for downloading some or
     * all output files from the pipeline's tasks, and running of a visualizer on the final results, if requested by the
     * user.
     * 
     * @return String code
     * @author Jim Lerner
     */
    public String emitEpilog() {

	return "";
    }

    /**
     * concrete method for AbstractPipelineCodeGenerator answering: "what language is this?"
     * 
     * @return String language (Java)
     * @author Jim Lerner
     */
    public String getLanguage() {
	return "MATLAB";
    }

    protected static String matlabEncodeName(String varName) {
	return varName.replace('.', '_');
    }

    protected static String javaEncodeName(String varName) {
	return varName.replace('.', '_').replace('-', '_').replace(' ', '_');
    }

    public String getFileExtension() {
	return ".m";
    }

}
