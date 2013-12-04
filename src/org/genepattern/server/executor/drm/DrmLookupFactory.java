package org.genepattern.server.executor.drm;

public class DrmLookupFactory {
    public static final DrmLookup initializeDrmLookup(final String jobRunnerClassname, final String jobRunnerId) {
        return new HashMapLookup(jobRunnerClassname, jobRunnerId);
        //return new DbLookup(jobRunnerClassname, jobRunnerId);
    }
    
    
}
