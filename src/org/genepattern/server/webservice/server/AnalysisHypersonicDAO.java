package org.genepattern.server.webservice.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.genepattern.server.AnalysisManager;
import org.genepattern.server.JobIDNotFoundException;
import org.genepattern.server.TaskIDNotFoundException;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.dao.AdminDAOSysException;
import org.genepattern.server.webservice.server.dao.AdminHSQLDAO;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.JobStatus;

/**
 * AnalysisHypersonicDAO.java
 * 
 * @author rajesh kuttan, Hui Gong
 * @version
 */

public class AnalysisHypersonicDAO implements 
		AnalysisJobDataSource {

	public static int JOB_WAITING_STATUS = 1;

	public int PROCESSING_STATUS = 2;

	private static Logger logger = Logger.getRootLogger();

	public static final int UNPROCESSABLE_TASKID = -1; // taskID for tasks which
													   // aren't runnable

	private static int PUBLIC_ACCESS_ID = 1;

	private AdminDAO adminDAO = new AdminHSQLDAO();

	/** Creates new AnalysisHypersonicAccess */
	public AnalysisHypersonicDAO() {
		logger.setLevel((Level) Level.FATAL);
	}

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
			throws OmnigeneException, RemoteException {
		Vector jobVector = new Vector();
		java.sql.Connection conn = null;
		PreparedStatement stat = null;
		ResultSet resultSet = null;

		//initializing maxJobCount, if it has invalid value
		if (maxJobCount <= 0) {
			maxJobCount = 1;
		}

		try {
			//getConnection
			conn = getConnection();

			//Validating taskID is not done here bcos.
			//assuming once job is submitted, it should be executed even if
			// taskid is removed from task master

			//Query job table for waiting job
			stat = conn
					.prepareStatement("SELECT job_no,analysis_job.task_id,analysis_job.parameter_info,analysis_job.user_id, analysis_job.task_lsid, analysis_job.task_name FROM analysis_job, task_master where analysis_job.task_id=task_master.task_id and  status_id = ? order by date_submitted");
			stat.setInt(1, JOB_WAITING_STATUS);
			resultSet = stat.executeQuery();

			int jobNo = 0, taskID = 0;
			String parameter_info = "";
			String lsid = null;
			boolean recordFoundFlag = false;

			ParameterFormatConverter parameterFormatConverter = new ParameterFormatConverter();
			int i = 1;
			// Moves to the next record until no more records
			while (resultSet.next() && i++ <= maxJobCount) {
				recordFoundFlag = true;
				jobNo = resultSet.getInt(1);
				taskID = resultSet.getInt(2);
				parameter_info = resultSet.getString(3);
				lsid = resultSet.getString("task_lsid");
            String taskName = resultSet.getString("task_name");

				updateJob(jobNo, PROCESSING_STATUS);

				//Add waiting job info to vector, for AnalysisTask
				ParameterInfo[] params = parameterFormatConverter
						.getParameterInfoArray(parameter_info);
				JobInfo singleJobInfo = new JobInfo(jobNo, taskID, null, null, null, params,
						resultSet.getString(4), lsid, taskName);
				jobVector.add(singleJobInfo);
           
				//break; // JL: only one job at a time, so that other threads
				// can compete for same classname jobs
			}

		} catch (Exception e) {
			logger.error("AnalysisHypersonicDAO: getWaitingJob failed", e);
			throw new OmnigeneException(e.getMessage());
		}

		finally {
			closeConnection(resultSet, stat, conn);
		}

		return jobVector;

	}
   
   public String getTemporaryPipelineName(int jobNumber) throws OmnigeneException, RemoteException {
      java.sql.Connection conn = null;
		PreparedStatement stat = null;
		ResultSet resultSet = null;
		try {
			conn = getConnection();
         stat = conn
               .prepareStatement("SELECT task_name FROM analysis_job WHERE job_no=" + jobNumber);
         
         resultSet = stat.executeQuery();
         if (!resultSet.next()) {
            throw new OmnigeneException(
                  "AnalysisHypersonicDAO:getTemporaryPipelineName " + jobNumber
                        + " not found");
         }
         return resultSet.getString(1);
      } catch(SQLException sqle) {
         throw new OmnigeneException(sqle.getMessage()); 
      } finally {
			closeConnection(resultSet, stat, conn);
		}
   }

   public JobInfo createPipeline(int pipelineTaskId, String user_id, String parameter_info) throws OmnigeneException, RemoteException {
      return addNewJob(pipelineTaskId, user_id, parameter_info, null, null, null);
   }
   
   
   public JobInfo createTemporaryPipeline(String user_id, String parameter_info, String pipelineName, String lsid) throws OmnigeneException, RemoteException {
      return addNewJob(UNPROCESSABLE_TASKID, user_id, parameter_info, pipelineName, null, lsid);
   }
   
   
   public JobInfo recordClientJob(int taskID, String user_id, String parameter_info) throws OmnigeneException, RemoteException {
       return  recordClientJob( taskID,  user_id,  parameter_info, -1);
   }
       
   public JobInfo recordClientJob(int taskID, String user_id, String parameter_info, int parentJobNumber) throws OmnigeneException, RemoteException {
      JobInfo job = null;
      try {
          
         Integer parent = null;
         if(parentJobNumber!=-1) {
             parent = new Integer(parentJobNumber);
         }
         job = addNewJob(taskID, user_id, parameter_info, null, parent, null);
         updateJob(job.getJobNumber(), JobStatus.JOB_FINISHED);
         setJobDeleted(job.getJobNumber(), true);
         return job;
      } catch(OmnigeneException e) {
         if(job!=null) {
            deleteJob(job.getJobNumber()); 
         }
         throw e;
      } catch(RemoteException re) {
         if(job!=null) {
            deleteJob(job.getJobNumber()); 
         }
         throw re;
      }
   }
   
 
   public JobInfo addNewJob(int taskID, String user_id, String parameter_info) throws OmnigeneException, RemoteException {
      return addNewJob(taskID, user_id, parameter_info, null, null, null);
   }
   
   public JobInfo addNewJob(int taskID, String user_id, String parameter_info, int parentJobNumber) throws OmnigeneException, RemoteException {
      return addNewJob(taskID, user_id, parameter_info, null, new Integer(parentJobNumber), null);
   }
   
	/**
	 * Submit a new job
	 * 
	 * @param taskID the task id or UNPROCESSABLE_TASKID if the task is a temporary pipeline
	 * @param user_id the user id
	 * @param parameter_info the parameter info
    * @param taskName the task name if the task is a temporary pipeline
    * @param parentJobNumber the parent job number of <tt>null</tt> if the job has no parent
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return Job ID
	 */
	private JobInfo addNewJob(int taskID, String user_id, String parameter_info, String taskName, Integer parentJobNumber, String task_lsid) throws OmnigeneException, RemoteException {
		int updatedRecord = 0;
		JobInfo jobInfo = null;

		java.sql.Connection conn = null;
		PreparedStatement stat = null;
		ResultSet resultSet = null;
		int jobNo = 0;
		try {
			conn = getConnection();
			String lsid = null;
			//Check taskID is valid
			if (taskID != UNPROCESSABLE_TASKID) {
				stat = conn
						.prepareStatement("SELECT task_Name, lsid FROM task_master WHERE task_id = ? ");
				stat.setInt(1, taskID);
				resultSet = stat.executeQuery();

				if (!resultSet.next()) {
					throw new TaskIDNotFoundException(
							"AnalysisHypersonicDAO:addNewJob TaskID " + taskID
									+ " not a valid TaskID ");
				}
				taskName = resultSet.getString(1);
				lsid = resultSet.getString(2);
			} else {
				if (task_lsid != null) lsid = task_lsid;
			}
			//Store submitted job
			stat = conn
					.prepareStatement("INSERT INTO analysis_job(task_id,status_id, "
							+ "date_submitted, parameter_info,user_id, task_name, task_lsid, parent)  VALUES (? , ?, current_timestamp,?,?, ?, ?, ?)");

		
			stat.setInt(1, taskID);
			stat.setInt(2, JOB_WAITING_STATUS);
			stat.setString(3, parameter_info);
			stat.setString(4, user_id);
			stat.setString(5, taskName);
			stat.setString(6, lsid);
         stat.setObject(7, parentJobNumber);

			updatedRecord = stat.executeUpdate();

			//Get new job no.
			stat = conn.prepareStatement("CALL IDENTITY()");
			resultSet = stat.executeQuery();

			if (resultSet.next()) {
				jobNo = resultSet.getInt(1);
				//		LSID lsid = new
				// LSID(LSIDManager.getInstance().getAuthority(),
				// "genepatternjobs", ""+jobNo, "1");
				//		stat = conn.prepareStatement("update analysis_job set lsid='"
				// + lsid.toString() + "' where jobNo=" + jobNo);
				//		updatedRecord=stat.executeUpdate() ;
			} else {
				logger
						.error("AnalysisHypersonicDAO:addNewJob Call to job_sequence failed. Could not generate job no");
				throw new Exception(
						"AnalysisHypersonicDAO:addNewJob Call to job_sequence failed. Could not generate job no");
			}

			jobInfo = getJobInfo(jobNo);

		} catch (TaskIDNotFoundException e) {
			logger
					.error("AnalysisHypersonicDAO:addNewJob JobIDNotFoundException "
							+ e);
			throw e;
		} catch (Exception e) {
			logger.error("AnalysisHypersonicDAO:addNewJob failed " + e);
			throw new OmnigeneException(e.getMessage());
		} finally {
			closeConnection(resultSet, stat, conn);
		}
		return jobInfo;
	}

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
	public int updateJob(int jobNo, int jobStatusID)
			throws OmnigeneException, RemoteException {
		int updateRecord = 0;
		java.sql.Connection conn = null;
		PreparedStatement stat = null;
		try {
			conn = getConnection();
			stat = conn
					.prepareStatement("UPDATE analysis_job SET status_id = ?, date_completed = current_timestamp WHERE job_no = ?");
			stat.setInt(1, jobStatusID);
			stat.setInt(2, jobNo);
			updateRecord = stat.executeUpdate();
		} catch (Exception e) {
			logger.error("AnalysisHypersonicDAO:updateJob failed " + e);
			throw new OmnigeneException(e.getMessage());
		} finally {
			closeConnection(null, stat, conn);
		}
		return updateRecord;
	}

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
			throws OmnigeneException, RemoteException {
		int updateRecord = 0;
		java.sql.Connection conn = null;
		PreparedStatement stat = null;
		try {
			conn = getConnection();
			stat = conn
					.prepareStatement("UPDATE analysis_job SET status_id = ?, parameter_info = ?, date_completed = current_timestamp WHERE job_no = ?");
			stat.setInt(1, jobStatusID);
			stat.setString(2, parameters);
			stat.setInt(3, jobNo);
			updateRecord = stat.executeUpdate();

		} catch (Exception e) {
			logger.error("AnalysisHypersonicDAO:updateJob failed " + e);
			throw new OmnigeneException(e.getMessage());
		} finally {
			closeConnection(null, stat, conn);
		}
		return updateRecord;
	}

	/**
	 * Fetches JobInformation
	 * 
	 * @param jobNo
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return <CODE>JobInfo</CODE>
	 */
	public JobInfo getJobInfo(int jobNo) throws OmnigeneException,
			RemoteException {
		JobInfo ji = null;
		java.sql.Connection conn = null;
		PreparedStatement stat = null;
		ResultSet resultSet = null;
		try {
			conn = getConnection();

			//Fetch from database
			stat = conn
					.prepareStatement(getJobInfoSelectClause()
							+ " FROM analysis_job, job_status WHERE job_no = ? and analysis_job.status_id = job_status.status_id");
			stat.setInt(1, jobNo);
			resultSet = stat.executeQuery();
			boolean recordFound = false;
			while (resultSet.next()) {
				recordFound = true;
				ji = jobInfoFromResultSet(resultSet);
				logger.debug("submit: " + ji.getDateSubmitted() + "complete: "
						+ ji.getDateCompleted());
			}

			//If jobNo not found
			if (!recordFound)
				throw new JobIDNotFoundException(
						"AnalysisHypersonicDAO:getJobInfo JobID " + jobNo
								+ " not found");

		} catch (SQLException e) {
			logger.error("AnalysisHypersonicDAO:getJobInfo failed " + e);
			throw new OmnigeneException(e.getMessage());
		} finally {
			closeConnection(resultSet, stat, conn);
		}

		return ji;
	}

	/**
	 * Fetches list of JobInformation
	 * 
	 * @param user_id
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return <CODE>JobInfo[]</CODE>
	 */
	public JobInfo[] getJobInfo(String user_id) throws OmnigeneException,
			RemoteException {
		JobInfo ji = null;
		java.sql.Connection conn = null;
		PreparedStatement stat = null;
		ResultSet resultSet = null;
		Vector jobVector = new Vector();
		try {
			conn = getConnection();

			//Fetch from database
			stat = conn
					.prepareStatement(getJobInfoSelectClause()
							+ " FROM analysis_job , job_status WHERE user_id = ? and analysis_job.status_id = job_status.status_id");
			stat.setString(1, user_id);

			resultSet = stat.executeQuery();
			boolean recordFound = false;
			while (resultSet.next()) {
				recordFound = true;
				ji = jobInfoFromResultSet(resultSet);
				jobVector.add(ji);
				logger.debug("submit: " + ji.getDateSubmitted() + "complete: "
						+ ji.getDateCompleted());
			}

		} catch (Exception e) {
			logger.error("AnalysisHypersonicDAO:getJobInfo failed " + e);
			throw new OmnigeneException(e.getMessage());
		} finally {
			closeConnection(resultSet, stat, conn);
		}

		return (JobInfo[]) jobVector.toArray(new JobInfo[] {});
	}

    public JobInfo getParent(int jobId) throws OmnigeneException, RemoteException {
      java.sql.Connection conn = null;
		Statement stat = null;
		ResultSet resultSet = null;
      
		try {
			conn = getConnection();
         String sql = "SELECT parent_job.job_no,parent_job.task_id, status_name, parent_job.date_submitted, parent_job.date_completed, parent_job.parameter_info, parent_job.user_id, parent_job.task_lsid, parent_job.task_name FROM analysis_job AS child_job, analysis_job AS parent_job, job_status WHERE child_job.job_no = " + jobId + " AND parent_job.job_no = child_job.parent AND parent_job.status_id = job_status.status_id";
			
			stat = conn.createStatement();
         resultSet = stat.executeQuery(sql);
         if (resultSet.next()) {
				return jobInfoFromResultSet(resultSet);
			}
         return null;
      } catch (Exception e) {
			logger.error("AnalysisHypersonicDAO:getChildren failed " + e);
			throw new OmnigeneException(e.getMessage());
		} finally {
			closeConnection(resultSet, stat, conn);
		}
      	     
   }
   
   
   public JobInfo[] getChildren(int jobId) throws OmnigeneException, RemoteException {
      java.sql.Connection conn = null;
		Statement stat = null;
		ResultSet resultSet = null;
      java.util.List results = new java.util.ArrayList();
		try {
			conn = getConnection();

			//Fetch from database
			stat = conn
					.createStatement();
               String sql = getJobInfoSelectClause() + " FROM analysis_job, job_status WHERE analysis_job.status_id = job_status.status_id AND parent = " + jobId;
         resultSet = stat.executeQuery(sql);
         while (resultSet.next()) {
				JobInfo ji = jobInfoFromResultSet(resultSet);
				results.add(ji);
			}
      } catch (Exception e) {
			logger.error("AnalysisHypersonicDAO:getChildren failed " + e);
			throw new OmnigeneException(e.getMessage());
		} finally {
			closeConnection(resultSet, stat, conn);
		}
      return (JobInfo[]) results.toArray(new JobInfo[0]);
			     
   }
   
   public void setJobDeleted(int jobNumber, boolean deleted) throws OmnigeneException {
      java.sql.Connection conn = null;
		Statement stat = null;
		try {
			conn = getConnection();
         String sql = "UPDATE analysis_job SET deleted = " + deleted + " WHERE job_no=" + jobNumber;
         stat = conn.createStatement();
         stat.executeUpdate(sql);
      } catch (Exception e) {
			logger.error("setJobDeleted failed " + e);
			throw new OmnigeneException(e.getMessage());
		} finally {
			closeConnection(null, stat, conn);
		}
   }

	public JobInfo[] getJobs(String username, int maxJobNumber, int maxEntries, boolean allJobs) throws OmnigeneException,
			RemoteException {
		java.util.List results = new java.util.ArrayList();

		java.sql.Connection conn = null;
		Statement stat = null;
		ResultSet resultSet = null;

		try {
			conn = getConnection();
         String sql = "SELECT LIMIT 0 " +  maxEntries + " job_no,task_id,status_name,date_submitted,date_completed,parameter_info,user_id, task_lsid, task_name FROM analysis_job, job_status WHERE analysis_job.status_id = job_status.status_id AND parent IS NULL";
         if(username != null) {
            sql += " AND user_id='" + username + "'";
         }
         if(maxJobNumber != -1) {
              sql += " AND job_no <= " + maxJobNumber;  
         }
         if(!allJobs) {
            sql += " AND deleted IS FALSE";
         }
         sql += " ORDER BY job_no DESC";
        
			stat = conn
					.createStatement();

			
			resultSet = stat.executeQuery(sql);

			while (resultSet.next()) {
				JobInfo ji = jobInfoFromResultSet(resultSet);
				results.add(ji);
			}

		} catch (Exception e) {
			logger.error("AnalysisHypersonicDAO:getJobs failed " + e);
			throw new OmnigeneException(e.getMessage());
		} finally {
			closeConnection(resultSet, stat, conn);
		}

		return (JobInfo[]) results.toArray(new JobInfo[] {});
	}

	/**
	 * Fetches list of JobInfo based on completion date on or before a specified
	 * date
	 * 
	 * @param date
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return <CODE>JobInfo[]</CODE>
	 */
	public JobInfo[] getJobInfo(java.util.Date date) throws OmnigeneException,
			RemoteException {
		JobInfo ji = null;
		java.sql.Connection conn = null;
		PreparedStatement stat = null;
		ResultSet resultSet = null;
		Vector jobVector = new Vector();
		try {
			conn = getConnection();

			//Fetch from database
			stat = conn
					.prepareStatement(getJobInfoSelectClause()
							+ " FROM analysis_job, job_status WHERE date_completed <= ? and analysis_job.status_id = job_status.status_id");
			stat.setTimestamp(1, new java.sql.Timestamp(date.getTime()));

			resultSet = stat.executeQuery();
			boolean recordFound = false;
			while (resultSet.next()) {
				recordFound = true;
				ji = jobInfoFromResultSet(resultSet);
				jobVector.add(ji);
				logger.debug("submit: " + ji.getDateSubmitted() + "complete: "
						+ ji.getDateCompleted());
			}

		} catch (Exception e) {
			logger.error("AnalysisHypersonicDAO:getJobInfo failed " + e);
			throw new OmnigeneException(e.getMessage());
		} finally {
			closeConnection(resultSet, stat, conn);
		}

		return (JobInfo[]) jobVector.toArray(new JobInfo[] {});
	}

   /** 
   * Gets the SELECT clause to use when retrieving a JobInfo obect with jobInfoFromResultSet
   * @see #jobInfoFromResultSet
   * @return the SELECT clause
   */
   private String getJobInfoSelectClause() {
      return "SELECT job_no,task_id,status_name,date_submitted,date_completed,parameter_info,user_id, task_lsid, task_name";   
   }
   
	protected JobInfo jobInfoFromResultSet(ResultSet resultSet)
			throws SQLException, OmnigeneException {
		ParameterFormatConverter parameterFormatConverter = new ParameterFormatConverter();

		JobInfo ji = new JobInfo(resultSet.getInt(1), resultSet.getInt(2),
				resultSet.getString(3), resultSet.getTimestamp(4), resultSet
						.getTimestamp(5), parameterFormatConverter
						.getParameterInfoArray(resultSet.getString(6)),
				resultSet.getString(7), resultSet.getString(8), resultSet.getString(9));
		return ji;
	}

	/**
	 * Removes a job and all it's input and output files based on jobID
	 * 
	 * @param taskID
	 * @throws OmnigeneException
	 * @throws RemoteException
	 */
	public void deleteJob(int jobID) throws OmnigeneException, RemoteException {
		java.sql.Connection conn = null;
		PreparedStatement stat = null;
		ResultSet resultSet = null;
		boolean DEBUG = false;
		JobInfo jobInfo = getJobInfo(jobID);

		ParameterInfo[] pia = jobInfo.getParameterInfoArray();
		if (pia != null) {
			for (int i = 0; i < pia.length; i++) {
				if (pia[i].isOutputFile() || pia[i].isInputFile()) {
					if (DEBUG)
						System.out.println("deleting " + pia[i].getValue());
					new File(pia[i].getValue()).delete();
				}
			}
		}

		try {

			conn = getConnection();
			//delete from task table
			stat = conn
					.prepareStatement("DELETE FROM analysis_job WHERE job_no = ?");
			stat.setInt(1, jobID);
			int updatedRecord = stat.executeUpdate();

			//If no record updated
			if (updatedRecord == 0) {
				logger
						.error("deleteTask Could not delete task, taskID not found");
				throw new JobIDNotFoundException(
						"AnalysisHypersonicDAO:deleteJob JobID " + jobID
								+ " not a valid jobID ");
			}

		} catch (Exception e) {
			logger.error("deleteJob failed " + e);
			throw new OmnigeneException(e.getMessage());
		} finally {
			closeConnection(resultSet, stat, conn);
		}
	}

	public TaskInfo getTask(int taskID) throws OmnigeneException,
			RemoteException {
		try {
			return adminDAO.getTask(taskID);
		} catch (AdminDAOSysException e) {
			throw new OmnigeneException(e.getMessage());
		}
	}

	public Vector getTasks() throws OmnigeneException, RemoteException {
		try {
			return toVector(adminDAO.getAllTasks());
		} catch (AdminDAOSysException e) {
			throw new OmnigeneException(e.getMessage());
		}
	}

	public Vector getAllTypeTasks() throws OmnigeneException, RemoteException {
		try {
			return toVector(adminDAO.getAllTasks());
		} catch (AdminDAOSysException e) {
			throw new OmnigeneException(e.getMessage());
		}
	}

	public Vector getTasks(String user_id) throws OmnigeneException,
			RemoteException {
		try {
			if (user_id == null)
				return toVector(adminDAO.getAllTasks());
			return toVector(adminDAO.getAllTasks(user_id));
		} catch (AdminDAOSysException e) {
			throw new OmnigeneException(e.getMessage());
		}

	}

	public Vector getAllTypeTasks(String user_id) throws OmnigeneException,
			RemoteException {
		try {
			return toVector(adminDAO.getAllTasks(user_id));
		} catch (AdminDAOSysException e) {
			throw new OmnigeneException(e.getMessage());
		}
	}

	public int getTaskIDByName(String name, String user_id)
			throws OmnigeneException, RemoteException {
		try {
			return adminDAO.getTaskId(name, user_id);
		} catch (AdminDAOSysException e) {
			throw new OmnigeneException(e.getMessage());
		}
	}

	private Vector toVector(TaskInfo[] tasks) {
		Vector v = new Vector();
		if (tasks != null) {
			for (int i = 0, length = tasks.length; i < length; i++) {
				v.add(tasks[i]);
			}
		}
		return v;
	}

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
			RemoteException {
		int updatedRecord = 0;
		TaskInfo taskInfo = null;

		java.sql.Connection conn = null;
		PreparedStatement stat = null;
		ResultSet resultSet = null;
		int taskID = 0;
		try {
			conn = getConnection();
			conn.setAutoCommit(false); // begin transaction

			TaskInfoAttributes tia = TaskInfoAttributes
					.decode(taskInfoAttributes);
			String sLSID = null;
			if (tia != null) {
				sLSID = tia.get(GPConstants.LSID);
			}

			//Add to task table
			stat = conn
					.prepareStatement("INSERT INTO task_master(task_name, description, parameter_info, taskInfoAttributes, user_id,access_id, lsid) VALUES (?,?,?,?,?,?,?)");

			//stat.setInt(1,taskID);
			stat.setString(1, taskName);
			stat.setString(2, description);
			stat.setString(3, parameter_info);
			stat.setString(4, taskInfoAttributes);
			stat.setString(5, user_id);
			stat.setInt(6, access_id);

			if (sLSID != null && !sLSID.equals("")) {
				stat.setString(7, sLSID.toString());
			} else {
				stat.setString(7, null);
			}

			updatedRecord = stat.executeUpdate();

			//Get new taskID
			stat = conn.prepareStatement("CALL IDENTITY()");
			resultSet = stat.executeQuery();

			if (resultSet.next()) {
				taskID = resultSet.getInt(1);
			} else {
				logger
						.error("Call to task_sequence failed. Could not generate task id");
				throw new Exception(
						"AnalysisHypersonicDAO:addNewTask Call to task_sequence failed. Could not generate task id");
			}

			// write a record into the LSIDs table corresponding to this task
			stat = conn
					.prepareStatement("INSERT INTO lsids(lsid, lsid_no_version, lsid_version) values (?,?,?)");
			if (sLSID != null && !sLSID.equals("")) {
				LSID lsid = new LSID(sLSID);
				stat.setString(1, lsid.toString());
				stat.setString(2, lsid.toStringNoVersion());
				stat.setString(3, lsid.getVersion());
			} else {
				stat.setString(1, null);
				stat.setString(2, null);
				stat.setString(3, null);
			}
			updatedRecord = stat.executeUpdate();

			conn.commit();
			logger.info("Created new regular task, id is " + taskID);

		} catch (Exception e) {
			try {
				conn.rollback();
			} catch (SQLException se) {
				// ignore
			}
			logger.error("addNewTask failed " + e);
			throw new OmnigeneException(e.getMessage());
		} finally {
			closeConnection(resultSet, stat, conn);
		}
		return taskID;
	}

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
			int access_id) throws OmnigeneException, RemoteException {
		int updatedRecord = 0;
		java.sql.Connection conn = null;
		PreparedStatement stat = null;
		ResultSet resultSet = null;
		try {
			conn = getConnection();
			conn.setAutoCommit(false); // begin transaction

			stat = conn
					.prepareStatement("SELECT lsid FROM task_master where task_id=?");
			stat.setInt(1, taskID);
			resultSet = stat.executeQuery();
			String oldLSID = null;
			if (resultSet.next()) {
				oldLSID = resultSet.getString(1);
			}

			//update task
			stat = conn
					.prepareStatement("UPDATE task_master SET parameter_info = ?, description =?, taskInfoAttributes = ?, user_id = ?, access_id = ?, isIndexed=null, lsid=? WHERE task_id = ?");
			stat.setString(1, parameter_info);
			stat.setString(2, taskDescription);
			stat.setString(3, taskInfoAttributes);
			stat.setString(4, user_id);
			stat.setInt(5, access_id);

			TaskInfoAttributes tia = TaskInfoAttributes
					.decode(taskInfoAttributes);
			String sLSID = null;
			LSID lsid = null;
			if (tia != null) {
				sLSID = tia.get(GPConstants.LSID);
			}
			if (sLSID != null && !sLSID.equals("")) {
				lsid = new LSID(sLSID);
				stat.setString(6, sLSID);
			} else {
				stat.setString(6, null);
			}

			stat.setInt(7, taskID);

			updatedRecord = stat.executeUpdate();

			if (oldLSID != null) {
				// delete the old LSID record
				stat = conn.prepareStatement("DELETE from lsids where lsid=?");
				stat.setString(1, oldLSID);
				updatedRecord = stat.executeUpdate();
			}

			if (sLSID != null) {
				// create a new LSID record
				stat = conn
						.prepareStatement("INSERT into lsids(lsid, lsid_no_version, lsid_version) values(?,?,?)");
				stat.setString(1, lsid.toString());
				stat.setString(2, lsid.toStringNoVersion());
				stat.setString(3, lsid.getVersion());
				updatedRecord = stat.executeUpdate();
			}
			conn.commit();
		} catch (Exception e) {
			try {
				conn.rollback();
			} catch (SQLException se) {
				// ignore
			}
			logger.error("AnalysisHypersonicDAO: updateTask failed " + e);
			throw new OmnigeneException(e.getMessage());
		} finally {
			closeConnection(resultSet, stat, conn);
		}
		return updatedRecord;
	}

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
			throws OmnigeneException, RemoteException {
		int updatedRecord = 0;
		java.sql.Connection conn = null;
		PreparedStatement stat = null;
		ResultSet resultSet = null;
		try {
			conn = getConnection();
			conn.setAutoCommit(false); // begin transaction

			stat = conn
					.prepareStatement("SELECT lsid FROM task_master where task_id=?");
			stat.setInt(1, taskID);
			resultSet = stat.executeQuery();
			String oldLSID = null;
			if (resultSet.next()) {
				oldLSID = resultSet.getString(1);
			}

			//update task
			stat = conn
					.prepareStatement("UPDATE task_master SET parameter_info = ?, taskInfoAttributes = ?, user_id = ?, access_id = ?, isIndex=null, lsid=? WHERE task_id = ?");
			stat.setString(1, parameter_info);
			stat.setString(2, taskInfoAttributes);
			stat.setString(3, user_id);
			stat.setInt(4, access_id);

			TaskInfoAttributes tia = TaskInfoAttributes
					.decode(taskInfoAttributes);
			String sLSID = null;
			LSID lsid = null;
			if (tia != null) {
				sLSID = tia.get(GPConstants.LSID);
			}
			if (sLSID != null && !sLSID.equals("")) {
				lsid = new LSID(sLSID);
				stat.setString(5, sLSID);
			} else {
				stat.setString(5, null);
			}

			stat.setInt(6, taskID);
			updatedRecord = stat.executeUpdate();

			if (oldLSID != null) {
				// delete the old LSID record
				stat = conn.prepareStatement("DELETE from lsids where lsid=?");
				stat.setString(1, oldLSID);
				updatedRecord = stat.executeUpdate();
			}

			if (sLSID != null) {
				// create a new LSID record
				stat = conn
						.prepareStatement("INSERT into lsids(lsid, lsid_no_version, lsid_version) values(?,?,?)");
				stat.setString(1, lsid.toString());
				stat.setString(2, lsid.toStringNoVersion());
				stat.setString(3, lsid.getVersion());
				updatedRecord = stat.executeUpdate();
			}

			conn.commit();
		} catch (Exception e) {
			try {
				conn.rollback();
			} catch (SQLException se) {
				// ignore
			}
			logger.error("AnalysisHypersonicDAO: updateTask failed " + e);
			throw new OmnigeneException(e.getMessage());
		} finally {
			closeConnection(null, stat, conn);
		}
		return updatedRecord;
	}

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
			throws OmnigeneException, RemoteException {
		int updatedRecord = 0;
		java.sql.Connection conn = null;
		PreparedStatement stat = null;
		try {
			conn = getConnection();
			//update task
			stat = conn
					.prepareStatement("UPDATE task_master SET user_id = ?, access_id = ? WHERE task_id = ?");
			stat.setString(1, user_id);
			stat.setInt(2, access_id);
			stat.setInt(3, taskID);

			updatedRecord = stat.executeUpdate();
		} catch (Exception e) {
			logger.error("AnalysisHypersonicDAO: updateTask failed " + e);
			throw new OmnigeneException(e.getMessage());
		} finally {
			closeConnection(null, stat, conn);
		}
		return updatedRecord;
	}

	/**
	 * To remove registered task based on task ID
	 * 
	 * @param taskID
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return No. of updated records
	 */
	public int deleteTask(int taskID) throws OmnigeneException, RemoteException {
		int updatedRecord = 0;

		java.sql.Connection conn = null;
		PreparedStatement stat = null;
		ResultSet resultSet = null;

		try {

			conn = getConnection();
			//delete from task table
			stat = conn
					.prepareStatement("DELETE FROM task_master WHERE task_id = ? ");

			stat.setInt(1, taskID);
			updatedRecord = stat.executeUpdate();

			//If no record updated
			if (updatedRecord == 0) {
				logger
						.error("deleteTask Could not delete task, taskID not found");
				throw new TaskIDNotFoundException(
						"AnalysisHypersonicDAO:deleteTask TaskID " + taskID
								+ " not a valid TaskID ");
			}

		} catch (Exception e) {
			logger.error("deleteTask failed " + e);
			throw new OmnigeneException(e.getMessage());
		} finally {
			closeConnection(resultSet, stat, conn);
		}

		return updatedRecord;
	}

	/**
	 * To remove registered task based on task ID
	 * 
	 * @param taskID
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return No. of updated records
	 */
	public int deleteTask(LSID lsid) throws OmnigeneException, RemoteException {
		int updatedRecord = 0;

		java.sql.Connection conn = null;
		PreparedStatement stat = null;
		ResultSet resultSet = null;

		try {

			conn = getConnection();
			//delete from task table
			stat = conn
					.prepareStatement("DELETE FROM task_master WHERE lsid = ? ");

			stat.setString(1, lsid.toString());
			updatedRecord = stat.executeUpdate();

			//If no record updated
			if (updatedRecord == 0) {
				logger
						.error("deleteTask Could not delete task, lsid not found");
				throw new TaskIDNotFoundException(
						"AnalysisHypersonicDAO:deleteTask LSID " + lsid
								+ " not a valid LSID");
			}

		} catch (Exception e) {
			logger.error("deleteTask failed " + e);
			throw new OmnigeneException(e.getMessage());
		} finally {
			closeConnection(resultSet, stat, conn);
		}

		return updatedRecord;
	}

	//Returns Connection from connection pool based on DAO settings
	private Connection getConnection() throws OmnigeneException {
		Properties props = new Properties();

		// use properties file for database connection driver class, URL,
		// username, password
		String gpPropsFilename = System.getProperty("genepattern.properties");
		File gpProps = new File(gpPropsFilename, "genepattern.properties");
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(gpProps);
			props.load(fis);
		} catch (IOException ioe) {
			throw new OmnigeneException(gpProps.getAbsolutePath()
					+ " cannot be loaded.  " + ioe.getMessage());
		} finally {
			try {
				if (fis != null)
					fis.close();
			} catch (IOException ioe) {
			}
		}
		String driver = props.getProperty("DB.driver", "org.hsqldb.jdbcDriver");
		String url = props.getProperty("DB.url",
				"jdbc:hsqldb:hsql://localhost:9001");
		String username = props.getProperty("DB.username", "sa");
		String password = props.getProperty("DB.password", "");

		try {
			Class.forName(driver);
			return DriverManager.getConnection(url, username, password);
		} catch (ClassNotFoundException cnfe) {
			throw new OmnigeneException(cnfe.toString());
		} catch (SQLException sqle) {
			System.err
					.println("AnalysisHypersonicDAO:getConnection UNABLE to get a connection from AnalysisDS!"
							+ sqle);
			System.err
					.println("Please make sure that you have setup the connection pool properly");
			throw new OmnigeneException(sqle.toString());
		}

	}

	private void closeConnection(ResultSet resultSet, Statement stat,
			Connection conn) {
		if (resultSet != null) {
			try {
				resultSet.close();
			} catch (Exception rex) {
				logger.error("Failed to close ResultSet " + rex);
			}
		}
		if (stat != null) {
			try {
				stat.close();
			} catch (Exception sex) {
				logger.error("Failed to close Statement " + sex);
			}
		}
		if (conn != null) {
			try {
				conn.close();
			} catch (Exception cex) {
				logger.error("Failed to close connection " + cex);
			}
		}
	}

	/**
	 * Starts a running thread of a new task, this method could be called after
	 * creating a new task
	 * 
	 * @param id
	 *            taskID
	 * @throws OmnigeneException
	 */
	public void startNewTask(int id) throws OmnigeneException, RemoteException {
		AnalysisManager analysisManager = AnalysisManager.getInstance();
		analysisManager.startNewAnalysisTask(id);
	}


	/**
	 * Stops the running thread of a task
	 * 
	 * @param taskID
	 *            analysis task ID
	 */
	public void stopTask(int taskID) throws RemoteException {
		AnalysisManager analysisManager = AnalysisManager.getInstance();
		analysisManager.stop(taskID);
	}

	/**
	 * reset any previous running (but incomplete) jobs to waiting status, clear
	 * their output files
	 * 
	 * @return true if there were running jobs
	 * @author Jim Lerner
	 *  
	 */
	public boolean resetPreviouslyRunningJobs() throws RemoteException,
			OmnigeneException {
		java.sql.Connection conn = null;
		PreparedStatement stat = null;
		boolean exist = false;
		try {
			//getConnection
			conn = getConnection();

			stat = conn.prepareStatement("update analysis_job set status_id="
					+ JOB_WAITING_STATUS + " where status_id="
					+ PROCESSING_STATUS);
			exist = (stat.executeUpdate() > 0);

		} catch (Exception e) {
			logger
					.error("AnalysisHypersonicDAO: resetPreviouslyRunningJobs() failed: "
							+ e);
			throw new OmnigeneException(e.getMessage());
		} finally {
			closeConnection(null, stat, conn);
		}
		return exist;
	}

	/**
	 * execute arbitrary SQL on database, returning ResultSet
	 * 
	 * @param sql
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return ResultSet
	 */
	public ResultSet executeSQL(String sql) throws OmnigeneException,
			RemoteException {
		java.sql.Connection conn = null;
		Statement stat = null;
		ResultSet resultSet = null;

		try {
			//getConnection
			conn = getConnection();

			stat = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			resultSet = stat.executeQuery(sql);

		} catch (Exception e) {
			logger.error("AnalysisHypersonicDAO: executeSQL for " + sql
					+ " failed " + e);
			throw new OmnigeneException(e.getMessage());
		}

		finally {
			closeConnection(null, null, conn);
		}

		return resultSet;

	}

	/**
	 * execute arbitrary SQL on database, returning int
	 * 
	 * @param sql
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return int number of rows returned
	 */
	public int executeUpdate(String sql) throws OmnigeneException,
			RemoteException {
		java.sql.Connection conn = null;
		Statement stat = null;
		int ret = 0;

		try {
			//getConnection
			conn = getConnection();

			stat = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			ret = stat.executeUpdate(sql);

		} catch (Exception e) {
			logger.error("AnalysisHypersonicDAO: executeSQL for " + sql
					+ " failed " + e);
			throw new OmnigeneException(e.getMessage());
		}

		finally {
			closeConnection(null, null, conn);
		}

		return ret;

	}

	/**
	 * get the next available LSID identifer from the database
	 * 
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return int next identifier in sequence
	 */
	public int getNextLSIDIdentifier() throws OmnigeneException,
			RemoteException {
		java.sql.Connection conn = null;
		PreparedStatement stat = null;
		ResultSet resultSet = null;
		int nextIdentifier = -1;
		try {
			//getConnection
			conn = getConnection();

			stat = conn
					.prepareStatement("select next value for lsid_identifier_seq from dual");
			resultSet = stat.executeQuery();

			//only one record
			if (resultSet.next()) {
				nextIdentifier = resultSet.getInt(1);
			} else {
				throw new OmnigeneException(
						"Unable to retrieve lsid_identifier_seq");
			}

		} catch (Exception e) {
			logger.error("AnalysisHypersonicDAO: getNextLSIDIdentifier failed"
					+ e);
			throw new OmnigeneException(e.getMessage());
		} finally {
			closeConnection(resultSet, stat, conn);
		}
		return nextIdentifier;
	}

	/**
	 * get the next available LSID version for a given identifer from the
	 * database
	 * 
	 * @throws OmnigeneException
	 * @throws RemoteException
	 * @return int next version in sequence
	 */
	public String getNextLSIDVersion(LSID lsid) throws OmnigeneException,
			RemoteException {
		java.sql.Connection conn = null;
		PreparedStatement stat = null;
		ResultSet resultSet = null;
		String nextVersion = "1";
		try {
			//getConnection
			conn = getConnection();

			LSID newLSID = lsid.copy();
			newLSID.setVersion(newLSID.getIncrementedMinorVersion());
			String query = "select lsid from lsids where lsid='" + newLSID
					+ "'";
			stat = conn.prepareStatement(query);
			resultSet = stat.executeQuery();

			//only one record
			// example: old version=9, see if version 10 is taken. If not,
			// that's the answer.
			// If it is taken, see if version 9.1 is taken. Etc.
			if (resultSet.next()) {
				newLSID.setVersion(lsid.getVersion() + ".0");
				//System.out.println("AHDAO.getNextLSIDVersion: recursing with
				// " + newLSID.getVersion());
				nextVersion = getNextLSIDVersion(newLSID);
			} else {
				// not found: must be version 1
				nextVersion = newLSID.getVersion();
				//System.out.println("AHDAO.getNextLSIDVersion: returning " +
				// nextVersion);
			}

		} catch (Exception e) {
			logger.error("AnalysisHypersonicDAO: getNextLSIDVersion failed: "
					+ e);
			throw new OmnigeneException(e.getMessage());
		} finally {
			closeConnection(resultSet, stat, conn);
		}
		return nextVersion;
	}

}