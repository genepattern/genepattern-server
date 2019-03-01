package org.genepattern.server.webapp.rest.api.v1.job;


import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.json.JSONObject;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

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
    
    protected static Cache<String, JSONObject> jobCache;
    
    protected static final Cache<String, JSONObject> initJobCache(final GpConfig gpConfig, final GpContext serverContext) {
        final long maxSize=JobObjectCache.getMaximumSize(gpConfig, serverContext);
        final long days=JobObjectCache.getExpireAfterWriteDays(gpConfig, serverContext);
        return CacheBuilder.newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(days, TimeUnit.DAYS)
            .build();
    }
    
    protected static boolean isCacheEnabled(final GpConfig gpConfig, final GpContext gpContext) {
        final boolean isEnabled=JobObjectCache.isEnabled(ServerConfigurationFactory.instance(), gpContext);
        
        if (isEnabled && jobCache == null) {
            jobCache = initJobCache(gpConfig, gpContext); 
        }
        
        // special-case: cleanup when the cache is switched from enabled to disabled while the server is running
        if (!isEnabled && jobCache != null) {
            jobCache.invalidateAll();
            jobCache = null;
        }
        return isEnabled;
    }
    
    protected static final String initCompositeKey(final Integer jobId,final boolean includeChildren, final boolean includeOutputFiles, final boolean includeComments, final boolean includeTags) {
        // ***** the paramMap is a hack to get all this stuff up via the composite key even though it must be final
        final String composite_key = ""+jobId + includeChildren + includeOutputFiles + includeComments + includeTags;
        return composite_key;
    }
    
    public static JSONObject getJobJson(final String key, final Callable<JSONObject> f) throws ExecutionException {
        return jobCache.get(key, f);
    }
    
    public static final boolean[] flags=new boolean[] { false, true };
    public static void removeJobFromCache(final Integer jobId) {
        if (jobCache==null) {
            return;
        }
        for(boolean includeChildren : flags) {
            for(boolean includeOutputFiles : flags) {
                for(boolean includeComments : flags) {
                    for(boolean includeTags : flags) {
                        final String key=initCompositeKey(jobId, includeChildren, includeOutputFiles, includeComments, includeTags);
                        jobCache.invalidate(key);
                    }
                }
            }
        }
    }

}
