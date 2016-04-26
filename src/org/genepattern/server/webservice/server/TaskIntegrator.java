/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webservice.server;

import static org.genepattern.util.GPConstants.ACCESS_PRIVATE;
import static org.genepattern.util.GPConstants.ACCESS_PUBLIC;
import static org.genepattern.util.GPConstants.COMMAND_LINE;
import static org.genepattern.util.GPConstants.LSID;
import static org.genepattern.util.GPConstants.PRIVACY;
import static org.genepattern.util.GPConstants.PRIVATE;
import static org.genepattern.util.GPConstants.SERIALIZED_MODEL;
import static org.genepattern.util.GPConstants.TASK_TYPE;
import static org.genepattern.util.GPConstants.TASK_TYPE_PIPELINE;
import static org.genepattern.util.GPConstants.USERID;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.apache.axis.MessageContext;
import org.apache.log4j.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.FileSet;
import org.genepattern.codegenerator.AbstractPipelineCodeGenerator;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.TaskUtil;
import org.genepattern.server.TaskUtil.ZipFileType;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.Suite;
import org.genepattern.server.domain.SuiteDAO;
import org.genepattern.server.domain.TaskMasterDAO;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.genepattern.TaskInstallationException;
import org.genepattern.server.process.InstallTask;
import org.genepattern.server.process.InstallTasksCollectionUtils;
import org.genepattern.server.process.SuiteRepository;
import org.genepattern.server.process.ZipSuite;
import org.genepattern.server.taskinstall.InstallInfo;
import org.genepattern.server.util.AuthorizationManagerFactory;
import org.genepattern.server.util.IAuthorizationManager;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.server.webservice.server.dao.TaskIntegratorDAO;
import org.genepattern.server.webservice.server.local.IAdminClient;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.util.LSIDUtil;
import org.genepattern.util.LsidVersion;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.TaskInfoCache;
import org.genepattern.webservice.WebServiceErrorMessageException;
import org.genepattern.webservice.WebServiceException;

/**
 * TaskIntegrator Web Service.
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
     * Clones the given task. Cloning is a form of task creation and requires the "createModule" permission.
     * 
     * @param lsid, The lsid of the task to clone
     * @param cloneName, The name of the cloned task
     * 
     * @return The LSID of the cloned task
     * @exception WebServiceException, If an error occurs
     */
    public String cloneTask(String oldLSID, String cloneName) throws WebServiceException {
        TaskInfo oldTaskInfo = null;
        try {
            oldTaskInfo = TaskInfoCache.instance().getTask(oldLSID);
            Integer oldTaskId = oldTaskInfo.getID();
            TaskInfoCache.instance().removeFromCache(oldTaskId);
        }
        catch (Throwable t) {
            log.error(t);
            throw new WebServiceException(t);
        }

        String userID = getUserName();
        String taskType = (String)oldTaskInfo.getAttributes().get("taskType");
        if ("pipeline".equals(taskType)){
            isAuthorized(userID, "createPipeline");
        } 
        else {
            isAuthorized(userID, "createModule");
        }

        try {
            TaskInfoAttributes tia = oldTaskInfo.giveTaskInfoAttributes();

            tia.put(USERID, userID);
            tia.put(PRIVACY, PRIVATE);
            oldLSID = (String) tia.remove(LSID);
            if (tia.get(TASK_TYPE).equals(TASK_TYPE_PIPELINE)) {
                PipelineModel model = PipelineModel.toPipelineModel((String) tia.get(SERIALIZED_MODEL));

                //for taskLib files, such as input files uploaded in the pipeline designer,
                //    replace old lsid with '<LSID>' command substitution
                // in our test-case of a pipeline exported from GP 3.3.3, the value contains a urlencoded old lsid
                //<GenePatternURL>getFile.jsp?task=urn%3Alsid%3A8080.genepatt.genepattern.broadinstitute.org%3Agenepatternmodules%3A4220%3A1&file=all_aml_test.gct
                // it should look like this instead
                //<GenePatternURL>getFile.jsp?task=<LSID>&file=all_aml_test.gct

                String encodedOldLSID;
                try {
                    encodedOldLSID = URLEncoder.encode(oldLSID, "UTF-8");
                }
                catch (Throwable t) {
                    log.error("failed to encode oldLSID as UTF-8: '"+oldLSID+"'",t);
                    encodedOldLSID = oldLSID;
                }
                for(final JobSubmission js : model.getTasks()) {
                    for(final Object piObj : js.getParameters()) {
                        final ParameterInfo pi = (ParameterInfo) piObj;
                        final String value = pi.getValue();
                        if (value.contains(oldLSID)) {
                            log.error("replacing oldLSID with '<LSID>' substitution parameter");
                            final String newValue = value.replace(oldLSID, "<LSID>");
                            pi.setValue(newValue);
                        }
                        if (value.contains(encodedOldLSID)) {
                            log.debug("replacing url encoded oldLSID with '<LSID>' substitution parameter");
                            final String newValue = value.replace(encodedOldLSID, "<LSID>");
                            pi.setValue(newValue);
                        }
                    }
                }

                // update the pipeline model with the new name
                model.setName(cloneName);
                model.setUserid(userID);

                // update the task with the new model and command line
                TaskInfoAttributes newTIA = AbstractPipelineCodeGenerator.getTaskInfoAttributes(model);
                model.setPrivacy(PRIVATE);
                tia.put(SERIALIZED_MODEL, model.toXML());
                tia.put(COMMAND_LINE, newTIA.get(COMMAND_LINE));
                tia.put(PRIVACY, PRIVATE);
            }
            String newLSID = modifyTask(ACCESS_PRIVATE, cloneName, oldTaskInfo.getDescription(), oldTaskInfo.getParameterInfoArray(), tia, null, null);
            cloneTaskLib(oldTaskInfo.getName(), cloneName, oldLSID, newLSID, userID);
            return newLSID;
        } 
        catch (Exception e) {
            log.error("cloning", e);
            throw new WebServiceException(e);
        }
    }

    /**
     * Deletes the given files that belong to the given task
     * 
     * Only owners and administrators may delete a task.
     * 
     * @param lsid
     *                The LSID
     * @param fileNames
     *                Description of the Parameter
     * @return The LSID of the new task
     * @exception WebServiceException
     *                    If an error occurs
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
     *                The LSID
     * @exception WebServiceException
     *                    If an error occurs
     */
    public void delete(String lsid) throws WebServiceException {
        if (LSIDUtil.isSuiteLSID(lsid)) {
            isSuiteOwnerOrAuthorized(getUserName(), lsid, "adminSuites");
            TaskIntegratorDAO dao = new TaskIntegratorDAO();
            dao.deleteSuite(lsid);
        } 
        else {
            deleteTask(lsid);
        }
    }

    /**
     * Deletes the given task
     * 
     * @param lsid
     *                The LSID
     * @exception WebServiceException
     *                    If an error occurs
     */
    public void deleteTask(String lsid) throws WebServiceException {
        String username = getUserName();
         
        isTaskOwnerOrAuthorized(username, lsid, "adminModules");
        if (lsid == null || lsid.equals("")) {
            throw new WebServiceException("Invalid LSID");
        }
        try {
            TaskInfo taskInfo = new LocalAdminClient(username).getTask(lsid);
            if (taskInfo == null) {
                throw new WebServiceException("no such module " + lsid);
            }
            String moduleDirectory = DirectoryManager.getTaskLibDir(taskInfo);

            GenePatternAnalysisTask.deleteTask(lsid);
            Delete del = new Delete();
            del.setDir(new File(moduleDirectory));
            del.setIncludeEmptyDirs(true);
            del.setProject(new Project());
            del.execute();
        } 
        catch (Throwable e) {
            e.printStackTrace();
            log.error(e);
            throw new WebServiceException("while deleting module " + lsid, e);
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
     *                The LSID
     * @return The zip file
     * @exception WebServiceException
     *                    If an error occurs
     */
    public DataHandler exportToZip(String lsid) throws WebServiceException {
    return exportToZip(lsid, false);
    }

    public DataHandler exportToZip(String lsid, boolean recursive) throws WebServiceException {
    try {

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
        } 
        catch (Exception e) {
            log.error(e);
            throw new WebServiceException("while getting doc filenames", e);
        }
    }

    /**
     * Gets the an array of the last modification times of the given files that belong to the given task
     * 
     * @param lsid
     *                The LSID
     * @param fileNames
     *                The fileNames
     * @return The last modification times
     * @exception WebServiceException
     *                    If an error occurs
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
     *                The LSID
     * @return The supportFileNames
     * @exception WebServiceException
     *                    If an error occurs
     */
    public String[] getSupportFileNames(String lsid) throws WebServiceException {

    if (lsid == null || lsid.equals("")) {
        throw new WebServiceException("Invalid LSID");
    }

    try {

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
     *                The LSID
     * @return The supportFiles
     * @exception WebServiceException
     *                    If an error occurs
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
     *                The LSID
     * @param fileNames
     *                The fileNames
     * @return The files
     * @exception WebServiceException
     *                    If an error occurs
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
     * @param handler, The zip file
     * @param privacy, One of GPConstants.ACCESS_PUBLIC or GPConstants.ACCESS_PRIVATE
     * @return The LSID of the task
     * @throws WebServiceException, If an error occurs
     */
    public String importZip(DataHandler handler, int privacy) throws WebServiceException {
        return importZip(handler, privacy, true, null);
    }

    private String importZip(DataHandler handler, int privacy, boolean recursive, Status status)
    throws WebServiceException 
    {
        File axisFile = Util.getAxisFile(handler);
        File zipFile = new File(handler.getName() + ".zip");
        axisFile.renameTo(zipFile);
        try {
            return importZipFromURL(zipFile.getCanonicalPath(), privacy, recursive, status);
        } 
        catch (IOException e) {
            throw new WebServiceException();
        }
    }

    /**
     * Installs the zip file at the given url overwriting anything already there.
     * 
     * @param url
     *                The url or the file path of the zip file.
     * @param privacy
     *                One of GPConstants.ACCESS_PUBLIC or GPConstants.ACCESS_PRIVATE
     * @return The LSID of the task
     * @throws WebServiceException
     *                 If an error occurs
     */
    public String importZipFromURL(String url, int privacy) throws WebServiceException {
    return importZipFromURL(url, privacy, true, null);
    }

    public String importZipFromURL(String url, int privacy, boolean recursive, Status taskIntegrator)
    throws WebServiceException {
        File zipFile = null;
        if (recursive) {
            // recursive install only allowed if createModule permission granted
            recursive = AuthorizationHelper.createModule(getUserName());
        }
        try {
            String username = getUserName();
            zipFile = Util.downloadUrl(url);

            ZipFileType type = TaskUtil.getZipFileType(zipFile);
            if (zipFile.equals(ZipFileType.INVALID_ZIP)) {
                throw new WebServiceException("Couldn't find task or suite manifest in zip file ");
            }

            if (type.equals(ZipFileType.MODULE_ZIP)) {
                isAuthorized(getUserName(), "createModule");
                final InstallInfo installInfo=new InstallInfo(InstallInfo.Type.MODULE_ZIP);
                return GenePatternAnalysisTask.installNewTask(zipFile.getCanonicalPath(), username, privacy, recursive, taskIntegrator, installInfo);
            } 
            else if (type.equals(ZipFileType.PIPELINE_ZIP)) {
                isAuthorized(getUserName(), "createPipeline");
                final InstallInfo installInfo=new InstallInfo(InstallInfo.Type.PIPELINE_ZIP);
                return GenePatternAnalysisTask.installNewTask(zipFile.getCanonicalPath(), username, privacy, recursive, taskIntegrator, installInfo);
            } 
            else if (type.equals(ZipFileType.PIPELINE_ZIP_OF_ZIPS)) {
                isAuthorized(getUserName(), "createPipeline");
                final InstallInfo installInfo=new InstallInfo(InstallInfo.Type.PIPELINE_ZIP_OF_ZIPS);
                return GenePatternAnalysisTask.installNewTask(zipFile.getCanonicalPath(), username, privacy, recursive, taskIntegrator, installInfo);
            } 
            else if (type.equals(ZipFileType.SUITE_ZIP)) {
                isAuthorized(getUserName(), "createSuite");
                return installSuite(zipFile, privacy);
            } 
            else if (type.equals(ZipFileType.SUITE_ZIP_OF_ZIPS)) {
                isAuthorized(getUserName(), "createSuite");
                return installSuiteFromZipOfZips(zipFile, recursive, privacy);
            }
        } 
        catch (TaskInstallationException tie) {
            throw new WebServiceErrorMessageException(tie.getErrors());
        } 
        catch (IOException ioe) {
            throw new WebServiceException("while importing zip from " + url, ioe);
        } 
        finally {
            if (zipFile != null) {
                zipFile.delete();
            }
        }
        return url;
    }

    /**
     * Create a new suite from the SuiteInfo object.
     * 
     * @param suiteInfo
     * @throws WebServiceException
     */
    private String saveOrUpdateSuite(SuiteInfo suiteInfo) throws WebServiceException {

    isAuthorized(getUserName(), "createSuite");

    if (suiteInfo.getLSID() != null) {
        if (suiteInfo.getLSID().trim().length() == 0)
        suiteInfo.setLSID(null);
    }

    return (new TaskIntegratorDAO()).saveOrUpdate(suiteInfo);
    }

    /**
     * Expose <code>IAuthorizationManager.checkPermission</code> to the SOAP interface.
     * Checks permissions as the current user.
     * Permissions are configured in <pre>resources/permissionMap.xml</pre>.
     * 
     * @param permission
     * @return true if the current user has the given permission.
     * 
     * @see IAuthorizationManager#checkPermission(String, String)
     */
    public boolean checkPermission(String permission) {
        IAuthorizationManager authManager = AuthorizationManagerFactory.getAuthorizationManager();
        String userName = getUserName();
        return authManager.checkPermission(permission, userName);
    }

    public int getPermittedAccessId(int access_id) {

    int access = GPConstants.ACCESS_PRIVATE;
    IAuthorizationManager authManager = AuthorizationManagerFactory.getAuthorizationManager();
    if (!authManager.checkPermission("createPublicSuite", getUserName())) {
        access = GPConstants.ACCESS_PRIVATE;
    } else {
        access = access_id;
    }
    log.debug("Perm=" + authManager.checkPermission("createPublicSuite", getUserName()));
    log.debug("TI installSuite  priv in=" + access_id + "  set to=" + access);
    return access;
    }

    /**
     * Create a new suite from the SuiteInfo object.
     * 
     * @param suiteInfo
     * @throws WebServiceException
     */
    private String installSuite(SuiteInfo suiteInfo, int privacy) throws WebServiceException {
    isAuthorized(getUserName(), "createSuite");

    try {
        if (suiteInfo.getLSID() != null) {
        if (suiteInfo.getLSID().trim().length() == 0)
            suiteInfo.setLSID(null);
        }
        suiteInfo.setAccessId(getPermittedAccessId(privacy));
        (new TaskIntegratorDAO()).saveOrUpdate(suiteInfo);

        final File suiteDir = DirectoryManager.getSuiteLibDir(suiteInfo, true);
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

    private String installSuite(File file, int privacy) throws WebServiceException {
    isAuthorized(getUserName(), "createSuite");
    ZipFile suiteZipFile = null;

    try {
        suiteZipFile = new ZipFile(file);
        SuiteInfo suiteInfo = new SuiteInfo(SuiteRepository.getSuiteMap(suiteZipFile));

        // now we need to extract the doc files and repoint the suiteInfo
        // docfiles to the file url of the extracted version
        String[] filenames = suiteInfo.getDocumentationFiles();
        if (filenames != null) {
        File tmpDir = File.createTempFile("dir", null);
        tmpDir.delete();
        tmpDir.mkdirs();

        for (int j = 0; j < filenames.length; j++) {
            String name = filenames[j];
            ZipEntry zipEntry = (ZipEntry) suiteZipFile.getEntry(name);
            if (zipEntry != null) {
            File outFile = unzip(tmpDir, suiteZipFile, zipEntry);
            filenames[j] = outFile.toURL().toString();
            }
        }
        }

        suiteZipFile.close();
        String lsid = installSuite(suiteInfo, privacy);
        return lsid;
    } catch (Exception e) {
        throw new WebServiceException(e);
    } finally {
        if (suiteZipFile != null) {
        try {
            suiteZipFile.close();
        } catch (IOException e) {

        }
        }
    }

    }

    /**
     * Install a suite from a zip of zips. Extracts the contents of the zip then calls installSuite(SuiteInfo)
     * 
     * @param zipFile
     * @return
     * @throws WebServiceException
     */
    private String installSuiteFromZipOfZips(File file, boolean recursive, int privacy) throws WebServiceException {
    isAuthorized(getUserName(), "createSuite");
    if (recursive) {
        recursive = authManager.checkPermission("createModule", getUserName());
    }
    try {

        SuiteInfo suiteInfo;
        try {
        suiteInfo = TaskUtil.getSuiteInfoFromZipOfZips(file);
        } catch (Exception e) {
        throw new WebServiceException("Invalid suite zip file.");
        }

        File suiteSuiteFile = TaskUtil.getFirstEntry(file);

        ZipFile suiteZipFile = new ZipFile(suiteSuiteFile);
        // now we need to extract the doc files and repoint the suiteInfo
        // docfiles to the file url of the extracted version
        String[] filenames = suiteInfo.getDocumentationFiles();
        if (filenames != null) {
        File tmpDir = File.createTempFile("dir", null);
        tmpDir.delete();
        tmpDir.mkdirs();
        for (int j = 0; j < filenames.length; j++) {
            String name = filenames[j];
            ZipEntry zipEntry = (ZipEntry) suiteZipFile.getEntry(name);
            if (zipEntry != null) {
            File outFile = unzip(tmpDir, suiteZipFile, zipEntry);
            filenames[j] = outFile.toURL().toString();
            }
        }
        }

        suiteZipFile.close();
        String lsid = installSuite(suiteInfo, privacy);
        if (recursive) {
        // install modules
        ZipFile zipOfZips = new ZipFile(file);
        Enumeration e = zipOfZips.entries();
        if (e.hasMoreElements()) {
            e.nextElement(); // ignore first entry which is the suite
            // zip
        }
        File tmpDir = File.createTempFile("dir", null);
        tmpDir.delete();
        tmpDir.mkdirs();
        while (e.hasMoreElements()) {
            File moduleZipFile = unzip(tmpDir, zipOfZips, (ZipEntry) e.nextElement());
            importZipFromURL(moduleZipFile.getCanonicalPath(), privacy, false, new Status() {

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
        return lsid;
    } catch (IOException ioe) {
        throw new WebServiceException(ioe);

    }
    }

    private File unzip(File unzipDirectory, ZipFile zipFile, ZipEntry zipEntry) throws IOException {
    InputStream is = null;
    FileOutputStream os = null;

    try {
        is = zipFile.getInputStream(zipEntry);
        String name = zipEntry.getName();
        File outputFile = new File(unzipDirectory, name);
        os = new FileOutputStream(outputFile);
        byte[] buf = new byte[100000];
        int bytesRead;
        while ((bytesRead = is.read(buf)) != -1) {
        os.write(buf, 0, bytesRead);
        }
        outputFile.setLastModified(zipEntry.getTime());
        return outputFile;

    } finally {
        try {
        if (is != null)
            is.close();
        } catch (IOException e) {

        }
        try {
        if (os != null)
            os.close();
        } catch (IOException e) {

        }
    }
    }

    /**
     * Installs the task with the given LSID from the module repository
     * 
     * @param lsid
     *                The task LSID
     * @throws WebServiceException
     *                 If an error occurs
     */

    public void installTask(String lsid) throws WebServiceException {
    isAuthorized(getUserName(), "createModule");
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

    /**
     * 
     * 
     * Modifies the suite with the given name. If the suite does not exist, it will be created.
     * 
     * @param accessId
     *                One of GPConstants.ACCESS_PUBLIC or GPConstants.ACCESS_PRIVATE
     * @param name
     *                The name of the suite
     * @param lsid
     *                The LSID of the suite
     * @param description
     *                The description
     * @param author
     *                The author
     * @param owner
     *                The owner
     * @param moduleLsids
     *                lsids of modules that are in this suite
     * 
     * @param files
     *                The file names for the <tt>dataHandlers</tt> array. If the array has more elements than the
     *                <tt>dataHandlers</tt> array, then the additional elements are assumed to be uploaded files for
     *                an existing task with the given lsid.
     * 
     * @return The LSID of the suite
     * @exception WebServiceException
     *                    If an error occurs
     */
    private String modifySuite(int access_id, String lsid, String name, String description, String author,
        String owner, ArrayList moduleLsids, ArrayList<File> files) throws WebServiceException {

    ArrayList<String> docs = new ArrayList<String>();

    if ((lsid != null) && (lsid.length() > 0)) {
        try {

        IAdminClient adminClient = new LocalAdminClient(getUserName());

        SuiteInfo oldsi = adminClient.getSuite(lsid);
        File oldDir = DirectoryManager.getSuiteLibDir(null, lsid, getUserName());
        String[] oldDocs = oldsi.getDocumentationFiles();

        for (int i = 0; i < oldDocs.length; i++) {
            File f = new File(oldDir, oldDocs[i]);
            docs.add(f.getAbsolutePath());
        }

        } catch (Exception e) {
        log.error(e);
        throw new WebServiceException(e);
        }
    }

    for (int i = 0; i < files.size(); i++) {
        File f = files.get(i);
        docs.add(f.getAbsolutePath());
    }

    SuiteInfo si = new SuiteInfo(lsid, name, description, author, owner, moduleLsids, access_id, docs);
    return saveOrUpdateSuite(si);
    // return installSuite(si, access_id);
    }

    /**
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
    isAuthorized(getUserName(), "createSuite");
    if (access_id == 1) {
        isAuthorized(getUserName(), "createPublicSuite");
    }
    else {
        isAuthorized(getUserName(), "createPrivateSuite");
    }
    String newLsid = modifySuite(access_id, lsid, name, description, author, owner, new ArrayList(Arrays
        .asList(moduleLsids)), new ArrayList());
    IAdminClient adminClient = new LocalAdminClient(getUserName());

    SuiteInfo si = adminClient.getSuite(newLsid);
    ArrayList docFiles = new ArrayList(Arrays.asList(si.getDocFiles()));
    if (dataHandlers != null) {
        for (int i = 0; i < dataHandlers.length; i++) {
        File axisFile = Util.getAxisFile(dataHandlers[i]);
        try {
            File dir = DirectoryManager.getSuiteLibDir(null, newLsid, getUserName());
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
            File oldLibDir = DirectoryManager.getSuiteLibDir(null, lsid, getUserName());
            for (int i = start; i < fileNames.length; i++) {
            String text = fileNames[i];
            if (oldLibDir != null && oldLibDir.exists()) { // file
                // from
                // previous version
                // of task
                File src = new File(oldLibDir, text);
                Util.copyFile(src, new File(DirectoryManager.getSuiteLibDir(null, newLsid, getUserName()),
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
     * @param accessId, One of GPConstants.ACCESS_PUBLIC or GPConstants.ACCESS_PRIVATE
     * @param taskName, The name of the task
     * @param description, The description
     * @param parameterInfoArray, The input parameters
     * @param taskAttributes, Attributes that go in the task manifest file
     * @param dataHandlers, Holds the uploaded files
     * @param fileNames, The file names for the <tt>dataHandlers</tt> array. If the array has more elements than the
     *                <tt>dataHandlers</tt> array, then the additional elements are assumed to be uploaded files for
     *                an existing task with the LSID contained in <tt>taskAttributes</tt> or the the element is of the
     *                form 'job #, filename', then the element is assumed to be an output from a job.
     * 
     * @return The LSID of the task
     * @exception WebServiceException, If an error occurs
     */
    public String modifyTask(int accessId, String taskName, String description, ParameterInfo[] parameterInfoArray, Map taskAttributes, DataHandler[] dataHandlers, String[] fileNames) 
    throws WebServiceException {
        return modifyTask(accessId, taskName, description, parameterInfoArray, taskAttributes, LsidVersion.Increment.next, dataHandlers, fileNames); 
    }
    
    public String modifyTask(final int accessId, final String taskName, final String description, 
            ParameterInfo[] parameterInfoArray, final Map taskAttributes, 
            final LsidVersion.Increment versionIncrement,
            final DataHandler[] dataHandlers, final String[] fileNames) 
    throws WebServiceException 
    {
        final String username = getUserName();
        final String taskType = (String)taskAttributes.get("taskType");
        if ("pipeline".equals(taskType)){
            isAuthorized(username, "createPipeline");
        }
        else {
            isAuthorized(username, "createModule");
        }
        if (parameterInfoArray == null) {
            parameterInfoArray = new ParameterInfo[0];
        }
        taskAttributes.put(PRIVACY, accessId);

        final String oldLSID = (String) taskAttributes.get(LSID);
        String newLSID = null;
        try {
            resetLsidAuthorityIfNecessary(taskAttributes, username);
            newLSID =  GenePatternAnalysisTask.installNewTask(
                    taskName, 
                    description, 
                    parameterInfoArray, 
                    new TaskInfoAttributes(taskAttributes), 
                    username, 
                    accessId, 
                    versionIncrement,
                    new Status() {
                        public void beginProgress(String string) {
                        }
                        public void continueProgress(int percent) {
                        }
                        public void endProgress() {
                        }
                        public void statusMessage(String message) {
                        }
                    },
                    new InstallInfo(InstallInfo.Type.EDIT)
            );
            taskAttributes.put(LSID, newLSID); 
            // update so that upon return, the LSID is the new one
            final String attachmentDir = DirectoryManager.getTaskLibDir(taskName, newLSID, username);
            final File dir = new File(attachmentDir);
            for (int i = 0, length = dataHandlers != null ? dataHandlers.length : 0; i < length; i++) {
                DataHandler dataHandler = dataHandlers[i];
                File f = Util.getAxisFile(dataHandler);
                if (f.isDirectory()) {
                    continue;
                }
                File newFile = new File(dir, fileNames[i]);
                if (!f.getParentFile().getParent().equals(dir.getParent())) {
                    f.renameTo(newFile);
                }
                else {
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
                    } 
                    else if (oldAttachmentDir != null) { 
                        // file from previous version of task
                        Util.copyFile(new File(oldAttachmentDir, text), new File(dir, text));
                    }
                }
            }
            
            if (log.isDebugEnabled()) {
                log.debug("modifyTask, from oldLSID="+oldLSID+", to newLSID="+newLSID);
            }
        } 
        catch (TaskInstallationException tie) {
            throw new WebServiceErrorMessageException(tie.getErrors());
        } 
        catch (Exception e) {
            throw new WebServiceException(e);
        }
        TaskInfoCache.instance().removeFromCache(HibernateUtil.instance(), newLSID);
        return newLSID;
    }

    /**
     * If an LSID is set, make sure that it is for the current authority, not the task's source, since it is now modified.
     * 
     * @param taskAttributes
     * @param lsid
     * @param username
     * @return
     */
    protected void resetLsidAuthorityIfNecessary(final Map taskAttributes, final String username) {
        final String lsid = (String) taskAttributes.get(LSID);
        if (lsid != null && lsid.length() > 0) {
            LSID l;
            try {
                l = new LSID(lsid);
            }
            catch (final MalformedURLException mue) {
                if (log.isDebugEnabled()) {
                    log.debug("Error initializing LSID from lsid='"+lsid+"'", mue);
                }
                return;
            }
            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            final GpContext gpContext=GpContext.getServerContext();
            final String authority=gpConfig.getLsidAuthority(gpContext);
            if (!l.getAuthority().equals(authority)) {
                if (log.isDebugEnabled()) {
                    log.debug("lsid.authority does not match, requested='" + l.getAuthority() + "', server='" + authority + "'");
                    log.debug("setting taskAttributes.LSID from '" + lsid + "' to " + "''");
                }
                // reset lsid to empty string
                taskAttributes.put(LSID, ""); 
                // change owner to current user
                String owner = (String) taskAttributes.get(USERID);
                if (owner == null) {
                    owner = "";
                }
                if (owner.length() > 0) {
                    owner = " (" + owner + ")";
                }
                owner = username + owner;
                if (log.isDebugEnabled()) {
                    log.debug("setting taskAttributes.OWNER from '" + taskAttributes.get(USERID) + "' to '" + owner+"'");
                }
                taskAttributes.put(USERID, owner);
            }
        }
    }

    protected String importZipFromURL(String url, int privacy, Status taskIntegrator) throws WebServiceException {
    return importZipFromURL(url, privacy, true, taskIntegrator);
    }

    // copy the taskLib entries to the new directory
    private void cloneTaskLib(String oldTaskName, String cloneName, String lsid, String cloneLSID, String username)
        throws Exception {
    String dir = DirectoryManager.getTaskLibDir(oldTaskName, lsid, username);
    String newDir = DirectoryManager.getTaskLibDir(cloneName, cloneLSID, username);

    // copy files in dir to newDir
    Copy copy = new Copy();
    copy.setTodir(new File(newDir));
    copy.setPreserveLastModified(true);
    copy.setVerbose(false);
    copy.setFiltering(false);
    copy.setProject(new Project());

    FileSet fileSet = new FileSet();
    fileSet.setIncludes("*/**");
    fileSet.setDir(new File(dir));

    copy.addFileset(fileSet);
    copy.execute();
    }

    protected void isAuthorized(String user, String permission) throws WebServiceException {
    if (!authManager.checkPermission(permission, user)) {
        throw new WebServiceException("You do not have permission to perfom this action: "+permission);
    }
    }

    private void isAuthorizedCreateTask(String user, TaskInfoAttributes tia) throws WebServiceException {
    if (!(authManager.checkPermission("createModule", user) || (authManager.checkPermission("createPipeline", user) && isPipeline(tia)))) {
       
        throw new WebServiceException("You do not have permission to perfom this action.");

    }
    }

    private boolean isPipeline(TaskInfoAttributes tia) {
    return tia != null && tia.get(TASK_TYPE) != null && tia.get(TASK_TYPE).endsWith("pipeline");
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

    private boolean isTaskOwner(String user, String lsid) {
        TaskMasterDAO dao = new TaskMasterDAO();
        return dao.isTaskOwner(user, lsid);
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
