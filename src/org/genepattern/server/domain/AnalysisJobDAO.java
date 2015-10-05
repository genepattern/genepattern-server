/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.domain;

// Generated Sep 21, 2006 12:36:06 PM by Hibernate Tools 3.1.0.beta5

import org.apache.log4j.Logger;
import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateSessionManager;

/**
 * Home object for domain model class AnalysisJob.
 * 
 * @see org.genepattern.server.domain.AnalysisJob
 * @author Hibernate Tools
 */
public class AnalysisJobDAO extends BaseDAO {
	private static final Logger log = Logger.getLogger(AnalysisJobDAO.class);

	/** @deprecated */
	public AnalysisJobDAO() {
	}
	
	public AnalysisJobDAO(final HibernateSessionManager mgr) {
	    super(mgr);
	}

	public AnalysisJob findById(java.lang.Integer id) {
		log.debug("getting AnalysisJob instance with id: " + id);
		try {
			return (AnalysisJob) mgr.getSession().get(
					"org.genepattern.server.domain.AnalysisJob", id);
		} catch (RuntimeException re) {
			log.error("get failed", re);
			throw re;
		}
	}

}
