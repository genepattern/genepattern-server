package org.genepattern.drm.impl.lsf.core;

import org.genepattern.drm.DrmJobState;

/**
 * Lookup table from an LSF status code returned by the bjobs -W command and the GenePattern DrmJobState class.
 * 
 * See: http://iwww.broadinstitute.org/itsystems/softwaredocs/lsf7.0.6/api_ref/index.html
 * 
 * @author pcarr
 *
 */
public enum LsfState {
    NULL (DrmJobState.UNDETERMINED, "0x00",    "State null"),
    PEND (DrmJobState.QUEUED,       "0x01",    "The job is pending, i.e., it has not been dispatched yet."),
    PSUSP(DrmJobState.SUSPENDED,    "0x02",    "The pending job was suspended by its owner or the LSF system administrator."),
    RUN  (DrmJobState.RUNNING,      "0x04",    "The job is running."),
    SSUSP(DrmJobState.SUSPENDED ,   "0x08",    "The running job was suspended by the system because an execution host was overloaded or the queue run window closed."),
    USUSP(DrmJobState.SUSPENDED,    "0x10",    "The running job was suspended by its owner or the LSF system administrator."),
    EXIT (DrmJobState.FAILED,       "0x20",    "The job has terminated with a non-zero status - it may have been aborted due to an error in its execution, or killed by its owner or by the LSF system administrator."),
    DONE (DrmJobState.DONE,         "0x40",    "The job has terminated with status 0."),
    PDONE(DrmJobState.DONE,         "(0x80)",  "Post job process done successfully."),
    PERR (DrmJobState.FAILED,       "(0x100)", "Post job process has error."),
    WAIT (DrmJobState.QUEUED,       "(0x200)", "Chunk job waiting its turn to exec."),
    UNKWN(DrmJobState.UNDETERMINED, "0x10000", "The slave batch daemon (sbatchd) on the host on which the job is processed has lost contact with the master batch daemon (mbatchd)."),
    ZOMBI(DrmJobState.UNDETERMINED, "", "A job becomes zombi for a number of reasons, see the bjobs man page for details.")
    ;

    LsfState(DrmJobState drmJobState, String code, String description) {
        this.drmJobState=drmJobState;
        this.code=code;
        this.description=description;
    }
    private final DrmJobState drmJobState;
    private final String code;
    private final String description;

    public DrmJobState getDrmJobState() {
        return drmJobState;
    }
    public String getCode() {
        return code;
    }
    public String getDescription() {
        return description;
    }
}
