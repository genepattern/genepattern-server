package org.genepattern.server.domain;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.genepattern.util.SemanticUtil;


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

        this.extension = SemanticUtil.getExtension(file);
        this.kind = SemanticUtil.getKind(file);
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
    
    //extra methods, not mapped to DB
    
    /**
     * Get a link to the file, e.g. /gp/data/<path_to_file>.
     * @return
     */
    public String getLink() {
        //hard-coded to the dataservlet
        //example 1, fully qualified server file path
        //    http://127.0.0.1:8080/gp/data//Shared/file.txt
        //example 2, relative server file path, relative to the working dir of the server
        //    http://127.0.0.1:8080/gp/data/../users/test/user.uploads/file.txt
        
        //TODO: hard-coded context path should be configurable
        String link = "/gp/data/" +  path;
        return link;
    }

}
