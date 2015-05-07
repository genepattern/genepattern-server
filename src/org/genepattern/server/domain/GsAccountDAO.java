/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.domain;

import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.Query;

public class GsAccountDAO extends BaseDAO {
    private static Logger log = Logger.getLogger(GsAccountDAO.class);
    
    public GsAccount getByGPUserId(String gpUserid) {
        Query query = HibernateUtil.getSession().createQuery("from org.genepattern.server.domain.GsAccount where GP_USERID = :gpUserId");
        query.setString("gpUserId", gpUserid);
        List<GsAccount> accountList = query.list();
        
        if (accountList.size() > 1) {
            log.error("DATABASE ERROR: Multiple GS users associated with GP Account: " + gpUserid);
        }
        if (accountList.size() == 0) {
            log.debug("No Accounts found for GP USERID: " + gpUserid);
            return null;
        }
        
        return accountList.get(0);
        //return (GsAccount) HibernateUtil.getSession().get(GsAccount.class, gpUserid);
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
    
    public boolean deleteExtraGSAssociation(String gsUsername, String gpUsername) {
        Query query = HibernateUtil.getSession().createQuery("from org.genepattern.server.domain.GsAccount where GS_USERID = :gsUsername and GP_USERID != :gpUsername");
        query.setString("gsUsername", gsUsername);
        query.setString("gpUsername", gpUsername);
        List<GsAccount> accountList = query.list();
        
        for (GsAccount i : accountList) {
            this.delete(i);
        }
        
        return accountList.size() > 0;
    }

}
