package org.genepattern.server.executor.lsf;

public class LsfConfigurationFactory {
    public static LsfConfiguration getLsfConfiguration() {
        return getLsfConfigurationJson();
        
    }
    private static LsfConfigurationJson getLsfConfigurationJson() {
        LsfConfigurationJson config = new LsfConfigurationJson();
        try {
            config.reloadConfiguration();
        }
        catch (Exception e) {
            //ignoring, it is logged in the reloadPropertiesFromFile ...
            return new LsfConfigurationJson();            
        }
        return config;
    }
}
