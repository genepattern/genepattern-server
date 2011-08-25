package org.genepattern.server.dm;

import java.io.File;
import java.net.URI;


public class UserUploadFile extends GpFilePath {
    private URI relativeUri;
    private File serverFile;
    private File relativeFile;

    public UserUploadFile(URI relativeUri) {
        this.relativeUri = relativeUri;
    }
    
    public URI getRelativeUri() {
        return this.relativeUri;
    }

    public File getServerFile() {
        return this.serverFile;
    }
    void setServerFile(File file) {
        this.serverFile = file;
    }
    
    public File getRelativeFile() {
        return this.relativeFile;
    }
    void setRelativeFile(File file) {
        this.relativeFile = file;
    }

    public String getFormFieldValue() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getParamInfoValue() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getTasklibValue() {
        // TODO Auto-generated method stub
        return null;
    }
    
    
}
