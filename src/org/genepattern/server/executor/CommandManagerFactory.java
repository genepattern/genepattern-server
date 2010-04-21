package org.genepattern.server.executor;

import java.util.Properties;
import org.apache.log4j.Logger;

/**
 * Initialize and hold a single instance of a CommandManager for the GenePattern Server.
 * 
 * This extra layer of abstraction makes it possible to provide a different instance of the CommandManager
 * without requiring a full build and reinstall of GenePattern.
 * 
 * The command manager is created by passing a config file to a parser which implements the CommandManagerConfigParser interface.
 * Add the following properties to the  'genepattern.properties' file to override the default settings:
 * 
 * <code>
 * command.manager.config.parser=<class which implements org.genepattern.server.exec.CommandManagerConfigParser>
 * command.manager.config.file=<configuration file>
 * </code>
 * 
 * @author pcarr
 */
public class CommandManagerFactory {
    private static Logger log = Logger.getLogger(CommandManagerFactory.class);
    
    private final static String PROP_COMMAND_MANAGER_CONFIG_PARSER="command.manager.config.parser";
    private final static String PROP_COMMAND_MANAGER_CONFIG_FILE="command.manager.config.file";
    
    private static String configParser = null;
    private static String configFile = null;

    private static CommandManager manager = null;
    
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
        initializeCommandManager(null);
        return manager;
    }
    
    /**
     * Create a new default CommandManager.
     */
    public static synchronized void initializeCommandManager() {
        initializeCommandManager(new Properties());
    }

    /**
     * Create a new instance of CommandManager, using the given properties.
     * This method replaces the current manager with a new instance.
     * 
     * @param properties
     */
    public static synchronized void initializeCommandManager(Properties properties) {
        if (manager != null) {
            log.info("replacing current command manager with a new instance");
        }
        setProperties(properties);
        manager = createCommandManager(configParser, configFile);
    }

    /**
     * Reset to defaults, then check for custom settings in the properties object.
     * If the properties is null check System properties.
     */
    private static void setProperties(Properties properties) {
        configParser = null;
        configFile = null;
        if (properties == null) {
            properties = System.getProperties();
        }
        configParser = properties.getProperty(PROP_COMMAND_MANAGER_CONFIG_PARSER);
        configFile = properties.getProperty(PROP_COMMAND_MANAGER_CONFIG_FILE);
    }

    private static synchronized CommandManager createCommandManager(String configParserClass, String configFile) {
        CommandManagerConfigParser configParser = null;
        if (configParserClass == null) {
            return createDefaultCommandManager();
        }
        try {
            log.info("loading CommandManagerLoader from class "+configParserClass);
            configParser = (CommandManagerConfigParser) Class.forName(configParserClass).newInstance();
            return configParser.parseConfigFile(configFile);
        } 
        catch (final Exception e) {
            log.error("Failed to load custom command manager loader class: "+configParserClass, e);
            return createDefaultCommandManager();
        }
    }
    
    private static synchronized CommandManager createDefaultCommandManager() {
        BasicCommandManager commandManager = new BasicCommandManager();
        CommandExecutor cmdExecutor = new RuntimeCommandExecutor();
        try {
            commandManager.addCommandExecutor("RuntimeExec", cmdExecutor);
        }
        catch (Exception e) {
            log.error(e);
        }
        return commandManager;
    }

}
