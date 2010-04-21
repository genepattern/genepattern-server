package org.genepattern.server.webapp.jsf;

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
public class CommandExecutorsBean {
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


    /**
     * Reload the configuration file for the mapper.
     */
    public void reloadMapperConfiguration() throws Exception {
        CommandManagerFactory.getCommandManager().reloadMapperConfiguration();
    }

    public void reloadCustomProperties() { 
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
        if (this.cmdExecutor != null) {
            cmdExecutor.reloadConfiguration();
        }
        else {
            System.err.println("unknown cmd executor!");
        }
    }

}
