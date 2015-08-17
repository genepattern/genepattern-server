/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.dm.userupload;

import static org.junit.Assert.*;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.dm.userupload.MigrationTool;
import org.genepattern.server.domain.PropsTable;
import org.junit.Assert;
import org.junit.Test;

public class TestMigrationTool {
    
    @Test
    public void checkDbForSyncUploadsComplete() throws Exception {
        HibernateSessionManager mgr=DbUtil.getTestDbSession();
        
        boolean check=MigrationTool.checkDbForSyncUserUploadsComplete(mgr);
        assertFalse("before update", check);
        
        boolean success=PropsTable.saveProp(mgr, "sync.user.uploads.complete", "true");
        if (!success) {
            Assert.fail("Failed to save property to DB");
        }
        assertTrue("after update", MigrationTool.checkDbForSyncUserUploadsComplete(mgr));
    }
    
}
