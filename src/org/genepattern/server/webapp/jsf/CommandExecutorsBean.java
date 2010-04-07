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
    public static class Obj {
        private String id = "defaultId";
        private String classname = "defaultClassname";
        public String getId() {
            return id;
        }
        
        public String getClassname() {
            return classname;
        } 
    }

    /**
     * Reload the configuration file for the mapper.
     */
    public void reloadMapperConfiguration() throws Exception {
        CommandExecutorManager.instance().reloadMapperConfiguration();
    }

    public void reloadCustomProperties() { 
    }
    
    public List<Obj> getCommandExecutors() {
        List<Obj> rval = new ArrayList<Obj>();

        CommandExecutorManager mgr = CommandExecutorManager.instance();
        CommandExecutorFactory f = mgr.getCommandExecutorFactory();
        List<CommandExecutor> l = f.getCommandExecutors();
        for(CommandExecutor cmd : l) {
            String classname = cmd.getClass().getCanonicalName();
            if (classname != null) {
                Obj obj = new Obj();
                obj.classname = classname;
                rval.add(obj);
            }
        }
        
        return rval;
    }

}
