package org.genepattern.server.domain;

import java.io.File;
import java.io.IOException;
import java.util.Date;


/**
 * Hibernate mapping class to the UPLOAD_FILE table.
 * @author pcarr
 */
public class UploadFile {
    private String path;
    private String userId;
    private String name;
    private String extension;
    private String kind;
    private long fileLength;
    private Date lastModified;
    
    /**
     * Reset the properties based on the given file.
     * @param file
     */
    public void initFromFile(File file) throws IOException {
        this.path = file.getCanonicalPath();
        this.name = file.getName();
        
        int idx = name.lastIndexOf('.');
        if (idx > 0 && idx < name.length() - 1) {
            this.extension = name.substring(idx+1);
        }
        
        this.lastModified = new Date(file.lastModified());
        this.fileLength = file.length();
        
        //TODO: implement kind
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String basename) {
        this.name = basename;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public long getFileLength() {
        return fileLength;
    }

    public void setFileLength(long fileLength) {
        this.fileLength = fileLength;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

}
