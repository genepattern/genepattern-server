package org.genepattern.server.webapp.uploads;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.server.DataManager;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.dm.GpDirectoryNode;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.userupload.UserUploadManager;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.user.UserEntry;
import org.genepattern.server.webapp.jsf.UIBeanHelper;

public class UserControlPanelBean {
    private static Logger log = Logger.getLogger(UserControlPanelBean.class);
    
    private UserEntry userEntry = null;
    private String username = UIBeanHelper.getUserId();
    private List<GpFilePath> files = null;
    
    public UserControlPanelBean() {
        
    }
    
    public List<GpFilePath> getFiles() {
        if (files == null) {
            files = getFiles(username);
        }
        
        return files;
    }
    
    public void selectUser() {
        String passedId = (String) UIBeanHelper.getRequest().getParameter("selectuser:select_username");
        if (passedId == null) {
            log.error("The is passed into selectUser() is null");
            return;
        }
        
        files = null;
        userEntry = null;
        username = passedId;
        getUserEntry();
        getFiles();
    }
    
    public UserEntry getUserEntry() {
        if (userEntry != null) {
            return userEntry;
        }
        else {
            User user = new UserDAO().findById(username);
            if (user == null) {
                return null;
            }
            else {
                userEntry = new UserEntry(user);
                Set<String> groups = UserAccountManager.instance().getGroupMembership().getGroups(username);
                for (String groupId : groups) {
                    userEntry.addGroup(groupId);
                }
                return userEntry;
            }
        }
    }
    
    /**
     * Get the list o files for a specific user
     * This method doesn't cache anything
     * @param user
     * @return
     */
    public List<GpFilePath> getFiles(String user) {
        List<GpFilePath> uploadedFiles;
        try {
            GpDirectoryNode root = UserUploadManager.getFileTree(GpContext.getContextForUser(user));
            uploadedFiles = root.getAllFilePaths();
        }
        catch (Exception e) {
            log.error("Error getting all file paths for " + user + ": " + e.getMessage(), e);
            return files;
        }
        
        files = uploadedFiles;
        return files;
    }
    
    public void syncFiles() {
        DataManager.syncUploadFiles(username);
    }

}
