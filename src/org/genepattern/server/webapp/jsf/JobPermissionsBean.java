/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.jsf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

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
public class JobPermissionsBean implements Serializable {
    private static Logger log = Logger.getLogger(JobPermissionsBean.class);

    private PermissionsHelper _ph = null;

    private int jobId = -1;
    private List<GroupPermission> groupAccessPermissions;
    
    //for displaying read-only summary information (e.g. in Job Results Page)
    private List<String> groupIdsWithFullAccess;
    private List<String> groupIdsWithReadOnlyAccess;

    public JobPermissionsBean(final boolean isAdmin, final String rootJobOwner, final Integer rootJobId) {
        String currentUserId = UIBeanHelper.getUserId();
        PermissionsHelper ph = new PermissionsHelper(isAdmin, currentUserId, jobId, rootJobOwner, rootJobId);
        this._ph = ph;
        reset();
    }
    public JobPermissionsBean(PermissionsHelper ph) {
        this._ph = ph;
        reset();
    }
    
    /**
     * Load (or reload) the values from the database. Requires a valid jobId.
     */
    private void reset() { 
        List<GroupPermission> currentPermissions = this._ph.getNonPublicPermissions();
        //copy
        groupAccessPermissions = new ArrayList<GroupPermission>();
        for (GroupPermission gp : currentPermissions) {
            groupAccessPermissions.add(new GroupPermission(gp.getGroupId(), gp.getPermission()));
        }
        
        SortedSet<String> g_full_access = new TreeSet<String>();
        SortedSet<String> g_read_only_access = new TreeSet<String>();
        for(GroupPermission gp : _ph.getJobResultPermissions(false)) {
            if (gp.getPermission() == Permission.READ_WRITE) {
                g_full_access.add(gp.getGroupId());
            }
            else if (gp.getPermission() == Permission.READ) {
                g_read_only_access.add(gp.getGroupId());
            }
        }
        groupIdsWithFullAccess = new ArrayList<String>(g_full_access);
        groupIdsWithReadOnlyAccess = new ArrayList<String>(g_read_only_access);
    }
    
    public void setJobId(int jobId) {
        this.jobId = jobId;
        reset();
    }
    
    public int getJobId() {
    	return this.jobId;
    }
    
    public Permission getPublicAccessPermission() {
        return _ph.getPublicAccessPermission();
    }
    
    public int getNumGroupAccessPermissions() {
        return groupAccessPermissions == null ? 0 : groupAccessPermissions.size();
    }

    public List<GroupPermission> getGroupAccessPermissions() {
        return groupAccessPermissions;
    }
    
    public boolean isDeleteAllowed() {
        return _ph.canWriteJob();
    }

    public boolean isEditPermissionsAllowed() {
        return _ph.canSetJobPermissions();
    }

    public boolean isPublic() {
        return _ph.isPublic();
    }

    public boolean isShared() {
        return _ph.isShared();
    }
    
    //helpers for read only view on 'Job Results' page
    public String getPermissionsLabel() {
        if (isPublic()) {
            return "Public";
        }
        else if (isShared()) {
            return "Shared";
        }
        return "Private";
    }

    public int getNumGroupsWithFullAccess() {
        return groupIdsWithFullAccess.size();
    }
    
    public List<String> getGroupsWithFullAcess() {
        return Collections.unmodifiableList(groupIdsWithFullAccess);
    }

    public int getNumGroupsWithReadOnlyAccess() {
        return groupIdsWithReadOnlyAccess.size();
    }

    public List<String> getGroupsWithReadOnlyAccess() {
        return Collections.unmodifiableList(groupIdsWithReadOnlyAccess);
    }
}
