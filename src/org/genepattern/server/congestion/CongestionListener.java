package org.genepattern.server.congestion;

import com.google.common.eventbus.Subscribe;
import org.apache.log4j.Logger;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.domain.AnalysisJobDAO;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.executor.events.JobCompletionEvent;
import org.genepattern.server.executor.events.JobEventBus;

import javax.swing.event.ChangeEvent;

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
    public void updateCongestionTable(JobCompletionEvent event) {
        // Get the job
        int jobId = (Integer) event.getSource();
        AnalysisJobDAO dao = new AnalysisJobDAO();
        AnalysisJob job = dao.findById(jobId);

        // If the job has completed, update the congestion data
        // This will ignore canceled or erroneous jobs
        if (job.getJobStatus().getStatusId() == JobStatus.JOB_FINISHED) {
            // Get the task LSID
            String lsid = job.getTaskLsid();

            // Find the time difference between submission and completion, then convert to seconds
            long runtime = (job.getCompletedDate().getTime() - job.getSubmittedDate().getTime()) / 1000;

            // Update the database
            try {
                CongestionManager.updateCongestion(lsid, runtime);
            }
            catch (Exception e) {
                log.error("Error updating congestion data for job ID: " + jobId);
            }
        }
    }
}
