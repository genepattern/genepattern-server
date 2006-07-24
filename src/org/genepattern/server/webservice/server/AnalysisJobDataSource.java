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


package org.genepattern.server.webservice.server;

import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import org.genepattern.webservice.AnalysisJob;
import org.genepattern.util.LSID;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.TaskInfo;

/**
 * Interface for analysis web service tasks. 
 * 
 * @deprecated
 * 
 * @author rajesh kuttan
 */

public interface AnalysisJobDataSource {

	/**
	 * Used to get list of submitted job info
	 * 
	 * @param maxJobCount
	 *            max. job count
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return JobInfo Vector
	 */

	public Vector getWaitingJob(int maxJobCount)
			throws OmnigeneException, RemoteException;


   /**
	 * Saves a record of a job that was executed on the client into the database
	 * 
	 * @param taskID
	 * @param user_id
	 * @param parameter_info
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return Job ID
	 */
	public JobInfo recordClientJob(int taskID, String user_id, String parameter_info) throws OmnigeneException, RemoteException;
   
	/**
	 * Saves a record of a job that was executed on the client into the database
	 * 
	 * @param taskID
	 * @param user_id
	 * @param parameter_info
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return Job ID
	 */
	public JobInfo recordClientJob(int taskID, String user_id, String parameter_info, int parentJobNumber) throws OmnigeneException, RemoteException;
   
   


   /**
   * Gets the task name of a temporary (unsaved) pipeline
   * @throws OmnigeneException
   *             if thrown by Omnigene
   * @throws RemoteException
   *             if thrown by Omnigene
   * @return the temporary pipeline name
   */
   public String getTemporaryPipelineName(int jobNumber) throws OmnigeneException, RemoteException;
   
   /**
   * Gets the child jobs for the given job number
   * @param jobId the parent job id
   * @throws OmnigeneException
   *             if thrown by Omnigene
   * @throws RemoteException
   *             if thrown by Omnigene
   * @return an array of children <tt>JobInfo</tt> objects 
   */
   public JobInfo[] getChildren(int jobId) throws OmnigeneException, RemoteException;
   
   /**
   * Gets the parent job or <tt>null</tt> for the given job number
   * @param jobId the job id
   * @throws OmnigeneException
   *             if thrown by Omnigene
   * @throws RemoteException
   *             if thrown by Omnigene
   * @return the parent <tt>JobInfo</tt> object or <tt>null</tt> if no parent exists
   */
   public JobInfo getParent(int jobId) throws OmnigeneException, RemoteException;
   
	/**
	 * Update job information like status and resultfilename
	 * 
	 * @param jobNo
	 * @param jobStatusID
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return record count of updated records
	 */
	public int updateJobStatus(int jobNo, int jobStatusID)
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
	 * @throws SQLException 
	 * @throws OmnigeneException 
	 */
	public JobInfo getJobInfo(int jobNo) throws  OmnigeneException;



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
	 * To create a new regular task
	 * 
	 * @param taskName
	 * @param user_id
	 * @param access_id
	 * @param description
	 * @param parameter_info
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return task ID
	 */
	public int addNewTask(String taskName, String user_id, int access_id,
			String description, String parameter_info,
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
	 * 
	 * Gets the jobs for the given user
    * @param username the username to retrieve jobs for
    * @param startIndex the index, beginning at 0, to start retrieving jobs from
    * @param maxEntries the maximum number of jobs to return
    * @param allJobs if <tt>true</tt> return all jobs that the given user has run, otherwise return jobs that have not been deleted 
	 * 
	 * @return the jobs
	 */
	public JobInfo[] getJobs(String username, int startIndex, int maxEntries, boolean allJobs) throws OmnigeneException, RemoteException;

   /**
   * Sets the deleted flag for the given job
   * @param jobNumber the job number
   * @param deleted the deleted flag
   */
   public void setJobDeleted(int jobNumber, boolean deleted) throws OmnigeneException, RemoteException;
   

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
	public int getNextLSIDIdentifier(String namespace) throws OmnigeneException,
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

