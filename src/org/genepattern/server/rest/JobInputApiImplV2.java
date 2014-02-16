package org.genepattern.server.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.server.JobManager;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.eula.GetTaskStrategy;
import org.genepattern.server.eula.GetTaskStrategyDefault;
import org.genepattern.server.executor.CommandManagerFactory;
import org.genepattern.server.executor.JobSubmissionException;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamListHelper;
import org.genepattern.server.jobqueue.JobQueue;
import org.genepattern.server.webservice.server.dao.AdminDAO;
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
 * TODO: 
 *     5) transferring data from an external URL as well as generating a file list can take a while, we should
 *         update this code so that it does not have to block the web client.
 *     
 * @author pcarr
 * 
 *
 */
public class JobInputApiImplV2 implements JobInputApi {
    final static private Logger log = Logger.getLogger(JobInputApiImplV2.class);
    
    private GetTaskStrategy getTaskStrategy;
    /**
     * special-case, as a convenience, for input parameters which have not been set, initialize them from their default value.
     * By default, this value is false.
     */
    private boolean initDefault=false;
    protected JobInputApiImplV2() {
    }
    protected JobInputApiImplV2(final boolean initDefault) {
        this.initDefault=initDefault;
    }
    protected JobInputApiImplV2(final GetTaskStrategy getTaskStrategy) {
        this.getTaskStrategy=getTaskStrategy;
    }
    protected JobInputApiImplV2(final GetTaskStrategy getTaskStrategy, final boolean initDefault) {
        this.getTaskStrategy=getTaskStrategy;
        this.initDefault=initDefault;
    }
    
    /**
     * Optionally set the strategy for initializing a TaskInfo from a task lsid.
     * 
     * @param impl, an object which implements this interface, can be null.
     */
    public void setGetTaskStrategy(final GetTaskStrategy getTaskStrategy) {
        this.getTaskStrategy=getTaskStrategy;
    }

    @Override
    public String postJob(final Context jobContext, final JobInput jobInput) throws GpServerException {
        if (jobContext==null) {
            throw new IllegalArgumentException("jobContext==null");
        }
        if (jobContext.getUserId()==null) {
            throw new IllegalArgumentException("jobContext.userId==null");
        }
        if (jobInput==null) {
            throw new IllegalArgumentException("jobInput==null");
        }
        if (jobInput.getLsid()==null) {
            throw new IllegalArgumentException("jobInput.lsid==null");
        }
        try {
            JobInputHelper jobInputHelper=new JobInputHelper(jobContext, jobInput, getTaskStrategy, initDefault);
            final String jobId=jobInputHelper.submitJob();
            return jobId;
        }
        catch (Throwable t) {
            String message="Error adding job to queue, currentUser="+jobContext.getUserId()+", lsid="+jobInput.getLsid();
            log.error(message,t);
            throw new GpServerException(t.getLocalizedMessage(), t);
        }
    }
    
    // copied implementation from JobInputApiLegacy class
    private static class JobInputHelper {
        final static private Logger log = Logger.getLogger(JobInputApiLegacy.class);

        private Context userContext;
        private JobInput jobInput;
        private TaskInfo taskInfo;
        private final boolean initDefault;
        private final int parentJobId;

        public JobInputHelper(final Context userContext, final JobInput jobInput, final GetTaskStrategy getTaskStrategyIn, final boolean initDefault) {
            this(userContext, jobInput, getTaskStrategyIn, initDefault, -1);
        }
        public JobInputHelper(final Context userContext, final JobInput jobInput, final GetTaskStrategy getTaskStrategyIn, final boolean initDefault, final int parentJobId) {
            this.userContext=userContext;
            this.jobInput=jobInput;
            this.initDefault=initDefault;
            this.parentJobId=parentJobId;

            final GetTaskStrategy getTaskStrategy;
            if (getTaskStrategyIn == null) {
                getTaskStrategy=new GetTaskStrategyDefault();
            }
            else {
                getTaskStrategy=getTaskStrategyIn;
            }
            this.taskInfo=getTaskStrategy.getTaskInfo(jobInput.getLsid());
            this.taskInfo.getParameterInfoArray();
            userContext.setTaskInfo(taskInfo);
        }

        public TaskInfo getTaskInfo() {
            return taskInfo;
        }

        private ParameterInfo[] initParameterValues() throws Exception 
        {
            if (jobInput.getParams()==null) {
                log.debug("jobInput.params==null");
                return new ParameterInfo[0];
            }

            //initialize a map of paramName to ParameterInfo 
            final Map<String,ParameterInfoRecord> paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskInfo);

            //for each formal input parameter ... set the actual value to be used on the command line
            for(Entry<String,ParameterInfoRecord> entry : paramInfoMap.entrySet()) {
                // validate num values
                // and initialize input file (or parameter) lists as needed
                Param inputParam=jobInput.getParam( entry.getKey() );
                ParamListHelper plh=new ParamListHelper(userContext, entry.getValue(), inputParam, initDefault);
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
            final int taskId=getTaskInfo().getID();
            final JobInfo jobInfo=submitJob(taskId, actualValues);
            final String jobId = "" + jobInfo.getJobNumber();
            return jobId;
        }

        public JobInfo submitJob(final int taskID, final ParameterInfo[] parameters) throws JobSubmissionException {
            final JobInfo jobInfo = executeRequest(taskID, userContext.getUserId(), parameters, parentJobId);
            return jobInfo;
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
        private JobInfo executeRequest(final int taskId, final String userId, final ParameterInfo[] parameterInfoArray, final int parentJobId ) throws JobSubmissionException {
            TaskInfo taskInfo = null;
            try {
                HibernateUtil.beginTransaction();
                taskInfo = new AdminDAO().getTask(taskId);
                JobInfo jobInfo = JobManager.addJobToQueue(taskInfo, userId, parameterInfoArray, parentJobId, JobQueue.Status.PENDING);
                HibernateUtil.commitTransaction();
                final boolean wakeupJobQueue = true;
                if (wakeupJobQueue) {
                    log.debug("Waking up job queue");                
                    CommandManagerFactory.getCommandManager().wakeupJobQueue();
                }
                return jobInfo;
            }
            catch (JobSubmissionException e) {
                HibernateUtil.rollbackTransaction();
                throw e;
            }
            catch (Throwable t) {
                HibernateUtil.rollbackTransaction();
                throw new JobSubmissionException("Unexpected exception thrown while adding job to queue: taskID="+taskId, t);
            }
        }

    }
    

}
