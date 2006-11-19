package org.genepattern.server.user;

// Generated Sep 21, 2006 12:36:06 PM by Hibernate Tools 3.1.0.beta5

import java.util.List;
import javax.naming.InitialContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Example;

/**
 * Home object for domain model class User.
 * @see org.genepattern.server.user.User
 * @author Hibernate Tools
 */
public class UserDAO extends BaseDAO {

    public static final Logger log = Logger.getLogger(UserDAO.class);

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

 

}
