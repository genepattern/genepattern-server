package org.genepattern.server.executor.pipeline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.MissingTasksException;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.data.pipeline.PipelineModelException;
import org.genepattern.data.pipeline.PipelineUtil;
import org.genepattern.server.JobManager;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.jobresult.JobResultFile;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.executor.AnalysisJobScheduler;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.executor.JobSubmissionException;
import org.genepattern.server.executor.JobTerminationException;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.webservice.server.AdminService;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;
import org.hibernate.Query;

/**
 * Refactor code from GP 3.2.3 PipelineExecutor. This uses almost the same code base, but makes some changes
 * so that pipelines can be executed asynchronously rather than within a single thread.
 * 
 * TODO: add support for stopAfterTask
 * TODO: handle exceptions
 *     a) server error in startPipeline
 *     b) server error in prepareNextStep
 * 
 * @author pcarr
 */
public class PipelineHandler {
    private static Logger log = Logger.getLogger(PipelineHandler.class);
    
    public static boolean isBatchStep(JobInfo jobInfo) {
        //TODO: get this info from the config file
        final String[] batchSteps = { "BatchExecPipeline", "BatchPreprocessThenCMS" };
        
        String taskName = jobInfo.getTaskName();
        for (String match : batchSteps) {
            //all pipelines named 'Scatter*' ...
            if (taskName.startsWith("Scatter")) {
                return true;
            }
            if (match.equals(taskName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Initialize the pipeline and add the first job to the queue.
     */
    public static void startPipeline(JobInfo pipelineJobInfo, int stopAfterTask) throws CommandExecutorException {
        if (pipelineJobInfo == null) {
            throw new CommandExecutorException("Error starting pipeline, pipelineJobInfo is null");
        }
        
        log.debug("starting pipeline: "+pipelineJobInfo.getTaskName()+" ["+pipelineJobInfo.getJobNumber()+"]");
        boolean isBatchStep = isBatchStep(pipelineJobInfo);
        try {
            runPipeline(pipelineJobInfo, stopAfterTask, isBatchStep);
            HibernateUtil.commitTransaction();
        }
        catch (Throwable t) {
            HibernateUtil.rollbackTransaction();
            throw new CommandExecutorException("Error starting pipeline: "+t.getLocalizedMessage(), t);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }
    
    public static void terminatePipeline(JobInfo jobInfo) {
        if (jobInfo == null) {
            log.error("Ignoring null arg");
            return;
        }
        // handle special-case: pipeline already finished before user terminated event was processed
        if (JobStatus.ERROR.equals(jobInfo.getStatus()) || JobStatus.FINISHED.equals(jobInfo.getStatus())) {
            log.info("job #"+jobInfo.getJobNumber()+" already finished, status="+jobInfo.getStatus());
            return;
        }
        
        //get current job and terminate it ... if all steps have already completed ... log an error but do nothing
        int processingJobId = -1;
        HibernateUtil.beginTransaction();
        List<Object[]> jobInfoObjs = getChildJobObjs(jobInfo.getJobNumber());
        HibernateUtil.closeCurrentSession();
        for(Object[] row : jobInfoObjs) {
            int jobId = (Integer) row[0];
            int statusId = (Integer) row[1];
            if (JobStatus.JOB_PROCESSING == statusId) {
                processingJobId = jobId;
            }
        }
        if (processingJobId >= 0) {
            try {
                AnalysisJobScheduler.terminateJob(processingJobId);
            }
            catch (JobTerminationException e) {
                log.error("Error terminating job #"+processingJobId+" in pipeline "+jobInfo.getJobNumber(), e);
                return;
            }
            return;
        }
        else {
            terminatePipelineSteps(jobInfo.getJobNumber());
            handlePipelineJobCompletion(jobInfo.getJobNumber(), -1, "Job #"+jobInfo.getJobNumber()+" terminated by user.");
        }
    }
    
    /**
     * Call this before running the next step in the given pipeline. Must be called after all dependent steps are completed.
     * Note: circa GP 3.2.3 and earlier this code was part of a loop which ran all steps in the same thread.
     * Note: circa GP 3.2.4 this method makes changes to the jobInfo which are saved to the DB.
     *
     * @param jobInfo - the job which is about to run
     */
    public static void prepareNextStep(int parentJobId, JobInfo jobInfo) throws PipelineException { 
        try {
            AnalysisDAO dao = new AnalysisDAO();
            boolean updated = setInheritedParams(dao, parentJobId, jobInfo);
            //TODO: double-check that we don't need to updateParameterInfo unless there were inherited parameters used
            //    as input to this step in the pipeline
            updateParameterInfo(jobInfo);
            HibernateUtil.commitTransaction();
        }
        catch (Throwable t) {
            HibernateUtil.rollbackTransaction();
            throw new PipelineException("Error preparing next step in pipeline job #"+parentJobId+" for step #"+jobInfo.getJobNumber(), t);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }
    
    /**
     * Helper method (could go into AnalysisDAO) for saving edits to the ANALYSIS_JOB.PARAMETER_INFO for a given job.
     */
    private static void updateParameterInfo(JobInfo jobInfo) {
        int jobId = jobInfo.getJobNumber();
        String paramString = jobInfo.getParameterInfo();
        AnalysisJob aJob = (AnalysisJob) HibernateUtil.getSession().get(AnalysisJob.class, jobId);
        aJob.setParameterInfo(paramString);
        HibernateUtil.getSession().update(aJob);
    }
    
    //DAO helpers
    /**
     * Get all of the immediate child jobs for the given pipeline.
     * 
     * @return a List of [jobNo(Integer),statusId(Integer)]
     */
    private static List<Object[]> getChildJobObjs(int parentJobId) {
        //TODO: hard-coded so that no more than 10000 batch jobs can be handled
        final int maxChildJobs = 10000; //limit total number of results to 10000
        String hql = "select a.jobNo, jobStatus.statusId from org.genepattern.server.domain.AnalysisJob a where a.parent = :jobNo ";
        hql += " ORDER BY jobNo ASC";
        Query query = HibernateUtil.getSession().createQuery(hql);
        query.setInteger("jobNo", parentJobId);
        query.setFetchSize(maxChildJobs);
        
        List<Object[]> rval = query.list();
        return rval;
    }
    
    public static boolean startNextJob(int parentJobNumber) {
        Integer nextJobId = getNextJobId(parentJobNumber);
        if (nextJobId == null) {
            return false;
        }

        //if there is another job to run, change the status of the next job to pending
        int rval = AnalysisJobScheduler.changeJobStatus(nextJobId, JobStatus.JOB_WAITING, JobStatus.JOB_PENDING);
        //indicate there is another step waiting on the queue
        return true;
    }
    
    /**
     * Called when a step in a pipeline job has completed, put the next job on the queue.
     * Check for and handle pipeline termination.
     * 
     * @param jobInfo
     * @param parentJobInfo
     * @param jobStatus
     * @param completionDate
     * @return
     */
    public static boolean handleJobCompletion(int childJobNumber) {
        if (childJobNumber < 0) {
            log.error("Invalid jobNumber: "+childJobNumber);
        }
        JobInfo childJobInfo = null;
        int parentJobNumber = -1;
        try {
            AnalysisDAO ds = new AnalysisDAO();
            childJobInfo = ds.getJobInfo(childJobNumber);
            parentJobNumber = childJobInfo._getParentJobNumber();
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }

        if (parentJobNumber < 0) {
            log.error("Invalid parentJobNumber: "+parentJobNumber);
            return false;
        }
        
        //change from the 3.3.3 implementation to support for parallel execution of batch jobs
        // a) in batch pipelines, more than one step in a pipeline gets started; we can't automically flag the parent
        //    pipeline as complete until all of the steps are complete
        // b) in batch pipelines, we don't want to terminate the parent pipeline, when one of the steps fails


        //check the status of the job
        if (JobStatus.FINISHED.equals(childJobInfo.getStatus())) { 
            //get the next step in the pipeline, by jobId
            HibernateUtil.beginTransaction();
            List<Object[]> jobInfoObjs = getChildJobObjs(parentJobNumber);
            HibernateUtil.closeCurrentSession();
            
            // the job is complete iff the list is empty
            int nextWaitingJob = -1;
            boolean lastStepComplete = true;
            for(Object[] row : jobInfoObjs) {
                int jobId = (Integer) row[0];
                int statusId = (Integer) row[1];
                if (nextWaitingJob == -1 && JobStatus.JOB_WAITING == statusId) {
                    nextWaitingJob = jobId;
                }
                if (JobStatus.JOB_FINISHED != statusId && JobStatus.JOB_ERROR != statusId) {
                    lastStepComplete = false;
                }
            }
            
            if (nextWaitingJob >= 0) {
                //if there is another job to run, change the status of the next job to pending
                startNextStep(nextWaitingJob);
                //indicate there is another step waiting on the queue
                return true;
            }
            if (lastStepComplete) {
                //it's the last step in the pipeline, update its status
                //TODO: double-check this code ...
                HibernateUtil.beginTransaction();
                handlePipelineJobCompletion(parentJobNumber, 0);
                HibernateUtil.commitTransaction();
                return false;
            }
        }
        else {
            //TODO: in some cases, we don't want to kill the parent pipeline or its child jobs
            //handle an error in a pipeline step
            terminatePipelineSteps(parentJobNumber);
            handlePipelineJobCompletion(parentJobNumber, -1, "Pipeline terminated because of an error in child job [id: "+childJobNumber+"]");
        }
        return false;
    }
    
    public static void handleRunningPipeline(JobInfo pipeline) {
        if (pipeline == null) {
            log.error("Invalid null arg");
            return;
        }
        if (pipeline.getJobNumber() < 0) {
            log.error("Invalid arg: pipeline.jobNumber="+pipeline.getJobNumber());
        }

        //the number of child steps which have not yet finished
        int numStepsToGo = 0;
        //true if at least one of the steps finished with an error
        boolean errorFlag = false;
        String errorMessage = null;
        List<Object[]> jobInfoObjs = null;
        try {
            HibernateUtil.beginTransaction();
            jobInfoObjs = getChildJobObjs(pipeline.getJobNumber());
        }
        catch (Throwable t) {
            jobInfoObjs = new ArrayList<Object[]>();
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
        for(Object[] row : jobInfoObjs) {
            int jobId = (Integer) row[0];
            int statusId = (Integer) row[1];
            if (JobStatus.JOB_FINISHED == statusId) {
            }
            else if (JobStatus.JOB_ERROR == statusId) {
                errorFlag = true;
                errorMessage = "Error in child job "+jobId;
            }
            else {
                ++numStepsToGo;
            }
        }
        
        if (numStepsToGo == 0 || errorFlag) {
            if (errorFlag) {
                //handle an error in a pipeline step
                terminatePipelineSteps(pipeline.getJobNumber());
                handlePipelineJobCompletion(pipeline.getJobNumber(), -1, errorMessage);
            }
            else {
                PipelineHandler.handlePipelineJobCompletion(pipeline.getJobNumber(), 0);
            }
        }
    }
    
    private static void startNextStep(int nextJobId) {
        try {
            HibernateUtil.beginTransaction();
            int rval = AnalysisJobScheduler.changeJobStatus(nextJobId, JobStatus.JOB_WAITING, JobStatus.JOB_PENDING);
            HibernateUtil.commitTransaction();
        }
        catch (Throwable t) {
            HibernateUtil.rollbackTransaction();
        }
    }
    
    private static void terminatePipelineSteps(int parentJobNumber) {
        try {
            HibernateUtil.beginTransaction();
            List<Integer> incompleteJobIds = getIncompleteJobs(parentJobNumber);
            for(Integer jobId : incompleteJobIds) {
                AnalysisJobScheduler.setJobStatus(jobId, JobStatus.JOB_ERROR);
            }
            HibernateUtil.commitTransaction();
        }
        catch (Throwable t) {
            log.error("Error updating job status for child jobs in pipeline #"+parentJobNumber, t);
            HibernateUtil.rollbackTransaction();
        }
    }

    private static void handlePipelineJobCompletion(int parentJobNumber, int exitCode) {
        handlePipelineJobCompletion(parentJobNumber, exitCode, (String) null);
    }
    
    /**
     * For the given pipeline, set its status to complete (and notify any interested listeners).
     * Delegated to the GenePatternAnalysisTask.
     * 
     * @param parentJobNumber
     * @param exitCode, with a non-zero exitCode, an errorMessage is automatically created, if necessary.
     * @param errorMessage, can be null
     */
    private static void handlePipelineJobCompletion(int parentJobNumber, int exitCode, String errorMessage) {
        //HACK: special-case code
        //if this pipeline is a batch exec pipeline, gather all result files from all submitted jobs
        //into a filelist and save this filelist as the first output file of the pipeline
        try {
            AnalysisDAO dao = new AnalysisDAO();
            JobInfo parentJobInfo = dao.getJobInfo(parentJobNumber);
            boolean isBatchStep = isBatchStep(parentJobInfo);
            if (isBatchStep) {
                List<ParameterInfo> allResultFiles = getOutputFilesRecursive(dao, parentJobInfo); 
                GatherResults gatherResults = new GatherResults(parentJobInfo, allResultFiles); 
                //TODO: parameterize this ... so that each scatter module can define a custom 
                //    set of output filelists, e.g. { 0: *.gtf, 1: *.bam }
                GpFilePath filelist = gatherResults.writeFilelist();
                if (parentJobInfo.getTaskName().startsWith("Scatter")) {
                    //2nd output is the list of all bam files
                    FileFilter f2 = new FileFilter() {
                        public boolean accept(File arg0) {
                            if (arg0 != null && arg0.getName() != null && arg0.getName().toLowerCase().endsWith(".bam")) {
                                return true;
                            }
                            return false;
                        }
                    };
                    gatherResults.writeFilelist("all.bam.filelist.txt", f2);
                    //3rd output is the list of all gtf files
                    FileFilter f3 = new FileFilter() {
                        public boolean accept(File arg0) {
                            if (arg0 != null && arg0.getName() != null && arg0.getName().toLowerCase().endsWith(".gtf")) {
                                return true;
                            }
                            return false;
                        }
                    };
                    gatherResults.writeFilelist("all.gtf.filelist.txt", f3);
                }
            }
        }
        catch (Throwable t) {
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }

        try {
            if (exitCode != 0 && errorMessage == null) {
                errorMessage = "Pipeline terminated with non-zero exitCode: "+exitCode;
            }
            if (errorMessage == null) {
                GenePatternAnalysisTask.handleJobCompletion(parentJobNumber, exitCode);
            }
            else {
                GenePatternAnalysisTask.handleJobCompletion(parentJobNumber, exitCode, errorMessage);
            }
        }
        catch (Throwable t) {
            log.error("Error recording pipeline job completion for job #"+parentJobNumber, t);
        }
    }
    
    /**
     * For the given pipeline, get the next 'WAITING' job id.
     * 
     * @param parentJobId, the jobId of the pipeline
     * @return the jobId if the first job whose status is WAITING, or null if no waiting jobs are found
     */
    private static Integer getNextJobId(final int parentJobId) {
        List<Object[]> jobInfoObjs = getChildJobObjs(parentJobId);
        for(Object[] row : jobInfoObjs) {
            int jobNo = (Integer) row[0];
            int statusId = (Integer) row[1];
            if (JobStatus.JOB_WAITING == statusId) {
                return jobNo;
            }
        }
        return null;
    }

    /**
     * For the given pipeline, get the list of child jobs which have not yet finished.
     * Only get immediate child jobs.
     * 
     * @param parentJobId
     * @return
     */
    private static List<Integer> getIncompleteJobs(final int parentJobId) {
        List<Object[]> jobInfoObjs = getChildJobObjs(parentJobId);
        List<Integer> waitingJobs = new ArrayList<Integer>();
        for(Object[] row : jobInfoObjs) {
            int jobId = (Integer) row[0];
            int statusId = (Integer) row[1];
            //ignore completed jobs
            if (JobStatus.JOB_FINISHED != statusId && JobStatus.JOB_ERROR != statusId) {
                waitingJobs.add(jobId);
            }
        }
        return waitingJobs;
    }
    
    private static Integer runPipeline(JobInfo pipelineJobInfo, int stopAfterTask, boolean isBatchStep) 
    throws PipelineModelException, MissingTasksException, JobSubmissionException
    {
        if (!isBatchStep) {
            return runPipeline(pipelineJobInfo, stopAfterTask);
        }
        return runBatchPipeline(pipelineJobInfo, stopAfterTask);
    }
    
    /**
     * Start the pipeline.
     * 
     * @param pipelineJobInfo, must be a valid pipeline job
     * @param stopAfterTask
     * 
     * @return the jobNumber of the first step of the pipeline
     * 
     * @throws PipelineModelException
     * @throws MissingTasksException, if the required modules are not installed on the server
     * @throws WebServiceException
     * @throws JobSubmissionException
     */
    private static Integer runPipeline(JobInfo pipelineJobInfo, int stopAfterTask) 
    throws PipelineModelException, MissingTasksException, JobSubmissionException
    {  
        PipelineModel pipelineModel = PipelineUtil.getPipelineModel(pipelineJobInfo);
        pipelineModel.setLsid(pipelineJobInfo.getTaskLSID());
        checkForMissingTasks(pipelineJobInfo.getUserId(), pipelineModel);
        
        //initialize the pipeline args
        Map<String,String> additionalArgs = new HashMap<String,String>();
        for(ParameterInfo param : pipelineJobInfo.getParameterInfoArray()) {
            additionalArgs.put(param.getName(), param.getValue());
        }

        int stepNum = 0;
        Vector<JobSubmission> tasks = pipelineModel.getTasks();
        Integer firstStep = null;
        for(JobSubmission jobSubmission : tasks) { 
            if (stepNum >= stopAfterTask) {
                break; // stop and execute no further
            }
            
            ParameterInfo[] parameterInfo = jobSubmission.giveParameterInfoArray();
            substituteLsidInInputFiles(pipelineJobInfo.getTaskLSID(), parameterInfo);
            ParameterInfo[] params = parameterInfo;
            params = setJobParametersFromArgs(jobSubmission.getName(), stepNum + 1, params, additionalArgs);

            int jobStatusId = JobStatus.JOB_WAITING;
            JobInfo submittedJob = addJobToPipeline(pipelineJobInfo.getJobNumber(), pipelineJobInfo.getUserId(), jobSubmission, params, jobStatusId);
            if (firstStep == null) {
                firstStep = submittedJob.getJobNumber();
            }
            ++stepNum;
        }
        return firstStep;
    }

    private static Integer runBatchPipeline(JobInfo pipelineJobInfo, int stopAfterTask) 
    throws PipelineModelException, MissingTasksException, JobSubmissionException
    { 
        log.debug("starting batch pipeline: "+pipelineJobInfo.getTaskName()+" ["+pipelineJobInfo.getJobNumber()+"]");
        
        // add a batch of jobs, as children of the given pipelineJobInfo 
        PipelineModel pipelineModel = PipelineUtil.getPipelineModel(pipelineJobInfo);
        pipelineModel.setLsid(pipelineJobInfo.getTaskLSID());
        checkForMissingTasks(pipelineJobInfo.getUserId(), pipelineModel);

        //initialize the pipeline args
        Map<String,String> additionalArgs = new HashMap<String,String>();
        for(ParameterInfo param : pipelineJobInfo.getParameterInfoArray()) {
            additionalArgs.put(param.getName(), param.getValue());
        }

        int stepNum = 0;
        Vector<JobSubmission> tasks = pipelineModel.getTasks();
        if (tasks.size() == 0)  {
            throw new JobSubmissionException("Don't know what to do with 0 tasks in pipelineModel");
        }
        if (tasks.size() > 1) {
            //TODO: fix this
            throw new JobSubmissionException("Only a single batch step is allowed, num steps = "+tasks.size());
        }

        //assuming a 1-step pipeline
        TaskInfo taskInfo = tasks.get(0).getTaskInfo();
        ParameterInfo[] parameterInfo = tasks.get(0).giveParameterInfoArray();
        substituteLsidInInputFiles(pipelineJobInfo.getTaskLSID(), parameterInfo);
        ParameterInfo[] params = parameterInfo;
        params = setJobParametersFromArgs(tasks.get(0).getName(), stepNum + 1, params, additionalArgs);
        
        //for each step, if it has a batch input parameter, expand into a bunch of child steps
        Map<ParameterInfo,List<String>> batchParamMap = new HashMap<ParameterInfo, List<String>>();
        for(ParameterInfo p : params) {
            if (p.isInputFile()) {
                //TODO: come up with a better way to tag an input parameter as a batch value
                boolean isBatchParameter = false;
                isBatchParameter = (p.getValue().endsWith("filelist.txt"));
                if (isBatchParameter) {
                    log.debug("batch input parameter: "+p.getName()+"="+p.getValue());
                    //assume it's a batch job
                    try {
                        List<String> inputFiles = parseFileList( p.getValue() );
                        batchParamMap.put(p, inputFiles);
                    }
                    catch (Throwable t) {
                        log.error("Error reading filelist from file: "+p.getValue(), t);
                    }
                }
            }
        }
        
        Integer firstJob = null;
        List<JobInfo> submittedJobs = addBatchJobsToPipeline(pipelineJobInfo, taskInfo, params, batchParamMap);
        if (submittedJobs != null && submittedJobs.size() > 0) {
            firstJob = submittedJobs.get(0).getJobNumber();
        }
        return firstJob;
    }

    
    private static List<JobInfo> addBatchJobsToPipeline(JobInfo pipelineJobInfo, TaskInfo taskInfo, ParameterInfo[] params, Map<ParameterInfo,List<String>> batchParamMap) 
    throws JobSubmissionException
    {
        log.debug("adding batch jobs to pipeline, "+pipelineJobInfo.getTaskName()+"["+pipelineJobInfo.getJobNumber()+"] ... ");
        List<JobInfo> submittedJobs = new ArrayList<JobInfo>();
        //validate the number of batch jobs to submit
        List<List<String>> row = new ArrayList<List<String>>();
        int jobCount = -1;
        List<Integer> jobCounts = new ArrayList<Integer>();
        List<List<String>> table = new ArrayList<List<String>>();
        for(Entry<ParameterInfo, List<String>> entry : batchParamMap.entrySet()) {
            List<String> values = new ArrayList<String>();
            row.add(values);
            ParameterInfo batchParam = entry.getKey();
            int numJobs = 0;
            for(String batchParamValue : entry.getValue()) {
                values.add( batchParamValue );
                //batchParam.setValue( batchParamValue );
                ++numJobs;
            }
            jobCounts.add(numJobs);
            if (jobCount == -1) {
                jobCount = numJobs;
            }
            else {
                if (jobCount != numJobs) {
                    throw new IllegalArgumentException("Mismatch of batch input files: maxJobCount="+jobCount+", jobCount for batch param, "+batchParam.getName()+", is "+numJobs );
                }
            }
        }

        //don't create a batch job if there are no input values
        if (jobCount <= 0) {
            //TODO: should we allow empty batch steps?
            //     as currently implemented, a batch job with zero steps will get stuck in a permanent processing state, 
            //     because it waits for all of its sub-steps to complete
            throw new IllegalArgumentException("No batch jobs to submit");
        }
        
        log.debug("\tsubmitting "+jobCount+" batch jobs...");
        for(int job_idx = 0; job_idx < jobCount; ++job_idx) {
            for(ParameterInfo batchParam : batchParamMap.keySet()) {
                String batchParamValue = batchParamMap.get(batchParam).get(job_idx);
                batchParam.setValue(batchParamValue);
                log.debug("\t\tstep "+job_idx+": "+batchParam.getName()+"="+batchParam.getValue());
            }
            JobInfo submittedJob = addJobToPipeline(pipelineJobInfo.getJobNumber(), pipelineJobInfo.getUserId(), taskInfo, params, JobStatus.JOB_WAITING);
            submittedJobs.add(submittedJob);
        }
        return submittedJobs;
    }

    private static List<String> parseFileList(String value) throws IOException {
        log.debug("Reading filelist from: "+value);
        List<String> inputValues = new ArrayList<String>();
        File filelist = new File(value);
        if (!filelist.canRead()) {
            //special-case, url input
            try {
                //TODO: GpFileObjFactory#getRequestedGpFileObj is the correct thing to do, 
                //    but it has not yet been implemented for /jobResults/ files
                //GpFilePath path = GpFileObjFactory.getRequestedGpFileObj(value);
                //if (path != null) {
                //    filelist = path.getServerFile();
                //}
                String[] parts = GpFileObjFactory.getPathInfo(value);
                String servletPath = parts[0];
                String pathInfo = parts[1];
                if ("/jobResults".equals(servletPath)) {
                    JobResultFile jobResultFile = new JobResultFile(pathInfo);
                    filelist = jobResultFile.getServerFile();
                }

            }
            catch (Exception e) {
                log.error(e);
            }
        }
        if (!filelist.canRead()) {
            //TODO: throw exception
            return inputValues;
        }
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(filelist);
        }
        catch (FileNotFoundException e) {
            //TODO: throw exception
            return inputValues;
        }
        
        BufferedReader in = null;        
        try {
            in = new BufferedReader(fileReader);
            String line = null;
            while((line = in.readLine()) != null) {
                inputValues.add(line);
            }
        }
        finally {
            if (in != null) {
                in.close();
            }
        }
        return inputValues;
    }
    
    /**
     * Add the job to the pipeline and add it to the internal job queue in a WAITING state.
     * 
     * @throws IllegalArgumentException, if the given JobSubmission does not have a valid taskId
     * @throws JobSubmissionException, if not able to add the job to the internal queue
     */
    private static JobInfo addJobToPipeline(int parentJobId, String userID, JobSubmission jobSubmission, ParameterInfo[] params, int jobStatusId)
    throws JobSubmissionException
    {
        if (jobSubmission == null) {
            throw new IllegalArgumentException("jobSubmission is null");
        }
        if (jobSubmission.getTaskInfo() == null) {
            throw new IllegalArgumentException("jobSubmission.taskInfo is null");
        }
        if (jobSubmission.getTaskInfo().getID() < 0) {
            throw new IllegalArgumentException("jobSubmission.taskInfo.ID not set");
        }

        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (params[i].isInputFile()) {
                    String file = params[i].getValue(); // bug 724
                    if (file != null && file.trim().length() != 0) {
                        String val = file;
                        try {
                            new URL(file);
                        } 
                        catch (MalformedURLException e) {
                            val = new File(file).toURI().toString();
                        }
                        params[i].setValue(val);
                        params[i].getAttributes().remove("TYPE");
                        params[i].getAttributes().remove("MODE");
                    }
                }
            }
        }

        JobInfo jobInfo = JobManager.addJobToQueue(jobSubmission.getTaskInfo(), userID, params, parentJobId, jobStatusId);
        return jobInfo;
    }
    
    /**
     * Add the job to the pipeline and add it to the internal job queue in a WAITING state.
     * 
     * @throws IllegalArgumentException, if the given JobSubmission does not have a valid taskId
     * @throws JobSubmissionException, if not able to add the job to the internal queue
     */
    private static JobInfo addJobToPipeline(int parentJobId, String userID, TaskInfo taskInfo, ParameterInfo[] params, int jobStatusId)
    throws JobSubmissionException
    {
        if (taskInfo == null) {
            throw new IllegalArgumentException("taskInfo is null");
        }
        if (taskInfo.getID() < 0) {
            throw new IllegalArgumentException("taskInfo.ID not set");
        }

        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (params[i].isInputFile()) {
                    String file = params[i].getValue(); // bug 724
                    if (file != null && file.trim().length() != 0) {
                        String val = file;
                        try {
                            new URL(file);
                        } 
                        catch (MalformedURLException e) {
                            val = new File(file).toURI().toString();
                        }
                        params[i].setValue(val);
                        params[i].getAttributes().remove("TYPE");
                        params[i].getAttributes().remove("MODE");
                    }
                }
            }
        }

        //make sure to commit db changes
        JobInfo jobInfo = JobManager.addJobToQueue(taskInfo, userID, params, parentJobId, jobStatusId);
        return jobInfo;
    }
    
    private static void checkForMissingTasks(String userID, PipelineModel model) throws MissingTasksException {
        MissingTasksException ex = null;
        AdminService adminService = new AdminService(userID);
        for(JobSubmission jobSubmission : model.getTasks()) {
            String lsid = jobSubmission.getLSID();
            TaskInfo taskInfo = null;
            try {
                taskInfo = adminService.getTask(lsid);
            }
            catch (Exception e) {
            }
            if (taskInfo == null) {
                if (ex == null) {
                    ex = new MissingTasksException();
                }
                ex.addError(jobSubmission);
            }
        }
        if (ex != null) {
            throw ex;
        }
    }
    
    /**
     * Before starting the next step in the pipeline, link output files from previous steps to input parameters for this step.
     * 
     * @param parentJobId
     * @param currentStep
     * 
     * @return true iff an input param was modified
     */
    private static boolean setInheritedParams(AnalysisDAO dao, int parentJobId, JobInfo currentStep) { 
        List<ParameterInfo> inheritedInputParams = getInheritedInputParams(currentStep);
        if (inheritedInputParams.size() == 0) {
            return false;
        }

        boolean modified = false;
        JobInfo[] childJobs = dao.getChildren(parentJobId);

        for(ParameterInfo inputParam : inheritedInputParams) {
            String url = getInheritedFilename(dao, childJobs, inputParam);
            if (url != null && url.trim().length() > 0) {
                modified = true;
                inputParam.setValue(url);
            }
            else {
                if (inputParam.isOptional()) {
                    //ignore
                }
                else {
                    log.error("Error setting inherited file name for non-optional input parameter in pipeline, param.name="+inputParam.getName());
                }
            }
        }            
        return modified;
    }
    
    /**
     * For the given job, get the list of ParameterInfo which are input parameters which 
     * inherit a result file from a job in the parent pipeline.
     */
    private static List<ParameterInfo> getInheritedInputParams(JobInfo currentStep) {
        List<ParameterInfo> inputParams = new ArrayList<ParameterInfo>();
        ParameterInfo[] parameterInfos = currentStep.getParameterInfoArray();
        for (ParameterInfo param : parameterInfos) {
            boolean isInheritTaskName = false;
            HashMap attributes = param.getAttributes();
            if (attributes != null) {
                isInheritTaskName = attributes.get(PipelineModel.INHERIT_TASKNAME) != null;
            }
            if (isInheritTaskName) {
                inputParams.add(param);
            }
        }
        return inputParams;
    }

    /**
     * if an inherited filename is set, update the inputParam attributes (must save back to the DB in calling method)
     * and return the url to the inherited result file.
     * 
     * @param childJobs
     * @param inputParam
     * @return
     */
    private static String getInheritedFilename(AnalysisDAO dao, JobInfo[] childJobs, ParameterInfo inputParam) {
        // these params must be removed so that the soap lib doesn't try to send the file as an attachment
        HashMap attributes = inputParam.getAttributes();
        final String taskStr = (String) attributes.get(PipelineModel.INHERIT_TASKNAME);
        final String fileStr = (String) attributes.get(PipelineModel.INHERIT_FILENAME);
        attributes.remove("TYPE");

        //taskStr holds the current step number in the pipeline
        int stepNum = -1;
        try {
            stepNum = Integer.parseInt(taskStr);
        }
        catch (NumberFormatException e) {
            log.error("Invalid taskStr="+taskStr, e);
            return "";
        }
        if (stepNum < 0 || stepNum >= childJobs.length) {
            log.error("Invalid stepNum: stepNum="+stepNum+", childJobs.length="+childJobs.length);
            return "";
        }        
        JobInfo fromJob = childJobs[stepNum];
        String fileName = null;
        try {
            fileName = getOutputFileName(dao, fromJob, fileStr);
            attributes.put(ParameterInfo.MODE, ParameterInfo.URL_INPUT_MODE);
            String context = System.getProperty("GP_Path", "/gp");
            String url = getServer() + context + "/jobResults/" + fileName;
            return url;
        }
        catch (ServerConfiguration.Exception e) {
            log.error(e.getLocalizedMessage());
            return "";            
        }
        catch (FileNotFoundException e) {
            return "";
        }
    }
    
    private static String getOutputFileName(AnalysisDAO dao, JobInfo fromJob, String fileStr)
    throws ServerConfiguration.Exception, FileNotFoundException 
    {
        //get the ordered list of output files for the job
        List<ParameterInfo> allResultFiles = getOutputFilesRecursive(dao, fromJob);
        
        //fileStr holds the reference to the output file, either an ordinal or a type
        int outputNum = -1;
        String outputType = null;
        try {
            //1...5, for 1st output, 2nd output, et cetera
            outputNum = Integer.parseInt(fileStr);
        }
        catch (NumberFormatException e) {
            //not a number, must be a type
            outputType = fileStr;
        }
        String fileName = null;
        if (outputNum > -1) {
            fileName = getNthOutputFilename(outputNum, allResultFiles);
        }
        else {
            fileName = getOutputFilenameByType(fromJob, outputType, allResultFiles);
        }
        if (fileName != null) {
            int lastIdx = fileName.lastIndexOf(File.separator);
            lastIdx = fileName.lastIndexOf(File.separator, lastIdx - 1); // get the job # too

            if (lastIdx != -1) {
                fileName = fileName.substring(lastIdx + 1);
            }
        }
        if (fileName == null) {
            throw new FileNotFoundException("Unable to find output file from job " + fromJob.getJobNumber() + " that matches " + fileStr + ".");
        }
        return fileName;
    }
    
    /**
     * Get the nth result file for the job, index starts at 1.
     * 
     * @param fileIdx
     * @param jobResultFiles, the ordered list of output files, including nested results for pipelines.
     * @return
     */
    private static String getNthOutputFilename(int fileIdx, List<ParameterInfo> jobResultFiles) {
        fileIdx = fileIdx - 1;
        if (fileIdx < 0 || fileIdx >= jobResultFiles.size()) {
            //TODO: log error
            return "";
        }
        ParameterInfo inheritedFile = jobResultFiles.get(fileIdx);
        String filename = inheritedFile.getValue();
        return filename;
    }
    
    /**
     * Get the result file from the job, based on the given file type.
     * 
     * @param fromJob
     * @param fileStr
     * @param jobResultFiles
     * @return
     * @throws ServerConfiguration.Exception
     * @throws FileNotFoundException
     */
    private static String getOutputFilenameByType(final JobInfo fromJob, final String fileStr, List<ParameterInfo> jobResultFiles) 
    throws ServerConfiguration.Exception, FileNotFoundException
    {
        String fileName = null;
        
        Context context = ServerConfiguration.Context.getContextForJob(fromJob);
        File rootJobDir = ServerConfiguration.instance().getRootJobDir(context);
        String jobDir = rootJobDir.getAbsolutePath();

        for (ParameterInfo outputFile : jobResultFiles ) {
            // get the filename
            String fn = outputFile.getValue();
            File aFile = new File(fn);
            if (!aFile.exists()) {
                aFile = new File("../", fn);
            }
            if (!aFile.exists()) {
                aFile = new File("../" + jobDir + "/", fn);
            }
            if (!aFile.exists()) {
                aFile = new File(jobDir + "/", fn);
            }

            if (isFileType(aFile, fileStr)) {
                fileName = fn;
                break;
            }
        }
        
        if (fileName != null) {
            return fileName;
        }
        
        if (fileStr.equals(GPConstants.STDOUT) || fileStr.equals(GPConstants.STDERR)) {
            fileName = fileStr;
        }
        
        return fileName; 
    }
    
    /**
     * Recursively get an ordered list of all of the output files for the given job, 
     * filtering out execution log files.
     * 
     * @param jobInfo
     * @return
     */
    private static List<ParameterInfo> getOutputFilesRecursive(AnalysisDAO dao, JobInfo jobInfo) {
        List<ParameterInfo> outs = new ArrayList<ParameterInfo>();
        List<ParameterInfo> immediateResults = getOutputParameterInfos(jobInfo);
        outs.addAll(immediateResults);
        List<JobInfo> allChildJobs = getChildJobInfosRecursive(dao, jobInfo);
        for(JobInfo child : allChildJobs) {
            List<ParameterInfo> childResults = getOutputParameterInfos(child);
            outs.addAll(childResults);
        }
        return outs;
    }
    
    /**
     * Get the list of all child job infos of the given job, not including the given job.
     * @param jobInfo
     * @return
     */
    private static List<JobInfo> getChildJobInfosRecursive(AnalysisDAO dao, JobInfo parent) {
        List<JobInfo> allChildren = new ArrayList<JobInfo>();
        if (parent == null) {
            log.error("Unexpected null arg");
            return allChildren;
        }
        appendChildJobInfos(dao, allChildren, parent);
        return allChildren;
    }

    private static void appendChildJobInfos(AnalysisDAO dao, List<JobInfo> jobInfoList, JobInfo jobInfo) {
        JobInfo[] childJobs = dao.getChildren(jobInfo.getJobNumber());
        for(JobInfo childJob : childJobs) {
            jobInfoList.add(childJob);
            appendChildJobInfos(dao, jobInfoList, childJob);
        }
    }
    
    private static List<ParameterInfo> getOutputParameterInfos(JobInfo jobInfo) {
        List<ParameterInfo> outs = new ArrayList<ParameterInfo>();
        ParameterInfo[] childParams = jobInfo.getParameterInfoArray();
        for (ParameterInfo childParam : childParams) {
            if (childParam.isOutputFile()) {
                //don't add taskLogs to the results
                boolean isTaskLog = PipelineHandler.isTaskLog(childParam);
                if (!isTaskLog) {
                    outs.add(childParam);
                }
            }
        }
        return outs;
    }
    
    private static boolean isTaskLog(ParameterInfo param) {
        if (param == null) {
            return false;
        }
        if (!param.isOutputFile()) {
            return false;
        }
        String name = param.getName();
        boolean isTaskLog = 
            name != null &&
            ( name.equals(GPConstants.TASKLOG) || 
                    name.endsWith(GPConstants.PIPELINE_TASKLOG_ENDING)
            );
        return isTaskLog;
    }

    private static String server = null;
    private static String getServer() {
        if (server != null) {
            return server;
        }
        //1) set server
        String gpUrl = System.getProperty("GenePatternURL");
        URL serverFromFile = null;
        try {
            serverFromFile = new URL(gpUrl);
        } 
        catch (MalformedURLException e) {
            log.error("Invalid GenePatternURL: " + gpUrl, e);
        }
        String host = serverFromFile.getHost();
        String port = "";
        int portNum = serverFromFile.getPort();
        if (portNum >= 0) {
            port = ":" + portNum;
        }
        server = serverFromFile.getProtocol() + "://" + host + port;
        return server;
    }

    private static boolean isFileType(File file, String fileFormat) {
        if (file.getName().toLowerCase().endsWith(".odf")) {
            return ODFModelType(file).equalsIgnoreCase(fileFormat);
        }
        // when fileFormat does not contain the '.' character, 
        // assume that fileFormat refers to a file extension and prepend '.'. 
        // For example if value of fileFormat is 'gct', fileFormat becomes '.gct' when testing if file.getName() ends with fileFormat
        // when the file format does contain the '.' character, 
        // assume fileFormat can refer to a complete filename (e.g. all_aml_train.gct).
        if (fileFormat.indexOf('.') == -1) {
            fileFormat = "." + fileFormat;
        }
        return file.getName().toLowerCase().endsWith(fileFormat.toLowerCase());
    }

    private static String ODFModelType(File file) {
        String model = "";
        BufferedReader inputB = null;
        try {
            if (!file.exists()) {
                log.error("Can't find " + file.getCanonicalPath());
            }
            // System.out.println(file.getCanonicalPath());
            inputB = new BufferedReader(new FileReader(file));
            String modelLine = inputB.readLine();
            while (modelLine != null && !modelLine.startsWith("Model")) {
                modelLine = inputB.readLine();
            }

            if (modelLine != null) {
                model = modelLine.substring(modelLine.indexOf("=") + 1).trim();
            }
        } 
        catch (IOException e) {
            log.error("Error reading " + file);
        } 
        finally {
            if (inputB != null) {
                try {
                    inputB.close();
                } 
                catch (IOException x) {
                }
            }
        }
        return model;
    }
    
    /**
     * Substitute '<LSID>' in input files. 
     * This is a special case for steps in a pipeline which use input files from the pipeline or from a previous step in the pipeline.
     *
     * Note: this substitution used to depend on a call to System.setProperty(LSID)
     *    This won't work now that pipelines are no longer executing in their own JVM 
     *    This property seems to have been set in GenePatternAnalysisTask 
     *    For some reason, pipelineModel.getLsid() is null
     *
     * Note: must be called after {@link #setInheritedJobParameters(ParameterInfo[], JobInfo[])}
     * 
     * @param parameterInfos
     */
    private static void substituteLsidInInputFiles(String lsidValue, ParameterInfo[] parameterInfos) {
        final String lsidTag = "<LSID>";
        final String gpUrlTag = "<GenePatternURL>";
        for (ParameterInfo param : parameterInfos) {
            String value = param.getValue();
            if (value != null && value.startsWith(gpUrlTag)) {
                // substitute <LSID> flags for pipeline files
                value = value.replace(lsidTag, lsidValue);
                param.setValue(value);
            }
        }        
    }
    
    /**
     * Look for parameters that are passed in on the command line and put them into the ParameterInfo array
     */
    private static ParameterInfo[] setJobParametersFromArgs(
            String name, 
            int taskNum, //this is the step number
            ParameterInfo[] parameterInfo, 
            Map args) 
    {
        for (int i = 0; i < parameterInfo.length; i++) {
            ParameterInfo aParam = parameterInfo[i];
            HashMap attributes = aParam.getAttributes();
            if (attributes != null) {
                if (attributes.get(PipelineModel.RUNTIME_PARAM) != null) {
                    attributes.remove(PipelineModel.RUNTIME_PARAM);
                    String key = name + taskNum + "." + aParam.getName();
                    String val = (String) args.get(key);                    
                    if ((val != null)) { 
                        //We don't want to double prefix the arguments.  
                        // If this was run from the GenePattern webpage, 
                        // the arguments will have been prefixed as the "run pipeline" job was run to get us here.
                        if (attributes.containsKey(GPConstants.PARAM_INFO_PREFIX[0])){
                            String prefix = (String) attributes.get(GPConstants.PARAM_INFO_PREFIX[0]);
                            if (val.startsWith(prefix)){
                                val = val.substring(prefix.length());
                            }
                        }
                        aParam.setValue(val);
                    }
                }
            }
        }
        return parameterInfo;
    }
}