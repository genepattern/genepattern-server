package org.genepattern.server.domain;

// Generated Sep 21, 2006 12:36:06 PM by Hibernate Tools 3.1.0.beta5

import java.util.List;
import javax.naming.InitialContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Example;

/**
 * Home object for domain model class AnalysisJob.
 * 
 * @see org.genepattern.server.domain.AnalysisJob
 * @author Hibernate Tools
 */
public class AnalysisJobDAO extends BaseDAO {

	private static final Logger log = Logger.getLogger(AnalysisJobDAO.class);

	public AnalysisJob findById(java.lang.Integer id) {
		log.debug("getting AnalysisJob instance with id: " + id);
		try {
			return (AnalysisJob) HibernateUtil.getSession().get(
					"org.genepattern.server.domain.AnalysisJob", id);
		} catch (RuntimeException re) {
			log.error("get failed", re);
			throw re;
		}
	}

}
