import java.io.File;

import org.apache.log4j.Logger;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;

/**
 * Test fixture for GreenPepper testing.
 * @author pcarr
 */
public class UserUploadFileFixture {
    public static Logger log = Logger.getLogger(UserUploadFileFixture.class);

    //for test cases
    public String filename = null;
    public String parentDir = null; 
    public String serverPath = null;
    public String url = null;
    public String userId = null;

    public UserUploadFileFixture() {
        this(null, "http://127.0.0.1:8080/gp");
    }
    
    public UserUploadFileFixture(String userId, String genePatternUrl ) {
        this.userId = userId;
        System.setProperty("GenePatternURL", genePatternUrl);
        System.setProperty("user.root.dir", "/Applications/GenePatternServer/users");
    }

    private GpFilePath initUserUploadFile() throws Exception {
        UserAccountManager.validateUsername(userId);
        
        GpFilePath userUploadFile = null;
        ServerConfiguration.Context userContext = ServerConfiguration.Context.getContextForUser(userId);
        
        File uploadFile = new File(parentDir, filename);
        userUploadFile = GpFileObjFactory.getUserUploadFile(userContext, uploadFile);
        //userUploadFile = GpFileObjFactory.getUserUploadFile(userContext, relativePath, filename);
        return userUploadFile;
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

}
