package org.genepattern.server.congestion;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.congestion.Congestion;
import org.genepattern.server.dm.congestion.CongestionDao;

import java.util.concurrent.TimeUnit;

/**
 * Manager for the job status congestion data
 *
 * @author Thorin Tabor
 */
public class CongestionManager {
    private static Logger log = Logger.getLogger(CongestionManager.class);

    /**
     * Represents the status of the job queue for the specified task
     *
     * RED = Severe wait time
     * YELLOW = Moderate wait time
     * GREEN = Light or no wait time
     */
    public static enum QueueStatus {
        RED, YELLOW, GREEN
    }

    /**
     * Get the status of the job queue for the specified task
     *
     * @param queue
     * @return
     */
    static public QueueStatus getQueueStatus(String queue) {
        boolean inTransaction = HibernateUtil.isInTransaction();

        CongestionDao dao = new CongestionDao();

        try {
            Congestion congestion = dao.getCongestion(queue);

            if (!inTransaction) {
                HibernateUtil.commitTransaction();
            }

            // If the status is unknown, assume yellow
            if (congestion == null) {
                return QueueStatus.YELLOW;
            }

            return applyQueueThresholds(congestion);
        }
        catch (Throwable t) {
            log.error("Error in getQueueStatus()", t);
            return QueueStatus.YELLOW;
        }
    }

    /**
     * Get a congestion object by LSID
     * @param queue
     * @return
     */
    static public Congestion getCongestion(String queue) {
        CongestionDao dao = new CongestionDao();
        return dao.getCongestion(queue);
    }

    /**
     * Update the congestion queue time data for a particular task
     * Use when you don't already have a congestion object
     *
     * Estimate algorithm:
     *     If there is no waiting jobs in thw queue, return 0
     *     Otherwise return the time the last job that left the queue waited
     *
     * @param queuetime
     * @return
     * @throws Exception
     */
    static public void updateCongestionQueuetime(String queueName, long queuetime) throws Exception {
        boolean inTransaction = HibernateUtil.isInTransaction();

        CongestionDao dao = new CongestionDao();

        try {
            Congestion congestion = dao.getCongestion(queueName);

            if (congestion == null) {
                createCongestion(queueName, queuetime);
                return;
            }

            long estimatedQueuetime = calculateQueuetime(dao, queueName, queuetime);

            congestion.setQueuetime(estimatedQueuetime);
            dao.saveOrUpdate(congestion);

            HibernateUtil.commitTransaction();
        }
        catch (Throwable t) {
            log.error("Error in updateCongestion(), rolling back commit.", t);
            HibernateUtil.rollbackTransaction();
            throw new Exception("Error updating congestion: " + queueName);
        }
    }

    /**
     * Returns a pretty string for the runtime
     *
     * @param runtime - A counter of the seconds between when a job was submitted and when it was completed
     * @return
     */
    static public String prettyRuntime(long runtime) {
        long day = TimeUnit.SECONDS.toDays(runtime);
        long hour = TimeUnit.SECONDS.toHours(runtime) - (day * 24);
        long minute = TimeUnit.SECONDS.toMinutes(runtime) - (TimeUnit.SECONDS.toHours(runtime) * 60);

        String prettyPrint = "";
        if (day > 0) {
            prettyPrint += day + (day == 1 ? " day" : " days") + " ";
        }
        if (hour > 0 || day > 0) {
            prettyPrint += hour + (hour == 1 ? " hour" : " hours") + " ";
        }
        if (minute > 0 || hour > 0 || day > 0) {
            prettyPrint += minute + (minute == 1 ? " minute" : " minutes") + " ";
        }

        if (minute == 0 && hour == 0 && day == 0) {
            prettyPrint = "Less than a minute";
        }

        return prettyPrint;
    }

    /**
     * Creates a new congestion object from queue and an initial queuetime
     *
     * @param queue
     * @param queuetime
     * @return
     * @throws Exception
     */
    static private Congestion createCongestion(String queue, long queuetime) throws Exception {
        boolean inTransaction = HibernateUtil.isInTransaction();

        CongestionDao dao = new CongestionDao();

        Congestion congestion = new Congestion();
        congestion.setQueuetime(queuetime);
        congestion.setQueue(queue);

        try {
            dao.save(congestion);
            HibernateUtil.commitTransaction();
            return congestion;
        }
        catch (Throwable t) {
            log.error("Error in createCongestion(), rolling back commit.", t);
            HibernateUtil.rollbackTransaction();
            throw new Exception("Runtime exception creating congestion: " + congestion.getQueue());
        }
    }

    /**
     * Calculates an estimated queuetime for a task based on the just completed queuetime
     * and the previous queuetime data.
     *
     * @param dao
     * @param virtualQueue
     * @param queuetime
     * @return
     */
    static private long calculateQueuetime(CongestionDao dao, String virtualQueue, long queuetime) {
        int count = dao.getQueueCount(virtualQueue);
        if (count <= 1) return 0;
        else return queuetime;
    }

    /**
     * Look up the queue for the specific congestion object, compare to the color thresholds and return the color status
     * @param congestion
     * @return
     */
    static private QueueStatus applyQueueThresholds(Congestion congestion) {
        int jobsWaiting = getJobsWaiting(congestion);

        // Get the thresholds from the config
        GpContext context = GpContext.getServerContext();
        int yellowThreshold = ServerConfigurationFactory.instance().getGPIntegerProperty(context, "congestion.threshold.yellow", 1);
        int redThreshold = ServerConfigurationFactory.instance().getGPIntegerProperty(context, "congestion.threshold.red", 10);

        // Compare to thresholds
        if (jobsWaiting > redThreshold) {
            return QueueStatus.RED;
        }
        else if (jobsWaiting > yellowThreshold) {
            return QueueStatus.YELLOW;
        }
        else {
            return QueueStatus.GREEN;
        }
    }

    /**
     * Return the number of jobs waiting in the virtual queue
     * @param congestion
     * @return
     */
    static private int getJobsWaiting(Congestion congestion) {
        boolean inTransaction = HibernateUtil.isInTransaction();

        CongestionDao dao = new CongestionDao();

        try {
            int waiting = dao.getQueueCount(congestion.getQueue());

            if (!inTransaction) {
                HibernateUtil.commitTransaction();
            }
            return waiting;
        }
        catch (Throwable t) {
            log.error("Error in getJobsWaiting()", t);
            return 0;
        }
    }
}
