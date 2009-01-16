package org.genepattern.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.genepattern.server.auth.GroupPermission;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;

/**
 * Common interface for getting and setting user and group access permissions for job results, pipelines and modules.
 * Initially designed to isolate DAO from JSF beans.
 * 
 * @author pcarr
 *
 */
public class PermissionsHelper {
    private String currentUser = null;
    private JobInfo jobInfo = null;

    public PermissionsHelper(String userId, JobInfo jobInfo) {
        this.currentUser = userId;
        this.jobInfo = jobInfo;
    }

    /**
     * Does the given user have read access to the given job.
     * 
     * @param currentUser
     * @param jobInfo
     * @return
     */
    public boolean canReadJob() {
        return checkPermission(currentUser, jobInfo, false);
    }

    /**
     * Does the given user have write access to the given job.
     * 
     * @param userId
     * @param jobInfo
     * @return
     */
    public boolean canWriteJob() {
        return checkPermission(currentUser, jobInfo, true);
    }

    public boolean canSetJobPermissions() {
        AnalysisDAO ds = new AnalysisDAO();
        String ownerUserId = ds.getJobOwner(jobInfo.getJobNumber());
        if (!currentUser.equals(ownerUserId)) {
            return false;
        }
        return true;
    }

    private boolean checkPermission(String userId, JobInfo jobInfo, boolean checkWrite) {
        if (AuthorizationHelper.adminJobs(userId)) {
            return true;
        }
        String jobOwner = jobInfo.getUserId();
        if (userId.equals(jobOwner)) {
            return true;
        }
        AnalysisDAO ds = new AnalysisDAO();
        Set<GroupPermission>  groupPermissions = ds.getGroupPermissions(jobInfo.getJobNumber());
        IGroupMembershipPlugin groupMembership = UserAccountManager.instance().getGroupMembership();
        for(GroupPermission gp : groupPermissions) {
            if (groupMembership.isMember(userId, gp.getGroupId())) {
                if (checkWrite) {
                    if (gp.getPermission().getWrite()) {
                        return true;
                    }
                }
                else {
                    if (gp.getPermission().getRead()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Get the list of group permissions for the job, include any groups which the current user is a member of, 
     * even if permissions have not been set for those groups.
     * 
     * @param jobNumber
     * @return
     */
    public List<GroupPermission> getJobResultPermissions() {
        return getJobResultPermissions(false);
    }
    
    public List<GroupPermission> getJobResultPermissions(boolean includeUsersGroups) {
        Set<String> groups = UserAccountManager.instance().getGroupMembership().getGroups(currentUser);

        AnalysisDAO ds = new AnalysisDAO();
        Set<GroupPermission>  groupPermissions = ds.getGroupPermissions(jobInfo.getJobNumber());
        
        //add any groups the user is a member of for which no permissions are set
        if (includeUsersGroups) {
            Set<String> pg = new HashSet<String>(); //groups with permissions set
            for(GroupPermission gp : groupPermissions) {
                pg.add(gp.getGroupId());
            }
            groups.removeAll(pg);
            for(String groupId : groups) {
                groupPermissions.add(new GroupPermission(groupId, GroupPermission.Permission.NONE));
            }
        }
        
        //sorted by group
        SortedSet<GroupPermission> sorted = new TreeSet<GroupPermission>(groupPermissions); 
        return new ArrayList<GroupPermission>( sorted );
    }

    /**
     * Change the access permissions for the given job.
     * 
     * Note: only the owner of a job is allowed to change the permissions.
     * 
     * @param jobId
     * @param permissions
     * 
     * @throws Exception - if current user is not authorized to change the permissions
     */
    public void setPermissions(Set<GroupPermission> permissions) 
        throws Exception 
    {
        if (!canSetJobPermissions()) {
            throw new Exception("Insufficient permissions: Only job owner can change group access permissions!");
        }
        AnalysisDAO ds = new AnalysisDAO();
        ds.setGroupPermissions(jobInfo.getJobNumber(), permissions);
    }
}
