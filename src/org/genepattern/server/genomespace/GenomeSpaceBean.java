package org.genepattern.server.genomespace;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.userupload.UserUploadManager;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.server.webapp.uploads.UploadFilesBean;
import org.genepattern.server.webapp.uploads.UploadFilesBean.DirectoryInfoWrapper;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.richfaces.component.UITree;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

/**
 * This bean handles the information necessary for logging into GenomeSpace
 * from GenePattern and managing the GenomeSpace session.  It is session-scoped.
 * @author tabor
 *
 */
public class GenomeSpaceBean {
    private static Logger log = Logger.getLogger(GenomeSpaceBean.class);

    public static String GS_SESSION_KEY = "GS_SESSION";
    public static String GS_USER_KEY = "GS_USER";
    public static String GS_DIRECTORIES_KEY = "GS_DIRECTORIES";
    public static String GS_FILE_METADATAS = "GS_FILE_METADATAS";
    
    private boolean genomeSpaceEnabled = false;
    private boolean loginFailed = false;
    private String genomeSpaceUsername = null;
    private Map<String, Set<TaskInfo>> kindToModules = null;
    private String currentTaskLsid = null;
    private TaskInfo currentTaskInfo = null;
    private TreeNode<GenomeSpaceFile> fileTree = null;
    private List<GenomeSpaceFile> allFiles = null;
    private List<GenomeSpaceFile> allDirectories = null;
    private Map<String, Set<String>> kindToTools = null;

    /**
     * Determine whether GenomeSpace is enabled when the bean is created.  This makes sure that GenomeSpace
     * classes aren't loaded unless it is enabled.  It also means that if GenomeSpace is enabled in the 
     * config.yaml file then the user will need to log out and log back in before the GenomeSpace options are visible.
     */
    public GenomeSpaceBean() {
        genomeSpaceEnabled = GenomeSpaceClientFactory.isGenomeSpaceEnabled(UIBeanHelper.getUserContext());
        log.info("GenomeSpaceEnabled = " + genomeSpaceEnabled + " for " + UIBeanHelper.getUserId());
        
        // Attain a copy of the kindToModules map
        TaskInfo[] moduleArray = new AdminDAO().getLatestTasks(UIBeanHelper.getUserId());
        List<TaskInfo> allModules = Arrays.asList(moduleArray);
        kindToModules = SemanticUtil.getKindToModulesMap(allModules);
    }
    
    /**
     * Clears all the GenomeSpace session parameters kept in memory by the bean.
     * Called when the user logs out of GenomeSpace.
     */
    public void clearSessionParameters() {
        loginFailed = false;
        genomeSpaceUsername = null;
        kindToModules = null;
        currentTaskLsid = null;
        currentTaskInfo = null;
        fileTree = null;
        allFiles = null;
        allDirectories = null;
        kindToTools = null;
    }
    
    /**
     * If the GenomeSpace file tree has changed this method should be called to tell the bean that
     * it should rebuild the tree the next time it loads.
     */
    public void forceFileRefresh() {
        fileTree = null;
        allFiles = null;
        allDirectories = null;
    }
    
    /**
     * Handle form submission for the GenomeSpace login page
     * @return The JSF page navigation rule to go to next
     */
    public String submitLogin() {
        if (!genomeSpaceEnabled) {
            log.error("GenomeSpace is not enabled at GenomeSpace login");
            return "home";
        }
        
        if (UIBeanHelper.getUserId() == null) {
            return "home";
        } 
        
        genomeSpaceUsername = UIBeanHelper.getRequest().getParameter("username");
        String genomeSpacePassword = UIBeanHelper.getRequest().getParameter("password");
        String env = UIBeanHelper.getRequest().getParameter("envSelect");
        if (env == null || genomeSpaceUsername == null || genomeSpacePassword == null) {
            log.error("Error getting login criteria for GenomeSpace. Username: " + genomeSpaceUsername + " Password: " + genomeSpacePassword + " Environment: " + env);
            this.setMessageToUser("Error logging into GenomeSpace");
            this.loginFailed = true;
            return "genomeSpaceLoginFailed";
        }
        
        try {
            boolean loginSuccess = GenomeSpaceLoginManager.loginFromUsername(env, genomeSpaceUsername, genomeSpacePassword, UIBeanHelper.getSession());
            
            if (loginSuccess) {
                this.setMessageToUser("Signed in to GenomeSpace as " + genomeSpaceUsername);
                loginFailed = false;
                return "home";
            }
            else {
                log.error("GenomeSpaceLogin was null loging into GenomeSpace");
                this.loginFailed = true;
                this.setMessageToUser("Error logging into GenomeSpace");
                return "genomeSpaceLoginFailed";
            }
        } 
        catch (GenomeSpaceException e) {
            this.loginFailed = true;
            this.setMessageToUser(e.getMessage());
            return "genomeSpaceLoginFailed";
        }
    }
    
    /**
     * Determine whether the user is logged into GenomeSpace
     * @return
     */
    public boolean isLoggedIn() {
        if (!genomeSpaceEnabled) {
            return false;
        }
        HttpSession httpSession = UIBeanHelper.getSession();
        Object gsSessionObj = httpSession.getAttribute(GS_SESSION_KEY);
        return GenomeSpaceClientFactory.getGenomeSpaceClient().isLoggedIn(gsSessionObj);
    }
    
    /**
     * Handle submission of the GenomeSpace logout form.
     * Clear the GenomeSpace session variables.
     * @return
     */
    public String submitLogout() {
        if (!genomeSpaceEnabled) {
            log.error("GenomeSpace is not enabled");
            return "home";
        }
        
        HttpSession httpSession = UIBeanHelper.getSession();
        Object gsSession = httpSession.getAttribute(GS_SESSION_KEY);
        GenomeSpaceClientFactory.getGenomeSpaceClient().logout(gsSession);
        httpSession.setAttribute(GS_USER_KEY, null);
        httpSession.setAttribute(GS_SESSION_KEY, null);
        clearSessionParameters();
        setMessageToUser("Logged out of GenomeSpace.");
       
        return "home";
    }
    
    /**
     * Handle submission of the GenomeSpace registration form.
     * Then log the user into their new GenomeSpace account
     * @return
     */
    public String submitRegistration() {
        if (!genomeSpaceEnabled) {
            UIBeanHelper.setErrorMessage("GenomeSpace is not enabled");
            log.error("GenomeSpace is not enabled");
            return "genomeSpaceRegFailed";
        }
        
        genomeSpaceUsername = UIBeanHelper.getRequest().getParameter("username");
        String genomeSpacePassword = UIBeanHelper.getRequest().getParameter("password");
        String regPassword = UIBeanHelper.getRequest().getParameter("regPassword");
        String regEmail = UIBeanHelper.getRequest().getParameter("email");
        String env = UIBeanHelper.getRequest().getParameter("envSelect");
        if (env == null || genomeSpaceUsername == null || genomeSpacePassword == null || regPassword == null || regEmail == null) {
            log.error("Field null when trying to register for GenomeSpace " + genomeSpaceUsername + " " + genomeSpacePassword + 
                    " " + regPassword + " " + regEmail + " " + env);
            this.setMessageToUser("Error Registering With GenomeSpace");
            this.loginFailed = true;
            return "genomeSpaceRegFailed";
        }
        
        if (genomeSpaceUsername == null) {
            this.setMessageToUser("GenomeSpace username is blank");
            this.loginFailed = true;
            return "genomeSpaceRegFailed";
        }
        if (! regPassword.equals(genomeSpacePassword)) {
            UIBeanHelper.setInfoMessage("GenomeSpace password does not match");
            this.loginFailed = true;
            return "genomeSpaceRegFailed";
        }
    
        try {
            GenomeSpaceClientFactory.getGenomeSpaceClient().registerUser(env, genomeSpaceUsername, genomeSpacePassword, regEmail);
            this.loginFailed = false;
            submitLogin();
        }
        catch (GenomeSpaceException e) {
            log.error(e);
            setMessageToUser(e.getLocalizedMessage());
            this.loginFailed = true;
            return "genomeSpaceRegFailed";
        }
      
        return "home";
    }
    
    /**
     * Initialize the current TaskInfo from the current task LSID.
     * Used when viewing the RunTaskForm for a module.
     */
    public void initCurrentLsid() {
        String currentUser = UIBeanHelper.getUserId();
        AdminDAO adminDao = new AdminDAO();
        this.currentTaskInfo = adminDao.getTask(currentTaskLsid, currentUser);
    }
    
    /**
     * Set the current task LSID.  Used when viewing the RunTaskForm for a module.
     * @param selectedModule
     */
    public void setSelectedModule(String selectedModule) {
        this.currentTaskLsid = selectedModule;
        initCurrentLsid();
    }
    
    /**
     * Send an info message to the user when is displayed when the next page loads.
     * @param messageToUser
     */
    public void setMessageToUser(String messageToUser) {
        UIBeanHelper.setInfoMessage(messageToUser);
    }
    
    /**
     * Returns a map of file kinds to a set of modules that accept files of the kind in question.
     * This set is iterated over in the JSF for different files.
     * @return
     */
    public Map<String, Set<TaskInfo>> getKindToModules() {
        return kindToModules;
    }
    
    /**
     * Returns whether GenomeSpace is enabled or not
     * @return
     */
    public boolean isGenomeSpaceEnabled() {
        return genomeSpaceEnabled;
    }
    
    /**
     * Returns  current GenomeSpace username
     * @return
     */
    public String getUsername() {
        return genomeSpaceUsername;
    }
    
    /**
     * The loginFailed flag is used to signal the JSF form that something went wrong with the
     * GenomeSpace login.
     * @return
     */
    public boolean isLoginFailed() {
        return loginFailed;
    }
    
    /**
     * Determines whether the file tree should be expanded by default
     */
    public boolean openTreeNode(UITree tree) {
        return true;
    }
    
    /**
     * Recursively constructs a list of all GenomeSpace files in or in subdirectories of a provided directory.
     * Adds these files to the provided list and then returns that list.
     * @param list
     * @param dir
     * @return
     */
    private List<GenomeSpaceFile> buildFilesList(List<GenomeSpaceFile> list, GenomeSpaceFile dir) {
        if (!dir.isDirectory()) {
            log.error("buildFilesList() was given a non-directory: " + dir.getName());
            return list;
        }
        
        for (GenomeSpaceFile i : dir.getChildFiles()) {
            if (i.isDirectory()) {
                buildFilesList(list, i);
            }
            else {
                list.add(i);
            }
        }
        
        return list;
    }
    
    /**
     * Returns a flat list of all GenomeSpace files.
     * Constructs the list of files if necessary
     * @return
     */
    public List<GenomeSpaceFile> getAllFiles() {
        if (isLoggedIn() && allFiles == null) {
            // Get the children of the dummy node, which should contain only one child: the GenomeSpace root directory
            // Since this is of type Set you cannot just get the first child, you have it iterate over the set
            Set<GenomeSpaceFile> rootSet = getFileTree().getData().getChildFiles();
            for (GenomeSpaceFile i : rootSet) {
                allFiles = buildFilesList(new ArrayList<GenomeSpaceFile>(), i);
                break;
            }
            
        }
        
        return allFiles;
    }
    
    /**
     * Recursively builds a list of all GenomeSpace directories  given a parent directory.  Adds these directories
     * to a provided list and then returns that list.
     * @param list
     * @param dir
     * @return
     */
    private List<GenomeSpaceFile> buildDirectoriesList(List<GenomeSpaceFile> list, GenomeSpaceFile dir) {
        if (!dir.isDirectory()) {
            log.error("buildDirectoriesList() was given a non-directory: " + dir.getName());
            return list;
        }
        
        list.add(dir);
        for (GenomeSpaceFile i : dir.getChildFiles()) {
            if (i.isDirectory()) {
                buildDirectoriesList(list, i);
            }
        }
        
        return list;
    }
    
    /**
     * Returns a flat list of all GenomeSpace directories
     * Constructs the list of directories if necessary
     * @return
     */
    public List<GenomeSpaceFile> getAllDirectories() {
        if (isLoggedIn() && allDirectories == null) {
            // Get the children of the dummy node, which should contain only one child: the GenomeSpace root directory
            // Since this is of type Set you cannot just get the first child, you have it iterate over the set
            Set<GenomeSpaceFile> rootSet = getFileTree().getData().getChildFiles();
            for (GenomeSpaceFile i : rootSet) {
                allDirectories = buildDirectoriesList(new ArrayList<GenomeSpaceFile>(), i);
                break;
            }
        }
        
        return allDirectories;
    }
    
    /**
     * Determines whether the GenomeSpace file free is empty
     * @return
     */
    public boolean isEmptyTree() {
        if (fileTree == null) return true;
        GenomeSpaceFile file = fileTree.getData();
        if (file == null) return true;
        Set<GenomeSpaceFile> children = file.getChildFiles();
        if (children == null) return true;
        if (children.size() == 0) return true;
        return false;
    }
    
    /**
     * Recursively constructs a node of the GenomeSpace file tree for display in the JSF.
     * @param data
     * @return
     */
    private TreeNode<GenomeSpaceFile> constructTreeNode(GenomeSpaceFile data) {
        TreeNode<GenomeSpaceFile> node = new TreeNodeImpl<GenomeSpaceFile>();
        node.setData(data);
        int count = 0;
        
        if (data.isDirectory()) {
            for (GenomeSpaceFile i : data.getChildFiles()) {
                TreeNode<GenomeSpaceFile> child = constructTreeNode(i);
                node.addChild(count, child);
                count++;
            }
        }
        
        return node;
    }
    
    /**
     * Constructs the GenomeSpace file tree for display in the JSF.  Included in this construction is
     * a dummy node which serves as the root node.  This dummy node is necessary because when the file
     * tree is displayed using the JSF tree component the root node is always hidden in the display.  This
     * allows the user to interact with the root GenomeSpace directory, since the dummy node is hidden, leaving
     * the root GenomeSpace directory the most fundamental displayed node.
     * @return
     */
    private TreeNode<GenomeSpaceFile> constructFileTree() {
        HttpSession httpSession = UIBeanHelper.getSession();
        Object gsSessionObject = httpSession.getAttribute(GS_SESSION_KEY);
        if (gsSessionObject == null) {
            log.error("GenomeSpace session is null in GenomeSpaceBean.constructFileTree()");
            return null;
        }
        GenomeSpaceFile data = GenomeSpaceClientFactory.getGenomeSpaceClient().buildFileTree(gsSessionObject);
        TreeNode<GenomeSpaceFile> rootChild = constructTreeNode(data);
        
        // Create a dummy file for the hidden root node of the file tree
        // Gets around a quirk of the JSF component
        GenomeSpaceFile dummy = new GenomeSpaceFile();
        dummy.setKind(GenomeSpaceFile.DIRECTORY_KIND);
        dummy.setName("GenomeSpace Files");
        dummy.setChildFiles(new HashSet<GenomeSpaceFile>());
        dummy.getChildFiles().add(data);
        
        TreeNode<GenomeSpaceFile> rootNode = new TreeNodeImpl<GenomeSpaceFile>();
        rootNode.setData(dummy);
        rootNode.addChild(0, rootChild);

        return rootNode;
    }
    
    /**
     * Returns a copy of the GenomeSpace file tree, initializing it lazily if it has not already been built.
     * @return
     */
    public TreeNode<GenomeSpaceFile> getFileTree() {
        if (fileTree == null) {
            fileTree = constructFileTree();
        }
        return fileTree;
    }
    
    /**
     * Iterates over the GenomeSpace file list--initializing lazily if necessary--and returns the first file found
     * with a matching GenomeSpace URL.  (In theory these URLs should be unique.)
     * @param url
     * @return
     */
    public GenomeSpaceFile getFile(URL url) {
        for (GenomeSpaceFile i : getAllFiles()) {
            URL iUrl;
            try {
                iUrl = i.getUrl();
            }
            catch (Exception e) {
                log.error("Error getting url in getFile() from " + i.getName());
                continue;
            }
            if (url.equals(iUrl)) {
                return i;
            }
        }
        log.info("Unable to find the GenomeSpace file in file list: " + url);
        return null;
    }
    
    /**
     * Iterates over the GenomeSpace file list--initializing lazily if necessary--and returns the first file found
     * with a matching GenomeSpace URL.  (In theory these URLs should be unique.)
     * Takes the URL as a string and then converts to a URL object.
     * @param url
     * @return
     */
    public GenomeSpaceFile getFile(String url) {
        try {
            return getFile(new URL(url));
        }
        catch (MalformedURLException e) {
            log.error("Error trying to get a URL object in getFile() for " + url);
            return null;
        }
    }
    
    /**
     * Iterates over the GenomeSpace directory list--initializing lazily if necessary--and returns the first 
     * directory found with a matching GenomeSpace URL.  (In theory these URLs should be unique.)
     * @param url
     * @return
     */
    public GenomeSpaceFile getDirectory(URL url) {
        for (GenomeSpaceFile i : getAllDirectories()) {
            URL iUrl;
            try {
                iUrl = i.getUrl();
            }
            catch (Exception e) {
                log.error("Error getting url in getDirectory() from " + i.getName());
                continue;
            }
            if (url.equals(iUrl)) {
                return i;
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
    public GenomeSpaceFile getDirectory(String url) {
        try {
            return getDirectory(new URL(url));
        }
        catch (MalformedURLException e) {
            log.error("Error trying to get a URL object in getDirectory() for " + url);
            return null;
        }
    }
    
    /**
     * Handles submission from a GenomeSpace file menu to delete a GenomeSpace file.
     * Then signals the bean to rebuild the file tree next load, since the tree has changed.
     */
    public void deleteFile() {
        if (!genomeSpaceEnabled) {
            this.setMessageToUser("GenomeSpace is not enabled");
            return;
        }
        
        String url = UIBeanHelper.getRequest().getParameter("url");
        
        GenomeSpaceFile file = getFile(url);
        HttpSession httpSession = UIBeanHelper.getSession();
        Object gsSessionObject = httpSession.getAttribute(GS_SESSION_KEY);
        
        try { 
            GenomeSpaceClientFactory.getGenomeSpaceClient().deleteFile(gsSessionObject, file);
            forceFileRefresh(); // force a refresh
            setMessageToUser("Deleted from GenomeSpace " + file.getName());
        }
        catch (GenomeSpaceException e) {
            setMessageToUser(e.getLocalizedMessage());
        }
    }
    
    /**
     * Handles submission from the GenomeSpace file menu to save a GenomeSpace file locally.
     */
    public void saveFile() {
        String url = UIBeanHelper.getRequest().getParameter("url");
        
        try {
            GenomeSpaceFile file = getFile(url);
            HttpServletResponse response = UIBeanHelper.getResponse();
            response.sendRedirect(file.getUrl().toString());
         
        } 
        catch (Exception e) {
            log.error("Error saving file: " + url + "Message: " + e.getMessage());
        }
    }
    
    /**
     * Returns a map of file kinds to a list of parameters of the current module.
     * This is iterated over to display send to parameters.
     * @return
     */
    public Map<String, List<ParameterInfo>> getSendToParameters() {
        if (currentTaskInfo != null) {
            return currentTaskInfo._getKindToParameterInfoMap();
        }
        else {
            return null;
        }
    }
    
    /**
     * Handle form submission from an upload file menu to send a file local to GenePattern to GenomeSpace
     * and puts it in the GenomeSpace directory selected.
     * 
     * Note: Currently assumes a user upload file.  May want to make this more generic in the future.
     * @return JSF navigation rule for where to go next
     */
    public String sendFileToGenomeSpace() {
        String fileToSend = UIBeanHelper.getRequest().getParameter("file");
        String directoryTarget = UIBeanHelper.getRequest().getParameter("directory");
        
        if (fileToSend == null || directoryTarget == null) {
            log.error("Error saving a file to GenomeSpace: " + fileToSend + " " + directoryTarget);
        }

        try {
            GpFilePath file = UserUploadManager.getUploadFileObj(UIBeanHelper.getUserContext(), new File(fileToSend), false);
            GenomeSpaceFile directory = getDirectory(directoryTarget);
            
            HttpSession httpSession = UIBeanHelper.getSession();
            Object gsSession = httpSession.getAttribute(GS_SESSION_KEY);
            GenomeSpaceClientFactory.getGenomeSpaceClient().saveFileToGenomeSpace(gsSession, file, directory); 
            setMessageToUser("File uploaded to GenomeSpace " + file.getName());
            forceFileRefresh();
        } 
        catch (Exception e) {
            UIBeanHelper.setErrorMessage(e.getLocalizedMessage());
        }
        return "home";
    }
    
    /**
     * Returns a map of file kinds to a set of GenomeSpace tools (Cytoscape, Galaxy, GenePattern, etc.) that are 
     * listed as accepting files of that kind.  This set is iterated over to display send to tools.
     * @return
     */
    public Map<String, Set<String>> getKindToTools() {
        if (kindToTools == null) {
            HttpSession httpSession = UIBeanHelper.getSession();
            Object gsSessionObject = httpSession.getAttribute(GS_SESSION_KEY);
            kindToTools = GenomeSpaceClientFactory.getGenomeSpaceClient().getKindToTools(gsSessionObject);
        }
        
        return kindToTools;
    }
    
    /**
     * Handles form submission for sending a GenomeSpace file to a GenomeSpace tool (Cytoscape, IGV, etc.).  Then 
     * forwards the user to the URL necessary to send the selected file to the selected tool.
     */
    public void forwardToTool() {
        String filePath = UIBeanHelper.getRequest().getParameter("file");
        String tool = UIBeanHelper.getRequest().getParameter("tool");
        HttpSession httpSession = UIBeanHelper.getSession();
        Object gsSessionObject = httpSession.getAttribute(GS_SESSION_KEY);
        
        if (filePath == null || tool == null) {
            log.error("Null value forwarding to the GenomeSpace tool URL: " + filePath + " " + tool);
        }

        try {
            GenomeSpaceFile file = getFile(filePath);
            URL url = GenomeSpaceClientFactory.getGenomeSpaceClient().getSendToToolUrl(gsSessionObject, file, tool);
            HttpServletResponse response = UIBeanHelper.getResponse();
            response.sendRedirect(url.toString());
        }
        catch (Exception e) {
            log.error("Error forwarding to the GenomeSpace tool URL: " + e.getMessage());
        }
    }
    
    private void downloadGenomeSpaceFile(URL url, File destinationFile) throws IOException, GenomeSpaceException {
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            is = GenomeSpaceClientFactory.getGenomeSpaceClient().getInputStream(genomeSpaceUsername, url);
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
    public void saveFileToUploads(String fileUrl, String directoryPath) {
        GenomeSpaceFile file = (GenomeSpaceFile) GenomeSpaceFileManager.createFile(fileUrl);
        GpFilePath directory = null;
        
        UploadFilesBean uploadBean = (UploadFilesBean) UIBeanHelper.getManagedBean("#{uploadFilesBean}");
        for (DirectoryInfoWrapper i : uploadBean.getDirectories()) {
            if (i.getPath().equals(directoryPath)) {
                directory = i.getFile();
                break;
            }
        }
        
        if (file == null || directory == null) {
            UIBeanHelper.setErrorMessage("Unable to save GenomeSpace file to uploads directory");
            log.error("Unable to get directory or file to save GenomeSpace file to uploads: " + file + " " + directory);
            return;
        }
        
        // Append a new file extension on if the downloaded kind of different than the base
        String name = file.getName();
        if (file.converted) {
            name += "." + file.getKind();
        }
        
        // Download the file
        File serverFile = new File(directory.getServerFile(), name);
        try {
            downloadGenomeSpaceFile(file.getUrl(), serverFile);
        }
        catch (Exception e) {
            log.error("Error downloading GenomeSpaceFile to input directory: " + e.getMessage());
            return;
        }

        // Update Database
        try {
            Context context = UIBeanHelper.getUserContext();
            File relativeFile = new File(directory.getRelativeFile(), name);
            GpFilePath asUploadFile = UserUploadManager.getUploadFileObj(context, relativeFile, true);
            UserUploadManager.createUploadFile(context, asUploadFile, 1);
            UserUploadManager.updateUploadFile(context, asUploadFile, 1, 1);
        }
        catch (Exception e) {
            UIBeanHelper.setErrorMessage("Unable to update database to include new file");
            log.error("Unable to update database to include new file " + e.getMessage());
        }
    }
}
