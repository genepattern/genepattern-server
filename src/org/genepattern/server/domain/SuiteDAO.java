/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.domain;

// Generated Sep 21, 2006 12:36:06 PM by Hibernate Tools 3.1.0.beta5

import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.Query;

/**
 * Home object for domain model class Suite.
 * 
 * @see org.genepattern.server.domain.Suite
 * @author Hibernate Tools
 */
public class SuiteDAO extends BaseDAO {

    private static final Logger log = Logger.getLogger(SuiteDAO.class);

    public Suite findById(String id) {
        try {
            return (Suite) HibernateUtil.getSession().get("org.genepattern.server.domain.Suite", id);
        }
        catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }

    public List<Suite> findAll() {
        try {
            return HibernateUtil.getSession().createQuery("from org.genepattern.server.domain.Suite order by name")
                    .list();
        }
        catch (RuntimeException re) {
            log.error("find by example failed", re);
            throw re;
        }

    }

    /**
     * 
     * @param ownerName
     * @return
     */
    public List<Suite> findByOwner(String ownerName) {
        try {
            Query query = HibernateUtil.getSession().createQuery(
                    "from org.genepattern.server.domain.Suite where userId = :ownerName order by name");
            query.setString("ownerName", ownerName);
            return query.list();
        }
        catch (RuntimeException re) {
            log.error("find by example failed", re);
            throw re;
        }

    }
    
    public List<Suite> findByOwnerOrPublic(String ownerName) {
        try {
            Query query = HibernateUtil.getSession().createQuery(
                    "from org.genepattern.server.domain.Suite where userId = :ownerName  or accessId = 1 order by name");
            query.setString("ownerName", ownerName);
            return query.list();
        }
        catch (RuntimeException re) {
            log.error("find by example failed", re);
            throw re;
        }

    }

}
