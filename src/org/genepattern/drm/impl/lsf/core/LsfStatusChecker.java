package org.genepattern.drm.impl.lsf.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
    
    private final CmdRunner cmdRunner;
    private final List<String> lsfStatusCmdPrefix;
    private final Pattern lsfStatusPattern;

    public LsfStatusChecker() {
        this(null, null, null);
    }

    public LsfStatusChecker(CmdRunner cmdRunner) {
        this(cmdRunner, null, null);
    }

    public LsfStatusChecker(List<String> lsfStatusCmdPrefix, Pattern lsfStatusPattern) {
        this(null, lsfStatusCmdPrefix, lsfStatusPattern);
    }

    public LsfStatusChecker(CmdRunner cmdRunner, List<String> lsfStatusCmdPrefix, Pattern lsfStatusPattern) {
        // null means, use default
        if (cmdRunner==null) {
            this.cmdRunner=initCmdRunner();
        }
        else {
            this.cmdRunner=cmdRunner;
        }
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
    }

    // for debugging
    protected CmdRunner getCmdRunner() {
        return cmdRunner;
    }

    // for debugging
    protected Pattern getLsfStatusPattern() {
        return lsfStatusPattern;
    }
    
    protected static final CmdRunner initCmdRunner() {
        //skip the first line
        final int numHeaderLines=1;
        return new CommonsExecCmdRunner(numHeaderLines);
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
    protected List<String> initStatusCmd(final DrmJobRecord jobRecord) {
        List<DrmJobRecord> jobRecords=Arrays.asList(jobRecord);
        return initStatusCmd(jobRecords);
    }
    
    protected List<String> initStatusCmd(final Collection<DrmJobRecord> jobRecords) {
        // e.g. bjobs -W 1044898 1044899
        List<String> cmd=new ArrayList<String>();
        cmd.addAll(lsfStatusCmdPrefix);
        for(DrmJobRecord jobRecord : jobRecords) {
            if (!Strings.isNullOrEmpty(jobRecord.getExtJobId())) {
                cmd.add(""+jobRecord.getExtJobId());
            }
        }
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
    public DrmJobStatus checkStatus(DrmJobRecord jobRecord, File lsfLogFile) throws CmdException, InterruptedException {
        if (log.isDebugEnabled()) {
            log.debug("checkStatus for jobId="+jobRecord.getExtJobId());
        }
        final List<String> cmd=initStatusCmd(jobRecord);
        if (log.isDebugEnabled()) {
            log.debug("cmd="+cmd);
        }
        final List<String> out=cmdRunner.runCmd(cmd);
        if (out==null) {
            throw new CmdException("Null output from cmd="+cmd);
        }
        //expecting just one line of output
        if (out.size()==0) {
            throw new CmdException("No output from cmd="+cmd);
        }
        if (out.size()!=1) {
            throw new CmdException("Can only accept one line from 'bjobs -W' command, num lines="+out.size());
        }
    
        String line=out.get(0);
        if (log.isDebugEnabled()) {
            log.debug("line="+line);
            log.debug("lsfLogFile="+lsfLogFile);
        }
        DrmJobStatus jobStatus=LsfBjobsParser.parseAsJobStatus(lsfStatusPattern, line, lsfLogFile);
        if (log.isDebugEnabled()) {
            log.debug("jobStatus="+jobStatus);
        }
        return jobStatus;
    }

}
