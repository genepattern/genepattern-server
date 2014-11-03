package org.genepattern.server.config;

import java.io.File;

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
    
    public static final String PROP_CONFIG_FILE = "config.file";
    //for compatibility with GP 3.2.3 and GP 3.2.4
    public static final String PROP_LEGACY_CONFIG_FILE = "command.manager.config.file";

    private static GpConfig gpConfigSingleton=null;
    
    private static File resourcesDir=null;
    public static void setResourcesDir(final File resourcesDir) {
        ServerConfigurationFactory.resourcesDir=resourcesDir;
    }
    
    private static File logDir=null;
    public static void setLogDir(final File logDir) {
        ServerConfigurationFactory.logDir=logDir;
    }

    /** Called from junit tests. */
    public static void setGpConfig(final GpConfig gpConfig) {
        gpConfigSingleton=gpConfig;
        ConfigRepositoryInfoLoader.clearCache();
    }

    private ServerConfigurationFactory() {
    }
    
    synchronized public static void reloadConfiguration() {
        gpConfigSingleton=GpConfigLoader.createFromSystemProps(resourcesDir, logDir);
        ConfigRepositoryInfoLoader.clearCache();
    }

    public static GpConfig instance() {
        // lazy init
        if (gpConfigSingleton==null) {
            gpConfigSingleton=GpConfigLoader.createFromSystemProps(logDir);
        }
        return gpConfigSingleton;
    }

}
