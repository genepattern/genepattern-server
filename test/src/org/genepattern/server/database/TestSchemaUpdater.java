package org.genepattern.server.database;

import java.io.File;
import java.util.Properties;

import org.genepattern.junitutil.ConfigUtil;
import org.genepattern.junitutil.DbUtil;
//import org.junit.Rule;
import org.junit.Test;
//import org.junit.rules.TemporaryFolder;

/**
 * Unit tests for the SchemaUpdater class.
 * @author pcarr
 *
 */
public class TestSchemaUpdater {

    //@Rule
    //public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void initDbSchemaHsqlDb() throws Throwable {
        final File workingDir=new File(System.getProperty("user.dir"));
        final File resourcesDir=new File(workingDir, "resources");

        Properties p=new Properties();
        ConfigUtil.loadPropertiesInto(p, new File(resourcesDir, "database_default.properties"));

        // path to load the schema files (e.g. ./resources/analysis_hypersonic-1.3.1.sql)
        File schemaDir=resourcesDir;
        // path to load the hsql db files (e.g. ./resources/GenePatternDB.script and ./resources/GenePatternDB.properties)
        final File hsqlDbDir=new File("junitdb");
        final String hsqlDbName="GenePatternDB";

        DbUtil.startDb(hsqlDbDir, hsqlDbName);
        try {
            p.setProperty("database.vendor", "HSQL");
            p.setProperty("hibernate.connection.url", "jdbc:hsqldb:hsql://127.0.0.1:9001/xdb");
            HibernateSessionManager mgr=new HibernateSessionManager(p);
            SchemaUpdater.updateSchema(mgr, schemaDir, "analysis_hypersonic-", "3.9.2");
        }
        finally {
            DbUtil.shutdownDb();
        } 
    }

}
