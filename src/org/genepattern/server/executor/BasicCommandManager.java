package org.genepattern.server.executor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.JobInfoManager;
import org.genepattern.server.config.ConfigurationException;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.executor.pipeline.PipelineExecutor;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * Default implementation of the CommandManager interface.
 * Note, there is special-handling for pipeline execution. There is one, and only one, CommandExecutor for pipelines.
 * This is set implicitly and can not be modified with the configuration file.
 * 
 * @author pcarr
 */
public class BasicCommandManager implements CommandManager {
    private static Logger log = Logger.getLogger(BasicCommandManager.class);

    private AnalysisJobScheduler analysisTaskScheduler = null;
    
    @Override
    public void startAnalysisService() { 
        log.info("starting analysis service...");
        handleJobsOnServerStartup();

        if (analysisTaskScheduler == null) {
            analysisTaskScheduler = new AnalysisJobScheduler(getJobQueueSuspendedFlag());
        }
        analysisTaskScheduler.startQueue();
        log.info("...analysis service started!");
    }
    
    @Override
    public void shutdownAnalysisService() {
        if (analysisTaskScheduler != null) {
            analysisTaskScheduler.stopQueue();
        }
        log.info("shutting down analysis service...done!");
    }

    private boolean getJobQueueSuspendedFlag() {
        GpContext serverContext = GpContext.getServerContext();
        boolean suspended = ServerConfigurationFactory.instance().getGPBooleanProperty(serverContext, "job.queue.suspend_on_start");
        return suspended;
    }
    
    //TODO: use paged results to handle large number of 'Processing' jobs
    public void handleJobsOnServerStartup() {
        log.info("handling 'DISPATCHING' and 'PROCESSING' jobs on server startup ...");
        List<MyJobInfoWrapper> openJobs = getOpenJobs();

        for(MyJobInfoWrapper jobInfoWrapper : openJobs) {
            JobInfo jobInfo = jobInfoWrapper.jobInfo;
            String jobId = ""+jobInfo.getJobNumber();
            String curStatus = jobInfo.getStatus();
            int updatedStatusId = JobStatus.JOB_ERROR;
            
            //Note: handle special-case for DISPATCHING jobs
            try {
                CommandExecutor cmdExec = CommandManagerFactory.getCommandManager().getCommandExecutor(jobInfo);
                updatedStatusId = cmdExec.handleRunningJob(jobInfo);
            }
            catch (CommandExecutorNotFoundException e) {
                log.error("error getting command executor for job #"+jobId, e); 
            }
            catch (Exception e) {
                log.error("error handling running job on server startup for job #"+jobId, e);
            }
            if (statusChanged(curStatus, updatedStatusId)) {
                setJobStatus(jobInfo, updatedStatusId);
            }
        }
        log.info("... done handling 'DISPATCHING' and 'PROCESSING' jobs on server startup.");
    }
    
    private static boolean statusChanged(String origStatus, int updatedStatusId) {
        int origStatusId = JobStatus.STATUS_MAP.get(origStatus);
        return origStatusId != updatedStatusId;
    }
    
    private void setJobStatus(JobInfo jobInfo, int jobStatus) {
        String jobId = ""+jobInfo.getJobNumber();
        try {
            AnalysisDAO dao = new AnalysisDAO();
            dao.updateJobStatus(jobInfo.getJobNumber(), jobStatus);
            HibernateUtil.commitTransaction();
        }
        catch (Exception e) {
            HibernateUtil.rollbackTransaction();
            log.error("Unable to set status to "+jobStatus+" for job #"+jobId);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        } 
    }

    private List<MyJobInfoWrapper> getOpenJobs() {
        List<MyJobInfoWrapper> openJobs = new ArrayList<MyJobInfoWrapper>();
        try {
            AnalysisDAO dao = new AnalysisDAO();
            int numRunningJobs = -1;
            List<Integer> statusIds = new ArrayList<Integer>();
            //statusIds.add(JobStatus.JOB_DISPATCHING);
            statusIds.add(JobStatus.JOB_PROCESSING);
            openJobs = getJobsWithStatusId(statusIds, dao, numRunningJobs);
        }
        catch (Throwable t) {
            log.error("error getting list of running jobs from the server", t);
            return new ArrayList<MyJobInfoWrapper>();
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
        
        //sort the open jobs ....
        Collections.sort(openJobs, new Comparator<MyJobInfoWrapper>() {
            //crude ranking for sorting jobs 
            private int rank(MyJobInfoWrapper o) {
                //first all jobs which are not in pipelines
                if (!o.isInPipeline && !o.isPipeline) {
                    return 0;
                }
                //then all jobs which are in pipelines
                if (o.isInPipeline && !o.isPipeline) {
                    return 1;
                }
                //then all nested pipelines
                if (o.isInPipeline && o.isPipeline) {
                    return 2;
                }
                //then all root pipelines
                return 3;
            }

            public int compare(MyJobInfoWrapper o1, MyJobInfoWrapper o2) {
                int r1 = rank(o1);
                int r2 = rank(o2);
                if (r1 == r2) {
                    return o1.jobInfo.getJobNumber() - o2.jobInfo.getJobNumber();
                }
                else {
                    return r1 - r2;
                }
            }
        });
        return openJobs;
    }
    
    /**
     * Get the list of jobs whose status is running or dispatching
     */
    private static List<MyJobInfoWrapper> getJobsWithStatusId(List<Integer> statusIds, AnalysisDAO dao, int maxJobCount) {
        List<MyJobInfoWrapper> runningJobs = new ArrayList<MyJobInfoWrapper>();
        Session session = HibernateUtil.getSession();
        //final String hql = "from org.genepattern.server.domain.AnalysisJob where ( jobStatus.statusId = :statusId or jobStatus.statusId = :dispatchingStatusId ) and deleted = false order by submittedDate ";
        final String hql = "from org.genepattern.server.domain.AnalysisJob where jobStatus.statusId in ( :statusIds ) and deleted = false order by submittedDate ";
        Query query = session.createQuery(hql);
        if (maxJobCount > 0) {
            query.setMaxResults(maxJobCount);
        }
        query.setParameterList("statusIds", statusIds);
        //query.setInteger("statusId", JobStatus.JOB_PROCESSING);
        //query.setInteger("dispatchingStatusId", JobStatus.JOB_DISPATCHING);
        List<AnalysisJob> jobList = query.list();
        for(AnalysisJob aJob : jobList) {
            JobInfo singleJobInfo = new JobInfo(aJob);
            
            final boolean closeDbSession = false;
            boolean isPipeline = JobInfoManager.isPipeline(singleJobInfo, closeDbSession);
            boolean isInPipeline = false;
            if (!isPipeline) {
                //find out if it is a top-level job
                JobInfo parentJobInfo = dao.getParent(singleJobInfo.getJobNumber());
                isInPipeline = parentJobInfo != null;
            }
            MyJobInfoWrapper m = new MyJobInfoWrapper();
            m.jobInfo = singleJobInfo;
            m.isPipeline = isPipeline;
            m.isInPipeline = isInPipeline;
            runningJobs.add(m);
        }
        return runningJobs;
    }
    
    private static class MyJobInfoWrapper {
        JobInfo jobInfo;
        boolean isPipeline;
        boolean isInPipeline;
    }
    
    //map cmdExecId - commandExecutor
    private LinkedHashMap<String,CommandExecutor> cmdExecutorsMap = new LinkedHashMap<String,CommandExecutor>();
    //the cmdExecutorsMap.get( DEFAULT_PIPELINE_EXEC_ID ) ... the name for the default pipeline executor when none is specified in the config file
    private static final String DEFAULT_PIPELINE_EXEC_ID = "DefaultGenePatternPipelineExecutor";
    
    public void addCommandExecutor(String id, CommandExecutor cmdExecutor) throws ConfigurationException {
        if (cmdExecutorsMap.containsKey(id)) {
            throw new ConfigurationException("duplicate id: "+id);
        }
        cmdExecutorsMap.put(id, cmdExecutor);
    }
    
    private String pipelineExecId = DEFAULT_PIPELINE_EXEC_ID;
    public void setPipelineExecutor(String id) {
        this.pipelineExecId = id;
    }
    public String getPipelineExecutorId() {
        return this.pipelineExecId;
    }
    
    public CommandExecutor getCommandExecutorById(String cmdExecutorId) {
        return cmdExecutorsMap.get(cmdExecutorId);
    }

    //implement the CommandExecutorMapper interface
    @Override
    public CommandExecutor getCommandExecutor(JobInfo jobInfo) throws CommandExecutorNotFoundException {
        CommandExecutor cmdExec = null;
        
        //special case for pipelines ...
        boolean isPipeline = JobInfoManager.isPipeline(jobInfo);
        if (isPipeline) {
            log.debug("job "+jobInfo.getJobNumber()+" is a pipeline");
            return getPipelineExecutor();
        }
        //TODO: special case for visualizers ... if a job is a visualizer ignore it
        //boolean isVisualizer = JobInfoManager.isVisualizer(jobInfo);
        //if (isVisualizer) {
        //    
        //}

        //initialize to default executor
        String cmdExecId = ServerConfigurationFactory.instance().getCommandExecutorId(jobInfo);
        if (cmdExecId == null) {
            log.info("no commandExecutorId found for job, use the first one from the list.");
            cmdExecId = getFirstCmdExecId();
        }
        
        cmdExec = cmdExecutorsMap.get(cmdExecId);
        if (cmdExec == null) {
            String errorMessage = "no CommandExecutor found for job: ";
            if (jobInfo != null) {
                errorMessage += (jobInfo.getJobNumber()+". "+jobInfo.getTaskName());
            }
            else {
                errorMessage += "null";
            }
            throw new CommandExecutorNotFoundException(errorMessage);
        }
        return cmdExec;
    }

    private String getFirstCmdExecId() {
        String firstKey = cmdExecutorsMap.keySet().iterator().next();
        return firstKey;
    }
    
    /**
     * @deprecated, newer implementations of the CommandExecutor interface should call GpConfig.getValue(GpContext gpContext, String key) instead.
     */
    @Override
    public CommandProperties getCommandProperties(JobInfo jobInfo) {
        CommandProperties props = ServerConfigurationFactory.instance().getCommandProperties(jobInfo);
        CommandProperties commandProps = new CommandProperties(props);
        return commandProps;
    }

    public Map<String, CommandExecutor> getCommandExecutorsMap() {
        return Collections.unmodifiableMap(cmdExecutorsMap);
    }

    /**
     * Call this at system startup to initialize the list of CommandExecutorService instances.
     * Don't start the pipeline executor until all of the other executors have started.
     */
    public void startCommandExecutors() { 
        //need this first to identify which executor in the list is the pipeline executor
        initPipelineExecutor();

        for(String cmdExecId : cmdExecutorsMap.keySet()) {
            if (cmdExecId.equals(pipelineExecId)) {
                //don't start the pipeline executor
            }
            else {
                CommandExecutor cmdExec = cmdExecutorsMap.get(cmdExecId);
                if (cmdExec == null) {
                    log.error("null CommandExecutor for cmdExecId: '"+cmdExecId+"'");
                }
                else {
                    try {
                        cmdExec.start();
                    }
                    catch (Throwable t) {
                        log.error("Error starting CommandExecutor, for class: "+cmdExec.getClass().getCanonicalName()+": "+t.getLocalizedMessage(), t);
                    }
                }
            }
        }
        
        //start pipeline executor
        getPipelineExecutor().start();
    }
    
    /**
     * Call this at system shutdown to stop the list of running CommandExecutorService instances.
     * 
     * Implementation note: pipeline execution is treated as a special-case. First, terminate all running pipelines, then terminate any remaining jobs.
     */
    public void stopCommandExecutors() {
        CommandExecutor pipelineExec = getPipelineExecutor();
        if (pipelineExec != null) {
            try {
                pipelineExec.stop();
            }
            catch (Throwable t) {
                log.error("Error stopping PipelineExecutor", t);
            }
        }
        
        for(String cmdExecId : cmdExecutorsMap.keySet()) {
            CommandExecutor cmdExec = cmdExecutorsMap.get(cmdExecId);
            try {
                if (cmdExec != pipelineExec) {
                    cmdExec.stop();
                }
            }
            catch (Throwable t) {
                log.error("Error stopping CommandExecutorService, for class: "+cmdExec.getClass().getCanonicalName()+": "+t.getLocalizedMessage(), t);
            }
        }
    }
    
    private synchronized void initPipelineExecutor() {
        log.debug("initializing pipeline executor");
        this.pipelineExecId = DEFAULT_PIPELINE_EXEC_ID;
        CommandExecutor pipelineExec = cmdExecutorsMap.get(pipelineExecId);
        if (pipelineExec == null) {
            //initialize to default setting
            pipelineExec = new PipelineExecutor();
            cmdExecutorsMap.put(pipelineExecId, pipelineExec);
        }
        log.info("pipeline.executor, id="+pipelineExecId);
        log.info("pipeline.executor, class="+pipelineExec.getClass().getCanonicalName());
    }
    
    private CommandExecutor getPipelineExecutor() {
        CommandExecutor pipelineExec = cmdExecutorsMap.get(pipelineExecId);
        if (pipelineExec == null) {
            log.error("Pipeline configuration error: no CommandExecutor exists with id="+pipelineExecId);
            //hard-coded to default pipeline execution in spite of config file error
            pipelineExec = new PipelineExecutor();
            cmdExecutorsMap.put(pipelineExecId, pipelineExec);
        }
        return pipelineExec;
    }
    
    public void wakeupJobQueue() {
        if (analysisTaskScheduler == null) {
            log.error("analysisTaskScheduler is null");
        }
        analysisTaskScheduler.wakeupJobQueue();
    }
    
    public void suspendJobQueue() {
        if (analysisTaskScheduler != null) {
            analysisTaskScheduler.suspendJobQueue();
        }
    }
    
    public void resumeJobQueue() {
        if (analysisTaskScheduler == null) {
            analysisTaskScheduler = new AnalysisJobScheduler(getJobQueueSuspendedFlag());
        }
        analysisTaskScheduler.resumeJobQueue();
    }
    
    public boolean isSuspended() {
        if (analysisTaskScheduler != null) {
            return analysisTaskScheduler.isSuspended();
        }
        return true;
    }
    
    public void terminateJob(final Integer jobId) throws JobTerminationException {
        if (analysisTaskScheduler == null) {
            throw new JobTerminationException("Did not terminate jobId="+jobId+", analysisTaskScheduler not instantiated!");
        }
        analysisTaskScheduler.terminateJob(jobId);
    }
    
    public void terminateJob(final JobInfo jobInfo) throws JobTerminationException {
        if (analysisTaskScheduler == null) {
            throw new JobTerminationException("Did not terminate jobId="+jobInfo.getJobNumber()+", analysisTaskScheduler not instantiated!");
        }
        analysisTaskScheduler.terminateJob(jobInfo);
    }
    
    
}
