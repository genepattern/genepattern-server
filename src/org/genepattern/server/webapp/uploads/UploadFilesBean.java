package org.genepattern.server.webapp.uploads;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.UploadFile;
import org.genepattern.server.domain.UploadFileDAO;
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
    
    public class FileInfoWrapper {
        private UploadFile file;
        public FileInfoWrapper(UploadFile file) {
            this.file = file;
        }
        
        public UploadFile getFile() {
            return file;
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
    

}

