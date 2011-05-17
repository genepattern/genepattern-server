package org.genepattern.server.webapp.uploads;

import java.io.File;
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

/**
 * Created for debugging, 
 * Display direct uploaded files for current user.
 * @author pcarr
 *
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

    public class FileInfoWrapper {
        private UploadFile file;
        
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
        
        public String getUrl() {
            return file.getLink();
        }
        
        public String getFullUrl() {
            return UIBeanHelper.getServer() + "/data/" + file.getPath().replaceAll(" ", "%20");
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
        
        public Collection<TaskInfo> getSendToModules() {
            String kind = file.getKind();
            Collection<TaskInfo> taskInfos =  kindToTaskInfo.get(kind);
            if (taskInfos == null) {
                return Collections.emptySet();
            }
            return taskInfos;
        }

        /**
         * Get the list of input parameters which accept this file as input.
         * @param file
         * @return
         */
        public List<ParameterInfo> getSendToParameters() { 
            if (currentTaskInfo == null) {
                return Collections.emptyList();
            }
            
            //nice to have a map of kind -> inputParameters for the current task
            String kind = file.getKind();
            return currentTaskInfo._getSendToParameterInfos(kind); 
        }
        
        
        //--- JSF actions
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

    private void initModuleMenuItems() {
        kindToTaskInfo = new HashMap<String, SortedSet<TaskInfo>>();
        
        AdminDAO adminDao = new AdminDAO();
        if (currentTaskLsid != null) {
            currentTaskInfo = adminDao.getTask(currentTaskLsid, currentUser);
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

