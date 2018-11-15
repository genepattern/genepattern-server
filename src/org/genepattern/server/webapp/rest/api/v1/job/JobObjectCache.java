package org.genepattern.server.webapp.rest.api.v1.job;


import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;

/**
 * A cache of job objects (in json format) to be returned by the REST API.
 * 
 * Configuration options in the config_custom.yaml file:
 * <pre>
   # the max size of the cache, corresponds to number of objects
   jobObjectCache.maximumSize: "10000"
   # the number of days to keep the records in the cache
   jobObjectCache.expireAfterWriteDays: 10
   # to disable the cache
   jobObjectCache.enabled: false
 * </pre>
 * 
 * @author pcarr
 */
public class JobObjectCache {
    public static final String PROP_ENABLED="jobObjectCache.enabled";
    public static final String PROP_MAX_SIZE="jobObjectCache.maximumSize";
    public static final String PROP_EXPIRE_AFTER_WRITE_DAYS="jobObjectCache.expireAfterWriteDays";
    
    public static boolean isEnabled(final GpConfig gpConfig, final GpContext serverContext) {
        // default is false
        boolean enabled = gpConfig.getGPBooleanProperty(serverContext, PROP_ENABLED, false);
        return enabled;
    }
    
    public static long getMaximumSize(final GpConfig gpConfig, final GpContext serverContext) {
        // default is 10,000 records
        return gpConfig.getGPLongProperty(serverContext, PROP_MAX_SIZE, 10000L);
    }
    
    public static long getExpireAfterWriteDays(final GpConfig gpConfig, final GpContext serverContext) {
        // default is 10 days
        return gpConfig.getGPLongProperty(serverContext, PROP_EXPIRE_AFTER_WRITE_DAYS, 10L);        
    }
}
