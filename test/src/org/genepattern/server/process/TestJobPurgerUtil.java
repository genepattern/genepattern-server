package org.genepattern.server.process;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.genepattern.junitutil.ConfigUtil;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.GpContext;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for JobPurgerUtil.
 * 
 * @author pcarr
 */
public class TestJobPurgerUtil {
    final private Date parseDateFormat(final String dateSpec) throws ParseException {
        final DateFormat df=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return df.parse(dateSpec);
    }

    @BeforeClass
    public static void beforeClass() {
        ConfigUtil.loadConfigFile(TestJobPurgerUtil.class, "config.yaml");
    }

    @Test
    public void testNextPurgeTimeToday() throws ParseException {
        String purgeTime = "09:00";
        Date now = parseDateFormat("2009-06-04 08:00:00");
        Date expectedNext = parseDateFormat("2009-06-04 09:00:00");
        Assert.assertEquals("next purge later today", expectedNext, JobPurgerUtil.getNextPurgeTime(now, purgeTime));
    }
    
    @Test
    public void testNextPurgeTimeExactlyOneDay() throws ParseException {
        String purgeTime = "09:00";
        Date now = parseDateFormat("2009-06-04 09:00:00");
        Date expectedNext = parseDateFormat("2009-06-05 09:00:00");
        Assert.assertEquals("next purge exactly a day", expectedNext, JobPurgerUtil.getNextPurgeTime(now, purgeTime));
    }
    
    @Test
    public void testNextPurgeTimeTomorrow() throws ParseException {
        String purgeTime = "09:00";
        Date now = parseDateFormat("2009-06-04 09:11:00");
        Date expectedNext = parseDateFormat("2009-06-05 09:00:00");
        Assert.assertEquals("next purge tomorrow", expectedNext, JobPurgerUtil.getNextPurgeTime(now, purgeTime));
    }
    
    @Test
    public void testJobPurgeDate() throws ParseException {
        System.setProperty("purgeJobsAfter", "5");
        System.setProperty("purgeTime", "13:25");
        
        Assert.assertEquals("job run same day, before purgeTime", 
                parseDateFormat("2109-06-09 13:25:00"), 
                JobPurgerUtil.getJobPurgeDate(parseDateFormat("2109-06-04 11:43:21")));
        Assert.assertEquals("job run same day, exactly on purgeTime", 
                parseDateFormat("2109-06-09 13:25:00"), 
                JobPurgerUtil.getJobPurgeDate(parseDateFormat("2109-06-04 13:25:00")));
        Assert.assertEquals("job run same day, after purgeTime", 
                parseDateFormat("2109-06-10 13:25:00"), 
                JobPurgerUtil.getJobPurgeDate(parseDateFormat("2109-06-04 19:21:03")));
        Assert.assertEquals("job run previous day, after purgeTime", 
                parseDateFormat("2109-06-09 13:25:00"), 
                JobPurgerUtil.getJobPurgeDate(parseDateFormat("2109-06-03 21:37:41")));
    }
    
    /**
     * special case for jobs which have not yet been purged but which will be purged the next time the purger runs.
     */
    @Test
    public void testJobPurgeDateOverdue() throws ParseException {
        System.setProperty("purgeJobsAfter", "6");
        System.setProperty("purgeTime", "12:00");
 
        Date now = new Date();
        Date jobCompletionDate05 = parseDateFormat("2009-01-01 23:00:00");
        
        Date expectedPurgeDate05 = JobPurgerUtil.getNextPurgeTime(now, "12:00");
        Assert.assertEquals("job overdue", expectedPurgeDate05, JobPurgerUtil.getJobPurgeDate(jobCompletionDate05));
    }
    
    /**
     * Unit test job purge date when purgeJobsAfter is less than zero.
     */
    @Test
    public void testJobPurgeDateNoPurgeInterval() {
        System.setProperty("purgeJobsAfter", "-1");
        System.setProperty("purgeTime", "23:00");

        Assert.assertNull("job", JobPurgerUtil.getJobPurgeDate(new Date()));
    }
    
    /**
     * jUnit test when purgeJobsAfter is less than 0, e.g. -1, meaning no jobs are purged.
     * @throws ParseException
     */
    @Test
    public void testCutoff_null() throws ParseException {
        //the date on which the purger is running, may not necessarily be the same as the purgeTime
        Date now=parseDateFormat("2013-11-01 23:00:01"); // 11pm Nov 11
        final String purgeTime="23:00";
        int purgeJobsAfter=-1;
        Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, purgeTime);
        Assert.assertNull("Expecting null cutoff for '-1'", cutoff);
    }
    
    /**
     * jUnit test when purgeJobsAfter is 0, now is before purgeTime.
     * @throws ParseException
     */
    @Test
    public void testCutoff_02a() throws ParseException {
        final int purgeJobsAfter=0;
        final Date now=parseDateFormat("2013-11-01 18:00:00");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "23:00");
        Assert.assertEquals("purgeJobsAfter="+purgeJobsAfter,
                parseDateFormat("2013-10-31 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 0, now is identical to purgeTime.
     * @throws ParseException
     */
    @Test
    public void testCutoff_02b() throws ParseException {
        final int purgeJobsAfter=0;
        final Date now=parseDateFormat("2013-11-01 23:00:00");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "23:00");
        Assert.assertEquals("purgeJobsAfter="+purgeJobsAfter,
                parseDateFormat("2013-11-01 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 0, now is after purgeTime.
     * @throws ParseException
     */
    @Test
    public void testCutoff_02c() throws ParseException {
        //test 2a, 0 cutoff, jump ahead
        final int purgeJobsAfter=0;
        final Date now=parseDateFormat("2013-11-01 23:00:01");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "23:00");
        Assert.assertEquals("purgeJobsAfter="+purgeJobsAfter,
                parseDateFormat("2013-11-01 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 1, now is before purgeTime.
     * @throws ParseException
     */
    @Test
    public void testCutoff_03a() throws ParseException {
        final int purgeJobsAfter=1;
        final Date now=parseDateFormat("2013-11-01 18:00:00");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "23:00");
        Assert.assertEquals("purgeJobsAfter="+purgeJobsAfter,
                parseDateFormat("2013-10-30 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 1, now is identical to purgeTime.
     * @throws ParseException
     */
    @Test
    public void testCutoff_03b() throws ParseException {
        final int purgeJobsAfter=1;
        final Date now=parseDateFormat("2013-11-01 23:00:00");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "23:00");
        Assert.assertEquals("purgeJobsAfter="+purgeJobsAfter,
                parseDateFormat("2013-10-31 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 1, now is after purgeTime.
     * @throws ParseException
     */
    @Test
    public void testCutoff_03c() throws ParseException {
        final int purgeJobsAfter=1;
        final Date now=parseDateFormat("2013-11-01 23:05:23");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "23:00");
        Assert.assertEquals("purgeJobsAfter="+purgeJobsAfter,
                parseDateFormat("2013-10-31 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 7, now is before purgeTime.
     * @throws ParseException
     */
    public void testCutoff_04a() throws ParseException {
        final int purgeJobsAfter=7;
        final Date now=parseDateFormat("2013-11-01 18:00:00");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "23:00");
        Assert.assertEquals("purgeJobsAfter="+purgeJobsAfter,
                parseDateFormat("2013-10-24 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 1, now is identical to purgeTime.
     * @throws ParseException
     */
    public void testCutoff_04b() throws ParseException {
        final int purgeJobsAfter=7;
        final Date now=parseDateFormat("2013-11-01 23:00:00");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "23:00");
        Assert.assertEquals("purgeJobsAfter="+purgeJobsAfter,
                parseDateFormat("2013-10-25 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 1, now is after purgeTime.
     * @throws ParseException
     */
    @Test
    public void testCutoff_04c() throws ParseException {
        final int purgeJobsAfter=7;
        final Date now=parseDateFormat("2013-11-01 23:05:23");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "23:00");
        Assert.assertEquals("purgeJobsAfter="+purgeJobsAfter,
                parseDateFormat("2013-10-25 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 7, purgeTime has white space characters.
     * @throws ParseException
     */
    @Test
    public void testCutoff_trimPurgeTime() throws ParseException {
        final int purgeJobsAfter=7;
        final Date now=parseDateFormat("2013-11-01 18:00:00");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, " 19:05");
        Assert.assertEquals("purgeJobsAfter="+purgeJobsAfter,
                parseDateFormat("2013-10-24 19:05:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 7,purgeTime is the empty string.
     * @throws ParseException
     */
    @Test
    public void testCutoff_emtpyPurgeTime() throws ParseException {
        final int purgeJobsAfter=7;
        final Date now=parseDateFormat("2013-11-01 23:00:00");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "");
        Assert.assertEquals("purgeJobsAfter="+purgeJobsAfter,
                parseDateFormat("2013-10-25 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 7, purgeTime is null.
     * @throws ParseException
     */
    @Test
    public void testCutoff_nullPurgeTime() throws ParseException {
        final int purgeJobsAfter=7;
        final Date now=parseDateFormat("2013-11-01 23:05:23");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, null);
        Assert.assertEquals("purgeJobsAfter="+purgeJobsAfter,
                parseDateFormat("2013-10-25 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 7, purgeTime is invalid.
     * @throws ParseException
     */
    @Test
    public void testCutoff_invalidPurgeTime_a() throws ParseException {
        final int purgeJobsAfter=7;
        final Date now=parseDateFormat("2013-11-01 23:05:23");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "25:00");
        Assert.assertEquals("purgeJobsAfter="+purgeJobsAfter,
                parseDateFormat("2013-10-25 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 7, purgeTime is invalid.
     * @throws ParseException
     */
    @Test
    public void testCutoff_invalidPurgeTime_b() throws ParseException {
        final int purgeJobsAfter=7;
        final Date now=parseDateFormat("2013-11-01 23:05:23");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "13:99");
        Assert.assertEquals("purgeJobsAfter="+purgeJobsAfter,
                parseDateFormat("2013-10-25 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 7, purgeTime is invalid.
     * @throws ParseException
     */
    @Test
    public void testCutoff_invalidPurgeTime_c() throws ParseException {
        final int purgeJobsAfter=7;
        final Date now=parseDateFormat("2013-11-01 23:05:23");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, ":");
        Assert.assertEquals("purgeJobsAfter="+purgeJobsAfter,
                parseDateFormat("2013-10-25 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 7, purgeTime is invalid.
     * @throws ParseException
     */
    @Test
    public void testCutoff_invalidPurgeTime_d() throws ParseException {
        final int purgeJobsAfter=7;
        final Date now=parseDateFormat("2013-11-01 23:05:23");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "-13487645");
        Assert.assertEquals("purgeJobsAfter="+purgeJobsAfter,
                parseDateFormat("2013-10-25 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 7, purgeTime is invalid.
     * @throws ParseException
     */
    @Test
    public void testCutoff_invalidPurgeTime_e() throws ParseException {
        final int purgeJobsAfter=7;
        final Date now=parseDateFormat("2013-11-01 23:05:23");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "Not a number");
        Assert.assertEquals("purgeJobsAfter="+purgeJobsAfter,
                parseDateFormat("2013-10-25 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test when purgeJobsAfter is 7, purgeTime is invalid.
     * @throws ParseException
     */
    @Test
    public void testCutoff_invalidPurgeTime_f() throws ParseException {
        final int purgeJobsAfter=7;
        final Date now=parseDateFormat("2013-11-01 23:05:23");
        final Date cutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, "3.14");
        Assert.assertEquals("purgeJobsAfter="+purgeJobsAfter,
                parseDateFormat("2013-10-25 23:00:00"),
                cutoff);
    }

    //
    // test related to custom configuration of purgeInterval
    //
    /**
     * jUnit test for userContext specific cutoff date, using default settings.
     * @throws ParseException
     */
    @Test public void testCutoffForUser_defaultConfig() throws ParseException {
        final GpContext userContext=GpContext.getContextForUser("test");
        final Date now=parseDateFormat("2013-11-01 23:05:23");
        final Date cutoff=JobPurgerUtil.getCutoffForUser(userContext, now);
        
        Assert.assertNull("Expecting null cutoffDate, by default the purge interval is '-1', which means don't purge files", cutoff);
    }
    
    /**
     * jUnit test for userContext specific cutoff date, using default settings.
     * @throws ParseException
     */
    @Test public void testCutoffForUser_defaultPurgeTime() throws ParseException {
        final GpContext userContext=GpContext.getContextForUser("customInterval");
        final Date now=parseDateFormat("2013-11-01 23:05:23");
        final Date cutoff=JobPurgerUtil.getCutoffForUser(userContext, now);
        Assert.assertEquals("customInterval is 3",
                parseDateFormat("2013-10-29 23:00:00"),
                cutoff);
    }

    /**
     * jUnit test for userContext specific purgeJobsAfter and purgeTime.
     * @throws ParseException
     */
    @Test public void testCutoffForUser_customConfig() throws ParseException {
        final GpContext userContext=GpContext.getContextForUser("customUser");
        final Date now=parseDateFormat("2013-11-01 23:05:23");
        final Date cutoff=JobPurgerUtil.getCutoffForUser(userContext, now);
        Assert.assertEquals("Expecting purgeTime=09:00 and purgeInterval=3",
                parseDateFormat("2013-10-29 09:00:00"),
                cutoff);
    }
    
    @Test public void testNextPurgeTime_defaultConfig() throws ParseException {
        final GpContext userContext=GpContext.getContextForUser("test");
        //by default there is no cutoff 
        final Date completionDate=parseDateFormat("2013-10-25 13:25:00");
        final Date purgeDate=JobPurgerUtil.getJobPurgeDate(userContext, completionDate);
        Assert.assertNull("By default, jobs are not purged", purgeDate);
    }

    @Test public void testNextPurgeTime_customConfig_before() throws ParseException {
        final Date now=new Date();
        final Date next=JobPurgerUtil.getNextPurgeTime(now, "09:00");
        final GregorianCalendar cal = new GregorianCalendar();
        final GpContext userContext=GpContext.getContextForUser("customUser");
        //See config.yaml for 'customUser' customizations
        //purge jobs after 3 days, purgeTime is 9:00 am
        
        // job completed just before purgeTime
        cal.setTime(next);
        cal.add(Calendar.SECOND, -1);
        final Date completionDate=cal.getTime();
        cal.setTime(next);
        cal.add(Calendar.DAY_OF_YEAR, 3);
        final Date expectedPurgeDate=cal.getTime();

        final Date purgeDate=JobPurgerUtil.getJobPurgeDate(userContext, completionDate);
        
        Assert.assertEquals("expecting purgeDate to be the next purge date", expectedPurgeDate, purgeDate);
    }

    @Test public void testNextPurgeTime_customConfig_at() throws ParseException {
        final Date now=new Date();
        final Date next=JobPurgerUtil.getNextPurgeTime(now, "09:00");
        final GregorianCalendar cal = new GregorianCalendar();
        final GpContext userContext=GpContext.getContextForUser("customUser");
        // job completed at purgeTime
        cal.setTime(next);
        cal.add(Calendar.DAY_OF_YEAR, -3);
        final Date completionDate=cal.getTime();
        final Date purgeDate=JobPurgerUtil.getJobPurgeDate(userContext, completionDate);
        Assert.assertEquals("custom config, a job which completed at purgeTime", next, purgeDate);
    }

    @Test public void testNextPurgeTime_customConfig_after() throws ParseException {
        final Date now=new Date();
        final Date next=JobPurgerUtil.getNextPurgeTime(now, "09:00");
        final GregorianCalendar cal = new GregorianCalendar();        
        final GpContext userContext=GpContext.getContextForUser("customUser");
        // job completed after purgeTime
        cal.setTime(next);
        cal.add(Calendar.SECOND, 1);
        final Date completionDate=cal.getTime();
        cal.setTime(next);
        cal.add(Calendar.DAY_OF_YEAR, 4);
        final Date expectedPurgeDate=cal.getTime();
        
        final Date purgeDate=JobPurgerUtil.getJobPurgeDate(userContext, completionDate);
        Assert.assertEquals("custom config, a job which completed after purgeTime", expectedPurgeDate, purgeDate);
    }
    
}
