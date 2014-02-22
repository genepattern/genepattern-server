package org.genepattern.server.config;

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
    private static ServerConfigurationV1 singletonV1 = new ServerConfigurationV1();
    public static ServerConfiguration instance() {
        return singletonV1;
    }
    public static void reloadConfiguration() {
        singletonV1.reloadConfiguration();
    }
    public static void reloadConfiguration(final String configFilepath) {
        singletonV1.reloadConfiguration(configFilepath);
    }

//    // proposed new code
//    private static ServerConfigurationWrapper singletonWrapper=new ServerConfigurationWrapper(init());
//    public static ServerConfigurationWrapper instanceWrapper() {
//        return singletonWrapper;
//    }
//    public static void reloadConfigurationWrapper() {
//        singletonWrapper=new ServerConfigurationWrapper(init());
//    }
//    public static void reloadConfigurationWrapper(final String configFilepath) {
//        final File configFile=ConfigFileParser.initConfigurationFile(configFilepath);
//        singletonWrapper=new ServerConfigurationWrapper(init(configFile));
//    }
//    
//    private static ServerConfigurationV2 init() {
//        return new ServerConfigurationV2.Builder().build();
//    }
//    
//    private static ServerConfigurationV2 init(final File configFile) {
//        return new ServerConfigurationV2.Builder()
//            .configFile(configFile)
//            .build();
//    }

}
