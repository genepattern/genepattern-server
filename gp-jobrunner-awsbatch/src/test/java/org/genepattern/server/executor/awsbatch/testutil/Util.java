package org.genepattern.server.executor.awsbatch.testutil;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

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

    /**
     * Initialize a path to a test file or folder, optionally from a system property.
     * @param SYS_PROP a System property
     * @param default_path a default value
     * 
     * Example:
     * <pre>
     *   File gpDataDir=initFile("GP_DATA_DIR", "test/data");
     * </pre>
     */
    protected static File initFile(final String SYS_PROP, final String default_path) { 
        return new File(System.getProperty(SYS_PROP, default_path)).getAbsoluteFile();
    }

    /**
     * Initialize the dataDir, optionally from the GP_DATA_DIR system property.
     * Default: 'test/data'
     * Customize:
     *   -DGP_DATA_DIR="../test/data"
     */
    public static File getDataDir() {
        return initFile("GP_DATA_DIR", "test/data");
    }

    /**
     * Initialize the awsbatchConfDir, optionally from the AWSBATCH_CONF_DIR system property.
     * Default: 'gp-jobrunner-awsbatch/src/main/conf'
     * Customize:
     *   -DAWSBATCH_CONF_DIR="src/main/conf"
     */
    public static File getAwsbatchConfDir() {
        return initFile("AWSBATCH_CONF_DIR", "gp-jobrunner-awsbatch/src/main/conf");
    }

    /**
     * Load a file from the standard maven location:
     *     gp-jobrunner-awsbatch/src/test/resources
     */
    public static File getTestResource(final Class<?> clazz, final String filename) throws UnsupportedEncodingException {
        final URL url=clazz.getResource(filename);
        if (url==null) {
            fail("test resource not found: "+filename);
        }
        final File file = new File( URLDecoder.decode( url.getFile(), "UTF-8" ) );
        if (!file.exists()) {
            fail("test resource file doesn't exist: "+file);
        }
        return file;
    }



}
