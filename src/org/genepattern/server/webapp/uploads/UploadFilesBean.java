package org.genepattern.server.webapp.uploads;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.DataManager;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.domain.UploadFile;
import org.genepattern.server.domain.UploadFileDAO;
import org.genepattern.server.webapp.FileDownloader;
import org.genepattern.server.webapp.jsf.JobHelper;
import org.genepattern.server.webapp.jsf.RunTaskBean;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.server.webapp.jsf.UsersAndGroupsBean;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoCache;
import org.richfaces.component.UITree;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

/**
 * Backing bean for displaying user uploaded files and their associated popup menus.
 * 
 * @author pcarr
 */
public class UploadFilesBean {
    private static Logger log = Logger.getLogger(UsersAndGroupsBean.class);
    static SimpleDateFormat formatter = new SimpleDateFormat();
    static {
        formatter.applyPattern("MMM dd hh:mm:ss aaa");
    }
    
    public final String RECENT_JOBS = "recentJobs";
    public final String UPLOADS = "uploads";
    public final String SELECTED_TAB = "selectedTab";
    
    public UploadFilesBean() {
        initFiles();
    }

    /**
     * Helper class for displaying the send to module menu item for a file.
     * @author pcarr
     *
     */
    static public class SendToModule {
        private TaskInfo toTask;
        private FileInfoWrapper sendFile;
        private String paramName;
        
        public SendToModule(FileInfoWrapper sendFile, TaskInfo task) {
            this.sendFile = sendFile;
            this.toTask = task;
        }
        
        public String getName() {
            return toTask.getName();
        }
        
        /**
         * Get the first parameter from the taskInfo which accepts this file type.
         * @return
         */
        public String getLsid() {
            return toTask.getLsid();
        }
        /**
         * In response to a sendToModule request for a given file, get the name of the first parameter
         * which accepts the file.
         * 
         * @param taskInfo
         * @param kind
         * @return
         */
        public String getParamName() {
            if (paramName == null) {
                initParamName();
            }
            return paramName;
        }

        private void initParamName() {
            String kind = sendFile.getKind();
            List<ParameterInfo> l = toTask._getSendToParameterInfos(kind);
            if (l != null && l.size() > 0) {
                paramName = l.get(0).getName();
            }
            else {
                //TODO: log error, shouldn't be here
                paramName = "";
            }
        }

        /**
         * Get the link to redirect to in order to load the task.
         * @return
         */
        public String getSendToLink() {
            String link = UIBeanHelper.getServer() + "/pages/index.jsf?lsid=" 
                + UIBeanHelper.encode( toTask.getLsid() ) 
                + "&" + UIBeanHelper.encode( getParamName() ) 
                + "=" + UIBeanHelper.encode( sendFile.getFullUrl() );
            return link;
        }
    }

    public class FileInfoWrapper {
        private UploadFile file = null;
        private String url = null;
        private List<SendToModule> sendToModules = null;
        private boolean directory = false;
        private boolean root = false;

        public FileInfoWrapper(UploadFile file) {
            this.file = file;
        }
        
        public String getType() {
            if (isDirectory()) {
                return "directory";
            }
            else {
                return "file";
            }
        }
        
        public boolean isDirectory() {
            return directory;
        }

        public void setDirectory(boolean directory) {
            this.directory = directory;
        }
        
        public UploadFile getFile() {
            return file;
        }
        
        public long getModified() {
            return file.getLastModified().getTime();
        }
        
        public String getFormattedModified() {
            return formatter.format(file.getLastModified());
        }
        
        public String getFormattedLength() {
            return JobHelper.getFormattedSize(file.getFileLength());
        }
        
        public boolean isRoot() {
            return root;
        }

        public void setRoot(boolean root) {
            this.root = root;
        }
        
        public String getKind() {
            return file.getKind();
        }
        
        public String getFilename() {
            return file.getName();
        }
        
        // Returns full URL
        public String getUrl() {
            return getFullUrl();
        }
        
        // Returns absolute URL
        public String getFullUrl() {
            if (url==null) {
               url = initUrl();
            }
            return url;
        }

        private String initUrl() {
            String encodedPath = UIBeanHelper.encodeFilePath(file.getPath());
            String server = UIBeanHelper.getServer();
            String url = server + "/data/" + encodedPath;
            return url;
        }
        
        private String initUrl2() {
            String server = UIBeanHelper.getServer();
            String link = file.getLink();
            String url = server + link;
            
            //String encodedPath = UIBeanHelper.encodeFilePath(file.getPath());
            //String url = server + "/data/" + encodedPath;
            return url;
        }

        public String getPath() {
            return file.getPath();
        }
        
        // Returns a path encoded for use in div names
        public String getEncodedPath() {
            return file.getPath() != null ? file.getPath().replaceAll("[^a-zA-Z0-9]", "_") : "";
        }
        
        public boolean getPartial() {
            return file.isPartial();
        }
        
        public boolean isParent(DirectoryInfoWrapper dir) {
            int occur = this.getPath().indexOf(dir.getPath());
            if (occur < 0) { // This file is not inside the dir or the dir's subdirs
                return false;
            }
            int dirlength = dir.getPath().length();
            int thislength = this.getPath().length();
            if (occur + dir.getPath().length() >= this.getPath().length()) {
                return false; // A dir is never a parent of itself
            }
            String relPath = this.getPath().substring(occur + dir.getPath().length() + 1);
            if (relPath.equalsIgnoreCase(this.getFilename())) {
                return true;
            }
            else {
                return false;
            }
        }
        
        public Collection<SendToModule> getSendToModules() {
            if (sendToModules == null) {
                sendToModules = initSendToModules();
            }
            return sendToModules;
        }

        private List<SendToModule> initSendToModules() {
            List<SendToModule> sendToModules = new ArrayList<SendToModule>();
            Collection<TaskInfo> sendToTaskInfos = initSendToTaskInfos();
            for(TaskInfo taskInfo : sendToTaskInfos ) {
                SendToModule sendToModule = new SendToModule(this, taskInfo);
                sendToModules.add( sendToModule );
            }
            return Collections.unmodifiableList( sendToModules );            
        }

        private Collection<TaskInfo> initSendToTaskInfos() {
            String kind = file.getKind();
            Collection<TaskInfo> taskInfos =  kindToTaskInfo.get(kind);
            if (taskInfos == null) {
                return Collections.emptySet();
            }
            return Collections.unmodifiableCollection(taskInfos);
        }
        
        /**
         * Get the list of input parameters which accept this file as input.
         * @param file
         * @return
         */
        public List<ParameterInfo> getSendToParameters() {
            if (currentTaskInfo == null && currentTaskLsid != null) {
                AdminDAO adminDao = new AdminDAO();
                initCurrentLsid(adminDao);
            }
            if (currentTaskInfo == null) {
                return Collections.emptyList();
            }
            
            
            //nice to have a map of kind -> inputParameters for the current task
            String kind = file.getKind();
            return currentTaskInfo._getSendToParameterInfos(kind); 
        }
        
        //--- JSF actions
        
        /**
         * In response to selecting a module from the send to menu for a file, 
         * load the run module page, setting the appropriate input parameter to the value for this file.
         * 
         * Currently implemented by redirecting to the home page with the 'lsid' and <input.param.name> values set.
         * Similar to the way pages are loaded from the protocols links.
         * 
         * E.g. http://127.0.0.1:8080/gp/pages/index.jsf?lsid=PreprocessDataset&input.filename=ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.gct
         */
        public void sendToTaskInfo() {
            //sendToAlaProtocolsLink();
            //for legacy
            sendToAlaLoadTask();
        }
        
        /**
         * Based on JobBean#loadTask, this method is similar to that which was in place circa GP 3.3.1 and earlier.
         * Request attributes are set here and are read in RunTaskBean#setTask.
         * 
         * @return
         */
        private String sendToAlaLoadTask() {
            HttpServletRequest request = UIBeanHelper.getRequest();
            String lsid = request.getParameter("lsid");
            if (lsid == null) {
                log.error("Missing required parameter, lsid");
                UIBeanHelper.setErrorMessage("Missing required parameter, lsid");
                return "error";
            }
            
            request.setAttribute("lsid", lsid);
            request.setAttribute("outputFileName", this.getFilename());
            request.setAttribute("downloadPath", this.getUrl());
            request.setAttribute("outputFileSource", "UploadedFiles");
            
            RunTaskBean runTaskBean = (RunTaskBean) UIBeanHelper.getManagedBean("#{runTaskBean}");
            if (runTaskBean != null) {
                runTaskBean.setTask(lsid);
                return "run task";
            }
            
            return "error";
        }

        /**
         * Equivalent to the links from the protocols pages, which include the lsid and the url to the input file in the path.
         * TODO: once we refactor file references we should use a simpler approach which passes a reference to a file object
         * along to the run task form.
         */
        private void sendToAlaProtocolsLink() {
            String lsid = UIBeanHelper.getRequest().getParameter("lsid");
            if (lsid == null) {
                log.error("Missing required parameter, lsid");
                UIBeanHelper.setErrorMessage("Missing required parameter, lsid");
                return;
            }
            
            TaskInfo taskInfo = TaskInfoCache.instance().getTask(lsid);
            SendToModule s = new SendToModule(this, taskInfo);
            
            //redirect ...
            String redirect = s.getSendToLink();
            try {
                UIBeanHelper.getResponse().sendRedirect(redirect);
            }
            catch (IOException e) {
                //e is ignored, usually means client connection was closed
            }
            UIBeanHelper.getFacesContext().responseComplete();
            return;
        }
        
        /**
         * In response to selecting the 'Download' link from the popup menu for the file.
         */
        public void downloadFile() { 
            try { 
                ServletContext servletContext = UIBeanHelper.getServletContext();
                HttpServletResponse response = UIBeanHelper.getResponse();
                HttpServletRequest request = UIBeanHelper.getRequest();

                boolean serveContent = true;
                File fileObj = new File(file.getPath());

                FileDownloader.serveFile(servletContext, request, response, serveContent, FileDownloader.ContentDisposition.ATTACHMENT, fileObj);
            }
            catch (Throwable t) {
                log.error("Error downloading "+file.getPath()+" for user "+UIBeanHelper.getUserId(), t);
                UIBeanHelper.setErrorMessage("Error downloading "+file.getName()+": "+t.getLocalizedMessage());
            }
            FacesContext.getCurrentInstance().responseComplete();
        }
        
        /**
         * In response to selecting the 'Delete' link from the popup menu for the file.
         * Make sure to update the model (aka list of files).
         */
        public String deleteFileAction() {
            boolean deleted = deleteFile();
            if (deleted) {
                UploadFilesBean.this.files = null;
                return "success";
            }
            return "error";
        }

        /**
         * Helper method, which actually deletes the file.
         */
        public boolean deleteFile() {
            boolean deleted = DataManager.deleteFile(file);
            if (deleted) {
                UIBeanHelper.setInfoMessage("Deleted file: " + file.getName());
                return true;
            }
            else {
                UIBeanHelper.setErrorMessage("Unable to delete file: " + file.getName());
                return false;
            }
        }
    }
    
    public class DirectoryInfoWrapper extends FileInfoWrapper {
        List<FileInfoWrapper> dirFiles;
        boolean init = false;

        public DirectoryInfoWrapper(UploadFile file) {
            super(file);
            this.setDirectory(true);
        }

        public List<FileInfoWrapper> getFiles() {
            return dirFiles;
        }

        public void setFiles(List<FileInfoWrapper> files) {
            this.dirFiles = files;
        }
        
        public boolean isInit() {
            return init;
        }
        
        public void initDirectory() {
            if (files == null) {
                initFiles();
            }
            dirFiles = new ArrayList<FileInfoWrapper>();
            
            // Add subdirectories
            for (DirectoryInfoWrapper i : directories) {
                if (i.isParent(this)) {
                    dirFiles.add(i);
                }
            }
            
            // Add files
            for (FileInfoWrapper i : files) {
                if (i.isParent(this)) {
                    dirFiles.add(i);
                }
            }
            init = true;
        }
    }
    
/*
 * JSF usage:
   #{uploadFileBean.user}
   #{uploadFileBean.files}"

      #{file.fileLength}
      #{file.lastModified}
      #{file.name}
      #{file.path}
*/
    private List<FileInfoWrapper> files;
    private List<DirectoryInfoWrapper> directories;
    private String currentUser;
    private String currentTaskLsid = null;
    private TaskInfo currentTaskInfo = null;
    private Map<String,SortedSet<TaskInfo>> kindToTaskInfo;
    
    private static final Comparator<TaskInfo> taskInfoComparator =  new Comparator<TaskInfo>() {
        public int compare(TaskInfo o1, TaskInfo o2) {
            //1) null arg test
            if (o1 == null) {
                if (o2 == null) {
                    return 0;
                }
                return -1;
            } 
            if (o2 == null) {
                return 1;
            }

            //2) null name test
            if (o1.getName() == null) {
                if (o2.getName() == null) {
                    return 0;
                }
                return -1;
            }
            
            return o1.getName().compareTo( o2.getName() );
        } 
    };

    public String getCurrentUser() {
        if (currentUser == null) {
            currentUser = UIBeanHelper.getUserId();
        }
        return currentUser;
    }
    
    public void setCurrentTaskLsid(String lsid) {
        this.currentTaskLsid = lsid;
    }
    
    /**
     * for debugging
     * @return
     */
    public FileInfoWrapper getFile() {
        if (this.getFiles().size() > 0) {
            return this.getFiles().get(0);
        }
        return null;
    }
    
    public List<FileInfoWrapper> getFiles() {
        if (files == null) {
            initFiles(); 
        }
        return files;
    }
    
    public List<DirectoryInfoWrapper> getDirectories() {
        if (directories == null) {
            initFiles(); 
        }
        return directories;
    }
    
    public boolean openTreeNode(UITree tree) {
        return true;
    }
    
    /**
     * Lists input files from all sources in a unified tree
     * @return
     * @throws IOException 
     */
    public TreeNode<FileInfoWrapper> getFileTree() throws IOException {
        // Set up the root node
        TreeNode<FileInfoWrapper> rootNode = new TreeNodeImpl<FileInfoWrapper>();
        UploadFile rootFileFacade = new UploadFile();
        rootFileFacade.setName(UIBeanHelper.getUserId());
        FileInfoWrapper rootWrapper = new FileInfoWrapper(rootFileFacade);
        rootWrapper.setDirectory(true);
        rootWrapper.setRoot(true);
        rootNode.setData(rootWrapper);

        // Add component trees
        rootNode.addChild(0, getUploadFilesTree());
        return rootNode;
    }
    
    /**
     * Lists all upload files in a tree with the root being the user's upload dir
     * @return
     * @throws IOException 
     */
    public TreeNode<FileInfoWrapper> getUploadFilesTree() throws IOException {
        // Set up the tree's directory structure
        initDirectories();
        return getDirectoryNode(getWrappedUploadDir());
    }
    
    public TreeNode<FileInfoWrapper> getDirectoryNode(DirectoryInfoWrapper dir) {
        // Set up the root node
        TreeNode<FileInfoWrapper> rootNode = new TreeNodeImpl<FileInfoWrapper>();
        rootNode.setData(dir);
        int count = 0;

        // Add the upload dir's files
        for (FileInfoWrapper i : dir.getFiles()) {
            TreeNode<FileInfoWrapper> fileNode;
            if (i.isDirectory()) {
                fileNode = getDirectoryNode((DirectoryInfoWrapper) i);
            }
            else {
                fileNode = new TreeNodeImpl<FileInfoWrapper>();  
            }
            fileNode.setData(i);
            rootNode.addChild(count, fileNode);
            count++;
        }
        return rootNode;
    }
    
    public void flagUploadPath() {
        String filePath = UIBeanHelper.getRequest().getParameter("uploadPath");
        UIBeanHelper.getSession().setAttribute("uploadPath", filePath);
    }
    
    public File getUserUploadDir() {
        return ServerConfiguration.instance().getUserUploadDir(Context.getContextForUser(UIBeanHelper.getUserId()));
    }
    
    public DirectoryInfoWrapper getWrappedUploadDir() {
     // Set up the fake UploadFile for the wrapper
        UploadFile rootFileFacade = new UploadFile();
        try {
            rootFileFacade.initFromFile(getUserUploadDir(), UploadFile.COMPLETE);
        }
        catch (IOException e) {
            log.error("Unable to get canonical path for user root upload dir");
        }
        rootFileFacade.setUserId(UIBeanHelper.getUserId());
        rootFileFacade.setName("Uploaded Files");

        // Create the dir wrapper for the user upload dir
        DirectoryInfoWrapper rootWrapper = new DirectoryInfoWrapper(rootFileFacade);
        rootWrapper.initDirectory();
        rootWrapper.setRoot(true);
        return rootWrapper;
    }
    
    private void initFiles() {
        currentUser = UIBeanHelper.getUserId();
        List<UploadFile> uploadedFiles = new UploadFileDAO().findByUserId(currentUser);
        files = new ArrayList<FileInfoWrapper>();
        directories = new ArrayList<DirectoryInfoWrapper>();
        directories.add(getWrappedUploadDir());
        for(UploadFile file : uploadedFiles) {
            if (file.getKind() == null || !file.getKind().equalsIgnoreCase("directory")) {
                files.add(new FileInfoWrapper(file));
            }
            else {
                directories.add(new DirectoryInfoWrapper(file));
            }
        }
        initModuleMenuItems();
    }
    
    private void initDirectories() {
        if (files == null) {
            initFiles();
        }
        
        for (DirectoryInfoWrapper i : directories) {
            if (!i.isInit()) {
                i.initDirectory();
            }
        }
    }
    
    public void initCurrentLsid(AdminDAO adminDao) {
        currentTaskInfo = adminDao.getTask(currentTaskLsid, currentUser);
    }

    private void initModuleMenuItems() {
        kindToTaskInfo = new HashMap<String, SortedSet<TaskInfo>>();
        
        AdminDAO adminDao = new AdminDAO();
        if (currentTaskLsid != null) {
            initCurrentLsid(adminDao);
        }
        TaskInfo[] taskInfos = adminDao.getLatestTasks(currentUser);
        for(TaskInfo taskInfo : taskInfos) {
            for(String kind : taskInfo._getInputFileTypes()) {
                SortedSet<TaskInfo> taskInfosForMap = kindToTaskInfo.get(kind);
                if (taskInfosForMap == null) {
                    taskInfosForMap = new TreeSet<TaskInfo>(taskInfoComparator);
                    kindToTaskInfo.put(kind, taskInfosForMap);
                }
                taskInfosForMap.add(taskInfo);
            }
        }
    }
    
    public void deleteFile(ActionEvent ae) {
        String filePath = UIBeanHelper.getRequest().getParameter("filePath");
        for (final FileInfoWrapper i : files) {
            if (i.getPath().equals(filePath)) {
                if (i.deleteFile()) {
                    files.remove(i);
                }
                return;
            }
        }
        for (final DirectoryInfoWrapper i : directories) {
            if (i.getPath().equals(filePath)) {
                if (i.deleteFile()) {
                    directories.remove(i);
                }
                return;
            }
        } 
    }
    
    public void createSubdirectory() {
        String name = null;
        for (Object i : UIBeanHelper.getRequest().getParameterMap().keySet()) {
            if (((String) i).contains("subdirName")) {
                String potentialName = UIBeanHelper.getRequest().getParameter((String) i);
                if (potentialName.length() > 0) {
                    name = potentialName;
                    break;
                }
            }
        }
        File parent = new File(UIBeanHelper.getRequest().getParameter("parentPath"));
        if (name != null) {
            name = name.replaceAll("[^a-zA-Z0-9 ]", "");
        }
        if (name != null && name.length() > 0) {
            if (parent.exists() && DataManager.createSubdirectory(parent, name, UIBeanHelper.getUserId())) {
                UIBeanHelper.setInfoMessage("Subdirectory " + name + " successfully created");
                files = null;
            }
            else {
                UIBeanHelper.setErrorMessage("Unable to create the subdirectory");
            }
        }
        else {
            UIBeanHelper.setErrorMessage("Please enter a valid subdirectory name");
        }
    }
    
    public String getSelectedTab() {
        String attr = (String) UIBeanHelper.getSession().getAttribute(SELECTED_TAB);
        if (attr == null) {
            return RECENT_JOBS;
        }
        else {
            return attr;
        }
    }
    
    public void syncFiles() {
        DataManager.syncUploadFiles(UIBeanHelper.getUserId());
    }
    
    public void setSelectedTab(String selected) {
        UIBeanHelper.getSession().setAttribute(SELECTED_TAB, selected);
    }
    
    public int getPartitionLength() {
        Context context = Context.getContextForUser(UIBeanHelper.getUserId());
        return ServerConfiguration.instance().getGPIntegerProperty(context, "upload.partition.size", 10000000);
    }
    
    public long getMaxUploadSize() {
        Context context = Context.getContextForUser(UIBeanHelper.getUserId());
        return Long.parseLong(ServerConfiguration.instance().getGPProperty(context, "upload.max.size", "20000000000"));
    }
    
    public String getUploadWindowName() {
        // The replaceAll is necessary because IE is picky about what characters can be in window names
        return "uploadWindow" + (UIBeanHelper.getRequest().getServerName() +  UIBeanHelper.getUserId()).replaceAll("[^A-Za-z0-9 ]", "");
    }
    
    public boolean getUploadEnabled() {
        String userId = UIBeanHelper.getUserId();
        Context userContext = Context.getContextForUser(userId);
        return ServerConfiguration.instance().getGPBooleanProperty(userContext, "upload.jumploader", false);
    }
}

