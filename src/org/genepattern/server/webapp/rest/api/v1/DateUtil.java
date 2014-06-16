package org.genepattern.server.webapp.rest.api.v1;

import java.util.Date;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class DateUtil {
    private static final Logger log = Logger.getLogger(DateUtil.class);

    /**
     * The default ISO 8601 date formatter returned when getting JSON representations
     * of jobs and result files.
     * Using Joda Time without the milliseconds
     * <pre>
       yyyy-MM-dd'T'HH:mm:ssZZ
     * </pre>
     */
    public static final DateTimeFormatter isoNoMillis=DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ");
    public static final DateTimeFormatter isoNoMillisUtc=DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZoneUTC();
    
    /**
     * Outputs the date in 'ECMAScript 5 ISO-8601' format, in the server's timezone.
     * 
     * @param date
     * @return
     */
    public static String toIso8601(final Date date) {
        return toIso8601(date, null);
    }

    /**
     * 
     * @param date
     * @param TimeZone
     * @return
     */
    public static String toIso8601(final Date date, final TimeZone tz) {
        if (date==null) {
            log.debug("Unexpected null date");
            return "";
        }
        if (tz==null) {
            return isoNoMillis.print(date.getTime());
        }
        DateTimeZone jodaTz=DateTimeZone.forTimeZone(tz);
        if (jodaTz.getOffset(date.getTime())==0L) {
            return isoNoMillisUtc.withZone(jodaTz).print(date.getTime());
        }
        else {
            return isoNoMillis.withZone(jodaTz)
                .print(date.getTime());
        }
    }

}
