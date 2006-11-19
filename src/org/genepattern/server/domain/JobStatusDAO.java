package org.genepattern.server.domain;

// Generated Sep 21, 2006 12:36:06 PM by Hibernate Tools 3.1.0.beta5

import java.util.List;
import javax.naming.InitialContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Example;

/**
 * Home object for domain model class JobStatus.
 * 
 * @see org.genepattern.server.domain.JobStatus
 * @author Hibernate Tools
 */
public class JobStatusDAO {

	private static final Logger log = Logger.getLogger(JobStatusDAO.class);

	public JobStatus findById(java.lang.Integer id) {
		log.debug("getting JobStatus instance with id: " + id);
		try {
			return (JobStatus) HibernateUtil.getSession().get(
					"org.genepattern.server.domain.JobStatus", id);
		} catch (RuntimeException re) {
			log.error("get failed", re);
			throw re;
		}
	}

}
