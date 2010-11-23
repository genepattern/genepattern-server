package org.genepattern.server.webapp.jsf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.genepattern.server.executor.CommandExecutor;
import org.genepattern.server.executor.CommandManager;
import org.genepattern.server.executor.CommandManagerFactory;

/**
 * Backing bean for configuring command executors via the web interface.
 * 
 * @author pcarr
 */
public class JobConfigurationBean {

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
    
    public File getConfigurationFile() {
        return CommandManagerFactory.getConfigurationFile();
    }

    public String getConfigurationFilepath() {
        File configFile = getConfigurationFile();
        if (configFile != null) {
            return configFile.getAbsolutePath();
        }
        return "";
    }
    
    public boolean getHasErrors() {
        return CommandManagerFactory.getInitializationErrors().size() > 0;
    }

    public List<Throwable> getConfigurationFileErrors() {
        return CommandManagerFactory.getInitializationErrors();
    }
    
    /**
     * Display the contents of the configuration file.
     * @return
     */
    public String getConfigurationFileContent() {
        File configFile = getConfigurationFile();
        if (configFile == null) {
            return "";
        }
        if (!configFile.canRead()) {
            return "File is not readable";
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
