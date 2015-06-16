/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1;

import java.util.Date;

import org.genepattern.server.webapp.rest.api.v1.DateUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.junit.Assert;
import org.junit.Test;

/**
 * junit tests for the time formatter for REST API calls.
 * @author pcarr
 *
 */
public class TimeFormatTest {
    @Test
    public void nullDate() {
        Assert.assertEquals("Expecting empty string", "", DateUtil.toIso8601(null));
    }
    
    @Test
    public void testLocalTimeZone() {
        DateTime dt = new DateTime("2007-06-29T08:55:10.23");
        Date theDate = dt.toDate();
        String tzOffsetStr=DateTimeFormat.forPattern("ZZ").print(dt);
        Assert.assertEquals("2007-06-29T08:55:10"+tzOffsetStr, DateUtil.toIso8601(theDate));
    }
    
    @Test
    public void testGmtTimeZone() {
        //specify date in California time
        DateTime dt = new DateTime("2007-06-29T08:55:10.23-07:00");
        //display the date in GMT
        Date theDate = dt.toDateTime(DateTimeZone.UTC).toDate();
        Assert.assertEquals("2007-06-29T15:55:10Z", DateUtil.toIso8601(theDate, DateTimeZone.UTC.toTimeZone()));
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
        Assert.assertEquals("2007-12-29 13:55:10", DateUtil.toTimeAgoUtc(localDate));
    }

    @Test
    public void toTimeAgoUtc_boston_dst() {
        //specify date in Boston time
        DateTime dt = new DateTime("2007-06-29T08:55:10.23-04:00");
        Date localDate=dt.toDate();
        Assert.assertEquals("2007-06-29 12:55:10", DateUtil.toTimeAgoUtc(localDate));
    }
    
    @Test
    public void toTimeAgoUtc_california() {
        //specify date in California time
        DateTime dt = new DateTime("2007-06-29T08:55:10.23-07:00");
        Date localDate=dt.toDate();
        Assert.assertEquals("2007-06-29 15:55:10", DateUtil.toTimeAgoUtc(localDate));
    }
    
    /** Expecting IllegalArgumentException with null Date. */
    @Test(expected=IllegalArgumentException.class)
    public void toTimeAgoUtc_NullArg() {
        DateUtil.toTimeAgoUtc(null);
    }
    

}
