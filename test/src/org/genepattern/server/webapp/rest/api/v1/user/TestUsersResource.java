package org.genepattern.server.webapp.rest.api.v1.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;

import org.genepattern.server.webapp.rest.api.v1.DateUtil;
import org.genepattern.server.webapp.rest.api.v1.TimeFormatTest;
import org.joda.time.format.DateTimeFormat;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for the REST '/gp/rest/v1/users/new' endpoint.
 */
public class TestUsersResource {
    
    @Rule
    public ExpectedException exception = ExpectedException.none();
    
    // "yyyy-MM-dd'T'HH:mm:ss"
    @Test
    public void parseDates_example() {
        final Date now = new Date();
        final Date startDate=UsersResource.parseStartDate(now, "2019-01-01T00:00:00");
        final Date endDate=UsersResource.parseEndDate(now, "2019-03-31T23:59:59");

        final String offset1=DateTimeFormat.forPattern("ZZ").print(startDate.getTime());
        final String offset2=DateTimeFormat.forPattern("ZZ").print(endDate.getTime());
        assertEquals("startDate", "2019-01-01T00:00:00"+offset1, DateUtil.toIso8601(startDate));
        assertEquals("endDate",   "2019-03-31T23:59:59"+offset2, DateUtil.toIso8601(endDate));

        assertEquals("formatDate(startDate)", "2019-01-01T00:00:00"+offset1, UsersResource.UserJSON.formatDate(startDate));
        assertEquals("formatDate(endDate)",   "2019-03-31T23:59:59"+offset2, UsersResource.UserJSON.formatDate(endDate));
    }

    @Test
    public void parseDates_example_by_year() {
        final Date now = new Date();
        final Date startDate=UsersResource.parseStartDate(now, "2018");
        final Date endDate=UsersResource.parseEndDate(now, "2019");

        final String offset=DateTimeFormat.forPattern("ZZ").print(endDate.getTime());
        assertEquals("startDate", "2018-01-01T00:00:00"+offset, DateUtil.toIso8601(startDate));
        assertEquals("endDate",   "2019-01-01T00:00:00"+offset, DateUtil.toIso8601(endDate));
    }

    @Test
    public void parseDates_example_by_month() {
        final Date now = new Date();
        final Date startDate=UsersResource.parseStartDate(now, "2019-01");
        final Date endDate=UsersResource.parseEndDate(now, "2019-02");

        final String offset=DateTimeFormat.forPattern("ZZ").print(endDate.getTime());
        assertEquals("startDate", "2019-01-01T00:00:00"+offset, DateUtil.toIso8601(startDate));
        assertEquals("endDate",   "2019-02-01T00:00:00"+offset, DateUtil.toIso8601(endDate));
    }

    @Test
    public void parseDates_example_by_day() {
        final Date now = new Date();
        final Date startDate=UsersResource.parseStartDate(now, "2019-01-01");
        final Date endDate=UsersResource.parseEndDate(now, "2019-02-28");

        final String offset=DateTimeFormat.forPattern("ZZ").print(endDate.getTime());
        assertEquals("startDate", "2019-01-01T00:00:00"+offset, DateUtil.toIso8601(startDate));
        assertEquals("endDate",   "2019-02-28T00:00:00"+offset, DateUtil.toIso8601(endDate));
    }

    @Test
    public void parseEndDate() {
        final Date endDate=UsersResource.parseEndDate("2019-01-01T00:00:00");
        final String offset=DateTimeFormat.forPattern("ZZ").print(endDate.getTime());

        final String actual=UsersResource.UserJSON.formatDate(endDate);
        assertEquals("2019-01-01T00:00:00"+offset, actual);
    }
    
    @Test
    public void parseEndDate_error() {
        exception.expect(java.lang.IllegalArgumentException.class);
        exception.expectMessage(TimeFormatTest.expectedDateFormat);
        @SuppressWarnings("unused")
        final Date endDate=UsersResource.parseEndDate("2019/01/01");
    }

    @Test
    public void parseEndDate_nowArg_nullArg() {
        final Date now = new Date();
        final Date endDate=UsersResource.parseEndDate(now, null);
        assertEquals(DateUtil.toTimeAgoUtc(now), DateUtil.toTimeAgoUtc(endDate));
    }

    @Test
    public void parseEndDate_nowArg_emptyArg() {
        final Date now = new Date();
        final Date endDate=UsersResource.parseEndDate(now, "");
        assertEquals(DateUtil.toTimeAgoUtc(now), DateUtil.toTimeAgoUtc(endDate));
    }

    @Test
    public void parseStartDate() {
        final Date now = new Date();
        final String startDateParam="2018-01-01T23:00:00";
        final Date startDate = UsersResource.parseStartDate(now, startDateParam);
        assertNotNull(startDate);
    }

    @Test
    public void parseStartDate_null() {
        final Date now = new Date();
        final Date startDate = UsersResource.parseStartDate(now, (String)null);
        assertEquals(0, startDate.compareTo(UsersResource.sevenDaysBefore(now)));
    }
    
    @Test
    public void parseStartDate_empty() {
        final Date now = new Date();
        final Date startDate = UsersResource.parseStartDate(now, "");
        assertEquals(0, startDate.compareTo(UsersResource.sevenDaysBefore(now)));
    }

    @Test
    public void parseStartDate_error() {
        exception.expect(java.lang.IllegalArgumentException.class);
        exception.expectMessage(TimeFormatTest.expectedDateFormat);
        final Date now = new Date();
        final String startDateParam="2018-01-01 23:00";
        UsersResource.parseStartDate(now, startDateParam);
    }

}
