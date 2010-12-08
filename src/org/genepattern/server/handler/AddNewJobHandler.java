/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2009) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.handler;

import org.apache.log4j.Logger;
import org.genepattern.server.TaskIDNotFoundException;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.executor.CommandManagerFactory;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.ParameterInfo;

/**
 * AddNewJobHandler to submit a job request and get back <CODE>JobInfo</CODE>
 *
 * @author rajesh kuttan
 * @version 1.0
 */

public class AddNewJobHandler extends RequestHandler {
    private static Logger log = Logger.getLogger(AddNewJobHandler.class);
    private int taskID = 1;
    private String parameter_info = "";
    private ParameterInfo[] parameterInfoArray = null;
    private String userID;
    //default value of -1 means the job has no parent
    private int parentJobID = -1;
    
    private boolean wakeupJobQueue = true;
    
    /**
     * Constructor with taskID, ParameterInfo[]
     *
     * @param taskID
     *            taskID from <CODE>TaskInfo</CODE>
     * @param parameterInfoArray
     *            <CODE>ParameterInfo</CODE>
     */
    public AddNewJobHandler(int taskID, String userID, ParameterInfo[] parameterInfoArray) {
        this.taskID = taskID;
        this.userID = userID;
        this.parameterInfoArray = parameterInfoArray;
    }
    
    /**
     * Constructor with taskID, ParameterInfo[] and parentJobID
     *
     * @param taskID
     *            taskID from <CODE>TaskInfo</CODE>
     * @param parameterInfoArray
     *            <CODE>ParameterInfo</CODE>
     * @param parentJobID
     *            the parent job number
     */
    public AddNewJobHandler(int taskID, String userID, ParameterInfo[] parameterInfoArray, int parentJobID) {
        this.taskID = taskID;
        this.userID = userID;
        this.parameterInfoArray = parameterInfoArray;
        this.parentJobID = parentJobID;
    }
    
    public void setWakeupJobQueueFlag(boolean b) {
        this.wakeupJobQueue = b;
    }

    /**
     * Creates job. Call this fun. if you need JobInfo object
     *
     * @throws TaskIDNotFoundException
     *             TaskIDNotFoundException
     * @throws OmnigeneException
     * @return <CODE>JobIndo</CODE>
     */
    public JobInfo executeRequest() throws OmnigeneException, TaskIDNotFoundException {
        JobInfo ji = null;
        try {
            if(log.isDebugEnabled()) {
                log.debug("executeRequest");
            }
            parameter_info = ParameterFormatConverter.getJaxbString(parameterInfoArray);
            
            // Insert job record.  Transaction is committed to avoid deadlock.
            HibernateUtil.beginTransaction();
            AnalysisDAO ds = new AnalysisDAO();
            ji = ds.addNewJob(taskID, userID, parameter_info, parentJobID);

            // Checking for null
            if (ji == null) {
                HibernateUtil.rollbackTransaction();
                throw new OmnigeneException(
                    "AddNewJobRequest:executeRequest Operation failed, null value returned for JobInfo");
            }
            
            HibernateUtil.commitTransaction();
            
            if (wakeupJobQueue) {
                log.debug("Waking up job queue");                
                CommandManagerFactory.getCommandManager().wakeupJobQueue();
            }
            
            // Reparse parameter_info before sending to client
            ji.setParameterInfoArray(ParameterFormatConverter.getParameterInfoArray(parameter_info));
        } 
        catch (TaskIDNotFoundException taskEx) {
            HibernateUtil.rollbackTransaction();
            log.error("AddNewJob(executeRequest) " + taskID, taskEx);
            throw taskEx;
        } 
        catch (Exception ex) {
            HibernateUtil.rollbackTransaction();
            log.error("AddNewJob(executeRequest): Error ",  ex);
            throw new OmnigeneException(ex.getMessage());
        }
        
        return ji;
    }
}
