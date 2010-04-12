package org.genepattern.server.webapp.jsf;

import java.util.ArrayList;
import java.util.List;

import org.genepattern.server.executor.CommandExecutor;
import org.genepattern.server.executor.CommandExecutorFactory;
import org.genepattern.server.executor.CommandExecutorManager;

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
        CommandExecutorManager.instance().reloadMapperConfiguration();
    }

    public void reloadCustomProperties() { 
    }
    
    public List<Obj> getCommandExecutors() {
        if (commandExecutors != null) {
            return commandExecutors;
        }
        commandExecutors = new ArrayList<Obj>();

        CommandExecutorManager mgr = CommandExecutorManager.instance();
        CommandExecutorFactory f = mgr.getCommandExecutorFactory();
        List<CommandExecutor> l = f.getCommandExecutors();
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
