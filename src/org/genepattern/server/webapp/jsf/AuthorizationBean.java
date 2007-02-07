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

    public boolean isAdminServer() {
        return authManager.checkPermission("adminServer", UIBeanHelper.getUserId());
    }
    
    public boolean isAdminJobs() {
        return authManager.checkPermission("adminServer", UIBeanHelper.getUserId());
    }

    public boolean isAdminModules() {
        return authManager.checkPermission("adminServer", UIBeanHelper.getUserId());
    }
    
    public boolean isAdminPipeline() {
        return authManager.checkPermission("adminServer", UIBeanHelper.getUserId());
    }

    public boolean isAdminSuites() {
        return authManager.checkPermission("adminServer", UIBeanHelper.getUserId());
    }

    public boolean isCreateModuleAllowed() {
        return authManager.checkPermission("createModule", UIBeanHelper.getUserId());
    }

    public boolean isCreateSuiteAllowed() {
        return authManager.checkPermission("createSuite", UIBeanHelper.getUserId());
    }

    public boolean isCreatePipelineAllowed() {
        return authManager.checkPermission("createPipeline", UIBeanHelper.getUserId());
    }

 
    // The methods below are to support jsps
    public boolean isAdminServer(String username) {
        return authManager.checkPermission("adminServer", username);
    }

    public boolean isCreateModuleAllowed(String username) {
        return authManager.checkPermission("createModule", username);
    }

    public boolean isCreateSuiteAllowed(String username) {
        return authManager.checkPermission("createSuite", username);
    }

    public boolean isCreatePipelineAllowed(String username) {
        return authManager.checkPermission("createPipeline", username);
    }

  

}
