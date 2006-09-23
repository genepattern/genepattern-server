package org.genepattern.server.user;

// Generated Sep 21, 2006 12:36:06 PM by Hibernate Tools 3.1.0.beta5

import java.util.List;
import javax.naming.InitialContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.genepattern.server.database.AbstractHome;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Example;

/**
 * Home object for domain model class User.
 * @see org.genepattern.server.user.User
 * @author Hibernate Tools
 */
public class UserHome extends AbstractHome {

    private static final Logger log = Logger.getLogger(UserHome.class);

    public User merge(User detachedInstance) {
        log.debug("merging Props instance");
        try {
            return (User) HibernateUtil.getSession().merge(detachedInstance);
        }
        catch (RuntimeException re) {
            log.error("merge failed", re);
            throw re;
        }
    }

    public User findById(String id) {
        log.debug("getting Props instance with id: " + id);
        try {
            return (User) HibernateUtil.getSession().get("org.genepattern.server.user.User", id);
        }
        catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }

    public List<User> findByExample(User instance) {
        log.debug("finding User instance by example");
        try {
            return HibernateUtil.getSession().createCriteria("org.genepattern.server.user.User").add(
                    Example.create(instance)).list();
        }
        catch (RuntimeException re) {
            log.error("find by example failed", re);
            throw re;
        }
    }

}
