/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2009) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.domain;

// Generated Sep 21, 2006 12:36:06 PM by Hibernate Tools 3.1.0.beta5

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;

/**
 * Home object for domain model class JobStatus.
 * 
 * @see org.genepattern.server.domain.JobStatus
 * @author Hibernate Tools
 */
public class JobStatusDAO {
    private static Logger log = Logger.getLogger(JobStatusDAO.class);

    public JobStatus findById(java.lang.Integer id) {
        log.debug("getting JobStatus instance with id: " + id);
        try {
            return (JobStatus) HibernateUtil.getSession().get("org.genepattern.server.domain.JobStatus", id);
        } 
        catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }
}
