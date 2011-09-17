import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;

/**
 * Test fixture for the GpFilePathFactory class.
 * @author pcarr
 *
 */
public class GpFilePathFactoryFixture {
    private GpFilePath gpFilePath = null;
    private Exception exception = null;
    
    public GpFilePathFactoryFixture() {
        this("http://127.0.0.1:8080/gp/");
    }
    public GpFilePathFactoryFixture(String genePatternUrl) {
        System.setProperty("GenePatternURL", genePatternUrl);
        System.setProperty("user.root.dir", "/Applications/GenePatternServer/users");
    }
    
    public void setUrlStr(String urlStr) {
        try {
            this.exception = null;
            this.gpFilePath = GpFileObjFactory.getRequestedGpFileObj(urlStr);
        }
        catch (Exception e) {
            this.exception = e;
        }
    }
    
    public String getOwner() throws Exception {
        if (exception != null) throw exception;
        return gpFilePath.getOwner();
    }
    
    public String getUrl() throws Exception {
        if (exception != null) throw exception;
        return gpFilePath.getUrl().toString();
    }
    
    public String getServerFile() throws Exception {
        if (exception != null) throw exception;
        return gpFilePath.getServerFile().getPath();
    }
    
    public String getRelativeFile() throws Exception {
        if (exception != null) throw exception;
        return gpFilePath.getRelativeFile().getPath();
    }

}
