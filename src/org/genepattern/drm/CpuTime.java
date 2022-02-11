/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.drm;

import java.util.concurrent.TimeUnit;

import org.joda.time.Duration;
import org.joda.time.format.PeriodFormat;

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
        this(0L, TimeUnit.MILLISECONDS);
    }
    public CpuTime(long numMillis) {
        this(numMillis, TimeUnit.MILLISECONDS);
    }
    public CpuTime(long time, TimeUnit timeUnit) throws IllegalArgumentException {
        if (time<0) {
            throw new IllegalArgumentException("time must be > 0");
        }
        this.time=time;
        this.timeUnit=timeUnit;
    }
    
    public long getTime() {
        return time;
    }
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }
    
    public long asSeconds() {
        return TimeUnit.SECONDS.convert(time, timeUnit);
    }
    
    public long asMillis() {
        return TimeUnit.MILLISECONDS.convert(time, timeUnit);
    }
    
    /**
     * Get (default) human readable display value for this instance.
     */
    public String getDisplayValue() {
        return PeriodFormat.getDefault().print(new Duration(asMillis()).toPeriod());
    }
    
    public String toString() {
        return ""+time+" "+timeUnit.toString();
    }
}
