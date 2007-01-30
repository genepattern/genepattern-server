package org.genepattern.server.util;

import java.net.MalformedURLException;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.TaskMaster;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.util.LSIDUtil;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;

import static org.genepattern.util.GPConstants.*;

public class AuthorizationRules {
    private static Logger log = Logger.getLogger(AuthorizationRules.class);

    private static IAuthorizationManager authManager = AuthorizationManagerFactory.getAuthorizationManager();

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
    public static boolean isAllowed(TaskInfo taskInfo, String userId, ActionType actionType) {

        String owner = taskInfo.getUserId();
        int accessId = taskInfo.getAccessId();

        switch (actionType) {
        case View:
            return authManager.checkPermission("administrateServer", userId) || owner.equals(userId)
                    || GPConstants.ACCESS_PUBLIC == accessId;

        case Create:
            String perm = taskInfo.isPipeline() ? "createPipeline" : "createTask";
            return authManager.checkPermission(perm, userId);

        case Edit:
            // @todo -- check LSID authority. Only allow if local.
            perm = taskInfo.isPipeline() ? "createPipeline" : "createTask";

            if (authManager.checkPermission(perm, userId)
                    && (owner.equals(userId) || GPConstants.ACCESS_PUBLIC == accessId)) {
                try {
                    LSID lsidObj = new LSID(taskInfo.getLsid());
                    return LSIDUtil.getInstance().isAuthorityMine(lsidObj);
                } catch (MalformedURLException e) {
                    log.error(e);
                    return false;
                }

            }

        case Delete:
            perm = taskInfo.isPipeline() ? "deletePipeline" : "deleteTask";
            return authManager.checkPermission(perm, userId) && owner.equals(userId);

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
    public static boolean isAllowed(JobInfo jobInfo, String userId, ActionType actionType) {

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
