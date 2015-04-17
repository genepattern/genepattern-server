package org.genepattern.server.taskinstall.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.genepattern.server.DbException;
import org.genepattern.server.database.HibernateSessionManager;


/**
 * For categorizing modules via the task_install, categories, and task_install_categories tables.
 * @author pcarr
 *
 */
@Entity
@Table(name="category")
public class Category {
    
    /**
     * For debugging, get the list of all categories from the DB.
     * @param mgr
     * @return
     * @throws DbException
     */
    @SuppressWarnings("unchecked")
    public static List<Category> getAllCategories(final HibernateSessionManager mgr) throws DbException {
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            if (!isInTransaction) {
                mgr.beginTransaction();
            }
            return (List<Category>) mgr.getSession().createCriteria(Category.class).list();
        }
        catch (Throwable t) {
            throw new DbException("Unexpected error getting categories", t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }    
    
    @Id
    @GeneratedValue
    @Column(name="category_id", unique = true, nullable = false)
    private Integer categoryId;
    
    @Column(name="name", nullable = false)
    private String name;
    
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "categories", cascade={CascadeType.ALL} )
    private Set<TaskInstall> taskInstalls = new HashSet<TaskInstall>(0);
    
    public Category() {
    }

    public Category(String name) {
        this.name=name;
    }
    
    public Integer getCategoryId() {
        return this.categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId=categoryId;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setName(final String name) {
        this.name=name;
    }
    
    public Set<TaskInstall> getTaskInstalls() {
        return this.taskInstalls;
    }
    
    public void setTaskInstalls(Set<TaskInstall> taskInstalls) {
        this.taskInstalls=taskInstalls;
    }
}
