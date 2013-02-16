package org.genepattern.server.dm.jobinput;

import java.util.List;

import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.Query;

public class JobInputDao extends BaseDAO {
    public List<JobInput> selectInputsForJob(int job) {
        String hql = "from " + JobInput.class.getName() + " ji where ji.job_id = :jobId";
        Query query = HibernateUtil.getSession().createQuery( hql );
        query.setInteger("jobId", job);
        @SuppressWarnings("unchecked")
        List<JobInput> rval = query.list();
        return rval;
    }
    
    public JobInput selectInput(int job, String name) {
        String hql = "from " + JobInput.class.getName() + " ji where ji.job_id = :jobId and name = :name";
        Query query = HibernateUtil.getSession().createQuery( hql );
        query.setInteger("jobId", job);
        query.setString("name", name);
        @SuppressWarnings("unchecked")
        List<JobInput> rval = query.list();
        if (rval != null && rval.size() == 1) {
            return rval.get(0);
        }
        return null;
    }
    
    public long insertInput(int job, String name, String userValue, String type) {
        JobInput ji = new JobInput();
        ji.setJob(job);
        ji.setName(name);
        ji.setUserValue(userValue);
        ji.setKind(type);
        HibernateUtil.getSession().save(ji);
        
        return ji.getId();
    }
    
    public boolean updateCmdValue(long id, String cmdValue) {
        JobInput input = (JobInput) HibernateUtil.getSession().get(JobInput.class, id);
        input.setCommandValue(cmdValue);
        HibernateUtil.getSession().update(input);
        return true;
    }

}
