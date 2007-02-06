/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.util;

import org.apache.log4j.Logger;

/**
 * @author Liefeld
 * 
 */

public class AuthorizationManagerFactory {
    private static Logger log = Logger.getLogger(AuthorizationManagerFactory.class);

    private static IAuthorizationManager authorizationManager = init();

    private AuthorizationManagerFactory() {
    }

    private static IAuthorizationManager init() {
        String className = System.getProperty("org.genepattern.server.util.AuthorizationManager");

        if (className == null) {
            return new AuthorizationManager();
        }

        try {
            return (IAuthorizationManager) Class.forName(className).newInstance();

        } catch (ClassNotFoundException e) {
            log.error(className, e);
        } catch (SecurityException e) {
            log.error(className, e);
        } catch (InstantiationException e) {
            log.error(className, e);
        } catch (IllegalAccessException e) {
            log.error(className, e);
        }
        return new AuthorizationManager();
    }

    public static IAuthorizationManager getAuthorizationManager() {
        return authorizationManager;
    }

}