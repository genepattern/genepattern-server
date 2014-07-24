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
     * @param lsid
     * @return
     */
    static public QueueStatus getQueueStatus(String lsid) {
        boolean inTransaction = HibernateUtil.isInTransaction();

        CongestionDao dao = new CongestionDao();

        try {
            Congestion congestion = dao.getCongestion(lsid);

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
     * @param lsid
     * @return
     */
    static public Congestion getCongestion(String lsid) {
        CongestionDao dao = new CongestionDao();
        return dao.getCongestion(lsid);
    }

    /**
     * Update the congestion data for a particular task
     * Use when you don't already have a congestion object
     *
     * Explicitly updates runtime, lazily updates virtual queue.
     *
     * @param lsid
     * @param runtime
     * @return
     * @throws Exception
     */
    static public Congestion updateCongestion(String lsid, long runtime, long queuetime) throws Exception {
        boolean inTransaction = HibernateUtil.isInTransaction();

        CongestionDao dao = new CongestionDao();

        try {
            Congestion congestion = dao.getCongestion(lsid);

            if (!inTransaction) {
                HibernateUtil.commitTransaction();
            }

            if (congestion == null) {
                return createCongestion(lsid, runtime, queuetime);
            }

            return updateCongestion(congestion, runtime, queuetime);
        }
        catch (Throwable t) {
            log.error("Error in updateCongestion(), rolling back commit.", t);
            HibernateUtil.rollbackTransaction();
            throw new Exception("Error updating congestion: " + lsid);
        }
    }

    /**
     * Update the congestion data for a particular task
     * Use when you have a congestion object
     *
     * Explicitly updates runtime, lazily updates virtual queue.
     *
     * @param congestion
     * @param runtime
     * @return
     * @throws Exception
     */
    static public Congestion updateCongestion(Congestion congestion, long runtime, long queuetime) throws Exception {
        boolean inTransaction = HibernateUtil.isInTransaction();

        CongestionDao dao = new CongestionDao();

        long averageRuntime = calculateRuntime(congestion, runtime);
        congestion.setRuntime(averageRuntime);

        long averageQueuetime = calculateQueuetime(congestion, queuetime);
        congestion.setQueuetime(averageQueuetime);

        String virtualQueue = getVirtualQueue(congestion.getLsid());
        congestion.setVirtualQueue(virtualQueue);

        try {
            dao.saveOrUpdate(congestion);
            HibernateUtil.commitTransaction();
            return congestion;
        }
        catch (Throwable t) {
            log.error("Error in updateCongestion(), rolling back commit.", t);
            HibernateUtil.rollbackTransaction();
            throw new Exception("Runtime exception updating congestion: " + congestion.getLsid());
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
            prettyPrint += day + (day == 1 ? "day" : "days") + " ";
        }
        if (hour > 0 || day > 0) {
            prettyPrint += hour + (hour == 1 ? "hour" : "hours") + " ";
        }
        if (minute > 0 || hour > 0 || day > 0) {
            prettyPrint += minute + (minute == 1 ? "minute" : "minutes") + " ";
        }

        if (minute == 0 && hour == 0 && day == 0) {
            prettyPrint = "Less than a minute";
        }

        return prettyPrint;
    }

    /**
     * Creates a new congestion object from a task lsid and an initial runtime
     *
     * @param lsid
     * @param runtime
     * @return
     * @throws Exception
     */
    static private Congestion createCongestion(String lsid, long runtime, long queuetime) throws Exception {
        boolean inTransaction = HibernateUtil.isInTransaction();

        CongestionDao dao = new CongestionDao();
        String virtualQueue = getVirtualQueue(lsid);

        Congestion congestion = new Congestion();
        congestion.setLsid(lsid);
        congestion.setRuntime(runtime);
        congestion.setQueuetime(queuetime);
        congestion.setVirtualQueue(virtualQueue);

        try {
            dao.save(congestion);
            HibernateUtil.commitTransaction();
            return congestion;
        }
        catch (Throwable t) {
            log.error("Error in createCongestion(), rolling back commit.", t);
            HibernateUtil.rollbackTransaction();
            throw new Exception("Runtime exception creating congestion: " + congestion.getLsid());
        }
    }

    /**
     * Returns the appropriate virtual queue string for a given task lsid
     *
     * @param lsid
     * @return
     */
    static private String getVirtualQueue(String lsid) {
        GpContext context = GpContext.getContextForTask(lsid);
        return ServerConfigurationFactory.instance().getGPProperty(context, "queue.name", "");
    }

    /**
     * Calculates an estimated runtime for a task based on the just completed runtime
     * and the previous runtime data.
     *
     * Formula:
     * Averages the current runtime with the previous average, weighting the previous average as configured.
     *
     * @param congestion
     * @param runtime
     * @return
     */
    static private long calculateRuntime(Congestion congestion, long runtime) {
        GpContext context = GpContext.getServerContext();
        int weight = ServerConfigurationFactory.instance().getGPIntegerProperty(context, "congestion.compare.weight", 3);

        /*
            Pretend the last WEIGHT jobs took the current average amount of time to complete,
            then average with the runtime of the current completed job.
         */
        return (congestion.getRuntime() * weight + runtime) / (weight + 1);
    }

    /**
     * Calculates an estimated queuetime for a task based on the just completed queuetime
     * and the previous queuetime data.
     *
     * Formula:
     * Averages the current queuetime with the previous average, weighting the previous average as configured.
     *
     * @param congestion
     * @param queuetime
     * @return
     */
    static private long calculateQueuetime(Congestion congestion, long queuetime) {
        GpContext context = GpContext.getServerContext();
        int weight = ServerConfigurationFactory.instance().getGPIntegerProperty(context, "congestion.compare.weight", 3);

        /*
            Pretend the last WEIGHT jobs took the current average amount of time to complete,
            then average with the runtime of the current completed job.
         */
        return (congestion.getQueuetime() * weight + queuetime) / (weight + 1);
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
            int waiting = dao.getVirtualQueueCount(congestion.getVirtualQueue());

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
