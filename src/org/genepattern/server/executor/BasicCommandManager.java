package org.genepattern.server.executor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.server.JobInfoManager;
import org.genepattern.server.config.ConfigurationException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.executor.drm.JobExecutor;
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
    private final GpConfig gpConfig;
    
    public BasicCommandManager() {
        this(null);
    }

    public BasicCommandManager(final GpConfig gpConfig) {
        this.gpConfig=gpConfig;
    }
    
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
        if (gpConfig != null) {
            return gpConfig.getGPBooleanProperty(serverContext, "job.queue.suspend_on_start");
        } 
        else {
            return ServerConfigurationFactory.instance().getGPBooleanProperty(serverContext, "job.queue.suspend_on_start");
        }
    }
    
    //Note: would be nice to use paged results to handle large number of 'Processing' jobs
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
                CommandExecutor cmdExec = CommandManagerFactory.getCommandManager().getCommandExecutor(jobInfo, jobInfoWrapper.isPipeline);
                updatedStatusId = cmdExec.handleRunningJob(jobInfo);
            }
            catch (CommandExecutorNotFoundException e) {
                log.error("error getting command executor for job #"+jobId, e); 
            }
            catch (Throwable t) {
                log.error("error handling running job on server startup for job #"+jobId, t);
            }
            if (statusChanged(curStatus, updatedStatusId)) {
                setJobStatus(jobInfo, updatedStatusId);
            }
        }
        log.info("... done handling 'DISPATCHING' and 'PROCESSING' jobs on server startup.");
    }
    
    private static boolean statusChanged(String origStatus, int updatedStatusId) {
        // ignore -1
        if (updatedStatusId < 0) {
            return false;
        }
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
        final String hql = "from org.genepattern.server.domain.AnalysisJob where jobStatus.statusId in ( :statusIds ) and deleted = false order by submittedDate ";
        Query query = session.createQuery(hql);
        if (maxJobCount > 0) {
            query.setMaxResults(maxJobCount);
        }
        query.setParameterList("statusIds", statusIds);
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
    private LinkedHashMap<String,JobExecutor> jobExecutorsMap = new LinkedHashMap<String,JobExecutor>();
    
    public void addCommandExecutor(String id, CommandExecutor cmdExecutor) throws ConfigurationException {
        if (cmdExecutorsMap.containsKey(id)) {
            throw new ConfigurationException("duplicate id: "+id);
        }
        cmdExecutorsMap.put(id, cmdExecutor);
        //special-case for JobExecutor
        if (cmdExecutor instanceof JobExecutor) {
            JobExecutor jobExec = (JobExecutor) cmdExecutor;
            if (jobExecutorsMap.containsKey(jobExec.getJobRunnerName())) {
                throw new ConfigurationException("duplicate jobRunnerName: "+jobExec.getJobRunnerName());
            }
            jobExecutorsMap.put( jobExec.getJobRunnerName(), jobExec );
        }
    }
    
    public CommandExecutor getCommandExecutorById(String cmdExecutorId) {
        return cmdExecutorsMap.get(cmdExecutorId);
    }

    public CommandExecutor getCommandExecutor(final GpConfig gpConfig, final JobInfo jobInfo) throws CommandExecutorNotFoundException {
        boolean isPipeline = JobInfoManager.isPipeline(jobInfo);
        return getCommandExecutor(gpConfig, jobInfo, isPipeline);
    }
    
    public CommandExecutor getCommandExecutor(final GpConfig gpConfig, final JobInfo jobInfo, final boolean isPipeline) throws CommandExecutorNotFoundException {
        CommandExecutor cmdExec = null;
        
        //special case for pipelines ...
        if (isPipeline) {
            log.debug("job "+jobInfo.getJobNumber()+" is a pipeline");
            return getPipelineExecutor();
        }

        //initialize to default executor
        String cmdExecId = gpConfig.getCommandExecutorId(jobInfo);
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
    
    public CommandProperties getCommandProperties(final GpConfig gpConfig, final JobInfo jobInfo) {
        CommandProperties props = gpConfig.getCommandProperties(jobInfo);
        CommandProperties commandProps = new CommandProperties(props);
        return commandProps;
    }
    
    public String getCommandExecutorId(CommandExecutor cmdExecutor) {
        if (cmdExecutorsMap==null) {
            log.error("cmdExecutorsMap==null");
            return null;
        }
        if (!cmdExecutorsMap.containsValue(cmdExecutor)) {
            log.error("commandExecutorsMap does not contain value for "+cmdExecutor.getClass().getCanonicalName());
            return null;
        }
        for(Entry<String,CommandExecutor> entry : cmdExecutorsMap.entrySet()) {
            if(cmdExecutor == entry.getValue()) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    /**
     * For a given jobRunnerName, get the JobExecutor instance that was initialized at startup from the
     * config_yaml file. This name is usually selected from the 'jr_name' column of the  'job_runner_job' table in the DB.
     * This was added to support reverse-looked to ge the correct JobExecutor that was used to launch a job.
     * 
     * Assumptions:
     *     Each 'jobRunnerName' in the config_yaml file must be unique.
     *     The 'jobRunnerName' must always be associated with the same exact JobRunnerClassname.
     * 
     * @param jobRunnerName
     * @return
     */
    public JobExecutor lookupJobExecutorByJobRunnerName(final String jobRunnerName) {
        if (jobExecutorsMap == null) {
            return null;
        }
        return jobExecutorsMap.get(jobRunnerName);
    }

    //implement the CommandExecutorMapper interface
    @Override
    public CommandExecutor getCommandExecutor(JobInfo jobInfo) throws CommandExecutorNotFoundException {
        boolean isPipeline = JobInfoManager.isPipeline(jobInfo);
        return getCommandExecutor(jobInfo, isPipeline);
    }

    public CommandExecutor getCommandExecutor(final JobInfo jobInfo, final boolean isPipeline) throws CommandExecutorNotFoundException {
        if (gpConfig != null) {
            return getCommandExecutor(gpConfig, jobInfo, isPipeline);
        }
        else {
            return getCommandExecutor(ServerConfigurationFactory.instance(), jobInfo, isPipeline);
        }
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
        if (gpConfig != null) {
            return getCommandProperties(gpConfig, jobInfo);
        }
        else {
            return getCommandProperties(ServerConfigurationFactory.instance(), jobInfo);
        }
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
            if (cmdExecId.equals(PIPELINE_EXEC_ID)) {
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
        CommandExecutor pipelineExec = cmdExecutorsMap.get(PIPELINE_EXEC_ID);
        if (pipelineExec == null) {
            //initialize to default setting
            pipelineExec = new PipelineExecutor();
            cmdExecutorsMap.put(PIPELINE_EXEC_ID, pipelineExec);
        }
        log.info("pipeline.executor, id="+PIPELINE_EXEC_ID);
        log.info("pipeline.executor, class="+pipelineExec.getClass().getCanonicalName());
    }
    
    private CommandExecutor getPipelineExecutor() {
        CommandExecutor pipelineExec = cmdExecutorsMap.get(PIPELINE_EXEC_ID);
        if (pipelineExec == null) {
            log.error("Pipeline configuration error: no CommandExecutor exists with id="+PIPELINE_EXEC_ID);
            //hard-coded to default pipeline execution in spite of config file error
            pipelineExec = new PipelineExecutor();
            cmdExecutorsMap.put(PIPELINE_EXEC_ID, pipelineExec);
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
