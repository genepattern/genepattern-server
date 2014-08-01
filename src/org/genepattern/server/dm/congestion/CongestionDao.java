package org.genepattern.server.dm.congestion;

import org.apache.log4j.Logger;
import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.Query;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * @author Thorin Tabor
 */
public class CongestionDao extends BaseDAO {
    private static Logger log = Logger.getLogger(CongestionDao.class);

    /**
     * Get a congestion object from the database, based on the queue name
     * @param queue
     * @return
     */
    public Congestion getCongestion(String queue) {
        String hql = "from " + Congestion.class.getName() + " cg where cg.queue = '" + queue + "'";
        Query query = HibernateUtil.getSession().createQuery(hql);

        List<Congestion> rval = query.list();
        if (rval != null && rval.size() == 1) {
            return rval.get(0);
        }
        return null;
    }

    /**
     * Get the number of jobs currently waiting in the queue
     * @param queue
     * @return
     */
    public int getQueueCount(String queue) {
        // Handle special case for nulls
        String selectCase = null;
        if ("".equals(queue)) { selectCase = "is null"; }
        else { selectCase = "= :queue"; }

        String hql = "select count(*) from job_runner_job jrj where jrj.queue_id " + selectCase + " and jrj.start_time is null ";
        Query query = HibernateUtil.getSession().createSQLQuery(hql);
        if (!"".equals(queue)) query.setString("queue", queue);
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
}
