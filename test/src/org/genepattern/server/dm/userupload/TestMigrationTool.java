package org.genepattern.server.dm.userupload;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.dm.userupload.MigrationTool;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class TestMigrationTool {
    
//    protected HibernateSessionManager initSessionMySql() {
//        return new HibernateSessionManager("hibernate.mysql.cfg.xml", "jdbc:mysql://127.0.0.1:3306/genepattern");
//    }
//    
//    protected HibernateSessionManager initSessionHsql() throws Exception {
//        DbUtil.initDb();
//        return new HibernateSessionManager("hibernate.junit.cfg.xml", "jdbc:hsqldb:hsql://127.0.0.1:9001/xdb");
//    }

    @Ignore @Test
    public void dbQueryHql() throws Exception {
        //initSessionHsql();
        DbUtil.initDb();
        boolean check=MigrationTool.checkDbForSyncUserUploadsComplete();
        Assert.assertFalse(check);
    }
    
    @Test
    public void dbQueryMySql() {
        System.setProperty("hibernate.configuration.file", "hibernate.mysql.cfg.xml");
        boolean check=MigrationTool.checkDbForSyncUserUploadsComplete();
        Assert.assertFalse(check);
        
    }
}
