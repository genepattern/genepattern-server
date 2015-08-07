/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JobStatus { 
    //public static int JOB_WAITING = 0;
    public static int JOB_PENDING = 1;
    //public static int JOB_DISPATCHING = 5;
    public static int JOB_PROCESSING = 2;
    public static int JOB_FINISHED = 3;
    public static int JOB_ERROR = 4;

    //public static String WAITING = "Waiting";
    public static String PENDING = "Pending";
    //public static String DISPATCHING = "Dispatching";
    public static String PROCESSING = "Processing";
    public static String FINISHED = "Finished";
    public static String ERROR = "Error";

    /**
     * an unmodifiable map that maps a string representation of the status to
     * the numberic representation
     */
    public static final Map<String, Integer> STATUS_MAP;

    static {
        Map<String, Integer> statusHash = new HashMap<String, Integer>();
        //statusHash.put(WAITING, new Integer(JOB_WAITING));
        statusHash.put(PENDING, new Integer(JOB_PENDING));
        //statusHash.put(DISPATCHING, new Integer(JOB_DISPATCHING));
        statusHash.put(PROCESSING, new Integer(JOB_PROCESSING));
        statusHash.put(FINISHED, new Integer(JOB_FINISHED));
        statusHash.put(ERROR, new Integer(JOB_ERROR));
        STATUS_MAP = Collections.unmodifiableMap(statusHash);
    }
    
    
    private Integer statusId;
    private String statusName;

    public JobStatus() {
    }

    public Integer getStatusId() {
        return this.statusId;
    }

    public void setStatusId(Integer value) {
        this.statusId = value;
    }

    public String getStatusName() {
        return this.statusName;
    }

    public void setStatusName(String value) {
        this.statusName = value;
    }

}
