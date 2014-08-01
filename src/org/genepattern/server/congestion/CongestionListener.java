package org.genepattern.server.congestion;

import org.apache.log4j.Logger;
import org.genepattern.server.executor.events.JobStartedEvent;
import org.genepattern.server.job.status.Status;

import com.google.common.eventbus.Subscribe;

/**
 * Listen for job completion events and update the database as appropriate.
 * You must register this listener with the JobEvent bus in order to listen for events.
 * The registration is done in the JobEvenBus initializer.
 *
 * @author Thorin Tabor
 */
public class CongestionListener {
    private static Logger log = Logger.getLogger(CongestionListener.class);

    @Subscribe
    public void updateUponProcessing(JobStartedEvent event) {
        if (event==null || event.getJobStatus()==null) {
            log.error("event.jobStatus==null, ignoring");
            return;
        }

        long queuetime = getQueueTime(event.getJobStatus());
        String queueName = event.getJobStatus().getQueueId();

        // Update the database
        try {
            CongestionManager.updateCongestionQueuetime(queueName, queuetime);
        }
        catch (Exception e) {
            log.error("Error updating congestion data for job ID: " + event.getJobStatus().getGpJobNo());
        }
    }
    
    /**
     * Find the time difference between submission and start time, then convert to seconds
     * If JobRunnerJob isn't available, fall back to 0
     * 
     * @param jobStatus, the Status instance from the JobStatusEvent.
     * @return
     */
    protected long getQueueTime(Status jobStatus) {
        if (jobStatus == null) {
            log.error("jobStatus is null");
            return 0;
        }
        if (jobStatus.getStartTime()==null) {
            log.debug("jobStatus.startTime is null");
            return 0;
        }
        if (jobStatus.getSubmitTime()==null) {
            log.debug("jobStatus.submitTime is null");
            return 0;
        }

        // Note: this field can be used if the submitTime is not known jobStatus.getDateSubmittedToGp();
        long queuetime = (jobStatus.getStartTime().getTime() - jobStatus.getSubmitTime().getTime()) / 1000;
        return queuetime;
    }
}
