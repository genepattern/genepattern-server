package org.genepattern.server.job.input;

/**
 * Data structure for indicating the number of input values that a
 * module input parameter can accept.
 * 
 * @author pcarr
 *
 */
public class NumValues {
    Integer min=null;
    Integer max=null;

    public NumValues(final Integer min, final Integer max) {
        this.min=min;
        this.max=max;
    }
    
    public Integer getMin() {
        return min;
    }
    
    public Integer getMax() {
        return max;
    }
    
}

