package org.genepattern.server.webapp.jsf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.executor.CommandExecutor;
import org.genepattern.server.executor.CommandManager;
import org.genepattern.server.executor.CommandManagerFactory;

/**
 * Backing bean for configuring command executors via the web interface.
 * 
 * @author pcarr
 */
public class JobConfigurationBean {
    private static Logger log = Logger.getLogger(JobConfigurationBean.class);

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
    private CommandExecutor cmdExecutor = null;

    public void startAnalysisService() {
        CommandManagerFactory.getCommandManager().startAnalysisService();
    }
    
    public void shutdownAnalysisService() {
        CommandManagerFactory.getCommandManager().shutdownAnalysisService();
    }
    
    public void startCommandExecutors() {
        CommandManagerFactory.getCommandManager().startCommandExecutors();
    }
    
    public void stopCommandExecutors() {
        CommandManagerFactory.getCommandManager().stopCommandExecutors();
    }

    /**
     * Reload the configuration file for the mapper.
     */
    public void reloadJobConfiguration() throws Exception {
        CommandManagerFactory.reloadConfigFile();
    }
    
    public void stopAndRestartAllExecutors() {
        CommandManagerFactory.getCommandManager().shutdownAnalysisService();
        CommandManagerFactory.getCommandManager().stopCommandExecutors();
        
        String parser = CommandManagerFactory.getParser();
        String configFile = CommandManagerFactory.getConfigFile();
        CommandManagerFactory.initializeCommandManager(parser, configFile);
        
        CommandManagerFactory.getCommandManager().startCommandExecutors();
        CommandManagerFactory.getCommandManager().startAnalysisService();
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
        return ServerSettingsBean.getLog(configFile);
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
    
    public void setCmdExecutor(Obj obj) {
        this.cmdExecutor = obj.getCommandExecutor();
    }
    
    public void reloadConfigurationForItem() throws Exception {
        log.error("Ignoring reloadConfigurationForItem: "+cmdExecutor.getClass().getCanonicalName());
    }

}
