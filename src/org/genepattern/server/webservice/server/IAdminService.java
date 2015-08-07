/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webservice.server;

import java.util.Map;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * @author Joshua Gould
 */
public interface IAdminService {
    /**
     * Gets a map that contains information about this service.
     * 
     * @return A map with information about this service
     * @exception WebServiceException
     *                If an error occurs
     */
    public Map getServiceInfo() throws WebServiceException;

    /**
     * Gets the task with the given lsid or task name. If <code>lsidOrTaskName</code> is a valid LSID with a version,
     * then it is unambiguous which task to retrieve. If <code>lsidOrTaskName</code> is a valid LSID with no version,
     * the latest version for the task is retrieved. If <code>lsidOrTaskName</code> is a task name, the latest version
     * of the task with the nearest authority is selected. The nearest authority is the first match in the sequence:
     * local authority,Broad authority, other authority.
     * 
     * @param username
     *            The username
     * @param lsidOrTaskName
     *            Description of the Parameter
     * @return The task or <code>null</code> if not found
     * @exception WebServiceException
     *                If an error occurs
     */
    public TaskInfo getTask(String lsid) throws WebServiceException;

    /**
     * Return the latest version of each task accessible to the user sorted by task name. Tasks are groupd by LSID minus
     * the version, so tasks with the same nambe but different authority are treated as distinct by this method. To
     * group tasks by name use getLatestTasksByName().
     * 
     * @return The latest tasks
     * @exception WebServiceException
     *                If an error occurs
     */
    public TaskInfo[] getLatestTasks() throws WebServiceException;

    /**
     * Gets all versions of all tasks that are either public, or owned by the current user.
     * 
     * @return The tasks
     * @exception WebServiceException
     *                If an error occurs
     */
    public TaskInfo[] getAllTasks() throws WebServiceException;

    // XX

    /**
     * Gets the suite with the given lsid or task name. If <code>lsidOrTaskName</code> is a valid LSID with a version,
     * then it is unambiguous which task to retrieve. If <code>lsidOrTaskName</code> is a valid LSID with no version,
     * the latest version for the suite is retrieved. If <code>lsidOrTaskName</code> is a suite name, the latest
     * version of the task with the nearest authority is selected. The nearest authority is the first match in the
     * sequence: local authority, Broad authority, other authority.
     * 
     * @param username
     *            The username
     * @param lsidOrSuiteName
     *            Description of the Parameter
     * @return The suite or <code>null</code> if not found
     * @exception WebServiceException
     *                If an error occurs
     */
    public SuiteInfo getSuite(String lsid) throws WebServiceException;

    /**
     * Gets the latest versions of all suites
     * 
     * @return The latest suites
     * @exception WebServiceException
     *                If an error occurs
     */
    public SuiteInfo[] getLatestSuites() throws WebServiceException;

    /**
     * Gets all versions of all suites that are either owned by the current user, or are public.
     * 
     * @return The suites
     * @exception WebServiceException
     *                If an error occurs
     */
    public SuiteInfo[] getAllSuites() throws WebServiceException;

    /**
     * Gets all suites this task is a part of
     * 
     * @return The suites
     * @exception WebServiceException
     *                If an error occurs
     */
    public SuiteInfo[] getSuiteMembership(String taskLsid) throws WebServiceException;

    // XX

    /**
     * Gets the server log
     * 
     * @return The server log
     * @exception WebServiceException
     *                If an error occurs
     */
    public javax.activation.DataHandler getServerLog() throws WebServiceException;

    /**
     * Gets the GenePattern log
     * 
     * @return The GenePattern log
     * @exception WebServiceException
     *                If an error occurs
     */
    public javax.activation.DataHandler getGenePatternLog() throws WebServiceException;

    /**
     * Gets a vector of job results
     * 
     * @param returnAll
     *            Whether to return all job results or only the current user's results
     * @return The job results
     * @exception WebServiceException
     *                If an error occurs
     */
    // public java.util.Vector getJobResults(boolean returnAll)
    // throws WebServiceException;
    /**
     * Gets a map that maps the LSID without the version to a list of versions for that LSID
     * 
     * @return LSID to versions map
     * @exception WebServiceException
     *                If an error occurs
     */
    public Map getLSIDToVersionsMap() throws WebServiceException;
}
