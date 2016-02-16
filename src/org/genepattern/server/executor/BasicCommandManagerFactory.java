/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ConfigurationException;
import org.genepattern.server.config.ExecutorConfig;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.JobConfigObj;
import org.genepattern.server.database.HibernateSessionManager;

/**
 * Create a new instance of a CommandManager based on the ServerConfiguration.
 * 
 * @author pcarr
 */
public class BasicCommandManagerFactory {
    private static Logger log = Logger.getLogger(BasicCommandManagerFactory.class);
    
    private BasicCommandManager cmdMgr = null;
    public BasicCommandManagerFactory() {
    }
    
    public BasicCommandManager createCommandManager(final HibernateSessionManager mgr, final GpConfig gpConfig, final JobConfigObj jobConfigObj) throws ConfigurationException {
        this.cmdMgr = new BasicCommandManager(mgr, gpConfig); 
        initializeCommandExecutors(mgr, cmdMgr, jobConfigObj.getExecutors());
        return cmdMgr;
    }
    
    //initialize executors list
    private void initializeCommandExecutors(final HibernateSessionManager mgr, final BasicCommandManager cmdMgr, final Map<String,ExecutorConfig> executors)  throws ConfigurationException {
        log.info("initializing command executors ...");
        for(final Entry<String,ExecutorConfig> entry : executors.entrySet()) {
            try {
                initializeCommandExecutor(mgr, cmdMgr, entry.getKey(), entry.getValue());
            }
            catch (Throwable t) {
                log.error("error initializing command executor, execId='"+entry.getKey()+"'", t);
            }
        }
    }

    private void initializeCommandExecutor(final HibernateSessionManager mgr, final BasicCommandManager cmdMgr, final String execId, final ExecutorConfig execObj) throws ConfigurationException {
        CommandExecutor cmdExecutor = initializeCommandExecutor(mgr, execObj);
        cmdMgr.addCommandExecutor(execId, cmdExecutor);
    }

    /**
     * Initialize an instance of a CommandExecutor from the settings stored in the given CmdExecConfigObj.
     * If the CommandExecutor has a single HibernateSessionManager.class arg constructor (e.g. JobExecutor) it will use that constructor.
     * Otherwise it will initialize with the empty constructor empty and [optionally] call setConfigurationFilename and setConfigurationProperties.
     * 
     * @param execObj
     * @return
     */
    protected static CommandExecutor initializeCommandExecutor(final HibernateSessionManager mgr, org.genepattern.server.config.ExecutorConfig execObj) {
        CommandExecutor cmdExecutor = null;
        //1) load cmdExecutor from classname
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Class<?> svcClass = Class.forName(execObj.getClassname(), false, classLoader);
            if (!CommandExecutor.class.isAssignableFrom(svcClass)) {
                log.error(""+svcClass.getCanonicalName()+" does not implement "+CommandExecutor.class.getCanonicalName());
            }
            Constructor<?> constructor=null;
            try {
                constructor=svcClass.getConstructor(new Class[]{HibernateSessionManager.class});
            }
            catch (NoSuchMethodException e) {
                // ignore expected error
                if (log.isDebugEnabled()) {
                    log.debug("class '"+execObj.getClassname()+"' does not have a HibernateSessionManager.class constructor");
                }
            }
            catch (Throwable t) {
                // ignore unexpected error
                log.error("Unexpected error, ignored", t);
            }
            if (constructor != null) {
                log.debug("initializing with "+HibernateSessionManager.class.getName() +" arg constructor");
                cmdExecutor = (CommandExecutor) constructor.newInstance(mgr);
            }
            else {
                log.debug("initializing with no-arg constructor");
                cmdExecutor = (CommandExecutor) svcClass.newInstance();
            }
        }
        catch (Throwable t) {
            log.error("Error loading CommandExecutor for classname: "+execObj.getClassname()+", "+t.getLocalizedMessage(), t);
        }
        if (cmdExecutor == null) {
            return cmdExecutor;
        }
        //2) optionally set configuration parameters
        if (execObj.getConfigurationFile() != null) {
            cmdExecutor.setConfigurationFilename(execObj.getConfigurationFile());
        }
        if (execObj.getConfigurationProperties() != null) {
            cmdExecutor.setConfigurationProperties(execObj.getConfigurationProperties());
        }
        return cmdExecutor;
    }
}

