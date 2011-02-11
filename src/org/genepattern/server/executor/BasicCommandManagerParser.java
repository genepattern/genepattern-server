package org.genepattern.server.executor;

import java.io.File;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ConfigFileParser;
import org.genepattern.server.config.JobConfigObj;

/**
 * Parse job configuration properties from a file in YAML format.
 * 
 * @author pcarr
 */
public class BasicCommandManagerParser implements CommandManagerParser {
    private static Logger log = Logger.getLogger(BasicCommandManagerParser.class);
    
    private String configFilename = null;
    private File configFile = null;
    private BasicCommandManager cmdMgr = null;
    
    public BasicCommandManagerParser() {
    }
    
    public CommandManager parseConfigFile(String pathToConfiguration) throws ConfigurationException {
        BasicCommandManager cmdMgr = new BasicCommandManager();
        reloadConfigFile(cmdMgr, pathToConfiguration);
        return cmdMgr;
    }
    
    public void reloadConfigFile(CommandManager commandManager, String pathToConfiguration) throws ConfigurationException {
        if (!(commandManager instanceof BasicCommandManager)) {
            throw new ConfigurationException("Expecting an instanceof "+this.getClass().getCanonicalName());
        }
        this.cmdMgr = (BasicCommandManager) commandManager;
        setConfigFilename(pathToConfiguration);
        
        
        ConfigFileParser parser = new ConfigFileParser();
        parser.parseConfigFile(pathToConfiguration);
        JobConfigObj jobConfigObj = parser.getJobConfig();
        cmdMgr.setConfigProperties( parser.getConfig() );
        //JobConfigObj jobConfigObj = ServerConfiguration.instance().getJobConfiguration();
        initializeCommandExecutors(cmdMgr, jobConfigObj);
//
//        
//        synchronized(cmdMgr) {
//            ServerConfiguration.instance().reloadConfiguration(pathToConfiguration);
//            cmdMgr.setConfigProperties(parser.getConfig);
//            
//            JobConfigObj jobConfigObj = this.parse(this.configFile);
//            reloadCommandManagerProperties(jobConfigObj);
//        }
    }

//    public CommandManager parseConfigFile(String pathToConfiguration) throws ConfigurationException {
//        setConfigFilename(pathToConfiguration);
//        JobConfigObj jobConfigObj = this.parse(this.configFile);
//        this.commandManager = this.initializeCommandManager(jobConfigObj);
//        return this.commandManager;
//    }
//    
//    public void reloadConfigFile(CommandManager cmdMgr, String pathToConfiguration) throws Exception {
//        if (!(cmdMgr instanceof BasicCommandManager)) {
//            throw new Exception("Expecting an instanceof "+this.getClass().getCanonicalName());
//        }
//        this.commandManager = (BasicCommandManager) cmdMgr;
//        setConfigFilename(pathToConfiguration);
//        synchronized(commandManager) {
//            JobConfigObj jobConfigObj = this.parse(this.configFile);
//            reloadCommandManagerProperties(jobConfigObj);
//        }
//    }
    
    private void setConfigFilename(String s) throws ConfigurationException {
        this.configFilename = s;
        this.configFile = CommandManagerFactory.getConfigurationFile(configFilename);
    }
    
    //initialize executors list
    private void initializeCommandExecutors(BasicCommandManager cmdMgr, org.genepattern.server.config.JobConfigObj jobConfigObj) throws ConfigurationException {
        for(String execId : jobConfigObj.getExecutors().keySet()) {
            org.genepattern.server.config.ExecutorConfig execObj = jobConfigObj.getExecutors().get(execId);
            CommandExecutor cmdExecutor = initializeCommandExecutor(execObj);
            cmdMgr.addCommandExecutor(execId, cmdExecutor);
        }
    }

//    private void reloadCommandManagerProperties(JobConfigObj jobConfigObj) throws Exception {
//        CommandManagerProperties config = this.commandManager.getConfigProperties();
//        config.clear();
//        setCommandManagerProperties(this.commandManager, jobConfigObj);
//    }

//    private void setCommandManagerProperties(BasicCommandManager cmdMgr, JobConfigObj jobConfigObj) throws ConfigurationException {
//        CommandManagerProperties config = cmdMgr.getConfigProperties();
//
//        for(String execId : jobConfigObj.getExecutors().keySet()) {
//            ExecutorConfig execObj = jobConfigObj.getExecutors().get(execId);
//            //load executor->default.properties
//            if (execObj.defaultProperties != null) { 
//                PropObj propObj = config.getPropsForExecutor(execId);
//                for (String key : (Set<String>) (Set) execObj.defaultProperties.keySet()) {
//                    Value value = execObj.defaultProperties.get(key);
//                    propObj.addDefaultProperty(key, value);
//                }
//            }
//        }
//        //store top level default.properties
//        config.getTop().setDefaultProperties(jobConfigObj.getDefaultProperties());
//        //store top level module.properties
//        config.getTop().setModuleProperties(jobConfigObj.getModuleProperties());
//        //store custom group.properties
//        initializeCustomProperties(config, jobConfigObj.getGroupPropertiesObj(), true);
//        //store custom user.properties
//        initializeCustomProperties(config, jobConfigObj.getUserPropertiesObj(), false);
//    }

    /**
     * Initialize an instance of a CommandExecutor from the settings stored in the given CmdExecConfigObj.
     * This method calls the constructor and [optionally] calls setConfigurationFilename and setConfigurationProperties.
     * @param execObj
     * @return
     */
    private static CommandExecutor initializeCommandExecutor(org.genepattern.server.config.ExecutorConfig execObj) {
        CommandExecutor cmdExecutor = null;
        //1) load cmdExecutor from classname
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Class svcClass = Class.forName(execObj.getClassname(), false, classLoader);
            if (!CommandExecutor.class.isAssignableFrom(svcClass)) {
                log.error(""+svcClass.getCanonicalName()+" does not implement "+CommandExecutor.class.getCanonicalName());
            }
            cmdExecutor = (CommandExecutor) svcClass.newInstance();
        }
        catch (Throwable t) {
            log.error("Error loading CommandExecutor for classname: "+execObj.getClassname()+", "+t.getLocalizedMessage(), t);
        }
        if (cmdExecutor == null) {
            return cmdExecutor;
        }
        //2) optionally set configuration parameters
        if (execObj.getConfigurationFile() != null) {
            cmdExecutor.setConfigurationFilename(execObj.getConfigurationFile());
        }
        if (execObj.getConfigurationProperties() != null) {
            log.error("Ignoring configuration properties for execObj: "+execObj.getClassname());
        }
        return cmdExecutor;
    }
}

