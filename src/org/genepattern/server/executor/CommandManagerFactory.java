/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.JobConfigObj;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;

/**
 * Initialize and hold a single instance of a CommandManager for the GenePattern Server.
 * 
 * This extra layer of abstraction makes it possible to provide a different instance of the CommandManager
 * without requiring a full build and reinstall of GenePattern.
 * 
 * The command manager is created by passing a config file to a parser which implements the CommandManagerParser interface.
 * Add the following properties to the 'genepattern.properties' file to override the default settings:
 * 
 * <code>
 * command.manager.parser=<class which implements org.genepattern.server.exec.CommandManagerParser>
 * command.manager.config.file=<configuration file>
 * </code>
 * 
 * @author pcarr
 */
public class CommandManagerFactory {
    private static Logger log = Logger.getLogger(CommandManagerFactory.class);

    private static final Object mgrLock=new Object();
    private static boolean running = false;
    private static BasicCommandManager manager = null;
    
    private CommandManagerFactory() {
    }

    /**
     * Get the command manager. This method initializes the manager from system properties if necessary.
     */
    public static BasicCommandManager getCommandManager() {
        synchronized(mgrLock) {
            if (manager == null) {
                final HibernateSessionManager mgr=HibernateUtil.instance();
                final GpConfig gpConfig=ServerConfigurationFactory.instance();
                manager = createCommandManager(mgr, gpConfig);
            }
            return manager;
        }
    }
    
    public static boolean isRunning() {
        return running;
    }
    
    public static void startJobQueue() {
        synchronized(mgrLock) {
            if (manager==null) {
                final HibernateSessionManager mgr=HibernateUtil.instance();
                final GpConfig gpConfig=ServerConfigurationFactory.instance();
                manager = createCommandManager(mgr, gpConfig);
            }

            //start the command executors before starting the internal job queue ...
            log.info("\tstarting job queue...");
            manager.startCommandExecutors();
            manager.startAnalysisService();
            running = true;
        }
    }
    
    public static void stopJobQueue() {
        synchronized(mgrLock) {
            if (manager == null) {
                running = false;
                return;
            }

            //first, stop the internal job queue
            manager.shutdownAnalysisService();

            //then stop the command executors, which are responsible for stopping/suspending/or allowing to continue each running job ...
            //pipelines are shut down here
            manager.stopCommandExecutors();

            running = false;
        }
    }
    
    protected static BasicCommandManager createCommandManager(final HibernateSessionManager mgr, final GpConfig gpConfig) {
        log.info("\tinitializing command manager ...");
        if (gpConfig == null) {
            log.error("server error, gpConfig==null, creating default command manager");
            return createDefaultCommandManager(mgr, gpConfig);
        }
        if (gpConfig.getInitializationErrors().size() > 0) {
            log.error("server configuration errors, creating default command manager");
            return createDefaultCommandManager(mgr, gpConfig);
        }
        final JobConfigObj jobConfigObj = gpConfig.getJobConfiguration();
        if (jobConfigObj==null) {
            log.error("server error, gpConfig.jobConfigObj==null, creating default command manager");
            return createDefaultCommandManager(mgr, gpConfig);
        }
        try {
            BasicCommandManagerFactory basicCmdMgrFactory = new BasicCommandManagerFactory();
            BasicCommandManager cmdMgr =  basicCmdMgrFactory.createCommandManager(mgr, gpConfig, jobConfigObj);
            return cmdMgr;
        }
        catch (final Exception e) {
          log.error("Failed to load custom command manager loader class: "+BasicCommandManagerFactory.class.getCanonicalName(), e);
          return createDefaultCommandManager(mgr, gpConfig);
        }
    }
    
    private static BasicCommandManager createDefaultCommandManager(final HibernateSessionManager mgr, final GpConfig gpConfig) {
        log.error("Settig the CommandExecutor to 'RuntimeExec'; Edit the config file to use the newer JobRunner API");
        BasicCommandManager commandManager = new BasicCommandManager(mgr, gpConfig);
        CommandExecutor cmdExecutor = new RuntimeCommandExecutor();
        try {
            commandManager.addCommandExecutor("RuntimeExec", cmdExecutor);
        }
        catch (Exception e) {
            log.error(e);
        }
        return commandManager;
    }
    
}
