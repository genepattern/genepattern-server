/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.handler;

import org.genepattern.server.AnalysisTask;
import org.genepattern.server.TaskIDNotFoundException;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.ParameterInfo;

// import edu.mit.wi.omnigene.omnidas.*;

/**
 * AddNewJobHandler to submit a job request and get back <CODE>JobInfo</CODE>
 * 
 * @author rajesh kuttan
 * @version 1.0
 */

public class AddNewJobHandler extends RequestHandler {

    private int taskID = 1;

    private String parameter_info = "", inputFileName = "";

    private ParameterInfo[] parameterInfoArray = null;

    private String userID;

    private int parentJobID;

    private boolean hasParent = false;

    /** Creates new GetAvailableTasksHandler */
    public AddNewJobHandler() {
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
    public AddNewJobHandler(int taskID, String userID, ParameterInfo[] parameterInfoArray) {
        this.taskID = taskID;
        this.userID = userID;
        this.parameterInfoArray = parameterInfoArray;
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
    public AddNewJobHandler(int taskID, String userID, ParameterInfo[] parameterInfoArray, int parentJobID) {
        this.taskID = taskID;
        this.userID = userID;
        this.parameterInfoArray = parameterInfoArray;
        this.parentJobID = parentJobID;
        hasParent = true;
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
            ParameterFormatConverter pfc = new ParameterFormatConverter();
            parameter_info = pfc.getJaxbString(parameterInfoArray);
            // Get EJB reference
            AnalysisDAO ds = new AnalysisDAO();
            // Invoke EJB function
            if (hasParent) {
                ji = ds.addNewJob(taskID, userID, parameter_info, parentJobID);
            }
            else {
                ji = ds.addNewJob(taskID, userID, parameter_info, -1);
            }
            // Checking for null
            if (ji == null) throw new OmnigeneException(
                    "AddNewJobRequest:executeRequest Operation failed, null value returned for JobInfo");

            synchronized (AnalysisTask.getJobQueueSynchro()) {
                // System.out.println("AddNewJobHandler: notifying ds about new
                // job to run");
                AnalysisTask.getJobQueueSynchro().notify();
            }
            // Reparse parameter_info before sending to client
            ji.setParameterInfoArray(pfc.getParameterInfoArray(parameter_info));
        }
        catch (TaskIDNotFoundException taskEx) {
            System.out.println("AddNewJob(executeRequest): TaskIDNotFoundException " + taskID);
            throw taskEx;
        }
        catch (Exception ex) {
            System.out.println("AddNewJob(executeRequest): Error " + ex.getMessage());
            ex.printStackTrace();
            throw new OmnigeneException(ex.getMessage());
        }

        return ji;
    }
}
