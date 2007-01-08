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

import org.genepattern.server.util.AuthorizationManagerFactoryImpl;
import org.genepattern.server.util.IAuthorizationManager;

/**
 * 
 * @author Joshua Gould
 * 
 */
public class AuthorizationBean {
    private IAuthorizationManager authManager;

    public AuthorizationBean() {
        authManager = new AuthorizationManagerFactoryImpl().getAuthorizationManager();
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

    public boolean isDeleteJobAllowed() {
        return authManager.checkPermission("deleteJob", UIBeanHelper.getUserId());
    }

}
