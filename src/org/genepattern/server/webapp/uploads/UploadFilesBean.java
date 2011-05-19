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
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.server.webapp.jsf.UsersAndGroupsBean;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoCache;

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
        
        public FileInfoWrapper(UploadFile file) {
            this.file = file;
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
        
        public String getKind() {
            return file.getKind();
        }
        
        public String getFilename() {
            return file.getName();
        }
        
        // Returns relative URL
        public String getUrl() {
            //return file.getLink();
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

        public String getPath() {
            return file.getPath();
        }
        
        public boolean getPartial() {
            if (file.getName().endsWith(".part")) {
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
         */
        public void deleteFile() {
            boolean deleted = DataManager.deleteFile(file);
            if (deleted) {
                UIBeanHelper.setInfoMessage("Deleted file: "+file.getName());
                //remove the deleted item from the view
                UploadFilesBean.this.files.remove(file);
            }
            else {
                UIBeanHelper.setErrorMessage("Error deleting file: "+file.getName());
            }
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
    private String currentUser;
    private String currentTaskLsid = null;
    private TaskInfo currentTaskInfo = null;
    private Map<String,SortedSet<TaskInfo>> kindToTaskInfo;
    private UploadDirectory uploadDir;
    
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
    
    public List<FileInfoWrapper> getFiles() {
        if (files == null) {
            initFiles(); 
        }
        return files;
    }

    public UploadDirectory getUploadDir() {
        if (uploadDir == null) {
            uploadDir = new UploadDirectory("UploadedFiles");
            uploadDir.setUploadFiles(this.getFiles());
            return uploadDir;
        }
        else {
            return uploadDir;
        } 
    }
   
    public List<UploadDirectory> getAvailableDirectories() {
        List<UploadDirectory> dirs = new ArrayList<UploadDirectory>();
        dirs.add(this.getUploadDir());
        return dirs;
    }
    
    private void initFiles() {
        currentUser = UIBeanHelper.getUserId();
        List<UploadFile> uploadedFiles = new UploadFileDAO().findByUserId(currentUser);
        files = new ArrayList<FileInfoWrapper>();
        for(UploadFile file : uploadedFiles) {
            files.add(new FileInfoWrapper(file));
        }
        initModuleMenuItems();
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
    
    public String getSelectedTab() {
        String attr = (String) UIBeanHelper.getSession().getAttribute(SELECTED_TAB);
        if (attr == null) {
            return RECENT_JOBS;
        }
        else {
            return attr;
        }
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

