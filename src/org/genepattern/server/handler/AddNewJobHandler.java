/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.handler;

import org.apache.log4j.Logger;
import org.genepattern.server.JobManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.executor.CommandManagerFactory;
import org.genepattern.server.executor.JobSubmissionException;
import org.genepattern.server.jobqueue.JobQueue;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * AddNewJobHandler to submit a job request and get back <CODE>JobInfo</CODE>
 *
 * @author rajesh kuttan
 * @version 1.0
 */
public class AddNewJobHandler extends RequestHandler {
    private static Logger log = Logger.getLogger(AddNewJobHandler.class);

    private String userId;
    private int taskId = 1;
    private ParameterInfo[] parameterInfoArray = null;
    //default value of -1 means the job has no parent
    private int parentJobId = -1;
    
    private final boolean wakeupJobQueue = true;
    
    /**
     * Constructor with taskID, ParameterInfo[]
     *
     * @param taskId
     * @param parameterInfoArray
     */
    public AddNewJobHandler(int taskId, String userId, ParameterInfo[] parameterInfoArray) {
        this(taskId, userId, parameterInfoArray, -1);
    }
    
    /**
     * Constructor with taskId, ParameterInfo[] and parentJobID
     *
     * @param taskId
     *            taskID from <CODE>TaskInfo</CODE>
     * @param parameterInfoArray
     *            <CODE>ParameterInfo</CODE>
     * @param parentJobID
     *            the parent job number
     */
    public AddNewJobHandler(int taskId, String userId, ParameterInfo[] parameterInfoArray, int parentJobId) {
        this.taskId = taskId;
        this.userId = userId;
        this.parameterInfoArray = parameterInfoArray;
        this.parentJobId = parentJobId;
    }

    /**
     * Adds the job to GenePattern and commits the changes to the DB.
     * Delegates to JobManager, which does not commit to the DB.
     * @return the newly created JobInfo
     * @throws JobSubmissionException
     */
    public JobInfo executeRequest() throws JobSubmissionException {
        TaskInfo taskInfo = null;
        try {
            HibernateUtil.beginTransaction();
            taskInfo = new AdminDAO().getTask(taskId);
            JobInfo jobInfo = JobManager.addJobToQueue(taskInfo, userId, parameterInfoArray, parentJobId, JobQueue.Status.PENDING);
            HibernateUtil.commitTransaction();
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
