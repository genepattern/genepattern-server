package org.genepattern.server.webapp.jsf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;

/**
 * Backing bean for the admin page for displaying all genepattern users and groups.
 * Request scope.
 * 
 * @author pcarr
 */
public class UsersAndGroupsBean {
    private SortedSet<UserEntry> userEntries;
    private SortedSet<GroupEntry> groupEntries;
    private GroupEntry adminGroupEntry;
    private GroupEntry publicGroupEntry;
    
    private static String ADMIN_GROUP_ID = "administrators";
    private static String PUBLIC_GROUP_ID = "public";
        
    public UsersAndGroupsBean() {
        init();
    }
    
    private void init() {
        List<User> allUsers = new UserDAO().getAllUsers();
        IGroupMembershipPlugin groupMembership = UserAccountManager.instance().getGroupMembership();

        userEntries = new TreeSet<UserEntry>(new UserEntryComparator());
        groupEntries = new TreeSet<GroupEntry>();
        Map<String,SortedSet<String>> mapGroupIdToUserIds = new HashMap<String,SortedSet<String>>();
        
        for(User user : allUsers) {
            UserEntry userEntry = new UserEntry(user);
            userEntries.add(userEntry);
            Set<String> groupIds = groupMembership.getGroups(user.getUserId());
            
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
            if (PUBLIC_GROUP_ID.equalsIgnoreCase(groupId)) {
                publicGroupEntry = ge;
            }
            else if (ADMIN_GROUP_ID.equalsIgnoreCase(groupId)) {
                adminGroupEntry = ge;
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
            
            return s1.compareToIgnoreCase(s2);
        }
    }

    /**
     * Map each user to its groups, excluding 'public' from the list.
     */
    public static class UserEntry {
        private User user;
        private SortedSet<String> groups;
        private boolean isAdmin = false;
        
        public UserEntry(User user) {
            this.user = user;
            this.groups = new TreeSet<String>();
        }
        
        public void addGroup(String groupId) {
            if (PUBLIC_GROUP_ID.equalsIgnoreCase(groupId)) {
                return;
            }
            groups.add(groupId);
            if (ADMIN_GROUP_ID.equalsIgnoreCase(groupId)) {
                isAdmin = true;
            }
        }
        
        public User getUser() {
            return user;
        }
        
        public List<String> getGroups() {
            return new ArrayList<String>(groups);
        }
        
        public boolean isAdmin() {
            return isAdmin;
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
            return from.compareToIgnoreCase(to);
        }
    }
    
    public List<UserEntry> getUserEntries() {
        List<UserEntry> rval = new ArrayList<UserEntry>();
        rval.addAll(userEntries);
        return rval;
    }

    public GroupEntry getPublicGroupEntry() {
        return publicGroupEntry;
    }

    public GroupEntry getAdminGroupEntry() {
        return adminGroupEntry;
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
