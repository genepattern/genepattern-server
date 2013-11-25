package org.genepattern.server.domain;

import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.Query;

public class PinModuleDAO extends BaseDAO {
    private static Logger log = Logger.getLogger(PinModuleDAO.class);
    
    public boolean pinModule(String user, String lsid, double position) {
        modifyPositions(user, position, 1);
        
        PinModule pinned = new PinModule();
        pinned.setUser(user);
        pinned.setLsid(lsid);
        pinned.setPosition(position);
        this.save(pinned);

        return true;
    }
    
    public int modifyPositions(String user, double greaterThanThis, int modification) {
        Query query = HibernateUtil.getSession().createQuery("update org.genepattern.server.domain.PinModule set PIN_POSITION = PIN_POSITION + :mod where USERNAME = :username and PIN_POSITION >= :gtt");
        query.setString("username", user);
        query.setString("gtt", new Double(greaterThanThis).toString());
        query.setString("mod", new Integer(modification).toString());
        return query.executeUpdate();
    }
    
    @SuppressWarnings("unchecked")
    public boolean unpinModule(String user, String lsid) {
        Query query = HibernateUtil.getSession().createQuery("from org.genepattern.server.domain.PinModule where USERNAME = :username and LSID = :lsid");
        query.setString("username", user);
        query.setString("lsid", lsid);
        List<PinModule> deleteList = query.list();
        
        if (deleteList.size() > 1) {
            log.error("Too many modules matching LSID " + lsid + " found for user " + user + " with unpin");
        }
        
        for (PinModule i : deleteList) {
            this.delete(i);
            modifyPositions(user, i.getPosition(), -1);
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
    
    public List<PinModule> getPinsForUser(String user) {
        Query query = HibernateUtil.getSession().createQuery("from org.genepattern.server.domain.PinModule where USERNAME = :username");
        query.setString("username", user);
        return query.list();
    }
}
