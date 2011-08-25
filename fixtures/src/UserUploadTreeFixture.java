import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.database.HsqlDbUtil;
import org.genepattern.server.dm.GpDirectory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.userupload.UserUploadManager;
import org.genepattern.server.dm.userupload.dao.UserUpload;


public class UserUploadTreeFixture {
    public static Logger log = Logger.getLogger(UserUploadTreeFixture.class);

    public String userId = "test";
    
    private static boolean isDbInitialized = false;
    private static void init() {
        //some of the classes being tested require a Hibernate Session connected to a GP DB
        if (!isDbInitialized) {
            //TODO: use DbUnit to improve Hibernate and DB configuration for the unit tests 
            System.setProperty("hibernate.configuration.file", "hibernate.junit.cfg.xml");
            
            //String args = System.getProperty("HSQL.args", " -port 9001  -database.0 file:../resources/GenePatternDB -dbname.0 xdb");
            System.setProperty("HSQL.args", " -port 9001  -database.0 file:testdb/GenePatternDB -dbname.0 xdb");
            System.setProperty("hibernate.connection.url", "jdbc:hsqldb:hsql://127.0.0.1:9001/xdb");
            System.setProperty("GenePatternVersion", "3.3.3");

            File resourceDir = new File("resources");
            String pathToResourceDir = resourceDir.getAbsolutePath();
            System.setProperty("genepattern.properties", pathToResourceDir);
            System.setProperty("resources", pathToResourceDir);

            try {
                isDbInitialized = true;
                HsqlDbUtil.startDatabase();
            }
            catch (Throwable t) {
                //the unit tests can pass even if db initialization fails, so ...
                // ... try commenting this out if it gives you problems
                //throw new Exception("Error initializing test database", t);
                log.error("Error initializing test database", t);
            }
        }
        
        try {
            //TODO: create table rows from specification (.html doc) rather than hard coded here in the fixture
            UserUpload uu = createUploadFile("test", "all_aml_test.gct", 1);
            uu = createUploadFile("test", "z.txt", 1);
            uu = createUploadFile("test", "sub/file_01.txt", 1);
            uu = createUploadFile("test", "sub/file_02.txt", 1);
            uu = createUploadFile("test", "sub/a/file_03.txt", 1);
            uu = createUploadFile("test", "sub/a/file_04.txt", 1);
            uu = createUploadFile("test", "sub/g/01/file_05.txt", 1);
            uu = createUploadFile("test", "sub/g/01/file_06.txt", 1);
        }
        catch (Throwable t) {
            log.error("error creating upload file: "+t.getLocalizedMessage(), t);
        }
    }
    
    static private UserUpload createUploadFile(String userId, String relativePath, int numParts) throws Exception {
        ServerConfiguration.Context userContext = ServerConfiguration.Context.getContextForUser(userId);
        GpFilePath gpFileObj = UserUploadManager.getUploadFileObj(userContext, new File(relativePath));
        UserUpload uu = UserUploadManager.createUploadFile(userContext, gpFileObj, numParts);
        return uu;
    }
    
    /**
     * get the list of all upload files for the given user, each element in the list is a relative path name.
     */
    public List<GpFilePath> query() throws Exception {
        try {
            init();
            ServerConfiguration.Context userContext = ServerConfiguration.Context.getContextForUser(userId);
            GpDirectory rootDir = null;
            try {
                rootDir = UserUploadManager.getFileTree(userContext);
            }
            catch (Throwable t) {
                log.error("Error getting fileTree for user: "+userId, t);
                return Collections.emptyList();
            }
            List<GpFilePath> rval = null;
            try {
                rval = rootDir.getAllFilePaths();
            }
            catch (Throwable t) {
                rval = Collections.emptyList();
            }
            return rval;
        } 
        finally {
            try {
                HsqlDbUtil.shutdownDatabase();
                isDbInitialized = false;
            }
            catch (Throwable t) {
                log.error("Error shutting down hsqldb: "+t.getLocalizedMessage(), t);
            }
        }
    }

}
