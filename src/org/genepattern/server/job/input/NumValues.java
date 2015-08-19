/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input;

/**
 * Data structure for indicating the number of input values that a
 * module input parameter can accept.
 * 
 * @author pcarr
 *
 */
public class NumValues {
    public static final String PROP_NUM_VALUES="numValues";
    public static final String PROP_LIST_MODE="listMode";
    public static final String PROP_LIST_MODE_SEP="listModeSep";


    private Integer min=null;
    private Integer max=null;

    /**
     * @param min, must be a non-null integer, greater than or equal to zero.
     * @param max, can be null, which means 'unlimited', or an integer greater than zero.
     * 
     * @throws IllegalArgumentException
     */
    public NumValues(final Integer min, final Integer max) {
        if (min == null) {
            throw new IllegalArgumentException("min==null");
        }
        if (min<0) {
            throw new IllegalArgumentException("min<0, min=="+min);
        }
        if (max != null && max==0) {
            throw new IllegalArgumentException("max==0");
        }
        if (max != null && max<0) {
            throw new IllegalArgumentException("max<0, max=="+max);
        }
        
        if (max != null && min>max) {
            throw new IllegalArgumentException("max>min, min=="+min+", max=="+max);
        }
        
        this.min=min;
        this.max=max;
    }
    
    public Integer getMin() {
        return min;
    }
    
    public Integer getMax() {
        return max;
    }
    
    public boolean isOptional() {
        if (min==0) {
            return true;
        }
        return false;
    }
    
    public boolean acceptsList() {
        if (min>1) {
            return true;
        }
        if (max == null || max > 1) {
            return true;
        }
        return false;
    }
    
}

