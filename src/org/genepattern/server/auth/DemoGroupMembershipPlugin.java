/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.auth;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Demo implementation of GroupMembershipPlugin interface.
 * 
 * This implementation defines a single group, ADMIN, with members, all usernames which begin
 * with the letter 'p' regardless of case. 
 * 
 * @author pcarr
 */
public class DemoGroupMembershipPlugin implements IGroupMembershipPlugin {
    private Set<String> empty;
    private Set<String> pGroups;
    private Set<String> allGroups;
    
    public DemoGroupMembershipPlugin() {
        empty = new HashSet<String>();
        pGroups = new HashSet<String>();
        pGroups.add("ADMIN");
        allGroups = new HashSet<String>();
        allGroups.add("dev-group");
        allGroups.add("testers");
        
        pGroups.addAll(allGroups);
    }

    public Set<String> getGroups(String userId) {
        if (userId == null) {
            userId = "";
        }
        if (userId.toLowerCase().startsWith("u")) {
            // users matching the pattern u* are not in any group
            return empty;
        }
        if (userId.toLowerCase().startsWith("p")) {
            return Collections.unmodifiableSet(pGroups);
        }
        return Collections.unmodifiableSet(allGroups);
    }

    public boolean isMember(String userId, String groupId) {
        if (userId == null) {
            userId = "";
        }
        if (userId.toLowerCase().startsWith("u")) {
            // users matching the pattern u* are not in any group
            return false;
        }
        if (allGroups.contains(groupId)) {
            return true;
        }
        if (userId.toLowerCase().startsWith("p")) {
            return pGroups.contains(groupId);
        }
        return false;
    }

}
