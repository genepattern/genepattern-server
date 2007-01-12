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
import org.genepattern.server.util.AuthorizationManagerFactoryImpl;
import org.genepattern.server.util.IAuthorizationManager;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.server.webservice.server.dao.TaskIntegratorDAO;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.util.LSIDUtil;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.WebServiceErrorMessageException;
import org.genepattern.webservice.WebServiceException;
import org.hibernate.Transaction;


import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * TaskIntegrator Web Service. Do a Thread.yield at beginning of each method-
 * fixes BUG in which responses from AxisServlet are sometimes empty
 * 
 * @author Joshua Gould
 */

public class TaskIntegrator implements ITaskIntegrator {

    private static Logger log = Logger.getLogger(TaskIntegrator.class);
    IAuthorizationManager authManager  = (new AuthorizationManagerFactoryImpl()).getAuthorizationManager();

    protected String getUserName() {
        MessageContext context = MessageContext.getCurrentContext();
        String username = context.getUsername();
        if (username == null) {
            username = "";
        }
        return username;
    }

    public DataHandler exportToZip(String taskName) throws WebServiceException {
    	isAuthorized(getUserName(),"TaskIntegrator.exportToZip");
        return exportToZip(taskName, false);
    }

    public DataHandler exportSuiteToZip(String lsid) throws WebServiceException {
    	isAuthorized(getUserName(),"TaskIntegrator.exportSuiteToZip");
        
    	try {
            ZipSuite zs = new ZipSuite();
            File zipFile = zs.packageSuite(lsid, getUserName());
            DataHandler h = new DataHandler(new FileDataSource(zipFile.getCanonicalPath()));
            return h; // FIXME delete zip file
        }
        catch (Exception e) {
            throw new WebServiceException(e);
        }
    }

    public DataHandler exportToZip(String taskName, boolean recursive) throws WebServiceException {
       	isAuthorized(getUserName(),"TaskIntegrator.exportToZip");
 
    	try {
            Thread.yield();
            String username = getUserName();
            org.genepattern.server.process.ZipTask zt;
            if (recursive) {
                zt = new org.genepattern.server.process.ZipTaskWithDependents();
            }
            else {
                zt = new org.genepattern.server.process.ZipTask();
            }
            File zipFile = zt.packageTask(taskName, username);
            // FIXME delete zip file after returning
            DataHandler h = new DataHandler(new FileDataSource(zipFile.getCanonicalPath()));
            return h;
        }
        catch (Exception e) {
            throw new WebServiceException("while exporting to zip file", e);
        }
    }

    public String importZip(DataHandler handler, int privacy) throws WebServiceException {
       	isAuthorized(getUserName(),"TaskIntegrator.importZip");

    	return importZip(handler, privacy, true);
    }

    public String importZip(DataHandler handler, int privacy, boolean recursive) throws WebServiceException {
       	isAuthorized(getUserName(),"TaskIntegrator.importZip");
        return importZip(handler, privacy, recursive, null);
    }

    public String installSuite(SuiteInfo suiteInfo, DataHandler[] supportFiles, String[] fileNames)
            throws WebServiceException {
       	isAuthorized(getUserName(),"TaskIntegrator.importZip");
        try {
            TaskIntegratorDAO dao = new TaskIntegratorDAO();
            dao.createSuite(suiteInfo);

            String lsid = suiteInfo.getLSID();
            String suiteDir = DirectoryManager.getSuiteLibDir(suiteInfo.getName(), suiteInfo.getLSID(), suiteInfo
                    .getOwner());
            String[] docs = suiteInfo.getDocumentationFiles();
            for (int i = 0; i < docs.length; i++) {
                System.out.println("Doc=" + docs[i]);
                File f2 = new File(docs[i]);
                // if it is a url, download it and put it in the suiteDir now
                if (!f2.exists()) {
                    String file = GenePatternAnalysisTask.downloadTask(docs[i]);
                    f2 = new File(suiteDir, filenameFromURL(docs[i]));
                    boolean success = GenePatternAnalysisTask.rename(new File(file), f2, true);
                    System.out.println("Doc rename =" + success);

                }
                else {
                    // move file to suitedir

                    File f3 = new File(suiteDir, f2.getName());
                    boolean success = GenePatternAnalysisTask.rename(f2, f3, true);
                    System.out.println("Doc rename =" + success);

                }

            }

            if (supportFiles != null) {
                for (int i = 0; i < supportFiles.length; i++) {
                    String attachmentDir;
                    try {
                        attachmentDir = DirectoryManager.getSuiteLibDir(null, lsid, getUserName());
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        throw new WebServiceException(e);
                    }
                    File f = Util.getAxisFile(supportFiles[i]);
                    File dir = new File(attachmentDir);
                    File newFile = new File(dir, fileNames[i]);
                    f.renameTo(newFile);
                }
            }
            return lsid;
        }
        catch (Exception e) {
            log.error(e);
            throw new WebServiceException(e);
        }
    }

    protected String importZip(DataHandler handler, int privacy, boolean recursive, Status taskIntegrator)
            throws WebServiceException {
       	isAuthorized(getUserName(),"TaskIntegrator.importZip");
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
                ZipEntry taskManifestEntry = zippedFile.getEntry(GPConstants.MANIFEST_FILENAME);
                ZipEntry suiteManifestEntry = zippedFile.getEntry(GPConstants.SUITE_MANIFEST_FILENAME);
                if (suiteManifestEntry != null) {
                    isSuite = true;
                }
            }
            catch (IOException ioe) {
                throw new WebServiceException("Couldn't open " + path + ": " + ioe.getMessage());
            }
            if (isSuite) {
                lsid = installSuite(zippedFile);
            }
            else {
                try {
                    lsid = GenePatternAnalysisTask.installNewTask(path, username, privacy, recursive, taskIntegrator);
                }
                catch (TaskInstallationException tie) {
                    vProblems = tie.getErrors();
                }
            }
        }
        catch (Exception e) {
            throw new WebServiceException("while importing from zip file", e);
        }
        if (vProblems != null && vProblems.size() > 0) {
            throw new WebServiceErrorMessageException(vProblems);
        }
        return lsid;
    }

    public void installSuite(String lsid) throws WebServiceException {
       	isAuthorized(getUserName(),"TaskIntegrator.installSuite");
       	try {
            SuiteRepository sr = new SuiteRepository();
            HashMap suites = sr.getSuites(System.getProperty("SuiteRepositoryURL"));

            HashMap hm = (HashMap) suites.get(lsid);
            // get the info from the HashMap and install it into the DB
            SuiteInfo suite = new SuiteInfo(hm);

            installSuite(suite);
        }
        catch (Exception e) {
            throw new WebServiceException(e);
        }
    }

    public String installSuite(ZipFile zipFile) throws WebServiceException {
    	isAuthorized(getUserName(),"TaskIntegrator.installSuite");
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
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new WebServiceException(e);
        }
    }

    public String installSuite(SuiteInfo suiteInfo) throws WebServiceException {
    	isAuthorized(getUserName(),"TaskIntegrator.installSuite");
        
        try {
            if (suiteInfo.getLSID() != null) {
                if (suiteInfo.getLSID().trim().length() == 0) suiteInfo.setLSID(null);
            }

            (new TaskIntegratorDAO()).createSuite(suiteInfo);

            String suiteDir = DirectoryManager.getSuiteLibDir(suiteInfo.getName(), suiteInfo.getLSID(), suiteInfo
                    .getOwner());

            String[] docs = suiteInfo.getDocumentationFiles();
            for (int i = 0; i < docs.length; i++) {
                System.out.println("Doc=" + docs[i]);
                File f2 = new File(docs[i]);
                // if it is a url, download it and put it in the suiteDir now
                if (!f2.exists()) {
                    String file = GenePatternAnalysisTask.downloadTask(docs[i]);
                    f2 = new File(suiteDir, filenameFromURL(docs[i]));
                    boolean success = GenePatternAnalysisTask.rename(new File(file), f2, true);
                    System.out.println("Doc rename =" + success);

                }
                else {
                    // move file to suitedir

                    File f3 = new File(suiteDir, f2.getName());
                    boolean success = GenePatternAnalysisTask.rename(f2, f3, true);
                    System.out.println("Doc rename =" + success);

                }

            }

            return suiteInfo.getLSID();
        }
        catch (Exception e) {
            log.error(e);
            throw new WebServiceException(e);
        }

    }

    public String importZipFromURL(String url, int privacy, boolean recursive) throws WebServiceException {
    	isAuthorized(getUserName(),"TaskIntegrator.importZipFromURL");
        return importZipFromURL(url, privacy, recursive, null);
    }

    protected String importZipFromURL(String url, int privacy, boolean recursive, Status taskIntegrator)
            throws WebServiceException {
    	isAuthorized(getUserName(),"TaskIntegrator.importZipFromURL");
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
                ZipEntry taskManifestEntry = zippedFile.getEntry(GPConstants.MANIFEST_FILENAME);
                ZipEntry suiteManifestEntry = zippedFile.getEntry(GPConstants.SUITE_MANIFEST_FILENAME);
                isZipOfZips = isZipOfZips(url);
                if (taskManifestEntry != null) {
                    isTask = true;
                }
                if (suiteManifestEntry != null) {
                    isSuite = true;
                }
            }
            catch (IOException ioe) {
                throw new WebServiceException("Couldn't open " + path + ": " + ioe.getMessage());
            }
            if (!(isTask || isSuite || isZipOfZips)) {
                throw new WebServiceException("Couldn't find task or suite manifest in zip file ");
            }
            if (isSuite) {
                lsid = installSuite(zippedFile);
            }
            else { // isTask
                // replace task, do not version lsid or replace the lsid in the
                // zip
                // with a local one
                lsid = GenePatternAnalysisTask.installNewTask(path, username, privacy, recursive, taskIntegrator);
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
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException x) {
                }
            }
        }
        return lsid;
    }

    public String importZipFromURL(String url, int privacy) throws WebServiceException {
    	isAuthorized(getUserName(),"TaskIntegrator.importZipFromURL");
        return importZipFromURL(url, privacy, true, null);
    }

    public void installTask(String lsid) throws WebServiceException {
    	isAuthorized(getUserName(),"TaskIntegrator.installTask");
        InstallTasksCollectionUtils utils = new InstallTasksCollectionUtils(getUserName(), false);
        try {
            InstallTask[] tasks = utils.getAvailableModules();
            for (int i = 0; i < tasks.length; i++) {
                if (tasks[i].getLsid().equalsIgnoreCase(lsid)) {
                    tasks[i].install(getUserName(), GPConstants.ACCESS_PUBLIC, this);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void install(String lsid) throws WebServiceException {
    	isAuthorized(getUserName(),"TaskIntegrator.install");
        if (LSIDUtil.isSuiteLSID(lsid)) {
            installSuite(lsid);
        }
        else {
            installTask(lsid);
        }
    }

    public void delete(String lsid) throws WebServiceException {
    	if (LSIDUtil.isSuiteLSID(lsid)) {
    		isSuiteOwnerOrAuthorized(getUserName(), lsid, "TaskIntegrator.delete");
            TaskIntegratorDAO dao = new TaskIntegratorDAO();
            dao.deleteSuite(lsid);
            
        }
        else {
            deleteTask(lsid);
        }
    }

    protected String importZipFromURL(String url, int privacy, Status taskIntegrator)
            throws WebServiceException {
        return importZipFromURL(url, privacy, true, taskIntegrator);
    }

    public String[] getSupportFileNames(String lsid) throws WebServiceException {
        isTaskOwnerOrAuthorized(getUserName(), lsid, "TaskIntegrator.getSupportFileNames");

        if (lsid == null || lsid.equals("")) {
            throw new WebServiceException("Invalid LSID");
        }
    	
        try {
            Thread.yield();
            String attachmentDir = DirectoryManager.getTaskLibDir(lsid);
            File dir = new File(attachmentDir);
            String[] oldFiles = dir.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return (!name.endsWith(".old"));
                }
            });
            return oldFiles;
        }
        catch (Exception e) {
            throw new WebServiceException("while getting support filenames", e);
        }
    }

    private boolean isTaskOwner(String user, String lsid) throws WebServiceException{
        TaskMaster tm = (new TaskMasterDAO()).findByIdLsid(lsid);
        if (tm == null) return false; // can't own what you can't see
    	return user.equals(tm.getUserId());
    }
    
    private void isTaskOwnerOrAuthorized(String user, String lsid, String method) throws WebServiceException{  
 
    	if (!isTaskOwner(user, lsid)){
    		isAuthorized(user,method);
    	}
    }
    
    private void isAuthorized(String user, String method) throws WebServiceException {
    	if (!authManager.isAllowed(method, user)) {
        	throw new WebServiceException("You do not have permission for items owned by other users."); 	
        } 
    }
    
    private boolean isSuiteOwner(String user, String lsid){
    	
        Suite aSuite = (new SuiteDAO()).findById(lsid);
        String owner = aSuite.getOwner();
        return owner.equals(getUserName());
        
    }
    
    private void isSuiteOwnerOrAuthorized(String user, String lsid, String method) throws WebServiceException{  
    	if (!isSuiteOwner(user, lsid)){
    		isAuthorized(user,method);
    	}
    }
    
    public DataHandler getSupportFile(String lsid, String fileName) throws WebServiceException {
        isTaskOwnerOrAuthorized(getUserName(), lsid, "TaskIntegrator.getSupportFile");
          	
        if (lsid == null || lsid.equals("")) {
            throw new WebServiceException("Invalid LSID");
        }
    	
        try {
            Thread.yield();
            String attachmentDir = DirectoryManager.getTaskLibDir(lsid);
            File dir = new File(attachmentDir);
            File f = new File(dir, fileName);
            if (!f.exists()) {
                throw new WebServiceException("File " + fileName + " not found.");
            }
            return new DataHandler(new FileDataSource(f));
        }
        catch (Exception e) {
            throw new WebServiceException("while getting support file " + fileName + " from " + lsid, e);
        }
    }

    public DataHandler[] getSupportFiles(String lsid, String[] fileNames) throws WebServiceException {
    	isTaskOwnerOrAuthorized(getUserName(), lsid, "TaskIntegrator.getSupportFiles");
     	try {
            if (lsid == null || lsid.equals("")) {
                throw new WebServiceException("Invalid LSID");
            }
           
            DataHandler[] dhs = new DataHandler[fileNames.length];
            String attachmentDir = DirectoryManager.getTaskLibDir(lsid);
            File dir = new File(attachmentDir);
            for (int i = 0; i < fileNames.length; i++) {
                File f = new File(dir, fileNames[i]);
                if (!f.exists()) {
                    throw new WebServiceException("File " + fileNames[i] + " not found.");
                }
                dhs[i] = new DataHandler(new FileDataSource(f));
            }
            return dhs;
        }
        catch (Exception e) {
            throw new WebServiceException("Error getting support files.", e);
        }
    }

    public long[] getLastModificationTimes(String lsid, String[] fileNames) throws WebServiceException {
    	isTaskOwnerOrAuthorized(getUserName(), lsid, "TaskIntegrator.getLastModificationTimes");
     	try {
            if (lsid == null || lsid.equals("")) {
                throw new WebServiceException("Invalid LSID");
            }
            long[] modificationTimes = new long[fileNames.length];
            String attachmentDir = DirectoryManager.getTaskLibDir(lsid);
            File dir = new File(attachmentDir);
            for (int i = 0; i < fileNames.length; i++) {
                File f = new File(dir, fileNames[i]);
                modificationTimes[i] = f.lastModified();
            }
            return modificationTimes;
        }
        catch (Exception e) {
            throw new WebServiceException("Error getting support files.", e);
        }
    }

    public DataHandler[] getSupportFiles(String lsid) throws WebServiceException {
       isTaskOwnerOrAuthorized(getUserName(), lsid, "TaskIntegrator.getSupportFiles");
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

    public String modifyTask(int accessId, String taskName, String description, ParameterInfo[] parameterInfoArray,
            Map taskAttributes, DataHandler[] dataHandlers, String[] fileNames) throws WebServiceException {

    	isAuthorized(getUserName(), "TaskIntegrator.modifyTask");

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
            lsid = (String) taskAttributes.get(GPConstants.LSID);
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
                        taskAttributes.put(GPConstants.LSID, lsid);
                        // change owner to current user
                        String owner = (String) taskAttributes.get(GPConstants.USERID);
                        if (owner == null) {
                            owner = "";
                        }
                        if (owner.length() > 0) {
                            owner = " (" + owner + ")";
                        }
                        owner = username + owner;
                        taskAttributes.put(GPConstants.USERID, owner);
                    }
                }
                catch (MalformedURLException mue) {
                }
            }
            lsid = GenePatternAnalysisTask.installNewTask(taskName, description, parameterInfoArray,
                    new TaskInfoAttributes(taskAttributes), username, accessId, this);
            taskAttributes.put(GPConstants.LSID, lsid); // update so that upon
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
                    else if (oldAttachmentDir != null) { // file from
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
                        }
                        catch (Exception x) {
                        }
                    }
                }.start();
            }
        }
        catch (TaskInstallationException tie) {
            throw new WebServiceErrorMessageException(tie.getErrors());
        }
        catch (Exception e) {
            throw new WebServiceException(e);
        }
        return lsid;
    }

    public String deleteFiles(String lsid, String[] fileNames) throws WebServiceException {
    isTaskOwnerOrAuthorized(getUserName(), lsid, "TaskIntegrator.deleteFiles");
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
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new WebServiceException("while deleting files from " + lsid, e);
        }
    }

    public void deleteTask(String lsid) throws WebServiceException {
    	isTaskOwnerOrAuthorized(getUserName(), lsid, "TaskIntegrator.deleteTask");
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
        }
        catch (Throwable e) {
            throw new WebServiceException("while deleting task " + lsid, e);
        }
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

    public String cloneTask(String oldLSID, String cloneName) throws WebServiceException {
    	isAuthorized(getUserName(),  "TaskIntegrator.cloneTask");
        String userID = getUserName();
        
        
        try {
            TaskInfo taskInfo = null;
            try {
                taskInfo = new LocalAdminClient(userID).getTask(oldLSID);
            }
            catch (Exception e) {
                throw new WebServiceException(e);
            }
            taskInfo.setName(cloneName);
            taskInfo.setAccessId(GPConstants.ACCESS_PRIVATE);
            taskInfo.setUserId(userID);
            TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
            tia.put(GPConstants.USERID, userID);
            tia.put(GPConstants.PRIVACY, GPConstants.PRIVATE);
            oldLSID = (String) tia.remove(GPConstants.LSID);
            if (tia.get(GPConstants.TASK_TYPE).equals(GPConstants.TASK_TYPE_PIPELINE)) {
                PipelineModel model = PipelineModel.toPipelineModel((String) tia.get(GPConstants.SERIALIZED_MODEL));

                // update the pipeline model with the new name
                model.setName(cloneName);
                model.setUserid(userID);
                // update the task with the new model and command line
                TaskInfoAttributes newTIA = AbstractPipelineCodeGenerator.getTaskInfoAttributes(model);
                tia.put(GPConstants.SERIALIZED_MODEL, model.toXML());
                tia.put(GPConstants.COMMAND_LINE, newTIA.get(GPConstants.COMMAND_LINE));
            }
            String newLSID = modifyTask(GPConstants.ACCESS_PRIVATE, cloneName, taskInfo.getDescription(), taskInfo
                    .getParameterInfoArray(), tia, null, null);
            cloneTaskLib(taskInfo.getName(), cloneName, oldLSID, newLSID, userID);
            return newLSID;
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            throw new WebServiceException(e);
        }
    }

    public String[] getDocFileNames(String lsid) throws WebServiceException {
    	isTaskOwnerOrAuthorized(getUserName(), lsid, "TaskIntegrator.getDocFileNames");
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
        }
        catch (Exception e) {
            throw new WebServiceException("while getting doc filenames", e);
        }
    }

    public DataHandler[] getDocFiles(String lsid) throws WebServiceException {
        String taskLibDir = null;
        isTaskOwnerOrAuthorized(getUserName(), lsid, "TaskIntegrator.getDocFiles");
    	
        try {
            taskLibDir = DirectoryManager.getLibDir(lsid);
        }
        catch (Exception e) {
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

    public boolean isZipOfZips(String url) throws WebServiceException {
        isAuthorized(getUserName(), "TaskIntegrator.isZipOfZips");
        File file = Util.downloadUrl(url);
        try {
            return org.genepattern.server.TaskUtil.isZipOfZips(file);
        }
        catch (java.io.IOException ioe) {
            throw new WebServiceException(ioe);
        }
    }

    public boolean isSuiteZip(String url) throws WebServiceException {
        isAuthorized(getUserName(), "TaskIntegrator.isSuiteZip");
               File file = Util.downloadUrl(url);
        try {
            return org.genepattern.server.TaskUtil.isSuiteZip(file);
        }
        catch (java.io.IOException ioe) {
            throw new WebServiceException(ioe);
        }
    }

    public boolean isPipelineZip(String url) throws WebServiceException {
		isAuthorized(getUserName(), "TaskIntegrator.isPipelineZip");
	    File file = Util.downloadUrl(url);
	    try {
            return org.genepattern.server.TaskUtil.isPipelineZip(file);
        }
        catch (java.io.IOException ioe) {
            throw new WebServiceException(ioe);
        }
    }

    public void statusMessage(String message) {
        System.out.println("statusMessage: " + message);
    }

    public void errorMessage(String message) {
        System.out.println("errorMessage: " + message);
    }

    public void beginProgress(String message) {
        System.out.println("beginProgress: " + message);
    }

    public void continueProgress(int percentComplete) {
        System.out.println("continueProgress: " + percentComplete);
    }

    public void endProgress() {
        System.out.println("endProgress");
    }

    public static String filenameFromURL(String url) {
        int idx = url.lastIndexOf("/");
        if (idx >= 0) return url.substring(idx + 1);
        else
            return url;
    }

    public String clone(String lsid, String name) throws WebServiceException {
        throw new WebServiceException("clone is not implemented");
    }

    public String modifySuite(int access_id, String lsid, String name, String description, String author, String owner,
            String[] moduleLsids, DataHandler[] dataHandlers, String[] fileNames) throws WebServiceException {
        throw new WebServiceException("ModifySuite is not implemented");

    }
}