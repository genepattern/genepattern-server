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
public class RPipelineCodeGenerator extends AbstractPipelineCodeGenerator implements TaskCodeGenerator {

    public RPipelineCodeGenerator(PipelineModel model, String server, List<TaskInfo> jobSubmissionTaskInfos) {
        super(model, server, jobSubmissionTaskInfos);
    }

    public RPipelineCodeGenerator(String server) {
        this.server = server;
    }

    public String emitUserInstructions() {
        return model.getName() + "." + GPConstants.TASK_TYPE_PIPELINE + " has been saved as a pipeline task on "
                + server + ".";
    }

    public String emitProlog() throws GenePatternException {
        Vector<String> vProblems = new Vector<String>();

        // check that all required parameters have been supplied

        int taskNum = 0;
        for (Enumeration eTasks = model.getTasks().elements(); eTasks.hasMoreElements(); taskNum++) {
            JobSubmission jobSubmission = (JobSubmission) eTasks.nextElement();
            String taskName = REncode(jobSubmission.getName());

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
                    vProblems.add("Missing required parameter " + parameterInfo[i].getName() + " for task " + taskName
                            + taskNum + "\npi: " + parameterInfo[i]);
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
        prolog.append("\n");
        prolog.append("library(GenePattern)\n");
        prolog.append("gp <- gp.login(\"" + server + "\", \"" + model.getUserID() + "\")\n");

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
     * @author Jim Lerner
     */
    public String emitTask(JobSubmission jobSubmission, TaskInfo taskInfo, ParameterInfo[] actualParams, int taskNum)
            throws GenePatternException {

        StringBuffer out = new StringBuffer();

        StringBuffer invocation = new StringBuffer();

        if (jobSubmission.isVisualizer()) {
            invocation.append("run.visualizer(gp, \"" + jobSubmission.getLSID() + "\", ");
        } else {
            invocation.append("run.analysis(gp, \"" + jobSubmission.getLSID() + "\", ");
        }
        HashMap paramName2ActaulParam = new HashMap();
        if (actualParams != null) {
            for (int i = 0, length = actualParams.length; i < length; i++) {
                paramName2ActaulParam.put(actualParams[i].getName(), actualParams[i]);
            }
        }

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

                if (i > 0) {
                    invocation.append(", ");
                }

                if (jobSubmission.getRuntimePrompt()[i]) {
                    invocation.append(actual.getName() + "=input.prompt('" + actual.getName() + "')");

                } else if (actualAttributes.get(INHERIT_FILENAME) != null) {
                    int inheritedTaskNum = Integer.parseInt((String) actualAttributes.get(INHERIT_TASKNAME));// task
                    // number
                    // of
                    // task to get
                    // output from
                    actualAttributes.remove("TYPE");
                    actualAttributes.put(ParameterInfo.MODE, ParameterInfo.URL_INPUT_MODE);

                    String fname = (String) actualAttributes.get(INHERIT_FILENAME);// can
                    // either
                    // by a
                    // number
                    // of
                    // stdout, stderr

                    try {
                        fname = String.valueOf(Integer.parseInt(fname) - 1);// 0th
                        // index
                        // is
                        // 1st
                        // output
                    } catch (NumberFormatException nfe) {
                        fname = "\"" + fname + "\"";
                    }
                    invocation.append(formal.getName() + "=");
                    TaskInfo inheritedTaskInfo = (TaskInfo) jobSubmissionTaskInfos.get(inheritedTaskNum);
                    invocation.append("job.result.get.url(" + inheritedTaskInfo.getName() + (inheritedTaskNum + 1) + ".result,"
                            + fname + ")");

                } else {
                    appendParameter(actual, invocation);
                }

            }

        }

        invocation.append(")");
        if (!jobSubmission.isVisualizer()) {
            out.append("\n\t\t" + jobSubmission.getName() + (taskNum + 1) + ".result <- " + invocation.toString()
                    + "\n");
        } else {
            out.append("\n\t\t" + invocation.toString() + "\n");
        }

        return out.toString();
    }

    public String emitTaskFunction(JobSubmission jobSubmission, TaskInfo taskInfo, ParameterInfo[] parameterInfo) {
        return "";// wrappers are generated in an additional file
    }

    public String emitEpilog() {
        return "";
    }

    private void appendParameter(ParameterInfo actual, StringBuffer invocation) {
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
                    if (temp.length > 0 && temp[0].equals("file")) {
                        file = URLDecoder.decode(temp[1], "utf-8");
                    }
                    if (temp.length > 0 && temp[0].equals("task")) {
                        if (temp.length > 1) {
                            task = URLDecoder.decode(temp[1], "utf-8");
                            if ("<LSID>".equalsIgnoreCase(task)) {
                                task = model.getLsid();
                            }
                        }
                    }
                } catch (UnsupportedEncodingException uee) {
                    // ignore
                }
            }
            if (task != null && file != null) {
                invocation.append(actual.getName() + "=gp.get.module.file.url(gp, \"" + task + "\", \"" + file
                        + "\")");
            } else {
                invocation.append(actual.getName() + "=\"" + val + "\"");
            }
        } else {
            invocation.append(actual.getName() + "=\"" + new File(val).getName() + "\"");
        }
    }

    public String generateTask(AnalysisJob analysisJob, ParameterInfo[] params) {
        JobInfo job = analysisJob.getJobInfo();
        boolean visualizer = analysisJob.isClientJob();
        String lsid = job.getTaskLSID();
        StringBuffer invocation = new StringBuffer();

        if (!visualizer) {
            invocation.append(job.getTaskName() + ".result <- ");
        }

        if (visualizer) {
            invocation.append("run.visualizer(gp, ");
        } else {
            invocation.append("run.analysis(gp, ");
        }
        invocation.append("\"" + lsid + "\", ");

        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (i > 0) {
                    invocation.append(", ");
                }
                appendParameter(params[i], invocation);
            }
        }
        invocation.append(")");
        return invocation.toString();
    }

    public String getLanguage() {
        return "R";
    }

    public String getFileExtension() {
        return ".R";
    }

    protected String REncode(String varName) {
        // anything but letters, digits, and period is an invalid R identifier
        // that must be quoted
        if (varName.indexOf("_") == -1) {
            return varName;
        } else {
            return "\"" + varName + "\"";
        }
    }

}
