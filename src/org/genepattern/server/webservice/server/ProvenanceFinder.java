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

package org.genepattern.server.webservice.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.webapp.PipelineController;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.WebServiceException;

public class ProvenanceFinder {
    private static String serverURL = null;

    private String userID = null;

    private LocalAnalysisClient service = null;

    private ArrayList filesToCopy = new ArrayList();

    static {
        try {
            serverURL = "http://" + InetAddress.getLocalHost().getCanonicalHostName() + ":"
                    + System.getProperty("GENEPATTERN_PORT");
            serverURL = serverURL.toUpperCase();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public ProvenanceFinder(String user) {
        userID = user;
        service = new LocalAnalysisClient(userID);
    }

    public String createProvenancePipeline(Set<JobInfo> jobs, String pipelineName) {
        String lsid = null;

        try {
            PipelineModel model = this.createPipelineModel(jobs, pipelineName);
            PipelineController controller = new PipelineController(model);
            lsid = controller.generateTask();
            model.setLsid(lsid);
            copyFilesToPipelineDir(lsid, pipelineName);
        } catch (Exception e) {
            e.printStackTrace();

        }
        return lsid;
    }

    public String createProvenancePipeline(String filename, String pipelineName) {
        String lsid = null;
        Set<JobInfo> jobs = this.findJobsThatCreatedFile(filename);
        lsid = createProvenancePipeline(jobs, pipelineName);
        return lsid;
    }

    public Set<JobInfo> findJobsThatCreatedFile(String fileURL) {
        ArrayList<String> files = new ArrayList<String>();
        Set<JobInfo> jobs = new TreeSet<JobInfo>(new Comparator<JobInfo>() {
            public int compare(JobInfo j1, JobInfo j2) {
                if (j1.getJobNumber() > j2.getJobNumber())
                    return 1;
                else if (j1.getJobNumber() < j2.getJobNumber())
                    return -1;
                else
                    return 0;
            }

        });

        files.add(fileURL);

        while (!files.isEmpty()) {
            String aFile = files.get(0);
            if (aFile == null)
                continue;
            JobInfo job = findJobThatCreatedFile(aFile);
            if (job != null)
                jobs.add(job);
            files.addAll(getLocalInputFiles(job));
            files.remove(0);
        }
        return jobs;
    }

    /**
     * Given a file URL find the Job that created it or return null. Must be a
     * job output file
     */
    public JobInfo findJobThatCreatedFile(String fileURL) {
        String jobNoStr = getJobNoFromURL(fileURL);
        if (jobNoStr == null) {
            try {
                // maybe just a job # passed in
                Integer.parseInt(fileURL);
                jobNoStr = fileURL;
            } catch (NumberFormatException nfe) {
            }

        }
        int jobid = -1;
        try {
            jobid = Integer.parseInt(jobNoStr);
            return service.getJob(jobid);

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Given an reverse ordered set of jobs (ordered by decreasing Job #) create
     * a pipeline model that represents it with the appropriate file inheritence
     * representing the original jobs
     */
    protected PipelineModel createPipelineModel(Set<JobInfo> jobs, String pipelineName) throws OmnigeneException,
            WebServiceException {
        filesToCopy = new ArrayList();
        // create an array list with the taskinfos at their taskid location for
        // easier retrieval later
        Collection<TaskInfo> taskCatalog = new LocalAdminClient(userID).getTaskCatalog();
        HashMap<String, TaskInfo> taskList = new HashMap<String, TaskInfo>();
        HashMap<Integer, Integer> jobOrder = new HashMap<Integer, Integer>();

        for (Iterator<TaskInfo> iter = taskCatalog.iterator(); iter.hasNext();) {
            TaskInfo ti = iter.next();
            taskList.put(ti.getLsid(), ti);
        }

        String taskLSID = "";

        PipelineModel model = new PipelineModel();
        model.setName(pipelineName); // XXX
        model.setDescription("describe it here");// XXX
        model.setAuthor(userID);
        model.setUserID(userID);
        model.setLsid(taskLSID); // temp pipeline
        model.setVersion("0");
        model.setPrivacy(GPConstants.PRIVATE);
        int i = 0;
        for (Iterator<JobInfo> iter = jobs.iterator(); iter.hasNext(); i++) {
            JobInfo job = iter.next();

            jobOrder.put(new Integer(job.getJobNumber()), new Integer(i));
            // map old job number to order in pipeline

            TaskInfo mTaskInfo = taskList.get(job.getTaskLSID());
            if (mTaskInfo == null) {
                throw new WebServiceException("Could not find job number: " + job.getJobNumber() + ", task: "
                        + job.getTaskName() + " in task list.");
            }
            TaskInfoAttributes mTia = mTaskInfo.giveTaskInfoAttributes();
            boolean isVisualizer = ((String) mTia.get(GPConstants.TASK_TYPE)).equals(GPConstants.TASK_TYPE_VISUALIZER);

            ParameterInfo[] adjustedParams = createPipelineParams(job.getParameterInfoArray(), mTaskInfo
                    .getParameterInfoArray(), jobOrder);

            boolean[] runTimePrompt = (adjustedParams != null ? new boolean[adjustedParams.length] : null);
            if (runTimePrompt != null) {
                for (int j = 0; j < adjustedParams.length; j++) {
                    runTimePrompt[j] = false;
                }

            }

            JobSubmission jobSubmission = new JobSubmission(mTaskInfo.getName(), mTaskInfo.getDescription(), mTia
                    .get(GPConstants.LSID), adjustedParams, runTimePrompt, // runtime
                    // prompts
                    // will
                    // always
                    // be
                    // false
                    // in
                    // these
                    // generated
                    // pipelines
                    isVisualizer, mTaskInfo);

            model.addTask(jobSubmission);

        }
        return model;
    }

    protected void copyFilesToPipelineDir(String pipelineLSID, String pipelineName) {
        String attachmentDir = null;
        try {
            attachmentDir = DirectoryManager.getTaskLibDir(pipelineName, pipelineLSID, userID);
        } catch (Exception e) {
            System.out.println("Could not copy files for pipeline: " + pipelineLSID);
            e.printStackTrace();
            return;
        }

        File dir = new File(attachmentDir);
        dir.mkdir();
        byte[] buf = new byte[100000];
        int j;

        for (Iterator iter = filesToCopy.iterator(); iter.hasNext();) {
            File aFile = (File) iter.next();
            FileInputStream is = null;
            FileOutputStream os = null;
            try {
                is = new FileInputStream(aFile);
                os = new FileOutputStream(new File(dir, aFile.getName()));
                while ((j = is.read(buf, 0, buf.length)) > 0) {
                    os.write(buf, 0, j);
                }

            } catch (IOException e) {
                System.out
                        .println("Could not copy file " + aFile.getAbsolutePath() + " todir " + dir.getAbsolutePath());
                e.printStackTrace();
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {

                }
                try {
                    if (os != null) {
                        os.close();
                    }
                } catch (IOException e) {

                }
            }
        }
        filesToCopy.clear();
    }

    protected String getJobNoFromURL(String fileURL) {
        return getParamFromURL(fileURL, "job");
    }

    protected String getParamFromURL(String fileURL, String key) {
        // if it is null or not a local file we can do nothing

        if (fileURL == null)
            return null;
        if (!(fileURL.toUpperCase().startsWith(serverURL)) && !fileURL.startsWith("http://127.0.0.1")
                && !fileURL.startsWith("http://localhost")) {
            return null;
        }

        // if it is not a result file do nothing
        boolean isResultFile = fileURL.indexOf("jobResults") >= 1;
        
        if (!((fileURL.indexOf("retrieveResults.jsp") >= 1) || (isResultFile)))
            return null;
        
        String jobNoStr = "";
        
        if (isResultFile){
            int idx = fileURL.indexOf("jobResults");
            idx += 11;
            int endidx = fileURL.indexOf('/', idx);
            jobNoStr = fileURL.substring(idx, endidx);
            
        } else {
        
            // now we think we have a local result file url so grab the job #
            int idx = fileURL.indexOf(key + "=");
            if (idx < 0)
                return null; // can't find a job #
    
                int endIdx = fileURL.indexOf("&", idx);
            if (endIdx == -1)
                endIdx = fileURL.length();

            jobNoStr = fileURL.substring(idx + 1 + key.length(), endIdx);
        }
        return jobNoStr;
    }

    protected ArrayList<String> getLocalInputFiles(JobInfo job) {
        ArrayList<String> inputFiles = new ArrayList<String>();
        if (job == null)
            return inputFiles;

        ParameterInfo[] params = job.getParameterInfoArray();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {

                String val = getURLFromParam(params[i]);

                if (val != null)
                    inputFiles.add(val);
            }
        }
        return inputFiles;
    }

    public String getURLFromParam(ParameterInfo pinfo) {
        HashMap attributes = pinfo.getAttributes();
        String pvalue = pinfo.getValue();
        if (pvalue.toUpperCase().startsWith(serverURL)) {
            return pvalue;
        } else if ("FILE".equals(attributes.get("TYPE"))) {

            if ("CACHED_IN".equals(attributes.get("MODE"))) {
                int idx = pvalue.indexOf("/");
                String jobstr = pvalue.substring(0, idx);
                String filename = pvalue.substring(idx + 1);

                return serverURL + "/gp/jobResults/" + jobstr + "/" + filename;
            } else {
                return pvalue;

            }
        }
        return null;
    }

    /**
     * realing input files from the old job params to the new pipeline. Look for
     * input files matching the request pattern like we did to find these jobs
     * and replace with gpUseResult() calls. Create a new ParameterInfo array to
     * return.
     */
    protected ParameterInfo[] createPipelineParams(ParameterInfo[] oldJobParams, ParameterInfo[] taskParams,
            HashMap jobOrder) throws WebServiceException {
        ParameterInfo[] newParams = new ParameterInfo[taskParams.length];

        for (int i = 0; i < taskParams.length; i++) {
            ParameterInfo taskParam = taskParams[i];
            ParameterInfo oldJobParam = null;
            for (int j = 0; j < oldJobParams.length; j++) {
                oldJobParam = oldJobParams[j];
                if (oldJobParam.getName().equalsIgnoreCase(taskParam.getName()))
                    break;
            }
            HashMap attrs = oldJobParam.getAttributes();

            String value = getURLFromParam(oldJobParam);

            String jobNoStr = getJobNoFromURL(value);

            if (jobNoStr == null) {
                // for files that are on the server, replace with generic URL
                // with LSID to be substituted at runtime
                // for anything else leave it unmodified
                value = oldJobParam.getValue();

                File inFile = new File(value);
                if (inFile.exists()) {
                    filesToCopy.add(inFile);
                    value = "<GenePatternURL>getFile.jsp?task=" + GPConstants.LEFT_DELIMITER + GPConstants.LSID
                            + GPConstants.RIGHT_DELIMITER + "&file=" + URLEncoder.encode(inFile.getName());
                }

            } else {
                // figure out the jobs order in the new pipeline and use
                // gpUseResult
                Integer jobNo = new Integer(jobNoStr);
                Integer pipeNo = (Integer) jobOrder.get(jobNo);

                attrs.put(PipelineModel.INHERIT_TASKNAME, "" + pipeNo);
                System.out.println("InheritTask: " + pipeNo);

                JobInfo priorJob = service.getJob(jobNo.intValue());
                String name = getParamFromURL(value, "filename");
                //
                // XXX use file index for now, Change to file type when I
                // understand how
                // to get the right information
                //
                ParameterInfo[] pjp = priorJob.getParameterInfoArray();
                int fileIdx = 0;
                for (int j = 0; j < pjp.length; j++) {
                    if (pjp[j].isOutputFile()) {
                        fileIdx++;
                        if (pjp[j].getValue().endsWith(name)) {
                            attrs.put(PipelineModel.INHERIT_FILENAME, "" + fileIdx);
                        }
                    }
                }

                // now we figure out which file to use from the job
                value = "";
            }
            newParams[i] = new ParameterInfo(taskParam.getName(), value, taskParam.getDescription());
            newParams[i].setAttributes(attrs);

        }
        return newParams;
    }

}