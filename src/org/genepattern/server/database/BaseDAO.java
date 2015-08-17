/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.database;

import org.apache.log4j.Logger;

public abstract class BaseDAO {
    private static final Logger log = Logger.getLogger(BaseDAO.class);

    protected final HibernateSessionManager mgr;
    
    /** @deprecated */
    public BaseDAO() {
        this(null);
    }
    
    /**
     * Constructor.  Conditionally starts a transaction,  if a transaction is already underway
     * the call to beginTransaction does nothing.
     *
     */
    public BaseDAO(final HibernateSessionManager mgrIn) {
        if (mgrIn==null) {
            this.mgr=HibernateUtil.instance();
        }
        else {
            this.mgr=mgrIn;
        }
        this.mgr.beginTransaction();
    }

    public void delete(Object persistentInstance) {
        log.debug("deleting  instance");
        try {
            mgr.getSession().delete(persistentInstance);
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
	        return mgr.getSession().save(newObject);
	    }
	    catch (RuntimeException re) {
	        log.error("merge failed", re);
	        throw re;
	    }
	}
	
	public void saveOrUpdate(Object obj) {
	    try {
	        mgr.getSession().saveOrUpdate(obj);
	    }
	    catch (RuntimeException re) {
	        log.error("saveOrUpdate failed", re);
	        throw re;
	    }
	}

}
