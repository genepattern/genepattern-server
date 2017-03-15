/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.google.common.collect.Maps;

/**
 * Utility class for managing the loading of properties from the genepattern.properties file, custom.properties file, et cetera.
 * This class should be used to replace all calls to System.getProperty() from the source code.
 * Once that has occurred it is fairly straightforward to allow reloading the properties without having to do a server restart.
 * 
 * The following properties files are loaded, in order, after reading System.properties:
 *     genepattern.properties, custom.properties, and build.properties.
 *     
 * Additional properties files are loaded as Records, but must be accessed individually. Their values are not
 * flattened into the stored properties.
 *     commandPrefix.properties
 *     taskPrefixMapping.properties
 * 
 * @author pcarr
 */
public class GpServerProperties {
    private static final Logger log = Logger.getLogger(GpServerProperties.class);

    /**
     * Helper method for initializing a new Properties instance from a config file.
     * Errors are reported to the log file.
     * You should verify that you can read the file before calling this method.
     * 
     * @param propFile
     * @return
     */
    public static Properties loadProps(final File propFile) {
        final Properties props=new Properties();
        final Long dateLoaded=loadProps(props, propFile);
        if (dateLoaded==null) {
            log.debug("dateLoaded==null");
        }
        return props;
    }

    /**
     * Helper method for loading the properties from the File into the given Properties instance.
     * 
     * @param props, must be non-null
     * @param propFile, the properties file
     * 
     * @return null on failure, or the current time that the file was loaded.
     */
    public static Long loadProps(Properties props, File propFile) {
        if (props==null) {
            throw new IllegalArgumentException("props==null");
        }
        if (propFile==null) {
            log.error("unexpected null arg, propFile==null");
            return null;
        }
        if (!propFile.exists()) {
            log.debug("File does not exist, propFile="+propFile);
            return System.currentTimeMillis();
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(propFile);
            props.load(fis);
            return System.currentTimeMillis();
        }
        catch (IOException e) {
            log.error("IOException reading file="+propFile.getAbsolutePath(), e);
            return null;
        }
        catch (Throwable t) {
            ///CLOVER:OFF
            log.error("unexpected error reading file="+propFile.getAbsolutePath(), t);
            return null;
            ///CLOVER:ON
        }
        finally {
            if (fis != null) {
                try {
                    fis.close();
                }
                catch (IOException e) {
                    ///CLOVER:OFF
                    log.error(e);
                    ///CLOVER:ON
                }
            }
        }
    }

    /**
     * Helper method for saving the given properties instance to the given file.
     * 
     * @param props
     * @param propFile
     * @param comment
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void writeProperties(final Properties props, final File propFile, final String comment)
    throws FileNotFoundException, IOException 
    {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(propFile);
            props.store(fos, comment);
        } 
        finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    public static boolean writePropertiesIgnoreError(final Properties props, final File propFile, final String comment)
    {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(propFile);
            props.store(fos, comment);
            return true;
        }
        catch (IOException e) {
            return false;
        }
        finally {
            if (fos != null) {
                try {
                    fos.close();
                }
                catch (IOException e) { 
                }
            }
        }
    }

    /**
     * Save custom plugin properties into the custom.properties file for the server.
     * You must reload the configuration to guarantee that the updates are available to the
     * GP runtime.
     * 
     * @param customPropsFile, the path to the GP server custom.properties file
     * @param pluginCustomProps, the custom properties to set from the plugin directory
     */
    public static void updateCustomProperties(final File customPropsFile, final Properties pluginCustomProps) throws FileNotFoundException, IOException {
        String comment=null;
        boolean skipExisting=true;
        updateCustomProperties(customPropsFile, pluginCustomProps, comment, skipExisting);
    }

    public static void updateCustomProperties(final File customPropsFile, final Properties pluginCustomProps, final String comment, final boolean skipExisting) throws FileNotFoundException, IOException {
        //1) load the file
        final Properties customProps=GpServerProperties.loadProps(customPropsFile);
        //2) save changes
        for(final String key : pluginCustomProps.stringPropertyNames()) {
            String updatedValue=pluginCustomProps.getProperty(key);
            String existingValue=customProps.getProperty(key);
            if (skipExisting && existingValue != null) {
                log.debug(key+"="+existingValue+" is already in custom.properties file");
                if (existingValue.equals(updatedValue)) {
                    log.debug("skipping duplicate custom plugin property");
                }
                else {
                    log.error(key+"="+existingValue+" is already in custom.properties file");
                    log.error("Ignoring custom plugin property, "+key+"="+updatedValue);
                }
            }
            else {
                customProps.setProperty(key, updatedValue);
            }
        }
        //3) save file
        GpServerProperties.writeProperties(customProps, customPropsFile, comment);
    }

    private Map<String,Record> propertiesList = new LinkedHashMap<String,Record>();

    private final boolean useSystemProperties; // default = false;
    private final boolean usePropertiesFiles;  // default = true;
    private final File resourcesDir; // can be null
    
    private final Map<String,String> serverProps;

    // additional properties files
    private final Record gpProps;
    private final Record customProps;
    private final Record buildProps;
    private final Record commandPrefixProps;
    private final Record taskPrefixMappingProps;

    private GpServerProperties(final Builder in) {
        log.debug("initializing GpServerProperties...");
        this.useSystemProperties=in.useSystemProperties;
        this.usePropertiesFiles=in.usePropertiesFiles;
        this.resourcesDir=in.resourcesDir;
        
        final Properties flattened=new Properties();
        if (in.initFromSystemProperties) {
            log.debug("loading system properties...");
            Record systemProps=new Record(System.getProperties());
            if (log.isDebugEnabled()) {
                log.debug("system.properties loaded at "+new Date(systemProps.getDateLoaded()));
            }
            propertiesList.put("system.properties", systemProps);
            flattened.putAll(systemProps.props);
        }
        this.gpProps=Record.createFromPropertiesFile(in.gpPropsFile);
        if (this.gpProps != null) {
            log.debug("loading genepattern.properties from file="+in.gpPropsFile);
            propertiesList.put("genepattern.properties", gpProps);
            flattened.putAll(gpProps.props);
        }
        this.customProps=Record.createFromPropertiesFile(in.customPropsFile);
        if (this.customProps != null) {
            log.debug("loading custom.properties from file="+in.customPropsFile);
            propertiesList.put("custom.properties", customProps);
            flattened.putAll(customProps.props);
        }
        if (in.customProperties != null) {
            flattened.putAll(in.customProperties);
        }
        this.buildProps=Record.createFromPropertiesFile(in.buildPropsFile);
        if (this.buildProps != null) {
            log.debug("loading build.properties from file="+in.buildPropsFile);
            propertiesList.put("build.properties", buildProps);
            flattened.putAll(buildProps.props);
        }
        this.serverProps=Maps.fromProperties(flattened);
        
        // additional records
        this.commandPrefixProps=Record.createFromPropertiesFile(in.commandPrefixPropsFile);
        this.taskPrefixMappingProps=Record.createFromPropertiesFile(in.taskPrefixMappingPropsFile);
    }
    
    public File getResourcesDir() {
        return resourcesDir;
    }
    
    public String getProperty(final String key) {
        if (this.usePropertiesFiles) {
            if (serverProps.containsKey(key)) {
                return serverProps.get(key);
            }
        }
        if (this.useSystemProperties) {
            return System.getProperty(key);
        }
        return null;
    }

    public String getProperty(final String key, final String defaultValue) {
        String rval=getProperty(key);
        if (rval==null) {
            return defaultValue;
        }
        return rval;
    } 
    
    public Value getValue(GpContext context, final String key) {
        if (context==null) {
            context=GpContext.getServerContext();
        }
        final String prop=getProperty(key);
        if (prop==null) {
            return null;
        }
        return new Value(prop);
    }
    
    /**
     * Helper method to find out if a given property is set in the genepattern.properties file.
     * @param key
     * @return
     */
    public boolean isSetInGpProperties(final String key) {
        return isSetIn("genepattern.properties", key);
    }
    
    protected boolean isSetIn(final String fileType, final String key) {
        Record record=propertiesList.get(fileType);
        if (record==null) {
            return false;
        }
        return record.getProps().containsKey(key);
    }
    
    public Record getCommandPrefixProps() {
        return commandPrefixProps;
    }
    
    public Record getTaskPrefixMappingProps() {
        return taskPrefixMappingProps;
    }
    
    public static final class Builder {
        private boolean initFromSystemProperties = true;
        private boolean useSystemProperties = false;
        private boolean usePropertiesFiles = true;
        private File resourcesDir=null;
        private File gpPropsFile=null;
        private File customPropsFile=null;
        private File buildPropsFile=null;
        private File commandPrefixPropsFile=null;
        private File taskPrefixMappingPropsFile=null;
        private Properties customProperties=null;

        public Builder initFromSystemProperties(final boolean initFromSystemProperties) {
            this.initFromSystemProperties=initFromSystemProperties;
            return this;
        }
        public Builder useSystemProperties(final boolean useSystemProperties) {
            this.useSystemProperties=useSystemProperties;
            return this;
        }
        public Builder usePropertiesFiles(final boolean usePropertiesFiles) {
            this.usePropertiesFiles=usePropertiesFiles;
            return this;
        }
        public Builder resourcesDir(final File resourcesDir) {
            this.resourcesDir=resourcesDir;
            return this;
        }
        public Builder gpProperties(final File gpProperties) {
            this.gpPropsFile=gpProperties;
            return this;
        }
        
        public Builder customProperties(final File customProperties) {
            this.customPropsFile=customProperties;
            return this;
        }
        
        public Builder buildProperties(final File buildProperties) {
            this.buildPropsFile=buildProperties;
            return this;
        }
        
        public Builder commandPrefixProperties(final File commandPrefixProperties) {
            this.commandPrefixPropsFile=commandPrefixProperties;
            return this;
        }
        
        public Builder addCustomProperty(final String key, final String value) {
            if (customProperties==null) {
                customProperties=new Properties();
            }
            customProperties.setProperty(key, value);
            return this;
        }
        
        public Builder addCustomProperties(final Properties props) {
            if (customProperties==null) {
                customProperties=new Properties();
            }
            for (Enumeration<?> propertyNames = props.propertyNames(); propertyNames.hasMoreElements(); ) {
                Object key = propertyNames.nextElement();
                customProperties.put(key, props.get(key));
            }
            return this;
        }
        
        protected File initPropFile(final File propFile, final String name) {
            if (propFile==null && resourcesDir != null) {
                return new File(resourcesDir, name);
            }
            else {
                return propFile;
            }
        }

        public GpServerProperties build() throws IllegalArgumentException {
            gpPropsFile                = initPropFile(gpPropsFile,                "genepattern.properties");
            customPropsFile            = initPropFile(customPropsFile,            "custom.properties");
            buildPropsFile             = initPropFile(buildPropsFile,             "build.properties");
            commandPrefixPropsFile     = initPropFile(commandPrefixPropsFile,     "commandPrefix.properties");
            taskPrefixMappingPropsFile = initPropFile(taskPrefixMappingPropsFile, "taskPrefixMapping.properties");
            return new GpServerProperties(this);
        }
    }

}
