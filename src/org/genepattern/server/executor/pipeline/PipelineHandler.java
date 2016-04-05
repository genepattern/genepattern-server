/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.pipeline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationException;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.dm.jobresult.JobResultFile;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.executor.AnalysisJobScheduler;
import org.genepattern.server.executor.BasicCommandManager;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.executor.CommandManagerFactory;
import org.genepattern.server.executor.JobSubmissionException;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.job.input.GroupId;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamId;
import org.genepattern.server.job.input.ParamValue;
import org.genepattern.server.job.input.dao.JobInputValueRecorder;
import org.genepattern.server.jobqueue.JobQueue;
import org.genepattern.server.jobqueue.JobQueueUtil;
import org.genepattern.server.util.FindFileFilter;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
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
     * @param mgr
     * @param jobContext
     * @param stopAfterTask
     * @throws CommandExecutorException
     */
    public static void startPipeline(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext jobContext, final int stopAfterTask) throws CommandExecutorException {
        if (jobContext==null) {
            throw new CommandExecutorException("jobContext==null");
        }
        final JobInfo pipelineJobInfo=jobContext.getJobInfo();
        if (pipelineJobInfo == null) {
            throw new CommandExecutorException("jobContext.jobInfo==null");
        }
        final TaskInfo pipelineTaskInfo=jobContext.getTaskInfo();
        if (pipelineTaskInfo==null) {
            throw new CommandExecutorException("jobContext.taskInfo==null");
        }
        PipelineModel pipelineModel=null;
        try {
            // Note: the checkForMissingTasks and the getPipelineModel call both initialize the list of tasks for the pipeline
            if (log.isDebugEnabled()) {
                log.debug("checking for missing tasks ...");
            }
            pipelineModel=checkForMissingTasks(mgr, jobContext, pipelineTaskInfo);
        }
        catch (MissingTasksException e) {
            throw new CommandExecutorException(e.getMessage());
        }
        catch (Throwable t) {
            throw new CommandExecutorException("Unexpected error checking for missing tasks: "+t.getLocalizedMessage(), t);
        } 
        
        if (log.isDebugEnabled()) {
            log.debug("starting pipeline: "+pipelineJobInfo.getTaskName()+" ["+pipelineJobInfo.getJobNumber()+"]");
        }
        final boolean isScatterStep = isScatterStep(pipelineJobInfo);
        final boolean isParallelExec = isParallelExec(pipelineJobInfo);
        int numAddedJobs=-1;
        try {
            mgr.beginTransaction();
            List<JobInfo> addedJobs = runPipeline(mgr, gpConfig, jobContext, pipelineModel, stopAfterTask, isScatterStep);
            numAddedJobs = addedJobs.size();
            //set the parent pipeline's status to PROCESSING 
            AnalysisJobScheduler.setJobStatus(mgr, pipelineJobInfo.getJobNumber(), JobStatus.JOB_PROCESSING); 

            //add job records to the job_queue
            if (!isScatterStep && !isParallelExec) {
                //for standard pipelines, set the status of the first job to PENDING, the rest to WAITING
                JobQueue.Status status = JobQueue.Status.PENDING;
                for(final JobInfo jobInfo : addedJobs) {
                    JobQueueUtil.addJobToQueue(mgr, jobInfo,  status);
                    status = JobQueue.Status.WAITING;
                }
            }
            else if (isScatterStep) {
                //for scatter-gather pipelines, set the status of all jobs to PENDING
                for(final JobInfo jobInfo : addedJobs) {
                    JobQueueUtil.addJobToQueue(mgr, jobInfo,  JobQueue.Status.PENDING);
                }
            }
            else if (isParallelExec) {
                //start all of the jobs which have no dependencies on upstream jobs
                PipelineGraph graph = PipelineGraph.getDependencyGraph(pipelineJobInfo, addedJobs);
                Set<JobInfo> jobsToRun = graph.getJobsToRun();
                for(JobInfo jobToRun : jobsToRun) {
                    log.debug("adding job to queue, jobId="+jobToRun.getJobNumber()+", "+jobToRun.getTaskName());
                    JobQueueUtil.addJobToQueue(mgr, jobToRun,  JobQueue.Status.PENDING);
                } 
            }
            else {
                log.error("server config error: shouldn't be here!");
            }
            mgr.commitTransaction();
        }
        catch (Throwable t) {
            mgr.rollbackTransaction();
            throw new CommandExecutorException("Error starting pipeline: "+t.getLocalizedMessage(), t);
        }
        
        //special-case: a pipeline with zero steps
        if (numAddedJobs==0) {
            log.warn("no jobs added to pipeline: "+pipelineJobInfo.getJobNumber()+": "+pipelineJobInfo.getTaskName());
            AnalysisJobScheduler.setJobStatus(mgr, pipelineJobInfo.getJobNumber(), JobStatus.JOB_FINISHED);
            int parentParentJobId = pipelineJobInfo._getParentJobNumber();
            if (parentParentJobId >= 0) {
                //special-case: a nested pipeline with zero steps, make sure to notify the parent pipeline that this step has completed
                boolean wakeupJobQueue = PipelineHandler.handleJobCompletion(mgr, pipelineJobInfo);
                if (wakeupJobQueue) {
                    //if the pipeline has more steps, wake up the job queue
                    CommandManagerFactory.getCommandManager().wakeupJobQueue();
                }
            }
        }
    }

    public static void terminatePipeline(final HibernateSessionManager mgr, final JobInfo jobInfo) {
        if (jobInfo == null) {
            log.error("Ignoring null arg");
            return;
        }
        
        // handle special-case: pipeline already finished before user terminated event was processed
        if (JobStatus.ERROR.equals(jobInfo.getStatus()) || JobStatus.FINISHED.equals(jobInfo.getStatus())) {
            log.info("job #"+jobInfo.getJobNumber()+" already finished, status="+jobInfo.getStatus());
            return;
        }
        
        // terminate processing jobs
        mgr.beginTransaction();
        List<Object[]> jobInfoObjs = getChildJobObjs(mgr, jobInfo.getJobNumber());
        mgr.closeCurrentSession();
        
        List<Integer> processingJobIds=new ArrayList<Integer>();
        for(Object[] row : jobInfoObjs) {
            int jobId = (Integer) row[0];
            int statusId = (Integer) row[1];
            if (isProcessingByStatusId(statusId)) {
                processingJobIds.add(jobId);
            }
        }
        for(final Integer processingJobId : processingJobIds) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("terminating pipeline step, job #"+processingJobId);
                }
                final BasicCommandManager cmdMgr=CommandManagerFactory.getCommandManager();
                cmdMgr.terminateJob(processingJobId);
            }
            catch (Throwable t) {
                log.error("Error terminating job #"+processingJobId+" in pipeline "+jobInfo.getJobNumber(), t);
            }
        }
        if (processingJobIds.size() == 0) {
            terminatePipelineSteps(mgr, jobInfo.getJobNumber());
            handlePipelineJobCompletion(mgr, jobInfo.getJobNumber(), -1, "Job #"+jobInfo.getJobNumber()+" terminated by user.");
        }
    }
    
    protected static boolean isProcessingByStatusId(int statusId) {
        return statusId==JobStatus.JOB_PROCESSING;
    }
    
    protected static boolean isFinishedByStatusId(int statusId) {
        return statusId==JobStatus.JOB_FINISHED || statusId==JobStatus.JOB_ERROR;
    }
    
    /**
     * 
     * @param mgr
     * @param gpConfig
     * @param jobContext, the job context for a job which is about to be started as a step in a pipeline.
     */
    public static void prepareNextStep(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext jobContext) throws PipelineException {
        final JobInfo currentStep=jobContext.getJobInfo();
        if (currentStep==null) {
            throw new PipelineException("currentStep==null");
        }
        final int parentJobId=currentStep._getParentJobNumber();
        if (log.isDebugEnabled()) {
            log.debug("prepareNextStep(parentJobId="+parentJobId+", currentStepJobId="+currentStep.getJobNumber()+", taskName="+currentStep.getTaskName());
        }
        final List<ParameterInfo> inheritedInputParams = getInheritedInputParams(currentStep);
        if (inheritedInputParams.size()==0) {
            return;
        }
        
        final JobInput currentJobInput=jobContext.getJobInput();
        if (currentJobInput==null) {
            throw new PipelineException("currentStep.jobInput==null");
        }

        try {
            final AnalysisDAO dao = new AnalysisDAO(mgr);
            final JobInfo[] childJobs = dao.getChildren(parentJobId);
            final Map<String,String> updatedValues=updateInheritedParamValues(mgr, dao, childJobs, inheritedInputParams);
            if (updatedValues.size()>0) {
                updateParameterInfo(mgr, currentStep);
                final JobInput updatedJobInput=new JobInput();
                for(final Entry<String,String> entry : updatedValues.entrySet()) {
                    currentJobInput.setValue(entry.getKey(), entry.getValue());
                    updatedJobInput.addValue(entry.getKey(), entry.getValue());
                }
                new JobInputValueRecorder(mgr).saveJobInput(currentStep.getJobNumber(), updatedJobInput);
                mgr.commitTransaction();
            }
        }
        catch (Throwable t) {
            mgr.rollbackTransaction();
            throw new PipelineException("Error preparing next step in pipeline job #"+parentJobId+" for step #"+currentStep.getJobNumber(), t);
        }
        finally {
            mgr.closeCurrentSession();
        }
    }
    
    /**
     * Helper method (could go into AnalysisDAO) for saving edits to the ANALYSIS_JOB.PARAMETER_INFO for a given job.
     */
    private static void updateParameterInfo(final HibernateSessionManager mgr, final JobInfo jobInfo) {
        final int jobId = jobInfo.getJobNumber();
        final String paramString = jobInfo.getParameterInfo();
        AnalysisJob aJob = (AnalysisJob) mgr.getSession().get(AnalysisJob.class, jobId);
        aJob.setParameterInfo(paramString);
        mgr.getSession().update(aJob);
    }
    
    //DAO helpers
    /**
     * Get all of the immediate child jobs for the given pipeline.
     * 
     * @return a List of [jobNo(Integer),statusId(Integer)]
     */
    private static List<Object[]> getChildJobObjs(final HibernateSessionManager mgr, final int parentJobId) {
        //TODO: hard-coded so that no more than 10000 batch jobs can be handled
        final int maxChildJobs = 10000; //limit total number of results to 10000
        String hql = "select a.jobNo, jobStatus.statusId from org.genepattern.server.domain.AnalysisJob a where a.parent = :jobNo ";
        hql += " ORDER BY jobNo ASC";
        Query query = mgr.getSession().createQuery(hql);
        query.setInteger("jobNo", parentJobId);
        query.setFetchSize(maxChildJobs);
        
        @SuppressWarnings("unchecked")
        List<Object[]> rval = query.list();
        return rval;
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
    public static boolean handleJobCompletion(final HibernateSessionManager mgr, final JobInfo completedJobInfo) {
        if (completedJobInfo==null) {
            log.error("completedJobInfo==null");
            return false;
        }
        JobInfo parentJobInfo=null;
        int parentJobId = -1;
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            AnalysisDAO ds = new AnalysisDAO(mgr);
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
                mgr.closeCurrentSession();
            }
        }
        return handleJobCompletion(mgr, parentJobInfo, completedJobInfo);
    }
    
    private static boolean handleJobCompletion(final HibernateSessionManager mgr, final JobInfo parentJobInfo, final JobInfo completedJobInfo) {
        if (parentJobInfo == null) {
            return false;
        }
        final boolean isParallelExec = isParallelExec(parentJobInfo);
        if (isParallelExec) {
            return handleJobCompletionParallel(mgr, parentJobInfo, completedJobInfo);
        }
        else {
            return handleJobCompletionOld(mgr, parentJobInfo, completedJobInfo);
        }
    }

    //TODO: this method must be made thread-safe
    private static boolean handleJobCompletionParallel(final HibernateSessionManager mgr, final JobInfo pipelineJobInfo, final JobInfo childJobInfo) {
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
            PipelineGraph graph = PipelineGraph.getDependencyGraph(mgr, pipelineJobInfo);
            
            boolean allStepsComplete=graph.allStepsComplete();
            if (allStepsComplete) {
                //it's the last step in the pipeline, update its status, and notify downstream jobs
                handlePipelineJobCompletion(mgr, parentJobNumber, 0);
                return false;
            }
            
            //otherwise, start any jobs which are ready
            Set<JobInfo> jobsToRun=graph.getJobsToRun();
            if (jobsToRun.size()>0) {
                try {
                    mgr.beginTransaction();
                    for(JobInfo jobToRun : jobsToRun) {
                        log.debug("adding job to queue, jobId="+jobToRun.getJobNumber()+", "+jobToRun.getTaskName());
                        try {
                            JobQueueUtil.addJobToQueue(mgr, jobToRun,  JobQueue.Status.PENDING);
                            addedJobs=true;
                        }
                        catch (Throwable t) {
                            //TODO: handle exception
                            log.error(t);
                        }
                    }
                    mgr.commitTransaction();
                }
                finally {
                    mgr.closeCurrentSession();
                }
            }
            return addedJobs;
        }
        else {
            //TODO: in some cases, we don't want to kill the parent pipeline or its child jobs
            //handle an error in a pipeline step
            terminatePipelineSteps(mgr, parentJobNumber);
            handlePipelineJobCompletion(mgr, parentJobNumber, -1, "Pipeline terminated because of an error in child job [id: "+childJobNumber+"]");
        }
        return false;
    }

    private static boolean handleJobCompletionOld(final HibernateSessionManager mgr, final JobInfo pipelineJobInfo, final JobInfo childJobInfo) {
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
                List<JobQueue> records = getWaitingJobs(mgr, parentJobNumber);
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
                startNextStep(mgr, nextWaitingJob);
                //indicate there is another step waiting on the queue
                return true;
            }
            
            //no waiting jobs found, find out if the last step in the pipeline is complete
            mgr.beginTransaction();
            List<Object[]> jobInfoObjs = getChildJobObjs(mgr, parentJobNumber);
            mgr.closeCurrentSession();
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
                handlePipelineJobCompletion(mgr, parentJobNumber, 0);
                return false;
            }
        }
        else {
            //TODO: in some cases, we don't want to kill the parent pipeline or its child jobs
            //handle an error in a pipeline step
            terminatePipelineSteps(mgr, parentJobNumber);
            handlePipelineJobCompletion(mgr, parentJobNumber, -1, "Pipeline terminated because of an error in child job [id: "+childJobNumber+"]");
        }
        return false;
    }
    
    /**
     * Get the next step in the pipeline, by jobId.
     * @return
     */
    private static List<JobQueue> getWaitingJobs(final HibernateSessionManager mgr, int parentJobNo) throws Exception {
        List<JobQueue> records = JobQueueUtil.getWaitingJobs(mgr, parentJobNo);
        return records;
    }
    
    public static void handleRunningPipeline(final HibernateSessionManager mgr, final JobInfo pipeline) {
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
            mgr.beginTransaction();
            jobInfoObjs = getChildJobObjs(mgr, pipeline.getJobNumber());
        }
        catch (Throwable t) {
            jobInfoObjs = new ArrayList<Object[]>();
        }
        finally {
            mgr.closeCurrentSession();
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
                terminatePipelineSteps(mgr, pipeline.getJobNumber());
                handlePipelineJobCompletion(mgr, pipeline.getJobNumber(), -1, errorMessage);
            }
            else {
                PipelineHandler.handlePipelineJobCompletion(mgr, pipeline.getJobNumber(), 0);
            }
        }
    }
    
    private static void startNextStep(final HibernateSessionManager mgr, final int nextJobId) {
        try {
            mgr.beginTransaction();
            JobQueueUtil.setJobStatus(mgr, nextJobId, JobQueue.Status.PENDING);
            mgr.commitTransaction();
        }
        catch (Throwable t) {
            mgr.rollbackTransaction();
        }
    }
    
    private static void terminatePipelineSteps(final HibernateSessionManager mgr, final int parentJobNumber) {
        try {
            mgr.beginTransaction();
            List<Integer> incompleteJobIds = getIncompleteJobs(mgr, parentJobNumber);
            for(Integer jobId : incompleteJobIds) {
                AnalysisJobScheduler.setJobStatus(mgr, jobId, JobStatus.JOB_ERROR);
            }
            mgr.commitTransaction();
        }
        catch (Throwable t) {
            log.error("Error updating job status for child jobs in pipeline #"+parentJobNumber, t);
            mgr.rollbackTransaction();
        }
    }

    private static void handlePipelineJobCompletion(final HibernateSessionManager mgr, int parentJobNumber, int exitCode) {
        handlePipelineJobCompletion(mgr, parentJobNumber, exitCode, (String) null);
    }
    
    /**
     * For the given pipeline, set its status to complete (and notify any interested listeners).
     * Delegated to the GenePatternAnalysisTask.
     * 
     * @param parentJobNumber
     * @param exitCode, with a non-zero exitCode, an errorMessage is automatically created, if necessary.
     * @param errorMessage, can be null
     */
    private static void handlePipelineJobCompletion(final HibernateSessionManager mgr, int parentJobNumber, int exitCode, String errorMessage) {
        try {
            if (exitCode != 0 && errorMessage == null) {
                errorMessage = "Pipeline terminated with non-zero exitCode: "+exitCode;
            }
            GenePatternAnalysisTask.handleJobCompletion(mgr, parentJobNumber, exitCode, errorMessage);
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
    private static List<Integer> getIncompleteJobs(final HibernateSessionManager mgr, final int parentJobId) {
        List<Object[]> jobInfoObjs = getChildJobObjs(mgr, parentJobId);
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
    private static List<JobInfo> runPipeline(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext jobContext, final PipelineModel pipelineModel, int stopAfterTask, boolean isScatterStep) 
    throws PipelineModelException, MissingTasksException, JobSubmissionException
    {
        final Vector<JobSubmission> tasks = pipelineModel.getTasks();

        // initialize the pipeline args
        final JobInfo pipelineJobInfo=jobContext.getJobInfo();
        final Map<String,String> additionalArgs = new HashMap<String,String>();
        for(ParameterInfo param : pipelineJobInfo.getParameterInfoArray()) {
            additionalArgs.put(param.getName(), param.getValue());
        }
        
        if (!isScatterStep) {
            // add job records to the analysis_job table
            final List<JobInfo> addedJobs = runPipeline(mgr, gpConfig, jobContext, tasks, additionalArgs, stopAfterTask);
            return addedJobs;
        }
        else {
            final List<JobInfo> addedJobs = runScatterPipeline(mgr, gpConfig, pipelineJobInfo, tasks, additionalArgs, stopAfterTask);
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
    private static List<JobInfo> runPipeline(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext jobContext, final List<JobSubmission> tasks, final Map<String,String> additionalArgs, final int stopAfterTask) 
    throws PipelineModelException, MissingTasksException, JobSubmissionException
    {  
        final JobInfo pipelineJobInfo=jobContext.getJobInfo();
        log.debug("starting pipeline: "+pipelineJobInfo.getTaskName()+" ["+pipelineJobInfo.getJobNumber()+"]");

        int stepNum = 0;
        final List<JobInfo> submittedJobs = new ArrayList<JobInfo>();
        for(final JobSubmission jobSubmission : tasks) { 
            if (stepNum >= stopAfterTask) {
                // stop and execute no further
                break;
            }
            
            final ParameterInfo[] parameterInfo = jobSubmission.giveParameterInfoArray();
            substituteLsidInInputFiles(gpConfig, pipelineJobInfo.getTaskLSID(), parameterInfo);
            ParameterInfo[] params = parameterInfo;
            params = setJobParametersFromArgs(jobSubmission.getName(), stepNum + 1, params, additionalArgs);
            
            //TODO: the taskInfos are all initialized in the checkForMissingTasks, should use the same data structure,
            //    rather making another call
            final TaskInfo taskInfo;
            try {
                taskInfo = getTaskInfo(mgr, jobSubmission.getLSID());
            }
            catch (Throwable t) {
                throw new JobSubmissionException("Error initializing task from lsid="+jobSubmission.getLSID()+": "+t.getLocalizedMessage());
            }
            JobInfo submittedJob = addJobToPipeline(mgr, pipelineJobInfo.getJobNumber(), pipelineJobInfo.getUserId(), taskInfo, params);
            
            //HACK: for listMode=CMD and listMode=CMD_OPT params, add values to the job_input_values table
            insertJobInputValues(mgr, jobContext, submittedJob, stepNum + 1);
            submittedJobs.add(submittedJob);
            ++stepNum;
        }
        return submittedJobs;
    }
    
    /**
     * When adding a new step to the pipeline, record values to the JOB_INPUT_VALUE table.
     * note: 'Use output from ...' parameters must be handled at runtime just before starting the job.
     * 
     * @param mgr
     * @param pipelineJobContext, the parent pipeline job
     * @param submittedJob, the step added to the pipeline
     * @param stepNum, the step number in the pipeline
     */
    protected static void insertJobInputValues(final HibernateSessionManager mgr, final GpContext pipelineJobContext, final JobInfo submittedJob, final int stepNum) {
        JobInput pipelineJobInput=pipelineJobContext.getJobInput();
        JobInput stepJobInput=new JobInput();
        stepJobInput.setLsid(submittedJob.getTaskLSID());
        stepJobInput.setBaseGpHref(pipelineJobContext.getBaseGpHref());
        for(final ParameterInfo pinfo : submittedJob.getParameterInfoArray()) {
            final ParamId paramId=new ParamId(pinfo.getName());
            final String key = submittedJob.getTaskName() + stepNum + "." + pinfo.getName();
            final Param pipelineParam=pipelineJobInput.getParam(key);
            if (pipelineParam != null) {
                // copy the parent pipeline input param into the job input param
                for(final Entry<GroupId,Collection<ParamValue>> groupEntry : pipelineParam.getGroupedValues().entrySet()) {
                    final GroupId groupId=groupEntry.getKey();
                    for(final ParamValue paramValue : groupEntry.getValue()) {
                        stepJobInput.addValue(paramId, paramValue, groupId);
                    }
                }
            }
            else {
                // add the job input param 
                stepJobInput.addValue(pinfo.getName(), pinfo.getValue());
            }
        }
        if (stepJobInput.getParams().size() > 0) {
            try {
                new JobInputValueRecorder(mgr).saveJobInput(submittedJob.getJobNumber(), stepJobInput);
            }
            catch (Throwable t) {
                log.error("Error saving job_input_values for step in pipeline, pipeline job #"+pipelineJobContext.getJobNumber()+
                        ", "+pipelineJobContext.getTaskName()+
                        ", step #"+stepNum+", job #"+submittedJob.getJobNumber(), t);
            }
        }
    }

    private static List<JobInfo> runScatterPipeline(final HibernateSessionManager mgr, final GpConfig gpConfig, final JobInfo pipelineJobInfo, final List<JobSubmission> tasks, final Map<String,String> additionalArgs, int stopAfterTask) 
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
            taskInfo = getTaskInfo(mgr, jobSubmission.getLSID());
        }
        catch (Throwable t) {
            throw new JobSubmissionException("Error initializing task from lsid="+jobSubmission.getLSID()+": "+t.getLocalizedMessage());
        }
    
        ParameterInfo[] parameterInfo = tasks.get(0).giveParameterInfoArray();
        substituteLsidInInputFiles(gpConfig, pipelineJobInfo.getTaskLSID(), parameterInfo);
        ParameterInfo[] params = parameterInfo;
        params = setJobParametersFromArgs(tasks.get(0).getName(), stepNum + 1, params, additionalArgs);
        
        //for each step, if it has a batch input parameter, expand into a bunch of child steps
        Map<ParameterInfo,List<String>> scatterParamMap = new HashMap<ParameterInfo, List<String>>();
        for(ParameterInfo p : params) {
            boolean doScatter=false;
            doScatter=p.getValue().contains("?scatter"); //Note: must be first parameter in query string
            if (doScatter) {
                List<String> scatterParamValues = getScatterParamValues(mgr, p);
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
        
        List<JobInfo> submittedJobs = addScatterJobsToPipeline(mgr, pipelineJobInfo, taskInfo, params, scatterParamMap);
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
        @SuppressWarnings("rawtypes")
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
    private static TaskInfo getTaskInfo(final HibernateSessionManager mgr, final String lsid) throws Exception {
        if (lsid == null) {
            throw new IllegalArgumentException("lsid == null");
        }
        boolean isInTransaction = mgr.isInTransaction();
        try {
            return TaskInfoCache.instance().getTask(mgr, lsid);
        }
        catch (Throwable t) {
            throw new Exception(t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
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
    private static List<String> getScatterParamValues(final HibernateSessionManager mgr, final ParameterInfo param) throws JobSubmissionException {
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
            gpFilePath = GpFileObjFactory.getRequestedJobResultFileObj(ServerConfigurationFactory.instance(), value);
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
                AnalysisDAO dao = new AnalysisDAO(mgr);
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

    private static List<JobInfo> addScatterJobsToPipeline(final HibernateSessionManager mgr, final JobInfo pipelineJobInfo, final TaskInfo taskInfo, final ParameterInfo[] params, final Map<ParameterInfo,List<String>> scatterParamMap) 
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
            JobInfo submittedJob = addJobToPipeline(mgr, pipelineJobInfo.getJobNumber(), pipelineJobInfo.getUserId(), taskInfo, params);
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
    private static JobInfo addJobToPipeline(final HibernateSessionManager mgr, final int parentJobId, final String userID, final TaskInfo taskInfo, final ParameterInfo[] params)
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
                        params[i].getAttributes().remove("TYPE");
                        params[i].getAttributes().remove("MODE");
                    }
                }
            }
        }

        //make sure to commit db changes
        JobInfo jobInfo = addJobToQueue(mgr, taskInfo, userID, params, parentJobId);
        return jobInfo;
    }

    private static PipelineModel checkForMissingTasks(final HibernateSessionManager mgr, final GpContext userContext, final TaskInfo forTask) throws MissingTasksException {
        final GetIncludedTasks taskChecker;
        try {
            taskChecker=new GetIncludedTasks(mgr, userContext, forTask);
        }
        catch (Throwable t) {
            log.error(t);
            return null;
        }
        
        if (taskChecker.allTasksAvailable()) {
            return taskChecker.getPipelineModel();
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
        return taskChecker.getPipelineModel();
    }

    /**
     * Update the value and return the map of updated values.
     * @param mgr
     * @param dao
     * @param childJobs
     * @param inheritedInputParams
     * @return
     */
    private static Map<String,String> updateInheritedParamValues(final HibernateSessionManager mgr, final AnalysisDAO dao, JobInfo[] childJobs, final List<ParameterInfo> inheritedInputParams) {
        final Map<String,String> rval=new LinkedHashMap<String,String>();
        for(final ParameterInfo inputParam : inheritedInputParams) {
            final String url = getInheritedFilename(mgr, dao, childJobs, inputParam);
            if (url != null && url.trim().length() > 0) {
                rval.put(inputParam.getName(), url);
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
        return rval; 
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
            @SuppressWarnings("rawtypes")
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
    @SuppressWarnings("unchecked")
    private static String getInheritedFilename(final HibernateSessionManager mgr, final AnalysisDAO dao, final JobInfo[] childJobs, final ParameterInfo inputParam) {
        log.debug("getInheritedFilename for inputParam="+inputParam.getName());
        // these params must be removed so that the soap lib doesn't try to send the file as an attachment
        @SuppressWarnings("rawtypes")
        final HashMap attributes = inputParam.getAttributes();
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

        final JobInfo fromJob = childJobs[stepNum];
        if (log.isDebugEnabled()) {
            log.debug("stepNum="+stepNum);
            if (fromJob==null) {
                log.debug("fromJob==null");
            }
            else {
                log.debug("fromJob.id="+fromJob.getJobNumber()+", "+fromJob.getTaskName());
            }
        }
        try {
            final String outputFileName = getOutputFileName(mgr, dao, fromJob, fileStr);
            if (log.isDebugEnabled()) { 
                log.debug("outputFileName="+outputFileName); 
            }
            if (outputFileName == null || outputFileName.trim().length() == 0) {
                //return an empty string if no output filename is found
                return "";
            }
            attributes.put(ParameterInfo.MODE, ParameterInfo.URL_INPUT_MODE);
            final String url = "<GenePatternURL>jobResults/" + outputFileName;
            return url;
        }
        catch (Exception e) {
            log.error("Error getting inherited output file fromJob="+fromJob+", fileStr="+fileStr, e);
            return "";            
        }
    }
    
    /**
     * Select an output file from the given upstream job in the pipeline. Selection is based on
     * a number of different 'fileStr' types, select <fileStr> from <fromJob>, where fileStr is
     * 
     * 1) ?scatter[&options...], a scatter step
     * 2) ?filelist[&options...], a gather step
     * 3) stdout, select stdout from job
     * 4) stderr, select stderr from job
     * 5) <integer>, select Nth output from job
     * 6) <file-type>, select (first) file-type from job
     * 
     * @param mgr, hibernate session required for '?filelist' type in scatter-gather pipeline  
     * @param dao, AnalysisDAO instance for recursively generating the list of output files for a job
     * @param fromJob, the completed job from which to select the output file
     * @param fileStr, the specification for the type of output file to select
     * @return
     * @throws ServerConfigurationException
     * @throws FileNotFoundException
     */
    private static String getOutputFileName(final HibernateSessionManager mgr, final AnalysisDAO dao, final JobInfo fromJob, final String fileStr)
    throws ServerConfigurationException, FileNotFoundException 
    {
        if (fromJob==null) {
            throw new IllegalArgumentException("fromJob==null");
        }
        if (log.isDebugEnabled()) {
            log.debug("getOutputFileName, fromJob="+fromJob.getJobNumber()+"."+fromJob.getTaskName()+", fileStr="+fileStr);
        }
        
        //special-case: scatter-step
        if (fileStr.startsWith("?scatter")) {
            //it's a scatter-step
            return fromJob.getJobNumber() + fileStr;
        }
        
        // special-case: gather-step
        if (fileStr.startsWith("?filelist")) {
            try {
                //it's a gather-step, write the filelist as an output of the fromJob 
                final GpFilePath filelist = writeFileList(mgr, fromJob, fileStr);
                final String fileName = fromJob.getJobNumber() + "/" + filelist.getRelativePath();
                final String encodedValue = UrlUtil.encodeFilePath(new File(fileName));
                return encodedValue;
            }
            catch (Exception e) {
                log.error(e);
            }
        } 
        
        //special-case: use 'stdout' from previous job
        if ("stdout".equals(fileStr)) {
            final ParameterInfo stdoutParam = getStdoutFile(fromJob);
            return getEncodedValue(stdoutParam);
        }

        //special-case: use 'stderr' from previous job
        if ("stderr".equals(fileStr)) {
            final ParameterInfo stderrParam = getStderrFile(fromJob);
            return getEncodedValue(stderrParam);
        } 
        
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
            //get the ordered list of output files for the job
            final List<ParameterInfo> allResultFiles = getOutputFilesRecursive(dao, fromJob);
            fileName = getNthOutputFilename(outputNum, allResultFiles);
        }
        else {
            //get the ordered list of output files for the job
            final List<ParameterInfo> allResultFiles = getOutputFilesRecursive(dao, fromJob);
            fileName = getOutputFilenameByType(fromJob, outputType, allResultFiles);
        }
        if (fileName != null) {
            return fileName;
        }

        if (fileName == null) {
            throw new FileNotFoundException("Unable to find output file from job " + fromJob.getJobNumber() + " that matches " + fileStr + ".");
        }
        return fileName;
    }
    
    /**
     * for scatter-gather feature, create a filelist file as an output file in the job results directory
     * @param mgr
     * @param fromJob
     * @param fileStr, e.g. fileStr=?filelist[&filter=<list of patterns>]
     * 
     * @return the GpFilePath reference to the filelist file
     * @throws Exception
     */
    private static GpFilePath writeFileList(final HibernateSessionManager mgr, final JobInfo fromJob, final String fileStr) throws Exception {
        boolean isInTransaction = false;
        AnalysisDAO dao = null;
        try {
            isInTransaction = mgr.isInTransaction();
            dao = new AnalysisDAO(mgr);
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
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
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
            log.error("invalid fileIdx="+fileIdx+", num files="+jobResultFiles.size());
            return "";
        }
        final ParameterInfo inheritedFile = jobResultFiles.get(fileIdx);
        return getEncodedValue(inheritedFile);
    }

    /**
     * Get the url encoded value for the inherited file parameter, aka a 'use output from job ...' parameter.
     * Values are in the form of,
     *     <job_number>/<encoded_output_file_path>
     * E.g.
     *     1882/file%20path.txt
     * 
     * @param inheritedFile
     * @return the encoded file path
     */
    protected static String getEncodedValue(final ParameterInfo inheritedFile) {
        if (inheritedFile == null) {
            return null;
        }
        final String filename = inheritedFile.getValue();
        if (filename == null) {
            return null;
        }
        final String encodedValue = UrlUtil.encodeFilePath(new File(filename));
        return encodedValue;
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
            final String encodedValue = UrlUtil.encodeFilePath(new File(fileName));
            return encodedValue;
        }
        else if (fileStr.equals(GPConstants.STDOUT) || fileStr.equals(GPConstants.STDERR)) {
            log.error("old version of pipeline: '"+fileStr+"' is deprecated");
            fileName = fileStr;
            final String encodedValue = UrlUtil.encodeFilePath(new File(fileStr));
            return encodedValue;
        }
        else {
            return null;
        }
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
    public static void substituteLsidInInputFiles(final GpConfig gpConfig, final String lsidValue, final ParameterInfo[] parameterInfos) {
        for (final ParameterInfo param : parameterInfos) {
            final String valueIn = param.getValue();
            if (valueIn != null) {
                final String value = substituteLsidInInputFile(gpConfig, lsidValue, param.getValue());
                param.setValue(value);
            }
        }        
    }
    
    protected static String substituteLsidInInputFile(final GpConfig gpConfig, final String lsidValue, final String valueIn) {
        final String lsidTag = "<LSID>";
        final String gpUrlTag = "<GenePatternURL>";
        if (valueIn != null && valueIn.startsWith(gpUrlTag)) {
            String value = valueIn.replace(lsidTag, lsidValue);
            return value;
        }
        else {
            return valueIn;
        }
    }
    
    /**
     * Look for parameters that are passed in on the command line and put them into the ParameterInfo array
     */
    private static ParameterInfo[] setJobParametersFromArgs(
            String name, 
            int taskNum, //this is the step number
            ParameterInfo[] parameterInfo, 
            @SuppressWarnings("rawtypes") Map args) 
    {
        for (int i = 0; i < parameterInfo.length; i++) {
            ParameterInfo aParam = parameterInfo[i];
            @SuppressWarnings("rawtypes")
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
    static private JobInfo addJobToQueue(final HibernateSessionManager mgr, final TaskInfo taskInfo, final String userId, final ParameterInfo[] parameterInfoArray, final Integer parentJobNumber) 
    throws JobSubmissionException
    {
        JobInfo jobInfo = null;
        final boolean isInTransaction = mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            AnalysisJob newJob = addNewJob(mgr, userId, taskInfo, parameterInfoArray, parentJobNumber);
            jobInfo = new JobInfo(newJob);
            JobManager.createJobDirectory(jobInfo); 
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
            return jobInfo;
        }
        catch (Throwable t) {
            mgr.rollbackTransaction();
            if (t instanceof JobSubmissionException) {
                throw (JobSubmissionException) t;
            }
            throw new JobSubmissionException(t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

    private static AnalysisJob addNewJob(final HibernateSessionManager mgr, final String userId, final TaskInfo taskInfo, final ParameterInfo[] parameterInfoArray, final Integer parentJobNumber) 
    throws JobSubmissionException
    { 
        if (taskInfo == null) {
            throw new JobSubmissionException("Error adding job to queue, taskInfo is null");
        }
        
        if (taskInfo.getID() < 0) {
            throw new JobSubmissionException("Error adding job to queue, invalid taskId, taskInfo.getID="+taskInfo.getID());
        }
        
        final boolean isInTransaction = mgr.isInTransaction();
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
            
            mgr.getSession().save(aJob);
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
            return aJob;
        }
        catch (Throwable t) {
            mgr.closeCurrentSession();
            throw new JobSubmissionException("Error adding job to queue, taskId="+taskInfo.getID()+
                    ", taskName="+taskInfo.getName()+
                    ", taskLsid="+taskInfo.getLsid(), t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

}