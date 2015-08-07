/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.output;

/**
 * Enumeration of valid values for the gpfiletype column of the job_output table.
 * 
 * Note: The 'GP_' prefix is reserved for all hidden file types, to make it easier to query the database.
 * @author pcarr
 *
 */
public enum GpFileType {
    /** A job result file. */
    FILE,
    /** The stdout output from the job. */
    STDOUT,
    /** The stderr output from the job. */
    STDERR,
    /** A job result directory. */
    DIR(Directory.IS),
    /** The working directory for the job. */
    GP_JOB_DIR(Hidden.IS, Directory.IS),
    /** A generic log file for the job, should be hidden from the list of output files. */
    GP_LOG_FILE(Log.IS),
      /** The 'gp_execution_log.txt' file. */
      GP_EXECUTION_LOG(Log.IS),
      /** The '<pipelineName>_execution_log.html' file. */
      GP_PIPELINE_LOG(Log.IS),
    /** A log directory for the job, should be hidden from the list of output files. */
    GP_LOG_DIR(Log.IS, Directory.IS),
    GP_HIDDEN_FILE(Hidden.IS),
    GP_HIDDEN_DIR(Hidden.IS, Directory.IS)
    ;
    
    private enum Hidden {
        IS
    }
    private enum Directory {
        IS
    }
    private enum Log {
        IS
    }
    
    private boolean isFile=true;
    private boolean isLog=false;
    private boolean isHidden=false;
    
    private GpFileType() {
    }
    private GpFileType(Directory d) {
        isFile=false;
    }
    private GpFileType(Hidden h, Directory d) {
        isHidden=true;
        isFile=false;
    }
    private GpFileType(Log l) {
        isHidden=true;
        isLog=true;
    }
    private GpFileType(Log l, Directory d) {
        isHidden=true;
        isLog=true;
        isFile=false;
    }
    private GpFileType(Hidden h) {
        isHidden=true;
    }
    
    /** 
     * Is this file hidden from the job results listing.
     * 
     * Note: this is different than the semantics for 'java.io.File#isHidden'.
     */
    public boolean isHidden() {
        return isHidden;
    }
    
    public boolean isDirectory() {
        return !isFile;
    }
    
    public boolean isFile() {
        return isFile;
    }

    /**
     * A log file is hidden, but it should be included in the list of 0 or more execution logs for the job.
     * @return
     */
    public boolean isLog() {
        return isLog;
    }

}
