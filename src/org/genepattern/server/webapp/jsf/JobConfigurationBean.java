package org.genepattern.server.webapp.jsf;

import java.util.ArrayList;
import java.util.List;

import org.genepattern.server.executor.CommandExecutor;
import org.genepattern.server.executor.CommandManager;
import org.genepattern.server.executor.CommandManagerFactory;
import org.jfree.util.Log;

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

    public List<Obj> getCommandExecutors() {
        if (commandExecutors != null) {
            return commandExecutors;
        }
        commandExecutors = new ArrayList<Obj>();

        CommandManager f = CommandManagerFactory.getCommandManager();
        //List<CommandExecutor> l = f.getCommandExecutors();
        Iterable<CommandExecutor> l = f.getCommandExecutorsMap().values();
        //List<CommandExecutor> l = f.getCommandExecutorMap().;
        for(CommandExecutor cmd : l) {
            commandExecutors.add(new Obj(cmd));
        } 
        return commandExecutors;
    }
    
    public void setCmdExecutor(Obj obj) {
        this.cmdExecutor = obj.getCommandExecutor();
    }
    
    public void reloadConfigurationForItem() throws Exception {
        Log.error("Ignoring reloadConfigurationForItem: "+cmdExecutor.getClass().getCanonicalName());
    }

}
