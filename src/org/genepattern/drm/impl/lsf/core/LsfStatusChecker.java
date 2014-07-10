package org.genepattern.drm.impl.lsf.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobStatus;


/**
 * Helper class for getting the status of an LSF job via a command line interface.
 * @author pcarr
 *
 */
public class LsfStatusChecker {
    private static final Logger log = Logger.getLogger(LsfStatusChecker.class);

    public static class LsfBjobsParserLogOutputStream extends LogOutputStream {
        private boolean first=true;
        private List<DrmJobStatus> results = new ArrayList<DrmJobStatus>();

        @Override
        protected void processLine(String line, int level) {
            if (first) {
                //ignore
                first=false;
                return;
            }
            
            DrmJobStatus jobStatus=LsfBjobsParser.parseAsJobStatus(line);
            if (jobStatus != null) {
                results.add(jobStatus);
            }
        }
        
        public List<DrmJobStatus> getResults() {
            return results;
        }
    }

    
    public DrmJobStatus getStatus(DrmJobRecord jobRecord) throws LsfCmdException {
        List<DrmJobRecord> jobRecords=new ArrayList<DrmJobRecord>(1);
        jobRecords.add(jobRecord);
        List<DrmJobStatus> results=getStatus(jobRecords);
        if (results != null && results.size()==1) {
            return results.get(0);
        }
        throw new LsfCmdException("Unexpected number of results from getStatus");
    }
    
    public List<DrmJobStatus> getStatus(final List<DrmJobRecord> jobRecords) throws LsfCmdException {
        List<String> cmd=initStatusCmd(jobRecords);
        CommandLine cmdLine=initCommandLine(cmd);
        try {
            return exec(cmdLine);
        }
        catch (ExecuteException e) {
            log.error(e);
            throw new LsfCmdException("Error running cmd", e);
        }
        catch (IOException e) {
            log.error(e);
            throw new LsfCmdException("Error running cmd", e);
        }
    }
    
    protected List<String> initStatusCmd(final List<DrmJobRecord> jobRecords) {
        // e.g. bjobs -W 1044898
        List<String> cmd=new ArrayList<String>();
        cmd.add("bjobs");
        cmd.add("-W");
        for(DrmJobRecord jobRecord : jobRecords) {
            cmd.add(""+jobRecord.getExtJobId());
        }
        return cmd;
    }
    
    protected CommandLine initCommandLine(final List<String> gpCommand) {
        boolean handleQuoting=false;
        CommandLine cl=new CommandLine(gpCommand.get(0));
        for(int i=1; i<gpCommand.size(); ++i) {
            cl.addArgument(gpCommand.get(i), handleQuoting);
        }
        return cl;
    }
    
    protected List<DrmJobStatus> exec(CommandLine cl) throws ExecuteException, IOException {
        LsfBjobsParserLogOutputStream collectResults=new LsfBjobsParserLogOutputStream();
        
        DefaultExecutor exec=new DefaultExecutor();
        // collect results from stdout
        exec.setStreamHandler( new PumpStreamHandler( collectResults ) );
        // kill the process after 60 seconds
        exec.setWatchdog(new ExecuteWatchdog(60000));
        exec.setProcessDestroyer(new ShutdownHookProcessDestroyer());
        exec.execute(cl);
        
        return collectResults.getResults();
    }

}
