package org.genepattern.server.config;

import org.apache.log4j.Logger;
import org.genepattern.server.repository.ConfigRepositoryInfoLoader;

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
    
    public static final String PROP_CONFIG_FILE = "config.file";
    //for compatibility with GP 3.2.3 and GP 3.2.4
    public static final String PROP_LEGACY_CONFIG_FILE = "command.manager.config.file";

    private static GpConfig gpConfigSingleton=GpConfigLoader.createFromSystemProps();
    
    private ServerConfigurationFactory() {
    }
    
    synchronized public static void reloadConfiguration() {
        gpConfigSingleton=GpConfigLoader.createFromSystemProps();
        ConfigRepositoryInfoLoader.clearCache();
    }
    synchronized public static void reloadConfiguration(final String configFilepath) {
        gpConfigSingleton=GpConfigLoader.createFromConfigFilepath(configFilepath);
        ConfigRepositoryInfoLoader.clearCache();
    }
    
    public static GpConfig instance() {
        return gpConfigSingleton;
    }

}
