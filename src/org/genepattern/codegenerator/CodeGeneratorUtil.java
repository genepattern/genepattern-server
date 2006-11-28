/*******************************************************************************
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright (2003-2006) by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are
 * reserved.
 *  
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 *  
 *******************************************************************************/
package org.genepattern.codegenerator;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

public class CodeGeneratorUtil {
   
    private static Map<String, TaskCodeGenerator> languageToCodeGenerator;
    static {
        languageToCodeGenerator = new HashMap<String, TaskCodeGenerator>();
        languageToCodeGenerator.put("Java",
                new JavaPipelineCodeGenerator());
        languageToCodeGenerator.put("MATLAB",
                new MATLABPipelineCodeGenerator());
        languageToCodeGenerator.put("R", new RPipelineCodeGenerator());
    }

    private static Logger log = Logger.getLogger(CodeGeneratorUtil.class);

    public static String getJobResultFileName(JobInfo job,
            int parameterInfoIndex) {
        String fileName = job.getParameterInfoArray()[parameterInfoIndex]
                .getValue();
        int index1 = fileName.lastIndexOf('/');
        int index2 = fileName.lastIndexOf('\\');
        int index = (index1 > index2 ? index1 : index2);
        if (index != -1) {
            fileName = fileName.substring(index + 1, fileName.length());

        }
        return fileName;
    }

    public static int getJobCreationJobNumber(JobInfo job,
            int parameterInfoIndex) {
        int jobNumber = job.getJobNumber();
        String fileName = job.getParameterInfoArray()[parameterInfoIndex]
                .getValue();
        int index1 = fileName.lastIndexOf('/');
        int index2 = fileName.lastIndexOf('\\');
        int index = (index1 > index2 ? index1 : index2);
        if (index != -1) {
            jobNumber = Integer.parseInt(fileName.substring(0, index));

        }
        return jobNumber;
    }
    
    public static String getFileExtension(String language)
    throws Exception {
        TaskCodeGenerator codeGenerator = languageToCodeGenerator.get(language);
        return codeGenerator.getFileExtension();
    }

    /**
     * Generates code for a job
     * 
     * @param language
     *            the language.
     * @param job
     *            the job.
     * @param username
     *            the username.
     * @return the code.
     * @throws Exception
     *             if an error occurs while generating the code.
     */

    public static String getCode(String language, AnalysisJob job)
            throws Exception {
        TaskCodeGenerator codeGenerator = languageToCodeGenerator.get(language);
        final JobInfo jobInfo = job.getJobInfo();
        String username = job.getJobInfo().getUserId();
        LocalAdminClient adminClient = new LocalAdminClient(username);
        List<ParameterInfo> parameterInfoList = new ArrayList<ParameterInfo>();
        ParameterInfo[] params = jobInfo.getParameterInfoArray();
        for (int i = 0; i < params.length; i++) {
            if (!params[i].isOutputFile()) {
                String mode = (String) params[i].getAttributes().get(
                        ParameterInfo.MODE);
                if (mode != null
                        && mode.equals(ParameterInfo.CACHED_INPUT_MODE)) {
                    String name = getJobResultFileName(jobInfo, i);
                    int jobNumber = getJobCreationJobNumber(jobInfo, i);
                    try {
                        String url = job.getServer()
                                + System.getProperty("GP_Path")
                                + "retrieveResults.jsp?job=" + jobNumber
                                + "&filename="
                                + java.net.URLEncoder.encode(name, "UTF-8");

                        parameterInfoList.add(new ParameterInfo(params[i]
                                .getName(), url, ""));
                    } catch (UnsupportedEncodingException x) {
                        log.error(x);
                        throw new Exception("Unable to encode " + name);
                    }
                } else if (params[i].getAttributes().get("client_filename") != null) {
                    String clientFile = (String) params[i].getAttributes().get(
                            "client_filename");
                    parameterInfoList.add(new ParameterInfo(
                            params[i].getName(), clientFile, ""));
                } else {
                    parameterInfoList.add(params[i]);
                }

            }
        }

        TaskInfo taskInfo = adminClient.getTask(jobInfo.getTaskLSID());
        String code = null;
        if (taskInfo != null) { // task exists on server

            String serializedModel = (String) taskInfo.getTaskInfoAttributes()
                    .get("serializedModel");
            if (serializedModel != null && serializedModel.length() > 0) {
                Map<String, ParameterInfo> runtimePrompts = new HashMap<String, ParameterInfo>();
                for (int i = 0; i < parameterInfoList.size(); i++) {
                    ParameterInfo p = (ParameterInfo) parameterInfoList.get(i);
                    if (!p.isOutputFile()) {
                        runtimePrompts.put(p.getName(), p);
                    }
                }

                PipelineModel model = null;
                try {
                    model = PipelineModel.toPipelineModel((String) taskInfo
                            .getTaskInfoAttributes().get("serializedModel"));
                } catch (Exception e) {
                    log.error(e);
                    throw e;
                }
                List<TaskInfo> taskInfos = new ArrayList<TaskInfo>();
                List<JobSubmission> jobSubmissions = model.getTasks();
                for (int i = 0; i < jobSubmissions.size(); i++) {
                    JobSubmission js = jobSubmissions.get(i);
                    Arrays.fill(js.getRuntimePrompt(), false);
                    List<ParameterInfo> p = js.getParameters();
                    for (int j = 0; j < p.size(); j++) {
                        ParameterInfo pi = (ParameterInfo) p.get(j);
                        if (pi.getAttributes().get("runTimePrompt") != null) {
                            String key = js.getName() + (i + 1) + "."
                                    + pi.getName();
                            ParameterInfo rt = (ParameterInfo) runtimePrompts
                                    .get(key);
                            p.set(j, rt);
                        }
                    }
                    model.setLsid((String) taskInfo.getTaskInfoAttributes()
                            .get(GPConstants.LSID));
                    model.setUserID(username);

                    TaskInfo pipelineStep = adminClient.getTask(js.getLSID());

                    if (pipelineStep == null) {
                        throw new Exception("Missing task " + js.getName()
                                + ".");
                    }
                    taskInfos.add(pipelineStep);
                }
                try {
                    code = AbstractPipelineCodeGenerator.getCode(model,
                            taskInfos, System.getProperty("GenePatternURL"),
                            codeGenerator.getLanguage());
                } catch (Exception e) {
                    log.error(e);
                    throw e;
                }

            }

        }

        if (code == null) {
            code = codeGenerator.generateTask(job,
                    (ParameterInfo[]) parameterInfoList
                            .toArray(new ParameterInfo[0]));
        }
        return code;
    }
}
