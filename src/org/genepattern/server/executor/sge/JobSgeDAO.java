/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.sge;

import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.webservice.JobInfo;

/**
 * Hibernate queries into the JOB_SGE table.
 * @author pcarr
 */
public class JobSgeDAO extends BaseDAO {
    public static Logger log = Logger.getLogger(JobSgeDAO.class);

    public void createOrUpdateJobRecord(JobSge jobRecord) {
        saveOrUpdate( jobRecord );
    }
    
    /**
     * Can throw an Exception if the record is not in the table.
     * @param gpJobInfo
     * @return null if no record found.
     */
    public JobSge getJobRecord(JobInfo gpJobInfo) {
        JobSge jobSge = null;
        jobSge = (JobSge) HibernateUtil.getSession().load( JobSge.class, gpJobInfo.getJobNumber());
        if (jobSge == null) {
            log.error("No record found in JOB_SGE for gpJobNo="+gpJobInfo.getJobNumber());
        }
        return jobSge;
    }
}
