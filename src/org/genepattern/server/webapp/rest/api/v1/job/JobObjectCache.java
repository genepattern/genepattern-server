package org.genepattern.server.webapp.rest.api.v1.job;


import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;

/**
 * A cache of job objects (in json format) to be returned by the REST API.
 * 
 * Configuration options in the config_custom.yaml file:
 * <pre>
   # set a default value
   jobCache.maximumSize: "10000"
   
   # to disable the cache, set the max size to 0
   jobCache.maximumSize: "0"
 * </pre>
 * 
 * @author pcarr
 */
public class JobObjectCache {
    public static final String PROP_ENABLED="jobObjectCache.enabled";
    public static final String PROP_MAX_SIZE="jobObjectCache.maximumSize";
    public static final String PROP_EXPIRE_AFTER_WRITE_DAYS="jobObjectCache.expireAfterWriteDays";
    
    public static boolean isEnabled(final GpConfig gpConfig, final GpContext serverContext) {
        boolean enabled = gpConfig.getGPBooleanProperty(serverContext, PROP_ENABLED, false);
        return enabled;
    }
    
    public static long getMaximumSize(final GpConfig gpConfig, final GpContext serverContext) {
        // default is a cache of size of 10,000 records
        return gpConfig.getGPLongProperty(serverContext, PROP_MAX_SIZE, 10000L);
    }
}
