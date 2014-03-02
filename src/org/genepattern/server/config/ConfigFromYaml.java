package org.genepattern.server.config;

/**
 * the output from parsing the config yaml file.
 * @author pcarr
 *
 */
public class ConfigFromYaml {
    private final JobConfigObj jobConfigObj;
    private final CommandManagerProperties cmdMgrProps;
    
    
    public ConfigFromYaml(final JobConfigObj jobConfigObj, final CommandManagerProperties cmdMgrProps) {
        this.jobConfigObj=jobConfigObj;
        this.cmdMgrProps=cmdMgrProps;
    }
    
    public JobConfigObj getJobConfigObj() {
        return jobConfigObj;
    }
    
    public CommandManagerProperties getCmdMgrProps() {
        return cmdMgrProps;
    }
}
