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
 * @author pcarr
 *
 */
public class GroupMembershipWrapper implements IGroupMembershipPlugin {
    private static final String wildCardGroupId = GroupPermission.PUBLIC;

    private IGroupMembershipPlugin groupMembership;
    
    public GroupMembershipWrapper(IGroupMembershipPlugin groupMembership) {
        this.groupMembership = groupMembership;
    }

    public Set<String> getGroups(String userId) {
        Set<String> wrappedGroups = new HashSet<String>(this.groupMembership.getGroups(userId)); 
        wrappedGroups.add(wildCardGroupId);
        return Collections.unmodifiableSet(wrappedGroups);
    }

    public boolean isMember(String userId, String groupId) {
        if (wildCardGroupId.equals(groupId)) {
            return true;
        }
        return groupMembership.isMember(userId, groupId);
    }
}
