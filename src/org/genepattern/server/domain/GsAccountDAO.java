package org.genepattern.server.domain;

import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.Query;

public class GsAccountDAO extends BaseDAO {
    private static Logger log = Logger.getLogger(GsAccountDAO.class);
    
    public GsAccount getByGPUserId(String gpUserid) {
        return (GsAccount) HibernateUtil.getSession().get(GsAccount.class, gpUserid);
    }
    
    public GsAccount getByGSUserId(String gsUserId) {
        Query query = HibernateUtil.getSession().createQuery("from org.genepattern.server.domain.GsAccount where GS_USERID = :gsUserId");
        query.setString("gsUserId", gsUserId);
        List<GsAccount> accountList = query.list();
        
        if (accountList.size() > 1) {
            log.error("DATABASE ERROR: Multiple GP users associated with GS Account: " + gsUserId);
        }
        if (accountList.size() == 0) {
            log.debug("No Accounts found for GS USERID: " + gsUserId);
            return null;
        }
        
        return accountList.get(0);
    }

}
