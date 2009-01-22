package org.genepattern.server.auth;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class DefaultGroupMembership implements IGroupMembershipPlugin {
    protected static final Set<String> EMPTY_GROUPS = new HashSet<String>();

    public Set<String> wildcardGroups = new HashSet<String>();
    public HashMap<String, Set<String>> userGroups = new HashMap<String, Set<String>>();
    public HashMap<String, Set<String>> groupUsers = new HashMap<String, Set<String>>();

    protected void init() {
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
            rval = EMPTY_GROUPS;
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