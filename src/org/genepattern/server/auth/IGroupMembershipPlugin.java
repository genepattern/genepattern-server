package org.genepattern.server.auth;

import java.util.Set;

/**
 * User and Group membership plugin.
 * 
 * @author pcarr
 */
public interface IGroupMembershipPlugin {
    /**
     * Get the set of groups which this GenePattern user is a member of.
     * @param user
     * @return a Set of zero or more Groups.
     */
    Set<String> getGroups(String userId);
    
    /**
     * @param user
     * @param group
     * @return true if the given GenePattern user is a member of the group.
     */
    boolean isMember(String userId, String groupId);
}
