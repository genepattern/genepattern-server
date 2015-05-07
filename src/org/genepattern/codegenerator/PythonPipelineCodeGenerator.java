/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.codegenerator;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * Generate R code to form a pipeline of tasks, complete with
 * <ul>
 * <li> server/interactive execution checking and differential support for
 * runtime prompting for missing parameters,</li>
 * <li> task existence checking,</li>
 * <li> inheritance of output files from a previous pipeline stage as input for
 * a subsequent stage,</li>
 * <li> intermediate output file results reporting,</li>
 * <li> final output file selection and download,</li>
 * <li> script creation for pipelineDesigner.jsp</li>
 * </ul>
 *
 *
 */
public class PythonPipelineCodeGenerator extends AbstractPipelineCodeGenerator implements TaskCodeGenerator {

    public PythonPipelineCodeGenerator(PipelineModel model, String server, List<TaskInfo> jobSubmissionTaskInfos) {
        super(model, server, jobSubmissionTaskInfos);
    }

    public PythonPipelineCodeGenerator(String server) {
        this.server = server;
    }

    public String emitUserInstructions() {
        return model.getName() + "." + GPConstants.TASK_TYPE_PIPELINE + " has been saved as a pipeline task on " + server + ".";
    }

    public String emitProlog() throws GenePatternException {
        Vector<String> vProblems = new Vector<String>();

        // check that all required parameters have been supplied

        int taskNum = 0;
        for (Enumeration eTasks = model.getTasks().elements(); eTasks.hasMoreElements(); taskNum++) {
            JobSubmission jobSubmission = (JobSubmission) eTasks.nextElement();
            String taskName = pythonEncode(jobSubmission.getName());

            ParameterInfo[] parameterInfo = jobSubmission.giveParameterInfoArray();

            for (int i = 0; i < parameterInfo.length; i++) {
                HashMap pia = parameterInfo[i].getAttributes();
                if (pia == null) {
                    pia = new HashMap();
                }

                if (pia.size() > 0
                        && pia.get(GPConstants.PARAM_INFO_OPTIONAL[GPConstants.PARAM_INFO_NAME_OFFSET]) == null
                        && !jobSubmission.getRuntimePrompt()[i] && parameterInfo[i].getValue().equals("")
                        && pia.get(INHERIT_TASKNAME) == null
                        && pia.get(GPConstants.PARAM_INFO_PREFIX[GPConstants.PARAM_INFO_NAME_OFFSET]) == null) {
                    vProblems.add("Missing required parameter " + parameterInfo[i].getName() + " for task " + taskName + taskNum + "\npi: " + parameterInfo[i]);
                }

            }

        }

        if (vProblems.size() > 0) {
            throw new GenePatternException("Cannot create pipeline", vProblems);
        }

        StringBuffer prolog = new StringBuffer();

        prolog.append(" # " + model.getName());
        if (model.getDescription().length() > 0) {
            prolog.append(" - ");
            prolog.append(model.getDescription());
        }
        if (model.getVersion() != null && !model.getVersion().trim().equals("")) {
            prolog.append(" - version ");
            prolog.append(model.getVersion());
        }
        prolog.append("\n # generated: ");
        prolog.append(new Date().toString());
        if (model.getAuthor() != null && !model.getAuthor().trim().equals("")) {
            prolog.append("\n # author\t");
            prolog.append(model.getAuthor());
        }
        prolog.append("\n\n");
        prolog.append("import gp\n\n");
        prolog.append("gpserver = gp.GPServer(\"" + server + "\", \"" + model.getUserID() + "\", \"INSERT_PASSWORD\")\n\n");

        return prolog.toString();
    }

    /**
     * generate Java code for a particular task to execute within the pipeline,
     * including a bit of documentation for the task. Generate file input
     * inheritance code for input from previously-run pipeline stages. At end of
     * task, generate links for downloading output files individually. Invoked
     * once for each task in the pipeline.
     *
     * @param jobSubmission
     *            description of task
     * @param taskInfo
     *            TaskInfo for this task
     * @param taskNum
     *            task number (indexed from zero)
     * @param actualParams
     *            Description of the Parameter
     * @return String generated R code
     * @exception GenePatternException
     *                Description of the Exception
     */
    public String emitTask(JobSubmission jobSubmission, TaskInfo taskInfo, ParameterInfo[] actualParams, int taskNum)
            throws GenePatternException {

        StringBuffer invocation = new StringBuffer();
        String taskVarName = pythonEncode(jobSubmission.getName() + (taskNum + 1)) + "_task";
        String jobSpecVarName = pythonEncode(jobSubmission.getName() + (taskNum + 1)) + "_job_spec";
        String jobVarName = pythonEncode(jobSubmission.getName() + (taskNum + 1)) + "_job";

        // Attach the module code
        invocation.append(taskVarName + " = ");

        invocation.append("gp.GPTask(gpserver, ");
        invocation.append("\"" + taskInfo.getLsid() + "\"");
        invocation.append(")\n\n");
        invocation.append("# Load the parameters from the GenePattern server\n");
        invocation.append(taskVarName + ".param_load()\n\n");
        invocation.append("# Create a JobSpec object for launching a job\n");
        invocation.append(jobSpecVarName + " = " + taskVarName + ".make_job_spec()\n\n");

        // Create actual parameters from formal parameters map
        HashMap paramName2ActualParam = new HashMap();
        if (actualParams != null) {
            for (int i = 0, length = actualParams.length; i < length; i++) {
                paramName2ActualParam.put(actualParams[i].getName(), actualParams[i]);
            }
        }

        // Loop through the parameters
        ParameterInfo[] formalParams = jobSubmission.giveParameterInfoArray();
        if (formalParams != null) {
            for (int i = 0; i < formalParams.length; i++) {
                ParameterInfo formal = formalParams[i];
                ParameterInfo actual = (ParameterInfo) paramName2ActualParam.get(formal.getName());

                HashMap actualAttributes = null;
                if (actual != null) {
                    actualAttributes = actual.getAttributes();
                }
                if (actualAttributes == null) {
                    actualAttributes = new HashMap();
                }

                if (jobSubmission.getRuntimePrompt()[i]) {
                    appendParameter(actual, invocation, jobSpecVarName);

                } else if (actualAttributes.get(INHERIT_FILENAME) != null) {
                    int inheritedTaskNum = Integer.parseInt((String) actualAttributes.get(INHERIT_TASKNAME));// task
                    actualAttributes.remove("TYPE");
                    actualAttributes.put(ParameterInfo.MODE, ParameterInfo.URL_INPUT_MODE);
                    String fname = (String) actualAttributes.get(INHERIT_FILENAME);// can
                    try {
                        fname = String.valueOf(Integer.parseInt(fname) - 1);// 0th
                    } catch (NumberFormatException nfe) {
                        fname = "\"" + fname + "\"";
                    }

                    TaskInfo inheritedTaskInfo = jobSubmissionTaskInfos.get(inheritedTaskNum);
                    String inheritedJobVar = pythonEncode(inheritedTaskInfo.getName() + (inheritedTaskNum + 1)) + "_job";
                    String val = inheritedJobVar + ".get_output_files()[" + fname + "].get_url()";
                    invocation.append(jobSpecVarName + ".set_parameter(\"" + actual.getName() + "\", " + val + ")\n");

                } else {
                    appendParameter(actual, invocation, jobSpecVarName);
                }

            }

        }

        invocation.append("\n" + jobVarName + " = gpserver.run_job(" + jobSpecVarName + ")\n\n");

        return invocation.toString();
    }

    public String emitEpilog() {
        return "";
    }

    private void appendParameter(ParameterInfo actual, StringBuffer invocation, String jobSpecName) {
        // Get the parameter value as needed for backward compatibility
        Map actualAttributes = actual.getAttributes();
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

        // Return the parameter setting
        invocation.append(jobSpecName + ".set_parameter(\"" + actual.getName() + "\", \"" + val + "\")\n");
    }

    public String generateTask(AnalysisJob analysisJob, ParameterInfo[] params) {
        JobInfo job = analysisJob.getJobInfo();
        String lsid = job.getTaskLSID();
        StringBuffer invocation = new StringBuffer();

        // Attach the header
        invocation.append(" # " + analysisJob.getTaskName());
        invocation.append("\n # generated: ");
        invocation.append(new Date().toString());
        invocation.append("\n\n");
        invocation.append("import gp\n\n");
        invocation.append("gpserver = gp.GPServer(\"" + server + "\", \"" + job.getUserId() + "\", \"INSERT_PASSWORD\")\n\n");

        // Attach the real working code
        invocation.append(pythonEncode(job.getTaskName()) + "_task = ");

        invocation.append("gp.GPTask(gpserver, ");
        invocation.append("\"" + lsid + "\"");
        invocation.append(")\n\n");
        invocation.append("# Load the parameters from the GenePattern server\n");
        invocation.append(pythonEncode(job.getTaskName()) + "_task.param_load()\n\n");
        invocation.append("# Create a JobSpec object for launching a job\n");
        invocation.append(pythonEncode(job.getTaskName()) + "_job_spec = " + pythonEncode(job.getTaskName()) + "_task.make_job_spec()\n\n");

        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                appendParameter(params[i], invocation, pythonEncode(job.getTaskName()) + "_job_spec");
            }
        }

        invocation.append("\n" + pythonEncode(job.getTaskName()) + "_job = gpserver.run_job(" + pythonEncode(job.getTaskName()) + "_job_spec)\n");

        return invocation.toString();
    }

    public String getLanguage() {
        return "Python";
    }

    public String getFileExtension() {
        return ".py";
    }

    protected String pythonEncode(String varName) {
        return varName.replaceAll("[^A-Za-z0-9]", "_").toLowerCase();
    }

}
