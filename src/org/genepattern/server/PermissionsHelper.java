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
 * Common interface for getting and setting user and group access permissions for job results.
 * Intended to isolate DAO from JSF beans; and to encode all permissions rules in on place.
 * 
 * By convention, the access permissions for a job result are based on the ownership and access permissions of the root job. 
 * 
 * @author pcarr
 *
 */
public class PermissionsHelper {
    private String currentUser = null;
    private boolean isAdmin = false;
    private boolean isOwner = false;
    private boolean canRead = false;
    private boolean canWrite = false;
    private boolean canSetPermissions = false;
    
    private JobInfo rootJobInfo;
    private String rootJobOwner;
    
    public PermissionsHelper(String userId, int jobNo) {
        this.currentUser = userId;
        this.isAdmin = AuthorizationHelper.adminJobs(currentUser);

        AnalysisDAO ds = new AnalysisDAO();
        int rootJobNo = ds.getRootJobNumber(jobNo);
        this.rootJobInfo = ds.getJobInfo(rootJobNo);
        init(ds);
    }

    /**
     * Suggested use: create one instance per HTTP request.
     * @param userId
     * @param jobInfo
     */
    public PermissionsHelper(String userId, JobInfo jobInfo) {
        this.currentUser = userId;
        this.isAdmin =  AuthorizationHelper.adminJobs(currentUser);

        this.rootJobInfo = jobInfo;
        AnalysisDAO ds = new AnalysisDAO();
        int rootJobId = ds.getRootJobNumber(jobInfo.getJobNumber());
        if (rootJobId != jobInfo.getJobNumber()) {
            this.rootJobInfo = ds.getJobInfo(rootJobId);
        }        
        init(ds);
    }
    
    private void init(AnalysisDAO ds)  {
        this.rootJobOwner = rootJobInfo.getUserId();
        
        this.isOwner = this.currentUser.equals(this.rootJobOwner);
        this.canSetPermissions = this.isOwner;
        
        if (isAdmin || isOwner) {
            canRead = true;
            canWrite = true;
        }
        else {
            Set<GroupPermission>  groupPermissions = ds.getGroupPermissions(rootJobInfo.getJobNumber());
            IGroupMembershipPlugin groupMembership = UserAccountManager.instance().getGroupMembership();
            for(GroupPermission gp : groupPermissions) {
                if (groupMembership.isMember(currentUser, gp.getGroupId())) {
                    if (gp.getPermission().getWrite()) {
                        this.canWrite = true;
                    }
                    if (gp.getPermission().getRead()) {
                            this.canRead = true;
                    }
                }
            }
        }
    }

 
    /**
     * Does the given user have read access to the given job.
     * 
     * @param currentUser
     * @param jobInfo
     * @return
     */
    public boolean canReadJob() {
        return this.canRead;
    }

    /**
     * Does the given user have write access to the given job.
     * 
     * @param userId
     * @param jobInfo
     * @return
     */
    public boolean canWriteJob() {
        return this.canWrite;
    }

    public boolean canSetJobPermissions() {
        return this.canSetPermissions;
    }
    
    /**
     * Get the list of group permissions for the job, include any groups which the current user is a member of, 
     * even if permissions have not been set for those groups.
     * 
     * @param includeUsersGroups - if true add groups of which the currentUser is a member, and for which access permissions are not set.
     * @return
     */
    public List<GroupPermission> getJobResultPermissions(boolean includeUsersGroups) {
        Set<String> groups = UserAccountManager.instance().getGroupMembership().getGroups(currentUser);

        AnalysisDAO ds = new AnalysisDAO();
        Set<GroupPermission>  groupPermissions = ds.getGroupPermissions(rootJobInfo.getJobNumber());
        
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
     * Change the access permissions for the current job.
     * <br />
     * Note: only the owner of a job is allowed to change the permissions.<br />
     * Note: changing access permissions on a child job sets the permissions for the root job and all siblings.
     * 
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
        ds.setGroupPermissions(rootJobInfo.getJobNumber(), permissions);
    }
}
