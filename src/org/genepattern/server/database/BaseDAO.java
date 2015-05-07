/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.database;

import org.apache.log4j.Logger;

public abstract class BaseDAO {
    
    Logger log = Logger.getLogger(BaseDAO.class);
    
    /*
     * Constructor.  Conditionally starts a transaction,  if a transaction is already underway
     * the call to beginTransaction does nothing.
     *
     */
    public BaseDAO() {
    	HibernateUtil.beginTransaction();
    }

    public void delete(Object persistentInstance) {
        log.debug("deleting  instance");
        try {
            HibernateUtil.getSession().delete(persistentInstance);
            log.debug("delete successful");
        }
        catch (RuntimeException re) {
            log.error("delete failed", re);
            throw re;
        }
    }


	public Object save(Object newObject) {
	    log.debug("merging Props instance");
	    try {
	        return HibernateUtil.getSession().save(newObject);
	    }
	    catch (RuntimeException re) {
	        log.error("merge failed", re);
	        throw re;
	    }
	}
	
	public void saveOrUpdate(Object obj) {
	    try {
	        HibernateUtil.getSession().saveOrUpdate(obj);
	    }
	    catch (RuntimeException re) {
	        log.error("saveOrUpdate failed", re);
	        throw re;
	    }
	}

}
