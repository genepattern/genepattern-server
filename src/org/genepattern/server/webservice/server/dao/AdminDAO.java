package org.genepattern.server.webservice.server.dao;

import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.SuiteInfo;

/**
 * Interface for adminstrative tasks. 
 * 
 * @author Joshua Gould
 */
public interface AdminDAO {
	/**
	 * Gets the task id with the given lsid or task name. If
	 * <code>lsidOrTaskName</code> is a valid LSID with a version, then it is
	 * unambiguous which task to retrieve. If <code>lsidOrTaskName</code> is a
	 * valid LSID with no version, the latest version for the task is retrieved.
	 * If <code>lsidOrTaskName</code> is a task name, the latest version of
	 * the task with the nearest authority is selected. The nearest authority is
	 * the first match in the sequence: local authority,ÊBroad authority, other
	 * authority.
	 * 
	 * @param username
	 *            The username. If <code>null</code> then privacy is not
	 *            enforced if <code>lsidOrTaskName</code> is a valid LSID.
	 *            This is necessary because GenePatternAnalysisTask passes
	 *            <code>null</code> for username in some cases, such as
	 *            deleteTask. FIXME
	 * @param lsidOrTaskName
	 *            A task name or LSID
	 * @return The task id or -1 if not found
	 * @exception AdminDAOSysException
	 *                If an error occurs
	 */
	public int getTaskId(String lsidOrTaskName, String username)
			throws AdminDAOSysException;

	/**
	 * Gets the task with the given lsid or task name. If
	 * <code>lsidOrTaskName</code> is a valid LSID with a version, then it is
	 * unambiguous which task to retrieve. If <code>lsidOrTaskName</code> is a
	 * valid LSID with no version, the latest version for the task is retrieved.
	 * If <code>lsidOrTaskName</code> is a task name, the latest version of
	 * the task with the nearest authority is selected. The nearest authority is
	 * the first match in the sequence: local authority,ÊBroad authority, other
	 * authority.
	 * 
	 * @param username
	 *            The username
	 * @param lsidOrTaskName
	 *            A task name or LSID
	 * @return The task or <code>null</code> if not found
	 * @exception AdminDAOSysException
	 *                If an error occurs
	 */
	public TaskInfo getTask(String lsidOrTaskName, String username)
			throws AdminDAOSysException;

	/**
	 * Gets all visible tasks for the given user. Tasks are ordered by
	 * lower(task_name), lsid_no_version, lsid version descending.
	 * 
	 * @param username
	 *            The username
	 * @return all tasks
	 * @exception AdminDAOSysException
	 *                If an error occurs
	 */
	public TaskInfo[] getAllTasks(String username) throws AdminDAOSysException;

	/**
	 * Used to get all tasks in the database, ignoring ownership and privacy.
	 * Tasks are ordered by lower(task_name), lsid_no_version, lsid version
	 * descending.
	 * 
	 * @return the tasks
	 * @exception AdminDAOSysException
	 *                If an error occurs
	 */
	public TaskInfo[] getAllTasks() throws AdminDAOSysException;

	/**
	 * Gets the latest tasks for the given user. This differs from
	 * getLatestTasksByName in that the returned array can contain more than
	 * one task with the same name if those tasks are from different
	 * authorities.
	 * 
	 * @param username
	 *            The username
	 * @return The latest tasks
	 * @exception AdminDAOSysException
	 *                If an error occurs
	 */
	public TaskInfo[] getLatestTasks(String username)
			throws AdminDAOSysException;

	/**
	 * Gets the latest tasks for each task name. For example, if there are
	 * multiple tasks named PreprocessDataset from different authorities, only
	 * the latest version from the nearest authority for PreprocessDataset is
	 * returned.
	 * 
	 * @param username
	 *            The username
	 * @return The latest tasks by name
	 * @exception AdminDAOSysException
	 *                If an error occurs
	 */
	public TaskInfo[] getLatestTasksByName(String username)
			throws AdminDAOSysException;
	

	/**
	 * Gets task from a task id
	 * 
	 * @param taskID
	 *            a task id
	 * @return a <code>TaskInfo</code> instance
	 * @exception AdminDAOSysException
	 *                If an error occurs or if the given taskId is not found.
	 */
	public TaskInfo getTask(int taskId) throws AdminDAOSysException;

	public SuiteInfo getSuite(String taskId) throws AdminDAOSysException;



	/**
	 * Gets the latest versions of all suites
	 * 
	 * @return The latest suites
	 * @exception WebServiceException
	 *                If an error occurs
	 */
	public SuiteInfo[] getLatestSuites() throws AdminDAOSysException;
	public SuiteInfo[] getLatestSuites(String userName) throws AdminDAOSysException;


	/**
	 * Gets all versions of all suites
	 * 
	 * @return The suites
	 * @exception WebServiceException
	 *                If an error occurs
	 */
	public SuiteInfo[] getAllSuites() throws AdminDAOSysException;
	public SuiteInfo[] getAllSuites(String userName) throws AdminDAOSysException;


	/**
	 * Gets all suites this task is a part of
	 * 
	 * @return The suites
	 * @exception WebServiceException
	 *                If an error occurs
	 */
	public SuiteInfo[] getSuiteMembership(String taskLsid) throws AdminDAOSysException;


}