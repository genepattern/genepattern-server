package org.genepattern.server.config;

import java.io.File;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * Helper class to keep a record of the Properties loaded from a File.
 * 
 * @author pcarr
 */
public class Record {
    private static final Logger log = Logger.getLogger(Record.class);

    /**
     * Create a new record from the propFile. 
     * Special-cases:
     *     when propFile is null, return null
     *     when propFile does not exist, return an empty record
     * 
     * @param propFile, the properties file to load
     * @return
     */
    public static final Record createFromPropertiesFile(final File propFile) {
        if (propFile==null) {
            return null;
        }
        return new Record(propFile);
    }
    
    private final File propFile;
    private final long dateLoaded;
    final ImmutableMap<String,String> props;

    public Record(final Properties from) {
        this.propFile=null;
        this.dateLoaded=System.currentTimeMillis();
        this.props=Maps.fromProperties(from);
    }

    public Record(final File propFile) {
        if (propFile==null) {
            throw new IllegalArgumentException("propFile==null");
        }
        this.propFile=propFile;
        final Properties props=GpServerProperties.loadProps(propFile);
        this.dateLoaded = System.currentTimeMillis();
        this.props=Maps.fromProperties(props);
    }
    
    /** get an immutable map of the properties */
    public ImmutableMap<String,String> getProps() {
        return props;
    }
    
    /** get a copy of the properties as a Properties instance */
    public Properties cloneProperties() {
        Properties p = new Properties();
        p.putAll(props);
        return p;
    }
    
    public File getPropertiesFile() {
        return propFile;
    }

    public long getDateLoaded() {
        return dateLoaded;
    }

    public Record reloadProps() {
        return new Record(propFile);
    }
    
    public void saveProperty(final String key, final String value) {
        final Record existingRecord=reloadProps();
        final Properties props=new Properties();
        props.putAll(existingRecord.getProps());
        props.setProperty(key, value);
        saveProperties(props);
    }
    
    public void deleteProperty(final String key) {
        final Record existingRecord=reloadProps();
        final Properties props=new Properties();
        props.putAll(existingRecord.getProps());
        if (props.containsKey(key)) { 
            props.remove(key);
            saveProperties(props);
        }
    }

    public void saveProperties(final Properties properties) {
        boolean success = GpServerProperties.writePropertiesIgnoreError(properties, this.propFile, " ");
        if (success) {
            ServerConfigurationFactory.reloadConfiguration();
        }
        else {
            log.error("Error saving properties to file: "+this.propFile);
        }
    }
}