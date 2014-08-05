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
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.GetIncludedTasks;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.MissingTasksException;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.data.pipeline.PipelineModelException;
import org.genepattern.server.JobManager;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationException;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.jobresult.JobResultFile;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.executor.AnalysisJobScheduler;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.executor.CommandManagerFactory;
import org.genepattern.server.executor.JobSubmissionException;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.jobqueue.JobQueue;
import org.genepattern.server.jobqueue.JobQueueUtil;
import org.genepattern.server.util.FindFileFilter;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.TaskInfoCache;
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
    
    /**
     * Initialize the pipeline and add the first job to the queue.
     */
    public static void startPipeline(JobInfo pipelineJobInfo, int stopAfterTask) throws CommandExecutorException {
        if (pipelineJobInfo == null) {
            throw new CommandExecutorException("Error starting pipeline, pipelineJobInfo is null");
        }        
        log.debug("starting pipeline: "+pipelineJobInfo.getTaskName()+" ["+pipelineJobInfo.getJobNumber()+"]");
        final boolean isInTransaction = HibernateUtil.isInTransaction(); //for debugging
        log.debug("isInTranscation="+isInTransaction);
        final boolean isScatterStep = isScatterStep(pipelineJobInfo);
        final boolean isParallelExec = isParallelExec(pipelineJobInfo);
        int numAddedJobs=-1;
        try {
            HibernateUtil.beginTransaction();
            List<JobInfo> addedJobs = runPipeline(pipelineJobInfo, stopAfterTask, isScatterStep);
            numAddedJobs = addedJobs.size();
            //set the parent pipeline's status to PROCESSING 
            AnalysisJobScheduler.setJobStatus(pipelineJobInfo.getJobNumber(), JobStatus.JOB_PROCESSING); 

            //add job records to the job_queue
            if (!isScatterStep && !isParallelExec) {
                //for standard pipelines, set the status of the first job to PENDING, the rest to WAITING
                JobQueue.Status status = JobQueue.Status.PENDING;
                for(final JobInfo jobInfo : addedJobs) {
                    JobQueueUtil.addJobToQueue( jobInfo,  status);
                    status = JobQueue.Status.WAITING;
                }
            }
            else if (isScatterStep) {
                //for scatter-gather pipelines, set the status of all jobs to PENDING
                for(final JobInfo jobInfo : addedJobs) {
                    JobQueueUtil.addJobToQueue( jobInfo,  JobQueue.Status.PENDING);
                }
            }
            else if (isParallelExec) {
                //start all of the jobs which have no dependencies on upstream jobs
                PipelineGraph graph = PipelineGraph.getDependencyGraph(pipelineJobInfo, addedJobs);
                Set<JobInfo> jobsToRun = graph.getJobsToRun();
                for(JobInfo jobToRun : jobsToRun) {
                    log.debug("adding job to queue, jobId="+jobToRun.getJobNumber()+", "+jobToRun.getTaskName());
                    JobQueueUtil.addJobToQueue( jobToRun,  JobQueue.Status.PENDING);
                } 
            }
            else {
                log.error("server config error: shouldn't be here!");
            }
            HibernateUtil.commitTransaction();
        }
        catch (Throwable t) {
            HibernateUtil.rollbackTransaction();
            throw new CommandExecutorException("Error starting pipeline: "+t.getLocalizedMessage(), t);
        }
        
        //special-case: a pipeline with zero steps
        if (numAddedJobs==0) {
            log.warn("no jobs added to pipeline: "+pipelineJobInfo.getJobNumber()+": "+pipelineJobInfo.getTaskName());
            AnalysisJobScheduler.setJobStatus(pipelineJobInfo.getJobNumber(), JobStatus.JOB_FINISHED);
            int parentParentJobId = pipelineJobInfo._getParentJobNumber();
            if (parentParentJobId >= 0) {
                //special-case: a nested pipeline with zero steps, make sure to notify the parent pipeline that this step has completed
                boolean wakeupJobQueue = PipelineHandler.handleJobCompletion(pipelineJobInfo);
                if (wakeupJobQueue) {
                    //if the pipeline has more steps, wake up the job queue
                    CommandManagerFactory.getCommandManager().wakeupJobQueue();
                }
            }
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
                CommandManagerFactory.getCommandManager().terminateJob(processingJobId);
            }
            catch (Throwable t) {
                log.error("Error terminating job #"+processingJobId+" in pipeline "+jobInfo.getJobNumber(), t);
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
        if (jobInfo==null) {
            throw new PipelineException("jobInfo==null");
        }
        log.debug("prepareNextStep(parentJobId="+parentJobId+", jobId="+jobInfo.getJobNumber()+", taskName="+jobInfo.getTaskName());
        try {
            AnalysisDAO dao = new AnalysisDAO();
            boolean updated = setInheritedParams(dao, parentJobId, jobInfo);
            if (updated) {
                updateParameterInfo(jobInfo);
                HibernateUtil.commitTransaction();
            }
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

    public static JobInfo getJobInfo(int jobId) {
        boolean inTransaction = HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            AnalysisDAO ds = new AnalysisDAO();
            JobInfo jobInfo = ds.getJobInfo(jobId);
            return jobInfo;
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
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
    public static boolean handleJobCompletion(final JobInfo completedJobInfo) {
        if (completedJobInfo==null) {
            log.error("completedJobInfo==null");
            return false;
        }
        JobInfo parentJobInfo=null;
        int parentJobId = -1;
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            AnalysisDAO ds = new AnalysisDAO();
            parentJobId = completedJobInfo._getParentJobNumber();
            if (parentJobId >= 0) {
                parentJobInfo = ds.getJobInfo(parentJobId);
            }
        }
        catch (Throwable t) {
            log.error("Unexpected exception in handleJobCompletion("+completedJobInfo.getJobNumber()+")", t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
        return handleJobCompletion(parentJobInfo, completedJobInfo);
    }
    
    private static boolean handleJobCompletion(JobInfo parentJobInfo, JobInfo completedJobInfo) {
        if (parentJobInfo == null) {
            return false;
        }
        final boolean isParallelExec = isParallelExec(parentJobInfo);
        if (isParallelExec) {
            return handleJobCompletionParallel(parentJobInfo, completedJobInfo);
        }
        else {
            return handleJobCompletionOld(parentJobInfo, completedJobInfo);
        }
    }

    //TODO: this method must be made thread-safe
    private static boolean handleJobCompletionParallel(final JobInfo pipelineJobInfo, final JobInfo childJobInfo) {
        boolean addedJobs=false;
        if (pipelineJobInfo==null) {
            throw new IllegalArgumentException("pipelineJobInfo==null");
        }
        if (childJobInfo==null) {
            throw new IllegalArgumentException("childJobInfo==null");
        }
        final int parentJobNumber=pipelineJobInfo.getJobNumber();
        final int childJobNumber=childJobInfo.getJobNumber();
        
        if (JobStatus.FINISHED.equals(childJobInfo.getStatus())) { 
            //rebuild the graph for the pipeline
            PipelineGraph graph = PipelineGraph.getDependencyGraph(pipelineJobInfo);
            
            boolean allStepsComplete=graph.allStepsComplete();
            if (allStepsComplete) {
                //it's the last step in the pipeline, update its status, and notify downstream jobs
                handlePipelineJobCompletion(parentJobNumber, 0);
                return false;
            }
            
            //otherwise, start any jobs which are ready
            Set<JobInfo> jobsToRun=graph.getJobsToRun();
            if (jobsToRun.size()>0) {
                try {
                    HibernateUtil.beginTransaction();
                    for(JobInfo jobToRun : jobsToRun) {
                        log.debug("adding job to queue, jobId="+jobToRun.getJobNumber()+", "+jobToRun.getTaskName());
                        try {
                            JobQueueUtil.addJobToQueue( jobToRun,  JobQueue.Status.PENDING);
                            addedJobs=true;
                        }
                        catch (Throwable t) {
                            //TODO: handle exception
                            log.error(t);
                        }
                    }
                    HibernateUtil.commitTransaction();
                }
                finally {
                    HibernateUtil.closeCurrentSession();
                }
            }
            return addedJobs;
        }
        else {
            //TODO: in some cases, we don't want to kill the parent pipeline or its child jobs
            //handle an error in a pipeline step
            terminatePipelineSteps(parentJobNumber);
            handlePipelineJobCompletion(parentJobNumber, -1, "Pipeline terminated because of an error in child job [id: "+childJobNumber+"]");
        }
        return false;
    }

    private static boolean handleJobCompletionOld(final JobInfo pipelineJobInfo, final JobInfo childJobInfo) {
        if (pipelineJobInfo==null) {
            throw new IllegalArgumentException("pipelineJobInfo==null");
        }
        if (childJobInfo==null) {
            throw new IllegalArgumentException("childJobInfo==null");
        }
        final int parentJobNumber=pipelineJobInfo.getJobNumber();
        final int childJobNumber=childJobInfo.getJobNumber();
        
        //change from the 3.3.3 implementation to support for parallel execution of scattered jobs
        // a) in scatter pipelines, more than one step in a pipeline gets started; we can't automatically flag the parent
        //    pipeline as complete until all of the steps are complete
        // b) in scatter pipelines, we don't want to terminate the parent pipeline, when one of the steps fails

        //check the status of the job
        if (JobStatus.FINISHED.equals(childJobInfo.getStatus())) { 

            //get the next step in the pipeline, by jobId
            int nextWaitingJob = -1;
            //boolean lastStepComplete = false;
            
            try {
                List<JobQueue> records = getWaitingJobs(parentJobNumber);
                if (records == null) {
                    log.error("Unexpected null result from getChildJobStatus, assuming all child steps are complete for parentJobNumber="+parentJobNumber);
                }
                //if (records == null || records.size() == 0) {
                //    lastStepComplete = true;
                //}
                if (records != null) {
                    for(JobQueue record : records) {
                        if (record.getStatus().equals( JobQueue.Status.WAITING.toString() )) {
                            if (nextWaitingJob == -1) {
                                nextWaitingJob = record.getJobNo();
                            }
                        }
                    }
                }
            }
            catch (Throwable t) {
                log.error(t);
            }
            
            if (nextWaitingJob >= 0) {
                //if there is another job to run, change the status of the next job to pending
                startNextStep(nextWaitingJob);
                //indicate there is another step waiting on the queue
                return true;
            }
            
            //no waiting jobs found, find out if the last step in the pipeline is complete
            HibernateUtil.beginTransaction();
            List<Object[]> jobInfoObjs = getChildJobObjs(parentJobNumber);
            HibernateUtil.closeCurrentSession();
            // the job is complete iff the list is empty
            boolean lastStepComplete = true;
            for(Object[] row : jobInfoObjs) {
                int statusId = (Integer) row[1];
                if (JobStatus.JOB_FINISHED != statusId && JobStatus.JOB_ERROR != statusId) {
                    lastStepComplete = false;
                }
            }

            if (lastStepComplete) {
                //it's the last step in the pipeline, update its status, and notify downstream jobs
                handlePipelineJobCompletion(parentJobNumber, 0);
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
    
    /**
     * Get the next step in the pipeline, by jobId.
     * @return
     */
    private static List<JobQueue> getWaitingJobs(int parentJobNo) throws Exception {
        List<JobQueue> records = JobQueueUtil.getWaitingJobs(parentJobNo);
        return records;
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
            JobQueueUtil.setJobStatus(nextJobId, JobQueue.Status.PENDING);
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
    
    /**
     * Add all of the steps in the pipeline as new jobs.
     * 
     * @param pipelineJobInfo
     * @param stopAfterTask
     * @param isScatterStep
     * 
     * @return - the list of added jobs
     * 
     * @throws PipelineModelException
     * @throws MissingTasksException
     * @throws JobSubmissionException
     */
    private static List<JobInfo> runPipeline(JobInfo pipelineJobInfo, int stopAfterTask, boolean isScatterStep) 
    throws PipelineModelException, MissingTasksException, JobSubmissionException
    {
        final boolean initIsAdmin=true;
        final GpContext userContext=GpContext.getContextForUser(pipelineJobInfo.getUserId(), initIsAdmin);

        final TaskInfo pipelineTaskInfo;
        try {
            pipelineTaskInfo=getTaskInfo(pipelineJobInfo.getTaskLSID());
        }
        catch (Throwable t) {
            log.error(t);
            throw new JobSubmissionException("Error getting task for pipeline: "+pipelineJobInfo.getTaskName()+" ["+pipelineJobInfo.getJobNumber()+"]");
        }
        
        
        //TODO: the checkForMissingTasks and the getPipelineModel call both initialize the list of tasks for the pipeline
        //    should refactor the code to avoid redundant calls
        checkForMissingTasks(userContext, pipelineTaskInfo);
        final PipelineModel pipelineModel = getPipelineModel(pipelineTaskInfo);
        final Vector<JobSubmission> tasks = pipelineModel.getTasks();

        //initialize the pipeline args
        Map<String,String> additionalArgs = new HashMap<String,String>();
        for(ParameterInfo param : pipelineJobInfo.getParameterInfoArray()) {
            additionalArgs.put(param.getName(), param.getValue());
        }

        List<JobInfo> addedJobs = new ArrayList<JobInfo>();
        if (!isScatterStep) {
            // add job records to the analysis_job table
            addedJobs = runPipeline(pipelineJobInfo, tasks, additionalArgs, stopAfterTask);
            return addedJobs;
        }
        else {
            addedJobs = runScatterPipeline(pipelineJobInfo, tasks, additionalArgs, stopAfterTask);
            return addedJobs;
        }
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
    private static List<JobInfo> runPipeline(final JobInfo pipelineJobInfo, final List<JobSubmission> tasks, final Map<String,String> additionalArgs, final int stopAfterTask) 
    throws PipelineModelException, MissingTasksException, JobSubmissionException
    {  
        log.debug("starting pipeline: "+pipelineJobInfo.getTaskName()+" ["+pipelineJobInfo.getJobNumber()+"]");

        int stepNum = 0;
        final List<JobInfo> submittedJobs = new ArrayList<JobInfo>();
        for(final JobSubmission jobSubmission : tasks) { 
            if (stepNum >= stopAfterTask) {
                // stop and execute no further
                break;
            }
            
            final ParameterInfo[] parameterInfo = jobSubmission.giveParameterInfoArray();
            substituteLsidInInputFiles(pipelineJobInfo.getTaskLSID(), parameterInfo);
            ParameterInfo[] params = parameterInfo;
            params = setJobParametersFromArgs(jobSubmission.getName(), stepNum + 1, params, additionalArgs);
            
            //TODO: the taskInfos are all initialized in the checkForMissingTasks, should use the same data structure,
            //    rather making another call
            final TaskInfo taskInfo;
            try {
                taskInfo = getTaskInfo(jobSubmission.getLSID());
            }
            catch (Throwable t) {
                throw new JobSubmissionException("Error initializing task from lsid="+jobSubmission.getLSID()+": "+t.getLocalizedMessage());
            }
            JobInfo submittedJob = addJobToPipeline(pipelineJobInfo.getJobNumber(), pipelineJobInfo.getUserId(), taskInfo, params);
            submittedJobs.add(submittedJob);
            ++stepNum;
        }
        return submittedJobs;
    }

    private static List<JobInfo> runScatterPipeline(final JobInfo pipelineJobInfo, final List<JobSubmission> tasks, final Map<String,String> additionalArgs, int stopAfterTask) 
    throws PipelineModelException, MissingTasksException, JobSubmissionException
    { 
        log.debug("starting scatter pipeline: "+pipelineJobInfo.getTaskName()+" ["+pipelineJobInfo.getJobNumber()+"]");
        
        int stepNum = 0;
        if (tasks.size() == 0)  {
            throw new JobSubmissionException("Don't know what to do with 0 tasks in pipelineModel");
        }
        if (tasks.size() > 1) {
            //TODO: fix this
            throw new JobSubmissionException("Only a single batch step is allowed, num steps = "+tasks.size());
        }

        //assuming a 1-step pipeline 
        // to avoid potential bug in pipelineModel, load the TaskInfo directly
        final JobSubmission jobSubmission=tasks.get(0);
        final TaskInfo taskInfo;
        try {
            taskInfo = getTaskInfo(jobSubmission.getLSID());
        }
        catch (Throwable t) {
            throw new JobSubmissionException("Error initializing task from lsid="+jobSubmission.getLSID()+": "+t.getLocalizedMessage());
        }
    
        ParameterInfo[] parameterInfo = tasks.get(0).giveParameterInfoArray();
        substituteLsidInInputFiles(pipelineJobInfo.getTaskLSID(), parameterInfo);
        ParameterInfo[] params = parameterInfo;
        params = setJobParametersFromArgs(tasks.get(0).getName(), stepNum + 1, params, additionalArgs);
        
        //for each step, if it has a batch input parameter, expand into a bunch of child steps
        Map<ParameterInfo,List<String>> scatterParamMap = new HashMap<ParameterInfo, List<String>>();
        for(ParameterInfo p : params) {
            boolean doScatter=false;
            doScatter=p.getValue().contains("?scatter"); //Note: must be first parameter in query string
            if (doScatter) {
                List<String> scatterParamValues = getScatterParamValues(p);
                if (scatterParamValues != null && scatterParamValues.size() > 0) {
                    scatterParamMap.put(p, scatterParamValues);
                }
                else { 
                    //special-case, if it's a scatter-param, and failOnEmpty is set, throw an error if no files match the selection rule
                    boolean isFailOnEmpty = isFailOnEmpty(p);
                    if (isFailOnEmpty) {
                        String errorMessage="No matching output files for scatter job, "+
                                "jobId="+pipelineJobInfo.getJobNumber()+", "+pipelineJobInfo.getTaskName()+" "+
                                p.getName()+"="+p.getValue();
                        throw new JobSubmissionException(errorMessage);
                    }
                } 
            }
        }
        
        List<JobInfo> submittedJobs = addScatterJobsToPipeline(pipelineJobInfo, taskInfo, params, scatterParamMap);
        return submittedJobs;
    }
    
    /**
     * rule: a scatter-step is any step in a pipeline which has at least one scatter-input-parameter.
     * @param jobInfo
     * @return
     */
    private static boolean isScatterStep(final JobInfo jobInfo) {
        // look for pipelines with a scatter-param
        ParameterInfo[] params = jobInfo.getParameterInfoArray();
        for(ParameterInfo param : params) {
            if (isScatterParam_beforeStartPipeline(param)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Experimental feature, circa GP 3.5.1, when enabled, run steps in parallel, which don't have other dependencies.
     * 
     * @param jobInfo
     * @return true, if child jobs should run in parallel
     * 
     * @deprecated
     */
    public static boolean isParallelExec(final JobInfo jobInfo) {
        GpContext jobContext=GpContext.getContextForJob(jobInfo);
        return isParallelExec(jobContext);
    }

    /**
     * @see isParallelExec(jobInfo)
     * 
     * @deprecated
     */
    public static boolean isParallelExec(GpContext jobContext) {
        boolean rval=ServerConfigurationFactory.instance().getGPBooleanProperty(jobContext, "org.genepattern.server.executor.pipeline.parallelExec", true);
        return rval;
    }

    /**
     * rule: a parameter is a scatter-input-parameter if the value of the INHERIT_FILENAME starts with '&scatter'.
     * rule: [not yet implemented] a parameter is a scatter-input parameter if the value of the IS_SCATTER_PARAM is true
     * 
     * Warning: because the Pipeline Engine modifies the ParameterInfo attributes at runtime, this method will incorrectly
     *     return false after the initial scatter job has been started.
     * 
     * @param param
     * @return
     */
    private static boolean isScatterParam_beforeStartPipeline(final ParameterInfo param) {
        HashMap attributes = param.getAttributes();
        final String taskStr = (String) attributes.get(PipelineModel.INHERIT_TASKNAME);
        final String fileStr = (String) attributes.get(PipelineModel.INHERIT_FILENAME);
        
        if (taskStr != null && fileStr != null && fileStr.startsWith("?scatter")) {
            return true;
        }
        return false;
    }

    /**
     * Determine if this scatter input parameter should cause the pipeline to fail
     * if there are no matching input parameters.
     * (failOnEmpty)
     * @param param
     * @return
     */
    private static boolean isFailOnEmpty(final ParameterInfo param) {
        if (param==null) {
            return false;
        }
        if (param.getValue()==null) {
            return false;
        }
        int idx=param.getValue().lastIndexOf("&failOnEmpty");
        if (idx>=0) {
            return true;
        }
        idx=param.getValue().lastIndexOf("?failOnEmpty");
        if (idx>=0) {
            return true;
        }
        return false;
    }

    /**
     * Get the TaskInfo for the given lsid.
     * @param lsid, must not be null
     * @return
     * @throws Exception
     */
    private static TaskInfo getTaskInfo(String lsid) throws Exception {
        if (lsid == null) {
            throw new IllegalArgumentException("lsid == null");
        }
        boolean isInTransaction = HibernateUtil.isInTransaction();
        try {
            return TaskInfoCache.instance().getTask(lsid);
        }
        catch (Throwable t) {
            throw new Exception(t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

    /**
     * Get the PipelineModel from the given TaskInfo.
     * 
     * @param taskInfo, must not be null, must be for a pipeline.
     * @return
     * @throws PipelineModelException
     */
    private static PipelineModel getPipelineModel(TaskInfo taskInfo) throws PipelineModelException
    {
        if (taskInfo == null) {
            throw new IllegalArgumentException("taskInfo is null");
        }
        TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
        if (tia == null) {
            throw new PipelineModelException("taskInfo.giveTaskInfoAttributes is null for taskInfo.ID="+taskInfo.getID()+", taskInfo.name="+taskInfo.getName());
        }
        String serializedModel = (String) tia.get(GPConstants.SERIALIZED_MODEL);
        if (serializedModel == null || serializedModel.length() == 0) {
            throw new PipelineModelException("Missing "+GPConstants.SERIALIZED_MODEL+" for taskInfo.ID="+taskInfo.getID()+", taskInfo.name="+taskInfo.getName());
        }
        PipelineModel model = null;
        try {
            model = PipelineModel.toPipelineModel(serializedModel);
        } 
        catch (Throwable t) {
            throw new PipelineModelException(t);
        }
        if (model == null) {
            throw new PipelineModelException("pipeline model is null for taskInfo.ID="+taskInfo.getID()+", taskInfo.name="+taskInfo.getName());
        }
        model.setLsid(taskInfo.getLsid());
        return model;
    }
    
    /**
     * Get the list of scatter parameter values for the given input parameter.
     * 
     * If the param is a scatter parameter, return the list of all values to be used as input to 
     * each new scattered sub job. One new job should be added to the pipeline for each
     * value in the returned list.
     * 
     * A null or empty list return value means that the param is not a scatter parameter.
     * 
     * @param param
     * @return a list of values or null
     * @throws JobSubmissionException
     */
    private static List<String> getScatterParamValues(ParameterInfo param) throws JobSubmissionException {
        //note: currently implemented with ad-hoc rules for determining if it's a scatter - parameter
        if (!param.isInputFile()) {
            return null;
        }

        String value = param.getValue();
        String queryString = null;
        int idx = value.lastIndexOf("?scatter");
        if (idx >= 0) {
            //TODO: use standard http methods for extracting the parameters from the query string
            queryString = value.substring(idx);
            value = value.substring(0, idx);
        }
        JobResultFile gpFilePath = null;
        try {
            gpFilePath = GpFileObjFactory.getRequestedJobResultFileObj(value);
        }
        catch (Exception e) {
            //this is to be expected ... 
            // ... not all input parameters will map to JobResultFile objects
            return null;
        }

        //parse the query string, for optional filter pattern
        FileFilter fileFilter = null;
        if (queryString != null && queryString.startsWith("?scatter")) {
            //get the optional filter param
            int idx0=queryString.indexOf("filter=");
            if (idx0>=0) {
                idx0+="filter=".length();
                int idx1=queryString.indexOf("&", idx0);
                if (idx1<idx0) {
                    idx1=queryString.length();
                }
                String filter=queryString.substring(idx0, idx1);
                fileFilter = createFilterFromPattern(filter);
            }
        }
        
        //rule 1: it's a scatter param if the value is a file whose name ends in 'filelist.txt'
        //    In this case, create one new sub job for each path in the file list file
        if (gpFilePath.isFile() && gpFilePath.getName().endsWith("filelist.txt")) {
            try {
                List<String> inputFiles = parseFileList( gpFilePath.getServerFile() );
                return inputFiles;
            }
            catch (Throwable t) {
                    throw new JobSubmissionException("Error reading filelist from file: "+param.getValue(), t);
            }
        }

        //rule 2: it's a scatter param if the value is a reference to a job result directory for a particular job
        //    In this case, create one new sub job for each job result file
        if (gpFilePath.isDirectory() && gpFilePath.isWorkingDir()) {
            try {
                String jobId = gpFilePath.getJobId();
                AnalysisDAO dao = new AnalysisDAO();
                JobInfo jobInfo = dao.getJobInfo(Integer.parseInt(jobId));
                List<ParameterInfo> outputFiles = getOutputParameterInfos(jobInfo);
                List<String> rval = new ArrayList<String>();
                for(ParameterInfo outputFile : outputFiles) {
                    //JobResultFile resultFile = new JobResultFile(jobInfo, outputFile);
                    JobResultFile resultFile = new JobResultFile(outputFile);
                    String urlStr = resultFile.getUrl().toString();
                    boolean accept = true;
                    if (fileFilter != null) {
                        accept = fileFilter.accept(resultFile.getServerFile());
                    }
                    if (accept) {
                        rval.add( urlStr );
                    }
                }
                //always sorted alphabetically
                Collections.sort(rval);
                return rval;
            }
            catch (Throwable t) {
                //expected
                log.debug(t);
            }
        }

        return null;
    }
    
    /**
     * Create a FileFilter from a comma-separated list of glob patterns.
     * 
     * A globPattern is one of,
     * a) a glob, e.g. "*.cls", or
     * b) a list of globs, e.g. "*.gct,*.cls", or
     * c) an anti-glob, e.g. "!*.gct"
     * 
     * @param globPattern
     */
    private static FileFilter createFilterFromPattern(String globPattern) {
        FindFileFilter includeFileFilter = new FindFileFilter();
        String[] globList = globPattern.split(",");
        for(String globSpec : globList) {
            includeFileFilter.addGlob(globSpec);
        }
        return includeFileFilter;
    }

    private static List<JobInfo> addScatterJobsToPipeline(JobInfo pipelineJobInfo, TaskInfo taskInfo, ParameterInfo[] params, Map<ParameterInfo,List<String>> scatterParamMap) 
    throws JobSubmissionException
    {
        log.debug("adding scatter jobs to pipeline, "+pipelineJobInfo.getTaskName()+"["+pipelineJobInfo.getJobNumber()+"] ... ");
        List<JobInfo> submittedJobs = new ArrayList<JobInfo>();
        //validate the number of batch jobs to submit
        List<List<String>> row = new ArrayList<List<String>>();
        int jobCount = -1;
        List<Integer> jobCounts = new ArrayList<Integer>();
        for(Entry<ParameterInfo, List<String>> entry : scatterParamMap.entrySet()) {
            List<String> values = new ArrayList<String>();
            row.add(values);
            ParameterInfo scatterParam = entry.getKey();
            int numJobs = 0;
            for(String scatterParamValue : entry.getValue()) {
                values.add( scatterParamValue );
                ++numJobs;
            }
            jobCounts.add(numJobs);
            if (jobCount == -1) {
                jobCount = numJobs;
            }
            else {
                if (jobCount != numJobs) {
                    throw new IllegalArgumentException("Mismatch of batch input files: maxJobCount="+jobCount+", jobCount for batch param, "+scatterParam.getName()+", is "+numJobs );
                }
            }
        }

        //don't create a scatter jobs if there are no matching input parameters
        if (jobCount <= 0) {
            // 
            log.warn("No batch jobs to submit, for pipeline jobId="+pipelineJobInfo.getJobNumber()+", task="+pipelineJobInfo.getTaskName());
            return Collections.emptyList();
        }
        
        log.debug("\tsubmitting "+jobCount+" scatter jobs...");
        for(int job_idx = 0; job_idx < jobCount; ++job_idx) {
            for(ParameterInfo scatterParam : scatterParamMap.keySet()) {
                String batchParamValue = scatterParamMap.get(scatterParam).get(job_idx);
                scatterParam.setValue(batchParamValue);
                log.debug("\t\tstep "+job_idx+": "+scatterParam.getName()+"="+scatterParam.getValue());
            }
            JobInfo submittedJob = addJobToPipeline(pipelineJobInfo.getJobNumber(), pipelineJobInfo.getUserId(), taskInfo, params);
            submittedJobs.add(submittedJob);
        }
        return submittedJobs;
    }
    
    public static List<String> parseFileList(File filelist) throws Exception {
        log.debug("Reading filelist from: "+filelist.getPath());
        List<String> inputValues = new ArrayList<String>();
        if (!filelist.canRead()) {
            throw new Exception("Can't read filelist, "+filelist.getPath());
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
    private static JobInfo addJobToPipeline(int parentJobId, String userID, TaskInfo taskInfo, ParameterInfo[] params)
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
                        // GP-4191: no longer need to replace the original value with the file#toURI 
                        final boolean keepOriginalValue=true;
                        if (!keepOriginalValue) {
                            String val = file;
                            try {
                                new URL(file);
                            } 
                            catch (MalformedURLException e) {
                                val = new File(file).toURI().toString();
                            }
                            params[i].setValue(val);
                        }
                        params[i].getAttributes().remove("TYPE");
                        params[i].getAttributes().remove("MODE");
                    }
                }
            }
        }

        //make sure to commit db changes
        JobInfo jobInfo = addJobToQueue(taskInfo, userID, params, parentJobId);
        return jobInfo;
    }

    private static void checkForMissingTasks(final GpContext userContext, final TaskInfo forTask) throws MissingTasksException {
        final GetIncludedTasks taskChecker;
        try {
            taskChecker=new GetIncludedTasks(userContext, forTask);
        }
        catch (Throwable t) {
            log.error(t);
            return;
        }
        
        if (taskChecker.allTasksAvailable()) {
            return;
        }

        MissingTasksException ex = new MissingTasksException();
        for(final JobSubmission jobSubmission : taskChecker.getMissingJobSubmissions()) {
            ex.addError(MissingTasksException.Type.NOT_FOUND, jobSubmission);
        }
        for(final TaskInfo privateTask : taskChecker.getPrivateTasks()) {
            ex.addError(MissingTasksException.Type.PERMISSION_ERROR, privateTask.getName(), privateTask.getLsid());
        }
        if (ex.hasErrors()) {
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
        if (currentStep==null) {
            throw new IllegalArgumentException("currentStep == null");
        }
        log.debug("setInheritedParams(parentJobId="+parentJobId+", currentStep="+currentStep.getJobNumber()+", task="+currentStep.getTaskName());
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
                log.debug("isInheritTaskName, jobId="+currentStep.getJobNumber()+", "+currentStep.getTaskName()+"."+param.getName()+"="+param.getValue());
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
        log.debug("getInheritedFilename for inputParam="+inputParam.getName());
        // these params must be removed so that the soap lib doesn't try to send the file as an attachment
        HashMap attributes = inputParam.getAttributes();
        final String taskStr = (String) attributes.get(PipelineModel.INHERIT_TASKNAME);
        final String fileStr = (String) attributes.get(PipelineModel.INHERIT_FILENAME);
        log.debug("taskStr="+taskStr);
        log.debug("fileStr="+fileStr);
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
        
        log.debug("stepNum="+stepNum);
        JobInfo fromJob = childJobs[stepNum];
        if (fromJob==null) {
            log.debug("fromJob==null");
        }
        else {
            log.debug("fromJob.id="+fromJob.getJobNumber()+", "+fromJob.getTaskName());
        }
        String fileName = null;
        try {
            fileName = getOutputFileName(dao, fromJob, fileStr);
            log.debug("outputFileName="+fileName);
            if (fileName == null || fileName.trim().length() == 0) {
                //return an empty string if no output filename is found
                return "";
            }
            attributes.put(ParameterInfo.MODE, ParameterInfo.URL_INPUT_MODE);
            String context = System.getProperty("GP_Path", "/gp");
            //special-case: handle space ' ' char in filename
            //TODO: make this more robust by using a standard method for transforming server files to URLs
            fileName = fileName.replaceAll(" ", "%20");
            String url = getServer() + context + "/jobResults/" + fileName;
            return url;
        }
        catch (Exception e) {
            log.error(e.getLocalizedMessage());
            return "";            
        }
    }
    
    private static String getOutputFileName(AnalysisDAO dao, JobInfo fromJob, String fileStr)
    throws ServerConfigurationException, FileNotFoundException 
    {
        if (fromJob==null) {
            throw new IllegalArgumentException("fromJob==null");
        }
        log.debug("getOutputFileName, fromJob="+fromJob.getJobNumber()+"."+fromJob.getTaskName()+", fileStr="+fileStr);
        //special-case: use 'stdout' from previous job
        if ("stdout".equals(fileStr)) {
            //use STDOUT from Job
            ParameterInfo stdoutParam = getStdoutFile(fromJob);
            String fileName = null;
            if (stdoutParam != null) {
                fileName = stdoutParam.getValue();
            }
            return fileName;
        }
        //special-case: use 'stderr' from previous job
        if ("stderr".equals(fileStr)) {
            //use STDERR from Job
            ParameterInfo stderrParam = getStderrFile(fromJob);
            String fileName = null;
            if (stderrParam != null) {
                fileName = stderrParam.getValue();
            }
            return fileName;
        }
        
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
            //TODO: can't get job results from sub-directories because of this code
            int lastIdx = fileName.lastIndexOf(File.separator);
            lastIdx = fileName.lastIndexOf(File.separator, lastIdx - 1);
            if (lastIdx != -1) {
                fileName = fileName.substring(lastIdx + 1);
            }
            return fileName;
        }

        try {
            String fileNameFromPattern = getOutputFileNameFromPattern(fromJob, fileStr);
            if (fileNameFromPattern != null) {
                return fileNameFromPattern;
            }
        }
        catch (Exception e) {
            log.error(e);
        }

        if (fileName == null) {
            throw new FileNotFoundException("Unable to find output file from job " + fromJob.getJobNumber() + " that matches " + fileStr + ".");
        }
        return fileName;
    }
    
    /**
     * Added this method to support scatter-gather pipelines.
     * See the Pipeline Designer, which generates the values that we are checking for in this method.
     * 
     *     ctl.options[ctl.options.length]  = new Option('scatter each output', '?scatter&filter=*');
     *     ctl.options[ctl.options.length]  = new Option('file list of all outputs', '?filelist&filter=*');
     * 
     * @return
     */
    private static String getOutputFileNameFromPattern(JobInfo fromJob, String fileStr) throws Exception {
        if (fileStr.startsWith("?scatter")) {
            //it's a scatter-step
            return fromJob.getJobNumber() + fileStr;
        }
        if (fileStr.startsWith("?filelist")) {
            //it's a gather-step, write the filelist as an output of the fromJob 
            GpFilePath filelist = writeFileList(fromJob, fileStr);
            return fromJob.getJobNumber() + "/" + filelist.getRelativePath();
        } 
        //unknown
        return null;
    }
    
    //fileStr=?filelist[&filter=<list of patterns>]
    private static GpFilePath writeFileList(JobInfo fromJob, String fileStr) throws Exception {
        boolean isInTransaction = false;
        AnalysisDAO dao = null;
        try {
            isInTransaction = HibernateUtil.isInTransaction();
            dao = new AnalysisDAO();
            List<ParameterInfo> allResultFiles = getOutputFilesRecursive(dao, fromJob); 
            
            //let's see if there's a filter
            FileFilter fileFilter = null;
            int idx = fileStr.indexOf("&filter=");
            if (idx >= 0) {
                int endIdx = fileStr.indexOf("&", 1+idx);
                if (endIdx < 0) {
                    endIdx = fileStr.length();
                }
                String filterStr = fileStr.substring(idx + "&filter=".length(), endIdx);
                fileFilter = createFilterFromPattern(filterStr);
            }
            
            //let's see if there's a filename
            String filename = "all.filelist.txt";
            idx = fileStr.indexOf("&filename=");
            if (idx >= 0) {
                int endIdx = fileStr.indexOf("&", idx+1);
                if (endIdx < 0) {
                    endIdx = fileStr.length();
                }
                filename = fileStr.substring(idx + "&filename=".length(), endIdx);
            }

            GatherResults gatherResults = new GatherResults(fromJob, allResultFiles); 
            GpFilePath filelist = gatherResults.writeFilelist(filename, fileFilter);
            return filelist;            
            //TODO: add the filelist to the fromJob
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
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
     * @throws ServerConfigurationException
     * @throws FileNotFoundException
     */
    private static String getOutputFilenameByType(final JobInfo fromJob, final String fileStr, List<ParameterInfo> jobResultFiles) 
    throws ServerConfigurationException, FileNotFoundException
    {
        String fileName = null;
        
        GpContext context = GpContext.getContextForJob(fromJob);
        File rootJobDir = ServerConfigurationFactory.instance().getRootJobDir(context);
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
            log.error("old version of pipeline: '"+fileStr+"' is deprecated");
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
    
    public static final List<ParameterInfo> getOutputParameterInfos(JobInfo jobInfo) {
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
    
    private static ParameterInfo getStdoutFile(JobInfo jobInfo) {
        ParameterInfo[] childParams = jobInfo.getParameterInfoArray();
        for (ParameterInfo childParam : childParams) {
            if (childParam._isStdoutFile()) {
                return childParam;
            }
        }
        return null;
    }

    private static ParameterInfo getStderrFile(JobInfo jobInfo) {
        ParameterInfo[] childParams = jobInfo.getParameterInfoArray();
        for (ParameterInfo childParam : childParams) {
            if (childParam._isStderrFile()) {
                return childParam;
            }
        }
        return null;
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
    public static void substituteLsidInInputFiles(String lsidValue, ParameterInfo[] parameterInfos) {
        final String lsidTag = "<LSID>";
        final String gpUrlTag = "<GenePatternURL>";
        for (ParameterInfo param : parameterInfos) {
            String value = param.getValue();
            if (value != null && value.startsWith(gpUrlTag)) {
                // substitute <GenePatternURL> with actual value
                value = value.replace(gpUrlTag, System.getProperty("GenePatternURL"));
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
    
    //custom implementation of JobManager code
    /**
     * Adds a new job entry to the ANALYSIS_JOB table.
     * 
     * Don't forget to add a record to the internal job queue, for dispatching ...
     *     JobQueueUtil.addJobToQueue( jobInfo, initialJobStatus );
     * 
     * @param taskID
     * @param userID
     * @param parameterInfoArray
     * @param parentJobID
     * @param jobStatusId
     * @return
     * @throws JobSubmissionException
     */
    static private JobInfo addJobToQueue(final TaskInfo taskInfo, final String userId, final ParameterInfo[] parameterInfoArray, final Integer parentJobNumber) 
    throws JobSubmissionException
    {
        JobInfo jobInfo = null;
        final boolean isInTransaction = HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            AnalysisJob newJob = addNewJob(userId, taskInfo, parameterInfoArray, parentJobNumber);
            jobInfo = new JobInfo(newJob);
            JobManager.createJobDirectory(jobInfo); 
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
            return jobInfo;
        }
        catch (Throwable t) {
            HibernateUtil.rollbackTransaction();
            if (t instanceof JobSubmissionException) {
                throw (JobSubmissionException) t;
            }
            throw new JobSubmissionException(t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

    private static AnalysisJob addNewJob(String userId, TaskInfo taskInfo, ParameterInfo[] parameterInfoArray, Integer parentJobNumber) 
    throws JobSubmissionException
    { 
        if (taskInfo == null) {
            throw new JobSubmissionException("Error adding job to queue, taskInfo is null");
        }
        
        if (taskInfo.getID() < 0) {
            throw new JobSubmissionException("Error adding job to queue, invalid taskId, taskInfo.getID="+taskInfo.getID());
        }
        
        final boolean isInTransaction = HibernateUtil.isInTransaction();
        try {
            String parameter_info = ParameterFormatConverter.getJaxbString(parameterInfoArray);

            AnalysisJob aJob = new AnalysisJob();
            aJob.setTaskId(taskInfo.getID());
            aJob.setSubmittedDate(Calendar.getInstance().getTime());
            aJob.setParameterInfo(parameter_info);
            aJob.setUserId(userId);
            aJob.setTaskName(taskInfo.getName());
            aJob.setParent(parentJobNumber);
            aJob.setTaskLsid(taskInfo.getLsid());
            
            JobStatus js = new JobStatus();
            js.setStatusId(JobStatus.JOB_PENDING);
            js.setStatusName(JobStatus.PENDING);
            aJob.setJobStatus(js);
            
            HibernateUtil.getSession().save(aJob);
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
            return aJob;
        }
        catch (Throwable t) {
            HibernateUtil.closeCurrentSession();
            throw new JobSubmissionException("Error adding job to queue, taskId="+taskInfo.getID()+
                    ", taskName="+taskInfo.getName()+
                    ", taskLsid="+taskInfo.getLsid(), t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

}