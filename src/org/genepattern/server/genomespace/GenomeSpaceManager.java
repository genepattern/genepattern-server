/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.genomespace;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.ExternalFile;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.userupload.UserUploadManager;
import org.genepattern.server.webapp.uploads.UploadFilesBean;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.TaskInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * @author Thorin Tabor
 */
public class GenomeSpaceManager {

    private static String GS_ENABLED = "gs.genomeSpaceEnabled";
    private static String GS_LOGGED_IN = "gs.loggedIn";
    private static String GS_LOGIN_FAILED = "gs.loginFailed";
    private static String GS_TOKEN_EXPIRED = "gs.tokenExpired";
    private static String GS_USERNAME = "gs.username";
    private static String GS_KIND_TO_MODULES = "gs.kindToModules";
    private static String GS_CURRENT_TASK_LSID = "gs.currentTaskLsid";
    private static String GS_CURRENT_TASK_INFO = "gs.currentTaskInfo";
    private static String GS_FILE_TREE = "gs.fileTree";
    private static String GS_ALL_FILES = "gs.allFiles";
    private static String GS_ALL_DIRECTORIES = "gs.allDirectories";
    private static String GS_KIND_TO_TOOLS = "gs.kindToTools";

    private static Logger log = Logger.getLogger(GenomeSpaceManager.class);

    public static void setGenomeSpaceEnabled(HttpSession session, boolean genomeSpaceEnabled) {
        session.setAttribute(GS_ENABLED, genomeSpaceEnabled);
    }

    public static void setLoggedIn(HttpSession session, Boolean loggedIn) {
        session.setAttribute(GS_LOGGED_IN, loggedIn);
    }

    public static void setLoginFailed(HttpSession session, boolean loginFailed) {
        session.setAttribute(GS_LOGIN_FAILED, loginFailed);
    }

    public static void setTokenExpired(HttpSession session, boolean tokenExpired) {
        session.setAttribute(GS_TOKEN_EXPIRED, tokenExpired);
    }

    public static void setGenomeSpaceUsername(HttpSession session, String genomeSpaceUsername) {
        session.setAttribute(GS_USERNAME, genomeSpaceUsername);
    }

    public static void setKindToModules(HttpSession session, Map<String, List<TaskInfo>> kindToModules) {
        session.setAttribute(GS_KIND_TO_MODULES, kindToModules);
    }

    public static void setCurrentTaskLsid(HttpSession session, String currentTaskLsid) {
        session.setAttribute(GS_CURRENT_TASK_LSID, currentTaskLsid);
    }

    public static void setCurrentTaskInfo(HttpSession session, TaskInfo currentTaskInfo) {
        session.setAttribute(GS_CURRENT_TASK_INFO, currentTaskInfo);
    }

    public static void setFileTree(HttpSession session, List<GenomeSpaceFile> fileTree) {
        session.setAttribute(GS_FILE_TREE, fileTree);
    }

    public static void setAllFiles(HttpSession session, List<GenomeSpaceFile> allFiles) {
        session.setAttribute(GS_ALL_FILES, allFiles);
    }

    public static void setAllDirectories(HttpSession session, List<GenomeSpaceFile> allDirectories) {
        session.setAttribute(GS_ALL_DIRECTORIES, allDirectories);
    }

    public static void setKindToTools(HttpSession session, Map<String, Set<String>> kindToTools) {
        session.setAttribute(GS_KIND_TO_TOOLS, kindToTools);
    }

    public static boolean isGenomeSpaceEnabled(HttpSession session) {
        Object obj = session.getAttribute(GS_ENABLED);
        if (obj == null) return false;
        else return (Boolean) obj;
    }

    public static Boolean getLoggedIn(HttpSession session) {
        Object obj = session.getAttribute(GS_LOGGED_IN);
        if (obj == null) return false;
        else return (Boolean) obj;
    }

    public static boolean isLoginFailed(HttpSession session) {
        Object obj = session.getAttribute(GS_LOGIN_FAILED);
        if (obj == null) return false;
        else return (Boolean) obj;
    }

    public static boolean isTokenExpired(HttpSession session) {
        Object obj = session.getAttribute(GS_TOKEN_EXPIRED);
        if (obj == null) return false;
        else return (Boolean) obj;
    }

    public static String getGenomeSpaceUsername(HttpSession session) {
        return (String) session.getAttribute(GS_USERNAME);
    }

    public static Map<String, List<TaskInfo>> getKindToModules(HttpSession session) {
        return (Map<String, List<TaskInfo>>) session.getAttribute(GS_KIND_TO_MODULES);
    }

    public static String getCurrentTaskLsid(HttpSession session) {
        return (String) session.getAttribute(GS_CURRENT_TASK_LSID);
    }

    public static TaskInfo getCurrentTaskInfo(HttpSession session) {
        return (TaskInfo) session.getAttribute(GS_CURRENT_TASK_INFO);
    }

    public static List<GenomeSpaceFile> getFileTree(HttpSession session) {
        return (List<GenomeSpaceFile>) session.getAttribute(GS_FILE_TREE);
    }

    public static List<GenomeSpaceFile> getAllFiles(HttpSession session) {
        return (List<GenomeSpaceFile>) session.getAttribute(GS_ALL_FILES);
    }

    public static List<GenomeSpaceFile> getAllDirectories(HttpSession session) {
        return (List<GenomeSpaceFile>) session.getAttribute(GS_ALL_DIRECTORIES);
    }

    public static Map<String, Set<String>> getKindToTools(HttpSession session) {
        return (Map<String, Set<String>>) session.getAttribute(GS_KIND_TO_TOOLS);
    }

    /**
     * Clears all the GenomeSpace session parameters kept in memory by the bean.
     * Called when the user logs out of GenomeSpace.
     */
    public static void clearSessionParameters(HttpSession session) {
        setLoginFailed(session, false);
        setGenomeSpaceUsername(session, null);
        setKindToModules(session, null);
        setCurrentTaskLsid(session, null);
        setCurrentTaskInfo(session, null);
        setFileTree(session, null);
        setAllFiles(session, null);
        setAllDirectories(session, null);
        setKindToTools(session, null);
    }

    /**
     * If the GenomeSpace file tree has changed this method should be called to tell the bean that
     * it should rebuild the tree the next time it loads.
     */
    public static void forceFileRefresh(HttpSession session) {
        GenomeSpaceManager.setAllFiles(session, null);
        GenomeSpaceManager.setAllDirectories(session, null);
        GenomeSpaceManager.setFileTree(session, null);
    }

    /**
     * Initialize the current TaskInfo from the current task LSID.
     * Used when viewing the RunTaskForm for a module.
     */
    public static void initCurrentLsid(HttpSession session, String currentUser) {
        AdminDAO adminDao = new AdminDAO();
        TaskInfo currentTaskInfo = adminDao.getTask(getCurrentTaskLsid(session), currentUser);
        GenomeSpaceManager.setCurrentTaskInfo(session, currentTaskInfo);
    }

    /**
     * Set the current task LSID.  Used when viewing the RunTaskForm for a module.
     * @param selectedModule
     */
    public static void setSelectedModule(HttpServletRequest request, String user, String selectedModule) {
        HttpSession session = request.getSession();

        // Ignore AJAX requests
        if (request.getParameter("AJAXREQUEST") != null) { return; }

        GenomeSpaceManager.setCurrentTaskLsid(session, selectedModule);
        GenomeSpaceManager.initCurrentLsid(session, user);
    }

    /**
     * If the URL has spaces that need encoded, encode them and return
     * @param url
     * @return
     */
    private static URL encodeURLIfNecessary(URL url) {
        // If this is true, encoding is not needed
        if (url.toString().indexOf(" ") < 0) {
            return url;
        }

        // Do the encoding here
        URI uri;
        try {
            uri = new URI(url.getProtocol(), url.getHost(), url.getPath(), null);
            return uri.toURL();
        }
        catch (Exception e) {
            log.error("Error trying to encode a URL: " + url);
            return url;
        }
    }

    /**
     * Called to force the GenomeSpace Session to be attached to the GenePattern Session
     */
    public static Object forceGsSession(HttpSession gpSession) {
        String username = (String) gpSession.getAttribute(GPConstants.USERID);
        boolean loggedIn = false;
        try {
            loggedIn = GenomeSpaceLoginManager.loginFromDatabase(username, gpSession);
        }
        catch (GenomeSpaceException e) {
            loggedIn = false;
            log.error("ERROR: Exception forcing a login to GenomeSpace");
        }
        if (loggedIn) {
            Object gsSession = gpSession.getAttribute(GenomeSpaceLoginManager.GS_SESSION_KEY);
            if (gsSession == null) {
                log.error("ERROR: GenomeSpace session is still null");
            }
            return gsSession;
        }
        else {
            log.error("ERROR: Unable to force GenomeSpace login");
            return null;
        }
    }


    /**
     * Iterates over the GenomeSpace file list--initializing lazily if necessary--and returns the first file found
     * with a matching GenomeSpace URL.  (In theory these URLs should be unique.)
     * Takes the URL as a string and then converts to a URL object.
     * @param url
     * @return
     */
    public static GenomeSpaceFile getFile(HttpSession session, String url) {
        try {
            return getFile(session, new URL(url));
        }
        catch (MalformedURLException e) {
            log.error("Error trying to get a URL object in getFile() for " + url);
            return null;
        }
    }

    /**
     * Iterates over the GenomeSpace file list--initializing lazily if necessary--and returns the first file found
     * with a matching GenomeSpace URL.  (In theory these URLs should be unique.)
     * @param url
     * @return
     */
    public static GenomeSpaceFile getFile(HttpSession session, URL url) {
        Object gsSession = session.getAttribute(GenomeSpaceLoginManager.GS_SESSION_KEY);
        if (gsSession == null) {
            log.error("ERROR: Null gsSession found in GenomeSpaceManager.getFile()");
            gsSession = GenomeSpaceManager.forceGsSession(session);
        }
        url = encodeURLIfNecessary(url);
        return GenomeSpaceFileHelper.createFile(gsSession, url);
    }

    /**
     * Iterates over the GenomeSpace directory list--initializing lazily if necessary--and returns the first
     * directory found with a matching GenomeSpace URL.  (In theory these URLs should be unique.)
     * @param url
     * @return
     */
    public static GenomeSpaceFile getDirectory(HttpSession session, URL url) {
        // First trial, if the directory is already in the cached list
        // Second trial, clear the cached list, rebuild and try again
        int ran = 0;
        while (ran < 2) {
            List<GenomeSpaceFile> dirList = ran == 0 ? getAllDirectoriesLazy(session) : getAllDirectories(session);
            for (GenomeSpaceFile i : dirList) {
                URL iUrl;
                try {
                    iUrl = i.getUrl();
                }
                catch (Exception e) {
                    log.error("Error getting url in getDirectory() from " + i.getName());
                    continue;
                }
                if (url.toString().equals(iUrl.toString())) {
                    return i;
                }
            }
            ran++;
            if (ran == 1) {
                GenomeSpaceManager.setAllDirectories(session, null);
            }
        }

        log.info("Unable to find the GenomeSpace directory in the directory list: " + url);
        return null;
    }

    /**
     * Iterates over the GenomeSpace directory list--initializing lazily if necessary--and returns the first
     * directory found with a matching GenomeSpace URL.  (In theory these URLs should be unique.)
     * Takes the URL as a string and then converts to a URL object.
     * @param url
     * @return
     */
    public static GenomeSpaceFile getDirectory(HttpSession session, String url) {
        try {
            return getDirectory(session, new URL(url));
        }
        catch (MalformedURLException e) {
            log.error("Error trying to get a URL object in getDirectory() for " + url);
            return null;
        }
    }

    /**
     * Returns a flat list of all GenomeSpace directories
     * Constructs the list of directories if necessary
     * @return
     */
    public static synchronized List<GenomeSpaceFile> getAllDirectoriesLazy(HttpSession session) {
        Boolean isLoggedIn = GenomeSpaceManager.getLoggedIn(session);
        List<GenomeSpaceFile> allDirectories = GenomeSpaceManager.getAllDirectories(session);

        if (isLoggedIn && allDirectories == null) {
            // Get the children of the dummy node, which should contain only one child: the GenomeSpace root directory
            // Since this is of type Set you cannot just get the first child, you have it iterate over the set
            for (GenomeSpaceFile i : getFileTreeLazy(session)) {
                allDirectories = buildDirectoriesList(new ArrayList<GenomeSpaceFile>(), i);
                GenomeSpaceManager.setAllDirectories(session, allDirectories);
                break;
            }
        }

        return allDirectories;
    }

    /**
     * Recursively builds a list of all GenomeSpace directories  given a parent directory.  Adds these directories
     * to a provided list and then returns that list.
     * @param list
     * @param dir
     * @return
     */
    private static List<GenomeSpaceFile> buildDirectoriesList(List<GenomeSpaceFile> list, GenomeSpaceFile dir) {
        if (!dir.isDirectory()) {
            log.error("buildDirectoriesList() was given a non-directory: " + dir.getName());
            return list;
        }

        list.add(dir);
        for (GenomeSpaceFile i : dir.getChildFilesNoLoad()) {
            if (i.isDirectory()) {
                buildDirectoriesList(list, i);

            }
        }

        return list;
    }

    /**
     * Returns a copy of the GenomeSpace file tree, initializing it lazily if it has not already been built.
     * @return
     */
    public static List<GenomeSpaceFile> getFileTreeLazy(HttpSession session) {
        List<GenomeSpaceFile> fileTree = GenomeSpaceManager.getFileTree(session);
        if (fileTree == null) {
            fileTree = constructFileTree(session);
            GenomeSpaceManager.setFileTree(session, fileTree);
        }
        return fileTree;
    }

    /**
     * Constructs the GenomeSpace file tree for display in the JSF.  Included in this construction is
     * a dummy node which serves as the root node.  This dummy node is necessary because when the file
     * tree is displayed using the JSF tree component the root node is always hidden in the display.  This
     * allows the user to interact with the root GenomeSpace directory, since the dummy node is hidden, leaving
     * the root GenomeSpace directory the most fundamental displayed node.
     * @return
     */
    private static List<GenomeSpaceFile> constructFileTree(HttpSession httpSession) {
        Object gsSessionObject = httpSession.getAttribute(GenomeSpaceLoginManager.GS_SESSION_KEY);
        if (gsSessionObject == null) {
            log.error("ERROR: Null gsSession found in constructFileTree()");
            gsSessionObject = GenomeSpaceManager.forceGsSession(httpSession);
        }
        GenomeSpaceFile data = GenomeSpaceClientFactory.instance().buildFileTree(gsSessionObject);
        List<GenomeSpaceFile> rootList = new ArrayList<GenomeSpaceFile>();
        rootList.add(data);

        return rootList;
    }

    public static URL getConvertedFileUrl(HttpSession session, String fileUrl, String fileType) {
        Object gsSessionObject = session.getAttribute(GenomeSpaceLoginManager.GS_SESSION_KEY);
        GenomeSpaceFile file = getFile(session, fileUrl);
        try {
            return GenomeSpaceClientFactory.instance().getConvertedURL(gsSessionObject, file, fileType);
        }
        catch (GenomeSpaceException e) {
            log.error("GenomeSpaceException in getConvertedFileUrl(): " + e.getMessage());
            // FIXME: UIBeanHelper.setErrorMessage("Unable to send file to module: " + file.getName());
            return null;
        }
    }

    /**
     * Handles transferring a GenomeSpace file from GenomeSpace to the GenePattern server
     * @param url
     * @param destinationFile
     * @throws IOException
     * @throws GenomeSpaceException
     */
    private static void downloadGenomeSpaceFile(String user, URL url, File destinationFile) throws IOException, GenomeSpaceException {
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            is = GenomeSpaceClientFactory.instance().getInputStream(user, url);
            fos = new FileOutputStream(destinationFile);
            byte[] buf = new byte[100000];
            int j;
            while ((j = is.read(buf, 0, buf.length)) > 0) {
                fos.write(buf, 0, j);
            }
        } finally {
            if (is != null) {
                is.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    /**
     * Saves the given GenomeSpace file to the given user upload directory
     * @param fileUrl
     * @param directoryPath
     */
    public static void saveFileToUploads(HttpSession session, UploadFilesBean uploadBean, String gpUser, String fileUrl, String directoryPath) {
        Object gsSessionObject = session.getAttribute(GenomeSpaceLoginManager.GS_SESSION_KEY);
        if (gsSessionObject == null) {
            log.error("ERROR: Null gsSession found in saveFileToUploads()");
            gsSessionObject = GenomeSpaceManager.forceGsSession(session);
        }

        GpFilePath directory = null;
        for (UploadFilesBean.DirectoryInfoWrapper i : uploadBean.getDirectories()) {
            if (i.getPath().equals(directoryPath)) {
                directory = i.getFile();
                break;
            }
        }

        GpFilePath file = null;
        String name = null;
        if (fileUrl.contains("genomespace.org")) {
            file = (GenomeSpaceFile) GenomeSpaceFileHelper.createFile(gsSessionObject, fileUrl);



            // Append a new file extension on if the downloaded kind of different than the base
            name = file.getName();
            if (((GenomeSpaceFile) file).converted) {
                name += "." + file.getKind();
            }
        }
        else {
            file = new ExternalFile(fileUrl);
            name = file.getName();
        }

        if (file == null || directory == null) {
            // FIXME: UIBeanHelper.setErrorMessage("Unable to save GenomeSpace file to uploads directory");
            log.error("Unable to get directory or file to save GenomeSpace file to uploads: " + file + " " + directory);
            return;
        }

        // Download the file
        File serverFile = new File(directory.getServerFile(), name);
        try {
            downloadGenomeSpaceFile(gpUser, file.getUrl(), serverFile);
        }
        catch (Exception e) {
            log.error("Error downloading GenomeSpaceFile to input directory: " + e.getMessage());
            return;
        }

        // Update Database
        try {
            final HibernateSessionManager mgr=HibernateUtil.instance();
            @SuppressWarnings("deprecation")
            GpContext context = GpContext.getContextForUser(gpUser);
            File relativeFile = new File(directory.getRelativeFile(), name);
            GpFilePath asUploadFile = UserUploadManager.getUploadFileObj(mgr, context, relativeFile, true);
            UserUploadManager.createUploadFile(mgr, context, asUploadFile, 1, true);
            UserUploadManager.updateUploadFile(mgr, context, asUploadFile, 1, 1);
        }
        catch (Exception e) {
            // FIXME: UIBeanHelper.setErrorMessage("Unable to update database to include new file");
            log.error("Unable to update database to include new file " + e.getMessage());
        }
    }

    /**
     * Returns a map of file kinds to a set of GenomeSpace tools (Cytoscape, Galaxy, GenePattern, etc.) that are
     * listed as accepting files of that kind.  This set is iterated over to display send to tools.
     * @return
     */
    public static Map<String, Set<String>> getKindToToolsLazy(HttpSession session) {
        boolean genomeSpaceEnabled = GenomeSpaceManager.isGenomeSpaceEnabled(session);
        Map<String, Set<String>> kindToTools = GenomeSpaceManager.getKindToTools(session);

        // Protect against GenomeSpace not being enabled
        if (!genomeSpaceEnabled) return null;

        if (kindToTools == null) {
            Object gsSessionObject = session.getAttribute(GenomeSpaceLoginManager.GS_SESSION_KEY);
            if (gsSessionObject == null) return null;

            kindToTools = GenomeSpaceClientFactory.instance().getKindToTools(gsSessionObject);
            GenomeSpaceManager.setKindToTools(session, kindToTools);
        }

        return kindToTools;
    }
}
