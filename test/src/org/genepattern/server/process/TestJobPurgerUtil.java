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

}
