/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.jsf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.executor.CommandExecutor;
import org.genepattern.server.executor.CommandManager;
import org.genepattern.server.executor.CommandManagerFactory;

/**
 * Backing bean for configuring command executors via the web interface.
 * 
 * @author pcarr
 */
public class JobConfigurationBean {
    public static Logger log = Logger.getLogger(JobConfigurationBean.class);

    /**
     * Gui view of a command executor.
     * @author pcarr
     */
    public static class Obj {
        private final CommandExecutor exec;
        private final String id;
        private final String classname;
        
        public Obj(String id, CommandExecutor exec) {
            this.id = id;
            this.exec = exec;
            this.classname = exec.getClass().getCanonicalName();
        }
        
        public String getId() {
            return id;
        }

        public CommandExecutor getCommandExecutor() {
            return exec;
        }

        public String getClassname() {
            return classname;
        }
    }
    
    private List<Obj> commandExecutors = null;
    
    public boolean isSuspended() {
        return CommandManagerFactory.getCommandManager().isSuspended();
    }
    
    /**
     * Suspend the internal job queue.
     */
    public void suspendJobQueue() {
        CommandManagerFactory.getCommandManager().suspendJobQueue();
    }

    /**
     * Resume the internal job queue.
     */
    public void resumeJobQueue() {
        CommandManagerFactory.getCommandManager().resumeJobQueue();
    }
    
    public boolean isRunning() {
        return CommandManagerFactory.isRunning();
    }

    /**
     * Restart the job execution system, reparse the config file and making the same calls that are done on server startup.
     */
    public void startup() {
        CommandManagerFactory.startJobQueue();
    }

    /**
     * Shutdown the job execution system, making the same calls that are done on server shutdown.
     */
    public void shutdown() {
        CommandManagerFactory.stopJobQueue();
    }

    /**
     * Reload the configuration file, this does not initialize command executors. To do that you will need to call #shutdown and then #startup.
     */
    public void reloadJobConfiguration() throws Exception {
        ServerConfigurationFactory.reloadConfiguration();
    }
    
    public String getParser() {
        return "";
    }
    
    public boolean getHasErrors() {
        return ServerConfigurationFactory.instance().getInitializationErrors().size() > 0;
    }

    public List<Throwable> getConfigurationFileErrors() {
        return ServerConfigurationFactory.instance().getInitializationErrors();
    }
    
    public String getConfigurationFilepath() {
        File configFile = null;
        try {
            configFile = ServerConfigurationFactory.instance().getConfigFile();
            if (configFile != null) {
                return configFile.getAbsolutePath();
            }
            else {
                return "";
            }
        }
        catch (Throwable t) {
            return "";
        }
    }
    
    /**
     * Display the contents of the configuration file.
     * @return
     */
    public String getConfigurationFileContent() {
        File configFile = null;
        try {
            configFile = ServerConfigurationFactory.instance().getConfigFile();
        } 
        catch (Throwable e) {
            return e.getLocalizedMessage();
        }
        if (configFile == null) {
            return "configFile is null";
        }
        String logFileContent = ServerSettingsBean.getEntireLog(configFile);
        return logFileContent;
    }

    public List<Obj> getCommandExecutors() {
        if (commandExecutors != null) {
            return commandExecutors;
        }
        commandExecutors = new ArrayList<Obj>();

        CommandManager f = CommandManagerFactory.getCommandManager();
        for(Entry<String,CommandExecutor> entry : f.getCommandExecutorsMap().entrySet()) {
            commandExecutors.add(new Obj(entry.getKey(), entry.getValue()));
        }
        return commandExecutors;
    }

}
