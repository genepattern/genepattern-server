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
public class PermissionsManager {
    private String userId = null;

    public PermissionsManager() { 
    }

    public PermissionsManager(String userId) {
        this.userId = userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Does the given user have read access to the given job.
     * 
     * @param userId
     * @param jobInfo
     * @return
     */
    public boolean canReadJob(String userId, JobInfo jobInfo) {
        return checkPermission(userId, jobInfo, false);
    }

    /**
     * Does the given user have write access to the given job.
     * 
     * @param userId
     * @param jobInfo
     * @return
     */
    public boolean canWriteJob(String userId, JobInfo jobInfo) {
        return checkPermission(userId, jobInfo, true);
    }

    public boolean canSetJobPermissions(JobInfo jobInfo) {
        AnalysisDAO ds = new AnalysisDAO();
        String ownerUserId = ds.getJobOwner(jobInfo.getJobNumber());
        if (!userId.equals(ownerUserId)) {
            return false;
        }
        return true;
    }

    private boolean checkPermission(String userId, JobInfo jobInfo, boolean write) {
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
                if (write && gp.getPermission().getWrite()) {
                    return true;
                }
                else if (gp.getPermission().getRead()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public List<GroupPermission> getJobResultPermissions(int jobNumber) {
        Set<String> groups = UserAccountManager.instance().getGroupMembership().getGroups(userId);

        AnalysisDAO ds = new AnalysisDAO();
        Set<GroupPermission>  groupPermissions = ds.getGroupPermissions(jobNumber);
        
        //add any groups the user is a member of for which no permissions are set
        Set<String> pg = new HashSet<String>(); //groups with permissions set
        for(GroupPermission gp : groupPermissions) {
            pg.add(gp.getGroupId());
        }
        groups.removeAll(pg);
        for(String groupId : groups) {
            groupPermissions.add(new GroupPermission(groupId, GroupPermission.Permission.NONE));
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
    public void setPermissions(JobInfo jobInfo, Set<GroupPermission> permissions) 
        throws Exception 
    {
        if (!canSetJobPermissions(jobInfo)) {
            throw new Exception("Insufficient permissions: Only job owner can change group access permissions!");
        }
        AnalysisDAO ds = new AnalysisDAO();
        ds.setGroupPermissions(jobInfo.getJobNumber(), permissions);
    }
}
