/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
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
@Table(name="PROPS")
public class PropsTable {
    private static final Logger log = Logger.getLogger(PropsTable.class);

    /** @deprecated */
    public static String selectValue(final String key) throws DbException {
        return selectValue(org.genepattern.server.database.HibernateUtil.instance(), key);
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
    
    /** @deprecated */
    public static List<String> selectKeys(final String matchingKey) {
        return selectKeys(org.genepattern.server.database.HibernateUtil.instance(), matchingKey);
    }

    /**
     * Get the list of keys from the PROPS table which match the given matchingKey.
     * To get more than one result pass in a '%' wildcard character, e.g.
     *     matchingKey='registeredVersion%'
     * @param mgr
     * @param matchingKey
     * @return
     */
    public static List<String> selectKeys(final HibernateSessionManager mgr, final String matchingKey) {
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            final String hql="select p.keyColumn from "+PropsTable.class.getName()+" p where p.keyColumn like :key";
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

    /** @deprecated */
    public static PropsTable selectRow(final String key) throws DbException {
        return selectRow(org.genepattern.server.database.HibernateUtil.instance(), key);
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
            final String hql="from "+PropsTable.class.getName()+" p where p.keyColumn like :key";
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

    /** @deprecated */
    public static List<PropsTable> selectAllProps() throws DbException {
        return selectAllProps(org.genepattern.server.database.HibernateUtil.instance());
    }

    /**
     * Get all entries from the PROPS table.
     * 
     * @param mgr
     * @return
     * @throws DbException
     */
    public static List<PropsTable> selectAllProps(final HibernateSessionManager mgr) throws DbException {
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            final String hql="from "+PropsTable.class.getName();
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

    /** @deprecated */
    public static boolean saveProp(final String key, final String value) 
    throws DbException
    {
        return saveProp(org.genepattern.server.database.HibernateUtil.instance(), key, value);
    }

    /**
     * Save a key/value pair to the PROPS table.
     * @param mgr
     * @param key
     * @param value
     * @return
     */
    public static boolean saveProp(final HibernateSessionManager mgr, final String key, final String value) 
    throws DbException
    {
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            
            PropsTable prop=new PropsTable();
            prop.setKeyColumn(key);
            prop.setValue(value);
            mgr.getSession().saveOrUpdate(prop);
            
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
            return true;
        }
        catch (Throwable t) {
            mgr.rollbackTransaction();
            throw new DbException("Error saving (key,value) to PROPS table in DB, ('"+key+"', '"+value+"')", t);
        }
    }

    /** @deprecated */
    public static void removeProp(final String key) {
        removeProp(org.genepattern.server.database.HibernateUtil.instance(), key);
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
            props.setKeyColumn(key);
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

    private String keyColumn;

    private String value;
    
    /** 
     * Quoted the MySQL reserved word 'key' with back ticks ("`"). 
     * The all upper-case name is required for compatibility with HSQLDB. 
     * When column names are quoted in HSQLDB, it forces case-sensitive comparison.
     */
    @Id
    @Column(name="`KEY`")
    public String getKeyColumn() {
        return keyColumn;
    }
    
    public void setKeyColumn(final String key) {
        this.keyColumn=key;
    }

    @Column
    public String getValue() {
        return this.value;
    }

    public void setValue(final String value) {
        this.value=value;
    }

}
