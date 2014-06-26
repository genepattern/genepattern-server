package org.genepattern.server.executor;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ConfigurationException;
import org.genepattern.server.config.ExecutorConfig;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.JobConfigObj;

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
    
    public BasicCommandManager createCommandManager(final GpConfig gpConfig, final JobConfigObj jobConfigObj) throws ConfigurationException {
        this.cmdMgr = new BasicCommandManager(gpConfig); 
        initializeCommandExecutors(cmdMgr, jobConfigObj.getExecutors());
        return cmdMgr;
    }
    
    //initialize executors list
    private void initializeCommandExecutors(final BasicCommandManager cmdMgr, final Map<String,ExecutorConfig> executors)  throws ConfigurationException {
        log.info("initializing command executors ...");
        for(final Entry<String,ExecutorConfig> entry : executors.entrySet()) {
            try {
                initializeCommandExecutor(cmdMgr, entry.getKey(), entry.getValue());
            }
            catch (Throwable t) {
                log.error("error initializing command executor, execId='"+entry.getKey()+"'", t);
            }
        }
    }

    private void initializeCommandExecutor(final BasicCommandManager cmdMgr, final String execId, final ExecutorConfig execObj) throws ConfigurationException {
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

