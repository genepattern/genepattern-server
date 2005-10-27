package org.genepattern.server.webservice.server.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.genepattern.server.genepattern.LSIDManager;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.server.webservice.server.DirectoryManager;

/**
 * @author Joshua Gould
 */
public class AdminHSQLDAO implements AdminDAO {
	private static String dbURL;

	private static String dbPassword;

	private static String dbUsername;

	private static int PUBLIC_ACCESS_ID = 1;

	private static Category log = Logger.getInstance(AdminHSQLDAO.class);

	protected static TaskInfo taskInfoFromResultSet(ResultSet resultSet)
			throws SQLException {
		int taskID = resultSet.getInt("task_id");
		String taskName = resultSet.getString("task_name");
		String description = resultSet.getString("description");
		String parameter_info = resultSet.getString("parameter_info");
		String taskInfoAttributes = resultSet.getString("taskInfoAttributes");
		String userId = resultSet.getString("user_id");
		int accessId = resultSet.getInt("access_id");
		TaskInfo task = new TaskInfo(taskID, taskName, description,
				parameter_info, TaskInfoAttributes
						.decode(taskInfoAttributes), userId, accessId);
		return task;
	}

	protected SuiteInfo suiteInfoFromResultSet(ResultSet resultSet)
			throws SQLException, AdminDAOSysException {

		String lsid = resultSet.getString("lsid");
		int access_id = resultSet.getInt("access_id");
		String name = resultSet.getString("name");
		String description = resultSet.getString("description");
		String owner = resultSet.getString("owner");
		String author = resultSet.getString("author");
		// int accessId = resultSet.getInt("access_id");

		ArrayList docs = new ArrayList();
		try {
			String suiteDirStr = DirectoryManager.getSuiteLibDir(name, lsid,owner);
			File suiteDir = new File(suiteDirStr);
		
			if (suiteDir.exists()){
				File docFiles[] = suiteDir.listFiles();
				for (int i=0; i < docFiles.length; i++){
					File f = docFiles[i];
					docs.add(f.getName());				
				}
			}
		} catch (Exception e){
			// swallow & no docs
			e.printStackTrace();
		}



		ArrayList mods = getSuiteModules(lsid);

		SuiteInfo suite = new SuiteInfo(lsid, name, description, owner, author, mods, access_id, docs);

		return suite;

	}


	protected void close(ResultSet rs, Statement st, Connection c) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException x) {
			}
		}
		if (st != null) {
			try {
				st.close();
			} catch (SQLException x) {
			}
		}
		if (c != null) {
			try {
				c.close();
			} catch (SQLException x) {
			}
		}
	}

	private TaskInfo[] _getTasks(String sql, boolean sort)
			throws AdminDAOSysException {
		Connection c = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			c = getConnection();
			st = c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			rs = st.executeQuery(sql);
			List tasks = new ArrayList();
			while (rs.next()) {
				tasks.add(taskInfoFromResultSet(rs));
			}
			TaskInfo[] unsorted = (TaskInfo[]) tasks.toArray(new TaskInfo[0]);

			if (sort) {
				// order by lower(task_name), lsid_no_version, lsid version
				// descending
				Arrays.sort(unsorted, new Comparator() {
					public int compare(Object o1, Object o2) {
						TaskInfo t1 = (TaskInfo) o1;
						TaskInfo t2 = (TaskInfo) o2;

						// compare task names
						int c;
						c = t1.getName().compareToIgnoreCase(t2.getName());
						if (c != 0) {
							return c;
						}

						// compare lsid_no_version
						try {
							LSID lsid1 = new LSID((String) t1
									.getTaskInfoAttributes().get(
											GPConstants.LSID));
							LSID lsid2 = new LSID((String) t2
									.getTaskInfoAttributes().get(
											GPConstants.LSID));
							c = lsid1.compareTo(lsid2);
							return c;
						} catch (MalformedURLException mue) {
							// ignore
							return 0;
						}
					}
				});
			}
			return unsorted;
		} catch (SQLException e) {
			e.printStackTrace();// FIXME
			throw new AdminDAOSysException("A database error occurred.", e);
		} finally {
			close(rs, st, c);
		}
	}

	// FIXME see doc for AdminDAO.getTaskId
	private ResultSet _getTask(String lsidOrTaskName, String username,
			Connection c, Statement st) throws SQLException {
		if (lsidOrTaskName == null || lsidOrTaskName.trim().equals("")) {
			return null;
		}
		ResultSet rs = null;
		String sql = null;
		try {
			LSID lsid = new LSID(lsidOrTaskName);
			String version = lsid.getVersion();
			if (version != null && !version.equals("")) {
				if (username != null) {
					sql = "SELECT * FROM task_Master WHERE lsid='"
							+ lsidOrTaskName + "' AND (user_id='" + username
							+ "' OR access_id=" + GPConstants.ACCESS_PUBLIC
							+ ")";
				} else {
					sql = "SELECT * FROM task_Master WHERE lsid='"
							+ lsidOrTaskName + "'";
				}

			} else {
				if (username != null) {
					sql = "SELECT * FROM task_master, (SELECT MAX(lsid_version) AS max_version, lsid_no_version FROM lsids, task_master WHERE lsids.lsid=task_master.lsid AND lsid_no_version='"
							+ lsidOrTaskName
							+ "' AND (user_id='"
							+ username
							+ "' OR access_id="
							+ GPConstants.ACCESS_PUBLIC
							+ ") GROUP BY lsid_no_version) WHERE task_master.lsid=CONCAT(CONCAT(lsid_no_version, ':'), max_version)";
				} else {
					sql = "SELECT * FROM task_master, (SELECT MAX(lsid_version) AS max_version, lsid_no_version FROM lsids, task_master WHERE lsids.lsid=task_master.lsid AND lsid_no_version='"
							+ lsidOrTaskName
							+ "' GROUP BY lsid_no_version) WHERE task_master.lsid=CONCAT(CONCAT(lsid_no_version, ':'), max_version)";
				}
			}

			rs = st.executeQuery(sql);
			if (rs.next()) {
				return rs;
			}
			return null;
		} catch (java.net.MalformedURLException e) {
			// find the 'best' match
			if (username != null) {
				sql = "SELECT * FROM task_master, (SELECT lsid_no_version AS no_version, MAX(lsid_version) AS max_version FROM task_master, lsids WHERE task_name='"
						+ lsidOrTaskName
						+ "' AND lsids.lsid=task_master.lsid AND (user_id='"
						+ username
						+ "' OR access_id="
						+ GPConstants.ACCESS_PUBLIC
						+ ") GROUP BY lsid_no_version) WHERE task_master.lsid=CONCAT(CONCAT(no_version, ':'), max_version)";
			} else {
				sql = "SELECT * FROM task_master, (SELECT lsid_no_version AS no_version, MAX(lsid_version) AS max_version FROM task_master, lsids WHERE task_name='"
						+ lsidOrTaskName
						+ "' AND lsids.lsid=task_master.lsid GROUP BY lsid_no_version) WHERE task_master.lsid=CONCAT(CONCAT(no_version, ':'), max_version)";
			}

			c = getConnection();
			st = c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			rs = st.executeQuery(sql);

			int index = 1;
			LSID closestLSID = null;
			int taskIndex = -1;
			while (rs.next()) {
				try {
					LSID lsid = new LSID(rs.getString("lsid"));
					if (closestLSID == null) {
						closestLSID = lsid;
					} else {
						closestLSID = LSIDManager.getInstance().getNearerLSID(
								closestLSID, lsid);
					}
					if (closestLSID == lsid) {
						taskIndex = index;
					}
				} catch (java.net.MalformedURLException mfe) {
				}// shouldn't happen
				index++;
			}
			if (taskIndex == -1) {
				return null;
			}
			rs.absolute(taskIndex);
			return rs;
		}

	}

	public int getTaskId(String lsidOrTaskName, String username)
			throws AdminDAOSysException {
		ResultSet rs = null;
		Connection c = null;
		Statement st = null;
		try {
			c = getConnection();
			st = c.createStatement();
			rs = _getTask(lsidOrTaskName, username, c, st);
			int returnValue = rs != null ? rs.getInt("task_id") : -1;
			return returnValue;
		} catch (SQLException e) {
			throw new AdminDAOSysException("A database error occurred.", e);
		} finally {
			close(rs, st, c);
		}
	}

	public TaskInfo getTask(String lsidOrTaskName, String username)
			throws AdminDAOSysException {
		ResultSet rs = null;
		Connection c = null;
		Statement st = null;
		try {
			c = getConnection();
			st = c.createStatement();
			rs = _getTask(lsidOrTaskName, username, c, st);
			return rs != null ? taskInfoFromResultSet(rs) : null;
		} catch (SQLException e) {
			throw new AdminDAOSysException("A database error occurred.", e);
		} finally {
			close(rs, st, c);
		}
	}

	public TaskInfo[] getAllTasks() throws AdminDAOSysException {
		String sql = "SELECT * FROM task_master";
		return _getTasks(sql, true);
	}

	public TaskInfo[] getAllTasks(String username) throws AdminDAOSysException {
		String sql = "SELECT * FROM task_master where (user_id='" + username
				+ "' OR access_id = " + PUBLIC_ACCESS_ID + ")";
		return _getTasks(sql, true);
	}

	public TaskInfo[] getLatestTasksByName(String username)
			throws AdminDAOSysException {
		Connection c = null;
		Statement st = null;
		ResultSet rs = null;
		Map taskName2TaskInfoMap = new HashMap();
		try {
			String sql = "SELECT * FROM task_master,(SELECT lsid_no_version AS no_version, MAX(lsid_version) AS max_version FROM task_master, lsids WHERE task_master.lsid=lsids.lsid AND (user_id='"
					+ username
					+ "' OR access_id="
					+ GPConstants.ACCESS_PUBLIC
					+ ") GROUP BY lsid_no_version) WHERE task_master.lsid=CONCAT(CONCAT(no_version, ':'), max_version)";
			c = getConnection();
			st = c.createStatement();
			rs = st.executeQuery(sql);
			while (rs.next()) {
				TaskInfo currentTask = taskInfoFromResultSet(rs);
				TaskInfo closestTask = (TaskInfo) taskName2TaskInfoMap
						.get(currentTask.getName());
				if (closestTask != null) {
					try {
						LSID currentTaskLSID = new LSID((String) currentTask
								.getTaskInfoAttributes().get(GPConstants.LSID));
						LSID closestTaskLSID = new LSID((String) closestTask
								.getTaskInfoAttributes().get(GPConstants.LSID));
						LSID result = LSIDManager.getInstance().getNearerLSID(
								currentTaskLSID, closestTaskLSID);
						closestTask = result == currentTaskLSID ? currentTask
								: closestTask;
					} catch (java.net.MalformedURLException mfe) {
					}
				} else {
					closestTask = currentTask;
				}
				taskName2TaskInfoMap.put(closestTask.getName(), closestTask);
			}
		} catch (SQLException e) {
			throw new AdminDAOSysException("A database error occurred.", e);
		} finally {
			close(rs, st, c);
		}
		TaskInfo[] results = new TaskInfo[taskName2TaskInfoMap.size()];
		int index = 0;
		for (Iterator keys = taskName2TaskInfoMap.keySet().iterator(); keys
				.hasNext();) {
			results[index++] = (TaskInfo) taskName2TaskInfoMap.get(keys.next());
		}
		return results;
	}
	
	public TaskInfo[] getLatestTasks(String username)
			throws AdminDAOSysException {
		String sql = "SELECT * FROM task_master, (SELECT lsid_no_version AS no_version, MAX(CONVERT(lsid_version, INT)) AS max_version FROM lsids, task_master WHERE task_master.lsid=lsids.lsid AND (user_id='"
				+ username
				+ "' OR access_id="
				+ GPConstants.ACCESS_PUBLIC
				+ ") GROUP BY lsid_no_version) WHERE task_master.lsid=CONCAT(CONCAT(no_version, ':'), max_version)";
		return _getTasks(sql, true);
	}

	public TaskInfo getTask(int taskId) throws AdminDAOSysException {
		Connection c = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try {
			c = getConnection();
			st = c
					.prepareStatement("SELECT * FROM task_Master where task_id =?");
			st.setInt(1, taskId);
			rs = st.executeQuery();
			if (rs.next()) {
				return taskInfoFromResultSet(rs);
			}
			throw new AdminDAOSysException("task id " + taskId + " not found");
		} catch (SQLException e) {
			throw new AdminDAOSysException("A database error occurred", e);
		} finally {
			close(rs, st, c);
		}
	}

	protected ArrayList getSuiteModules(String lsid) throws AdminDAOSysException{
		Connection c = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		ArrayList moduleLSIDs = new ArrayList();

		try {
			c = getConnection();
			st = c.prepareStatement("SELECT * FROM suite_modules where lsid =?");
			st.setString(1, lsid);
			rs = st.executeQuery();
			while (rs.next()) {
				String modlsid = rs.getString("module_lsid");
				moduleLSIDs.add(modlsid);				
			}
		} catch (SQLException e) {
			throw new AdminDAOSysException("A database error occurred", e);
		} finally {
			close(rs, st, c);
		}
		return moduleLSIDs;
	}



	public SuiteInfo getSuite(String lsid) throws AdminDAOSysException{
		Connection c = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try {
			c = getConnection();
			st = c.prepareStatement("SELECT * FROM suite where lsid =?");
			st.setString(1, lsid);
			rs = st.executeQuery();
			if (rs.next()) {
				return suiteInfoFromResultSet(rs);
			}
			throw new AdminDAOSysException("suite id " + lsid + " not found");
		} catch (SQLException e) {
			throw new AdminDAOSysException("A database error occurred", e);
		} finally {
			close(rs, st, c);
		}
	}

	/**
	 * Gets the latest versions of all suites
	 * 
	 * @return The latest suites
	 * @exception WebServiceException
	 *                If an error occurs
	 */
	public SuiteInfo[] getLatestSuites() throws AdminDAOSysException{
	try {	
		SuiteInfo[] allSuites = getAllSuites();
		TreeMap latestSuites = new TreeMap();		
		// loop through them placing them into a tree set based on their LSIDs
		for (int i=0; i < allSuites.length; i++){
			SuiteInfo si = allSuites[i];
			LSID siLsid = new LSID(si.getLSID());		
			
			SuiteInfo altSi = (SuiteInfo)latestSuites.get(siLsid.toStringNoVersion());



			if (altSi == null){
				latestSuites.put(siLsid.toStringNoVersion(), si);
			} else {
				LSID altLsid = new LSID(altSi.getLSID());
				if (altLsid.compareTo(siLsid) > 0){
					latestSuites.put(siLsid.toStringNoVersion(), si); // it is newer
				} // else it is older so leave it out

			}
		}

		SuiteInfo[] latest = new SuiteInfo[latestSuites.size()];
		int i=0; 
		for (Iterator iter = latestSuites.keySet().iterator(); iter.hasNext(); i++){
			latest[i] = (SuiteInfo)latestSuites.get(iter.next());
		}
		return latest;
		} catch (Exception mfe){
			throw new AdminDAOSysException("A database error occurred", mfe);

		}
	}

	/**
	 * Gets all versions of all suites
	 * 
	 * @return The suites
	 * @exception WebServiceException
	 *                If an error occurs
	 */
	public SuiteInfo[] getAllSuites() throws AdminDAOSysException{

		Connection c = null;
		PreparedStatement st = null;

		ResultSet rs = null;
		ArrayList suites = new ArrayList();
		try {
			c = getConnection();
			st = c.prepareStatement("SELECT * FROM suite");
			rs = st.executeQuery();
			while (rs.next()) {
				SuiteInfo suite = suiteInfoFromResultSet(rs);
				suites.add(suite);
			}

		} catch (SQLException e) {
			throw new AdminDAOSysException("A database error occurred", e);
		} finally {
			close(rs, st, c);
		}

		return (SuiteInfo[])suites.toArray(new SuiteInfo[suites.size()]);
	}

	/**
	 * Gets all suites this task is a part of
	 * 
	 * @return The suites
	 * @exception WebServiceException
	 *                If an error occurs
	 */
	public SuiteInfo[] getSuiteMembership(String taskLsid) throws AdminDAOSysException{
		Connection c = null;
		PreparedStatement st = null;

		ResultSet rs = null;
		ArrayList suites = new ArrayList();
		
		try {
			c = getConnection();
			st = c.prepareStatement("SELECT lsid FROM suite_modules where module_lsid = ?");
			st.setString(1, taskLsid);
			rs = st.executeQuery();
			while (rs.next()) {
				String suiteId = rs.getString("lsid");
				suites.add(getSuite(suiteId));
			}
			close(rs, st, c);

			
		} catch (SQLException e) {
			throw new AdminDAOSysException("A database error occurred", e);
		} finally {
			close(rs, st, c);
		}

		return (SuiteInfo[])suites.toArray(new SuiteInfo[suites.size()]);	}




	public Map getSchemaProperties() {
		Connection c = null;
		Statement st = null;
		ResultSet rs = null;
		HashMap hmResults = new HashMap();
		try {
			String sql = "SELECT key, value from props";

			c = getConnection();
			st = c.createStatement();
			rs = st.executeQuery(sql);
			while (rs.next()) {
				hmResults.put(rs.getString(1), rs.getString(2));
			}
		} catch (SQLException se) {
			System.err.println(se + " while getting schema properties");
		} finally {
			close(rs, st, c);
		}
		return hmResults;
	}

	public static String getSchemaVersion() {
		Map hmProperties = new AdminHSQLDAO().getSchemaProperties();
		return (String) hmProperties.get("schemaVersion");
	}

	private Connection getConnection() throws SQLException {
		try {
			return DriverManager.getConnection(dbURL, dbUsername, dbPassword);
		} catch (SQLException se) {
			System.err.println("AdminHSQLDAO: " + se
					+ " while getting connection to " + dbURL + ", "
					+ dbUsername + ", " + dbPassword);
			throw se;
		}
	}

	static {
		Properties props = new Properties();
		String gpPropsFilename = System.getProperty("genepattern.properties");
		//System.out.println("GPPropsFile="+ gpPropsFilename);
		File gpProps = new File(gpPropsFilename, "genepattern.properties");
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(gpProps);
			props.load(fis);
		} catch (IOException ioe) {
			log.error("Error reading genepattern.properties");
		} finally {
			try {
				if (fis != null) {
					fis.close();
				}
			} catch (IOException ioe) {
			}
		}

		String driver = props.getProperty("DB.driver", "org.hsqldb.jdbcDriver");
		try {
			Class.forName(driver);
		} catch (ClassNotFoundException cnfe) {
		}
		dbURL = props
				.getProperty("DB.url", "jdbc:hsqldb:hsql://localhost:9001");
		dbUsername = props.getProperty("DB.username", "sa");
		dbPassword = props.getProperty("DB.password", "");
	}
}