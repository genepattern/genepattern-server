package org.genepattern.server.config;

import org.apache.log4j.Logger;
import org.genepattern.server.executor.BasicCommandManager;
import org.genepattern.server.executor.CommandManager;
import org.genepattern.server.executor.CommandManagerFactory;
import org.genepattern.server.executor.CommandProperties;
import org.genepattern.webservice.JobInfo;

/**
 * Server configuration.
 * 
 * @author pcarr
 */
public class ServerConfiguration {
    private static Logger log = Logger.getLogger(ServerConfiguration.class);

    public static class Context {
        //hard-coded default value is true for compatibility with GP 3.2.4 and earlier
        private boolean checkSystemProperties = true;
        //hard-coded default value is true for compatibility with GP 3.2.4 and earlier
        private boolean checkPropertiesFiles = true;
        private String userId = null;
        private JobInfo jobInfo = null;
        
        public static Context getServerContext() {
            Context context = new Context();
            return context;
        }

        public static Context getContextForUser(String userId) {
            Context context = new Context();
            if (userId != null) {
                context.setUserId(userId);
            }
            return context;
        }

        public static Context getContextForJob(JobInfo jobInfo) {
            Context context = new Context();
            context.setCheckPropertiesFiles(false);
            context.setCheckSystemProperties(false);
            if (jobInfo != null) {
                context.setJobInfo(jobInfo);
                if (jobInfo.getUserId() != null) {
                    context.setUserId(jobInfo.getUserId());
                }
            }
            return context;
        }
        
        public void setCheckSystemProperties(boolean b) {
            this.checkSystemProperties = b;
        }

        public boolean getCheckSystemProperties() {
            return checkSystemProperties;
        }

        public void setCheckPropertiesFiles(boolean b) {
            this.checkPropertiesFiles = b;
        }
        
        public boolean getCheckPropertiesFiles() {
            return checkPropertiesFiles;
        }
        
        public void setUserId(String userId) {
            this.userId = userId;
        }
        public String getUserId() {
            return userId;
        }
        
        public void setJobInfo(JobInfo jobInfo) {
            this.jobInfo = jobInfo;
        }
        public JobInfo getJobInfo() {
            return jobInfo;
        }
    }

    private static ServerConfiguration singleton = new ServerConfiguration();
    public static ServerConfiguration instance() {
        return singleton;
    }
    
    private ServerConfiguration() {
    }

    /**
     * Utility method for parsing properties as a boolean.
     * The current implementation uses Boolean.parseBoolean, 
     * which returns true iff the property is set and equalsIgnoreCase 'true'.
     * 
     * @param key
     * @return
     */
    public boolean getGPBooleanProperty(Context context, String key) {
        String prop = getGPProperty(context, key);
        return Boolean.parseBoolean(prop);
    }
    
    /**
     * Utility method for parsing a property as an Integer.
     * 
     * When a non integer value is set in the config file, the default value is returned.
     * Errors are logged, but exceptions are not thrown.
     * 
     * @param key
     * @param defaultValue
     * 
     * @return the int value for the property, or the default value, can return null.
     */
    public Integer getGPIntegerProperty(Context context, String key, Integer defaultValue) {
        String val = getGPProperty(context, key);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val);
        }
        catch (NumberFormatException e) {
            log.error("Error parsing integer value for property, "+key+"="+val);
            return defaultValue;
        }
    }


    public String getGPProperty(Context context, String key) {
        CommandManager cmdMgr = CommandManagerFactory.getCommandManager();
        //HACK: until I change the API for the CommandManager interface to take a Context arg
        if (cmdMgr instanceof BasicCommandManager) {
            BasicCommandManager defaultCmdMgr = (BasicCommandManager) cmdMgr;
            return defaultCmdMgr.getConfigProperties().getProperty(context, key);
        }
        //this should not be called unless the default (BasicCommandManager) implementation is replaced
        CommandProperties props = null;
        log.error("getCommandProperties(jobInfo) is deprecated; GP 3.3.2 uses getConfigProperties.getProperty(Context, key)");
        if (context.jobInfo != null) {
            props = CommandManagerFactory.getCommandManager().getCommandProperties(context.jobInfo);
        }
        else {
            props = CommandManagerFactory.getCommandManager().getCommandProperties(null);
        }
        return props.getProperty(key);
    }
}
