/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.config;

/**
 * the output from parsing the config yaml file.
 * @author pcarr
 *
 */
public class ConfigFromYaml {
    private final JobConfigObj jobConfigObj;
    private final ConfigYamlProperties configYamlProps;
    
    public ConfigFromYaml(final JobConfigObj jobConfigObj, final ConfigYamlProperties cmdMgrProps) {
        this.jobConfigObj=jobConfigObj;
        this.configYamlProps=cmdMgrProps;
    }
    
    public JobConfigObj getJobConfigObj() {
        return jobConfigObj;
    }
    
    public ConfigYamlProperties getConfigYamlProperties() {
        return configYamlProps;
    }
}
