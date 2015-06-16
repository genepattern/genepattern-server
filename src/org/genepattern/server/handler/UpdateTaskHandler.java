/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.handler;

import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.ParameterInfo;

/**
 * <p>
 * Title: UpdateTaskHandler.java
 * </p>
 * <p>
 * Description: Updates the parameters of an existing task
 * </p>
 * 
 * @author Hui Gong
 * @version 1.0
 */

public class UpdateTaskHandler extends RequestHandler {
    private int _taskID;

    private ParameterInfo[] _parameterInfoArray;

    private String _taskInfoAttributeString;

    private String _user_id;

    private int _access_id;

    public UpdateTaskHandler() {
        super();
    }

    /**
     * Constructor with parameters
     * 
     * @param taskName
     * @param parameterInfoArray
     */
    public UpdateTaskHandler(int taskID, ParameterInfo[] parameterInfoArray, String taskInfoAttributeString,
            String user_id, int access_id) {
        this._taskID = taskID;
        this._parameterInfoArray = parameterInfoArray;
        this._taskInfoAttributeString = taskInfoAttributeString;
        this._user_id = user_id;
        this._access_id = access_id;
    }

    /**
     * Update the task parameters
     * 
     * @return int number of record updated, 0 is no record updated
     * @throws OmnigeneException
     */
    public int executeRequest() throws OmnigeneException {
        int updatedRecord = 0;
        try {
            String parameter_info = ParameterFormatConverter.getJaxbString(_parameterInfoArray);
            AnalysisDAO ds = new AnalysisDAO();
            updatedRecord = ds.updateTask(_taskID, parameter_info, _taskInfoAttributeString, _user_id, _access_id);
        }
        catch (Exception ex) {
            System.out.println("UpdateTaskRequest: Error " + ex.getMessage());
            ex.printStackTrace();
            throw new OmnigeneException(ex.getMessage());
        }
        return updatedRecord;
    }

}
