/* Auto generated file */

package org.genepattern.server.domain;

import java.util.*;

public class JobStatus {

    private Integer statusId;
    private String statusName;

    public JobStatus() {
    }

    public JobStatus(Integer statusId) {
        super();
        this.statusId = statusId;
    }

    public JobStatus(Integer statusId, String statusName) {
        super();
        this.statusId = statusId;
        this.statusName = statusName;
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

    /** Static members */
    public static int JOB_NOT_STARTED = 1;

    public static int JOB_PROCESSING = 2;

    public static int JOB_FINISHED = 3;

    public static int JOB_ERROR = 4;

    public static String NOT_STARTED = "Pending";

    public static String PROCESSING = "Processing";

    public static String FINISHED = "Finished";

    public static String ERROR = "Error";

    /**
     * an unmodifiable map that maps a string representation of the status to
     * the numberic representation
     */
    public static final Map STATUS_MAP;

    static {
        Map statusHash = new HashMap();
        statusHash.put(NOT_STARTED, new Integer(JOB_NOT_STARTED));
        statusHash.put(PROCESSING, new Integer(JOB_PROCESSING));
        statusHash.put(FINISHED, new Integer(JOB_FINISHED));
        statusHash.put(ERROR, new Integer(JOB_ERROR));
        STATUS_MAP = Collections.unmodifiableMap(statusHash);
    }

}
