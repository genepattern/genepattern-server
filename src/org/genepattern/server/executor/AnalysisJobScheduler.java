/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/

package org.genepattern.server.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.executor.drm.JobExecutor;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;
import org.genepattern.server.executor.drm.dao.JobRunnerJobDao;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.job.input.cache.FileCache;
import org.genepattern.server.jobqueue.JobQueue;
import org.genepattern.server.jobqueue.JobQueueUtil;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.hibernate.SQLQuery;

/**
 * Polls the db for new PENDING jobs and submits them to GenePatternAnalysisTask for execution.
 */
public class AnalysisJobScheduler implements Runnable {
    private static Logger log = Logger.getLogger(AnalysisJobScheduler.class);

    public static ThreadGroup THREAD_GROUP = new ThreadGroup("GPAnalysisJob");
    public static ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        public Thread newThread(Runnable r) {
            return new Thread(THREAD_GROUP, r);
        }
    };
    
    private static final int BOUND = 20000;
    private final BlockingQueue<Integer> pendingJobQueue = new LinkedBlockingQueue<Integer>(BOUND);
    private final ConcurrentMap<Integer,Future<JobSubmitter>> dispatchingJobsMap = new ConcurrentHashMap<Integer,Future<JobSubmitter>>();
    private static ExecutorService jobTerminationService=null;
    private Object jobQueueWaitObject = new Object();
    private boolean suspended = false;
    private Thread runner = null;

    public AnalysisJobScheduler() { 
    }
    public AnalysisJobScheduler(boolean suspended) {
        this.suspended = suspended;
    }
    
    /**
     * The status of the internal job queue, PENDING jobs are not executed when suspended is true.
     * Use this flag to allow running jobs to continue, while preventing any new jobs from starting.
     * This could be used as part of a controlled shutdown of the GP server.
     */
    public boolean isSuspended() {
        return suspended;
    }

    public void startQueue() {
        final int numJobTerminationThreads = 5;  
        if (jobTerminationService==null || jobTerminationService.isTerminated()) {
            jobTerminationService = Executors.newFixedThreadPool(numJobTerminationThreads);
        }
        runner = new Thread(THREAD_GROUP, this);
        runner.setName("AnalysisTaskThread");
        runner.setDaemon(false);
        runner.start();
    }
    
    public void stopQueue() {
        if (runner != null) {
            runner.interrupt();
            runner = null;
        }
        FileCache.instance().shutdownNow();
        shutdownJobTerminationService();
    }
    
    private void shutdownJobTerminationService() {
        if (jobTerminationService != null) {
            jobTerminationService.shutdown();
            try {
                if (!jobTerminationService.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.error("jobTerminationService shutdown timed out after 30 seconds.");
                    jobTerminationService.shutdownNow();
                }
            }
            catch (final InterruptedException e) {
                log.error("jobTerminationService executor.shutdown was interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Main AnalysisTask's thread method. */
    public void run() {
        log.info("Starting AnalysisTask thread ... ");
        
        //reset any stuck 'dispatching' jobs, this covers jobs which were in the middle of being dispatched
        //at the time of a server shutdown
        int numUpdated=JobQueueUtil.changeJobStatus(JobQueue.Status.DISPATCHING, JobQueue.Status.PENDING);
        if (numUpdated >0) {
            log.info("reset "+numUpdated+" jobs from "+JobQueue.Status.DISPATCHING+" to "+JobQueue.Status.PENDING);
        }
        
        //the monitor polls the pendingJobQueue and starts jobs as they are added to the DB
        final Thread monitor=new Thread(new Runnable() {
            final ExecutorService jobSubmissionService=Executors.newCachedThreadPool();
            final GenePatternAnalysisTask genePattern = new GenePatternAnalysisTask(jobSubmissionService);

            @Override
            public void run() {
                boolean interrupted=false;
                try {
                    while(true) {
                        final Integer jobId=pendingJobQueue.take();
                        try {
                            // start a new thread for adding the job to the queue,
                            // if necessary, input files will be downloaded to the cache before starting the job
                            final JobSubmitter jobSubmitter=new JobSubmitter(genePattern, jobId); 
                            Future<JobSubmitter> f = jobSubmissionService.submit(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        jobSubmitter.run();
                                    }
                                    finally {
                                        dispatchingJobsMap.remove(jobId);
                                    }
                                } 
                            },
                            jobSubmitter);
                            Future<JobSubmitter> f2 = dispatchingJobsMap.putIfAbsent(jobId, f);
                            if (f2 != null) {
                                log.error("duplicate dispatch for jobId="+jobId);
                                f.cancel(true);
                                f = f2;
                            }
                        }
                        catch (Throwable t) {
                            //log an error here because it's not expected
                            log.error("Unexpected error while starting jobId="+jobId, t);
                        }
                    }
                }
                catch (InterruptedException e) {
                    interrupted=true;
                }
                jobSubmissionService.shutdownNow();
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        monitor.start();
        
        //the batch size, the max number of pending jobs to fetch from the db at a time
        final int batchSize = 20;
        try {
            //the while loop polls the DB and adds job ids to the pendingJobQueue as they become available
            while (true) {
                // Load input data to input queue
                synchronized (jobQueueWaitObject) {
                    
                    if (!suspended && pendingJobQueue.isEmpty()) {
                        List<Integer> waitingJobs = null;
                        boolean deferredJobs = false;
                        
                        HibernateSessionManager mgr=HibernateUtil.instance();
                        boolean inTransaction = false;
                        try {
                            inTransaction = mgr.isInTransaction();
                            
                            final List<JobQueue> records;
                            try {
                                records = JobQueueUtil.getPendingJobs(mgr,batchSize);
                            } finally {
                                if (!inTransaction) mgr.closeCurrentSession();
                            }
                            waitingJobs = new ArrayList<Integer>();
                            if (records != null) {
                                for(JobQueue record : records) {
                                    waitingJobs.add( record.getJobNo() );
                                }
                            }
                            if (waitingJobs != null && !waitingJobs.isEmpty()) {

                                if (waitingJobs != null) {
                                    inTransaction = mgr.isInTransaction();
                                    
                                    for(Integer jobId : waitingJobs) { 

                                        if (pendingJobQueue.contains(jobId)) {
                                            log.error("duplicate entry in pending jobs queue: "+jobId);
                                        }  else {
                                            try {
                                                // check if this user is running too many already
                                                mgr=HibernateUtil.instance();
                                                inTransaction = mgr.isInTransaction();
                                                
                                                boolean okToStart = true;
                                                try {
                                                    okToStart = JobQueueUtil.canStartJob(mgr, jobId);
                                                    //System.out.println("Job: "+ jobId + "   ok2start: "+ okToStart);
                                                } finally {
                                                    if (!inTransaction) mgr.closeCurrentSession();
                                                }
                                                if (okToStart){
                                                    JobQueueUtil.setJobStatus(jobId, JobQueue.Status.DISPATCHING);
                                                    pendingJobQueue.put(jobId);
                                                    
                                                } else {
                                                    deferredJobs = true;
                                                }
                                            } catch (Throwable t) {
                                                log.error(t);
                                            }
                                        }
                                    }
                                } 

                            }
                        } catch (Exception e){
                            log.error(e);
                        } finally {
                            
                            if (!inTransaction) mgr.closeCurrentSession();
                        }
                        
                        
                        if (waitingJobs == null || waitingJobs.isEmpty() || deferredJobs==true) {  // waiting jobs is empty
                            //insurance against deadlock, poll for new PENDING jobs every 2 minutes, regardless of whether notify has been called
                            mgr=HibernateUtil.instance();
                            inTransaction = mgr.isInTransaction();
                            
                            if (!inTransaction) mgr.closeCurrentSession();
                            
                            final long timeout = 20000;
                            jobQueueWaitObject.wait(timeout);
                        }
                        
                        
                        
                    }
                }
            }
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        // on shutdown
        if (monitor != null) {
            monitor.interrupt();
        }
    }
    
    /** @deprecated pass in a valid Hibernate session */
    static public int setJobStatus(int jobNo, int toStatusId) {
        return setJobStatus(org.genepattern.server.database.HibernateUtil.instance(), jobNo, toStatusId);

    }
    static public int setJobStatus(final HibernateSessionManager mgr, int jobNo, int toStatusId) {
        final boolean isInTransaction = mgr.isInTransaction();
        mgr.beginTransaction();
        
        try {
            final String sqlUpdate = "update ANALYSIS_JOB set status_id=:toStatusId where job_no=:jobNo"; 
            final SQLQuery sqlQuery = mgr.getSession().createSQLQuery(sqlUpdate);
            sqlQuery.setInteger("toStatusId", toStatusId);
            sqlQuery.setInteger("jobNo", jobNo);
            int rval = sqlQuery.executeUpdate();
            if (rval != 1) {
                log.error("setJobStatus(jobNo="+jobNo+", toStatusId="+toStatusId+") had no effect");
            }
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
            return rval;
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

    /**
     * Wake up the job queue thread. The object is synchronized to obtain ownership of the monitor.
     */
    public void wakeupJobQueue() {
        synchronized (jobQueueWaitObject) {
            jobQueueWaitObject.notify();
        }
    }
    
    public void suspendJobQueue() {
        this.suspended = true;
    }
    
    public void resumeJobQueue() {
        this.suspended = false;
        wakeupJobQueue();
    }

    /**
     * Terminate the job with the given jobId.
     * 
     * @param jobId
     * @throws JobTerminationException
     */
    public void terminateJob(final HibernateSessionManager mgr, Integer jobId) throws JobTerminationException {
        if (jobId == null) {
            throw new JobTerminationException("Invalid null arg");
        }
        JobInfo jobInfo = null;
        try {
            AnalysisDAO dao = new AnalysisDAO(mgr);
            jobInfo = dao.getJobInfo(jobId);
        }
        catch (Throwable t) {
            String message="Server error: Not able to load jobInfo for jobId: "+jobId;
            log.error(message);
            throw new JobTerminationException(message, t);
        }
        finally {
            mgr.closeCurrentSession();
        }
        terminateJob(mgr, jobInfo);
    }
    
    /**
     * Terminate the job for the given jobInfo arg.
     * 
     * @param jobInfo
     * @throws JobTerminationException
     */
    public void terminateJob(final HibernateSessionManager mgr, final JobInfo jobInfo) throws JobTerminationException {
        if (jobInfo == null) {
            log.error("invalid null arg to terminateJob");
            return;
        }
        
        if (log.isDebugEnabled()) {
            log.debug("terminateJob, gpJobNo="+jobInfo.getJobNumber());
        }

        //note: don't terminate completed jobs
        boolean isFinished = isFinished(jobInfo); 
        if (isFinished) {
            log.debug("job #"+jobInfo.getJobNumber()+" is already finished");
            terminateJobRunnerJob(mgr, jobInfo);
            return;
        }

        final Integer jobId=jobInfo.getJobNumber();
        Future<JobSubmitter> f = dispatchingJobsMap.remove(jobId);
        if (f != null) {
            if (!f.isDone()) {
                boolean isCancelled=f.cancel(true);
                log.info("cancelled jobId="+jobId+", isCancelled="+isCancelled);
                int numDeleted=JobQueueUtil.deleteJobQueueStatusRecord(mgr, jobId);
                if (log.isDebugEnabled()) { log.debug("numDeleted="+numDeleted); }
            }
        }
        
        //terminate pending jobs immediately
        boolean isPending = isPending(jobInfo);
        if (isPending) {
            GenePatternAnalysisTask.handleJobCompletion(mgr, jobInfo.getJobNumber(), -1, "Pending job #"+jobInfo.getJobNumber()+" terminated by user");
            return;
        }
        
        // terminate the underlying job
        terminateJobWTimeout(mgr, jobInfo);
    }
    
    /**
     * Terminate the job, but kill the job termination process if it takes longer than the 
     * configured timeout interval.
     * 
     * @param jobInfo
     * @throws JobTerminationException
     */
    private static void terminateJobWTimeout(final HibernateSessionManager mgr, final JobInfo jobInfo) throws JobTerminationException {
        final int jobNumber;
        if (jobInfo != null) {
            jobNumber = jobInfo.getJobNumber();
        }
        else {
            jobNumber = -1;
        }
        FutureTask<Integer> task = new FutureTask<Integer>( new Callable<Integer>() {
            public Integer call() throws Exception {
                if (jobInfo == null) {
                    log.error("invalid null arg to terminateJob");
                    return -1;
                }
            
                //note: don't terminate completed jobs
                boolean isFinished = isFinished(jobInfo); 
                if (isFinished) {
                    log.debug("job #"+jobInfo.getJobNumber()+" is already finished");
                    terminateJobRunnerJob(mgr, jobInfo);
                    return jobInfo.getJobNumber();
                }
                
                try {
                    CommandExecutor cmdExec = CommandManagerFactory.getCommandManager().getCommandExecutor(jobInfo);
                    cmdExec.terminateJob(jobInfo);
                    return jobInfo.getJobNumber();
                }
                catch (Throwable t) {
                    throw new JobTerminationException(t);
                }
            }
        });
        long jobTerminationTimeout = getJobTerminationTimeout(jobInfo); 
        try {
            jobTerminationService.execute(task);
            int job_id = task.get(jobTerminationTimeout, TimeUnit.MILLISECONDS);
            if (job_id >= 0) {
                log.debug("terminated job #"+job_id);
            }
            else {
                log.debug("did not terminate job #"+jobNumber);
            }
        }
        catch (ExecutionException e) {
            throw new JobTerminationException(e);
        }
        catch (TimeoutException e) {
            task.cancel(true);
            throw new JobTerminationException("Timeout after "+jobTerminationTimeout+" ms while terminating job #", e);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Get the configured 'job.termination.timeout' interval, in milliseconds, based on 
     * the settings in the job configuration file.
     * Return the default value (30 seconds) if none is specified.
     * 
     * @param jobInfo
     * @return
     */
    private static long getJobTerminationTimeout(final JobInfo jobInfo) {
        final GpContext jobContext=GpContext.getContextForJob(jobInfo);
        //default value is 30 seconds
        final Long jobTerminationTimeout=ServerConfigurationFactory.instance().getGPLongProperty(jobContext, "job.termination.timeout", 1000L*30L);
        return jobTerminationTimeout;
    }

    public static boolean isPending(JobInfo jobInfo) {
        return isPending(jobInfo.getStatus());
    }

    private static boolean isPending(String jobStatus) {
        return JobStatus.PENDING.equals(jobStatus);
    }

    public static boolean isFinished(JobInfo jobInfo) {
        return isFinished(jobInfo.getStatus());
    }
    
    private static boolean isFinished(String jobStatus) {
        if ( JobStatus.FINISHED.equals(jobStatus) ||
                JobStatus.ERROR.equals(jobStatus) ) {
            return true;
        }
        return false;        
    }

    private static boolean terminateJobRunnerJob(final HibernateSessionManager mgr, final JobInfo jobInfo) {
        Integer gpJobNo=jobInfo.getJobNumber();
        JobRunnerJob jrj = null;
        try {
            jrj=new JobRunnerJobDao().selectJobRunnerJob(mgr, gpJobNo);
        } // <- throws DbException
        catch (Throwable t) {
            log.error(t);
        }
        if (jrj == null) {
            log.debug("No entry in job_runner_job table for gpJobNo="+gpJobNo);
            return false;
        }
        String jrName=jrj.getJobRunnerName();
        JobExecutor jobExec=CommandManagerFactory.getCommandManager().lookupJobExecutorByJobRunnerName(jrName);
        if (jobExec != null) {
            jobExec.terminateJob(jobInfo);
            return true;
        }
        return false;
    }

    
}

