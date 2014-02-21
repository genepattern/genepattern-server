package org.genepattern.server.rest;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfigurationFactory;

/**
 * Factory method for initializing an instance of the JobInputApi.
 * 
 * @author pcarr
 */
public class JobInputApiFactory {
    private static final Logger log = Logger.getLogger(JobInputApiFactory.class);
    static public JobInputApi createJobInputApi(ServerConfiguration.Context context) {
        final boolean initDefault=false;
        return createJobInputApi(context, initDefault);
    }
    
    static public JobInputApi createJobInputApi(ServerConfiguration.Context context, boolean initDefault) {
        final String jobInputApiClass=ServerConfigurationFactory.instance().getGPProperty(context, "jobInputApiClass");
        if ("org.genepattern.server.rest.JobInputApiImplV2".equals(jobInputApiClass)) {
            return new JobInputApiImplV2(initDefault);
        }
        if (jobInputApiClass != null) {
            log.error("Ignoring config property jobInputApiClass="+jobInputApiClass);
        }
        return new JobInputApiImpl(initDefault);
    }
}
