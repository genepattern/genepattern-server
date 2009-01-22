package org.genepattern.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.genepattern.server.auth.GroupPermission;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.server.auth.GroupPermission.Permission;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;

/**
 * Common interface for getting and setting user and group access permissions for job results.
 * Intended to isolate DAO from JSF beans; and to encode all permissions rules in on place.
 * 
 * By convention, the access permissions for a job result are based on the ownership and access permissions of the root job. 
 * This is implemented in this class; not implemented in AnalysisDAO.
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
    private boolean isPublic = false;
    private boolean isShared = false;
    
    private Permission publicAccessPermission = GroupPermission.Permission.NONE;
    private List<GroupPermission> nonPublicPermissions = null;
    private List<GroupPermission >jobResultPermissionsWithGroups = null;
    private List<GroupPermission> jobResultPermissionsNoGroups = null;
    
    private int rootJobNo;
    private String rootJobOwner;
    
    public PermissionsHelper(String userId, int jobNo) {
        this.currentUser = userId;
        this.isAdmin = AuthorizationHelper.adminJobs(currentUser);

        AnalysisDAO ds = new AnalysisDAO();
        this.rootJobNo = ds.getRootJobNumber(jobNo);
        this.rootJobOwner = ds.getJobOwner(rootJobNo);
        this.isOwner = this.currentUser.equals(this.rootJobOwner);
        this.canSetPermissions = this.isOwner;
        
        if (isAdmin || isOwner) {
            canRead = true;
            canWrite = true;
        }
        init(ds);
    }
    
    private void init(AnalysisDAO ds)  {
        Set<GroupPermission>  groupPermissions = ds.getGroupPermissions(rootJobNo);

        IGroupMembershipPlugin groupMembership = UserAccountManager.instance().getGroupMembership();
        for(GroupPermission gp : groupPermissions) {
            if ("public".equals(gp.getGroupId())) {
                this.isPublic = true;
                this.publicAccessPermission = gp.getPermission();
            }

            if (groupMembership.isMember(currentUser, gp.getGroupId())) {
                if (gp.getPermission().getWrite()) {
                    this.canWrite = true;
                }
                if (gp.getPermission().getRead()) {
                    this.canRead = true;
                }
            }
        }
        
        initJobResultPermissions(ds);
        initNonPublicPermissions();
    }
    
    private void initJobResultPermissions(AnalysisDAO ds) {
        Set<String> groups = UserAccountManager.instance().getGroupMembership().getGroups(currentUser);

        Set<GroupPermission>  groupPermissions = ds.getGroupPermissions(rootJobNo);
        SortedSet<GroupPermission> sortedNoGroups = new TreeSet<GroupPermission>(groupPermissions);
        jobResultPermissionsNoGroups = new ArrayList<GroupPermission>(sortedNoGroups);

        //get all of the groups which aren't in group permissions
        Set<String> groupsToAdd = new HashSet<String>();
        groupsToAdd.addAll(groups);
        for(GroupPermission gp : groupPermissions) {
            String groupId = gp.getGroupId();
            groupsToAdd.remove(groupId);
        }
        for(String groupId : groupsToAdd) {
            groupPermissions.add(new GroupPermission(groupId, GroupPermission.Permission.NONE));
        }
        
        //sorted by group
        SortedSet<GroupPermission> sorted = new TreeSet<GroupPermission>(groupPermissions); 
        jobResultPermissionsWithGroups = new ArrayList<GroupPermission>( sorted );
    }

    private void initNonPublicPermissions() {
        List<GroupPermission> groupPermissions = getJobResultPermissions(true);
        Set<GroupPermission> publicGroupPermissions = new HashSet<GroupPermission>();
        for(GroupPermission gp : groupPermissions) {
            if ("public".equals(gp.getGroupId())) {
                publicGroupPermissions.add(gp);
            }
            if (gp.getPermission().getRead()) {
                isShared = true;
            }
        }
        groupPermissions.removeAll(publicGroupPermissions);
        //sorted by group
        SortedSet<GroupPermission> sorted = new TreeSet<GroupPermission>(groupPermissions); 
        nonPublicPermissions =  new ArrayList<GroupPermission>( sorted ); 
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
     * Is the current job read or write accessible by members of the 'public' group.
     * @return
     */
    public boolean isPublic() {
        return isPublic;        
    }
    
    /**
     * Is the current job read or write accessible by anyone other than the owner?
     * @return
     */
    public boolean isShared() {
        return isShared;
    }
    
    /**
     * Get the public access permission flag (None, Read, or Read_Write)
     * @return
     */
    public Permission getPublicAccessPermission() {
        //Note: if more than one public group is involved (currently not implemented), user the most permissive
        return publicAccessPermission;
    }

    /**
     * Get the list of all non-public group permissions for the job.
     * @return
     */
    public List<GroupPermission> getNonPublicPermissions() {
        return nonPublicPermissions;
    }
    
    
    /**
     * Get the list of group permissions for the job, include any groups which the current user is a member of, 
     * even if permissions have not been set for those groups.
     * 
     * @param includeUsersGroups - if true add groups of which the currentUser is a member, and for which access permissions are not set.
     * @return
     */
    public List<GroupPermission> getJobResultPermissions(boolean includeUsersGroups) {
        if (includeUsersGroups) {
            return jobResultPermissionsWithGroups;
        }
        else {
            return jobResultPermissionsNoGroups;
        }
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
            throw new Exception("Insufficient permissions to change access permission for job: "+rootJobNo);
        }
        AnalysisDAO ds = new AnalysisDAO();
        ds.setGroupPermissions(rootJobNo, permissions);
    }
}
