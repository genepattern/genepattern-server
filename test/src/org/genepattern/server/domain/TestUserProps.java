/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.domain;

import static org.junit.Assert.*;

import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.user.UserProp;
import org.junit.Before;
import org.junit.Test;

public class TestUserProps {
    private HibernateSessionManager mgr;

    protected void createUser(final HibernateSessionManager mgr, final String userId) {
        final boolean isInTransaction=mgr.isInTransaction();
        mgr.beginTransaction();
        try {
            User user=new User();
            user.setUserId("test_user");
            mgr.getSession().saveOrUpdate(user);
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    protected String getUserProperty(final HibernateSessionManager mgr, final String userId, final String key) {
        final boolean isInTransaction=mgr.isInTransaction();
        mgr.beginTransaction();
        try {
            return new UserDAO(mgr).getPropertyValue(userId, key, null);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    } 
    
    protected void setUserProperty(final HibernateSessionManager mgr, final String userId, final String key, final String value) {
        final boolean isInTransaction=mgr.isInTransaction();
        mgr.beginTransaction();
        try {
            new UserDAO(mgr).setProperty(userId, key, value);
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    @Before
    public void setUp() throws ExecutionException {
        mgr=DbUtil.getTestDbSession();
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
        createUser(mgr, userId);

        mgr.beginTransaction();
        try {
            User user = (User) mgr.getSession().get(User.class.getName(), userId);
            if (user == null) {
                fail("User not found: "+userId);
            }
            Set<UserProp> userProps = user.getProps();
            assertEquals("before add", 0, userProps.size());
        }
        finally {
            mgr.closeCurrentSession();
        }
        
        
        assertEquals("before", null, getUserProperty(mgr, userId, key));
        setUserProperty(mgr, userId, key, value);
        assertEquals("after", value, getUserProperty(mgr, userId, key));
        setUserProperty(mgr, userId, key, "");
        assertEquals("after set to empty string", "", getUserProperty(mgr, userId, key));
        setUserProperty(mgr, userId, key, null);
        assertEquals("after set to null", null, getUserProperty(mgr, userId, key));
    }

}
