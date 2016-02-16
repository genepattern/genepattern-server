/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.drm.impl.lsf.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobStatus;

import com.google.common.base.Strings;


/**
 * Helper class for getting the status of an LSF job via a command line interface.
 * 
 * Flowchart:
 * Given, list of DrmJobRecord, link each item in that list to an optional lsfLogFile 
 * 
 * @author pcarr
 *
 */
public class LsfStatusChecker {
    private static final Logger log = Logger.getLogger(LsfStatusChecker.class);
    
    public static final List<String> DEFAULT_LSF_STATUS_CMD_PREFIX=Arrays.asList("bjobs", "-W");
    /** a time unit in milliseconds */
    public static final Long DEFAULT_LSF_STATUS_CMD_TIMEOUT=60L*1000L;
    
    private final List<String> lsfStatusCmdPrefix;
    private final Pattern lsfStatusPattern;
    private final Long lsfStatusCmdTimeout;

    public LsfStatusChecker() {
        this(null, null, null);
    }

    public LsfStatusChecker(final List<String> lsfStatusCmdPrefix, final Pattern lsfStatusPattern, final Long lsfStatusCmdTimeout) {
        // null means, use default
        if (lsfStatusCmdPrefix==null) {
            this.lsfStatusCmdPrefix=LsfStatusChecker.DEFAULT_LSF_STATUS_CMD_PREFIX;
        }
        else {
            this.lsfStatusCmdPrefix=lsfStatusCmdPrefix;
        }
        // null means, use default
        if (lsfStatusPattern==null) {
            this.lsfStatusPattern=LsfBjobsParser.LINE_PATTERN_DEFAULT;
        }
        else {
            this.lsfStatusPattern=lsfStatusPattern;
        }
        // 
        if (lsfStatusCmdTimeout==null) {
            this.lsfStatusCmdTimeout=DEFAULT_LSF_STATUS_CMD_TIMEOUT;
        }
        else {
            this.lsfStatusCmdTimeout=lsfStatusCmdTimeout;
        }
    }

    // for debugging
    protected Pattern getLsfStatusPattern() {
        return lsfStatusPattern;
    }
    
    protected Long getLsfStatusCmdTimeout() {
        return lsfStatusCmdTimeout;
    }

    protected static final File initLogFile(DrmJobRecord drmJobRecord) {
        return getRelativeFile(drmJobRecord.getWorkingDir(), drmJobRecord.getLogFile());
    }

    protected static final File getRelativeFile(final File workingDir, final File file) {
        if (file == null) {
            return null;
        }
        else if (file.isAbsolute()) {
            return file;
        }
        if (workingDir != null) {
            return new File(workingDir, file.getPath());
        }
        return file;
    }
    
    /**
     * Initialize the LSF status command line for the given job. This is the command
     * that the GP server runs to check for the status of a job.
     * By default use the 'bjobs -W {extJobId}' command.
     * This can optionally be configured with the 'lsf.statusCmd' property in the 'command.properties' 
     * section of the config_yaml file. E.g.
     * <pre>
       executors:
           LSF:
               command.properties:
                   lsf.statusCmd: [ "bjobs", "-W" ]
     * </pre>
     * 
     * Note: the extJobId is always appended at the end of the command line.
     * 
     * @param jobRecord
     * @return
     */
    protected List<String> initStatusCmd(final DrmJobRecord jobRecord) throws CmdException {
        final List<String> cmd=new ArrayList<String>();
        cmd.addAll(lsfStatusCmdPrefix);
        if (jobRecord==null) {
            throw new CmdException("jobRecord==null");
        }
        if (Strings.isNullOrEmpty(jobRecord.getExtJobId())) {
            throw new CmdException("jobRecord.extJobId not set, extJobId='"+jobRecord.getExtJobId()+"'");
        }
        cmd.add(""+jobRecord.getExtJobId());
        return cmd;
    }

    /**
     * Runs the 'bjobs -W' command for the given jobRecord.
     */
    public DrmJobStatus checkStatus(DrmJobRecord jobRecord) throws CmdException, InterruptedException {
        File lsfLogFile=initLogFile(jobRecord);
        return checkStatus(jobRecord, lsfLogFile);
    }

    /**
     * Runs the 'bjobs -W' command for the given jobRecord and lsfLogFile path.
     * 
     * @param jobRecord, the GenePattern job
     * @param lsfLogFile, the path to the '.lsf.out' log file
     * 
     * @return
     * @throws CmdException
     * @throws InterruptedException
     */
    public DrmJobStatus checkStatus(final DrmJobRecord jobRecord, final File lsfLogFile) throws CmdException, InterruptedException {
        if (log.isDebugEnabled()) {
            log.debug("checkStatus for jobId="+jobRecord.getExtJobId());
        }
        final List<String> cmd=initStatusCmd(jobRecord);
        if (log.isDebugEnabled()) {
            log.debug("cmd="+cmd);
        }
        final CommonsExecCmdRunner cmdRunner=new CommonsExecCmdRunner(lsfStatusCmdTimeout);
        final List<String> out=cmdRunner.runCmd(cmd);
        if (log.isDebugEnabled()) {
            log.debug("output="+out);
        }
        return parseStatusCmdOutput(cmd, out, lsfLogFile);
    }
    
    /**
     * Extract the DrmJobStatus record by parsing the output from the bjobs command.
     *  
     * @param cmd, for debugging, the lsf status command, e.g. 'bjobs' '-w' '<ext_job_id>'
     * @param out, the output from the command, as a list of lines
     * @param lsfLogFile
     * @return
     * @throws CmdException
     * @throws InterruptedException
     */
    protected DrmJobStatus parseStatusCmdOutput(final List<String> cmd, final List<String> out, final File lsfLogFile) throws CmdException, InterruptedException {
        if (out==null) {
            throw new CmdException("Null output from cmd="+cmd);
        }
        int numLines=out.size();
        if (numLines < 2) {
            throw new CmdException("Expecting two lines from 'bjobs -W' command, num lines="+out.size()+", output="+out);
        }
        if (numLines > 2) {
            log.warn("Expecting two lines from 'bjobs -W' command, num lines="+out.size()+", output="+out);
        }
        // skip the header line ( out[0] )
        final String line=out.get(1);
        if (log.isDebugEnabled()) {
            log.debug("line="+line);
            log.debug("lsfLogFile="+lsfLogFile);
        }
        final DrmJobStatus jobStatus=LsfBjobsParser.parseAsJobStatus(lsfStatusPattern, line, lsfLogFile);
        if (log.isDebugEnabled()) {
            log.debug("jobStatus="+jobStatus);
        }
        return jobStatus;
    }

}
