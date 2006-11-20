package org.genepattern.server.database;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.Lsid;
import org.hibernate.LockMode;

public abstract class BaseDAO {
    
    Logger log = Logger.getLogger(BaseDAO.class);

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
