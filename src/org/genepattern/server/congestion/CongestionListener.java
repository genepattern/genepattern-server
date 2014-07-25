package org.genepattern.server.congestion;

import com.google.common.eventbus.Subscribe;
import org.apache.log4j.Logger;
import org.genepattern.server.DbException;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.domain.AnalysisJobDAO;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;
import org.genepattern.server.executor.drm.dao.JobRunnerJobDao;
import org.genepattern.server.executor.events.JobCompletionEvent;
import org.genepattern.server.executor.events.JobEventBus;
import org.genepattern.server.executor.events.JobProcessingEvent;
import org.genepattern.server.executor.events.JobStartedEvent;

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
    public void updateUponProcessing(JobStartedEvent event) {
        int jobId = event.getJobStatus().getGpJobNo();

        AnalysisJobDAO dao = new AnalysisJobDAO();
        AnalysisJob job = dao.findById(jobId);

        JobRunnerJobDao jrjDao = new JobRunnerJobDao();
        JobRunnerJob jrjJob = null;

        try {
            jrjJob = jrjDao.selectJobRunnerJob(jobId);
        }
        catch (Exception e) {
            log.error("Error with JobRunnerJob for id: " + jobId + ", exiting updateCongestionTable()", e);
        }
        finally {
            if (jrjJob == null) {
                log.warn("Null JobRunnerJob for id: " + jobId);
            }
        }

        // Get the task LSID
        String lsid = job.getTaskLsid();

        // Find the time difference between submission and start time, then convert to seconds
        // If JobRunnerJob isn't available, fall back to 0
        long queuetime = jrjJob != null ? ((jrjJob.getStartTime().getTime() - jrjJob.getSubmitTime().getTime()) / 1000) : 0;

        // Get the queue name
        String queueName = jrjJob != null ? jrjJob.getQueueId() : null;

        // Update the database
        try {
            CongestionManager.updateCongestionQueuetime(lsid, queueName, queuetime);
        }
        catch (Exception e) {
            log.error("Error updating congestion data for job ID: " + jobId);
        }
    }

    @Subscribe
    public void updateUponCompletion(JobCompletionEvent event) {
        // Get the job
        int jobId = (Integer) event.getSource();

        AnalysisJobDAO dao = new AnalysisJobDAO();
        AnalysisJob job = dao.findById(jobId);

        JobRunnerJobDao jrjDao = new JobRunnerJobDao();
        JobRunnerJob jrjJob = null;

        try {
            jrjJob = jrjDao.selectJobRunnerJob(jobId);
        }
        catch (Exception e) {
            log.error("Error with JobRunnerJob for id: " + jobId + ", exiting updateCongestionTable()", e);
        }
        finally {
            if (jrjJob == null) {
                log.warn("Null JobRunnerJob for id: " + jobId);
            }
        }

        // If the job has completed, update the congestion data
        // This will ignore canceled or erroneous jobs
        if (job.getJobStatus().getStatusId() == JobStatus.JOB_FINISHED) {
            // Get the task LSID
            String lsid = job.getTaskLsid();

            // Find the time difference between submission and start time, then convert to seconds
            // If JobRunnerJob isn't available, fall back to full pending + running time
            long runtime = jrjJob != null ? jrjJob.getCpuTime() : ((job.getCompletedDate().getTime() - job.getSubmittedDate().getTime()) / 1000);

            // Get the queue name
            String queueName = jrjJob != null ? jrjJob.getQueueId() : null;

            // Update the database
            try {
                CongestionManager.updateCongestionRuntime(lsid, queueName, runtime);
            }
            catch (Exception e) {
                log.error("Error updating congestion data for job ID: " + jobId);
            }
        }
    }
}
