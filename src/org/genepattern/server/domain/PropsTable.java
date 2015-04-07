package org.genepattern.server.domain;

import java.util.Collections;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import org.genepattern.server.DbException;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.Query;

/**
 * Annotation based update to the 'Props' class; 
 * This is a replacement for the org.genepattern.server.domain.Props class which was configured in the Props.hbm.xml file.
 * 
 * This newer annotated file avoids problems with some DB integrations because 
 * the PROPS table has a column named 'key' which is a reserved word on some DB systems.
 * 
 * @author pcarr
 *
 */
@Entity
@Table(name="props")
public class PropsTable {
    private static final Logger log = Logger.getLogger(PropsTable.class);
    
    /**
     * Get the value for the given key in the PROPS table.
     * 
     * @param key
     * @return
     * @throws DbException
     */
    public static String selectValue(final String key) throws DbException {
        return selectValue(HibernateUtil.instance(), key);
    }
    
    /**
     * Get the value for the given key in the PROPS table.
     * 
     * @param mgr
     * @param key
     * @return
     * @throws DbException
     */
    public static String selectValue(final HibernateSessionManager mgr, final String key) throws DbException {
        PropsTable row=selectRow(mgr, key);
        if (row==null) {
            return "";
        }
        return row.getValue();
    }
    
    /**
     * Get the list of keys from the PROPS table which match the given matchingKey.
     * To get more than one result pass in a '%' wildcard character, e.g.
     *     matchingKey='registeredVersion%'
     * @param matchingKey
     * @return
     */
    public static List<String> selectKeys(final String matchingKey) {
        return selectKeys(HibernateUtil.instance(), matchingKey);
    }

    public static List<String> selectKeys(final HibernateSessionManager mgr, final String matchingKey) {
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            final String hql="select p.key from "+PropsTable.class.getName()+" p where p.key like :key";
            Query query = mgr.getSession().createQuery(hql);  
            query.setString("key", matchingKey);
            List<String> values=query.list();
            return values;
        }
        catch (Throwable t) {
            log.error("Error getting values from PROPS table for key='"+matchingKey+"': "+t.getLocalizedMessage(), t);
            return Collections.emptyList();
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

    public static PropsTable selectRow(final String key) throws DbException {
        return selectRow(HibernateUtil.instance(), key);
    }

    /**
     * Select a row from the PROPS table which matches the given key, 
     * return null if there is no entry in the table.
     * @param mgr
     * @param key
     * @return
     * @throws DbException 
     */
    public static PropsTable selectRow(final HibernateSessionManager mgr, final String key) 
    throws DbException
    {
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            final String hql="from "+PropsTable.class.getName()+" p where p.key like :key";
            Query query = mgr.getSession().createQuery(hql);  
            query.setString("key", key);
            List<PropsTable> props=query.list();
            if (props==null || props.size()==0) {
                return null;
            }
            else if (props.size() > 1) {
                log.error("More than one row in PROPS table for key='"+key+"'");
            }
            return props.get(0);
        }
        catch (Throwable t) {
            log.error(t);
            throw new DbException(
                    "Unexpected error getting row from PROPS table, key='"+key+"': "+t.getLocalizedMessage(), t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

    public static List<PropsTable> selectAllProps() throws DbException {
        return selectAllProps(HibernateUtil.instance());
    }

    /**
     * Get all entries from the PROPS table.
     * 
     * @param mgr
     * @return
     * @throws DbException
     */
    public static List<PropsTable> selectAllProps(final HibernateSessionManager mgr) throws DbException {
        // select value from props where `key`={key}
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            final String hql=" from "+PropsTable.class.getName();
            Query query = mgr.getSession().createQuery(hql);  
            List<PropsTable> rval=query.list();
            return rval;
        }
        catch (Throwable t) {
            throw new DbException("Error getting all entries from PROPS table: "+t.getLocalizedMessage(), t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

    /**
     * Save a key/value pair to the PROPS table.
     * @param key
     * @param value
     * @return
     */
    public static boolean saveProp(final String key, final String value) {
        return saveProp(HibernateUtil.instance(), key, value);
    }
    
    /**
     * Save a key/value pair to the PROPS table.
     * @param key
     * @param value
     * @return
     */
    public static boolean saveProp(final HibernateSessionManager mgr, final String key, final String value) {
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            PropsTable props=selectRow(mgr, key);
            if (props==null) {
                props=new PropsTable();
                props.setKey(key);
                props.setValue(value);
                mgr.getSession().save(props);
            }
            else {
                props.setValue(value);
                mgr.getSession().update(props);
            } 
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
            return true;
        }
        catch (Throwable t) {
            log.error("Error saving (key,value) to PROPS table in DB, ('"+key+"', '"+value+"')", t);
            mgr.rollbackTransaction();
            return false;
        }
    }
    
    /**
     * Remove an entry from the PROPS table.
     * @param key
     */
    public static void removeProp(final String key) {
        removeProp(HibernateUtil.instance(), key);
    }
    
    /**
     * Remove an entry from the PROPS table.
     * @param key
     */
    public static void removeProp(final HibernateSessionManager mgr, final String key) {
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            PropsTable props=new PropsTable();
            props.setKey(key);
            mgr.getSession().delete(props);
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error("Error deleting value from PROPS table, key='"+key+"'", t);
            mgr.rollbackTransaction();
        }
    }

    @Id
    @Column(name="key")
    private String key;
    @Column
    private String value;
    
    public String getKey() {
        return key;
    }
    public void setKey(final String key) {
        this.key=key;
    }
    
    public String getValue() {
        return this.value;
    }
    public void setValue(final String value) {
        this.value=value;
    }

}
