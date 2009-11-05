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

import org.genepattern.server.TaskIDNotFoundException;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.ParameterInfo;

public class AddNewJobHandlerNoWakeup extends AddNewJobHandler {

	   /** Creates new GetAvailableTasksHandler */
    public AddNewJobHandlerNoWakeup() {
        super();
    }

    /**
     * Constructor with taskID, ParameterInfo[] and inputFileName
     * 
     * @param taskID
     *            taskID from <CODE>TaskInfo</CODE>
     * @param parameterInfoArray
     *            <CODE>ParameterInfo</CODE>
     * @param inputFileName
     *            String
     */
    public AddNewJobHandlerNoWakeup(int taskID, String userID, ParameterInfo[] parameterInfoArray) {
        super(taskID, userID, parameterInfoArray);
        
    }

    /**
     * Constructor with taskID, ParameterInfo[], inputFileName, and parentJobID
     * 
     * @param taskID
     *            taskID from <CODE>TaskInfo</CODE>
     * @param parameterInfoArray
     *            <CODE>ParameterInfo</CODE>
     * @param inputFileName
     *            String
     * @param parentJobID
     *            the parent job number
     */
    public AddNewJobHandlerNoWakeup(int taskID, String userID, ParameterInfo[] parameterInfoArray, int parentJobID) {
       super(taskID, userID, parameterInfoArray, parentJobID);
    	
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
            parameter_info = ParameterFormatConverter.getJaxbString(parameterInfoArray);
            
            HibernateUtil.beginTransaction();
            AnalysisDAO ds = new AnalysisDAO();
            if (hasParent) {
                ji = ds.addNewJob(taskID, userID, parameter_info, parentJobID);
            }
            else {
                ji = ds.addNewJob(taskID, userID, parameter_info, -1);
            }
            HibernateUtil.commitTransaction();
            // Checking for null
            if (ji == null) throw new OmnigeneException(
                    "AddNewJobRequest:executeRequest Operation failed, null value returned for JobInfo");

            // don't wake up the Queue.  This is what seperates this from its superclass
            //AnalysisTask.getInstance().wakeupJobQueue();
            //
            
            // Reparse parameter_info before sending to client
            ji.setParameterInfoArray(ParameterFormatConverter.getParameterInfoArray(parameter_info));
        }
        catch (TaskIDNotFoundException taskEx) {
            HibernateUtil.rollbackTransaction();
            System.out.println("AddNewJob(executeRequest): TaskIDNotFoundException " + taskID);
            throw taskEx;
        }
        catch (Exception ex) {
            HibernateUtil.rollbackTransaction();
            System.out.println("AddNewJob(executeRequest): Error " + ex.getMessage());
            ex.printStackTrace();
            throw new OmnigeneException(ex.getMessage());
        }

        return ji;
    }
	
}
