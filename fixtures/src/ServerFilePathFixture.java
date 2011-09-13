import java.io.File;

import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.serverfile.ServerFileObjFactory;

/**
 * Test fixture for ServerFilePath operations.
 * 
 * @author pcarr
 */
public class ServerFilePathFixture {
    //private String filepath;
    private GpFilePath gpFilePath;
   
    public void setFilepath(String filepath) {
        //this.filepath = filepath;
        System.setProperty("GenePatternURL", "http://127.0.0.1:8080/gp/");
        gpFilePath = ServerFileObjFactory.getServerFile(new File(filepath));
    }
    
    public String getUrl() throws Exception {
        return gpFilePath.getUrl().toString();
    }
    
    public boolean isDir() {
        return gpFilePath.isDirectory();
    }
    
    public String getServerFile() {
        return gpFilePath.getServerFile().getPath();
    }
    
    public String getRelativeUri() {
        return gpFilePath.getRelativeUri().toString();
    }

}
