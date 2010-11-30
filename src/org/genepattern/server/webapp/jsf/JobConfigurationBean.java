package org.genepattern.server.webapp.jsf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.executor.CommandExecutor;
import org.genepattern.server.executor.CommandManager;
import org.genepattern.server.executor.CommandManagerFactory;
import org.genepattern.server.executor.ConfigurationException;

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
        private CommandExecutor exec = null;
        private String id = "";
        private String classname = "";
        
        public Obj(CommandExecutor exec) {
            this.exec = exec;
            this.classname = exec.getClass().getCanonicalName();
            this.id = CommandManagerFactory.getCommandExecutorId(this.exec);
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
        CommandManagerFactory.reloadConfigFile();
    }
    
    public String getParser() {
        return CommandManagerFactory.getParser();
    }
    
    public boolean getHasErrors() {
        return CommandManagerFactory.getInitializationErrors().size() > 0;
    }

    public List<Throwable> getConfigurationFileErrors() {
        return CommandManagerFactory.getInitializationErrors();
    }
    
    public String getConfigurationFilepath() {
        File configFile = null;
        try {
            configFile = CommandManagerFactory.getConfigurationFile();
            return configFile.getAbsolutePath();
        }
        catch (ConfigurationException e) {
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
            configFile = CommandManagerFactory.getConfigurationFile();
        } 
        catch (ConfigurationException e) {
            return e.getLocalizedMessage();
        }
        String logFileContent = ServerSettingsBean.getLog(configFile);
        return logFileContent;
    }

    public List<Obj> getCommandExecutors() {
        if (commandExecutors != null) {
            return commandExecutors;
        }
        commandExecutors = new ArrayList<Obj>();

        CommandManager f = CommandManagerFactory.getCommandManager();
        Iterable<CommandExecutor> l = f.getCommandExecutorsMap().values();
        for(CommandExecutor cmd : l) {
            commandExecutors.add(new Obj(cmd));
        } 
        return commandExecutors;
    }

}
