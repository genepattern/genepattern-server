package org.genepattern.server.domain;

import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.Query;

public class PinModuleDAO extends BaseDAO {
    private static Logger log = Logger.getLogger(PinModuleDAO.class);
    
    public boolean pinModule(String user, String lsid, double position) {
        PinModule pinned = new PinModule();
        pinned.setUser(user);
        pinned.setLsid(lsid);
        pinned.setPosition(position);
        this.save(pinned);

        return incrementPosition(user, position) != 0;
    }
    
    public int incrementPosition(String user, double greaterThanThis) {
        Query query = HibernateUtil.getSession().createQuery("update org.genepattern.server.domain.PinModule set INDEX = INDEX + 1 where USER = :username and INDEX > :gtt");
        query.setString("username", user);
        query.setString("gtt", new Double(greaterThanThis).toString());
        return query.executeUpdate();
    }
    
    @SuppressWarnings("unchecked")
    public boolean unpinModule(String user, String lsid) {
        Query query = HibernateUtil.getSession().createQuery("from org.genepattern.server.domain.PinModule where USER = :username and LSID = :lsid");
        query.setString("username", user);
        query.setString("lsid", lsid);
        List<PinModule> deleteList = query.list();
        
        for (PinModule i : deleteList) {
            this.delete(i);
        }
        
        return deleteList.size() > 0;
    }
    
    public boolean repinModule(String user, String lsid, double position) {
        boolean goodSoFar = unpinModule(user, lsid);
        if (goodSoFar) {
            goodSoFar = pinModule(user, lsid, position);
            if (!goodSoFar) {
                log.error("ERROR PINNING: " + user + " " + lsid);
            }
        }
        else {
            log.error("ERROR UNPINNING: " + user + " " + lsid);
        }

        return goodSoFar;
    }

}
