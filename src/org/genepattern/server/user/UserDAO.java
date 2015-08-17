/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.user;

// Generated Sep 21, 2006 12:36:06 PM by Hibernate Tools 3.1.0.beta5

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateSessionManager;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 * Home object for domain model class User.
 *
 * @see org.genepattern.server.user.User
 * @author Hibernate Tools
 */
public class UserDAO extends BaseDAO {
    /** @deprecated */
    public UserDAO() {
    }
    
    public UserDAO(final HibernateSessionManager mgr) {
        super(mgr);
    }

    public static final Logger log = Logger.getLogger(UserDAO.class);

    public User findById(String id) {
        if (id == null) {
            return null;
        }
        return (User) this.mgr.getSession().get("org.genepattern.server.user.User", id);
    }
    
    public User findByIdIgnoreCase(String id) {
        if (id == null) {
            return null;
        }
        
        Criteria criteria = this.mgr.getSession().createCriteria(User.class);
        Criterion criterion = Restrictions.ilike("userId", id);
        criteria.add(criterion);

        @SuppressWarnings("rawtypes")
        List results = criteria.list();
        if (results != null && results.size() > 0) {
            return (User) results.get(0);
        }
        return null;
    }
    
    public List<User> getAllUsers() {
        @SuppressWarnings("unchecked")
        List<User> users = 
            this.mgr.getSession().createQuery(
                    "from org.genepattern.server.user.User order by userId").list();
        return users;
    }

    public void setProperty(String userId, String key, String value) {
        UserProp userProp = getProperty(userId, key);
        if (userProp != null) {
            userProp.setValue(value);
        }
    }

    public String getPropertyValue(String userId, String key, String defaultValue) {
        UserProp prop = getProperty(userId, key);
        if (prop == null) {
            return defaultValue;
        }
        if (prop.getValue() == null) {
            prop.setValue(defaultValue);
        }
        return prop.getValue();
    }

    public UserProp getProperty(String userId, String key) {
        return getProperty(userId, key, null);
    }

    public UserProp getProperty(String userId, String key, String defaultValue) {
        UserProp rval = null;
        User user = findById(userId);
        if (user == null) {
            log.error("Error in UserDAO.getProperty("+userId+", "+key+"): User not found: "+userId);
        }
        if (user != null) {
            Set<UserProp> userProps = user.getProps();
            for(UserProp prop : userProps) {
                if (key.equals(prop.getKey())) {
                    rval = prop;
                    return prop;
                }
            }
            
            rval = new UserProp();
            rval.setGpUserId(userId);
            rval.setKey(key);
            rval.setValue(defaultValue);
            userProps.add(rval);
        }
        return rval;
    }
    
    public Set<UserProp> getUserProps(String userId) {
        final User user = findById(userId);
        if (user == null) {
            log.error("Error in UserDAO.getProps("+userId+"): User not found: "+userId);
            return Collections.emptySet();
        }
        return user.getProps();
    }
    
    public static String getPropertyValue(Set<UserProp> userProps, String key, String defaultValue) {
        for(UserProp prop : userProps) {
            if (key.equals(prop.getKey())) {
                return prop.getValue();
            }
        }
        return defaultValue;
    }

    /**
     * Get the value of the 'showExecutionLogs' property for the user.
     * @param userId
     * @return false if the value is not set for the given userId
     */
    public boolean getPropertyShowExecutionLogs(String userId) {
        String showExecutionLogsPropValue = getPropertyValue(userId, "showExecutionLogs", String.valueOf(false));
        return Boolean.valueOf( showExecutionLogsPropValue );
    }

}
