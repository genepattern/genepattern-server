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
    
    public long asMillis() {
        return TimeUnit.MILLISECONDS.convert(time, timeUnit);
    }
    
    public String format() {
        return PeriodFormat.getDefault().print(new Duration(asMillis()).toPeriod());
    }
}