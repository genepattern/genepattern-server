package org.genepattern.server.config;

import java.io.File;

import org.apache.log4j.Logger;
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
    
    private ServerConfigurationFactory() {
    }
    
    // legacy code, which uses the 'config.file' property from the genepattern.properties file
    // to maintain a singleton instance of a ServerConfiguration 
    /**
     * Get a File object for the named configuration file as specified in the 'genepattern.properties' file. E.g.
     * <code>
     *     config.file=config_default.yaml
     *     or
     *     config.file=/fully/qualified/path/to/config.yaml
     * </code>
     * If a relative path is given, load the file relative to the resources directory as specified by the 
     * system property, 'genepattern.properties'. 
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

    /**
     * Get the resource directory, the parent directory of the genepattern.properties file.
     * @return a File or null if there is a configuration error 
     */
    private static File getResourceDirectory() {
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

    private static ServerConfiguration singleton = new ServerConfiguration();
    public static ServerConfiguration instance() {
        return singleton;
    }
    public static void reloadConfiguration() {
        singleton.reloadConfiguration();
    }
    public static void reloadConfiguration(final String configFilepath) {
        singleton.reloadConfiguration(configFilepath);
    }


}
