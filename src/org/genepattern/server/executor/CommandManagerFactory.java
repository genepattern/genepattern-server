package org.genepattern.server.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfigurationFactory;

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

    private static boolean running = false;
    private static List<Throwable> errors = new ArrayList<Throwable>();
    private static BasicCommandManager manager = null;
    
    private CommandManagerFactory() {
    }

    /**
     * Get the command manager. This method initializes the manager from system properties if necessary.
     */
    public static CommandManager getCommandManager() {
        if (manager != null) {
            return manager;
        }
        //lazy init ...
        initializeCommandManager();
        return manager;
    }
    
    public static boolean isRunning() {
        return running;
    }
    
    public static void startJobQueue() {
        //start the command executors before starting the internal job queue ...
        log.info("\tstarting job queue...");
        initializeCommandManager();
        CommandManager cmdManager = getCommandManager();
        cmdManager.startCommandExecutors();
        cmdManager.startAnalysisService();
        running = true;
    }
    
    public static void stopJobQueue() {
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
    
    /**
     * Create a new instance of CommandManager.
     * This method replaces the current manager with a new instance.
     * 
     * @param properties
     */
    public static synchronized void initializeCommandManager() {
        if (manager != null) {
            log.info("replacing current command manager with a new instance");
        }
        manager = createCommandManager();
    }
    
    private static synchronized BasicCommandManager createCommandManager() {
        if (ServerConfigurationFactory.instance().getInitializationErrors().size() > 0) {
            log.error("server configuration errors, creating default command manager");
            return createDefaultCommandManager();
        }
        
        BasicCommandManagerFactory parser = new BasicCommandManagerFactory();
        try {
            BasicCommandManager cmdMgr =  parser.createCommandManager();
            return cmdMgr;
        }
        catch (final Exception e) {
          errors.add(e);
          log.error("Failed to load custom command manager loader class: "+BasicCommandManagerFactory.class.getCanonicalName(), e);
          return createDefaultCommandManager();
        }
    }
    
    private static synchronized BasicCommandManager createDefaultCommandManager() {
        BasicCommandManager commandManager = new BasicCommandManager();
        CommandExecutor cmdExecutor = new RuntimeCommandExecutor();
        try {
            commandManager.addCommandExecutor("RuntimeExec", cmdExecutor);
        }
        catch (Exception e) {
            errors.add(e);
            log.error(e);
        }
        return commandManager;
    }
    
    /**
     * Helper method, get the id (key into the commandExecutorsMap) for the given CommandExecutor.
     * @param cmdExecutor
     * @return null if the CommandExecutor is not in the map
     */
    public static synchronized String getCommandExecutorId(CommandExecutor cmdExecutor) { 
        if (cmdExecutor == null) {
            log.error("null arg");
            return null;
        }
        if (manager == null) {
            log.error("manager not initialized");
            return null;
        }
        
        Map<String,CommandExecutor> map = manager.getCommandExecutorsMap();
        if (!map.containsValue(cmdExecutor)) {
            log.error("commandExecutorsMap does not contain value for "+cmdExecutor.getClass().getCanonicalName());
            return null;
        }
        
        for(Entry<String,CommandExecutor> entry : manager.getCommandExecutorsMap().entrySet()) {
            if(cmdExecutor == entry.getValue()) {
                return entry.getKey();
            }
        }
        return null;
    }

}
