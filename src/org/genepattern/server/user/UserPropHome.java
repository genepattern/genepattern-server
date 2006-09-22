package org.genepattern.server.user;

// Generated Sep 21, 2006 12:36:06 PM by Hibernate Tools 3.1.0.beta5

import java.util.List;
import javax.naming.InitialContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.genepattern.server.util.HibernateUtil;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Example;

/**
 * Home object for domain model class User.
 * @see org.genepattern.server.user.User
 * @author Hibernate Tools
 */
public class UserPropHome {

	private static final Log log = LogFactory.getLog(UserPropHome.class);
	/** auto generated
	 * @es_generated
	 */


	public void persist(UserProp transientInstance) {
		log.debug("persisting UserProp instance");
		try {
			HibernateUtil.getSession().persist(transientInstance);
			log.debug("persist successful");
		} catch (RuntimeException re) {
			log.error("persist failed", re);
			throw re;
		}
	}

	public void attachDirty(UserProp instance) {
		log.debug("attaching dirty UserProp instance");
		try {
			HibernateUtil.getSession().saveOrUpdate(instance);
			log.debug("attach successful");
		} catch (RuntimeException re) {
			log.error("attach failed", re);
			throw re;
		}
	}

	public void attachClean(UserProp instance) {
		log.debug("attaching clean UserProp instance");
		try {
			HibernateUtil.getSession().lock(instance, LockMode.NONE);
			log.debug("attach successful");
		} catch (RuntimeException re) {
			log.error("attach failed", re);
			throw re;
		}
	}

	public void delete(UserProp persistentInstance) {
		log.debug("deleting UserProp instance");
		try {
			HibernateUtil.getSession().delete(persistentInstance);
			log.debug("delete successful");
		} catch (RuntimeException re) {
			log.error("delete failed", re);
			throw re;
		}
	}

	public UserProp merge(UserProp detachedInstance) {
		log.debug("merging UserProp instance");
		try {
			UserProp result = (UserProp) HibernateUtil.getSession().merge(
					detachedInstance);
			log.debug("merge successful");
			return result;
		} catch (RuntimeException re) {
			log.error("merge failed", re);
			throw re;
		}
	}

	public UserProp findById(java.lang.String id) {
		log.debug("getting UserProp instance with id: " + id);
		try {
			UserProp instance = (UserProp) HibernateUtil.getSession().get(
					"org.genepattern.server.UserProp.UserProp", id);
			if (instance == null) {
				log.debug("get successful, no instance found");
			} else {
				log.debug("get successful, instance found");
			}
			return instance;
		} catch (RuntimeException re) {
			log.error("get failed", re);
			throw re;
		}
	}

	public List findByExample(UserProp instance) {
		log.debug("finding UserProp instance by example");
		try {
			List results = HibernateUtil.getSession().createCriteria(
					"org.genepattern.server.UserProp.UserProp").add(
					Example.create(instance)).list();
			log.debug("find by example successful, result size: "
					+ results.size());
			return results;
		} catch (RuntimeException re) {
			log.error("find by example failed", re);
			throw re;
		}
	}
    

}
