package org.genepattern.server.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.genepattern.server.executor.CommandManager;
import org.genepattern.server.executor.CommandManagerParser;
import org.genepattern.server.executor.ConfigurationException;

/**
 * Initialize and hold a single instance of a ServerConfiguration for the GenePattern Server.
 * By using this factory we can reload the properties file without a server restart.
 * 
 * An instance is created by passing a config file to a parser which implements the ConfigurationFileParser interface.
 * Add the following properties to the 'genepattern.properties' file to override the default settings:
 * 
 * <code>
 * command.manager.parser=<class which implements org.genepattern.server.exec.CommandManagerParser>
 * command.manager.config.file=<configuration file>
 * </code>
 * 
 * @author pcarr
 */
public class ServerConfigurationFactory {
    private static Logger log = Logger.getLogger(ServerConfigurationFactory.class);

    private final static String PROP_PARSER="config.file.parser";
    private final static String PROP_CONFIG_FILE="config.file";

    //aka the location of the genepattern.properties file
    private static File resourceDirectory = null;
    
    private static String parser = null;
    private static String configFile = null;

    private static List<Throwable> errors = new ArrayList<Throwable>();
    private static CommandManager manager = null;
    
    private ServerConfigurationFactory() {
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
        initializeFromProperties(null);
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
    
    /**
     * Create a new instance of CommandManager, using the given properties.
     * This method replaces the current manager with a new instance.
     * 
     * @param properties
     */
    public static synchronized void initializeFromProperties(Properties properties) {
        if (manager != null) {
            log.info("replacing current server configuration with a new instance");
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
    public static synchronized void initializeFromClasspath(final String parserClass, final String configFilePath) {
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
        parser = properties.getProperty(PROP_PARSER);
        configFile = properties.getProperty(PROP_CONFIG_FILE);
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
            throw new IllegalArgumentException("configParserClass=null");
        }
        try {
            log.info("loading ServerConfigurationParser from class "+configParserClass);
            configParser = (CommandManagerParser) Class.forName(configParserClass).newInstance();
            return configParser.parseConfigFile(configFile);
        } 
        catch (final Exception e) {
            errors.add(e);
            log.error("Failed to load custom command manager loader class: "+configParserClass, e);
            throw new IllegalArgumentException(e);
        }
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

}
