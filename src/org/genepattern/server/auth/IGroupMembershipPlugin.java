/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
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
     * @param user, must handle null value.
     * @return a Set of zero or more Groups.
     */
    Set<String> getGroups(String userId);
    
    /**
     * @param user, must handle null value
     * @param group, must handle null value
     * @return true if the given GenePattern user is a member of the group.
     */
    boolean isMember(String userId, String groupId);
    
    /**
     * Get the set of users in the given group.
     * @param groupId
     * @return a Set of zero or more userId
     */
    Set<String> getUsers(String groupId);
}
