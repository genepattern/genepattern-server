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

import static org.genepattern.util.GPConstants.ACCESS_PRIVATE;
import static org.genepattern.util.GPConstants.ACCESS_PUBLIC;
import static org.genepattern.util.GPConstants.COMMAND_LINE;
import static org.genepattern.util.GPConstants.LSID;
import static org.genepattern.util.GPConstants.MANIFEST_FILENAME;
import static org.genepattern.util.GPConstants.PRIVACY;
import static org.genepattern.util.GPConstants.PRIVATE;
import static org.genepattern.util.GPConstants.SERIALIZED_MODEL;
import static org.genepattern.util.GPConstants.SUITE_MANIFEST_FILENAME;
import static org.genepattern.util.GPConstants.TASK_TYPE;
import static org.genepattern.util.GPConstants.TASK_TYPE_PIPELINE;
import static org.genepattern.util.GPConstants.USERID;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.apache.axis.MessageContext;
import org.apache.log4j.Logger;
import org.genepattern.codegenerator.AbstractPipelineCodeGenerator;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.domain.Suite;
import org.genepattern.server.domain.SuiteDAO;
import org.genepattern.server.domain.TaskMaster;
import org.genepattern.server.domain.TaskMasterDAO;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.genepattern.LSIDManager;
import org.genepattern.server.genepattern.TaskInstallationException;
import org.genepattern.server.process.InstallTask;
import org.genepattern.server.process.InstallTasksCollectionUtils;
import org.genepattern.server.process.SuiteRepository;
import org.genepattern.server.process.ZipSuite;
import org.genepattern.server.util.AuthorizationManagerFactory;
import org.genepattern.server.util.IAuthorizationManager;
import org.genepattern.server.webservice.server.dao.TaskIntegratorDAO;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.LSID;
import org.genepattern.util.LSIDUtil;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.WebServiceErrorMessageException;
import org.genepattern.webservice.WebServiceException;

/**
 * TaskIntegrator Web Service. Do a Thread.yield at beginning of each method- fixes BUG in which responses from
 * AxisServlet are sometimes empty
 * 
 * @author Joshua Gould
 */

public class TaskIntegrator {

    private static Logger log = Logger.getLogger(TaskIntegrator.class);

    private IAuthorizationManager authManager = AuthorizationManagerFactory.getAuthorizationManager();

    /**
     * Retrieve the user name from the message context
     * 
     * @return
     */
    protected String getUserName() {
        MessageContext context = MessageContext.getCurrentContext();
        String username = context.getUsername();
        if (username == null) {
            username = "";
        }
        return username;
    }

    /**
     * Clones the given task.
     * 
     * Cloning is a form of task creation and requires teh "createTask" permission
     * 
     * @param lsid
     *            The lsid of the task to clone
     * @param cloneName
     *            The name of the cloned task
     * @return The LSID of the cloned task
     * @exception WebServiceException
     *                If an error occurs
     */
    public String cloneTask(String oldLSID, String cloneName) throws WebServiceException {
        isAuthorized(getUserName(), "createTask");
        String userID = getUserName();

        try {
            TaskInfo taskInfo = null;
            try {
                taskInfo = new LocalAdminClient(userID).getTask(oldLSID);
            } catch (Exception e) {
                log.error(e);
                throw new WebServiceException(e);
            }
            taskInfo.setName(cloneName);
            taskInfo.setAccessId(ACCESS_PRIVATE);
            taskInfo.setUserId(userID);
            TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
            tia.put(USERID, userID);
            tia.put(PRIVACY, PRIVATE);
            oldLSID = (String) tia.remove(LSID);
            if (tia.get(TASK_TYPE).equals(TASK_TYPE_PIPELINE)) {
                PipelineModel model = PipelineModel.toPipelineModel((String) tia.get(SERIALIZED_MODEL));

                // update the pipeline model with the new name
                model.setName(cloneName);
                model.setUserid(userID);

                // update the task with the new model and command line
                TaskInfoAttributes newTIA = AbstractPipelineCodeGenerator.getTaskInfoAttributes(model);
                tia.put(SERIALIZED_MODEL, model.toXML());
                tia.put(COMMAND_LINE, newTIA.get(COMMAND_LINE));
            }
            String newLSID = modifyTask(ACCESS_PRIVATE, cloneName, taskInfo.getDescription(), taskInfo
                    .getParameterInfoArray(), tia, null, null);
            cloneTaskLib(taskInfo.getName(), cloneName, oldLSID, newLSID, userID);
            return newLSID;
        } catch (Exception e) {
            log.error(e);
            throw new WebServiceException(e);
        }
    }

    /**
     * Deletes the given files that belong to the given task
     * 
     * Only owners and administrators may delete a task.
     * 
     * @param lsid
     *            The LSID
     * @param fileNames
     *            Description of the Parameter
     * @return The LSID of the new task
     * @exception WebServiceException
     *                If an error occurs
     */
    public String deleteFiles(String lsid, String[] fileNames) throws WebServiceException {
        isTaskOwnerOrAuthorized(getUserName(), lsid, "adminModules");
        if (lsid == null || lsid.equals("")) {
            throw new WebServiceException("Invalid LSID");
        }
        try {
            String username = getUserName();
            TaskInfo taskInfo = new LocalAdminClient(username).getTask(lsid);
            Vector lAttachments = new Vector(Arrays.asList(getSupportFileNames(lsid))); // Vector
            // of
            // String
            Vector lDataHandlers = new Vector(Arrays.asList(getSupportFiles(lsid))); // Vector
            // of
            // DataHandler
            for (int i = 0; i < fileNames.length; i++) {
                int exclude = lAttachments.indexOf(fileNames[i]);
                lAttachments.remove(exclude);
                lDataHandlers.remove(exclude);
            }
            String newLSID = modifyTask(taskInfo.getAccessId(), taskInfo.getName(), taskInfo.getDescription(), taskInfo
                    .getParameterInfoArray(), taskInfo.getTaskInfoAttributes(), (DataHandler[]) lDataHandlers
                    .toArray(new DataHandler[0]), (String[]) lAttachments.toArray(new String[0]));
            return newLSID;
        } catch (Exception e) {
            log.error(e);
            throw new WebServiceException("while deleting files from " + lsid, e);
        }
    }

    /**
     * Deletes the given task, suite or other object identified by the lsid
     * 
     * @param lsid
     *            The LSID
     * @exception WebServiceException
     *                If an error occurs
     */
    public void delete(String lsid) throws WebServiceException {
        if (LSIDUtil.isSuiteLSID(lsid)) {
            isSuiteOwnerOrAuthorized(getUserName(), lsid, "adminSuites");
            TaskIntegratorDAO dao = new TaskIntegratorDAO();
            dao.deleteSuite(lsid);

        } else {
            deleteTask(lsid);
        }
    }

    /**
     * Deletes the given task
     * 
     * @param lsid
     *            The LSID
     * @exception WebServiceException
     *                If an error occurs
     */
    public void deleteTask(String lsid) throws WebServiceException {
        isTaskOwnerOrAuthorized(getUserName(), lsid, "adminModules");
        if (lsid == null || lsid.equals("")) {
            throw new WebServiceException("Invalid LSID");
        }
        String username = getUserName();
        try {
            TaskInfo taskInfo = new LocalAdminClient(username).getTask(lsid);
            if (taskInfo == null) {
                throw new WebServiceException("no such task " + lsid);
            }
            String attachmentDir = DirectoryManager.getTaskLibDir(taskInfo);
            GenePatternAnalysisTask.deleteTask(lsid);
            File dir = new File(attachmentDir);
            // clear out the directory
            File[] oldFiles = dir.listFiles();
            for (int i = 0, length = oldFiles != null ? oldFiles.length : 0; i < length; i++) {
                oldFiles[i].delete();
            }
            dir.delete();
        } catch (Throwable e) {
            log.error(e);
            throw new WebServiceException("while deleting task " + lsid, e);
        }
    }

    public DataHandler exportSuiteToZip(String lsid) throws WebServiceException {
        try {
            ZipSuite zs = new ZipSuite();
            File zipFile = zs.packageSuite(lsid, getUserName());
            DataHandler h = new DataHandler(new FileDataSource(zipFile.getCanonicalPath()));
            return h; // FIXME delete zip file
        } catch (Exception e) {
            log.error(e);
            throw new WebServiceException(e);
        }
    }

    /**
     * Exports the given task to a zip file
     * 
     * @param lsid
     *            The LSID
     * @return The zip file
     * @exception WebServiceException
     *                If an error occurs
     */
    public DataHandler exportToZip(String lsid) throws WebServiceException {
        return exportToZip(lsid, false);
    }

    public DataHandler exportToZip(String lsid, boolean recursive) throws WebServiceException {
        try {
            Thread.yield();
            String username = getUserName();
            org.genepattern.server.process.ZipTask zt;
            if (recursive) {
                zt = new org.genepattern.server.process.ZipTaskWithDependents();
            } else {
                zt = new org.genepattern.server.process.ZipTask();
            }
            File zipFile = zt.packageTask(lsid, username);
            // FIXME delete zip file after returning
            DataHandler h = new DataHandler(new FileDataSource(zipFile.getCanonicalPath()));
            return h;
        } catch (Exception e) {
            log.error(e);
            throw new WebServiceException("while exporting to zip file", e);
        }
    }

    /**
     * Return the documentation file names associated with the LSID as an array of strings. The LSID can represent a
     * suite or a task.
     * 
     * @param lsid
     * @return
     * @throws WebServiceException
     */
    public String[] getDocFileNames(String lsid) throws WebServiceException {
         if (lsid == null || lsid.equals("")) {
            throw new WebServiceException("Invalid LSID");
        }

        try {
            Thread.yield();
            String taskLibDir = DirectoryManager.getLibDir(lsid);
            String[] docFiles = new File(taskLibDir).list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return GenePatternAnalysisTask.isDocFile(name) && !name.equals("version.txt");
                }
            });
            if (docFiles == null) {
                docFiles = new String[0];
            }
            return docFiles;
        } catch (Exception e) {
            log.error(e);
            throw new WebServiceException("while getting doc filenames", e);
        }
    }

    /**
     * Gets the files that belong to the given task or suite that are considered to be documentation files. Returned as
     * an array of DataHandlers.
     * 
     * @param lsid
     *            The LSID
     * @return The docFiles
     * @exception WebServiceException
     *                If an error occurs
     */
    public DataHandler[] getDocFiles(String lsid) throws WebServiceException {
        String taskLibDir = null;
 
        try {
            taskLibDir = DirectoryManager.getLibDir(lsid);
        } catch (Exception e) {
            log.error(e);
            throw new WebServiceException(e);
        }
        File[] docFiles = new File(taskLibDir).listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return GenePatternAnalysisTask.isDocFile(name) && !name.equals("version.txt");
            }
        });
        boolean hasDoc = docFiles != null && docFiles.length > 0;
        if (hasDoc) {
            // put version.txt last, all others alphabetically
            Arrays.sort(docFiles, new Comparator() {
                public int compare(Object o1, Object o2) {
                    if (((File) o1).getName().equals("version.txt")) {
                        return 1;
                    }
                    return ((File) o1).getName().compareToIgnoreCase(((File) o2).getName());
                }
            });
        }
        if (docFiles == null) {
            return new DataHandler[0];
        }
        DataHandler[] dh = new DataHandler[docFiles.length];
        for (int i = 0, length = docFiles.length; i < length; i++) {
            dh[i] = new DataHandler(new FileDataSource(docFiles[i]));
        }
        return dh;
    }

    /**
     * Gets the an array of the last mofification times of the given files that belong to the given task
     * 
     * @param lsid
     *            The LSID
     * @param fileNames
     *            The fileNames
     * @return The last modification times
     * @exception WebServiceException
     *                If an error occurs
     */
    public long[] getLastModificationTimes(String lsid, String[] fileNames) throws WebServiceException {
        try {
            if (lsid == null || lsid.equals("")) {
                throw new WebServiceException("Invalid LSID");
            }
            long[] modificationTimes = new long[fileNames.length];
            String attachmentDir = DirectoryManager.getTaskLibDir(null, lsid, this.getUserName());
            File dir = new File(attachmentDir);
            for (int i = 0; i < fileNames.length; i++) {
                File f = new File(dir, fileNames[i]);
                modificationTimes[i] = f.lastModified();
            }
            return modificationTimes;
        } catch (Exception e) {
            log.error(e);
            throw new WebServiceException("Error getting support files.", e);
        }
    }

    /**
     * Gets the an array of file names that belong to the given task
     * 
     * @param lsid
     *            The LSID
     * @return The supportFileNames
     * @exception WebServiceException
     *                If an error occurs
     */
    public String[] getSupportFileNames(String lsid) throws WebServiceException {

        if (lsid == null || lsid.equals("")) {
            throw new WebServiceException("Invalid LSID");
        }

        try {
            Thread.yield();
            String attachmentDir = DirectoryManager.getTaskLibDir(null, lsid, getUserName());
            File dir = new File(attachmentDir);
            String[] oldFiles = dir.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return (!name.endsWith(".old"));
                }
            });
            return oldFiles;
        } catch (Exception e) {
            log.error(e);
            throw new WebServiceException("while getting support filenames", e);
        }
    }

    /**
     * Gets the an array of files that belong to the given task
     * 
     * @param lsid
     *            The LSID
     * @return The supportFiles
     * @exception WebServiceException
     *                If an error occurs
     */
    public DataHandler[] getSupportFiles(String lsid) throws WebServiceException {
         if (lsid == null || lsid.equals("")) {
            throw new WebServiceException("Invalid LSID");
        }

        String[] files = getSupportFileNames(lsid);
        DataHandler[] dhs = new DataHandler[files.length];
        for (int i = 0; i < files.length; i++) {
            dhs[i] = getSupportFile(lsid, files[i]);
        }
        return dhs;
    }

    /**
     * Return a specific support file as a DataHandler.
     * 
     * @param lsid
     * @param fileName
     * @return
     * @throws WebServiceException
     */
    private DataHandler getSupportFile(String lsid, String fileName) throws WebServiceException {
  
        if (lsid == null || lsid.equals("")) {
            throw new WebServiceException("Invalid LSID");
        }

        try {
            Thread.yield();
            String attachmentDir = DirectoryManager.getTaskLibDir(null, lsid, this.getUserName());
            File dir = new File(attachmentDir);
            File f = new File(dir, fileName);
            if (!f.exists()) {
                throw new WebServiceException("File " + fileName + " not found.");
            }
            return new DataHandler(new FileDataSource(f));
        } catch (Exception e) {
            log.error(e);
            throw new WebServiceException("while getting support file " + fileName + " from " + lsid, e);
        }
    }

    /**
     * Gets the an array of the given files that belong to the given task
     * 
     * @param lsid
     *            The LSID
     * @param fileNames
     *            The fileNames
     * @return The files
     * @exception WebServiceException
     *                If an error occurs
     */
    public DataHandler[] getSupportFiles(String lsid, String[] fileNames) throws WebServiceException {
         try {
            if (lsid == null || lsid.equals("")) {
                throw new WebServiceException("Invalid LSID");
            }

            DataHandler[] dhs = new DataHandler[fileNames.length];
            String attachmentDir = DirectoryManager.getTaskLibDir(null, lsid, this.getUserName());
            File dir = new File(attachmentDir);
            for (int i = 0; i < fileNames.length; i++) {
                File f = new File(dir, fileNames[i]);
                if (!f.exists()) {
                    throw new WebServiceException("File " + fileNames[i] + " not found.");
                }
                dhs[i] = new DataHandler(new FileDataSource(f));
            }
            return dhs;
        } catch (Exception e) {
            log.error(e);
            throw new WebServiceException("Error getting support files.", e);
        }
    }

    /**
     * Installs the zip file overwriting anything already there.
     * 
     * @param dataHandler
     *            The zip file
     * @param privacy
     *            One of GPConstants.ACCESS_PUBLIC or GPConstants.ACCESS_PRIVATE
     * @return The LSID of the task
     * @throws WebServiceException
     *             If an error occurs
     */
    public String importZip(DataHandler handler, int privacy) throws WebServiceException {
        return importZip(handler, privacy, true, null);
    }

    private String importZip(DataHandler handler, int privacy, boolean recursive, Status taskIntegrator)
            throws WebServiceException {
        Vector vProblems = null;
        String lsid = null;
        try {
            Thread.yield();
            String username = getUserName();
            File axisFile = Util.getAxisFile(handler);
            File zipFile = new File(handler.getName() + ".zip");
            axisFile.renameTo(zipFile);
            String path = zipFile.getCanonicalPath();

            // determine if we are installing a task or a suite
            boolean isSuite = false;
            ZipFile zippedFile = null;
            try {
                zippedFile = new ZipFile(path);
                ZipEntry taskManifestEntry = zippedFile.getEntry(MANIFEST_FILENAME);
                ZipEntry suiteManifestEntry = zippedFile.getEntry(SUITE_MANIFEST_FILENAME);
                if (suiteManifestEntry != null) {
                    isSuite = true;
                }
            } catch (IOException ioe) {
                log.error(ioe);
                throw new WebServiceException("Couldn't open " + path + ": " + ioe.getMessage());
            }
            if (isSuite) {
                isAuthorized(getUserName(), "createSuite");
                lsid = installSuite(zippedFile);
            } else {
                try {
                    isAuthorized(getUserName(), "createTask");
                    lsid = GenePatternAnalysisTask.installNewTask(path, username, privacy, recursive, taskIntegrator);
                } catch (TaskInstallationException tie) {
                    log.error(tie);
                    vProblems = tie.getErrors();
                }
            }
        } catch (Exception e) {
            log.error(e);
            throw new WebServiceException("while importing from zip file", e);
        }
        if (vProblems != null && vProblems.size() > 0) {
            throw new WebServiceErrorMessageException(vProblems);
        }
        return lsid;
    }

    /**
     * Installs the zip file at the given url overwriting anything already there.
     * 
     * @param url
     *            The url of the zip file
     * @param privacy
     *            One of GPConstants.ACCESS_PUBLIC or GPConstants.ACCESS_PRIVATE
     * @return The LSID of the task
     * @throws WebServiceException
     *             If an error occurs
     */
    public String importZipFromURL(String url, int privacy) throws WebServiceException {
        return importZipFromURL(url, privacy, true, null);
    }

    public String importZipFromURL(String url, int privacy, boolean recursive, Status taskIntegrator)
            throws WebServiceException {
        File zipFile = null;
        ZipFile zippedFile;
        InputStream is = null;
        String lsid = null;
        try {
            String username = getUserName();
            zipFile = Util.downloadUrl(url);
            String path = zipFile.getCanonicalPath();
            // determine if we are installing a task or a suite
            boolean isTask = false;
            boolean isSuite = false;
            boolean isZipOfZips = false;
            try {
                zippedFile = new ZipFile(path);
                ZipEntry taskManifestEntry = zippedFile.getEntry(MANIFEST_FILENAME);
                ZipEntry suiteManifestEntry = zippedFile.getEntry(SUITE_MANIFEST_FILENAME);
                isZipOfZips = isZipOfZips(url);
                if (taskManifestEntry != null) {
                    isTask = true;
                }
                if (suiteManifestEntry != null) {
                     isSuite = true;
                }
            } catch (IOException ioe) {
                log.error(ioe);
                throw new WebServiceException("Couldn't open " + path + ": " + ioe.getMessage());
            }
            if (!(isTask || isSuite || isZipOfZips)) {
                throw new WebServiceException("Couldn't find task or suite manifest in zip file ");
            }
            if (isSuite) {
                lsid = installSuite(zippedFile);
            } else { // isTask
                // replace task, do not version lsid or replace the lsid in the
                // zip
                // with a local one
                isAuthorized(getUserName(), "createTask");
                lsid = GenePatternAnalysisTask.installNewTask(path, username, privacy, recursive, taskIntegrator);
            }
        } catch (TaskInstallationException tie) {
            throw new WebServiceErrorMessageException(tie.getErrors());
        } catch (IOException ioe) {
            throw new WebServiceException("while importing zip from " + url, ioe);
        } finally {
            if (zipFile != null) {
                zipFile.delete();
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException x) {
                }
            }
        }
        return lsid;
    }

    /**
     * Install the suite with the given LSID from the repository.
     * 
     * @param lsid
     * @throws WebServiceException
     */

    public void installSuite(String lsid) throws WebServiceException {
        isAuthorized(getUserName(), "createSuite");
        try {
            SuiteRepository sr = new SuiteRepository();
            HashMap suites = sr.getSuites(System.getProperty("SuiteRepositoryURL"));

            HashMap hm = (HashMap) suites.get(lsid);
            // get the info from the HashMap and install it into the DB
            SuiteInfo suite = new SuiteInfo(hm);

            installSuite(suite);
        } catch (Exception e) {
            log.error(e);
            throw new WebServiceException(e);
        }
    }

    /**
     * Create a new suite from the SuiteInfo object.
     * 
     * @param suiteInfo
     * @return
     * @throws WebServiceException
     */
    public void saveOrUpdateSuite(SuiteInfo suiteInfo) throws WebServiceException {

        isAuthorized(getUserName(), "createSuite");

        if (suiteInfo.getLSID() != null) {
            if (suiteInfo.getLSID().trim().length() == 0)
                suiteInfo.setLSID(null);
        }

        (new TaskIntegratorDAO()).saveOrUpdate(suiteInfo);
    }

    /**
     * Create a new suite from the SuiteInfo object.
     * 
     * @param suiteInfo
     * @return
     * @throws WebServiceException
     */
    public String installSuite(SuiteInfo suiteInfo) throws WebServiceException {
        isAuthorized(getUserName(), "createSuite");

        try {
            if (suiteInfo.getLSID() != null) {
                if (suiteInfo.getLSID().trim().length() == 0)
                    suiteInfo.setLSID(null);
            }

            (new TaskIntegratorDAO()).saveOrUpdate(suiteInfo);

            String suiteDir = DirectoryManager.getSuiteLibDir(suiteInfo.getName(), suiteInfo.getLSID(), suiteInfo
                    .getOwner());
            String[] docs = suiteInfo.getDocumentationFiles();
            for (int i = 0; i < docs.length; i++) {
                log.debug("Doc=" + docs[i]);
                File f2 = new File(docs[i]);
                // if it is a url, download it and put it in the suiteDir now
                if (!f2.exists()) {
                    String file = GenePatternAnalysisTask.downloadTask(docs[i]);
                    f2 = new File(suiteDir, filenameFromURL(docs[i]));
                    boolean success = GenePatternAnalysisTask.rename(new File(file), f2, true);
                    log.debug("Doc rename =" + success);

                } else {
                    // move file to suitedir
                    File f3 = new File(suiteDir, f2.getName());
                    boolean success = GenePatternAnalysisTask.rename(f2, f3, true);
                    log.debug("Doc rename =" + success);

                }
            }

            return suiteInfo.getLSID();
        } catch (Exception e) {
            log.error(e);
            throw new WebServiceException(e);
        }

    }

    /**
     * Install a suite from a zip file. Extracts the contents of the zip then calls installSuite(SuiteInfo)
     * 
     * @param zipFile
     * @return
     * @throws WebServiceException
     */
    public String installSuite(ZipFile zipFile) throws WebServiceException {
        isAuthorized(getUserName(), "createSuite");
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
                    File outFile = new File(System.getProperty("java.io.tmpdir"), zipEntry.getName());
                    FileOutputStream os = new FileOutputStream(outFile);
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
            log.error(e);
            throw new WebServiceException(e);
        }
    }

    /**
     * Installs the task with the given LSID from the module repository
     * 
     * @param lsid
     *            The task LSID
     * @throws WebServiceException
     *             If an error occurs
     */

    public void installTask(String lsid) throws WebServiceException {
        isAuthorized(getUserName(), "createTask");
        InstallTasksCollectionUtils utils = new InstallTasksCollectionUtils(getUserName(), false);
        try {
            InstallTask[] tasks = utils.getAvailableModules();
            for (int i = 0; i < tasks.length; i++) {
                if (tasks[i].getLsid().equalsIgnoreCase(lsid)) {
                    tasks[i].install(getUserName(), ACCESS_PUBLIC, new Status() {

                        public void beginProgress(String string) {
                        }

                        public void continueProgress(int percent) {
                        }

                        public void endProgress() {
                        }

                        public void statusMessage(String message) {
                        }

                    });
                }
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

    private boolean isZipOfZips(String url) throws WebServiceException {
        File file = Util.downloadUrl(url);
        try {
            return org.genepattern.server.TaskUtil.isZipOfZips(file);
        } catch (java.io.IOException ioe) {
            throw new WebServiceException(ioe);
        }
    }

    /**
     * @deprecated This method is not currently used, and has not been tested for GP 3.0 and greater.
     * 
     * Modifies the suite with the given name. If the suite does not exist, it will be created.
     * 
     * @param accessId
     *            One of GPConstants.ACCESS_PUBLIC or GPConstants.ACCESS_PRIVATE
     * @param name
     *            The name of the suite
     * @param lsid
     *            The LSID of the suite
     * @param description
     *            The description
     * @param author
     *            The author
     * @param owner
     *            The owner
     * @param moduleLsids
     *            lsids of modules that are in this suite
     * 
     * @param files
     *            The file names for the <tt>dataHandlers</tt> array. If the array has more elements than the
     *            <tt>dataHandlers</tt> array, then the additional elements are assumed to be uploaded files for an
     *            existing task with the given lsid.
     * 
     * @return The LSID of the suite
     * @exception WebServiceException
     *                If an error occurs
     */
    public String modifySuite(int access_id, String lsid, String name, String description, String author, String owner,
            ArrayList moduleLsids, ArrayList<File> files) throws WebServiceException {

        String newlsid = lsid;
        ArrayList<String> docs = new ArrayList<String>();

        if ((lsid != null) && (lsid.length() > 0)) {
            try {
                LSIDManager lsidManager = LSIDManager.getInstance();
                newlsid = lsidManager.getNextIDVersion(lsid).toString();

                LocalAdminClient adminClient = new LocalAdminClient("GenePattern");

                SuiteInfo oldsi = adminClient.getSuite(lsid);
                String oldDir = DirectoryManager.getSuiteLibDir(null, lsid, "GenePattern");
                String[] oldDocs = oldsi.getDocumentationFiles();

                for (int i = 0; i < oldDocs.length; i++) {
                    File f = new File(oldDir, oldDocs[i]);
                    docs.add(f.getAbsolutePath());
                }

            } catch (Exception e) {
                log.error(e);
                throw new WebServiceException(e);
            }
        } else {
            newlsid = null;
        }

        for (int i = 0; i < files.size(); i++) {
            File f = files.get(i);
            docs.add(f.getAbsolutePath());
        }

        SuiteInfo si = new SuiteInfo(newlsid, name, description, author, owner, moduleLsids, access_id, docs);
        return installSuite(si);
    }

    /**
     * @deprecated This method is not currently used and has not been tested with GP 3.0.
     * 
     * @param access_id
     * @param lsid
     * @param name
     * @param description
     * @param author
     * @param owner
     * @param moduleLsids
     * @param dataHandlers
     * @param fileNames
     * @return
     * @throws WebServiceException
     */
    public String modifySuite(int access_id, String lsid, String name, String description, String author, String owner,
            String[] moduleLsids, javax.activation.DataHandler[] dataHandlers, String[] fileNames)
            throws WebServiceException {

        String newLsid = modifySuite(access_id, lsid, name, description, author, owner, new ArrayList(Arrays
                .asList(moduleLsids)), new ArrayList());
        LocalAdminClient adminClient = new LocalAdminClient("GenePattern");

        SuiteInfo si = adminClient.getSuite(newLsid);
        ArrayList docFiles = new ArrayList(Arrays.asList(si.getDocFiles()));
        if (dataHandlers != null) {
            for (int i = 0; i < dataHandlers.length; i++) {
                File axisFile = Util.getAxisFile(dataHandlers[i]);
                try {
                    File dir = new File(DirectoryManager.getSuiteLibDir(null, newLsid, "GenePattern"));
                    File newFile = new File(dir, fileNames[i]);
                    axisFile.renameTo(newFile);
                    docFiles.add(newFile.getAbsolutePath());
                } catch (Exception e) {
                    log.error(e);
                }

            }
            if (lsid != null) {
                int start = dataHandlers != null && dataHandlers.length > 0 ? dataHandlers.length - 1 : 0;

                try {
                    File oldLibDir = new File(DirectoryManager.getSuiteLibDir(null, lsid, "GenePattern"));
                    for (int i = start; i < fileNames.length; i++) {
                        String text = fileNames[i];
                        if (oldLibDir != null && oldLibDir.exists()) { // file
                            // from
                            // previous version
                            // of task
                            File src = new File(oldLibDir, text);
                            Util.copyFile(src, new File(DirectoryManager.getSuiteLibDir(null, newLsid, "GenePattern"),
                                    text));
                        }
                    }
                } catch (Exception e) {
                    log.error(e);
                }

            }

            si.setDocFiles((String[]) docFiles.toArray(new String[0]));
        }
        return newLsid;

    }

    /**
     * Modifies the task with the given name. If the task does not exist, it will be created.
     * 
     * @param accessId
     *            One of GPConstants.ACCESS_PUBLIC or GPConstants.ACCESS_PRIVATE
     * @param taskName
     *            The name of the task
     * @param description
     *            The description
     * @param parameterInfoArray
     *            The input parameters
     * @param taskAttributes
     *            Attributes that go in the task manifest file
     * @param dataHandlers
     *            Holds the uploaded files
     * @param fileNames
     *            The file names for the <tt>dataHandlers</tt> array. If the array has more elements than the
     *            <tt>dataHandlers</tt> array, then the additional elements are assumed to be uploaded files for an
     *            existing task with the LSID contained in <tt>taskAttributes</tt> or the the element is of the form
     *            'job #, filename', then the element is assumed to be an output from a job.
     * 
     * @return The LSID of the task
     * @exception WebServiceException
     *                If an error occurs
     */
    public String modifyTask(int accessId, String taskName, String description, ParameterInfo[] parameterInfoArray,
            Map taskAttributes, DataHandler[] dataHandlers, String[] fileNames) throws WebServiceException {

        isAuthorized(getUserName(), "createTask");

        String lsid = null;
        String username = getUserName();
        String oldLSID = null;
        try {
            Thread.yield();
            if (taskAttributes == null) {
                taskAttributes = new HashMap();
            }
            if (parameterInfoArray == null) {
                parameterInfoArray = new ParameterInfo[0];
            }
            lsid = (String) taskAttributes.get(LSID);
            oldLSID = lsid;
            // if an LSID is set, make sure that it is for the current
            // authority, not the task's source, since it is now modified
            if (lsid != null && lsid.length() > 0) {
                try {
                    LSID l = new LSID(lsid);
                    String authority = LSIDManager.getInstance().getAuthority();
                    if (!l.getAuthority().equals(authority)) {
                        System.out.println("TaskIntegrator.modifyTask: resetting authority from " + l.getAuthority()
                                + " to " + authority);
                        lsid = "";
                        taskAttributes.put(LSID, lsid);
                        // change owner to current user
                        String owner = (String) taskAttributes.get(USERID);
                        if (owner == null) {
                            owner = "";
                        }
                        if (owner.length() > 0) {
                            owner = " (" + owner + ")";
                        }
                        owner = username + owner;
                        taskAttributes.put(USERID, owner);
                    }
                } catch (MalformedURLException mue) {
                }
            }
            lsid = GenePatternAnalysisTask.installNewTask(taskName, description, parameterInfoArray,
                    new TaskInfoAttributes(taskAttributes), username, accessId, new Status() {

                        public void beginProgress(String string) {
                        }

                        public void continueProgress(int percent) {
                        }

                        public void endProgress() {
                        }

                        public void statusMessage(String message) {
                        }

                    });
            taskAttributes.put(LSID, lsid); // update so that upon
            // return, the LSID is
            // the new one
            String attachmentDir = DirectoryManager.getTaskLibDir(taskName, lsid, username);
            File dir = new File(attachmentDir);
            for (int i = 0, length = dataHandlers != null ? dataHandlers.length : 0; i < length; i++) {
                DataHandler dataHandler = dataHandlers[i];
                File f = Util.getAxisFile(dataHandler);
                if (f.isDirectory()) {
                    continue;
                }
                File newFile = new File(dir, fileNames[i]);
                if (!f.getParentFile().getParent().equals(dir.getParent())) {
                    f.renameTo(newFile);
                } else {
                    // copy file, leaving original intact
                    Util.copyFile(f, newFile);
                }
            }
            if (fileNames != null) {
                String oldAttachmentDir = null;
                if (oldLSID != null) {
                    oldAttachmentDir = DirectoryManager.getTaskLibDir(null, oldLSID, username);
                }
                int start = dataHandlers != null && dataHandlers.length > 0 ? dataHandlers.length - 1 : 0;
                for (int i = start; i < fileNames.length; i++) {
                    String text = fileNames[i];
                    if (text.startsWith("job #")) { // job output file
                        String jobNumber = text.substring(text.indexOf("#") + 1, text.indexOf(",")).trim();
                        String fileName = text.substring(text.indexOf(",") + 1, text.length()).trim();
                        String jobDir = GenePatternAnalysisTask.getJobDir(jobNumber);
                        Util.copyFile(new File(jobDir, fileName), new File(dir, fileName));
                    } else if (oldAttachmentDir != null) { // file from
                        // previous version
                        // of task
                        Util.copyFile(new File(oldAttachmentDir, text), new File(dir, text));
                    }
                }
            }
            if (System.getProperty("save.task.plugin") != null) {
                final String lsid1 = lsid;
                new Thread() {
                    public void run() {
                        try {
                            DataHandler handler = exportToZip(lsid1);
                            File tempFile = File.createTempFile("task", "zip");
                            FileOutputStream fis = new FileOutputStream(tempFile);
                            handler.writeTo(fis);
                            SaveTaskPlugin saveTaskPlugin = (SaveTaskPlugin) Class.forName(
                                    System.getProperty("save.task.plugin")).newInstance();
                            saveTaskPlugin.taskSaved(tempFile);
                            fis.close();
                            tempFile.delete();
                        } catch (Exception x) {
                        }
                    }
                }.start();
            }
        } catch (TaskInstallationException tie) {
            throw new WebServiceErrorMessageException(tie.getErrors());
        } catch (Exception e) {
            throw new WebServiceException(e);
        }
        return lsid;
    }

    protected String importZipFromURL(String url, int privacy, Status taskIntegrator) throws WebServiceException {
        return importZipFromURL(url, privacy, true, taskIntegrator);
    }

    // copy the taskLib entries to the new directory
    private void cloneTaskLib(String oldTaskName, String cloneName, String lsid, String cloneLSID, String username)
            throws Exception {
        String dir = DirectoryManager.getTaskLibDir(oldTaskName, lsid, username);
        String newDir = DirectoryManager.getTaskLibDir(cloneName, cloneLSID, username);

        String[] oldFiles = getSupportFileNames(lsid);
        byte[] buf = new byte[100000];
        int j;
        for (int i = 0; i < oldFiles.length; i++) {
            FileOutputStream os = new FileOutputStream(new File(newDir, oldFiles[i]));
            FileInputStream is = new FileInputStream(new File(dir, oldFiles[i]));
            while ((j = is.read(buf, 0, buf.length)) > 0) {
                os.write(buf, 0, j);
            }
            is.close();
            os.close();
        }
    }

    protected void isAuthorized(String user, String permission) throws WebServiceException {
        if (!authManager.checkPermission(permission, user)) {
            throw new WebServiceException("You do not have permission to perfom this action.");
        }
    }

    private boolean isSuiteOwner(String user, String lsid) {

        Suite aSuite = (new SuiteDAO()).findById(lsid);
        String owner = aSuite.getUserId();
        return owner.equals(getUserName());

    }

    private void isSuiteOwnerOrAuthorized(String user, String lsid, String method) throws WebServiceException {
        if (!isSuiteOwner(user, lsid)) {
            isAuthorized(user, method);
        }
    }

    private boolean isTaskOwner(String user, String lsid) throws WebServiceException {
        TaskMaster tm = (new TaskMasterDAO()).findByIdLsid(lsid);
        if (tm == null)
            return false; // can't own what you can't see
        return user.equals(tm.getUserId());
    }

    private void isTaskOwnerOrAuthorized(String user, String lsid, String permission) throws WebServiceException {

        if (!isTaskOwner(user, lsid)) {
            isAuthorized(user, permission);
        }
    }

    public static String filenameFromURL(String url) {
        int idx = url.lastIndexOf("/");
        if (idx >= 0)
            return url.substring(idx + 1);
        else
            return url;
    }
}