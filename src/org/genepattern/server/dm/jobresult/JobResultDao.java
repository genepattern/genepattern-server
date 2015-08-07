/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.dm.jobresult;

import java.util.List;

import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.Query;

public class JobResultDao extends BaseDAO {
    public List<JobResult> selectResultsForJob(int job, boolean selectLogs) {
        String hql = "from " + JobResult.class.getName() + " jr where jr.job_id = :jobId";
        Query query = HibernateUtil.getSession().createQuery(hql);
        query.setInteger("jobId", job);
        @SuppressWarnings("unchecked")
        List<JobResult> rval = query.list();
        return rval;
    }
    
    public List<JobResult> selectResultsForJob(int job) {
        return selectResultsForJob(job, false);
    }
    
    public long insertResult(int job, String name, String path, boolean log) {
        JobResult jr = new JobResult();
        jr.setJobId(job);
        jr.setName(name);
        jr.setPath(path);
        jr.setLog(log);
        HibernateUtil.getSession().save(jr);
        
        return jr.getId();
    }
}
