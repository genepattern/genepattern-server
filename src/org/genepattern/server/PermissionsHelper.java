/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.genepattern.server.auth.GroupPermission;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.server.auth.GroupPermission.Permission;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;

/**
 * Common interface for getting and setting user and group access permissions for job results.
 * Intended to isolate DAO from JSF beans; and to encode all permissions rules in one place.
 * 
 * By convention, the access permissions for a job result are based on the ownership and access permissions of the root job. 
 * This is implemented in this class; not implemented in AnalysisDAO.
 * 
 * @author pcarr
 *
 */
public class PermissionsHelper implements Serializable {
    private final HibernateSessionManager mgr;
    
    //the user viewing or editing the job
    private String currentUser = null;
    //is the current user the owner of the job
    private boolean isOwner = false;
    //can the current user read (view) the job
    private boolean canRead = false;
    //can the current user edit (write) the job
    private boolean canWrite = false;
    //can the current user edit the job permissions
    private boolean canSetPermissions = false;
    //is the job public
    private boolean isPublic = false;
    //is the job shared
    private boolean isShared = false;
    
    private Permission publicAccessPermission = GroupPermission.Permission.NONE;
    private List<GroupPermission> nonPublicPermissions = null;
    private List<GroupPermission> jobResultPermissionsWithGroups = null;
    private List<GroupPermission> jobResultPermissionsNoGroups = null;
    
    private int rootJobNo;
    private String rootJobOwner;

    /** @deprecated pass in a valid Hibernate session */
    public PermissionsHelper(final boolean _isAdmin, final String _userId, final int _jobNo) {
        this(org.genepattern.server.database.HibernateUtil.instance(),
                _isAdmin, _userId, _jobNo);
    }
    
    public PermissionsHelper(final HibernateSessionManager mgr, final boolean _isAdmin, final String _userId, final int _jobNo) {
        this.mgr=mgr;
        AnalysisDAO ds = new AnalysisDAO(mgr);
        final int _rootJobNo = ds.getRootJobNumber(_jobNo);
        final String _rootJobOwner = ds.getJobOwner(_rootJobNo);
        init(_isAdmin, _userId, _jobNo, _rootJobOwner, _rootJobNo, ds);
    }
    
    /** @deprecated pass in a valid Hibernate session */
    public PermissionsHelper(final boolean _isAdmin, final String _userId, final int _jobNo, final String _rootJobOwner, final int _rootJobNo) {
        this(org.genepattern.server.database.HibernateUtil.instance(), 
                _isAdmin, _userId, _jobNo, _rootJobOwner, _rootJobNo);
    }

    public PermissionsHelper(final HibernateSessionManager mgr, final boolean _isAdmin, final String _userId, final int _jobNo, final String _rootJobOwner, final int _rootJobNo) {
        this.mgr=mgr;
        init(_isAdmin, _userId, _jobNo, _rootJobOwner, _rootJobNo, (AnalysisDAO)null);
    }
    
    private void init(final boolean _isAdmin, final String _userId, final int _jobNo, final String _rootJobOwner, final int _rootJobNo, AnalysisDAO ds) {
        this.currentUser = _userId;
        this.rootJobOwner = _rootJobOwner;
        this.rootJobNo = _rootJobNo;
        this.isOwner = this.currentUser != null && this.currentUser.equals(this.rootJobOwner);
        this.canSetPermissions = this.isOwner;
        
        if (_isAdmin || isOwner) {
            canRead = true;
            canWrite = true;
        }
        init(mgr, ds);
    }
    
    private void init(HibernateSessionManager mgr, AnalysisDAO ds)  {
        if (ds == null) {
            ds = new AnalysisDAO(mgr);
        }
        Set<GroupPermission>  groupPermissions = ds.getGroupPermissions(rootJobNo);

        IGroupMembershipPlugin groupMembership = UserAccountManager.instance().getGroupMembership();
        for(GroupPermission gp : groupPermissions) {
            if (GroupPermission.PUBLIC.equals(gp.getGroupId())) {
                this.isPublic = true;
                this.publicAccessPermission = gp.getPermission();
                if (gp.getPermission().getWrite()) {
                    this.canWrite = true;
                }
                if (gp.getPermission().getRead()) {
                    this.canRead = true;
                }
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
        
        initJobResultPermissions(groupMembership, groupPermissions);
        initNonPublicPermissions();
    }
    
    private void initJobResultPermissions(IGroupMembershipPlugin groupMembership, Set<GroupPermission> groupPermissions) {
        Set<String> groups = groupMembership.getGroups(currentUser);
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
            if (GroupPermission.PUBLIC.equals(gp.getGroupId())) {
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
     * Does the user have read access to the job.
     * 
     * @return
     */
    public boolean canReadJob() {
        return this.canRead;
    }

    /**
     * Does the user have write access to the job.
     * 
     * @return
     */
    public boolean canWriteJob() {
        return this.canWrite;
    }

    /**
     * Can the user set permissions on the job.
     * @return
     */
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
        //Note: if more than one public group is involved (currently not implemented), use the most permissive
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
        
        // need to reset the isShared property based on the new permissions
        ds.setGroupPermissions(rootJobNo, permissions);
        isShared = false;
        for(GroupPermission gp : permissions) {
            if (gp.getPermission().getRead()) {
                isShared = true;
            }
        }
    }
}
