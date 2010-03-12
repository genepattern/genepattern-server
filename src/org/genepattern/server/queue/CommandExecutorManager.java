package org.genepattern.server.queue;

import org.apache.log4j.Logger;

/**
 * Provide single point of access to the CommandExecutorService for a running GenePattern Server.
 * This extra layer of abstraction (this is a factory for a factory) is here to make it easier to update the CommandExecutorFactory without 
 * requiring a full build and reinstall of GenePattern.
 * 
 * @author pcarr
 */
public class CommandExecutorManager {
    private static Logger log = Logger.getLogger(CommandExecutorManager.class);
    
    //enforce singleton pattern
    private CommandExecutorManager() {
        //#command.executor.factory.class=org.genepattern.server.queue.DefaultCommandExecutorFactory
        String commandExecutorFactoryClass = System.getProperty("command.executor.factory.class", DefaultCommandExecutorFactory.class.getName());
        loadCommandExecutorFactory(commandExecutorFactoryClass);
    }
    
    //enforce lazy loading of singleton instance
    public static CommandExecutorManager instance() {
        return Singleton.commandExecutorManager;
    }
    
    private static class Singleton {
        private static final CommandExecutorManager commandExecutorManager = new CommandExecutorManager();
    }
    
    private CommandExecutorFactory factory = null;

    public CommandExecutorFactory getCommandExecutorFactory() {
        return factory;
    }
    
    public CommandExecutorMapper getCommandExecutorMapper() {
        return factory.getCommandExecutorMapper();
    }
    
    public void reloadServiceMapperConfiguration() {
        log.error("reload CommandExecutorServieMapper not implemented!");
    }
    
    private void loadCommandExecutorFactory(String commandExecutorFactoryClass) {
        if (commandExecutorFactoryClass == null) {
            this.factory = new DefaultCommandExecutorFactory();
        }
        else {
            try {
                this.factory = (CommandExecutorFactory) Class.forName(commandExecutorFactoryClass).newInstance();
            } 
            catch (final Exception e) {
                log.error("Failed to load custom command executor factory class: "+commandExecutorFactoryClass, e);
                this.factory = new DefaultCommandExecutorFactory();
            } 
        }
    }
}
