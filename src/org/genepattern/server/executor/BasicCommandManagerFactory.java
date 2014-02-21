package org.genepattern.server.executor;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ExecutorConfig;
import org.genepattern.server.config.JobConfigObj;
import org.genepattern.server.config.ServerConfigurationFactory;

/**
 * Create a new instance of a CommandManager based on the ServerConfiguration.
 * 
 * @author pcarr
 */
public class BasicCommandManagerFactory {
    private static Logger log = Logger.getLogger(BasicCommandManagerFactory.class);
    
    private BasicCommandManager cmdMgr = null;
    public BasicCommandManagerFactory() {
    }
    
    public BasicCommandManager createCommandManager() throws ConfigurationException {
        this.cmdMgr = new BasicCommandManager(); 
        JobConfigObj jobConfigObj = ServerConfigurationFactory.instance().getJobConfiguration();
        initializeCommandExecutors(cmdMgr, jobConfigObj);
        return cmdMgr;
    }
    
    //initialize executors list
    private void initializeCommandExecutors(BasicCommandManager cmdMgr, JobConfigObj jobConfigObj)  throws ConfigurationException {
        log.info("initializing command executors ...");
        for(String execId : jobConfigObj.getExecutors().keySet()) {
            try {
                log.info("initializing command executor, execId='"+execId+"' ...");
                initializeCommandExecutor(cmdMgr, jobConfigObj, execId);
                log.info("... done initializing '"+execId+"'.");
            }
            catch (Throwable t) {
                log.error("error initializing command executor, execId='"+execId+"'", t);
            }
        }
    }
    
    private void initializeCommandExecutor(BasicCommandManager cmdMgr, JobConfigObj jobConfigObj, String execId) throws ConfigurationException {
        ExecutorConfig execObj = jobConfigObj.getExecutors().get(execId);
        CommandExecutor cmdExecutor = initializeCommandExecutor(execObj);
        cmdMgr.addCommandExecutor(execId, cmdExecutor);
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
            cmdExecutor.setConfigurationProperties(execObj.getConfigurationProperties());
        }
        return cmdExecutor;
    }
}

