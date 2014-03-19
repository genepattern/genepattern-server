package org.genepattern.server.rest;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;

/**
 * Factory method for initializing an instance of the JobInputApi.
 * 
 * @author pcarr
 */
public class JobInputApiFactory {
    private static final Logger log = Logger.getLogger(JobInputApiFactory.class);
    static public JobInputApi createJobInputApi(GpContext context) {
        final boolean initDefault=false;
        return createJobInputApi(context, initDefault);
    }
    
    static public JobInputApi createJobInputApi(GpContext context, boolean initDefault) {
        final String jobInputApiClass=ServerConfigurationFactory.instance().getGPProperty(context, "jobInputApiClass", JobInputApiImplV2.class.getName());
        if (JobInputApiImplV2.class.getName().equals(jobInputApiClass)) {
            return new JobInputApiImplV2(initDefault);
        }
        else if (JobInputApiImpl.class.getName().equals(jobInputApiClass)) {
            return new JobInputApiImpl(initDefault);
        }
        if (jobInputApiClass != null) {
            log.error("Ignoring config property jobInputApiClass="+jobInputApiClass);
        }
        return new JobInputApiImpl(initDefault);
    }
}
