package org.genepattern.drm.impl.lsf.core;

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
import org.genepattern.drm.DrmJobStatus;

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
    
    public static DrmJobStatus parseAsJobStatus(String line) {
        Matcher lineMatcher = LINE_PATTERN.matcher(line);
        if (lineMatcher.matches()) {
            DrmJobStatus.Builder b = new DrmJobStatus.Builder();
            b.extJobId(lineMatcher.group("JOBID"));
            
            LsfState lsfStatus=LsfState.valueOf(lineMatcher.group("STATUS"));
            if (lsfStatus != null) {
                b.jobState(lsfStatus.getDrmJobState());
                b.jobStatusMessage(lsfStatus.getDescription());
            }
            try {
                b.submitTime(parseDate(lineMatcher.group("SUBMITTIME")));
                b.startTime(parseDate(lineMatcher.group("STARTTIME")));
                b.endTime(parseDate(lineMatcher.group("FINISHTIME")));
            }
            catch (ParseException e) {
                log.error(e);
            }
            try {
                long millis= (Integer.parseInt(lineMatcher.group("CPUhours")) * 3600 * 1000)
                 + (Integer.parseInt(lineMatcher.group("CPUmins")) * 60 * 1000)
                 + Math.round(Double.parseDouble(lineMatcher.group("CPUsecs")) * 1000);
                b.cpuTime(new CpuTime(millis, TimeUnit.MILLISECONDS));
            }
            catch (NumberFormatException e) {
                log.error("error parsing cpuTime from line="+line, e);
            }
            
            Long memUsage=parseOptionalLong(lineMatcher.group("MEM"));
            if (memUsage != null) {
                b.memory(memUsage+" "+memUsageUnits);
            }
            return b.build();
        }
        log.error("Unable to initialize DrmJobStatus from line="+line);
        return null;
    }
    

    private static String parseAsString(String str) {
        return NA.equals(str) ? null : str;
    }
    
    private static Date parseDate(String date) throws ParseException {
        if (NA.equals(date)) {
            return null;
        }
        Calendar now = Calendar.getInstance();
        Calendar result = Calendar.getInstance();
        result.setTime(DATE_FORMAT.parse(date));
        
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
