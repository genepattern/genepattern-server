package org.genepattern.server.webapp.jsf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;
import org.genepattern.server.PermissionsHelper;
import org.genepattern.server.auth.GroupPermission;
import org.genepattern.server.auth.GroupPermission.Permission;

/**
 * Backing bean for viewing and editing access permissions for a job result.
 * 
 * Should be request scope.
 * @author pcarr
 */
public class JobPermissionsBean {
    private static Logger log = Logger.getLogger(JobPermissionsBean.class);

    private int jobId = -1;
    private Permission publicAccessPermission;
    private List<GroupPermission> groupAccessPermissions;
    
    private boolean isPublic = false;
    private boolean isShared = false;
    private boolean showPermissionsDiv = false;

    public JobPermissionsBean() {
    }
    
    /**
     * Load (or reload) the values from the database. Requires a valid jobId.
     */
    private void reset() { 
        String currentUserId = UIBeanHelper.getUserId();
        PermissionsHelper permissionsHelper = new PermissionsHelper(currentUserId, jobId);
        this.isPublic = permissionsHelper.isPublic();
        this.isShared = permissionsHelper.isShared();
        this.publicAccessPermission = permissionsHelper.getPublicAccessPermission();
        
        List<GroupPermission> currentPermissions = permissionsHelper.getNonPublicPermissions();
        //copy
        groupAccessPermissions = new ArrayList<GroupPermission>();
        for (GroupPermission gp : currentPermissions) {
            groupAccessPermissions.add(new GroupPermission(gp.getGroupId(), gp.getPermission()));
        }
    }
    
    public void setJobId(int jobId) {
        this.jobId = jobId;
        reset();
    }
    
    public Permission getPublicAccessPermission() {
        return publicAccessPermission;
    }
    
    public void setPublicAccessPermission(Permission p) {
        this.publicAccessPermission = p;
    }
    
    public int getNumGroupAccessPermissions() {
        return groupAccessPermissions == null ? 0 : groupAccessPermissions.size();
    }

    public List<GroupPermission> getGroupAccessPermissions() {
        return groupAccessPermissions;
    }
    
    public boolean isPublic() {
        return isPublic;
    }

    public boolean isShared() {
        return isShared;
    }

    public boolean isShowPermissionsDiv() {
        return showPermissionsDiv;
    }
    
    public void setShowPermissionsDiv(boolean b) {
        this.showPermissionsDiv = b;
    }
    
    /**
     * Process request parameters (from form submission) and update the access permissions for the current job.
     * Only the owner of a job is allowed to change its permissions.
     */
    public String savePermissions() { 
        // parse request parameters from jobResultsPermissionsForm (see jobSharing.xhtml)
        // JSF for public permission
        // generated html for the groups
        // name="jobAccessPerm:#{groupPermission.groupId}" value="#{groupPermission.permission.flag}"
        
        //NOTE: don't edit the jobSharing.xhtml without also editing this page 
        //    in other words, DON'T REUSE THIS CODE in another page unless you know what you are doing
        Set<GroupPermission> updatedPermissions = new HashSet<GroupPermission>();
        if (publicAccessPermission != Permission.NONE) {
            updatedPermissions.add(new GroupPermission("public", publicAccessPermission));
        }
        Map<String,String[]> requestParameters = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterValuesMap();
        for(String name : requestParameters.keySet()) {
            int idx = name.indexOf("jobAccessPerm:");
            if (idx >= 0) {
                idx += "jobAccessPerm:".length();
                String groupId = name.substring(idx);
                Permission groupAccessPermission = Permission.NONE;
                
                String permFlagStr = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(name);
                try {
                    groupAccessPermission = Permission.valueOf(permFlagStr);
                }
                catch (IllegalArgumentException e) {
                    handleException("Ignoring permissions flag: "+permFlagStr, e);
                    return "error";
                    
                }
                catch (NullPointerException e) {
                    handleException("Ignoring permissions flag: "+permFlagStr, e);
                    return "error";
                    
                }
                if (groupAccessPermission != Permission.NONE) {
                    //ignore NONE
                    updatedPermissions.add(new GroupPermission(groupId, groupAccessPermission));
                }
            }
        }
        
        try {
            String currentUserId = UIBeanHelper.getUserId();
            PermissionsHelper permissionsHelper = new PermissionsHelper(currentUserId, jobId);
            permissionsHelper.setPermissions(updatedPermissions);
            reset();
            return "success";
        }
        catch (Exception e) {
            handleException("You are not authorized to change the permissions for this job", e);
            return "error";
        }
    }

    private void handleException(String message, Exception e) {
        log.error(message, e);
        UIBeanHelper.setErrorMessage(message);
    }
}
