/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.drm;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;


public class TestWalltime {
    @Test
    public void testFullSpec() throws Exception {
        Walltime wt=Walltime.fromString("7-12:30:30");
        Assert.assertEquals("duration", 649830, wt.getDuration());
        Assert.assertEquals("units", TimeUnit.SECONDS, wt.getTimeUnit());
        Assert.assertEquals("toString()", "7-12:30:30", wt.toString());
    }

    @Test
    public void testPbsSpec() throws Exception {
        Walltime wt=Walltime.fromString("12:15:00");
        Assert.assertEquals("duration", 44100, wt.getDuration());
        Assert.assertEquals("units", TimeUnit.SECONDS, wt.getTimeUnit());        
        Assert.assertEquals("toString()", "12:15:00", wt.toString());
    }

    @Test
    public void testPbsSpecTrim() throws Exception {
        Walltime wt=Walltime.fromString(" 12:15:00 ");
        Assert.assertEquals("duration", 44100, wt.getDuration());
        Assert.assertEquals("units", TimeUnit.SECONDS, wt.getTimeUnit());        
        Assert.assertEquals("toString()", "12:15:00", wt.toString());
    }

    @Test
    public void testSevenDays() throws Exception {
        //7 days
        Walltime wt=Walltime.fromString("7-00:00:00");
        Assert.assertEquals("duration", 604800, wt.getDuration());
        Assert.assertEquals("units", TimeUnit.SECONDS, wt.getTimeUnit());
        Assert.assertEquals("toString()", "7-00:00:00", wt.toString());
    }
    
    @Test
    public void testJustSeconds() throws Exception {
        Walltime wt=Walltime.fromString("3600");
        Assert.assertEquals("duration", 3600, wt.getDuration());
        Assert.assertEquals("units", TimeUnit.SECONDS, wt.getTimeUnit());
        Assert.assertEquals("toString()", "3600", wt.toString());
    }
    
    @Test
    public void testMinutesAndSeconds() throws Exception {
        Walltime wt=Walltime.fromString("59:59");
        Assert.assertEquals("duration", 3599, wt.getDuration());
        Assert.assertEquals("units", TimeUnit.SECONDS, wt.getTimeUnit());
        Assert.assertEquals("toString()", "59:59", wt.toString());
    }
    
    @Test
    public void testNullIn() throws Exception {
        Walltime wt=Walltime.fromString(null);
        Assert.assertNull("Expecting null value from parse(null)", wt);
    }
    
    @Test
    public void testEmptyIn() throws Exception {
        Walltime wt=Walltime.fromString("");
        Assert.assertEquals("parse empty string", Walltime.NOT_SET, wt);
        Assert.assertEquals("toString()", "00:00", wt.toString());
    }

    /**
     * Expecting -W hh:mm
     */
    @Test
    public void formatHoursAndMinutes_7days() throws Exception {
        Walltime wt=Walltime.fromString("7-00:00:00");
        Assert.assertEquals("7 days in 'hh:mm' format", "168:00", wt.formatHoursAndMinutes());
    }
    
    @Test
    public void formatMinutes_7days() throws Exception {
        Assert.assertEquals("7 days in 'mm' format", 
                "10080", 
                Walltime.fromString("7-00:00:00").formatMinutes());
    }
    
    @Test
    public void formatHoursAndMinutes() throws Exception {
        Assert.assertEquals("01:15:45 in 'mm' format", 
                "01:15",
                Walltime.fromString("01:15:45").formatHoursAndMinutes());
    }

    @Test
    public void formatMinutes() throws Exception {
        Assert.assertEquals("01:15:45 in 'mm' format", 
                "75",
                Walltime.fromString("01:15:45").formatMinutes());
    }
    
    @Test
    public void formatHoursAndMinutes_45min() throws Exception {
        assertEquals("00:45:00 in 'mm' format", 
                "00:45",
                Walltime.fromString("00:45:00").formatHoursAndMinutes());
    }
    
    @Test
    public void testEquals() throws Exception {
        Walltime arg0=Walltime.fromString("12:00:00");
        Walltime arg1=Walltime.fromString("12:00:00");
        assertEquals(arg0, arg1);
    }

    //parse errors
    /**
     * should throw an exception when the '-' separator is the prefix.
     * @throws Exception
     */
    @Test(expected=Exception.class)
    public void testMissingDay() throws Exception {
        Walltime.fromString("-500000");
    }
    
    @Test(expected=Exception.class)
    public void testNegativeMinutes() throws Exception {
        Walltime.fromString("00:-60:00");
    }
    
    @Test(expected=Exception.class)
    public void testNegativeDays() throws Exception {
        Walltime t=Walltime.fromString("-7-12:30:30");
        Assert.assertEquals("duration", 649830, t.getDuration());
        Assert.assertEquals("units", TimeUnit.SECONDS, t.getTimeUnit());
    }

}
