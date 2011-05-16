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

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
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
    private String currentUser;
    private List<FileInfoWrapper> files;
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
    
    /**
     * Delete a file from the user's home dir
     * 
     * @param ae
     */
    public void deleteFile(ActionEvent ae) {
        String filenameParam = UIBeanHelper.getRequest().getParameter("filename");
        String dirnameParam = UIBeanHelper.getRequest().getParameter("path");
        
        Context context = Context.getContextForUser(UIBeanHelper.getUserId());
        File userUploadDir = ServerConfiguration.instance().getUserUploadDir(context);
        File theFile = new File(userUploadDir, filenameParam);
        boolean success = false;
        if (theFile.canRead()) {
            success = theFile.delete();
        }
        
        if (success) {
            log.debug("Deleted from upload directories: '" + dirnameParam + "/" + filenameParam + "'");
            UIBeanHelper.setInfoMessage("Deleted from upload directories: '" + filenameParam + "'");
        }
        
    }
    
    public void saveFile(ActionEvent ae) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        ServletContext servletContext = (ServletContext) externalContext.getContext();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();
        HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
        
        String path = request.getParameter("path");
        File fileObj = new File(path);

        try {
            FileDownloader.serveFile(servletContext, request, response, true, FileDownloader.ContentDisposition.ATTACHMENT, fileObj);
        }
        catch (IOException e) {
            log.error("Error downloading file to client, file="+fileObj.getPath(), e);
            UIBeanHelper.setErrorMessage("Error downloading "+fileObj.getName()+": "+e.getLocalizedMessage());
        }
        facesContext.responseComplete();
    }

}

