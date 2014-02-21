import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;


/**
 * Test cases for user upload directories.
 * 
 * @author pcarr
 */
public class UserUploadDirFixture {
    public static Logger log = Logger.getLogger(UserUploadDirFixture.class);

    //for test cases
    public String serverPath = null;
    public String url = null;
    public String userId = null;

    public UserUploadDirFixture() {
        this(null, "http://127.0.0.1:8080/gp");
    }
    
    public UserUploadDirFixture(String userId, String genePatternUrl ) {
        this.userId = userId;
        System.setProperty("GenePatternURL", genePatternUrl);
        System.setProperty("user.root.dir", "/Applications/GenePatternServer/users");
    }

    private GpFilePath initUserUploadFile() throws Exception {
        //UserAccountManager.validateUsername(userId); 
        GpContext userContext = GpContext.getContextForUser(userId);
        return GpFileObjFactory.getUserUploadDir(userContext);
    }

    public String getUrl() throws Exception {
        GpFilePath uploadFile = initUserUploadFile();
        return uploadFile.getUrl().toExternalForm();
    }
    
    public String getServerFile() throws Exception {
        GpFilePath uploadFile = initUserUploadFile();
        return uploadFile.getServerFile().getPath();
    }
    
    public String getRelativeFile() throws Exception {
        GpFilePath uploadFile = initUserUploadFile();
        return uploadFile.getRelativeFile().getPath();
    }
    
    public boolean isDirectory() throws Exception {
        GpFilePath uploadFile = initUserUploadFile();
        return uploadFile.isDirectory();
    }

}
