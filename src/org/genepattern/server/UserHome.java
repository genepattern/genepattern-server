package org.genepattern.server;

// Generated Aug 30, 2006 10:48:21 AM by Hibernate Tools 3.1.0.beta5

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.genepattern.server.webservice.server.dao.HibernateUtil;
import org.hibernate.LockMode;
import org.hibernate.Query;
import static org.hibernate.criterion.Example.create;

/**
 * Home object for domain model class UserPassword.
 * 
 * @see org.genepattern.server.UserPassword
 * @author Hibernate Tools
 */
public class UserHome {

    private static final Log log = LogFactory.getLog(UserHome.class);

    public void persist(User transientInstance) {
        log.debug("persisting User instance");
        try {
            HibernateUtil.getSession().persist(transientInstance);
            log.debug("persist successful");
        } catch (RuntimeException re) {
            log.error("persist failed", re);
            throw re;
        }
    }

    public void attachDirty(User instance) {
        log.debug("attaching dirty User instance");
        try {
            HibernateUtil.getSession().saveOrUpdate(instance);
            log.debug("attach successful");
        } catch (RuntimeException re) {
            log.error("attach failed", re);
            throw re;
        }
    }

    public void attachClean(User instance) {
        log.debug("attaching clean User instance");
        try {
            HibernateUtil.getSession().lock(instance, LockMode.NONE);
            log.debug("attach successful");
        } catch (RuntimeException re) {
            log.error("attach failed", re);
            throw re;
        }
    }

    public void delete(User persistentInstance) {
        log.debug("deleting User instance");
        try {
            HibernateUtil.getSession().delete(persistentInstance);
            log.debug("delete successful");
        } catch (RuntimeException re) {
            log.error("delete failed", re);
            throw re;
        }
    }

    public User merge(User detachedInstance) {
        log.debug("merging User instance");
        try {
            User result = (User) HibernateUtil.getSession().merge(detachedInstance);
            log.debug("merge successful");
            return result;
        } catch (RuntimeException re) {
            log.error("merge failed", re);
            throw re;
        }
    }

    public User findById(java.lang.Integer id) {
        log.debug("getting User instance with id: " + id);
        try {
            User instance = (User) HibernateUtil.getSession().get(
                    "org.genepattern.server.User", id);
            if (instance == null) {
                log.debug("get successful, no instance found");
            }
            else {
                log.debug("get successful, instance found");
            }
            return instance;
        } catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }

    public List<User> findByExample(User instance) {
        log.debug("finding User instance by example");
        try {
            List<User> results = (List<User>) HibernateUtil.getSession().createCriteria(
                    "org.genepattern.server.User").add(create(instance)).list();
            log.debug("find by example successful, result size: " + results.size());
            return results;
        } catch (RuntimeException re) {
            log.error("find by example failed", re);
            throw re;
        }
    }

    public User findByUsername(String username) {
        log.debug("getting User instance with username: " + username);
        try {
            Query query = HibernateUtil.getSession().createQuery(
                    "from org.genepattern.server.User where " + " username = :username");
            query.setString("username", username);
            User instance = (User) query.uniqueResult();
            if (instance == null) {
                log.debug("get successful, no instance found");
            }
            else {
                log.debug("get successful, instance found");
            }
            return instance;
        } catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }

    }

    public boolean userPasswordExists(String username, String password) {
        try {
            String hql = "select count(*) from org.genepattern.server.User where "
                    + " username = :username and password = :password";
            Query query = HibernateUtil.getSession().createQuery(hql);
            query.setString("username", username);
            query.setString("password", password);
            Integer count = (Integer) query.uniqueResult();
            return count > 0;
        } catch (RuntimeException e) {
            log.error("userPasswordExists failed", e);
            throw e;
        }

    }
}
