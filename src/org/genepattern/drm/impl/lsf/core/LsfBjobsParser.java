package org.genepattern.drm.impl.lsf.core;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.genepattern.drm.CpuTime;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.server.executor.lsf.LsfErrorCheckerImpl;
import org.genepattern.server.executor.lsf.LsfErrorStatus;

/**
 * Parse the output from the 'bjobs -W' command.
 * The regex LINE_PATTERN provided by Doug Voet, 
 * from the package org.broadinstitute.cga.execution.lsf;
 * 
 * Code was modified to make it easier to use apache commons exec to run the 'bjobs' command.
 * Code was modified to make it easier to convert the parsed output into a list of DrmJobStatus objects.
 */
public class LsfBjobsParser {
    private static Log log = LogFactory.getLog(LsfBjobsParser.class);
    
    // see this regex in action: http://regexr.com/38o8a
    public static final Pattern LINE_PATTERN = Pattern.compile("(?<JOBID>\\d+)\\s+(?<USER>\\S+)\\s+(?<STATUS>\\S+)\\s+(?<QUEUE>\\S+)\\s+(?<FROMHOST>\\S+)\\s+(?<EXECHOST>\\S+)\\s+(?<JOBNAME>.*\\S)\\s+(?<SUBMITTIME>\\d\\d\\/\\d\\d-\\d\\d:\\d\\d:\\d\\d)\\s+(?<PROJNAME>\\S+)\\s+(?<CPUhours>\\d\\d\\d):(?<CPUmins>\\d\\d):(?<CPUsecs>\\d\\d\\.\\d\\d)\\s+(?<MEM>\\d+)\\s+(?<SWAP>\\d+)\\s+(?<PIDS>-|(?:(?:\\d+,)*\\d+))\\s+(?<STARTTIME>-|\\d\\d\\/\\d\\d-\\d\\d:\\d\\d:\\d\\d)\\s+(?<FINISHTIME>-|\\d\\d\\/\\d\\d-\\d\\d:\\d\\d:\\d\\d)\\s*");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd-HH:mm:ss");
    
    private static final String NA = "-";
    private static final String memUsageUnits="mb";
    
    public static DrmJobStatus parseAsJobStatus(final String line) throws InterruptedException {
        File lsfLogFile=null;
        return parseAsJobStatus(line, lsfLogFile);
    }

    public static DrmJobStatus parseAsJobStatus(final String line, final File lsfLogFile) throws InterruptedException {
        Matcher lineMatcher = LINE_PATTERN.matcher(line);
        
        if (lineMatcher.matches()) {
            final LsfState lsfState=LsfState.valueOf(lineMatcher.group("STATUS"));
            final Date submitTime=parseDate(lineMatcher.group("SUBMITTIME"));
            final Date startTime=parseDate(lineMatcher.group("STARTTIME"));
            final Date endTime=parseDate(lineMatcher.group("FINISHTIME"));
            final DrmJobState jobState;
            final String jobStatusMessage;
            Integer exitCode=null;
            if (lsfState != null) {
                //special-case for cancelled job before it started
                if (lsfState==LsfState.EXIT && startTime==null) {
                    jobState=DrmJobState.ABORTED;
                    jobStatusMessage="Job was cancelled before it started running.";
                }
                else if (lsfState==LsfState.EXIT && lsfLogFile != null) {
                    //for completed job, parse the lsf log file (.lsf.out) for exitCode and custom status message
                    waitForFile(lsfLogFile);
                    LsfErrorStatus lsfErrorStatus = checkStatusFromLsfLogFile(lsfLogFile);
                    if (lsfErrorStatus != null && 
                            lsfErrorStatus.getExitCode()==130 && 
                            lsfErrorStatus.getErrorMessage().contains("TERM_OWNER: job killed by owner.")
                    ) {
                        //special-case: job cancelled after it started running
                        jobState=DrmJobState.CANCELLED;
                        jobStatusMessage=DrmJobState.CANCELLED.getDescription();
                    }
                    else if (lsfErrorStatus != null &&
                            lsfErrorStatus.getExitCode()==1 &&
                            lsfErrorStatus.getErrorMessage().contains("TERM_MEMLIMIT: job killed after reaching LSF memory usage limit.")                    
                    ) {
                        //special-case: job terminated because of memory limit
                        jobState=DrmJobState.TERM_MEMLIMIT;
                        jobStatusMessage="TERM_MEMLIMIT: job killed after reaching LSF memory usage limit.";
                    }
                    else if (lsfErrorStatus != null &&
                            lsfErrorStatus.getExitCode()==134 &&
                            lsfErrorStatus.getErrorMessage().startsWith("TERM_RUNLIMIT")
                    ) {
                        //special-case: job terminated because of runtime (wallclock) limit
                        jobState=DrmJobState.TERM_RUNLIMIT;
                        jobStatusMessage=lsfErrorStatus.getErrorMessage();
                    }
                    else {
                        jobState=lsfState.getDrmJobState();
                        jobStatusMessage=lsfState.getDescription();
                    }
                    if (lsfErrorStatus != null) {
                        exitCode=lsfErrorStatus.getExitCode();
                    }
                }
                else {
                    jobState=lsfState.getDrmJobState();
                    jobStatusMessage=lsfState.getDescription();
                }
            }
            else {
                jobState=null;
                jobStatusMessage=null;
            }
            
            //check for exitCode
            if (exitCode==null && jobState==DrmJobState.DONE) {
                exitCode=0;
            }

            DrmJobStatus.Builder b = new DrmJobStatus.Builder();
            b.extJobId(lineMatcher.group("JOBID"));
            
            if (lsfState != null) {
                b.jobState(jobState);
                b.jobStatusMessage(jobStatusMessage);
            }
            b.submitTime(submitTime);
            b.startTime(startTime);
            b.endTime(endTime);
            CpuTime cpuTime=null;
            try {
                cpuTime=parseCpuTime(lineMatcher);
            }
            catch (NumberFormatException e) {
                log.error("error parsing cpuTime from line="+line, e);
            }
            if (cpuTime != null) {
                b.cpuTime(cpuTime);
            }
            Long memUsage=parseOptionalLong(lineMatcher.group("MEM"));
            if (memUsage != null) {
                b.memory(memUsage+" "+memUsageUnits);
            }
            if (exitCode != null) {
                b.exitCode(exitCode);
            }
            return b.build();
        }
        log.error("Unable to initialize DrmJobStatus from line="+line);
        return null;
    }

    public static void waitForFile(final File file) throws InterruptedException {
        //wait 3 seconds to give the job a chance to write the .lsf.out file
        final int sleepInterval=3000;
        final int retryCount=5;
        waitForFile(file, sleepInterval, retryCount);
    }

    public static void waitForFile(final File file, int sleepInterval, int retryCount) throws InterruptedException {
        int count=0;
        while(count<retryCount && !file.exists()) {
            ++count;
            Thread.sleep(sleepInterval);
        }
    }
    
    /**
     * For a completed job ...
     * ... read the ".lsf.out" logFile to get the exitCode.
     */
    public static LsfErrorStatus checkStatusFromLsfLogFile(final File lsfLogFile) {
        if (lsfLogFile != null) {
            if (lsfLogFile.exists()) {
                log.debug("checking error status ... lsfJobOutputFile="+lsfLogFile);
                LsfErrorCheckerImpl errorCheck = new LsfErrorCheckerImpl(lsfLogFile);
                LsfErrorStatus status = errorCheck.getStatus();
                return status;
            }
        }
        log.error("Error getting LsfErrorStatus from lsfLogFile="+lsfLogFile);
        return null;
    }
    
    private static CpuTime parseCpuTime(Matcher lineMatcher) throws NumberFormatException {
        long millis= (Integer.parseInt(lineMatcher.group("CPUhours")) * 3600 * 1000)
                + (Integer.parseInt(lineMatcher.group("CPUmins")) * 60 * 1000)
                + Math.round(Double.parseDouble(lineMatcher.group("CPUsecs")) * 1000);
        return new CpuTime(millis, TimeUnit.MILLISECONDS);
    }

    private static String parseAsString(String str) {
        return NA.equals(str) ? null : str;
    }
    
    private static Date parseDate(String date) {
        if (NA.equals(date)) {
            return null;
        }
        Calendar now = Calendar.getInstance();
        Calendar result = Calendar.getInstance();
        try {
            result.setTime(DATE_FORMAT.parse(date));
        }
        catch (ParseException e) {
            log.error("Error parsing date from string="+date, e);
            return null;
        }
        // silly LSF does not have the year in the date format
        // assume the year is this year but if the date ends up being
        // in the future set it to last year
        result.set(Calendar.YEAR, now.get(Calendar.YEAR));
        if (result.after(now)) {
            result.set(Calendar.YEAR, now.get(Calendar.YEAR)-1);
        }
        return result.getTime();
    }
    
    private static Long parseOptionalLong(String n) {
        if (NA.equals(n)) {
            return null;
        }
        return Long.parseLong(n);
    }
    
    private static String[] parsePids(String pids) {
        if (NA.equals(pids)) {
            return new String[0];
        }
        return pids.split(",");
    }
    
}
