package org.genepattern.server.executor.lsf;

import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobState;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: nazaire
 * Date: Nov 2, 2012
 * Time: 2:16:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class LsfErrorCheckerImpl implements ILsfErrorChecker
{
    private static Logger log = Logger.getLogger(LsfErrorCheckerImpl.class);

    LsfErrorStatus errorStatus = null;

    public LsfErrorCheckerImpl(File lsfLogFile) {
        this(true, lsfLogFile);
    }
    public LsfErrorCheckerImpl(boolean hasStarted, File lsfLogFile)
    {
	    BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(lsfLogFile));
            StringBuffer message = new StringBuffer();
            int exitCode = -1;
            DrmJobState jobState=DrmJobState.TERMINATED;
            Integer maxThreads=null;
            Integer maxProcesses=null;

            String line;
            boolean appendMessage = false;
            while((line = reader.readLine()) != null)
            {
                //do not write any error including and after the output section
                if(line.startsWith("The output (if any) follows:")) {
                    break;
                }
                
                if (line.startsWith("Successfully completed.")) {
                    message.append(line);
                    exitCode=0;
                    jobState=DrmJobState.DONE;
                }
                else if (line.startsWith("TERM_OWNER: ")) { // job killed by owner.
                    if (!hasStarted) {
                        jobState=DrmJobState.ABORTED;
                    }
                    else {
                        jobState=DrmJobState.CANCELLED;
                    }
                    message.append(line);
                    message.append("\n");
                    appendMessage=true;
                }
                else if (line.startsWith("TERM_MEMLIMIT: ")) { // job killed after reaching LSF memory usage limit.
                    jobState=DrmJobState.TERM_MEMLIMIT;
                    message.append(line);
                    message.append("\n");
                    appendMessage=true;
                }
                else if (line.startsWith("TERM_RUNLIMIT: ")) { // job killed after reaching LSF run time limit.
                    jobState=DrmJobState.TERM_RUNLIMIT;
                    message.append(line);
                    message.append("\n");
                    appendMessage=true;
                }
                else if (line.startsWith("Exited with exit code")) {
                    exitCode = parseExitCode(line);
                    message.append(line);
                    if (appendMessage) {
                        message.append("\n");
                    }
                    else {
                        jobState=DrmJobState.FAILED;
                        break;
                    }
                }
                else if (line.trim().startsWith("Max Processes")) {
                    maxProcesses = parseResourceUsageIntValue(line);
                    if (appendMessage) {
                        message.append(line);
                        message.append("\n");
                    }
                }
                else if (line.trim().startsWith("Max Threads")) {
                    maxThreads = parseResourceUsageIntValue(line);
                    if (appendMessage) {
                        message.append(line);
                        message.append("\n");
                    }
                }
                else if (appendMessage) {
                    message.append(line);
                    message.append("\n");
                } 
            }
            errorStatus = new LsfErrorStatus(jobState, exitCode, message.toString());
            errorStatus.setMaxThreads(maxThreads);
            errorStatus.setMaxProcesses(maxProcesses);
        }
        catch(IOException io)
        {
           log.error(io);
        }
        finally
        {
            if(reader != null)
            {
                try{reader.close();} catch(IOException e){};
            }
        }
    }

    private int parseExitCode(String line) {
        //parse out exit code
        int startIndex = line.indexOf("exit code");
        String exitCodeString = line.substring(startIndex+10, line.length()-1);
        try {
            return Integer.parseInt(exitCodeString);
        }
        catch(NumberFormatException e) {
            log.error("Error parsing exit code from line="+line, e);
        }
        return -1;
    }
    
    private Integer parseResourceUsageIntValue(String line) {
        try {
            return Integer.valueOf(line.substring(line.indexOf(":")+1).trim());
        }
        catch (Throwable t) {
            log.error("Error parsing integer from line="+line, t);
        }
        return null;
    }

    public LsfErrorStatus getStatus()
    {
	    return errorStatus;
    }
}
