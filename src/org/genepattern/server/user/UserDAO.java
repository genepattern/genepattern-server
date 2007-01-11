package org.genepattern.server.user;

// Generated Sep 21, 2006 12:36:06 PM by Hibernate Tools 3.1.0.beta5

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.Query;

/**
 * Home object for domain model class User.
 * 
 * @see org.genepattern.server.user.User
 * @author Hibernate Tools
 */
public class UserDAO extends BaseDAO {

    public static final Logger log = Logger.getLogger(UserDAO.class);

    public User findById(String id) {
        log.debug("getting Props instance with id: " + id);
        try {
            return (User) HibernateUtil.getSession().get("org.genepattern.server.user.User", id);
        } catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }

    public void setProperty(String userId, String key, String value) {
        getProperty(userId, key).setValue(value);
    }

    public String getPropertyValue(String userId, String key, String defaultValue) {
        UserProp prop = getProperty(userId, key);
        if (prop.getValue() == null) {
            prop.setValue(defaultValue);
        }
        return prop.getValue();
    }

    public UserProp getProperty(String userId, String key) {
        return getProperty(userId, key, null);
    }

    public UserProp getProperty(String userId, String key, String defaultValue) {
        try {
            Query query = HibernateUtil.getSession().createQuery(
                    "from org.genepattern.server.user.UserProp where gpUserId = :gpUserId AND key = :key");
            query.setString("key", key);
            query.setString("gpUserId", userId);
            UserProp prop = (UserProp) query.uniqueResult();
            if (prop == null) {
                prop = new UserProp();
                prop.setKey(key);
                prop.setValue(defaultValue);
                prop.setGpUserId(userId);
                User user = findById(userId);
                List<UserProp> props = user.getProps();
                if (props == null) {
                    props = new ArrayList<UserProp>();
                    user.setProps(props);
                }
                props.add(prop);
            }
            return prop;
        } catch (RuntimeException re) {
            log.error("find by example failed", re);
            throw re;
        }

    }

}
