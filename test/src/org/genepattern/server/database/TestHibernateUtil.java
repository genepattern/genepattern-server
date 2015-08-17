/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.database;

import static org.junit.Assert.*;

import java.util.concurrent.ExecutionException;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestHibernateUtil {
    private HibernateSessionManager mgr;
    private GpConfig gpConfig;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();
    
    @Before
    public void setUp() throws Exception {
        mgr=DbUtil.getTestDbSession();
        final String userDir=temp.newFolder("users").getAbsolutePath();
        gpConfig=new GpConfig.Builder()
            .addProperty(GpConfig.PROP_USER_ROOT_DIR, userDir)
        .build();
    }
    
    @Test
    public void isInTransaction() throws ExecutionException {
        assertFalse(mgr.isInTransaction());
    }
    
    @Test
    public void mappingFromHbmXmlFile_User() throws DbException {
        String gp_username="test_user";
        DbUtil.addUserToDb(gpConfig, mgr, gp_username);
    }
}
