package edu.mit.broad.gp.core;

import edu.mit.wi.omnigene.framework.analysis.TaskInfo;
import edu.mit.wi.omnigene.framework.analysis.TaskInfoAttributes;
import edu.mit.wi.omnigene.service.analysis.genepattern.GenePatternAnalysisTask;
import edu.mit.wi.omnigene.util.OmnigeneException;
import edu.mit.genome.util.GPConstants;
import edu.mit.genome.gp.util.LSID;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

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

	public static final String REFRESHABLE = "refreshable"; // new or updated
	public static final String YES = "yes";
	public static final String NO = "no";
	public static Vector vRefreshable = new Vector(2);
	static {
		vRefreshable.add(YES);
		vRefreshable.add(NO);
	};

	// names of columns that can be displayed to the user
	protected static final String[] COLUMNS =  { 
		GPConstants.NAME,
		GPConstants.VERSION, 
		STATE,
		REFRESHABLE,
		GPConstants.DESCRIPTION,
		GPConstants.TASK_TYPE, 
		GPConstants.AUTHOR, 
		GPConstants.QUALITY,
		GPConstants.CPU_TYPE, 
		GPConstants.OS, 
		GPConstants.LANGUAGE, 
		GPConstants.JVM_LEVEL
	};

	// titles for columns that can be displayed to the user
	protected static final String[] TITLES =  { 
		GPConstants.NAME,
		GPConstants.VERSION, 
		STATE,
		REFRESHABLE,
		GPConstants.DESCRIPTION,
		"task type", 
		GPConstants.AUTHOR, 
		GPConstants.QUALITY,
		"CPU", 
		"OS", 
		GPConstants.LANGUAGE, 
		"lang. version"
	};

	protected String userID = null;
	protected TaskInfo taskInfo = null;
	protected TaskInfoAttributes tia = null;
	protected TaskInfoAttributes module = null;
	protected String[] docFileURLs = null;
	protected String installURL = null;
	protected String siteName = "";
	protected long downloadSize = 0L;
	protected long modificationTimestamp = 0L;

	public void setExistingTaskInfo(TaskInfo taskInfo) {
		this.taskInfo = taskInfo;
		tia = taskInfo.giveTaskInfoAttributes();
	}
	
	public InstallTask(String userID, String manifestString, String[] supportFiles, String installURL, long downloadSize, long modificationTimestamp, String siteName) {
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
		
		// let the name and description be TaskInfoAttributes for the purpose of this activity
		
		// delete the parameters from the manifest
		for (int i = 1; i <= GPConstants.MAX_PARAMETERS; i++) {
		    for (Enumeration p = props.propertyNames(); p.hasMoreElements(); ) {
			name = (String)p.nextElement();
			if (name.startsWith("p" + i + "_")) {
			    props.remove(name);
			}
		    }
		}
		// everything that remains is a TaskInfoAttribute
		module = new TaskInfoAttributes(props);

	

		// figure out which support files are documentation files
		Vector vSupportFiles = new Vector();
		for (int i = 0; supportFiles != null && i < supportFiles.length; i++) {
			if (GenePatternAnalysisTask.isDocFile(supportFiles[i])) {
				vSupportFiles.add(supportFiles[i]);
			}
		}
		docFileURLs = (String[])vSupportFiles.toArray(new String[0]);
		module.put(STATE, isAlreadyInstalled() ? (isNewer() ? UPDATED : UPTODATE) : NEW);
		module.put(REFRESHABLE, isAlreadyInstalled() ? (isNewer() ? YES : NO) : YES);
	}

	// convert a column name (eg. taskType) to a human-readable form (eg. "task type")
	public static String columnNameToHRV(String columnName) throws Exception {
		if (TITLES.length != COLUMNS.length) {
			throw new Exception("length of COLUMNS doesn't match length of TITLES");
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

	// determine whether the website version of a module is newer than the currently installed one
	// TODO: handle LSID version comparison
        public boolean isNewer() {
		boolean result = true;
		if (taskInfo == null) {
			//System.out.println(getName() + " isNewer: TaskInfo doesn't exist: " + result);
			return result; // newer because it doesn't exist on this system yet
		}
		String newLSID = module.get(GPConstants.LSID) ;
		String oldLSID = tia.get(GPConstants.LSID);

		// TODO: use LSID class to compare LSIDs
		if (newLSID != null && oldLSID == null) return true;
		// BUG: assumes that all but version portion of LSID are same and that they sort alphabetically
		if (newLSID != null && oldLSID != null && oldLSID.length() > 0 && newLSID.length() > 0) {
			try {
				result = (new LSID(newLSID).compareTo(new LSID(oldLSID)) > 0);
				//System.out.println(getName() + " isNewer: LSID comparison: " + result + " for " + newLSID + " vs. " + oldLSID);
				return result;
			} catch (MalformedURLException mue) {
				System.err.println("Bad LSID: " + newLSID + " or " + oldLSID);
			}
		}
		String installedVersion = tia.get(GPConstants.VERSION);
		String newVersion = module.get(GPConstants.VERSION);
		result = (newVersion.compareTo(installedVersion) > 0);
		//System.out.println(getName() + " isNewer: version comparison: " + result + " for " + newVersion + " vs. " + installedVersion);
		return result;
	}

	// return true if each name/value pair in the attributes matches in the TaskInfoAttributes
        public boolean matchesAttributes(Map attributes) {
		for (Iterator itAttr = attributes.keySet().iterator(); itAttr.hasNext(); ) {
			String name = (String)itAttr.next();
			Object oChoices = attributes.get(name);
			if (oChoices instanceof String) {
				if (!attributes.containsKey(name) || !attributes.get(name).equals(module.get(name))) {
					return false;
				}
			} else {
				// vChoices is a Vector of possible settings.  Any one is okay
				Vector vChoices = (Vector)oChoices;
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

        public String getExternalSiteName() {
		return siteName;
	}

        public String getURL() {
		return installURL;
	}

	public String getName() {
		return module.get(GPConstants.NAME);
	}

	// Date?
        public long getModificationTimestamp() {
		return modificationTimestamp;
	}

	// return Vector of error messages when attempting to install this Module
	public Vector install(String username, int access_id) {
		String filename = null;
		Vector vProblems;
		String url = getURL();
		try {
			boolean isNew = isNewer();
			boolean wasInstalled = isAlreadyInstalled();
			filename = GenePatternAnalysisTask.downloadTask(url);
			String taskName = GenePatternAnalysisTask.getTaskNameFromZipFile(filename);
			vProblems = GenePatternAnalysisTask.installTask(filename, username, access_id);
			if (vProblems == null || vProblems.size() == 0) {
				vProblems = new Vector();
				vProblems.add((wasInstalled ? OVERWROTE : INSTALLED_NEW) + " " + taskName);
			}
		} catch (Exception e) {
			vProblems = new Vector();
			vProblems.add(FAILED + ": unable to load " + url + ": " + e.getMessage());
		} finally {
			if (filename != null) { new File(filename).delete(); }
		}
		return vProblems;
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

	public static InstallTask loadFromRepositoryAndZipFile(String taskName, String userID) throws Exception {
		String repositoryPath = REPOSITORY_ROOT + "/modules/" + taskName;
		File[] supportFiles = new File(repositoryPath).listFiles();
		if (supportFiles == null) {
			throw new Exception("No such directory " + new File(repositoryPath).getCanonicalPath());
		}
		String[] supportFileURLs = new String[supportFiles.length];
		for (int i = 0; i < supportFiles.length; i++) {
		    try {
			supportFileURLs[i] = supportFiles[i].toURI().toURL().toString();
		    } catch (MalformedURLException ignore) {}
		}

		File zip = new File("../modules/" + taskName + ".zip");
		String installURL = "";
		try {
			installURL = zip.toURI().toURL().toString();
		} catch (MalformedURLException ignore) {}
		InstallTask task = new InstallTask(userID, InstallTask.loadManifest(taskName, userID), 
						   supportFileURLs, installURL, zip.length(), 
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
			InstallTask task = loadFromRepositoryAndZipFile(taskName, userID);
			out.append(task.toString());

			HashMap match = new HashMap();
			match.put(GPConstants.TASK_TYPE, "Prediction");
			match.put(GPConstants.LANGUAGE, "Java");
			out.append("matchesAttributes(taskType=Prediction, language=Java)=" + task.matchesAttributes(match) + "\n");

			match.put(GPConstants.LANGUAGE, "Perl");
			out.append("matchesAttributes(taskType=Prediction, language=Perl)=" + task.matchesAttributes(match) + "\n");

			out.append("installing...\n");
			Vector vProblems = task.install(userID, GPConstants.ACCESS_PUBLIC);
			while (vProblems.size() > 0) {
				out.append(vProblems.elementAt(0) + "\n");
				vProblems.remove(0);
			}

		} catch (Throwable t) {
			t.printStackTrace();
		}
	    }
	    return out.toString();
	}

	// test support: given a task name, return the manifest file as a string
	protected static String loadManifest(String taskName, String userID) throws Exception {
		Properties props = new Properties();
		// NB: assumes that source repository /gp2/modules exists!!!
		props.load(new FileInputStream(new File(REPOSITORY_ROOT + "/modules/" + taskName, GPConstants.MANIFEST_FILENAME)));
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
			if (i > 0) out.append(", ");
			out.append(docFiles[i]);
		}
		out.append("\n");

		out.append("url=" + getURL() + " is " + getDownloadSize() + " bytes, created " + new Date(getModificationTimestamp()) + "\n");
		return out.toString();
	}
}
