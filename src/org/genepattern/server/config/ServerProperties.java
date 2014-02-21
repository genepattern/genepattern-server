package org.genepattern.server.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;

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
    private boolean useSystemProperties = true;
    private boolean usePropertiesFiles = true;
    
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
    private Properties flattened = new Properties();
    
    private ServerProperties() {
        reloadProperties();
    }
    
    public synchronized void reloadProperties() {
        String r = System.getProperty("genepattern.properties");
        if (r==null) {
            log.error("SystemProperty not defined, 'genepattern.properties'");
            return;
        }
        File resourceDir = new File(r);
        reloadProperties(resourceDir);
    }

    public synchronized void reloadProperties(File resourceDir) {
        flattened.clear();
        propertiesList.clear();

        File genepatternPropsFile = new File(resourceDir, "genepattern.properties");
        appendPropertiesFromFile(genepatternPropsFile);

        File customPropsFile = new File(resourceDir, "custom.properties");
        appendPropertiesFromFile(customPropsFile);
        
        File buildPropsFile = new File(resourceDir, "build.properties");
        appendPropertiesFromFile(buildPropsFile);
    }

    private void appendPropertiesFromFile(File propsFile) {
        if (propsFile.canRead()) {
            Record propsRecord = Record.loadPropertiesFromFile(propsFile);
            if (propsRecord != null) {
                propertiesList.put(propsFile.getName(), propsRecord);
                flattened.putAll(propsRecord.props);
            }
        }
    }
    
    public Value getValue(final GpContext context, final String key) {
        String from = getProperty(context, key);
        if (from != null) {
            return new Value(from);
        }
        return null;
    }

    public String getProperty(GpContext context, final String key) {
        if (context==null) {
            log.error("context==null");
            //initialize, so that we use the default values
            context=new GpContext();
        }
        String rval = null;
        if (context.getCheckSystemProperties()) {
            rval = System.getProperty(key);
        }
        if (context.getCheckPropertiesFiles()) {
            for(Record record : propertiesList.values()) {
                if (record.props.containsKey(key)) {
                    rval = record.props.getProperty(key);
                }
            }
        }
        return rval;
    }

    public String getProperty(String key) {
        String rval = null;
        if (this.useSystemProperties) {
            rval = System.getProperty(key);
        }
        if (this.usePropertiesFiles) {
            for(Record record : propertiesList.values()) {
                if (record.props.containsKey(key)) {
                    rval = record.props.getProperty(key);
                }
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
