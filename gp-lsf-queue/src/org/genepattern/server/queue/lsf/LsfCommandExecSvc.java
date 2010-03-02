package org.genepattern.server.queue.lsf;

import java.io.File;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.queue.CommandExecutorService;
import org.genepattern.webservice.JobInfo;

import edu.mit.broad.core.lsf.LsfJob;
import edu.mit.broad.core.lsf.LsfWrapper;

public class LsfCommandExecSvc implements CommandExecutorService {
    private static Logger log = Logger.getLogger(LsfCommandExecSvc.class);
    private LsfWrapper lsfWrapper = null;

    public void start() {
        lsfWrapper = new LsfWrapper();
        lsfWrapper.start(10);
    }

    public void stop() {
        if (lsfWrapper != null) {
            lsfWrapper.stop();
        }
    }

    public void runCommand(String[] commandLine, Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, JobInfo jobInfo, String stdin, StringBuffer stderrBuffer) {
        LsfCommand cmd = new LsfCommand();
        cmd.runCommand(commandLine, environmentVariables, runDir, stdoutFile, stderrFile, jobInfo, stdin, stderrBuffer);
        
        LsfJob lsfJob = cmd.getLsfJob();
        lsfJob = lsfWrapper.dispatchLsfJob(lsfJob);
    }
    
    public void terminateJob(JobInfo jobInfo) {
        log.error("Terminate job not enabled");
        //TODO: implement terminate job in BroadCore library. It currently is not part of the library, pjc.
    }

}
