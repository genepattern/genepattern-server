package org.genepattern.server.taskinstall.dao;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.genepattern.server.DbException;
import org.genepattern.server.database.HibernateSessionManager;
import org.hibernate.criterion.Restrictions;

/**
 * Hibernate mapping class for recording Task Installation details to the GP database.
 * 
 * @author pcarr
 */
@Entity
@Table(name="task_install")
public class TaskInstall {

    /**
     * Set the categories for an installed task. There must already be an entry in the task_install table.
     * @param mgr
     * @param lsid, the lsid for the task
     * @param categoryNames, the set of categories
     * @throws DbException if the 
     */
    public static void setCategories(final HibernateSessionManager mgr, final String lsid, final List<String> categoryNames) throws DbException {
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            if (!isInTransaction) {
                mgr.beginTransaction();
            }
            TaskInstall taskInstall = (TaskInstall) mgr.getSession().get(TaskInstall.class, lsid);
            if (taskInstall==null) {
                throw new DbException("No entry in task_install for lsid="+lsid);
            }
            
            final Set<Category> categories=new LinkedHashSet<Category>();
            for(final String name : categoryNames) {
                Category category = (Category) mgr.getSession().createCriteria(Category.class).add(Restrictions.eq("name", name)).uniqueResult();
                if (category==null) {
                    category = new Category(name);
                    mgr.getSession().save(category);
                }
                categories.add(category);
            }
            taskInstall.setCategories(categories);
            mgr.getSession().saveOrUpdate(taskInstall);
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
        catch (Throwable t) {
            throw new DbException("Error setting categories for lsid="+lsid+", categories="+categoryNames, t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    /**
     * For debugging, get the list of all entries in the task_install_category mapping table.
     * @param mgr
     * @return
     * @throws DbException
     */
    public static List<?> getAllTaskInstallCategory(final HibernateSessionManager mgr) throws DbException {
        final String sql="select * from task_install_category order by lsid, category_id";
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            if (!isInTransaction) {
                mgr.beginTransaction();
            }
            List<?> items = mgr.getSession().createSQLQuery(sql).list();
            return items;
        }
        catch (Throwable t) {
            throw new DbException("Unexpected error getting entries from task_install_category table", t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    public TaskInstall() {
    }
    public TaskInstall(final String lsid) {
        this.setLsid(lsid);
    }
    
    /**
     * The full lsid of the installed task, it's the primary key in the table.
     */
    @Id
    @Column(name = "lsid")
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
    
    /*cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}*/
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL) 
    @JoinTable( name="task_install_category", 
        joinColumns = { 
            @JoinColumn(name="lsid", referencedColumnName="lsid") },
            inverseJoinColumns = { @JoinColumn(name="category_id") })
    private Set<Category> categories = new HashSet<Category>(0);

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

    public Set<Category> getCategories() {
        return this.categories;
    }
    
    public void setCategories(Set<Category> categories) {
        this.categories=categories;
    }

}
