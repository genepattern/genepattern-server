package org.genepattern.server.congestion;

import java.util.Date;

import org.apache.log4j.Logger;
import org.genepattern.server.executor.events.GpJobRecordedEvent;
import org.genepattern.server.executor.events.JobCompletedEvent;
import org.genepattern.server.executor.events.JobEventBus;
import org.genepattern.server.executor.events.JobStartedEvent;
import org.genepattern.server.job.status.Status;

import com.google.common.eventbus.Subscribe;

/**
 * Listen for job completion events and update the database as appropriate
 *
 * @author Thorin Tabor
 */
public class CongestionListener {
    private static Logger log = Logger.getLogger(CongestionListener.class);
    private static boolean isInit = false;

    /**
     * Initialize the listener, register it to the JobEventBus
     */
    public static void init() {
        if (!isInit) {
            JobEventBus.instance().register(new CongestionListener());
            isInit = true;
        }
    }

    @Subscribe
    public void updateUponProcessing(JobStartedEvent event) {
        if (event==null || event.getJobStatus()==null) {
            log.error("event.jobStatus==null, ignoring");
            return;
        }
        final String lsid=event.getTaskLsid();
        final long queuetime=getQueueTimeInSeconds(event.getJobStatus());
        final String queueName=event.getJobStatus().getQueueId();

        // Update the database
        try {
            CongestionManager.updateCongestionQueuetime(lsid, queueName, queuetime);
        }
        catch (Exception e) {
            log.error("Error updating congestion data for job ID: " + event.getJobStatus().getGpJobNo());
        }
    }
    
    /**
     * Helper method to calculate the interval between two dates.
     * @param start
     * @param end
     * @return the interval in number of seconds, or null if either arg is null.
     */
    protected long asSeconds(final Date start, final Date end) {
        if (end == null || start == null) {
            return -1;
        }
        // just in case you swap the args
        return Math.abs((end.getTime() - start.getTime()) / 1000);
    }

    /**
     * Find the time difference between submission and start time, then convert to seconds.
     * If JobRunnerJob isn't available, fall back to 0.
     * 
     * @param jobStatus, the Status instance from the JobStatusEvent.
     * @return
     */
    protected long getQueueTimeInSeconds(final Status jobStatus) {
        if (jobStatus==null) {
            log.error("jobStatus==null");
            return 0;
        }
        if (jobStatus.getStartTime()==null) {
            log.debug("jobStatus.startTime==null");
            return 0;
        }
        if (jobStatus.getSubmitTime()==null) {
            log.debug("jobStatus.submitTime==null");
            return 0;
        }

        // Note: this field can be used if the submitTime is not known jobStatus.getDateSubmittedToGp();
        long queuetime = (jobStatus.getStartTime().getTime() - jobStatus.getSubmitTime().getTime()) / 1000;
        return queuetime;
    }

    /**
     * Calculate the estimated runtime of the job in seconds,
     * based on the cpuTime.
     * If the cpuTime isn't available, use the queue startTime and endTime as a fall back.
     * For legacy jobs, if the queue times are not available,
     * use the GP dateSubmitted and dateCompleted as a fall back.
     * 
     * @param jobStatus
     * @return
     */
    protected long getRuntimeInSeconds(final Status jobStatus) {
        if (jobStatus.getCpuTime() != null) {
            if (log.isDebugEnabled()) {
                log.debug("gpJobNo="+jobStatus.getGpJobNo()+", computing runtime from cpuTime: "+jobStatus.getCpuTime());
            }
            if (jobStatus.getCpuTime().getTime()==0) {
                log.warn("cpuTime==0, fallback to startTime and endTime");
            }
            else {
                return jobStatus.getCpuTime().asSeconds();
            }
        }
        long runtime=asSeconds(jobStatus.getStartTime(), jobStatus.getEndTime());
        if (runtime >= 0) {
            if (log.isDebugEnabled()) {
                log.debug("gpJobNo="+jobStatus.getGpJobNo()+", computing runtime from jobStatus.startTime and jobStatus.endTime: "+runtime+" s");
            }
            return runtime;
        }
        runtime=asSeconds(jobStatus.getDateSubmittedToGp(), jobStatus.getDateCompletedInGp());
        if (runtime >= 0) {
            if (log.isDebugEnabled()) {
                log.debug("gpJobNo="+jobStatus.getGpJobNo()+", computing runtime from jobStatus.dateSubmittedToGp and jobStatus.dateCompletedInGp: "+runtime+" s");
            }
            return runtime;
        }
        log.error("Not enough jobStatus info to calculate runtime for gpJobNo="+jobStatus.getGpJobNo());
        return 0;
    }
    
    @Subscribe
    public void onJobCompletedEvent(JobCompletedEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("gpJobNo="+event.getJobStatus().getGpJobNo()+", "+event.getJobStatus().getJobState());
        }
        if (event.getJobStatus()==null) {
            log.error("event.jobStatus==null");
            return;
        }
        if (!event.getJobStatus().getIsFinished()) {
            log.error("jobStatus.isFinished==false, gpJobNo="+event.getJobStatus().getGpJobNo());
        }
        
        String lsid = event.getTaskLsid();
        String queueName = event.getJobStatus().getQueueId();
        long runtime=getRuntimeInSeconds(event.getJobStatus());
        // Update the database
        try {
            CongestionManager.updateCongestionRuntime(lsid, queueName, runtime);
        }
        catch (Throwable t) {
            log.error("Error updating congestion data for job ID: " + event.getJobStatus().getGpJobNo(), t);
        }
    }
    
    // @Subscribe
    public void onGpJobRecordedEvent(final GpJobRecordedEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("gpJobNo="+event.getJobStatus().getGpJobNo()+", "+event.getJobStatus().getJobState());
        }
    }

}
