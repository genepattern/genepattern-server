/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

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
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.executor.CommandProperties.Value;
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
                        try {
                            final List<JobQueue> records = JobQueueUtil.getPendingJobs(batchSize);
                            waitingJobs = new ArrayList<Integer>();
                            if (records != null) {
                                for(JobQueue record : records) {
                                    waitingJobs.add( record.getJobNo() );
                                }
                            }
                        }
                        catch (Exception e) {
                            log.error(e);
                        }
                        if (waitingJobs != null && !waitingJobs.isEmpty()) {
                            if (waitingJobs != null) {
                                for(Integer jobId : waitingJobs) { 
                                    if (pendingJobQueue.contains(jobId)) {
                                        log.error("duplicate entry in pending jobs queue: "+jobId);
                                    }
                                    else {
                                        try {
                                            JobQueueUtil.setJobStatus(jobId, JobQueue.Status.DISPATCHING);
                                            pendingJobQueue.put(jobId);
                                        }
                                        catch (Throwable t) {
                                            log.error(t);
                                        }
                                    }
                                }
                            }
                        }
                        else {
                            //insurance against deadlock, poll for new PENDING jobs every 5 minutes, regardless of whether notify has been called
                            final long timeout = 300000;
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

    /**
     * Change the statusId for the given job, only if the job's current status id is the same as the fromStatusId.
     * This condition is helpful to guard against another thread which has already changed the job status.
     * 
     * @param jobNo
     * @param fromStatusId
     * @param toStatusId
     * @return number of rows successfully updated
     */
    static public int changeJobStatus(int jobNo, int fromStatusId, int toStatusId) {
        String sqlUpdate = "update ANALYSIS_JOB set status_id=:toStatusId where job_no=:jobNo and status_id=:fromStatusId";
        SQLQuery sqlQuery = HibernateUtil.getSession().createSQLQuery(sqlUpdate);
        sqlQuery.setInteger("toStatusId", toStatusId);
        sqlQuery.setInteger("jobNo", jobNo);
        sqlQuery.setInteger("fromStatusId", fromStatusId);

        int rval = sqlQuery.executeUpdate();
        if (rval != 1) {
            log.error("changeJobStatus(jobNo="+jobNo+", fromStatusId="+fromStatusId+", toStatusId="+toStatusId+") ignored, statusId for jobNo was already changed in another thread");
        }
        return rval;
    }
    
    static public int setJobStatus(int jobNo, int toStatusId) {
        final boolean isInTransaction = HibernateUtil.isInTransaction();
        HibernateUtil.beginTransaction();
        
        try {
            final String sqlUpdate = "update ANALYSIS_JOB set status_id=:toStatusId where job_no=:jobNo"; 
            final SQLQuery sqlQuery = HibernateUtil.getSession().createSQLQuery(sqlUpdate);
            sqlQuery.setInteger("toStatusId", toStatusId);
            sqlQuery.setInteger("jobNo", jobNo);
            int rval = sqlQuery.executeUpdate();
            if (rval != 1) {
                log.error("setJobStatus(jobNo="+jobNo+", toStatusId="+toStatusId+") had no effect");
            }
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
            return rval;
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
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
    public void terminateJob(Integer jobId) throws JobTerminationException {
        if (jobId == null) {
            throw new JobTerminationException("Invalid null arg");
        }
        JobInfo jobInfo = null;
        try {
            AnalysisDAO dao = new AnalysisDAO();
            jobInfo = dao.getJobInfo(jobId);
        }
        catch (Throwable t) {
            throw new JobTerminationException("Server error: Not able to load jobInfo for jobId: "+jobId, t);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
        terminateJob(jobInfo);
    }
    
    /**
     * Terminate the job for the given jobInfo arg.
     * 
     * @param jobInfo
     * @throws JobTerminationException
     */
    public void terminateJob(JobInfo jobInfo) throws JobTerminationException {
        if (jobInfo == null) {
            log.error("invalid null arg to terminateJob");
            return;
        }

        //note: don't terminate completed jobs
        boolean isFinished = isFinished(jobInfo); 
        if (isFinished) {
            log.debug("job "+jobInfo.getJobNumber()+"is already finished");
            return;
        }

        final Integer jobId=jobInfo.getJobNumber();
        Future<JobSubmitter> f = dispatchingJobsMap.remove(jobId);
        if (f != null) {
            if (!f.isDone()) {
                boolean isCancelled=f.cancel(true);
                log.info("cancelled jobId="+jobId+", isCancelled="+isCancelled);
                try {
                    JobQueueUtil.deleteJobQueueStatusRecord(jobId);
                }
                catch (Exception ex) {
                    log.error("Error removing record from JOB_QUEUE for job_no="+jobId, ex);
                }
            }
        }
        
        //terminate pending jobs immediately
        boolean isPending = isPending(jobInfo);
        if (isPending) {
            terminatePendingJob(jobInfo);
            return;
        }

        //terminate the underlying job
        terminateJobWTimeout(jobInfo);
    }
    
    /**
     * Helper method, special case when a pending job is terminated.
     * No need to invoke terminate on the job's CommandExecutor.
     * 
     * @param jobInfo
     * @throws JobTerminationException
     */
    private static void terminatePendingJob(final JobInfo jobInfo) throws JobTerminationException {
        GenePatternAnalysisTask.handleJobCompletion(jobInfo.getJobNumber(), -1, "Pending job #"+jobInfo.getJobNumber()+" terminated by user");
    }
    
    /**
     * Terminate the job, but kill the job termination process if it takes longer than the 
     * configured timeout interval.
     * 
     * @param jobInfo
     * @throws JobTerminationException
     */
    private static void terminateJobWTimeout(final JobInfo jobInfo) throws JobTerminationException {
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
                    log.debug("job "+jobInfo.getJobNumber()+"is already finished");
                    return -1;
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
    private static long getJobTerminationTimeout(JobInfo jobInfo) {
        if (jobInfo != null) {
            try {
                CommandProperties cmdProperties = CommandManagerFactory.getCommandManager().getCommandProperties(jobInfo);
                Value value = cmdProperties.get("job.termination.timeout");
                if (value != null) {
                    return Long.parseLong( value.getValue() );
                }
            }
            catch (Throwable t) {
                log.error("Error getting jobTerminationTimeout", t);
            }
        }
        //default value is 30 seconds
        return 1000*30;
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

}

