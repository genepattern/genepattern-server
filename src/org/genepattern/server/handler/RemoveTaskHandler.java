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

import org.genepattern.server.TaskIDNotFoundException;
import org.genepattern.server.webservice.server.AnalysisJobDataSource;
import org.genepattern.server.webservice.server.dao.AdminDataService;
import org.genepattern.server.webservice.server.dao.AnalysisDataService;
import org.genepattern.server.util.BeanReference;
import org.genepattern.webservice.OmnigeneException;

// import edu.mit.wi.omnigene.omnidas.*;

/**
 * Class used to Remove existing task
 * 
 * @author rajesh kuttan
 * @version 1.0
 */

public class RemoveTaskHandler extends RequestHandler {

    private int taskID = 0;

    public RemoveTaskHandler() {
    }

    /**
     * Constructor accepts taskID
     * 
     * @param taskID
     */
    public RemoveTaskHandler(int taskID) {
        this.taskID = taskID;
    }

    /**
     * Removes task based on taskID and returns no. of deleted records
     * 
     * @throws TaskIDNotFoundException
     * @throws OmnigeneException
     *             OmnigeneException
     * @return No.of records deleted
     */
    public int executeRequest() throws OmnigeneException, TaskIDNotFoundException {
        int recordDeleted = 0;
        try {

            recordDeleted = AdminDataService.getInstance().deleteTask(taskID);
        }
        catch (TaskIDNotFoundException taskEx) {
            System.out.println("RemoveTaskRequest(executeRequest): TaskIDNotFoundException " + taskID);
            throw taskEx;
        }
        catch (Exception ex) {
            System.out.println("RemoveTaskRequest(execute): Error " + ex.getMessage());
            ex.printStackTrace();
            throw new OmnigeneException(ex.getMessage());
        }

        return recordDeleted;
    }

}