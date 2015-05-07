/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.util;

import java.io.File;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;

import com.google.common.base.Strings;

/**
 * @author Liefeld
 * 
 */

public class AuthorizationManagerFactory {

    private AuthorizationManagerFactory() {
    }

    public static IAuthorizationManager getAuthorizationManager() {
        return LazyHolder.INSTANCE;
    }
    
    private static class LazyHolder {
        public static final IAuthorizationManager INSTANCE = initAuthorizationManager();
    }

    public static IAuthorizationManager initAuthorizationManager() {
        GpConfig gpConfig=ServerConfigurationFactory.instance();
        GpContext gpContext=GpContext.getServerContext();
        return initAuthorizationManager(gpConfig, gpContext);
    }

    public static IAuthorizationManager initAuthorizationManager(final GpConfig gpConfig, final GpContext gpContext) {
        String className=gpConfig.getGPProperty(gpContext, "org.genepattern.server.util.AuthorizationManager");
        return initAuthorizationManager(className, gpConfig.getResourcesDir());
    }

    public static IAuthorizationManager initAuthorizationManager(final String className, final File resourcesDir) {
        if (Strings.isNullOrEmpty(className)) {
            return new AuthorizationManager(resourcesDir);
        }
        try {
            return (IAuthorizationManager) Class.forName(className).newInstance();
        } 
        catch (ClassNotFoundException e) {
            Logger.getLogger(AuthorizationManagerFactory.class).error("Error initializing IAuthorizationManager from class="+className, e);
        } 
        catch (SecurityException e) {
            Logger.getLogger(AuthorizationManagerFactory.class).error("Error initializing IAuthorizationManager from class="+className, e);
        } 
        catch (InstantiationException e) {
            Logger.getLogger(AuthorizationManagerFactory.class).error("Error initializing IAuthorizationManager from class="+className, e);
        } 
        catch (IllegalAccessException e) {
            Logger.getLogger(AuthorizationManagerFactory.class).error("Error initializing IAuthorizationManager from class="+className, e);
        }
        catch (Throwable t) {
            Logger.getLogger(AuthorizationManagerFactory.class).error("Error initializing IAuthorizationManager from class="+className, t);
        }
        return new AuthorizationManager(resourcesDir);
    }

}
