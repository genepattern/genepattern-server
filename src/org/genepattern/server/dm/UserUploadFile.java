package org.genepattern.server.dm;

import java.io.File;
import java.net.URI;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;


public class UserUploadFile extends GpFilePath {
    private static Logger log = Logger.getLogger(UserUploadFile.class);

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
        //TODO: experimental, untested implementation
        String formFieldValue = "";
        try {
            formFieldValue = this.getUrl().toExternalForm();
        }
        catch (Exception e) {
            log.error(e);
        }
        return formFieldValue;
    }

    public String getParamInfoValue() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean canRead(boolean isAdmin, Context userContext) {
        if (isAdmin) {
            return true;
        }

        //HACK: special-case for cached data files from external URL
        if (owner.equals( ".cache" )) {
            //TODO: implement access permissions for user data files
            return true;
        }

        if (owner == null || owner.length() == 0) {
            return false;
        }
        
        if (userContext == null) {
            return false;
        }
        
        return owner.equals( userContext.getUserId() );
    }
    
    
}
