package org.genepattern.server.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Utility class for managing the loading of properties from the genepattern.properties file, custom.properties file, et cetera.
 * This class should be used to replace all calls to System.getProperty() from the source code.
 * Once that has occurred it is fairly straightforward to allow reloading the properties without having to do a server restart.
 * 
 * The following properties files are loaded, in order, after reading System.properties:
 *     genepattern.properties, custom.properties, and build.properties.
 * 
 * TODO: include loading other legacy properties files, commandPrefix.properties, messages.properties, java_flags.properties, and taskPrefixMapping.properties
 * 
 * @author pcarr
 */
public class ServerProperties {
    private static Logger log = Logger.getLogger(ServerProperties.class);
    
    private static class Record {
        private Properties props;
        private File propFile;
        private Date dateLoaded = new Date();
        
        static Record loadPropertiesFromFile(File f) {
            Record r = new Record();
            r.propFile = f;
            r.props = new Properties();
            
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(r.propFile);
            }
            catch (FileNotFoundException e) {
                log.error("Configuration error, unable to load properties from file: "+f.getAbsolutePath(), e);
                return null;
            }
            try {
                r.props.load(fis);
            }
            catch (IOException e) {
                log.error("Configuration error, unable to load properties from file: "+f.getAbsolutePath(), e);
                return null;
            }
            r.dateLoaded = new Date();
            return r;
        }
    }
    
    private LinkedHashMap<String,Record> propertiesList = new LinkedHashMap<String,Record>();
    
    private ServerProperties() {
        reloadProperties();
    }
    
    public synchronized void reloadProperties() {
        Record sysProps = new Record();
        sysProps.props = new Properties(System.getProperties());
        propertiesList.put("system", sysProps);
        
        String dir = sysProps.props.getProperty("genepattern.properties");
        File genepatternPropsFile = new File(dir, "genepattern.properties");
        Record genepatternProps = Record.loadPropertiesFromFile(genepatternPropsFile); 
        if (genepatternProps != null) {
            propertiesList.put("genepattern.properties", genepatternProps);
        }

        File customPropsFile = new File(dir, "custom.properties");
        if (customPropsFile.canRead()) {
            Record customProps = Record.loadPropertiesFromFile(customPropsFile);
            if (customProps != null) {
                propertiesList.put("custom.properties", customProps);
            }
        }
        
        File buildPropsFile = new File(dir, "build.properties");
        if (buildPropsFile.canRead()) {
            Record buildProps = Record.loadPropertiesFromFile(buildPropsFile);
            if (buildProps != null) {
                propertiesList.put("build.properties", buildProps);
            }
        }
    }
    
    public String getProperty(String key) {
        String rval = null;
        for(Record record : propertiesList.values()) {
            if (record.props.containsKey(key)) {
                rval = record.props.getProperty(key);
            }
        }
        return rval;
    }
    
    public static ServerProperties instance() {
        return Singleton.instance;
    }

    private static class Singleton {
        static public ServerProperties instance = new ServerProperties();
    }

}
