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
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.WebServiceException;
import org.genepattern.server.process.SuiteRepository;

/**
 * @author Ted Liefeld
 * eventually some of the GenePatternAnalysisTask stuff should move here
 */
public class TaskIntegratorHSQLDAO {
	private static String dbURL;

	private static String dbPassword;

	private static String dbUsername;

	private static int PUBLIC_ACCESS_ID = 1;

	private static Category log = Logger.getInstance(TaskIntegratorHSQLDAO.class);

	protected SuiteInfo suiteInfoFromResultSet(ResultSet resultSet)
			throws SQLException, AdminDAOSysException {

		String lsid = (String)resultSet.getString("lsid");
		int access_id = (int)resultSet.getInt("access_id");
		String name = (String)resultSet.getString("name");
		String description = (String)resultSet.getString("description");
		String owner = (String)resultSet.getString("owner");
		String author = (String)resultSet.getString("author");
		// int accessId = resultSet.getInt("access_id");

		ArrayList mods = null;//getSuiteModules(lsid);

		SuiteInfo suite = new SuiteInfo(lsid, name, description, owner, author, mods, 1);

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
	



	public  void deleteSuite(String lsid) throws WebServiceException {
		Connection c = null;
		PreparedStatement st = null;
		try {
			c = getConnection();
			st = c.prepareStatement("delete FROM suite where lsid =?");
			st.setString(1, lsid);
			st.executeUpdate();
			close(null, st, c);
			
			c = getConnection();
			st = c.prepareStatement("delete FROM suite_modules where lsid =?");
			st.setString(1, lsid);
			st.executeUpdate();
			

		} catch (SQLException e) {
			e.printStackTrace();
			throw new WebServiceException("A database error occurred", e);
		} finally {
			close(null, st, c);
		}

	}

	public void installSuite(String lsid) throws WebServiceException {
		try {
			SuiteRepository sr = new SuiteRepository();
			HashMap suites = sr.getSuites(System.getProperty("SuiteRepositoryURL"));
	
			HashMap hm = (HashMap)suites.get(lsid);
			// get the info from the HashMap and install it into the DB
			SuiteInfo suite = new SuiteInfo(hm);

			installSuite(suite);
		} catch (Exception e){
			throw new WebServiceException(e);
		}
	}

	public void installSuite(SuiteInfo suite) throws WebServiceException {
		Connection c = null;
		PreparedStatement st = null;
		try {
			deleteSuite(suite.getLSID());

			c = getConnection();
			st = c.prepareStatement("insert into suite (lsid, name, description, author, owner, access_id ) values (?, ?, ?, ?, ?, ?)");
			st.setString(1, suite.getLSID());
			st.setString(2, suite.getName());
			st.setString(3, suite.getDescription());
			st.setString(4, suite.getAuthor());
			st.setString(5, suite.getOwner());
			st.setInt(6, 1);

			st.executeUpdate();
			close(null, st, c);
			String lsid = suite.getLSID();
			
			c = getConnection();
			st = c.prepareStatement("delete FROM suite_modules where lsid =?");
			st.setString(1, suite.getLSID());
			int done = st.executeUpdate();
			
			String[] modLsids = suite.getModuleLSIDs();
			for (int i=0; i < modLsids.length; i++){
				installSuiteModule(lsid, modLsids[i]);
			}


		} catch (SQLException e) {
			e.printStackTrace();
			throw new WebServiceException("A database error occurred", e);
		} finally {
			close(null, st, c);
		}
	}
	
	public void installSuiteModule(String lsid, String mod_lsid) throws WebServiceException {
		Connection c = null;
		PreparedStatement st = null;
		try {
			c = getConnection();
			st = c.prepareStatement("insert  into suite_modules (lsid, module_lsid) values (?, ?)");
			st.setString(1, lsid);
			st.setString(2, mod_lsid);
			st.executeUpdate();
			

		} catch (SQLException e) {
			e.printStackTrace();
			throw new WebServiceException("A database error occurred", e);
		} finally {
			close(null, st, c);
		}


	}

	public String cloneSuite(String lsid, String cloneName) throws WebServiceException {
		throw new WebServiceException("Clone suite Not implemented yet");
	}


	public String modifySuite(int access_id, String lsid, String name, String description,
			String author, String owner, String[] moduleLsids, 
			javax.activation.DataHandler[] dataHandlers, String[] fileNames)
			throws WebServiceException{

		throw new WebServiceException("modifySuite Not implemented yet");

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


	
	
	private Connection getConnection() throws SQLException {
		try {
			return DriverManager.getConnection(dbURL, dbUsername, dbPassword);
		} catch (SQLException se) {
			System.err.println("TaskIntegratorHSQLDAO: " + se
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