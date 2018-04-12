package org.genepattern.server.executor.awsbatch.testutil;

import static org.junit.Assert.fail;

import java.io.File;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.genepattern.server.config.GpConfig;

public class Util {
    /**
     * Mock lsid prefix for testing.
     * <pre>
     *   lsid=urn:lsid:{lsid.authority}:{namespace}:{identifier}:{version}
     * </pre>
     * Usage:
     * <pre>
     *   String mockLsid=MOCK_LSID_PREFIX=":00123:1";
     * </pre>
     */
    public static final String MOCK_LSID_PREFIX="urn:lsid:com.example:mock.module.analysis";

    public static final String PROP_GP_CONF_DIR="GP_CONF_DIR";
    public static final String DEFAULT_GP_CONF_DIR="gp-jobrunner-awsbatch/src/main/conf";
    
    //public static final String PROP_GP_DATA_DIR="GP_DATA_DIR";
    //public static final String DEFAULT_GP_DATA_DIR="test/data";
    
    private static final File confDir=initFile(PROP_GP_CONF_DIR, DEFAULT_GP_CONF_DIR);

    //private static final File dataDir=initFile(PROP_GP_DATA_DIR, DEFAULT_GP_DATA_DIR);
    
    /** 
     * Initialize the dataDir, optionally from the GP_DATA_DIR
     * system property.
     */
    protected static File initFile(final String prop_name, final String default_path) { 
        return new File(System.getProperty(prop_name, default_path)).getAbsoluteFile();
    }

    /** 
     * Initialize the dataDir, optionally from the GP_DATA_DIR
     * system property.
     */
    protected static File initDataDir() { 
        return new File(System.getProperty("GP_DATA_DIR", "test/data")).getAbsoluteFile();
    }

    /**
     * Get the top-level directory for data files used by the unit tests. 
     * It's hard-coded to 'test/data'.
     * Set GP_DATA_DIR as a system property if you need to use a different path.
     */
    //public static File getDataDir() {
    //    return dataDir;
    //}

    /**
     * Get the path to the awsbatch configuration file directory.
     * This is for unit tests.
      */
    public static File awsbatchConfDir() {
        return initFile("AWSBATCH_CONF_DIR", "gp-jobrunner-awsbatch/src/main/conf");
        //return confDir;
    }
    
    /**
     * factory method, create a new GpConfig instance from the given config_yaml file.
     * Initialize a GpConfig instance from a config_yaml file.
     * The returned value is not a mock.
     * 
     * Note: this automatically turns off log4j logging output.
     */
    public static GpConfig initGpConfig(final File configFile) throws Throwable {
        if (!configFile.exists()) { 
            fail("configFile doesn't exist: "+configFile);
        }
        LogManager.getRootLogger().setLevel(Level.OFF);
        final File webappDir=new File("website").getAbsoluteFile();
        final GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(webappDir)
            .configFile(configFile)
        .build();
        if (gpConfig.hasInitErrors()) {
            throw gpConfig.getInitializationErrors().get(0);
        }
        return gpConfig;
    }

}
