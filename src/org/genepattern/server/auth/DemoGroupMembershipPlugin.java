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
    
    public DemoGroupMembershipPlugin() {
        empty = new HashSet<String>();
        pGroups = new HashSet<String>();
        pGroups.add("ADMIN");
    }

    public Set<String> getGroups(String userId) {
        if (userId == null) {
            userId = "";
        }
        if (userId.toLowerCase().startsWith("p")) {
            return Collections.unmodifiableSet(pGroups);
        }
        return Collections.unmodifiableSet(empty);
    }

    public boolean isMember(String userId, String groupId) {
        if (userId == null) {
            userId = "";
        }
        if (userId.toLowerCase().startsWith("p")) {
            return pGroups.contains(groupId);
        }
        
        return false;
    }

}
