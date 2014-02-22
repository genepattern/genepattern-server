package org.genepattern.server.config;

import java.io.File;

import org.apache.log4j.Logger;

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
