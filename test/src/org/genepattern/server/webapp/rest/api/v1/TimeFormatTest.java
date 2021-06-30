/*******************************************************************************
 * Copyright (c) 2003-2021 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.TimeZone;

import org.genepattern.server.webapp.rest.api.v1.DateUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * junit tests for the time formatter for REST API calls.
 * @author pcarr
 *
 */
public class TimeFormatTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void nullDate() {
        assertEquals("Expecting empty string", "", DateUtil.toIso8601(null));
    }
    
    @Test
    public void testLocalTimeZone() {
        DateTime dt = new DateTime("2007-06-29T08:55:10.23");
        Date theDate = dt.toDate();
        String tzOffsetStr=DateTimeFormat.forPattern("ZZ").print(dt);
        assertEquals("2007-06-29T08:55:10"+tzOffsetStr, DateUtil.toIso8601(theDate));
    }
    
    @Test
    public void testGmtTimeZone() {
        //specify date in California time
        DateTime dt = new DateTime("2007-06-29T08:55:10.23-07:00");
        //display the date in GMT
        Date theDate = dt.toDateTime(DateTimeZone.UTC).toDate();
        assertEquals("2007-06-29T15:55:10Z", DateUtil.toIso8601(theDate, DateTimeZone.UTC.toTimeZone()));
    }
    
    protected Date convertToUtc(Date theDate) {
        DateTime dt = new DateTime(theDate);
        return dt.toDateTime(DateTimeZone.UTC).toDate();
    }

    @Test
    public void toTimeAgoUtc_boston() {
        //specify date in Boston time
        DateTime dt = new DateTime("2007-12-29T08:55:10.23-05:00");
        Date localDate=dt.toDate();
        assertEquals("2007-12-29 13:55:10", DateUtil.toTimeAgoUtc(localDate));
    }

    @Test
    public void toTimeAgoUtc_boston_dst() {
        //specify date in Boston time
        DateTime dt = new DateTime("2007-06-29T08:55:10.23-04:00");
        Date localDate=dt.toDate();
        assertEquals("2007-06-29 12:55:10", DateUtil.toTimeAgoUtc(localDate));
    }
    
    @Test
    public void toTimeAgoUtc_california() {
        //specify date in California time
        DateTime dt = new DateTime("2007-06-29T08:55:10.23-07:00");
        Date localDate=dt.toDate();
        assertEquals("2007-06-29 15:55:10", DateUtil.toTimeAgoUtc(localDate));
    }
    
    /** Expecting IllegalArgumentException with null Date. */
    @Test(expected=IllegalArgumentException.class)
    public void toTimeAgoUtc_NullArg() {
        DateUtil.toTimeAgoUtc(null);
    }
    

    public static final String expectedDateFormat="yyyy[-MM[-dd[THH[:mm[:ss]]]]]";
    protected static void assertDateFormat(final String expected, final String dateParam) {
        assertEquals("parseDateTime('"+dateParam+"')",
            expected,
            //UsersResource.parseDateParam(dateParam).getTime()
            DateUtil.timeAgoUtc.print(
                //ISODateTimeFormat.dateTimeParser().parseDateTime(dateParam).toDate().getTime()
                DateUtil.parseDate(dateParam).getTime()
            )
        );
    }
    
    protected static void assertDateFormatWithTz(final String expected, final String dateSpec, final DateTimeZone inputTz) {
        final TimeZone outputTz;
        if (inputTz == null) {
            outputTz=null;
        }
        else {
            outputTz=inputTz.toTimeZone();
        }
        assertDateFormatWithTz(expected, dateSpec, inputTz, outputTz);
    }
    protected static void assertDateFormatWithTz(final String expected, final String dateSpec, final DateTimeZone inputTz, final TimeZone outputTz) {
        //final DateTimeZone tzDefault=DateTimeZone.forOffsetHours(3);
        //final String dateSpec="2018-01-01T23:00:00";
        final Date date=DateUtil.parseDate(dateSpec,inputTz);
        //final String expected="2018-01-01T23:00:00+03:00";
        assertEquals(expected, DateUtil.toIso8601(date, outputTz));
    }
    
    @Test
    public void timezone_examples() {
        // use 'withChronology' and 'withOffsetParsed' to set a default TimeZone
        // when there is no offset in the dateSpec use the one from the chronology

        // no timezone in date string
        assertDateFormatWithTz( "2018-01-01T23:00:00Z", // <-- expected
            // date string
            "2018-01-01T23:00:00", 
            // input timezone, to use when parsing the date string
            DateTimeZone.UTC
            // no output timezone, use same as input
        );
        assertDateFormatWithTz( "2018-01-02T02:00:00+03:00", 
            "2018-01-01T23:00:00", 
            DateTimeZone.UTC, 
            // output in different timezone, offset +3 hours
            DateTimeZone.forOffsetHours(3).toTimeZone()
        );
        
        // with timezone in date string
        assertDateFormatWithTz("2018-01-01T20:00:00Z", 
            "2018-01-01T23:00:00+03:00", 
            DateTimeZone.UTC
        );
    }
    
    protected void assertDateFormatError(final String dateParam) {
        exception.reportMissingExceptionWithMessage("Expecting IllegalArgumentException for dateParam='"+dateParam+"'");
        exception.expect(java.lang.IllegalArgumentException.class);
        exception.expectMessage(expectedDateFormat);
        DateUtil.parseDate(dateParam);
    }

    @Test
    public void parseDateParam() {
        final Date date=DateUtil.parseDate("2018-01-01T23:00:00");
        final String offset=DateTimeFormat.forPattern("ZZ").print(date.getTime());
        assertEquals("DateUtil.toIso8601", 
             "2018-01-01T23:00:00"+offset, DateUtil.toIso8601(date));
        assertEquals("DateUtil.timeAgoUtc.print", 
            "2018-01-01 23:00:00", DateUtil.timeAgoUtc.print(date.getTime()));
    }

    @Test
    public void parseDateParam_examples() {
        final String expected="2018-01-01 00:00:00";
        assertDateFormat(expected, "2018");
        assertDateFormat(expected, "2018-01");
        assertDateFormat(expected, "2018-01-01");
        assertDateFormat(expected, "2018-01-01T00");
        assertDateFormat(expected, "2018-01-01T00:00");
        assertDateFormat(expected, "2018-01-01T00:00:00");

        assertDateFormat("2018-01-01 23:00:00", "2018-01-01T23");
        assertDateFormat("2018-01-01 23:15:00", "2018-01-01T23:15");
        assertDateFormat("2018-01-01 23:15:33", "2018-01-01T23:15:33");
        
        ///////////////////////
        // time zone examples
        ///////////////////////
        
        // 'Z' for Zulu means 'UTC'
        final Date date_utc=DateUtil.parseDate("2018-01-01T23:15:33Z");
        // expect this date to be in UTC time zone
        assertEquals(
            "2018-01-01T23:15:33Z", 
            DateUtil.toIso8601(date_utc, DateTimeZone.UTC.toTimeZone())
        );

        // -5 hour offset
        final Date date_offset=DateUtil.parseDate("2018-01-01T23:15:33-05:00");
        assertEquals(
            "2018-01-01T23:15:33-05:00", 
            DateUtil.toIso8601(date_offset, DateTimeZone.forOffsetHours(-5).toTimeZone())
        );
        
        // +3 hour offset
        final Date date_offset_2=DateUtil.parseDate("2018-01-01T23:15:33+03:00");
        assertEquals(
            "2018-01-01T23:15:33+03:00", 
            DateUtil.toIso8601(date_offset_2, DateTimeZone.forOffsetHours(3).toTimeZone())
        );
    }

    @Test
    public void parseDateParam_null() {
        exception.expect(java.lang.IllegalArgumentException.class);
        exception.expectMessage(expectedDateFormat);
        @SuppressWarnings("unused")
        final Date date=DateUtil.parseDate((String)null);
    }

    @Test
    public void parseDateParam_empty() {
        exception.expect(java.lang.IllegalArgumentException.class);
        exception.expectMessage(expectedDateFormat);
        @SuppressWarnings("unused")
        final Date date=DateUtil.parseDate("");
    }

    @Test
    public void parseDateParam_error_time_separator() {
        assertDateFormatError("2018-01-01 23:00");
    }

    @Test
    public void parseDateParam_error_timezone_01() {
        assertDateFormatError("2018-01-01T23:00+");
    }

    @Test
    public void parseDateParam_error_timezone_02() {
        assertDateFormatError("2018-01-01T23:00-");
    }

    @Test
    public void parseDateParam_error_timezone_outofbounds() {
        assertDateFormatError("2018-01-01T23:00-24:00");
    }

    @Test
    public void parseDateParam_error_timezone_outofbounds_02() {
        assertDateFormatError("2018-01-01T23:00+24:00");
    }

}
