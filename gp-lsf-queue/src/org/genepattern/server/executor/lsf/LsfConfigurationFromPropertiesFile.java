package org.genepattern.server.executor.lsf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Helper class for getting default and job specific properties used on the 'bsub' command line.
 * @author pcarr
 *
 */
public class LsfConfigurationFromPropertiesFile {
    private static Logger log = Logger.getLogger(LsfConfigurationFromPropertiesFile.class);

    private static Properties lsfProperties = new Properties();
    private static String project="gp_dev";
    private static String queue="genepattern";
    private static String maxMemory="2"; //2G
    private static String lsfWrapperScript=null;
    private static String lsfOutputFilename=".lsf.out";
    private static boolean usePreExecCommand=false;
    
    /**
     * Load custom properties from a file name 'lsf.properties' in the resources directory (same directory as genepattern.properties file).
     * @return
     */
    public static Properties loadLsfProperties() {
        Properties lsfProperties = new Properties();
        File lsfPropertiesFile = new File(System.getProperty("genepattern.properties"), "lsf.properties");
        if (!lsfPropertiesFile.canRead()) {
            return lsfProperties;
        } 
        try {
            log.info("loading properties file: "+lsfPropertiesFile.getAbsolutePath());
            lsfProperties.load(new FileInputStream(lsfPropertiesFile));
        } 
        catch (IOException e) {
            log.error("Error loading properties file: "+lsfPropertiesFile.getAbsolutePath(), e);
        }
        return lsfProperties;
    }
    
    public static synchronized void setCustomProperties(Properties props) {
        lsfProperties.clear();
        lsfProperties.putAll(props);
        
        if (lsfProperties.containsKey("lsf.project")) {
            setProject(lsfProperties.getProperty("lsf.project"));
        }
        if (lsfProperties.containsKey("lsf.queue")) {
            setQueue(lsfProperties.getProperty("lsf.queue"));
        }
        if (lsfProperties.containsKey("lsf.max.memory")) {
            setMaxMemory(lsfProperties.getProperty("lsf.max.memory"));
        }
        if (lsfProperties.containsKey("lsf.wrapper.script")) {
            setWrapperScript(lsfProperties.getProperty("lsf.wrapper.script"));
        }
        if (lsfProperties.containsKey("lsf.output.filename")) {
            setLsfOutputFilename(lsfProperties.getProperty("lsf.output.filename"));
        }
        if (lsfProperties.containsKey("lsf.use.pre.exec.command")) {
            setUsePreExecCommandFromProperty(lsfProperties.getProperty("lsf.use.pre.exec.command"));
        }

    }
    
    public static void setProject(String s) {
        project=s;
    }
    public static String getProject() {
        return project;
    }

    public static void setQueue(String s) {
        queue=s;
    }
    public static String getQueue() {
        return queue;
    }

    public static void setMaxMemory(String s) {
        //validate maxMemory
        if (s == null) {
            maxMemory = "2";
        }
        try {
            Integer.parseInt(s);
        }
        catch (NumberFormatException e) {
            log.error("Invalid setting for 'lsf.max.memory="+s+"': "+e.getLocalizedMessage(), e);
            maxMemory="2";
        }
        maxMemory=s;
    }
    public static String getMaxMemory() {
        return maxMemory;
    }

    public static void setWrapperScript(String s) {
        log.debug("setting lsf.wrapper.script: "+s+" ...");
        if (s != null) {
            File f = new File(s);
            if (!f.isAbsolute()) {
                f = new File(System.getProperty("genepattern.properties"), s);
            }
            if (!f.isFile() || !f.canRead()) {
                log.error("Configuration error, 'lsf.wrapper.script="+s+"' can't read: "+f.getAbsolutePath());
                s=null;
            }
            else {
                s=f.getAbsolutePath();
            }
        }
        lsfWrapperScript=s;
        log.debug("lsf.wrapper.script="+lsfWrapperScript);
    }
    public static String getWrapperScript() {
        return lsfWrapperScript;
    }
    
    public static void setLsfOutputFilename(String s) {
        lsfOutputFilename=s;
    }
    public static String getLsfOutputFilename() {
        return lsfOutputFilename;
    }
    
    public static void setUsePreExecCommandFromProperty(String s) {
        usePreExecCommand=Boolean.valueOf(s);
        log.debug("lsf.use.pre.exec.command="+usePreExecCommand);
    }
    public static boolean getUsePreExecCommand() {
        return usePreExecCommand;
    }
}
