package org.genepattern.server.domain;

import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;

public class GsAccountDAO extends BaseDAO {
    
    public GsAccount getGsAccount(String gpUserid) {
        return (GsAccount) HibernateUtil.getSession().get(GsAccount.class, gpUserid);
    }

}
