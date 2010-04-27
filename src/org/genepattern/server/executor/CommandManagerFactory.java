package org.genepattern.server.executor;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

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

    private final static String PROP_COMMAND_MANAGER_PARSER="command.manager.parser";
    private final static String PROP_COMMAND_MANAGER_CONFIG_FILE="command.manager.config.file";

    //aka the location of the genepattern.properties file
    private static File resourceDirectory = null;
    
    private static String parser = null;
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
     * Create a new default CommandManager, replacing the current manager with a new instance.
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
        manager = createCommandManager(parser, configFile);
    }

    /**
     * Reset to defaults, then check for custom settings in the properties object.
     * If the properties is null check System properties.
     */
    private static void setProperties(Properties properties) {
        parser = null;
        configFile = null;
        if (properties == null) {
            properties = System.getProperties();
        }
        parser = properties.getProperty(PROP_COMMAND_MANAGER_PARSER);
        configFile = properties.getProperty(PROP_COMMAND_MANAGER_CONFIG_FILE);
    }
    
    /**
     * Get a File object for the named configuration file as specified in the 'genepattern.properties' file. E.g.
     * <code>
     *     command.manager.config.file=job_configuration.yaml
     *     or
     *     command.manager.config.file=/fully/qualified/path/to/job_configuration.yaml
     * </code>
     * If a relative path is given, load the file relative to the resources directory as specified by the 
     * system property, 'genepattern.properties'. The location of the resources directory can be overwritten by calling setResourceDirectory().
     * If there is no resource directory, load the file relative to the current working directory.
     * @param configuration
     * @return a valid File or null
     */
    public static File getConfigurationFile(String configuration) {
        File f = new File(configuration);
        if (!f.isAbsolute()) {
            //load the configuration file from the resources directory
            File parent = getResourceDirectory();
            if (parent != null) {
                f = new File(parent, configuration);
            }
        }
        if (!f.canRead()) {
            if (!f.exists()) {
                log.error("Configuration file not found: "+f.getAbsolutePath());
            }
            else {
                log.error("Cannot read configuration file: "+f.getAbsolutePath());
            }
            f = null;
        }
        return f;
    }
    
    public static void setResourceDirectory(File dir) {
        resourceDirectory = dir;
    }

    /**
     * Get the resource directory, the parent directory of the genepattern.properties file.
     * @return a File or null if there is a configuration error 
     */
    public static File getResourceDirectory() {
        if (resourceDirectory != null) {
            return resourceDirectory;
        }
        File rval = null;
        String pathToResourceDir = System.getProperty("genepattern.properties");
        if (pathToResourceDir != null) {
            rval = new File(pathToResourceDir);
        }
        else {
            log.error("Missing required system property, 'genepattern.properties'");
        }
        return rval;
    }

    private static synchronized CommandManager createCommandManager(String configParserClass, String configFile) {
        CommandManagerParser configParser = null;
        if (configParserClass == null) {
            return createDefaultCommandManager();
        }
        try {
            log.info("loading CommandManagerLoader from class "+configParserClass);
            configParser = (CommandManagerParser) Class.forName(configParserClass).newInstance();
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
    
    public static synchronized void reloadConfigFile() {
        if (parser == null || configFile == null || manager == null) {
            return;
        }
        try {
            CommandManagerParser cmcp = (CommandManagerParser) Class.forName(parser).newInstance();
            cmcp.reloadConfigFile(manager, configFile);
        }
        catch (Exception e) {
            log.error("Unable to instantiate CommandManagerConfigParser for name: "+parser, e);
        }
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
        CommandManager mgr = getCommandManager();
        Map<String,CommandExecutor> map = mgr.getCommandExecutorsMap();
        if (!map.containsValue(cmdExecutor)) {
            log.error("commandExecutorsMap does not contain value for "+cmdExecutor.getClass().getCanonicalName());
            return null;
        }
        
        for(Entry<String,CommandExecutor> entry : mgr.getCommandExecutorsMap().entrySet()) {
            if(cmdExecutor == entry.getValue()) {
                return entry.getKey();
            }
        }
        return null;
    }

}
