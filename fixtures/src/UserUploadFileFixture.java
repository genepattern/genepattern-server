import java.io.File;
import java.net.URL;

import org.apache.log4j.Logger;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.config.GpContext;
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
    
    private GpFilePath userUploadFile = null;

    public UserUploadFileFixture() {
        this(null, "http://127.0.0.1:8080/gp");
    }
    
    public UserUploadFileFixture(String userId, String genePatternUrl) {
        this.userId = userId;
        System.setProperty("GenePatternURL", genePatternUrl);
        System.setProperty("user.root.dir", "/Applications/GenePatternServer/users");
    }
    
    public void setUrlStr(String urlStr) throws Exception {
        this.userUploadFile = GpFileObjFactory.getRequestedGpFileObj(urlStr);
    }

    private GpFilePath initUserUploadFile() throws Exception {
        UserAccountManager.validateUsername(userId);
        
        GpFilePath userUploadFile = null;
        GpContext userContext = GpContext.getContextForUser(userId);
        
        File uploadFile = new File(parentDir, filename);
        userUploadFile = GpFileObjFactory.getUserUploadFile(userContext, uploadFile);
        //userUploadFile = GpFileObjFactory.getUserUploadFile(userContext, relativePath, filename);
        return userUploadFile;
    }

    public String getOwner() throws Exception {
        GpFilePath uploadFile = initUserUploadFile();
        return uploadFile.getOwner();
    }
    
    public String getUrl() throws Exception {
        GpFilePath uploadFile = initUserUploadFile();
        URL url = uploadFile.getUrl();
        String urlStr = url.toString();
        String urlExt = url.toExternalForm();
        return urlStr;
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
