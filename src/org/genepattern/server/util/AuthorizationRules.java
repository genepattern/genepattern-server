package org.genepattern.server.util;

import org.genepattern.server.domain.TaskMaster;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;

import static org.genepattern.util.GPConstants.*;

public class AuthorizationRules {

    private IAuthorizationManager authManager = AuthorizationManagerFactory.getAuthorizationManager();

    public enum ActionType {
        View, Create, Edit, Delete
    };

    /**
     * Check to see if specific user can do the specified action on the specific object
     * 
     * @param taskMaster
     * @param userId
     * @param actionType
     * @return
     */
    public boolean isAllowed(TaskMaster taskMaster, String userId, ActionType actionType) {

        String owner = taskMaster.getUserId();
        int accessId = taskMaster.getAccessId();

        switch (actionType) {
        case View:
            return authManager.checkPermission("administrateServer", userId) || owner.equals(userId)
                    || GPConstants.ACCESS_PUBLIC == accessId;

        case Create:
            return authManager.checkPermission("createTask", userId);

        case Edit:
            return authManager.checkPermission("createTask", userId)
                    && (owner.equals(userId) || GPConstants.ACCESS_PUBLIC == accessId);

        case Delete:
            return authManager.checkPermission("deleteTask", userId) && owner.equals(userId);

        }
        return false;
    }
    
    /**
     * Check to see if specific user can do the specified action on the specific object
     * 
     * @param taskMaster
     * @param userId
     * @param actionType
     * @return
     */
    public boolean isAllowed(JobInfo jobInfo, String userId, ActionType actionType) {

        String owner = jobInfo.getUserId();
         

        switch (actionType) {
        case View:
            return authManager.checkPermission("administrateServer", userId) || owner.equals(userId);

        case Delete:
            return authManager.checkPermission("deleteJob", userId) || owner.equals(userId);

        }
        return false;
    }


}
