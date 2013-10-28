package org.genepattern.server.process;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import junit.framework.TestCase;

/**
 * Unit tests for JobPurgerUtil.
 * 
 * @author pcarr
 */
public class TestJobPurgerUtil extends TestCase {
    final private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public void testNextPurgeTimeToday() throws ParseException {
        String purgeTime = "09:00";
        Date now = df.parse("2009-06-04 08:00:00");
        Date expectedNext = df.parse("2009-06-04 09:00:00");
        assertEquals("next purge later today", expectedNext, JobPurgerUtil.getNextPurgeTime(now, purgeTime));
    }
    
    public void testNextPurgeTimeExactlyOneDay() throws ParseException {
        String purgeTime = "09:00";
        Date now = df.parse("2009-06-04 09:00:00");
        Date expectedNext = df.parse("2009-06-05 09:00:00");
        assertEquals("next purge exactly a day", expectedNext, JobPurgerUtil.getNextPurgeTime(now, purgeTime));
    }
    
    public void testNextPurgeTimeTomorrow() throws ParseException {
        String purgeTime = "09:00";
        Date now = df.parse("2009-06-04 09:11:00");
        Date expectedNext = df.parse("2009-06-05 09:00:00");
        assertEquals("next purge tomorrow", expectedNext, JobPurgerUtil.getNextPurgeTime(now, purgeTime));
    }
    
    public void testJobPurgeDate() throws ParseException {
        System.setProperty("purgeJobsAfter", "5");
        System.setProperty("purgeTime", "13:25");
        
        assertEquals("job run same day, before purgeTime", 
                df.parse("2109-06-09 13:25:00"), 
                JobPurgerUtil.getJobPurgeDate(df.parse("2109-06-04 11:43:21")));
        assertEquals("job run same day, exactly on purgeTime", 
                df.parse("2109-06-09 13:25:00"), 
                JobPurgerUtil.getJobPurgeDate(df.parse("2109-06-04 13:25:00")));
        assertEquals("job run same day, after purgeTime", 
                df.parse("2109-06-10 13:25:00"), 
                JobPurgerUtil.getJobPurgeDate(df.parse("2109-06-04 19:21:03")));
        assertEquals("job run previous day, after purgeTime", 
                df.parse("2109-06-09 13:25:00"), 
                JobPurgerUtil.getJobPurgeDate(df.parse("2109-06-03 21:37:41")));
    }
    
    /**
     * special case for jobs which have not yet been purged but which will be purged the next time the purger runs.
     */
    public void testJobPurgeDateOverdue() throws ParseException {
        System.setProperty("purgeJobsAfter", "6");
        System.setProperty("purgeTime", "12:00");
 
        Date now = new Date();
        Date jobCompletionDate05 = df.parse("2009-01-01 23:00:00");
        
        Date expectedPurgeDate05 = JobPurgerUtil.getNextPurgeTime(now, "12:00");
        assertEquals("job overdue", expectedPurgeDate05, JobPurgerUtil.getJobPurgeDate(jobCompletionDate05));
    }
    
    /**
     * Unit test job purge date when purgeJobsAfter is less than zero.
     */
    public void testJobPurgeDateNoPurgeInterval() {
        System.setProperty("purgeJobsAfter", "-1");
        System.setProperty("purgeTime", "23:00");

        assertNull("job", JobPurgerUtil.getJobPurgeDate(new Date()));
    }
    
    /**
     * jUnit test when purgeJobsAfter is less than 0, e.g. -1, meaning no jobs are purged.
     * @throws ParseException
     */
    public void testCutoff_null() throws ParseException {
        //the date on which the purger is running, may not necessarily be the same as the purgeTime
        Date now=df.parse("2013-11-01 23:00:01"); // 11pm Nov 11
        final String purgeTime="23:00";
        int purgeJobsAfter=-1;
        Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, purgeTime);
        assertNull("Expecting null cutoff for '-1'", cutoff);
    }
    
    /**
     * jUnit test when purgeJobsAfter is 0, now is before purgeTime.
     * @throws ParseException
     */
    public void testCutoff_02a() throws ParseException {
        final int purgeJobsAfter=0;
        final Date now=df.parse("2013-11-01 18:00:00");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "23:00");
        assertEquals("purgeJobsAfter="+purgeJobsAfter,
                df.parse("2013-10-31 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 0, now is identical to purgeTime.
     * @throws ParseException
     */
    public void testCutoff_02b() throws ParseException {
        final int purgeJobsAfter=0;
        final Date now=df.parse("2013-11-01 23:00:00");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "23:00");
        assertEquals("purgeJobsAfter="+purgeJobsAfter,
                df.parse("2013-11-01 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 0, now is after purgeTime.
     * @throws ParseException
     */
    public void testCutoff_02c() throws ParseException {
        //test 2a, 0 cutoff, jump ahead
        final int purgeJobsAfter=0;
        final Date now=df.parse("2013-11-01 23:00:01");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "23:00");
        assertEquals("purgeJobsAfter="+purgeJobsAfter,
                df.parse("2013-11-01 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 1, now is before purgeTime.
     * @throws ParseException
     */
    public void testCutoff_03a() throws ParseException {
        final int purgeJobsAfter=1;
        final Date now=df.parse("2013-11-01 18:00:00");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "23:00");
        assertEquals("purgeJobsAfter="+purgeJobsAfter,
                df.parse("2013-10-30 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 1, now is identical to purgeTime.
     * @throws ParseException
     */
    public void testCutoff_03b() throws ParseException {
        final int purgeJobsAfter=1;
        final Date now=df.parse("2013-11-01 23:00:00");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "23:00");
        assertEquals("purgeJobsAfter="+purgeJobsAfter,
                df.parse("2013-10-31 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 1, now is after purgeTime.
     * @throws ParseException
     */
    public void testCutoff_03c() throws ParseException {
        final int purgeJobsAfter=1;
        final Date now=df.parse("2013-11-01 23:05:23");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "23:00");
        assertEquals("purgeJobsAfter="+purgeJobsAfter,
                df.parse("2013-10-31 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 7, now is before purgeTime.
     * @throws ParseException
     */
    public void testCutoff_04a() throws ParseException {
        final int purgeJobsAfter=7;
        final Date now=df.parse("2013-11-01 18:00:00");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "23:00");
        assertEquals("purgeJobsAfter="+purgeJobsAfter,
                df.parse("2013-10-24 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 1, now is identical to purgeTime.
     * @throws ParseException
     */
    public void testCutoff_04b() throws ParseException {
        final int purgeJobsAfter=7;
        final Date now=df.parse("2013-11-01 23:00:00");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "23:00");
        assertEquals("purgeJobsAfter="+purgeJobsAfter,
                df.parse("2013-10-25 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 1, now is after purgeTime.
     * @throws ParseException
     */
    public void testCutoff_04c() throws ParseException {
        final int purgeJobsAfter=7;
        final Date now=df.parse("2013-11-01 23:05:23");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "23:00");
        assertEquals("purgeJobsAfter="+purgeJobsAfter,
                df.parse("2013-10-25 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 7, purgeTime has white space characters.
     * @throws ParseException
     */
    public void testCutoff_trimPurgeTime() throws ParseException {
        final int purgeJobsAfter=7;
        final Date now=df.parse("2013-11-01 18:00:00");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, " 19:05");
        assertEquals("purgeJobsAfter="+purgeJobsAfter,
                df.parse("2013-10-24 19:05:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 7,purgeTime is the empty string.
     * @throws ParseException
     */
    public void testCutoff_emtpyPurgeTime() throws ParseException {
        final int purgeJobsAfter=7;
        final Date now=df.parse("2013-11-01 23:00:00");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "");
        assertEquals("purgeJobsAfter="+purgeJobsAfter,
                df.parse("2013-10-25 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 7, purgeTime is null.
     * @throws ParseException
     */
    public void testCutoff_nullPurgeTime() throws ParseException {
        final int purgeJobsAfter=7;
        final Date now=df.parse("2013-11-01 23:05:23");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, null);
        assertEquals("purgeJobsAfter="+purgeJobsAfter,
                df.parse("2013-10-25 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 7, purgeTime is invalid.
     * @throws ParseException
     */
    public void testCutoff_invalidPurgeTime_a() throws ParseException {
        final int purgeJobsAfter=7;
        final Date now=df.parse("2013-11-01 23:05:23");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "25:00");
        assertEquals("purgeJobsAfter="+purgeJobsAfter,
                df.parse("2013-10-25 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 7, purgeTime is invalid.
     * @throws ParseException
     */
    public void testCutoff_invalidPurgeTime_b() throws ParseException {
        final int purgeJobsAfter=7;
        final Date now=df.parse("2013-11-01 23:05:23");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "13:99");
        assertEquals("purgeJobsAfter="+purgeJobsAfter,
                df.parse("2013-10-25 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 7, purgeTime is invalid.
     * @throws ParseException
     */
    public void testCutoff_invalidPurgeTime_c() throws ParseException {
        final int purgeJobsAfter=7;
        final Date now=df.parse("2013-11-01 23:05:23");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, ":");
        assertEquals("purgeJobsAfter="+purgeJobsAfter,
                df.parse("2013-10-25 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 7, purgeTime is invalid.
     * @throws ParseException
     */
    public void testCutoff_invalidPurgeTime_d() throws ParseException {
        final int purgeJobsAfter=7;
        final Date now=df.parse("2013-11-01 23:05:23");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "-13487645");
        assertEquals("purgeJobsAfter="+purgeJobsAfter,
                df.parse("2013-10-25 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 7, purgeTime is invalid.
     * @throws ParseException
     */
    public void testCutoff_invalidPurgeTime_e() throws ParseException {
        final int purgeJobsAfter=7;
        final Date now=df.parse("2013-11-01 23:05:23");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "Not a number");
        assertEquals("purgeJobsAfter="+purgeJobsAfter,
                df.parse("2013-10-25 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 7, purgeTime is invalid.
     * @throws ParseException
     */
    public void testCutoff_invalidPurgeTime_f() throws ParseException {
        final int purgeJobsAfter=7;
        final Date now=df.parse("2013-11-01 23:05:23");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "3.14");
        assertEquals("purgeJobsAfter="+purgeJobsAfter,
                df.parse("2013-10-25 23:00:00"),
                cutoff);
    }

}
