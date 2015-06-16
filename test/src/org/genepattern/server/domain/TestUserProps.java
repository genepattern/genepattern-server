/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.domain;

import static org.junit.Assert.*;

import java.util.Set;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.user.UserProp;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestUserProps {
    @BeforeClass
    public static void beforeClass() throws Exception {
        DbUtil.initDb();
    }

    protected void createUser(final String userId) {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        HibernateUtil.beginTransaction();
        try {
            User user=new User();
            user.setUserId("test_user");
            HibernateUtil.getSession().saveOrUpdate(user);
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    protected String getUserProperty(String userId, String key) {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        HibernateUtil.beginTransaction();
        try {
            return new UserDAO().getPropertyValue(userId, key, null);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    } 
    
    protected void setUserProperty(String userId, String key, String value) {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        HibernateUtil.beginTransaction();
        try {
            new UserDAO().setProperty(userId, key, value);
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    /**
     * CRUD tests for the 'GP_USER_PROP' table
     * @throws Exception
     */
    @Test
    public void createUpdateDeleteGpUserProp() throws Exception {
        final String userId="test_user";
        final String key="theKey";
        final String value="theValue";
        
        // first create a user
        createUser(userId);

        HibernateUtil.beginTransaction();
        try {
            User user = (User) HibernateUtil.getSession().get(User.class.getName(), userId);
            if (user == null) {
                fail("User not found: "+userId);
            }
            Set<UserProp> userProps = user.getProps();
            assertEquals("before add", 0, userProps.size());
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
        
        
        assertEquals("before", null, getUserProperty(userId, key));
        setUserProperty(userId, key, value);
        assertEquals("after", value, getUserProperty(userId, key));
        setUserProperty(userId, key, "");
        assertEquals("after set to empty string", "", getUserProperty(userId, key));
        setUserProperty(userId, key, null);
        assertEquals("after set to null", null, getUserProperty(userId, key));
        
    }


}
