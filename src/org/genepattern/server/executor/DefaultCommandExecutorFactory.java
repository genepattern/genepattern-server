package org.genepattern.server.executor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Default implementation of the CommandExecutorFactory interface, it loads configuration settings 
 * from a single properties file, 'executor.properties', which must be in the same directory as the 'genepattern.properties' file.
 * 
 * @author pcarr
 */
public class DefaultCommandExecutorFactory implements CommandExecutorFactory {
    private static Logger log = Logger.getLogger(DefaultCommandExecutorFactory.class);
    
    private List<CommandExecutor> cmdExecutorList = new ArrayList<CommandExecutor>();
    private DefaultCommandExecutorMapper mapper = new DefaultCommandExecutorMapper();

    public DefaultCommandExecutorFactory() {
    }

    private boolean initialized = false;

    /**
     * Initialize the list of CommandExecutors.
     */
    private void init() {
        if (initialized) {
            return;
        }
        synchronized(this) {
            try {
                Properties executorProperties = loadExecutorProperties();
                initFromProperties(executorProperties);
            }
            catch (Exception e) {
                log.error("Unable to load executor properties, using default settings", e);
                defaultInit();
                return;
            }
            finally {
                initialized=true;
            }
        }
    }
    
    private Properties loadExecutorProperties() throws Exception {
        File executorPropertiesFile = new File(System.getProperty("genepattern.properties"), "executor.properties");
        if (!executorPropertiesFile.canRead()) {
            if (!executorPropertiesFile.exists()) {
                throw new Exception("Command executor configuration file not found: "+executorPropertiesFile.getPath());
            }
            else {
                throw new Exception("Not able to read command executor configuration file: "+executorPropertiesFile.getPath());
            }
        }
        Properties executorProperties = new Properties();
        try {
            executorProperties.load(new FileInputStream(executorPropertiesFile));
        } 
        catch (IOException e) {
            throw new Exception("Failed to initialize command executor factory: "+e.getLocalizedMessage(), e);
        }
        return executorProperties;
    }

    /**
     * Configuration when no properties file is found.
     */
    private void defaultInit() {
        log.info("Using default configuration for command execution");
        //use runtime exec for all jobs
        CommandExecutor cmdExec = new RuntimeCommandExecutor();
        cmdExecutorList.add(cmdExec);
        mapper = new DefaultCommandExecutorMapper();
        mapper.addCommandExecutor("default", cmdExec);
        mapper.setDefaultCmdExecId("default");
    }
    
    private void initFromProperties(Properties props) {
        initFromProperties(props, true);
    }
    private void reloadMapperFromProperties(Properties props) {
        mapper = new DefaultCommandExecutorMapper();
        initFromProperties(props, false);
    }
    private void initFromProperties(Properties props, boolean loadExecutors) {
        Enumeration e = props.propertyNames();
        while(e.hasMoreElements()) {
            String propName = (String) e.nextElement();
            String propValue = props.getProperty(propName);
            if (isExecutorId(propName)) {
                if (loadExecutors) {
                    loadExecutor(propName, propValue);
                }
            }
            else if ("default".equals(propName)) {
                mapper.setDefaultCmdExecId(propValue);
            }
            else if(propName.startsWith("prop.")) {
                //add to system properties
                String sysProp=propName.substring("prop".length());
                System.setProperty(sysProp, propValue);
            }
            else {
                mapper.appendTask(propName, propValue);
            }
        }
    }
    
    private boolean isExecutorId(String propName) {
        return propName.startsWith("executor.");
    }
    
    private void loadExecutor(String executorId, String classname) {
        CommandExecutor svc = loadCommandExecutor(classname);
        if (svc != null) {
            cmdExecutorList.add(svc);
            mapper.addCommandExecutor(executorId, svc);
        }
        else {
            log.error("Failed to initialize command executor, executor.id="+executorId+", classname="+classname);
        }
    }
    
    private CommandExecutor loadCommandExecutor(String svcClassname) {
        CommandExecutor svc = null;
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Class svcClass = Class.forName(svcClassname, false, classLoader);
            if (!CommandExecutor.class.isAssignableFrom(svcClass)) {
                log.error(""+svcClass.getCanonicalName()+" does not implement "+CommandExecutor.class.getCanonicalName());
                return svc;
            }
            svc = (CommandExecutor) svcClass.newInstance();
            return svc;
        }
        catch (Throwable t) {
            log.error("Error loading CommandExecutor for classname: "+svcClassname+", "+t.getLocalizedMessage(), t);
        }
        return svc;
    }
    
    /**
     * call this at system startup to initialize the list of CommandExecutorService instances.
     */
    public void start() {
        init();
        
        for(CommandExecutor svc : cmdExecutorList) {
            try {
                svc.start();
            }
            catch (Throwable t) {
                log.error("Error starting CommandExecutorService, for class: "+svc.getClass().getCanonicalName()+": "+t.getLocalizedMessage(), t);
            }
        }
    }
    
    /**
     * call this at system shutdown to stop the list of running CommandExecutorService instances.
     */
    public void stop() {
        for(CommandExecutor svc : cmdExecutorList) {
            try {
                svc.stop();
            }
            catch (Throwable t) {
                log.error("Error stopping CommandExecutorService, for class: "+svc.getClass().getCanonicalName()+": "+t.getLocalizedMessage(), t);
            }
        }
    }

    public CommandExecutorMapper getCommandExecutorMapper() {
        return mapper;
    }
    
    public List<CommandExecutor> getCommandExecutors() {
        return Collections.unmodifiableList(cmdExecutorList);
    }
    
    public void reloadMapperConfiguration() throws Exception {
        synchronized(this) {
            Properties executorProperties = loadExecutorProperties();
            reloadMapperFromProperties(executorProperties);
        }
    }

}
