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

    public boolean isAdmin() {
        IAuthorizationManager authManager = new AuthorizationManagerFactoryImpl().getAuthorizationManager();
        return authManager.checkPermission("administrateServer", UIBeanHelper.getUserId());
    }
}
