package org.genepattern.server.webservice.server.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.genepattern.LSIDManager;
import org.genepattern.server.process.SuiteRepository;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.Util;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * @author Ted Liefeld eventually some of the GenePatternAnalysisTask stuff
 *         should move here
 */
public class TaskIntegratorHSQLDAO {
	private static String dbURL;

	private static String dbPassword;

	private static String dbUsername;

	private static int PUBLIC_ACCESS_ID = 1;

	private static Category log = Logger
			.getInstance(TaskIntegratorHSQLDAO.class);

	protected SuiteInfo suiteInfoFromResultSet(ResultSet resultSet)
			throws SQLException, AdminDAOSysException {

		String lsid = (String) resultSet.getString("lsid");
		int access_id = (int) resultSet.getInt("access_id");
		String name = (String) resultSet.getString("name");
		String description = (String) resultSet.getString("description");
		String owner = (String) resultSet.getString("owner");
		String author = (String) resultSet.getString("author");
		// int accessId = resultSet.getInt("access_id");

		ArrayList mods = null;// getSuiteModules(lsid);
		ArrayList docs = new ArrayList();

		try {
			String suiteDirStr = DirectoryManager.getSuiteLibDir(name, lsid,
					owner);
			File suiteDir = new File(suiteDirStr);
			if (suiteDir.exists()) {
				File docFiles[] = suiteDir.listFiles();
				for (int i = 0; i < docFiles.length; i++) {
					File f = docFiles[i];
					docs.add(f.getName());
				}
			}
		} catch (Exception e) {
			// swallow, just no docs
			e.printStackTrace();
		}

		SuiteInfo suite = new SuiteInfo(lsid, name, description, owner, author,
				mods, 1, docs);

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

	public void deleteSuite(String lsid) throws WebServiceException {
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
			HashMap suites = sr.getSuites(System
					.getProperty("SuiteRepositoryURL"));

			HashMap hm = (HashMap) suites.get(lsid);
			// get the info from the HashMap and install it into the DB
			SuiteInfo suite = new SuiteInfo(hm);

			installSuite(suite);
		} catch (Exception e) {
			throw new WebServiceException(e);
		}
	}

	public String installSuite(ZipFile zipFile) throws WebServiceException {
		try {
			System.out.println("Installing suite from zip");

			HashMap hm = SuiteRepository.getSuiteMap(zipFile);
			SuiteInfo suite = new SuiteInfo(hm);

			// now we need to extract the doc files and repoint the suiteInfo
			// docfiles to the file url of the extracted version
			String[] filenames = suite.getDocumentationFiles();
			for (int j = 0; j < filenames.length; j++) {
				int i = 0;
				String name = filenames[j];
				ZipEntry zipEntry = (ZipEntry) zipFile.getEntry(name);
				System.out.println("name= " + name + " ze= " + zipEntry);
				if (zipEntry != null) {
					InputStream is = zipFile.getInputStream(zipEntry);
					File outFile = new File(System
							.getProperty("java.io.tmpdir"), zipEntry.getName());
					FileOutputStream os = new FileOutputStream(outFile);
					long fileLength = zipEntry.getSize();
					long numRead = 0;
					byte[] buf = new byte[100000];
					while ((i = is.read(buf, 0, buf.length)) > 0) {
						os.write(buf, 0, i);
						numRead += i;
					}
					os.close();
					os = null;
					outFile.setLastModified(zipEntry.getTime());
					is.close();
					filenames[j] = outFile.toURL().toString();
				}
			}

			return installSuite(suite);
		} catch (Exception e) {
			e.printStackTrace();
			throw new WebServiceException(e);
		}
	}

	public String installSuite(SuiteInfo suite) throws WebServiceException {
		Connection c = null;
		PreparedStatement st = null;

		if (suite.getLSID() != null)
			if (suite.getLSID().trim().length() == 0)
				suite.setLSID(null);

		try {
			if (suite.getLSID() != null)
				deleteSuite(suite.getLSID());
			else {
				// create an LSID for this module
				LSIDManager lsidManager = LSIDManager.getInstance();

				String lsid = lsidManager.createNewID(
						GPConstants.SUITE_NAMESPACE).toString();

				suite.setLSID(lsid);
			}
			c = getConnection();
			st = c
					.prepareStatement("insert into suite (lsid, name, description, author, owner, access_id ) values (?, ?, ?, ?, ?, ?)");
			st.setString(1, suite.getLSID());
			st.setString(2, suite.getName());
			st.setString(3, suite.getDescription());
			st.setString(4, suite.getAuthor());
			st.setString(5, suite.getOwner());
			st.setInt(6, suite.getAccessId());

			st.executeUpdate();
			close(null, st, c);
			String lsid = suite.getLSID();

			c = getConnection();
			st = c.prepareStatement("delete FROM suite_modules where lsid =?");
			st.setString(1, suite.getLSID());
			int done = st.executeUpdate();
			String suiteDir = DirectoryManager.getSuiteLibDir(suite.getName(),
					suite.getLSID(), suite.getOwner());

			System.out.println("SuiteDir=" + suiteDir);
			String[] docs = suite.getDocumentationFiles();
			for (int i = 0; i < docs.length; i++) {
				System.out.println("Doc=" + docs[i]);
				File f2 = new File(docs[i]);
				// if it is a url, download it and put it in the suiteDir now
				if (!f2.exists()) {
					String file = GenePatternAnalysisTask.downloadTask(docs[i]);
					f2 = new File(suiteDir, filenameFromURL(docs[i]));
					boolean success = GenePatternAnalysisTask.rename(new File(
							file), f2, true);
					System.out.println("Doc rename =" + success);

				} else {
					// move file to suitedir

					File f3 = new File(suiteDir, f2.getName());
					boolean success = GenePatternAnalysisTask.rename(f2, f3,
							true);
					System.out.println("Doc rename =" + success);

				}

			}

			String[] modLsids = suite.getModuleLSIDs();
			for (int i = 0; i < modLsids.length; i++) {
				installSuiteModule(lsid, modLsids[i]);
			}

			return suite.getLSID();
		} catch (Exception e) {
			e.printStackTrace();
			throw new WebServiceException("A database error occurred", e);
		} finally {
			close(null, st, c);
		}

	}

	public static String filenameFromURL(String url) {
		int idx = url.lastIndexOf("/");
		if (idx >= 0)
			return url.substring(idx + 1);
		else
			return url;
	}

	public void installSuiteModule(String lsid, String mod_lsid)
			throws WebServiceException {
		Connection c = null;
		PreparedStatement st = null;
		try {
			c = getConnection();
			st = c
					.prepareStatement("insert  into suite_modules (lsid, module_lsid) values (?, ?)");
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

	public String cloneSuite(String lsid, String cloneName)
			throws WebServiceException {
		throw new WebServiceException("Clone suite Not implemented yet");
	}

	public String modifySuite(int access_id, String lsid, String name,
			String description, String author, String owner,
			ArrayList moduleLsids, ArrayList files) throws WebServiceException {

		String newlsid = lsid;
		ArrayList docs = new ArrayList();

		if ((lsid != null) && (lsid.length() > 0)) {
			try {
				LSIDManager lsidManager = LSIDManager.getInstance();
				newlsid = lsidManager.getNextIDVersion(lsid).toString();

				LocalAdminClient adminClient = new LocalAdminClient(
						"GenePattern");

				SuiteInfo oldsi = adminClient.getSuite(lsid);
				String oldDir = DirectoryManager.getSuiteLibDir(null, lsid,
						"GenePattern");
				String[] oldDocs = oldsi.getDocumentationFiles();

				for (int i = 0; i < oldDocs.length; i++) {
					File f = new File(oldDir, oldDocs[i]);
					docs.add(f.getAbsolutePath());
				}

			} catch (Exception e) {
				e.printStackTrace();
				throw new WebServiceException(e);
			}
		} else {
			newlsid = null;
		}

		for (int i = 0; i < files.size(); i++) {
			File f = (File) files.get(i);
			docs.add(f.getAbsolutePath());
		}

		SuiteInfo si = new SuiteInfo(newlsid, name, description, author, owner,
				moduleLsids, access_id, docs);
		return installSuite(si);
	}

	
	public String modifySuite(int access_id, String lsid, String name,
			String description, String author, String owner,
			String[] moduleLsids, javax.activation.DataHandler[] dataHandlers,
			String[] fileNames) throws WebServiceException {

		String newLsid = modifySuite(access_id, lsid, name, description,
				author, owner, new ArrayList(Arrays.asList(moduleLsids)),
				new ArrayList());
		LocalAdminClient adminClient = new LocalAdminClient("GenePattern");

		SuiteInfo si = adminClient.getSuite(newLsid);
		ArrayList docFiles = new ArrayList(Arrays.asList(si.getDocFiles()));
		if (dataHandlers != null) {
			for (int i = 0; i < dataHandlers.length; i++) {
				File axisFile = Util.getAxisFile(dataHandlers[i]);
				try {
					File dir = new File(DirectoryManager.getSuiteLibDir(null,
							newLsid, "GenePattern"));
					File newFile = new File(dir, fileNames[i]);
					axisFile.renameTo(newFile);
					docFiles.add(newFile.getAbsolutePath());
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
			if (lsid != null) {
				int start = dataHandlers != null && dataHandlers.length > 0 ? dataHandlers.length - 1
						: 0;
				
				try {
					File oldLibDir = new File(DirectoryManager.getSuiteLibDir(null,
							lsid, "GenePattern"));
					for (int i = start; i < fileNames.length; i++) {
						String text = fileNames[i];
						if (oldLibDir != null && oldLibDir.exists()) { // file
																		// from
							// previous version
							// of task
							File src = new File(oldLibDir, text);
							Util.copyFile(src, new File(DirectoryManager
									.getSuiteLibDir(null, newLsid,
											"GenePattern"), text));
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}

			si.setDocFiles((String[]) docFiles.toArray(new String[0]));
		}
		return newLsid;

	}

	public SuiteInfo getSuite(String lsid) throws AdminDAOSysException {
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
		// System.out.println("GPPropsFile="+ gpPropsFilename);
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