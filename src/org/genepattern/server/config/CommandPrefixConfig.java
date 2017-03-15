package org.genepattern.server.config;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.genepattern.util.LSID;
import org.genepattern.webservice.WebServiceException;

/**
 * Helper class for setting the command line prefix for a job based on the settings in the
 *     'commandPrefix.properties' and 'taskPrefixMapping.properties' files.
 * 
 * @author pcarr
 */
public class CommandPrefixConfig {

    public static String getCommandPrefix(final GpConfig gpConfig, final GpContext jobContext) 
    throws MalformedURLException
    {
        final boolean isCommandLineJob = TaskType.JOB == jobContext.getTaskType();
        final String lsidStr = jobContext.getLsid();
        return getCommandPrefix(gpConfig, isCommandLineJob, lsidStr);
    }

    /**
     * handle 'getCommandPrefix' for job in GenePatternAnalysisTask
     * 
     * Get the appropriate command prefix to use for this module. The hierarchy goes like this; 
     * 1. task version specific entry in task prefix mapping 
     * 2. task versionless entry in task prefix mapping 
     * 3. default command prefix only applies to non-visualizers
     * 
     * @param taskInfoAttributes
     * @return null if no command prefix is specified.
     * @throws MalformedURLException
     */
    protected static String getCommandPrefix(final GpConfig gpConfig, final boolean isCommandLineJob, final String lsidStr) 
            throws MalformedURLException
    {
        final LSID lsid=new LSID(lsidStr);
        final Map<String,String> taskPrefixMapping = gpConfig.getTaskPrefixMappingProps().getProps();
        final String commandPrefixName = getCommandPrefixName(taskPrefixMapping, lsid);

        final Map<String,String> commandPrefixProps=gpConfig.getCommandPrefixProps().getProps();
        final String commandPrefixCustom=commandPrefixProps.getOrDefault(commandPrefixName, null);
        if (commandPrefixCustom == null && isCommandLineJob) {
            // default command prefix only applies to command line jobs
            return gpConfig.getCommandPrefixProps().getProps().getOrDefault("default", null);
        }
        return commandPrefixCustom;
    }

    /**
     * For the given LSID instance, get the optional commandPrefixName from 
     * the taskPrefixMapping.properties file.
     * 
     * The full lsid takes precedence over the versionless lsid.
     *     {lsid}={prefixName}
     *     {lsidNoVersion}={prefixName}
     *     
     * @return a commandPrefixName from the file or null if there are not matching lsids
     */
    protected static String getCommandPrefixName(final Map<String,String> taskPrefixMapping, final LSID lsid) 
    {
        final String commandPrefixName = taskPrefixMapping.getOrDefault( lsid.toString(), null );
        if (commandPrefixName == null) {
            return taskPrefixMapping.getOrDefault( lsid.toStringNoVersion(), null );
        }
        else {
            return commandPrefixName;
        }
    }
    
    final GpConfig gpConfig;

    public CommandPrefixConfig() {
        this(ServerConfigurationFactory.instance());
    }
    public CommandPrefixConfig(final GpConfig gpConfig) {
        this.gpConfig=gpConfig != null ? gpConfig : ServerConfigurationFactory.instance();
    }

    private GpConfig gpConfig() {
        return gpConfig;
    }

    public String getDefaultCommandPrefix() {
        return gpConfig().getCommandPrefixProps().getProps().getOrDefault("default", "");
    }

    /**
     * handle 'getCommandPrefix' for job in GenePatternAnalysisTask
     * 
     * Get the appropriate command prefix to use for this module. The hierarchy goes like this; 
     * 1. task version specific entry in task prefix mapping 
     * 2. task versionless entry in task prefix mapping 
     * 3. default command prefix only applies to non-visualizers
     * 
     * @param taskInfoAttributes
     * @return null if no command prefix is specified.
     * @throws MalformedURLException
     */
    public String getCommandPrefix(final GpContext jobContext) 
    throws MalformedURLException
    {
        if (jobContext==null) {
            throw new IllegalArgumentException("jobContext is null");
        } 
        final boolean isCommandLineJob = TaskType.JOB == jobContext.getTaskType();
        final String lsidStr = jobContext.getLsid();
        final LSID lsid=new LSID(lsidStr);
        final Map<String,String> taskPrefixMapping = gpConfig.getTaskPrefixMappingProps().getProps();
        final String commandPrefixName = getCommandPrefixName(taskPrefixMapping, lsid);

        final Map<String,String> commandPrefixProps=gpConfig.getCommandPrefixProps().getProps();
        final String commandPrefixCustom=commandPrefixProps.getOrDefault(commandPrefixName, null);
        if (commandPrefixCustom == null && isCommandLineJob) {
            // default command prefix only applies to command line jobs
            return gpConfig.getCommandPrefixProps().getProps().getOrDefault("default", null);
        }
        return commandPrefixCustom;
    }

    public Properties getCommandPrefixProperties() {
        return gpConfig().getCommandPrefixProps().cloneProperties();
    }

    public Properties getTaskPrefixMappingProperties() {
        return gpConfig().getTaskPrefixMappingProps().cloneProperties();
    }

    /** handle 'save default' link */
    public void saveDefaultCommandPrefix(final String defaultCommandPrefix) {
        addCommandPrefix("default", defaultCommandPrefix);
    }

    /** handle 'add prefix' link */
    public void addCommandPrefix(final String name, final String value) {
        gpConfig().getCommandPrefixProps().saveProperty(name, value);
    }

    /** handle 'delete' prefix link */
    public void deleteCommandPrefix(final String commandPrefixName) {
        gpConfig().getCommandPrefixProps().deleteProperty(commandPrefixName);

        final Properties tpm = new Properties();
        tpm.putAll(gpConfig().getTaskPrefixMappingProps().getProps());
        boolean tpmChanged = false;
        for (final Object oKey : tpm.keySet()) {
            final String key = (String) oKey;
            final String prefixInUse = tpm.getProperty(key);
            if (prefixInUse.equals(commandPrefixName)) {
                tpm.remove(key);
                tpmChanged = true;
            }
        }
        if (tpmChanged) {
            gpConfig().getTaskPrefixMappingProps().saveProperties(tpm);
        }
    }

    /** handle 'add mapping' link */
    public void addTaskPrefixMapping(final List<String> baseLsids, final String commandPrefixName) throws MalformedURLException, WebServiceException {
        final Record taskPrefixMappingProps = gpConfig().getTaskPrefixMappingProps();

        final Properties p = taskPrefixMappingProps.cloneProperties();
        for (final String baseLsid : baseLsids) {
            p.setProperty(baseLsid, commandPrefixName);
        }
        taskPrefixMappingProps.saveProperties(p);
    } 

    /** handle 'delete' mapping link */
    public void deleteTaskPrefixMapping(final String baseLsid) {
        gpConfig().getTaskPrefixMappingProps().deleteProperty(baseLsid);
    }

}