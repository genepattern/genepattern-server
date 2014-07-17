package org.genepattern.server.dm.congestion;

import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.Query;

import java.util.List;

/**
 * Created by tabor on 7/15/14.
 */
public class CongestionDao extends BaseDAO {

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
}
