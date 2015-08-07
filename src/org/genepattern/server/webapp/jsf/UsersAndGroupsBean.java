/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.jsf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.GroupPermission;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.user.UserEntry;

/**
 * Backing bean for the admin page for displaying all genepattern users and groups.
 * Request scope.
 * 
 * TODO: link permissions to this page so that you can see the permissions that each user has.
 * 
 * @author pcarr
 */
public class UsersAndGroupsBean {
    private static Logger log = Logger.getLogger(UsersAndGroupsBean.class);

    private SortedSet<UserEntry> userEntries;
    private SortedSet<GroupEntry> groupEntries;
    private boolean displayGroups = false;
    
    //toggle whether or not to include the '*' wildcard group on the users and groups page
    //    if this is enabled, the '*' group, which includes all registered users, will be displayed.
    //    Set this to false to help with debugging.
    public final static boolean ignorePublicGroups = true;
  
    public UsersAndGroupsBean() {}
    
    public boolean getDisplayGroups() {
        return displayGroups;
    }
    
    public void display() {
        init();
        displayGroups = true;
    }
    
    private void init() {
        List<User> allUsers = new UserDAO().getAllUsers();

        userEntries = new TreeSet<UserEntry>(new UserEntryComparator());
        groupEntries = new TreeSet<GroupEntry>();
        Map<String,SortedSet<String>> mapGroupIdToUserIds = new HashMap<String,SortedSet<String>>();
        
        for(User user : allUsers) {
            UserEntry userEntry = new UserEntry(user);
            userEntries.add(userEntry);
            Set<String> groupIds = UserAccountManager.instance().getGroupMembership().getGroups(user.getUserId());
            
            for(String groupId : groupIds) {
                userEntry.addGroup(groupId);
                SortedSet<String> userIds = mapGroupIdToUserIds.get(groupId);
                if (userIds == null) {
                    userIds = new TreeSet<String>();
                    mapGroupIdToUserIds.put(groupId, userIds);
                }
                userIds.add(user.getUserId());
            }
        }
        
        //helpers
        for(String groupId : mapGroupIdToUserIds.keySet()) {
            GroupEntry ge = new GroupEntry(groupId);
            if (ignorePublicGroups && GroupPermission.PUBLIC.equalsIgnoreCase(groupId)) {
            }
            else {
                groupEntries.add(ge);
            }
            
            for(String userId : mapGroupIdToUserIds.get(groupId)) {
                ge.addUser(userId);
            }
        }
    }
    
    /**
     * Sort each UserEntry by user_id, ignoring case.
     * @author pcarr
     */
    private static class UserEntryComparator implements Comparator<UserEntry> {
        public int compare(UserEntry o1, UserEntry o2) {
            String s1 = "";
            if (o1 != null && o1.getUser() != null && o1.getUser().getUserId() != null) {
                s1 = o1.getUser().getUserId();
            }
            
            String s2 = "";
            if (o2 != null && o2.getUser() != null && o2.getUser().getUserId() != null) {
                s2 = o2.getUser().getUserId();
            }
            
            int rval = s1.compareToIgnoreCase(s2);
            if (rval == 0) {
                rval = s1.compareTo(s2);
            }
            return rval;
        }
    }

    

    /**
     * Map each group to its members.
     * 
     * @author pcarr
     */
    public static class GroupEntry implements Comparable<GroupEntry> {
        private String groupId;
        private SortedSet<String> users;
        
        public GroupEntry(String groupId) {
            this.groupId = groupId;
            this.users = new TreeSet<String>();
        }
        
        public void addUser(String userId) {
            this.users.add(userId);
        }
        
        public String getGroupId() {
            return groupId;
        }
        
        public List<String> getUsers() {
            return new ArrayList<String>(this.users);
        }

        /**
         * natural sort order based on groupId, ignoring case.
         */
        public int compareTo(GroupEntry arg) {
            String from = this.groupId == null ? "" : this.groupId;
            String to = arg == null ? "" : arg.groupId;
            int rval = from.compareToIgnoreCase(to); 
            if (rval == 0) {
                rval = from.compareTo(to);
            }
            return rval;
        }
    }
    
    public List<UserEntry> getUserEntries() {
        List<UserEntry> rval = new ArrayList<UserEntry>();
        rval.addAll(userEntries);
        return rval;
    }

    public List<GroupEntry> getGroupEntries() {
        return new ArrayList<GroupEntry>(groupEntries);
    }
    
    /**
     * Reload the users and groups database from the file system.
     */
    public void reloadUsersAndGroups() {
        UserAccountManager.instance().refreshUsersAndGroups();
        init();
    }
}
