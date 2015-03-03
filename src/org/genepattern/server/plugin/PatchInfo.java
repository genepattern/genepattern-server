package org.genepattern.server.plugin;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.genepattern.util.LSID;

@Entity
@Table(name="patch_info",
uniqueConstraints = {@UniqueConstraint(columnNames={"lsid"})})
public class PatchInfo {
    // DB columns
    private Long id=null;
    private String lsid;
    private String userId;
    private String url;
    private Date statusDate=new Date();

    // non-DB columns
    private LSID patchLsid;
    private URL patchUrl;
    
    public PatchInfo() {
    }
    
    public PatchInfo(final String patchLsid) throws MalformedURLException {
        this(patchLsid, (String)null);
    }
    
    public PatchInfo(final String patchLsid, final String patchUrl) throws MalformedURLException {
        setLsid(patchLsid);
        setUrl(patchUrl);
    }
    
    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    @Column(name="lsid")
    public String getLsid() {
        return lsid;
    }
    public void setLsid(final String lsid) throws MalformedURLException {
        this.lsid=lsid;
        this.patchLsid=lsid==null ? null : new LSID(lsid);
    }

    @Column(name="user_id")
    public String getUserId() {
        return userId;
    }
    public void setUserId(final String userId) {
        this.userId=userId;
    }

    @Column(name="url") 
    public String getUrl() {
        return url;
    }
    public void setUrl(final String url) throws MalformedURLException {
        this.url=url;
        this.patchUrl=url==null ? null : new URL(url);
    }
    
    /**
     * The date that the patch info was recorded into the database.
     */
    @Column(name="status_date", nullable=false)
    public Date getStatusDate() {
        return statusDate;
    }
    public void setStatusDate(final Date statusDate) {
        this.statusDate=statusDate;
    }        

    @Transient
    public LSID getPatchLsid() {
        return patchLsid;
    }
 
    @Transient
    public URL getPatchUrl() {
        return patchUrl;
    }
    
    public boolean equals(Object obj) {
        if (obj == null) { 
            return false;
        }
        if (!(obj instanceof PatchInfo)) {
            return false;
        }
        PatchInfo arg=(PatchInfo)obj;
        return //Objects.equals(id, arg.id) &&
                Objects.equals(lsid, arg.lsid) &&
                Objects.equals(url, arg.url);
    }
    
    public int hashCode() {
        //return Objects.hash(id, lsid, url);
        return Objects.hash(lsid, url);
    }
    
    public String toString() {
        return lsid;
    }
    
}
