/*******************************************************************************
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright (2003-2006) by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are
 * reserved.
 *  
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 *  
 *******************************************************************************/
package org.genepattern.server.webapp.jsf;

import org.genepattern.server.util.AuthorizationManagerFactory;
import org.genepattern.server.util.IAuthorizationManager;

/**
 * 
 * @author Joshua Gould
 * 
 */
public class AuthorizationBean {
    private IAuthorizationManager authManager;

    public AuthorizationBean() {
        authManager = AuthorizationManagerFactory.getAuthorizationManager();
    }

    public boolean isAdmin() {
        return authManager.checkPermission("administrateServer", UIBeanHelper.getUserId());
    }

    public boolean isCreateTaskAllowed() {
        return authManager.checkPermission("createTask", UIBeanHelper.getUserId());
    }

    public boolean isCreateSuiteAllowed() {
        return authManager.checkPermission("createSuite", UIBeanHelper.getUserId());
    }

    public boolean isCreatePipelineAllowed() {
        return authManager.checkPermission("createPipeline", UIBeanHelper.getUserId());
    }

    public boolean isDeleteTaskAllowed() {
        return authManager.checkPermission("deleteTask", UIBeanHelper.getUserId());
    }
    
    public boolean isDeletePipelineAllowed() {
        return authManager.checkPermission("deletePipeline", UIBeanHelper.getUserId());
    }

    public boolean isAdmin(String username) {
        return authManager.checkPermission("administrateServer", username);
    }

    public boolean isCreateTaskAllowed(String username) {
        return authManager.checkPermission("createTask", username);
    }

    public boolean isCreateSuiteAllowed(String username) {
        return authManager.checkPermission("createSuite", username);
    }

    public boolean isCreatePipelineAllowed(String username) {
        return authManager.checkPermission("createPipeline", username);
    }

    public boolean isDeleteTaskAllowed(String username) {
        return authManager.checkPermission("deleteTask", username);
    }

    public boolean isDeleteJobAllowed(String username) {
        return authManager.checkPermission("deleteJob", username);
    }

}
