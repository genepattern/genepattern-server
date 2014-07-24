package org.genepattern.server.dm.congestion;

import org.apache.log4j.Logger;
import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.Query;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * Created by tabor on 7/15/14.
 */
public class CongestionDao extends BaseDAO {
    private static Logger log = Logger.getLogger(CongestionDao.class);

    /**
     * Get a congestion object from the database, based on the task's lsid
     * @param lsid
     * @return
     */
    public Congestion getCongestion(String lsid) {
        String hql = "from " + Congestion.class.getName() + " cg where cg.lsid = '" + lsid + "'";
        Query query = HibernateUtil.getSession().createQuery(hql);

        List<Congestion> rval = query.list();
        if (rval != null && rval.size() == 1) {
            return rval.get(0);
        }
        return null;
    }

    /**
     * Get the number of jobs currently waiting in the queue
     * @param virtualQueue
     * @return
     */
    public int getVirtualQueueCount(String virtualQueue) {
        String hql = "select count(*) from task_congestion tc, analysis_job aj where tc.virtual_queue = :virtualQueue and tc.lsid = aj.task_lsid and aj.status_id = 1";
        Query query = HibernateUtil.getSession().createSQLQuery(hql);
        query.setString("virtualQueue", virtualQueue);
        query.setReadOnly(true);

        int count = 0;
        Object result = query.uniqueResult();
        if (result instanceof Integer) {
            count = (Integer) result;
        }
        else if (result instanceof BigInteger) {
            count = ((BigInteger) result).intValue();
        }
        else if (result instanceof BigDecimal) {
            try {
                count = ((BigDecimal) result).intValueExact();
            }
            catch (ArithmeticException e) {
                log.error("Invalid conversion from BigDecimal to int", e);
            }
        }
        else {
            log.error("Unknown type returned from query: " + result.getClass().getName());
        }
        return count;
    }

    /**
     * Update the queue time estimate for the given queue
     * @param virtualQueue
     * @param averageQueuetime
     * @return
     */
    public int updateQueuetime(String virtualQueue, long averageQueuetime) {
        String hql = "update task_congestion set queuetime = :averageQueuetime where virtual_queue = :virtualQueue";
        Query query = HibernateUtil.getSession().createSQLQuery(hql);
        query.setString("virtualQueue", virtualQueue);
        query.setLong("averageQueuetime", averageQueuetime);
        query.setReadOnly(true);

        return query.executeUpdate();
    }
}
