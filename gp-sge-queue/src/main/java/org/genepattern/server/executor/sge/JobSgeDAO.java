package org.genepattern.server.executor.sge;

import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.webservice.JobInfo;

/**
 * Hibernate queries into the JOB_SGE table.
 * @author pcarr
 */
public class JobSgeDAO extends BaseDAO {
    public void createOrUpdateJobRecord(JobSge jobRecord) {
        saveOrUpdate( jobRecord );
    }
    
    /**
     * Can throw an Exception if the record is not in the table.
     * @param gpJobInfo
     * @return
     */
    public JobSge getJobRecord(JobInfo gpJobInfo) {
        return (JobSge) HibernateUtil.getSession().load( JobSge.class, gpJobInfo.getJobNumber());
    }
}
