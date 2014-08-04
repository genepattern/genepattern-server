package org.genepattern.server.rest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.eula.GetTaskStrategy;
import org.genepattern.server.eula.GetTaskStrategyDefault;
import org.genepattern.server.executor.CommandManagerFactory;
import org.genepattern.server.executor.JobSubmissionException;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamListHelper;
import org.genepattern.server.job.input.dao.JobInputValueRecorder;
import org.genepattern.server.jobqueue.JobQueue;
import org.genepattern.server.jobqueue.JobQueueUtil;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * This class accepts a job submit form from the end user and adds a job to the queue.
 * Added in GP 3.8.1 for handling optional job configuration parameters as part of the input form.
 * 
 * special cases:
 *     1) when a value is not supplied for an input parameter, the default value will be used
 *         if there is no default value, then assume that the value is not set.
 *     2) when a parameter is not optional, throw an exception if no value has been set
 *     3) when a list of values is supplied, automatically generate a file list file before adding the job to the queue
 *     4) when an external URL is supplied, GET the contents of the file before adding the job to the queue
 *     
 *     Note: 
 *     5) transferring data from an external URL as well as generating a file list can take a while, we should
 *         update this code so that it does not have to block the web client.
 *     
 * @author pcarr
 * 
 *
 */
public class JobInputApiImplV2 implements JobInputApi {
    final static private Logger log = Logger.getLogger(JobInputApiImplV2.class);
    
    private final GetTaskStrategy getTaskStrategy;
    /**
     * special-case, as a convenience, for input parameters which have not been set, initialize them from their default value.
     * By default, this value is false.
     */
    private final boolean initDefault;
    public JobInputApiImplV2() {
        this(false);
    }
    public JobInputApiImplV2(final boolean initDefault) {
        this(null, initDefault);
    }
    public JobInputApiImplV2(final GetTaskStrategy getTaskStrategy, final boolean initDefault) {
        this.getTaskStrategy=getTaskStrategy;
        this.initDefault=initDefault;
    }

    @Override
    public String postJob(final GpContext taskContext, final JobInput jobInput) throws GpServerException {
        if (taskContext==null) {
            throw new IllegalArgumentException("taskContext==null");
        }
        if (taskContext.getUserId()==null) {
            throw new IllegalArgumentException("taskContext.userId==null");
        }
        if (jobInput==null) {
            throw new IllegalArgumentException("jobInput==null");
        }
        if (jobInput.getLsid()==null) {
            throw new IllegalArgumentException("jobInput.lsid==null");
        }
        try {
            JobInputHelper jobInputHelper=new JobInputHelper(taskContext, jobInput, getTaskStrategy, initDefault);
            final String jobId=jobInputHelper.submitJob();
            return jobId;
        }
        catch (Throwable t) {
            String message="Error adding job to queue, currentUser="+taskContext.getUserId()+", lsid="+jobInput.getLsid();
            log.error(message,t);
            throw new GpServerException(t.getLocalizedMessage(), t);
        }
    }
    
    // copied implementation from JobInputApiLegacy class
    private static class JobInputHelper {
        final static private Logger log = Logger.getLogger(JobInputApiLegacy.class);

        private final GpContext taskContext;
        private final JobInput jobInput;
        private final boolean initDefault;
        private final int parentJobId;

        public JobInputHelper(final GpContext taskContext, final JobInput jobInput, final GetTaskStrategy getTaskStrategyIn, final boolean initDefault) {
            this(taskContext, jobInput, getTaskStrategyIn, initDefault, -1);
        }
        public JobInputHelper(final GpContext taskContext, final JobInput jobInput, final GetTaskStrategy getTaskStrategyIn, final boolean initDefault, final int parentJobId) {
            if (taskContext==null) {
                throw new IllegalArgumentException("taskContext==null");
            }
            this.taskContext=taskContext;
            this.jobInput=jobInput;
            this.initDefault=initDefault;
            this.parentJobId=parentJobId;
            
            if (taskContext.getTaskInfo()==null) {
                log.debug("taskContext.taskInfo is null, initialize from getTaskStrategy");
                final GetTaskStrategy getTaskStrategy;
                if (getTaskStrategyIn == null) {
                    getTaskStrategy=new GetTaskStrategyDefault();
                }
                else {
                    getTaskStrategy=getTaskStrategyIn;
                }
                final TaskInfo taskInfo=getTaskStrategy.getTaskInfo(jobInput.getLsid());
                taskContext.setTaskInfo(taskInfo);
            }
            taskContext.getTaskInfo().getParameterInfoArray();
        }

        private ParameterInfo[] initParameterValues() throws Exception  {
            //initialize a map of paramName to ParameterInfo 
            final Map<String,ParameterInfoRecord> paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskContext.getTaskInfo());

            //for each formal input parameter ... set the actual value to be used on the command line
            for(Entry<String,ParameterInfoRecord> entry : paramInfoMap.entrySet()) {
                // validate num values
                // and initialize input file (or parameter) lists as needed
                Param inputParam=jobInput.getParam( entry.getKey() );
                ParamListHelper plh=new ParamListHelper(taskContext, entry.getValue(), inputParam, initDefault);
                plh.validateNumValues();
                plh.updatePinfoValue();
            }

            List<ParameterInfo> actualParameters = new ArrayList<ParameterInfo>();
            for(ParameterInfoRecord pinfoRecord : paramInfoMap.values()) {
                actualParameters.add( pinfoRecord.getActual() );
            }
            ParameterInfo[] actualParams = actualParameters.toArray(new ParameterInfo[0]);
            return actualParams;
        }

        public String submitJob() throws Exception {
            final ParameterInfo[] actualValues=initParameterValues();
            final Integer jobNo = executeRequest(taskContext.getTaskInfo(), taskContext.getUserId(), actualValues, parentJobId);
            final String jobId = "" + jobNo;
            return jobId;
        }
        
        //copied from AddNewJobHandler#executeRequest
        //        AddNewJobHandler req = new AddNewJobHandler(taskID, userContext.getUserId(), parameters);
        //        JobInfo jobInfo = req.executeRequest();
        /**
         * Adds the job to GenePattern and commits the changes to the DB.
         * Delegates to JobManager, which does not commit to the DB.
         * @return the newly created JobInfo
         * @throws JobSubmissionException
         */
        private Integer executeRequest(final TaskInfo taskInfo, final String userId, final ParameterInfo[] parameterInfoArray, final int parentJobId ) throws JobSubmissionException {
            try {
                HibernateUtil.beginTransaction();
                final Integer jobNo = addJobToQueue(taskInfo, userId, parameterInfoArray, parentJobId, JobQueue.Status.PENDING);
                HibernateUtil.commitTransaction();
                final boolean wakeupJobQueue = true;
                if (wakeupJobQueue) {
                    log.debug("Waking up job queue");                
                    CommandManagerFactory.getCommandManager().wakeupJobQueue();
                }
                return jobNo;
            }
            catch (JobSubmissionException e) {
                HibernateUtil.rollbackTransaction();
                throw e;
            }
            catch (Throwable t) {
                HibernateUtil.rollbackTransaction();
                throw new JobSubmissionException("Unexpected error adding task="+taskInfo.getName()+" for user="+userId, t);
            }
        }
        
        //copied from JobManager#addJobToQueue  
        /**
         * Adds a new job entry to the ANALYSIS_JOB table, with initial status either PENDING or WAITING.
         * 
         * @param taskID
         * @param userID
         * @param parameterInfoArray
         * @param parentJobID
         * @param jobStatusId
         * @return
         * @throws JobSubmissionException
         */
        private Integer addJobToQueue(
            final TaskInfo taskInfo, 
            final String userId, 
            final ParameterInfo[] parameterInfoArray, 
            final Integer parentJobNumber, 
            final JobQueue.Status initialJobStatus
        ) throws JobSubmissionException
        {
            try {
                AnalysisDAO ds = new AnalysisDAO();
                Integer jobNo = ds.addNewJob(userId, taskInfo, parameterInfoArray, parentJobNumber);
                if (jobNo == null) {
                    throw new JobSubmissionException(
                            "addJobToQueue: Operation failed, null value returned for JobInfo");
                }
                final JobInfo jobInfo = ds.getJobInfo(jobNo);
                new JobInputValueRecorder().saveJobInput(jobNo, jobInput);

                createJobDirectory(taskContext, jobNo);

                //add record to the internal job queue, for dispatching ...
                JobQueueUtil.addJobToQueue(jobInfo, initialJobStatus);
                return jobNo;
            }
            catch (JobSubmissionException e) {
                throw e;
            }
            catch (Throwable t) {
                throw new JobSubmissionException(t);
            }
        }

        /**
         * Create the job directory for a newly added job.
         * This method requires a valid jobId, but does not check if the jobId is valid.
         * 
         * @throws IllegalArgumentException, JobDispatchException
         */
        public static File createJobDirectory(final GpContext taskContext, final int jobNumber) throws JobSubmissionException { 
            File jobDir = null;
            try {
                jobDir = getWorkingDirectory(taskContext, jobNumber);
            }
            catch (Throwable t) {
                throw new JobSubmissionException(t.getLocalizedMessage());
            }

            //Note: should record the working dir with the jobInfo and save to DB
            //jobInfo.setWorkingDir(jobDir.getPath());
            // make directory to hold input and output files
            if (!jobDir.exists()) {
                boolean success = jobDir.mkdirs();
                if (!success) {
                    throw new JobSubmissionException("Error creating working directory for job #" + jobNumber +", jobDir=" + jobDir.getPath());
                }
            } 
            else {
                // clean out existing directory
                if (log.isDebugEnabled()) {
                    log.debug("clean out existing directory");
                }
                File[] old = jobDir.listFiles();
                for (int i = 0; old != null && i < old.length; i++) {
                    old[i].delete();
                }
            }
            return jobDir;
        }

        /**
         * Get the working directory for the given job.
         * In GP 3.3.2 and earlier, this is hard-coded based on a configured property.
         * In future releases, the working directory is configurable, and must be stored in the DB.
         * @param jobInfo
         * @return
         */
        private static File getWorkingDirectory(final GpContext jobContext, final int jobNumber) throws Exception {
            try {
                File rootJobDir = ServerConfigurationFactory.instance().getRootJobDir(jobContext);
                File jobDir = new File(rootJobDir, ""+jobNumber);
                return jobDir;
            }
            catch (Throwable t) {
                throw new Exception("Unexpected error getting working directory for jobId="+jobNumber, t);
            }
        }
        
    }

}
