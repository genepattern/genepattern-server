/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

/* Auto generated file */

package org.genepattern.server.domain;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.Query;

public class Props {
    private static Logger log = Logger.getLogger(Props.class);

    /**
     * Select the value for the given key.
     * @param key
     * @return the value or an empty string if no matches are found.
     */
    public static String selectValue(final String key) {
        // select value from props where `key`={key}
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        HibernateUtil.beginTransaction();
        try {
            final String hql="select p.value from "+Props.class.getName()+" p where p.key like :key";
            Query query = HibernateUtil.getSession().createQuery(hql);  
            query.setString("key", key);
            List<String> rval=query.list();
            if (rval==null || rval.size()==0) {
                return "";
            }
            return rval.get(0);
        }
        catch (Throwable t) {
            log.error("Error getting value from PROPS table for key='"+key+"': "+t.getLocalizedMessage(), t);
            return "";
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

    /**
     * Get the list of keys from the PROPS table which match the given matchingKey.
     * To get more than one result pass in a '%' wildcard character, e.g.
     *     matchingKey='registeredVersion%'
     * @param matchingKey
     * @return
     */
    public static List<String> selectKeys(final String matchingKey) {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        HibernateUtil.beginTransaction();
        try {
            final String hql="select p.key from "+Props.class.getName()+" p where p.key like :key";
            Query query = HibernateUtil.getSession().createQuery(hql);  
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
                HibernateUtil.closeCurrentSession();
            }
        }
    }

    public static Props selectRow(final String key) {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        HibernateUtil.beginTransaction();
        try {
            final String hql="from "+Props.class.getName()+" p where p.key like :key";
            Query query = HibernateUtil.getSession().createQuery(hql);  
            query.setString("key", key);
            List<Props> props=query.list();
            if (props==null || props.size()==0) {
                return null;
            }
            else if (props.size() > 1) {
                log.error("More than one row in PROPS table for key='"+key+"'");
            }
            return props.get(0);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
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
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        HibernateUtil.beginTransaction();
        try {
            Props props=selectRow(key);
            if (props==null) {
                props=new Props();
                props.setKey(key);
                props.setValue(value);
                HibernateUtil.getSession().save(props);
            }
            else {
                props.setValue(value);
                HibernateUtil.getSession().update(props);
            } 
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
            return true;
        }
        catch (Throwable t) {
            log.error("Error saving (key,value) to PROPS table in DB, ('"+key+"', '"+value+"')", t);
            HibernateUtil.rollbackTransaction();
            return false;
        }
    }
    
    /**
     * Remove an entry from the PROPS table.
     * @param key
     */
    public static void removeProp(final String key) {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        HibernateUtil.beginTransaction();
        try {
            Props props=new Props();
            props.setKey(key);
            HibernateUtil.getSession().delete(props);
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error("Error deleting value from PROPS table, key='"+key+"'", t);
            HibernateUtil.rollbackTransaction();
        }
    }
    

    /**
     * auto generated
     * 
     * @es_generated
     */
    private String key;
    /**
     * auto generated
     * 
     * @es_generated
     */
    private String value;

    /**
     * auto generated
     * 
     * @es_generated
     */
    public Props() {
        super();
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public Props(String key) {
        super();
        this.key = key;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public Props(String key, String value) {
        super();
        this.key = key;
        this.value = value;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public boolean equals(Object value) {
        // TODO Implement equals() using Business key equality.
        return super.equals(value);
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public int hashCode() {
        // TODO Implement hashCode() using Business key equality.
        return super.hashCode();
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public String toString() {
        // TODO Implement toString().
        return super.toString();
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public String getKey() {
        return this.key;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public void setKey(String value) {
        this.key = value;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public String getValue() {
        return this.value;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public void setValue(String value) {
        this.value = value;
    }
}
