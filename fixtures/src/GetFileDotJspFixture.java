import org.genepattern.server.dm.GetFileDotJspUtil;
import org.genepattern.server.dm.GpFilePath;

/**
 * Test fixture for creating GpFilePath instances from 'getFile.jsp' urls.
 * @author pcarr
 *
 */
public class GetFileDotJspFixture {
    private GpFilePath gpFilePath = null;
    private Exception exception = null;
    
    public GetFileDotJspFixture() {
        this("http://127.0.0.1:8080/gp/");
    }
    public GetFileDotJspFixture(String genePatternUrl) {
        System.setProperty("GenePatternURL", genePatternUrl);
        System.setProperty("java.io.tmpdir", "/Applications/GenePatternServer/Tomcat/temp");
    }
    
    public void setUrlStr(String urlStr) {
        try {
            this.exception = null;
            //TODO: eventually, just call GpFileObjFactory.getRequestedGpFileObj(urlStr);
            this.gpFilePath = GetFileDotJspUtil.getRequestedGpFileObjFromGetFileDotJsp(urlStr);
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
