/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.process;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;

public class JobPurgerUtil {
    private static Logger log = Logger.getLogger(JobPurgerUtil.class);

    /**
     * Allow customization of the purge interval by checking for the 'purgeJobsAfter' and 'purgeTime' properties
     * on a per-user basis. Defaults to the server setting if there are no user customizations.
     * 
     * @param userContext
     * @param now
     * @return
     */
    public static Date getCutoffForUser(final GpConfig gpConfig, GpContext userContext, final Date now) {
        if (userContext==null) {
            //use the system defaults
            userContext=GpContext.getServerContext();
        }
        final int purgeJobsAfter=gpConfig.getGPIntegerProperty(userContext, "purgeJobsAfter", -1);
        final String purgeTime=gpConfig.getGPProperty(userContext, "purgeTime", "23:00");
        return getCutoff(now, purgeJobsAfter, purgeTime);
    }

    /**
     * Given the current timestamp (now) a purgeTime (time of day) and a purgeJobsAfter interval
     * (the number of days to keep jobs before purging them) get a cutoff date.
     * Any job older than this cutoff date should be purged from the system.
     * 
     * @param now
     * @param purgeJobsAfter
     * @param purgeTime
     * @return
     */
    public static Date getCutoff(final Date now, final int purgeJobsAfter, final String purgeTime) {
        if (now==null) {
            throw new IllegalArgumentException("Must pass in a valid date arg");
        }
        if (purgeJobsAfter<0) {
            return null;
        }
        int hourOfDay=JobPurger.DEFAULT_PURGE_HOUR;
        int minute=JobPurger.DEFAULT_PURGE_MINUTE;
        try {
            if (purgeTime==null) {
                throw new Exception("purgeTime is null");
            }
            // expecting purgeTime=HH:mm, e.g. purgeTime=23:00
            final String[] split=purgeTime.trim().split(":");
            if (split.length==2) {
                hourOfDay=Integer.parseInt(split[0]);
                if (hourOfDay < 0 || hourOfDay >= 24) {
                    throw new Exception("Invalid format for purgeTime="+purgeTime+", hourOfDay must be between 0 and 23");
                }
                minute=Integer.parseInt(split[1]);
                if (minute < 0 || minute >= 60) {
                    throw new Exception("Invalid format for purgeTime="+purgeTime+", minute must be between 0 and 59");
                }                
            }
            else {
                throw new Exception("Invalid format for purgeTime="+purgeTime+", expecting purgeTime=HH:mm, e.g. purgeTime=23:00");
            }
        }
        catch (Exception e) {
            log.error(e);
            hourOfDay=JobPurger.DEFAULT_PURGE_HOUR;
            minute=JobPurger.DEFAULT_PURGE_MINUTE;
        }
        catch (Throwable t) {
            log.error("Unexpected exception for purgeJobsAfter="+purgeJobsAfter+", purgeTime="+purgeTime, t);
            hourOfDay=JobPurger.DEFAULT_PURGE_HOUR;
            minute=JobPurger.DEFAULT_PURGE_MINUTE;
        }

        Calendar c = Calendar.getInstance();
        c.setTime(now);
        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        //round to the nearest minute
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        //account for when the purgeTime is after now
        long cutoffTime=c.getTime().getTime();
        if (now.getTime() < cutoffTime) {
            //move the cutoff date back one day
            c.add(Calendar.DATE, -1);
        }
        c.add(Calendar.DATE, -purgeJobsAfter);
        return c.getTime();
    }

   /**
    * Helper method which gives the next time that the purger will run based on the current time.
    * 
    * @param now, the current time from which to compute the next run.
    * @param purgeTime, system setting giving the time of day (hour and minute) to run the purger.
    * 
    * @return
    */
    public static Date getNextPurgeTime(Date now, String purgeTime) {
        GregorianCalendar nextPurgeTime = new GregorianCalendar();
        nextPurgeTime.setTime(now);
        GregorianCalendar purgeTOD = new GregorianCalendar();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
        try {
            purgeTOD.setTime(dateFormat.parse(purgeTime));
        } 
        catch (ParseException pe) {
            purgeTOD.set(GregorianCalendar.HOUR_OF_DAY, JobPurger.DEFAULT_PURGE_HOUR);
            purgeTOD.set(GregorianCalendar.MINUTE, JobPurger.DEFAULT_PURGE_MINUTE);
        }
        nextPurgeTime.set(GregorianCalendar.HOUR_OF_DAY, purgeTOD.get(GregorianCalendar.HOUR_OF_DAY));
        nextPurgeTime.set(GregorianCalendar.MINUTE, purgeTOD.get(GregorianCalendar.MINUTE));
        nextPurgeTime.set(GregorianCalendar.SECOND, 0);
        nextPurgeTime.set(GregorianCalendar.MILLISECOND, 0);
        
        if (!nextPurgeTime.getTime().after(now)) {
            // it's already on or after today's purge time, wait until tomorrow's
            nextPurgeTime.add(GregorianCalendar.DATE, 1);
        }

        log.debug("next purge will be at " + nextPurgeTime.getTime());
        return nextPurgeTime.getTime();
    }

    /**
     * Helper method which gives the date at which a given job will be purged,  
     * based on the job completion date.
     * 
     * Allows for customization via the 'purgeJobsAfter' and 'purgeTime' configuration properties. 
     * 
     * @param gpConfig
     * @param userContext
     * @param jobCompletionDate
     * 
     * @return the purge date, or null if the date is unknown or if the purger is not configured to purge jobs.
     */
    public static final Date getJobPurgeDate(final GpConfig gpConfig, final GpContext userContext, final Date jobCompletionDate) {
        final int purgeJobsAfter=gpConfig.getGPIntegerProperty(userContext, "purgeJobsAfter", -1);
        final String purgeTime=gpConfig.getGPProperty(userContext, "purgeTime", "23:00");

        return getJobPurgeDate(purgeJobsAfter, purgeTime, jobCompletionDate);
    }
    
    private static final Date getJobPurgeDate(final int purgeJobsAfter, final String purgeTime, final Date jobCompletionDate) {
        if (jobCompletionDate == null) {
            return null;
        }
        
        if (purgeJobsAfter < 0) {
            return null;
        }

        GregorianCalendar purgeTOD = new GregorianCalendar();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
        try {
            purgeTOD.setTime(dateFormat.parse(purgeTime));
        } 
        catch (ParseException pe) {
            purgeTOD.set(GregorianCalendar.HOUR_OF_DAY, JobPurger.DEFAULT_PURGE_HOUR);
            purgeTOD.set(GregorianCalendar.MINUTE, JobPurger.DEFAULT_PURGE_MINUTE);
        }
        
        Calendar jobPurgeCal = new GregorianCalendar();
        jobPurgeCal.setTime(jobCompletionDate);
        jobPurgeCal.add(Calendar.DATE, purgeJobsAfter);
        // if the purgeTime is less than the job completion time, add another day
        Date jobPurgeDateInit = jobPurgeCal.getTime();
        jobPurgeCal.set(GregorianCalendar.HOUR_OF_DAY, purgeTOD.get(GregorianCalendar.HOUR_OF_DAY));
        jobPurgeCal.set(GregorianCalendar.MINUTE, purgeTOD.get(GregorianCalendar.MINUTE));
        jobPurgeCal.set(GregorianCalendar.SECOND, 0);
        jobPurgeCal.set(GregorianCalendar.MILLISECOND, 0);
        Date jobPurgeDateAdjusted = jobPurgeCal.getTime();
        if (jobPurgeDateAdjusted.before(jobPurgeDateInit)) {
            jobPurgeCal.add(Calendar.DATE, 1);
        }
        
        Date purgeDate = jobPurgeCal.getTime();
        
        //if the purgeDate is in the past, return the next time the purger will run
        Date now = new Date();
        if (!purgeDate.after(now)) {
            return getNextPurgeTime(now, purgeTime);
        }
        return jobPurgeCal.getTime();
    }

}
