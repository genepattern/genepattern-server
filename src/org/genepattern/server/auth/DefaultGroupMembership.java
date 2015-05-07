/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.auth;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class DefaultGroupMembership implements IGroupMembershipPlugin {
    public Set<String> wildcardGroups;
    public HashMap<String, Set<String>> userGroups;
    public HashMap<String, Set<String>> groupUsers;
   
    public DefaultGroupMembership() {
        wildcardGroups = new HashSet<String>();
        userGroups = new HashMap<String, Set<String>>();
        groupUsers = new HashMap<String, Set<String>>();
    }

    protected void addGroup(String groupId) {
        Set<String> members = groupUsers.get(groupId);
        if (members == null) {
            members = new HashSet<String>();
            groupUsers.put(groupId, members);
        }
    }
    
    protected void addUserToGroup(String userId, String groupId) {
        if ("*".equals(userId)) {
            wildcardGroups.add(groupId);
            return;
        }
        Set<String> myGroups = userGroups.get(userId);
        if (myGroups == null) {
            myGroups = new HashSet<String>();
            userGroups.put(userId, myGroups);
        }
        myGroups.add(groupId);
    }
    
    public Set<String> getGroups(String userId) {
        Set<String> rval = null;
        if (userId ==  null) {
            return rval;
        }
        
        rval = userGroups.get(userId);
        if (rval == null) {
            rval = new HashSet<String>();
        }
        rval.addAll(wildcardGroups);
        //return an unmodifiable set
        return Collections.unmodifiableSet(rval);
    }

    public boolean isMember(String userId, String groupId) {
        Set<String> myGroups = getGroups(userId);
        return myGroups.contains(groupId);
    }
}