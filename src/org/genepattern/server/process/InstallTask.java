/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.process;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.genepattern.TaskInstallationException;
import org.genepattern.server.webservice.server.Status;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

public class InstallTask {
    final static private Logger log = Logger.getLogger(InstallTask.class);

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

    //the url of the module repository
    protected URL reposURL;
    //the url of the zip file for the module
    protected String installURL = null;

    protected boolean initialInstall = false;

    protected String siteName = "";

    protected long downloadSize = 0L;

    protected long modificationTimestamp = 0L;

    protected String lsid = null;

    protected String lsidVersion = "";

    protected boolean deprecated = false;
    
    //protected String reposURL = null;

    public InstallTask(String userID, String manifestString,
            String[] supportFiles, String installURL, long downloadSize,
            long modificationTimestamp, String siteName, boolean deprecated) {
        this.userID = userID;
        this.installURL = installURL;
        this.downloadSize = downloadSize;
        this.modificationTimestamp = modificationTimestamp;
        this.siteName = siteName;
        this.deprecated = deprecated;

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

        // now that we know the module name, try to load the existing task of the same name
        LSID l = null;
        try {
            lsid = module.get(GPConstants.LSID);
            l = new LSID(lsid);
            lsidVersion = l.getVersion();
            taskInfo = GenePatternAnalysisTask.getTaskInfo(l.toStringNoVersion(), userID);
            if (taskInfo != null) {
                tia = taskInfo.giveTaskInfoAttributes();
            }
        } 
        catch (OmnigeneException oe) {
            log.error(oe);
        } 
        catch (MalformedURLException mue) {
            log.error(mue);
        }

        docFileURLs = initDocFileURLs(supportFiles);
        module.put(STATE, isAlreadyInstalled() ? (isNewer() ? UPDATED : UPTODATE) : NEW);
        module.put(REFRESHABLE, isAlreadyInstalled() ? (isNewer() ? YES : NO) : YES);
        module.put(LSID_VERSION, lsidVersion);
    }
    
    public void setReposUrl(final URL reposUrl) {
        this.reposURL=reposUrl;
    }
    
    /**
     * Get the filename from the url for the support file
     * 
     * @param supportFileUrl
     * @return
     */
    private String getSupportFilenameFromUrl(final String supportFileUrl) {
        String supportFilename="";
        int idx=supportFileUrl.lastIndexOf("/");
        ++idx; //start after that match
        if (idx>0 && supportFileUrl.length()>idx) {
            supportFilename=supportFileUrl.substring(idx);
        }
        return supportFilename;
    }

    /**
     * Get the list (can be empty) of support files for the module.
     * 
     * Note: even though this returns a list, we are only using the first item from the list
     * in the GUI.
     * 
     * @param supportFileUrls
     * @return an array of zero or more URLs to the documentation files for the module.
     */
    private String[] initDocFileURLs(final String[] supportFileUrls) {
        // figure out which support files are documentation files
        Vector vSupportFiles = new Vector();
        //check for declared license file (license=)
        final String licenseFilename=module.containsKey(GPConstants.LICENSE) ? module.get(GPConstants.LICENSE) : null;        
        //check for declared documentation file (taskDoc=)
        String taskDoc=module.containsKey(GPConstants.TASK_DOC) ? module.get(GPConstants.TASK_DOC) : null;
        if (taskDoc != null) {
            //don't allow space characters at beginning or end of taskDoc file
            taskDoc=taskDoc.trim();
        }
        if (taskDoc != null && taskDoc.length()==0) {
            //by definition, when the module declares that it has zero doc files
            //    e.g. taskDoc=
            //vSupportFiles should be an empty list
        }
        else { 
            //go through the list of support files, looking for doc files
            for(final String supportFileUrl : supportFileUrls) {
                //get the filename from the url for the support file
                String supportFilename=getSupportFilenameFromUrl(supportFileUrl);
                boolean isDocFile=false;
                boolean isLicense=supportFilename.equals(licenseFilename);
                if (isLicense) {
                    //by definition, it can't be a doc file if it's a license file, so do nothing
             
                    //Note: we can break this with a manual edit to the manifest
                    if (taskDoc != null && supportFilename.equals(taskDoc)) {
                        //tie goes to the license file
                        log.error("taskDoc= and licence= are both set to the same file: "+supportFilename+", for lsid="+lsid);
                    }
                }
                //the new way, the module has declared doc file
                else if (taskDoc != null) {
                    isDocFile=supportFilename.equals(taskDoc);
                }
                else {
                    //the old way
                    isDocFile=GenePatternAnalysisTask.isDocFile(supportFileUrl);
                } 
                boolean isVersionTxt="version.txt".equals(supportFileUrl); //I don't think this line does anything, pcarr
                if (isDocFile) {
                    vSupportFiles.add(supportFileUrl);
                }
            }
        } 
        return (String[]) vSupportFiles.toArray(new String[0]);
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
            // System.out.println(getName() + " isNewer: TaskInfo doesn't exist:
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
                // System.out.println(getName() + " isNewer: LSID comparison: "
                // + result + " for " + newLSID + " vs. " + oldLSID);
                return result;
            } catch (MalformedURLException mue) {
                System.err.println("Bad LSID: " + newLSID + " or " + oldLSID);
            }
        }
        String installedVersion = tia.get(GPConstants.VERSION);
        String newVersion = module.get(GPConstants.VERSION);
        result = (newVersion.compareTo(installedVersion) > 0);
        // System.out.println(getName() + " isNewer: version comparison: " +
        // result + " for " + newVersion + " vs. " + installedVersion);
        return result;
    }

    // return true if each name/value pair in the attributes matches in the
    // TaskInfoAttributes
    public boolean matchesAttributes(Map attributes) {
        for (Iterator itAttr = attributes.keySet().iterator(); itAttr.hasNext();) {
            String name = (String) itAttr.next();
            String value = (String) module.get(name);
            // if (value != null && value.equals(GPConstants.ANY)) return true;
            Object oChoices = attributes.get(name);
            if (oChoices instanceof String) {
                if (!attributes.containsKey(name)
                        || (!attributes.get(name).equals(value)
                                && !value.equals(GPConstants.ANY) && !value
                                .equals(""))) {
                    return false;
                }
            } else {
                // vChoices is a List of possible settings. Any one is okay
                List vChoices = (List) oChoices;
                if (!vChoices.contains(value) && !value.equals(GPConstants.ANY)
                        && !value.equals("")) {
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
    public String[] getDocUrls() {
        return docFileURLs;
    }

    public String getDocumentationUrl() {
        return docFileURLs.length > 0 ? docFileURLs[0] : "";
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

    public String getUrl() {
        return installURL + (initialInstall ? "?initialInstall=1" : "");
    }

    public String getName() {
        return module.get(GPConstants.NAME);
    }

    public String getLsid() {
        return lsid;
    }

    public String getLsidVersion() {
        return lsidVersion;
    }

    public String getRequirements() {
        StringBuffer buf = new StringBuffer();
        if (getLanguage() != null && !getLanguage().trim().equals("")) {
            buf.append(getLanguage());

            if (getLanguageLevel() != null && !getLanguageLevel().trim().equals("")) {
                buf.append(" ");
                buf.append(getLanguageLevel());

            }
            buf.append(", ");
        }

        if (getOperatingSystem() != null) {
            buf.append(getOperatingSystem() + " OS");
        } else {
            buf.append("any OS");
        }
        return buf.toString();
    }

    public String getLanguage() {
        return module.get(GPConstants.LANGUAGE);
    }

    public String getDescription() {
        return module.get(GPConstants.DESCRIPTION);
    }

    public String getLanguageLevel() {
        return module.get(GPConstants.JVM_LEVEL);
    }

    public String getOperatingSystem() {
        return module.get(GPConstants.OS);
    }

    public String getTaskType() {
        return module.get("taskType");
    }

    public String getAuthor() {
        return module.get("author");
    }

    public String getVersionComment() {
        return module.get("version");
    }

    public String getQuality() {
        return module.get("quality");
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    // Date?
    public long getModificationTimestamp() {
        return modificationTimestamp;
    }

    // return Vector of error messages when attempting to install this Module
    public boolean install(String username, int access_id, Status status)
            throws TaskInstallationException {
        String filename = null;
        Vector vProblems = new Vector();
        final String zipUrl = getUrl();
        try {
            boolean wasInstalled = isAlreadyInstalled()
                    && tia.get(GPConstants.LSID).equals(getLsid());
            filename = GenePatternAnalysisTask.downloadTask(zipUrl);
            String zipLSID = (String) GenePatternAnalysisTask
                    .getPropsFromZipFile(filename)
                    .getProperty(GPConstants.LSID);
            if (!zipLSID.equals(getLsid()))
                throw new Exception("requested LSID " + getLsid()
                        + " doesn't match actual " + zipLSID);

            String taskName = GenePatternAnalysisTask
                    .getTaskNameFromZipFile(filename);
            //lsid = GenePatternAnalysisTask.installNewTask(filename, username, access_id, status);
            lsid = GenePatternAnalysisTask.installNewTaskFromRepository(reposURL, installURL, filename, username, access_id, status);

            return wasInstalled;
        } catch (TaskInstallationException tie) {
            throw tie;
        } catch (Exception e) {
            Vector vErrors = new Vector();
            vErrors.add(FAILED + ": unable to load " + zipUrl + ": "
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
                zip.lastModified(), "Broad", false);
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
                if (task.install(userID, GPConstants.ACCESS_PUBLIC, null)) {
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

    public String toString() {
        return getLsid();
    }

    // display lots of information about the InstallTask
    public String toLongString() {

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

        String[] docFiles = getDocUrls();
        out.append("documentation: ");
        for (i = 0; i < docFiles.length; i++) {
            if (i > 0)
                out.append(", ");
            out.append(docFiles[i]);
        }
        out.append("\n");

        out.append("url=" + getUrl() + " is " + getDownloadSize()
                + " bytes, created " + new Date(getModificationTimestamp())
                + "\n");
        return out.toString();
    }
}
