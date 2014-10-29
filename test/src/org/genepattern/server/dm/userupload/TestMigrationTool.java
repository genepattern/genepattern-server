package org.genepattern.server.dm.userupload;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.dm.userupload.MigrationTool;
import org.genepattern.server.domain.Props;
import org.junit.Assert;
import org.junit.Test;

public class TestMigrationTool {
    
    @Test
    public void checkDbForSyncUploadsComplete() throws Exception {
        DbUtil.initDb();
        boolean check=MigrationTool.checkDbForSyncUserUploadsComplete();
        Assert.assertFalse("before update", check);
        
        boolean success=Props.saveProp("sync.user.uploads.complete", "true");
        if (!success) {
            Assert.fail("Failed to save property to DB");
        }
        Assert.assertTrue("after update", MigrationTool.checkDbForSyncUserUploadsComplete());
    }
    
}
