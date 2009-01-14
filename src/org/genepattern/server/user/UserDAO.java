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

package org.genepattern.server.user;

// Generated Sep 21, 2006 12:36:06 PM by Hibernate Tools 3.1.0.beta5

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 * Home object for domain model class User.
 *
 * @see org.genepattern.server.user.User
 * @author Hibernate Tools
 */
public class UserDAO extends BaseDAO {

    public static final Logger log = Logger.getLogger(UserDAO.class);

    public User findById(String id) {
        if (id == null) {
            return null;
        }
        return (User) HibernateUtil.getSession().get("org.genepattern.server.user.User", id);
    }
    
    public User findByIdIgnoreCase(String id) {
        if (id == null) {
            return null;
        }
        
        Criteria criteria = HibernateUtil.getSession().createCriteria(User.class);
        Criterion criterion = Restrictions.ilike("userId", id);
        criteria.add(criterion);

        List results = criteria.list();
        if (results != null && results.size() > 0) {
            return (User) results.get(0);
        }
        return null;
    }
    
    public List<User> getAllUsers() {
        List<User> users = 
            HibernateUtil.getSession().createQuery(
                    "from org.genepattern.server.user.User order by userId").list();
        return users;
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
                if (user == null) {
                    throw new NullPointerException(userId + " not found.");
                }
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
