/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.auth;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Wrapper class which has the effect of adding a 'hidden' group of all GenePattern users to the given
 * IGroupMembershipPlugin.
 * 
 * The intention is to make it easier for implementors of custom group authentication; while still
 * allowing users to share job results, pipelines, and modules with all genepattern users.
 * 
 * TODO: Once the ability to share a job result with a user (in addition to members of a group) is implemented, then 
 *       this functionality can be part of the user permissions logic (aka, share this job result with all (*) users).
 * 
 * @author pcarr
 */
public class GroupMembershipWrapper implements IGroupMembershipPlugin {
    private static final String wildCardGroupId = GroupPermission.PUBLIC;

    private IGroupMembershipPlugin groupMembership;
    
    public GroupMembershipWrapper(IGroupMembershipPlugin groupMembership) {
        this.groupMembership = groupMembership;
    }

    public Set<String> getGroups(String userId) {
        Set<String> groups = null;
        if (groupMembership != null) {
            groups = groupMembership.getGroups(userId);
        }
        if (groups == null) {
            return Collections.emptySet();
        }
        else {
            Set<String> wrappedGroups = new HashSet<String>(groups); 
            wrappedGroups.add(wildCardGroupId);
            return Collections.unmodifiableSet(wrappedGroups);
        }
    }

    public boolean isMember(String userId, String groupId) {
        if (wildCardGroupId.equals(groupId)) {
            return true;
        }
        return groupMembership != null && groupMembership.isMember(userId, groupId);
    }
}
