package org.genepattern.server.executor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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

    private static boolean running = false;
    private static List<Throwable> errors = new ArrayList<Throwable>();
    private static CommandManager manager = null;
    
    private CommandManagerFactory() {
    }
    
    public static String getParser() {
        return parser;
    }
    
    public static String getConfigFile() {
        return configFile;
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
     * Get the list of errors, if any, which resulted from instantiating the CommandManager.
     * 
     * Note: at the moment, calling reloadConfig does not reset the list of errors. This is because
     * there are some cases where errors in the config file can only be corrected
     * by restarting the job execution engine.
     * 
     * @return
     */
    public static List<Throwable> getInitializationErrors() {
        return errors;
    }
    
    public static boolean isRunning() {
        return running;
    }
    
    public static void startJobQueue() {
        //start the command executors before starting the internal job queue ...
        log.info("\tstarting job queue...");
        initializeCommandManager(System.getProperties());
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
     * Create a new instance of CommandManager, using the given properties.
     * This method replaces the current manager with a new instance.
     * 
     * @param properties
     */
    public static synchronized void initializeCommandManager(Properties properties) {
        if (manager != null) {
            log.info("replacing current command manager with a new instance");
        }
        errors.clear();
        setProperties(properties);
        manager = createCommandManager(parser, configFile);
    }

    /**
     * Create a new instance of the CommandManager, using the given parser and configuration file.
     * @param parserClass
     * @param configFilePath
     */
    public static synchronized void initializeCommandManager(final String parserClass, final String configFilePath) {
        parser = parserClass;
        configFile = configFilePath;
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
    
    public static File getConfigurationFile() throws ConfigurationException {
        return getConfigurationFile(configFile);
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
    public static File getConfigurationFile(String configuration) throws ConfigurationException {
        if (configuration == null || configuration.length() == 0) {
            return null;
        }
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
                throw new ConfigurationException("Configuration file does not exist: "+f.getAbsolutePath());
            }
            else {
                throw new ConfigurationException("Cannot read configuration file: "+f.getAbsolutePath());
            }
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
            errors.add(e);
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
            errors.add(e);
            log.error(e);
        }
        return commandManager;
    }
    
    public static synchronized void reloadConfigFile() {
        reloadConfigFile(configFile);
    }
    
    public static synchronized void reloadConfigFile(String filepath) {
        configFile = filepath;
        if (parser == null || configFile == null || manager == null) {
            log.error("reloadConfigFile("+filepath+") ignored!");
            return;
        }
        try {
            CommandManagerParser parserInstance = (CommandManagerParser) Class.forName(parser).newInstance();
            parserInstance.reloadConfigFile(manager, configFile);
        }
        catch (Exception e) {
            errors.add(e);
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
