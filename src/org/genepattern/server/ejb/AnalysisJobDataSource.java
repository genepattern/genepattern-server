package org.genepattern.server.ejb;

import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.util.Vector;

import org.genepattern.webservice.AnalysisJob;
import org.genepattern.util.LSID;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.TaskInfo;

/**
 * AnalysisJobDataSource.java This interface used by AnalysisDAO
 * 
 * @author rajesh kuttan
 * @version
 */

public interface AnalysisJobDataSource {

	/**
	 * Used to get list of submitted job info
	 * 
	 * @param classname
	 * @param maxJobCount
	 *            max. job count
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return JobInfo Vector
	 */

	public Vector getWaitingJob(String classname, int maxJobCount)
			throws OmnigeneException, RemoteException;

	/**
	 * Get task id from task class name
	 * 
	 * @param className
	 *            task class name
	 * @throws OmnigeneException
	 *             Error in db
	 * @throws RemoteException
	 *             Error in service
	 * @return task id
	 */
	public int getTaskIDByName(String name, String userID)
			throws OmnigeneException, RemoteException;

	/**
	 * Submit a new job
	 * 
	 * @param taskID
	 * @param user_id
	 * @param parameter_info
	 * @param inputfile
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return Job ID
	 */
	public JobInfo addNewJob(int taskID, String user_id, String parameter_info,
			String inputfile) throws OmnigeneException, RemoteException;

	/**
	 * Update job information like status and resultfilename
	 * 
	 * @param jobNo
	 * @param jobStatusID
	 * @param outputFilename
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return record count of updated records
	 */
	public int updateJob(int jobNo, int jobStatusID, String outputFilename)
			throws OmnigeneException, RemoteException;

	/**
	 * Update job info with paramter infos and status
	 * 
	 * @param jobNo
	 * @param parameters
	 * @param jobStatusID
	 * @return number of record updated
	 * @throws OmnigeneException
	 * @throws RemoteException
	 */
	public int updateJob(int jobNo, String parameters, int jobStatusID)
			throws OmnigeneException, RemoteException;

	/**
	 * Fetches JobInformation
	 * 
	 * @param jobNo
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return <CODE>JobInfo</CODE>
	 */
	public JobInfo getJobInfo(int jobNo) throws OmnigeneException,
			RemoteException;

	/**
	 * Gets task from a task id
	 * 
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return <CODE>TaskInfo</CODE>
	 */
	public TaskInfo getTask(int taskID) throws OmnigeneException,
			RemoteException;

	/**
	 * Used to get all regular tasks
	 * 
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return Vector of <CODE>TaskInfo</CODE>
	 */
	public Vector getTasks() throws OmnigeneException, RemoteException;

	/**
	 * Used to get all available tasks
	 * 
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return Vector of <CODE>TaskInfo</CODE>
	 */
	public Vector getAllTypeTasks() throws OmnigeneException, RemoteException;

	/**
	 * Used to get all regular public tasks and userid specific task
	 * 
	 * @param user_id
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return Vector of <CODE>TaskInfo</CODE>
	 */
	public Vector getTasks(String user_id) throws OmnigeneException,
			RemoteException;

	/**
	 * Fetches list of JobInfo based on completion date on or before a specified
	 * date
	 * 
	 * @param date
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return <CODE>JobInfo[]</CODE>
	 */
	public JobInfo[] getJobInfo(java.util.Date d) throws OmnigeneException,
			RemoteException;

	/**
	 * Deletes a JobInfo record and all related input and output files for the
	 * job, based on the job number
	 * 
	 * @param jobNo
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return <CODE>JobInfo[]</CODE>
	 */
	public void deleteJob(int jobNo) throws OmnigeneException, RemoteException;

	/**
	 * Used to get all public tasks and userid specific task
	 * 
	 * @param user_id
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return Vector of <CODE>TaskInfo</CODE>
	 */
	public Vector getAllTypeTasks(String user_id) throws OmnigeneException,
			RemoteException;

	/**
	 * To create a new regular task
	 * 
	 * @param taskName
	 * @param user_id
	 * @param access_id
	 * @param description
	 * @param parameter_info
	 * @param className
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return task ID
	 */
	public int addNewTask(String taskName, String user_id, int access_id,
			String description, String parameter_info, String className,
			String taskInfoAttributes) throws OmnigeneException,
			RemoteException;

	/**
	 * Updates task description and parameters
	 * 
	 * @param taskID
	 *            task ID
	 * @param description
	 *            task description
	 * @param parameter_info
	 *            parameters as a xml string
	 * @return No. of updated records
	 * @throws OmnigeneException
	 * @throws RemoteException
	 */
	public int updateTask(int taskID, String taskDescription,
			String parameter_info, String taskInfoAttributes, String user_id,
			int access_id) throws OmnigeneException, RemoteException;

	/**
	 * Updates task parameters
	 * 
	 * @param taskID
	 *            task ID
	 * @param parameter_info
	 *            parameters as a xml string
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return No. of updated records
	 */
	public int updateTask(int taskID, String parameter_info,
			String taskInfoAttributes, String user_id, int access_id)
			throws OmnigeneException, RemoteException;

	/**
	 * Updates user_id and access_id
	 * 
	 * @param taskID
	 *            task ID
	 * @param user_id
	 * @param access_id
	 * @return No. of updated records
	 * @throws OmnigeneException
	 * @throws RemoteException
	 */
	public int updateTask(int taskID, String user_id, int access_id)
			throws OmnigeneException, RemoteException;

	/**
	 * To remove registered task based on task ID
	 * 
	 * @param taskID
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return No. of updated records
	 */
	public int deleteTask(int taskID) throws OmnigeneException, RemoteException;

	/**
	 * Fetches list of JobInformation
	 * 
	 * @param user_id
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return <CODE>JobInfo[]</CODE>
	 */
	public JobInfo[] getJobInfo(String user_id) throws OmnigeneException,
			RemoteException;

	/**
	 * Gets the analysis jobs for the given user. The returned
	 * <tt>AnalysisJob</tt> instance will have its server attribute set to
	 * <tt>null</tt>.
	 * 
	 * @param username
	 *            the username
	 */
	public AnalysisJob[] getJobs(String username) throws OmnigeneException,
			RemoteException;

	/**
	 * Starts a running thread of a new task, this method could be called after
	 * creating a new task
	 * 
	 * @param id
	 *            taskID
	 * @throws OmnigeneException
	 */
	public void startNewTask(int id) throws OmnigeneException, RemoteException;

	/**
	 * Stops the running thread of a task
	 * 
	 * @param classname
	 *            analysis task name
	 */
	public void stopTask(String classname) throws RemoteException;

	/**
	 * Stops the running thread of a task
	 * 
	 * @param taskID
	 *            analysis task ID
	 */
	public void stopTask(int taskID) throws RemoteException;

	public boolean resetPreviouslyRunningJobs() throws OmnigeneException,
			RemoteException;

	/**
	 * execute arbitrary SQL on database, returning ResultSet
	 * 
	 * @param sql
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return ResultSet
	 */
	public ResultSet executeSQL(String sql) throws OmnigeneException,
			RemoteException;

	/**
	 * execute arbitrary SQL on database, returning number of rows affected
	 * 
	 * @param sql
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return int number of rows returned
	 */
	public int executeUpdate(String sql) throws OmnigeneException,
			RemoteException;

	/**
	 * get the next available LSID identifer from the database
	 * 
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return int next identifier in sequence
	 */
	public int getNextLSIDIdentifier() throws OmnigeneException,
			RemoteException;

	/**
	 * get the next available LSID version for a given lsid from the database
	 * 
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return String next version in sequence
	 */
	public String getNextLSIDVersion(LSID lsid) throws OmnigeneException,
			RemoteException;

}

