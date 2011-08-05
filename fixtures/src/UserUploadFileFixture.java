import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.filemanager.GpFileObj;
import org.genepattern.server.filemanager.GpFileObjFactory;

/**
 * Test fixture for GreenPepper testing.
 * @author pcarr
 */
public class UserUploadFileFixture {
    public static Logger log = Logger.getLogger(UserUploadFileFixture.class);

    //for test cases
    public String filename;
    public String relativePath;
    public String serverPath;
    public String url;
    public String userId;

    public UserUploadFileFixture() {
        this(null, "http://127.0.0.1:8080/gp");
    }
    
    public UserUploadFileFixture(String userId, String genePatternUrl ) {
        this.userId = userId;
        System.setProperty("GenePatternURL", genePatternUrl);
        System.setProperty("user.root.dir", "/Applications/GenePatternServer/users");
    }

    private GpFileObj initUserUploadFile() {
        GpFileObj userUploadFile = null;
        ServerConfiguration.Context userContext = ServerConfiguration.Context.getContextForUser(userId);
        userUploadFile = GpFileObjFactory.getUserUploadFile(userContext, relativePath, filename);
        return userUploadFile;
    }

    public String getUrl() throws Exception {
        GpFileObj uploadFile = initUserUploadFile();
        return uploadFile.getUrl().toExternalForm();
    }
    
    public String getServerPath() {
        GpFileObj uploadFile = initUserUploadFile();
        return uploadFile.getServerFile().getPath();
    }

}
