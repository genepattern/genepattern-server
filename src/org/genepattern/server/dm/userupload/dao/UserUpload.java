package org.genepattern.server.dm.userupload.dao;

import java.io.File;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.util.SemanticUtil;

@Entity
@Table(name="user_upload")
public class UserUpload {
    static public UserUpload initFromGpFileObj(Context userContext, GpFilePath fileObj) {
        UserUpload uf = new UserUpload();
        uf.setUserId(userContext.getUserId());
        uf.setPath(fileObj.getRelativePath());
        File file = fileObj.getServerFile();
        uf.name = file.getName();
        int idx = uf.name.lastIndexOf('.');
        if (idx > 0 && idx < uf.name.length() - 1) {
            uf.setExtension(uf.name.substring(idx+1));
        }
        
        if (file.exists()) {
            uf.setLastModified(new Date(file.lastModified()));
            uf.setFileLength(file.length());
            uf.setExtension(SemanticUtil.getExtension(file));
            uf.setKind(SemanticUtil.getKind(file));
            if (file.isDirectory()) {
                uf.setKind("directory");
            }
        }
        return uf;
    }
    
    
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO )
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private UserUpload parentDir;
    
    @Column(name = "user_id")
    private String userId;
    
    private String path;
    private String name;
    
    @Column(name = "last_modified")
    private Date lastModified;

    @Column(name = "file_length")
    private long fileLength;
    private String extension;
    private String kind;
    
    @Column(name = "num_parts")
    private int numParts = 1;
    
    @Column(name = "num_parts_recd")
    private int numPartsRecd = 0;
    
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }


    public UserUpload getParentDir() {
        return parentDir;
    }
    
    public void setParentDir(UserUpload parentDir) {
        this.parentDir = parentDir;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public long getFileLength() {
        return fileLength;
    }

    public void setFileLength(long fileLength) {
        this.fileLength = fileLength;
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
    public int getNumParts() {
        return numParts;
    }
    public void setNumParts(int numParts) {
        this.numParts = numParts;
    }
    public int getNumPartsRecd() {
        return numPartsRecd;
    }
    public void setNumPartsRecd(int numPartsRecd) {
        this.numPartsRecd = numPartsRecd;
    }

}
