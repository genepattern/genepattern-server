package org.genepattern.server.process;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.genepattern.TaskInstallationException;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

public class InstallTask {

	// DEBUG settings:
	protected static final String REPOSITORY_ROOT = "/gp2";

	// install() return values
	public static final String INSTALLED_NEW = "installed";

	public static final String OVERWROTE = "overwrote";

	public static final String FAILED = "failed";

	public static final String STATE = "state"; // new, updated, up to date

	public static final String NEW = "new";

	public static final String UPDATED = "updated";

	public static final String UPTODATE = "up to date";

	public static Vector vStates = new Vector(3);
	static {
		vStates.add(NEW);
		vStates.add(UPDATED);
		vStates.add(UPTODATE);
	};

	public static final String LSID_VERSION = "lsid_version";

	public static final String REFRESHABLE = "refreshable"; // new or updated

	public static final String YES = "yes";

	public static final String NO = "no";

	public static Vector vRefreshable = new Vector(2);
	static {
		vRefreshable.add(YES);
		vRefreshable.add(NO);
	};

	// names of columns that can be displayed to the user
	protected static final String[] COLUMNS = { GPConstants.NAME,
			GPConstants.VERSION, STATE, REFRESHABLE, GPConstants.DESCRIPTION,
			GPConstants.TASK_TYPE, GPConstants.AUTHOR, GPConstants.QUALITY,
			GPConstants.CPU_TYPE, GPConstants.OS, GPConstants.LANGUAGE,
			GPConstants.JVM_LEVEL, LSID_VERSION };

	// titles for columns that can be displayed to the user
	protected static final String[] TITLES = { GPConstants.NAME,
			GPConstants.VERSION, STATE, REFRESHABLE, GPConstants.DESCRIPTION,
			"task type", GPConstants.AUTHOR, GPConstants.QUALITY, "CPU", "OS",
			GPConstants.LANGUAGE, "lang. version", "ver" };

	protected String userID = null;

	protected TaskInfo taskInfo = null;

	protected TaskInfoAttributes tia = null;

	protected TaskInfoAttributes module = null;

	protected String[] docFileURLs = null;

	protected String installURL = null;

	protected boolean initialInstall = false;

	protected String siteName = "";

	protected long downloadSize = 0L;

	protected long modificationTimestamp = 0L;

	protected String lsid = null;

	protected String lsidVersion = "";

	public InstallTask(String userID, String manifestString,
			String[] supportFiles, String installURL, long downloadSize,
			long modificationTimestamp, String siteName) {
		this.userID = userID;
		this.installURL = installURL;
		this.downloadSize = downloadSize;
		this.modificationTimestamp = modificationTimestamp;
		this.siteName = siteName;

		String name;
		String value;
		Properties props = new Properties();
		try {
			props.load(new ByteArrayInputStream(manifestString.getBytes()));
		} catch (IOException e) {
			e.printStackTrace();
		}

		// let the name and description be TaskInfoAttributes for the purpose of
		// this activity

		// delete the parameters from the manifest
		for (int i = 1; i <= GPConstants.MAX_PARAMETERS; i++) {
			for (Enumeration p = props.propertyNames(); p.hasMoreElements();) {
				name = (String) p.nextElement();
				if (name.startsWith("p" + i + "_")) {
					props.remove(name);
				}
			}
		}
		// everything that remains is a TaskInfoAttribute
		module = new TaskInfoAttributes(props);

		// now that we know the module name, try to load the existing task of
		// the same name
		LSID l = null;
		try {
			lsid = module.get(GPConstants.LSID);
			l = new LSID(lsid);
			lsidVersion = l.getVersion();
			taskInfo = GenePatternAnalysisTask.getTaskInfo(l
					.toStringNoVersion(), userID);
			if (taskInfo != null) {
				tia = taskInfo.giveTaskInfoAttributes();
			}
		} catch (OmnigeneException oe) {
		} catch (MalformedURLException mue) {
		}

		// figure out which support files are documentation files
		Vector vSupportFiles = new Vector();
		for (int i = 0; supportFiles != null && i < supportFiles.length; i++) {
			if (GenePatternAnalysisTask.isDocFile(supportFiles[i])) {
				vSupportFiles.add(supportFiles[i]);
			}
		}
		docFileURLs = (String[]) vSupportFiles.toArray(new String[0]);
		module.put(STATE, isAlreadyInstalled() ? (isNewer() ? UPDATED
				: UPTODATE) : NEW);
		module.put(REFRESHABLE, isAlreadyInstalled() ? (isNewer() ? YES : NO)
				: YES);
		module.put(LSID_VERSION, lsidVersion);
	}

	// convert a column name (eg. taskType) to a human-readable form (eg. "task
	// type")
	public static String columnNameToHRV(String columnName) throws Exception {
		if (TITLES.length != COLUMNS.length) {
			throw new Exception(
					"length of COLUMNS doesn't match length of TITLES");
		}
		for (int i = 0; i < TITLES.length; i++) {
			if (COLUMNS[i].equals(columnName)) {
				return TITLES[i];
			}
		}
		return columnName;
	}

	// is this module already installed (without regard to version)?
	public boolean isAlreadyInstalled() {
		return (taskInfo != null);
	}

	// determine whether the website version of a module is newer than the
	// currently installed one
	// TODO: handle LSID version comparison
	public boolean isNewer() {
		boolean result = true;
		if (taskInfo == null) {
			//System.out.println(getName() + " isNewer: TaskInfo doesn't exist:
			// " + result);
			return result; // newer because it doesn't exist on this system yet
		}
		String newLSID = module.get(GPConstants.LSID);
		String oldLSID = tia.get(GPConstants.LSID);

		// TODO: use LSID class to compare LSIDs
		if (newLSID != null && oldLSID == null)
			return true;

		if (newLSID != null && oldLSID != null && oldLSID.length() > 0
				&& newLSID.length() > 0) {
			try {
				LSID l1 = new LSID(newLSID);
				LSID l2 = new LSID(oldLSID);
				if (!l1.isSimilar(l2)) {
					// different authority, namespace,or identifier
					result = (l1.compareTo(l2) > 0);
				} else {
					// only different version number
					result = (l1.getVersion().compareTo(l2.getVersion()) > 0);
				}
				//System.out.println(getName() + " isNewer: LSID comparison: "
				// + result + " for " + newLSID + " vs. " + oldLSID);
				return result;
			} catch (MalformedURLException mue) {
				System.err.println("Bad LSID: " + newLSID + " or " + oldLSID);
			}
		}
		String installedVersion = tia.get(GPConstants.VERSION);
		String newVersion = module.get(GPConstants.VERSION);
		result = (newVersion.compareTo(installedVersion) > 0);
		//System.out.println(getName() + " isNewer: version comparison: " +
		// result + " for " + newVersion + " vs. " + installedVersion);
		return result;
	}

	// return true if each name/value pair in the attributes matches in the
	// TaskInfoAttributes
	public boolean matchesAttributes(Map attributes) {
		for (Iterator itAttr = attributes.keySet().iterator(); itAttr.hasNext();) {
			String name = (String) itAttr.next();
			Object oChoices = attributes.get(name);
			if (oChoices instanceof String) {
				if (!attributes.containsKey(name)
						|| !attributes.get(name).equals(module.get(name))) {
					return false;
				}
			} else {
				// vChoices is a Vector of possible settings. Any one is okay
				Vector vChoices = (Vector) oChoices;
				if (!vChoices.contains(module.get(name))) {
					return false;
				}
			}
		}
		return true;
	}

	// return all displayable attribute names
	public static String[] getAttributeNames() {
		return COLUMNS;
	}

	public static String[] getTitles() {
		return TITLES;
	}

	// return all displayable attributes
	public Map getAttributes() {
		HashMap hmAttributes = new HashMap(COLUMNS.length + 2);
		hmAttributes.putAll(module);
		return hmAttributes;
	}

	// returns a (possibly zero-length) array of documentation URLs
	public String[] getDocURLs() {
		return docFileURLs;
	}

	public long getDownloadSize() {
		return downloadSize;
	}

	public void setInitialInstall(boolean initialInstall) {
		this.initialInstall = initialInstall;
	}

	public String getExternalSiteName() {
		return siteName;
	}

	public String getURL() {
		return installURL + (initialInstall ? "?initialInstall=1" : "");
	}

	public String getName() {
		return module.get(GPConstants.NAME);
	}

	public String getLSID() {
		return lsid;
	}

	public String getLSIDVersion() {
		return lsidVersion;
	}

	// Date?
	public long getModificationTimestamp() {
		return modificationTimestamp;
	}

	// return Vector of error messages when attempting to install this Module
	public boolean install(String username, int access_id)
			throws TaskInstallationException {
		String filename = null;
		Vector vProblems = new Vector();
		String url = getURL();
		try {
			boolean wasInstalled = isAlreadyInstalled()
					&& tia.get(GPConstants.LSID).equals(getLSID());
			filename = GenePatternAnalysisTask.downloadTask(url);
			String zipLSID = (String) GenePatternAnalysisTask
					.getPropsFromZipFile(filename)
					.getProperty(GPConstants.LSID);
			if (!zipLSID.equals(getLSID()))
				throw new Exception("requested LSID " + getLSID()
						+ " doesn't match actual " + zipLSID);
			String taskName = GenePatternAnalysisTask
					.getTaskNameFromZipFile(filename);
			lsid = GenePatternAnalysisTask.installNewTask(filename, username,
					access_id);
			return wasInstalled;
		} catch (TaskInstallationException tie) {
			throw tie;
		} catch (Exception e) {
			Vector vErrors = new Vector();
			vErrors.add(FAILED + ": unable to load " + url + ": "
					+ e.getMessage());
			throw new TaskInstallationException(vErrors);
		} finally {
			if (filename != null) {
				new File(filename).delete();
			}
		}
	}

	public Map getInstalledTaskInfoAttributes() {
		return tia;
	}

	/**
	 * everything below here is strictly for testing purposes
	 */

	public static void main(String[] args) {
		System.out.println(test(new String[] { "KNN" }));
		System.exit(0);
	}

	public static InstallTask loadFromRepositoryAndZipFile(String taskName,
			String userID) throws Exception {
		String repositoryPath = REPOSITORY_ROOT + "/modules/" + taskName;
		File[] supportFiles = new File(repositoryPath).listFiles();
		if (supportFiles == null) {
			throw new Exception("No such directory "
					+ new File(repositoryPath).getCanonicalPath());
		}
		String[] supportFileURLs = new String[supportFiles.length];
		for (int i = 0; i < supportFiles.length; i++) {
			try {
				supportFileURLs[i] = supportFiles[i].toURI().toURL().toString();
			} catch (MalformedURLException ignore) {
			}
		}

		File zip = new File("../modules/" + taskName + ".zip");
		String installURL = "";
		try {
			installURL = zip.toURI().toURL().toString();
		} catch (MalformedURLException ignore) {
		}
		InstallTask task = new InstallTask(userID, InstallTask.loadManifest(
				taskName, userID), supportFileURLs, installURL, zip.length(),
				zip.lastModified(), "Broad");
		return task;
	}

	public static String test(String[] args) {
		StringBuffer out = new StringBuffer();
		String userID = "GenePattern";
		int i;

		for (int arg = 0; arg < args.length; arg++) {
			String taskName = args[arg];
			try {
				InstallTask task = loadFromRepositoryAndZipFile(taskName,
						userID);
				out.append(task.toString());

				HashMap match = new HashMap();
				match.put(GPConstants.TASK_TYPE, "Prediction");
				match.put(GPConstants.LANGUAGE, "Java");
				out
						.append("matchesAttributes(taskType=Prediction, language=Java)="
								+ task.matchesAttributes(match) + "\n");

				match.put(GPConstants.LANGUAGE, "Perl");
				out
						.append("matchesAttributes(taskType=Prediction, language=Perl)="
								+ task.matchesAttributes(match) + "\n");

				out.append("installing...\n");
				if (task.install(userID, GPConstants.ACCESS_PUBLIC)) {
					out.append("overwrote");
				} else {
					out.append("installed");
				}
				out.append(" " + taskName + "\n");
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		return out.toString();
	}

	// test support: given a task name, return the manifest file as a string
	protected static String loadManifest(String taskName, String userID)
			throws Exception {
		Properties props = new Properties();
		// NB: assumes that source repository /gp2/modules exists!!!
		props.load(new FileInputStream(new File(REPOSITORY_ROOT + "/modules/"
				+ taskName, GPConstants.MANIFEST_FILENAME)));
		ByteArrayOutputStream manifestData = new ByteArrayOutputStream(10000);
		props.store(manifestData, taskName); // write properties to stream
		return manifestData.toString();
	}

	// display lots of information about the InstallTask
	public String toString() {
		StringBuffer out = new StringBuffer();
		String[] attributeNames = getAttributeNames();
		int i;
		Map attributes = getAttributes();
		for (i = 0; i < attributeNames.length; i++) {
			out.append(attributeNames[i]);
			try {
				out.append(" (" + columnNameToHRV(attributeNames[i]) + ")");
			} catch (Exception e) {
				e.printStackTrace();
			}
			out.append("=" + attributes.get(attributeNames[i]));
			out.append("\n");
		}
		out.append("isAlreadyInstalled=" + isAlreadyInstalled() + "\n");
		out.append("isNewer=" + isNewer() + "\n");

		String[] docFiles = getDocURLs();
		out.append("documentation: ");
		for (i = 0; i < docFiles.length; i++) {
			if (i > 0)
				out.append(", ");
			out.append(docFiles[i]);
		}
		out.append("\n");

		out.append("url=" + getURL() + " is " + getDownloadSize()
				+ " bytes, created " + new Date(getModificationTimestamp())
				+ "\n");
		return out.toString();
	}
}