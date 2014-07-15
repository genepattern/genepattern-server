package org.genepattern.drm.impl.lsf.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobStatus;


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
    
//    private LinkedHashMap<String, DrmJobRecord> jobRecords=new LinkedHashMap<String, DrmJobRecord>();
//    private LinkedHashMap<String, File> lsfLogFiles=new LinkedHashMap<String, File>();
//    private LinkedHashMap<String, String> bjobsOutputLines=new LinkedHashMap<String,String>();
//    private List<DrmJobStatus> jobStatuses=new ArrayList<DrmJobStatus>();
    
    private DrmJobRecord jobRecord=null;
    private File lsfLogFile=null;
    private CmdRunner cmdRunner;
    
    private DrmJobStatus jobStatus=null;
    
    
    public LsfStatusChecker(DrmJobRecord jobRecord) {
        this.jobRecord=jobRecord;
        this.lsfLogFile=getLogFile(jobRecord);
        this.cmdRunner=initCmdRunner();
    }
    
    public LsfStatusChecker(DrmJobRecord jobRecord, File lsfLogFile) {
        this.jobRecord=jobRecord;
        this.lsfLogFile=lsfLogFile;
        this.cmdRunner=initCmdRunner();
    }
    
    public LsfStatusChecker(DrmJobRecord jobRecord, File lsfLogFile, CmdRunner cmdRunner) {
        this.jobRecord=jobRecord;
        this.lsfLogFile=lsfLogFile;
        this.cmdRunner=cmdRunner;
    }
    
    protected CmdRunner initCmdRunner() {
        //skip the first line
        final int numHeaderLines=1;
        return new CommonsExecCmdRunner(numHeaderLines);
    }

    private File getLogFile(DrmJobRecord drmJobRecord) {
        return getRelativeFile(drmJobRecord.getWorkingDir(), drmJobRecord.getLogFile());
    }

    private File getRelativeFile(final File workingDir, final File file) {
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
    
    protected List<String> initStatusCmd(final DrmJobRecord jobRecord) {
        // e.g. bjobs -W 1044898
        List<String> cmd=new ArrayList<String>();
        cmd.add("bjobs");
        cmd.add("-W");
        cmd.add(""+jobRecord.getExtJobId());
        return cmd;
    }
    
    protected List<String> initStatusCmd(final Collection<DrmJobRecord> jobRecords) {
        // e.g. bjobs -W 1044898
        List<String> cmd=new ArrayList<String>();
        cmd.add("bjobs");
        cmd.add("-W");
        for(DrmJobRecord jobRecord : jobRecords) {
            cmd.add(""+jobRecord.getExtJobId());
        }
        return cmd;
    }

    /**
     * Runs the 'bjobs -W' command for the given list of jobRecords.
     */
    public void checkStatus() throws CmdException, InterruptedException {
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
        jobStatus = LsfBjobsParser.parseAsJobStatus(line, lsfLogFile);
        if (log.isDebugEnabled()) {
            log.debug("jobStatus="+jobStatus);
        }
    }

    public DrmJobStatus getStatus() {
        return jobStatus;
    }

}
