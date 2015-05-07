/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
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
}
