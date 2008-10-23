/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2008) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

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

}
