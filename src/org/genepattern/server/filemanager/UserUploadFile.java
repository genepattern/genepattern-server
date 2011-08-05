package org.genepattern.server.filemanager;

import java.io.File;
import java.net.URI;

public class UserUploadFile extends GpFileObj {
    private URI relativeUri;
    private File serverFile;

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
