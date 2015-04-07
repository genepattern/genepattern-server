package org.genepattern.server.database;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.genepattern.junitutil.FileUtil;
import org.hsqldb.Database;
import org.hsqldb.DatabaseManager;
import org.hsqldb.DatabaseURL;
import org.hsqldb.lib.HsqlArrayList;
import org.hsqldb.persist.HsqlProperties;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases added for updating from HSQL 1.8 to 2.3
 * @author pcarr
 *
 */
public class TestHsqlDbUpdate {    
    private final String dbDirPath="hsqldb/db_1.8_02/";
    private File dbDir;
    private File dbPath;
    private String url;
    private String[] args;
    HsqlProperties props;

    @Before
    public void setUp() {
        dbDir=new File(FileUtil.getDataDir(), dbDirPath);
        dbPath=new File(dbDir, "GenePatternDB");
        url="file:"+(new File(dbDir, "GenePatternDB").getAbsolutePath());
        args=new String[]{
                "--trace", "true", "-port", "9001", "-database.0", url, "-dbname.0", "xdb", "-no_system_exit", "true"};
        props = HsqlProperties.argArrayToProps(args, "server"); // ServerProperties.sc_key_prefix
    }
    
    @Test
    public void updateGenePatternDBscriptFrom_1_8() throws FileNotFoundException, IOException {
        FileInputStream fis=null;
        try {
            File propsFile=new File(dbDir, "GenePatternDB.properties");
            Properties props=new Properties();
            props.load(new FileInputStream(propsFile));
            // need this to be read-only so that the test does not edit the original file
            assertEquals("Incorrect readonly flag in "+propsFile, Boolean.TRUE, Boolean.valueOf(props.getProperty("readonly")));
        } 
        finally {
            if (fis!=null) {
                fis.close();
            }
        }
        
        String type=DatabaseURL.S_FILE;
        try {
            Database db=DatabaseManager.getDatabase(type, dbPath.getAbsolutePath(), props);
            HsqlArrayList tables=db.schemaManager.getAllTables(false);
            assertEquals("Expecting 39 tables", 39, tables.size());
        }
        catch (Throwable t) {
            throw t;
        }
    }
}
