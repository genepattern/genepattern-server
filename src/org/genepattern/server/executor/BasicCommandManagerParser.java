package org.genepattern.server.executor;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ConfigFileParser;
import org.genepattern.server.config.JobConfigObj;

/**
 * Create a new instance of a CommandManager based on the ServerConfiguration.
 * 
 * @author pcarr
 */
public class BasicCommandManagerParser implements CommandManagerParser {
    private static Logger log = Logger.getLogger(BasicCommandManagerParser.class);
    
    private BasicCommandManager cmdMgr = null;
    
    public BasicCommandManagerParser() {
    }
    
    public CommandManager parseConfigFile(String pathToConfiguration) throws ConfigurationException {
        this.cmdMgr = new BasicCommandManager();
        
        ConfigFileParser parser = new ConfigFileParser();
        parser.parseConfigFile(pathToConfiguration);
        JobConfigObj jobConfigObj = parser.getJobConfig();
        cmdMgr.setConfigProperties( parser.getConfig() );
        
        //TODO: use ServerConfiguration rather than parsing the file again
        //JobConfigObj jobConfigObj = ServerConfiguration.instance().getJobConfiguration();
        initializeCommandExecutors(cmdMgr, jobConfigObj);
        return cmdMgr;
    }
    
    /**
     * @deprecated, reload from the ServerConfiguration instead.
     * Delete this method after the {@link CommandManager#getCommandProperties(org.genepattern.webservice.JobInfo)} is modified.
     */
    public void reloadConfigFile(CommandManager commandManager, String pathToConfiguration) throws ConfigurationException {
        if (!(commandManager instanceof BasicCommandManager)) {
            throw new ConfigurationException("Expecting an instanceof "+this.getClass().getCanonicalName());
        }
        cmdMgr = (BasicCommandManager) commandManager;
        cmdMgr.getConfigProperties().clear();
        
        ConfigFileParser parser = new ConfigFileParser();
        parser.parseConfigFile(pathToConfiguration);
        cmdMgr.setConfigProperties( parser.getConfig() );
    }
    
    //initialize executors list
    private void initializeCommandExecutors(BasicCommandManager cmdMgr, org.genepattern.server.config.JobConfigObj jobConfigObj) throws ConfigurationException {
        for(String execId : jobConfigObj.getExecutors().keySet()) {
            org.genepattern.server.config.ExecutorConfig execObj = jobConfigObj.getExecutors().get(execId);
            CommandExecutor cmdExecutor = initializeCommandExecutor(execObj);
            cmdMgr.addCommandExecutor(execId, cmdExecutor);
        }
    }

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

