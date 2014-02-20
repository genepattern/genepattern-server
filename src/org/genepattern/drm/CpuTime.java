package org.genepattern.drm;

import java.util.concurrent.TimeUnit;

/**
 * Generic representation of the cpu time limit for 
 * a job submitted to the queue.
 * 
 * @author pcarr
 *
 */
public class CpuTime {
    private final long time;
    private final TimeUnit timeUnit;
    
    public CpuTime() {
        this(0, TimeUnit.MILLISECONDS);
    }
    public CpuTime(long time, TimeUnit timeUnit) {
        this.time=time;
        this.timeUnit=timeUnit;
    }
    
    public long getTime() {
        return time;
    }
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }
}