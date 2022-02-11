/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1;

import java.util.Date;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.format.ISOPeriodFormat;

public class DateUtil {
    private static final Logger log = Logger.getLogger(DateUtil.class);

    public static final Long SEC = 1000L;
    public static final Long MIN = 60*SEC;
    public static final Long HOUR = 60*MIN;
    
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
    public static final DateTimeFormatter timeAgoUtc=DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    
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
    
    public static String toIso8601_duration(final long durationInMillis) {
        return ISOPeriodFormat.standard().print(new Duration(durationInMillis).toPeriod());
    }
    
    public static final String dateFormatSpec="yyyy[-MM[-dd[THH[:mm[:ss]]]]]";
    
    /**
     * Parse the date string in IS0 8601 format.
     * 
     * @param dateStr, the input date in ISO 8601 format  
     */
    public static Date parseDate(final String dateStr) {
        try {
            return ISODateTimeFormat.dateTimeParser()
                .parseDateTime(dateStr)
            .toDate();
        }
        catch (Throwable t) {
            throw new IllegalArgumentException(t.getMessage()+" (format is '"+dateFormatSpec+"')");
        }
    }

    /**
     * Parse the date string in ISO 8601 format. This example method has special handling
     * for the time zone.
     * 
     * @param dateStr, the input date in ISO 8601 format
     * @param tz, the time zone to use when the input date string does not specifiy one.
     */
    protected static Date parseDate(final String dateStr, final DateTimeZone tz) {
       try {
            return ISODateTimeFormat.dateTimeParser()
                // '--withChronology' to set a different default timezone for the parser
                .withChronology(ISOChronology.getInstance(tz))
                // '--withOffsetParsed' to use the parsed timezone
                .withOffsetParsed()
                .parseDateTime(dateStr)
            .toDate();
        }
        catch (Throwable t) {
            throw new IllegalArgumentException(t.getMessage()+" (format is '"+DateUtil.dateFormatSpec+"')");
        }
    }

    /**
     * Custom date formatter for the <a href="http://henyana.github.io/jquery-comment/">jquery-comment plugin</a>, 
     * which uses the <a href="http://timeago.yarp.com/">timeago</a> plugin for jQuery. Convert the given date
     * to UTC time zone and format using the following spec:
     * <pre>
     * yyyy-MM-dd HH:mm:ss
     * </pre>
     * 
     * @param date, a non-null date as stored in a timestamp column in the database in the local server time zone.
     * @return a formatted String representation of the date in the UTC time zone
     * @throws IllegalArgumentException if the date is null
     */
    public static String toTimeAgoUtc(Date date) throws IllegalArgumentException {
        if (date==null) {
            throw new IllegalArgumentException("date==null");
        }
        return DateUtil.timeAgoUtc.withZoneUTC().print(date.getTime());
    }

}
