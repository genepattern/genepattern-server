package org.genepattern.server.config;

import java.io.File;

import org.apache.log4j.Logger;

import com.google.common.base.Strings;

public class GpConfigLoader {
    private static Logger log = Logger.getLogger(GpConfigLoader.class);

    public static final String PROP_GP_HOME="gp.home";
    public static final String PROP_RESOURCES="resources";
    public static final String PROP_GENEPATTERN_PROPERTIES="genepattern.properties";


    public static File initFileFromStr(final String fileStr) throws Exception {
        return initFileFromStr(null, fileStr);
    }
    public static File initFileFromStr(final File parentDir, final String fileStr) throws Exception {
        if (Strings.isNullOrEmpty(fileStr)) {
            throw new Exception("fileStr not set");
        }
        final File f=new File(fileStr);
        final File theFile;
        if (f.isAbsolute()) {
            theFile=f;
        }
        else if (parentDir != null) {
            theFile=new File(parentDir, f.getPath());
        }
        else {
            theFile=f.getAbsoluteFile();
        }
        
        if (!theFile.exists()) {
            throw new Exception("file does not exist: "+theFile.getPath());
        }
        else if (!theFile.canRead()) {
            throw new Exception("can't read file: "+theFile.getPath());
        }
        return theFile;
    }

    /**
     * Get the resources directory, the parent directory of the genepattern.properties file.
     * @return a File or null if there is a configuration error 
     */
    protected static File initResourcesDirFromSystemProps() throws Exception {
        log.debug("initializing resources directory from system properties ...");
        final File workingDir=new File("");

        File resourcesDir = null;
        if (System.getProperties().containsKey(PROP_GENEPATTERN_PROPERTIES)) {
            resourcesDir = initFileFromStr( workingDir, System.getProperty(PROP_GENEPATTERN_PROPERTIES) );
        }
        else if (System.getProperties().containsKey(PROP_RESOURCES)) {
            resourcesDir = initFileFromStr( workingDir, System.getProperty(PROP_RESOURCES) );
        }
        else {
            resourcesDir = new File("../resources");
        }

        if (!resourcesDir.exists()) {
            log.error("resources dir does not exist: "+resourcesDir.getAbsolutePath());
            throw new Exception("resources dir does not exist: "+resourcesDir.getAbsolutePath());
        }
        if (!resourcesDir.canRead()) {
            log.error("can't read resources dir: "+resourcesDir.getAbsolutePath());
            throw new Exception("can't read resources dir: "+resourcesDir.getAbsolutePath());
        }
        log.debug("resources directory="+resourcesDir);
        return resourcesDir;
    }

    public static File initConfigFileFromServerProps(final GpServerProperties serverProperties) throws Exception {
        String configFilepath=serverProperties.getProperty(ServerConfigurationFactory.PROP_CONFIG_FILE);
        if (configFilepath == null) {
            configFilepath = serverProperties.getProperty(ServerConfigurationFactory.PROP_LEGACY_CONFIG_FILE);
            log.info(""+ServerConfigurationFactory.PROP_CONFIG_FILE+" not set, checking "+ServerConfigurationFactory.PROP_LEGACY_CONFIG_FILE);
        }
        if (configFilepath == null) {
            configFilepath = "config_default.yaml";
            log.info(""+ServerConfigurationFactory.PROP_CONFIG_FILE+" not set, using default config file: "+configFilepath);
        }
        return initFileFromStr(serverProperties.getResourcesDir(), configFilepath);
    }

    public static GpConfig createFromSystemProps() {
        File logDir=null;
        return createFromSystemProps(logDir);
    }
    
    public static GpConfig createFromSystemProps(final File logDir) {
        GpConfig.Builder builder=new GpConfig.Builder();
        builder.logDir(logDir);
        File resourcesDir=null;
        try {
            resourcesDir=initResourcesDirFromSystemProps();
            builder.resourcesDir(resourcesDir);
        }
        catch (Throwable t) {
            builder.addError(t);
        }
        GpServerProperties serverProperties=new GpServerProperties.Builder()
            .resourcesDir(resourcesDir)
            .build();
        builder.serverProperties(serverProperties);
        try {
            File configFile=initConfigFileFromServerProps(serverProperties);
            builder.configFile(configFile);
        }
        catch (Throwable t) {
            builder.addError(t);
        }
        return builder.build();
    }
    
    public static GpConfig createFromSystemProps(final File resourcesDir, final File logDir) {
        GpConfig.Builder builder=new GpConfig.Builder();
        builder.logDir(logDir);
        builder.resourcesDir(resourcesDir);
        GpServerProperties serverProperties=new GpServerProperties.Builder()
            .resourcesDir(resourcesDir)
            .build();
        builder.serverProperties(serverProperties);
        try {
            File configFile=initConfigFileFromServerProps(serverProperties);
            builder.configFile(configFile);
        }
        catch (Throwable t) {
            builder.addError(t);
        }
        return builder.build();
    }

    public static GpConfig createFromConfigFilepath(final String configFilepath) {
        GpConfig.Builder builder=new GpConfig.Builder();
        File resourcesDir=null;
        try {
            resourcesDir=initResourcesDirFromSystemProps();
            builder.resourcesDir(resourcesDir);
        }
        catch (Throwable t) {
            builder.addError(t);
        }
        GpServerProperties serverProperties=new GpServerProperties.Builder()
            .resourcesDir(resourcesDir)
            .build();
        builder.serverProperties(serverProperties);
        if (resourcesDir != null) {
            try {
                builder.configFile( initFileFromStr(resourcesDir, configFilepath) );
            }
            catch (Throwable t) {
                builder.addError(t);
            }
        }
        return builder.build();
    }

    public static GpConfig createFromResourcesDir(final File resourcesDir) {
        GpConfig.Builder builder=new GpConfig.Builder();
        builder.resourcesDir(resourcesDir);
        GpServerProperties serverProperties=new GpServerProperties.Builder()
            .resourcesDir(resourcesDir)
            .build();
        builder.serverProperties(serverProperties);
        try {
            File configFile=initConfigFileFromServerProps(serverProperties);
            builder.configFile(configFile);
        }
        catch (Throwable t) {
            builder.addError(t);
        }
        return builder.build();
    }

    public static GpConfig createFromConfigYaml(final File configYaml) {
        GpConfig.Builder builder=new GpConfig.Builder();
        builder=builder.resourcesDir(configYaml.getParentFile());
        builder=builder.configFile(configYaml);
        return builder.build();
    }
}
