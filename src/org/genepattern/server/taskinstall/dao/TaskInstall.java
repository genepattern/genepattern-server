package org.genepattern.server.taskinstall.dao;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Hibernate mapping class for recording Task Installation details to the GP database.
 * 
 * @author pcarr
 */
@Entity
@Table(name="task_install")
public class TaskInstall {
    public TaskInstall() {
    }
    public TaskInstall(final String lsid) {
        this.setLsid(lsid);
    }
    
    /**
     * The full lsid of the installed task, it's the primary key in the table.
     */
    @Id
    private String lsid;

    /**
     * The GP user id of the owner of the task, the user who installed it.
     */
    @Column(name = "user_id")
    private String userId;
    
    /**
     * The date that the task was installed, cloned, created or edited on the server.
     */
    @Column(name = "date_installed")
    private Date dateInstalled = new Date();

    /**
     * The source from which the module was installed, for example
     * a module repository or a zip file or the module integrator.
     */
    @Column(name = "source_type")
    @Enumerated(EnumType.STRING)
    private String sourceType;

    /**
     * The url of the module repository from which this module was installed.
     * Can be null if there is no source repository or if we are not able to determine
     * the source repository.
     * 
     * Note: although not part of the initial implementation, this data model allows for
     *     repo_url to be set even when you install from zip.
     *     We haven't worked out the details yet, but we'd like a way to save this info.
     */
    @Column(name = "repo_url")
    private String repoUrl;

    /**
     * The path to the zip file from which this module was installed.
     * 
     * Note: we are not using this in the initial implementation.
     * However, it is possible, and desirable to use this table to map each task (by lsid)
     * to an actual zip file on the file system.
     * 
     * This would greatly improve the consistency and performance of the 'export from zip' method.
     */
    @Column(name = "zipfile")
    private String zipfile;

    /**
     * The lsid of the previous task from which this task was 'cloned' or 'edited',
     * can be null.
     */
    @Column(name = "prev_lsid") 
    private String prevLsid;

    /**
     * The server file path to the '<libdir>' installation directory for the module. 
     * The <tasklib> substitution parameter is allowed at the beginning of the string, e.g.
     *     "<tasklib>/ComparativeMarkerSelection.4.12"
     *     
     * Reminder, the <libdir> substitution param is the this installation directory.
     * The <tasklib> substitution param is to the global taskLib root directory, e.g.
     *     <libdir>=<tasklib>/ComparativeMarkerSelection.4.12
     */
    @Column(name = "libdir")
    private String libdir;

    public String getLsid() {
        return lsid;
    }
    public void setLsid(final String lsid) {
        this.lsid = lsid;
    }

    public String getUserId() {
        return userId;
    }
    public void setUserId(final String userId) {
        this.userId = userId;
    }

    public Date getDateInstalled() {
        return dateInstalled;
    }
    public void setDateInstalled(final Date dateInstalled) {
        this.dateInstalled = dateInstalled;
    }

    public String getSourceType() {
        return sourceType;
    }
    public void setSourceType(final String sourceType) {
        this.sourceType = sourceType;
    }

    public String getRepoUrl() {
        return repoUrl;
    }
    public void setRepoUrl(final String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public String getZipfile() {
        return zipfile;
    }
    public void setZipfile(final String zipfile) {
        this.zipfile = zipfile;
    }

    public String getPrevLsid() {
        return prevLsid;
    }
    public void setPrevLsid(final String prevLsid) {
        this.prevLsid = prevLsid;
    }

    public String getLibdir() {
        return libdir;
    }
    public void setLibdir(final String libdir) {
        this.libdir = libdir;
    }



}
