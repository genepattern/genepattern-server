package org.genepattern.server.webapp.rest.api.v1.job;

import java.io.File;

import org.apache.log4j.Logger;
import org.genepattern.drm.Memory;

/**
 * for debugging and profiling the JobObjectCache and related classes
 * 
 * To enable set log level to TRACE in the log4j.properties file and restart the server
 <pre>
       # for profiling the JobObjectCache
       log4j.logger.org.genepattern.server.webapp.rest.api.v1.job.JobObjectCacheDebug=TRACE, stdout
 </pre>
 */
public class JobObjectCacheDebug {
    public static Logger log = Logger.getLogger(JobObjectCacheDebug.class);

    /**
     * for debugging and profiling only, don't use this in production.
     * 
     * quick and dirty way to force the initialization of the JobObjectCache
     * to make a large number of File I/O metadata calls.
     * 
     * Note: I added this when profiling the JobObjectCache.
     *   The loop only happens when log.trace is enabled.
     */ 
    public static void throttleForProfiling() {
        throttleForProfiling(log);
    }
    
    public static void throttleForProfiling(final Logger log) {
        // only if trace is enabled for the particular logger
        if (log.isTraceEnabled()) {
            throttleFileSystem(log);
        }
    }
    
    private static void throttleFileSystem(final Logger log) {
        final File dir=new File(System.getProperty("user.home"));
        final int numIterations=100;
        final boolean logOutput=true;
        throttleFileSystem(log, dir, numIterations, logOutput);
    }
    
    //for profiling
    private static void throttleFileSystem(final Logger log, final File dir, final int numIterations, final boolean logOutput) {
        if (log.isTraceEnabled()) {
            for(int i=0; i<numIterations; ++i) {
                throttleDir(log, dir, logOutput);
            }
        }
    }

    //for profiling
    private static void throttleDir(final Logger log, final File dir, final boolean logOutput) {
        final String[] filenames=dir.list();
        for(final String name : filenames) {
            File file=new File(dir, name);
            long length=file.length(); 
            if (logOutput) {
                log.trace(""+name+": "+Memory.formatNumBytes(length));
            }
        }
    }

}
