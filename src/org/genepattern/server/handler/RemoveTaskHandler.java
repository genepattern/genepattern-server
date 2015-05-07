/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.server.handler;

import org.apache.log4j.Logger;
import org.genepattern.server.TaskIDNotFoundException;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.webservice.OmnigeneException;

/**
 * Class used to Remove existing task
 * 
 * @author rajesh kuttan
 * @version 1.0
 */
public class RemoveTaskHandler extends RequestHandler {
    private static Logger log = Logger.getLogger(RemoveTaskHandler.class);

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
     * @return No.of records deleted
     */
	public int executeRequest() throws OmnigeneException, TaskIDNotFoundException {
	    int recordDeleted = 0;
	    try {
	        AdminDAO ds = new AdminDAO();
	        recordDeleted = ds.deleteTask(taskID);
	    } 
	    catch (TaskIDNotFoundException taskEx) {
	        log.error("RemoveTaskRequest(executeRequest): TaskIDNotFoundException " + taskID);
	        throw taskEx;
	    } 
	    catch (Exception ex) {
	        log.error("RemoveTaskRequest(execute): Error " + ex.getMessage(), ex);
	        throw new OmnigeneException(ex.getMessage());
	    }
	    return recordDeleted;
	}

}
