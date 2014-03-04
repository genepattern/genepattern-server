package org.genepattern.server.job.input.dao;

import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.job.input.GroupId;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamValue;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * Helper class for creating/reading values from the job_input_value table.
 * @author pcarr
 *
 */
public class JobInputValueRecorder {
    private static final Logger log = Logger.getLogger(JobInputValueRecorder.class);

    public void saveJobInput(final Integer gpJobNo, final JobInput jobInput) throws Exception {
        final boolean inTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            int numRows=0;
            for(final Param param : jobInput.getParams().values()) {
                int idx=1; //Note, db index starts at 1 so that max(idx)==num values
                for(final Entry<GroupId,ParamValue> entry : param.getValuesAsEntries()) {
                    final GroupId groupId=entry.getKey();
                    final ParamValue paramValue=entry.getValue();
                    final JobInputValue j=JobInputValue.create(gpJobNo, param.getParamId(), idx, groupId, paramValue);
                    HibernateUtil.getSession().saveOrUpdate(j);
                    ++idx;
                    ++numRows;
                }
            }
            if (!inTransaction) {
                HibernateUtil.commitTransaction();
            }
            if (log.isDebugEnabled()) {
                log.debug("saved "+numRows+" rows to job_input_value table for gpJobNo="+gpJobNo);
            }
        }
        catch (Throwable t) {
            log.error("error saving entries to job_input_value table for gpJobNo="+gpJobNo, t);
            HibernateUtil.rollbackTransaction();
            throw new Exception("Error saving job_input_values for gpJobNo="+gpJobNo, t);
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    public JobInput fetchJobInput(final int gpJobNo) throws Exception {
        boolean inTransaction=HibernateUtil.isInTransaction();
        try {
            final String hql = "from "+JobInputValue.class.getName()+" vv where vv.gpJobNo = :gpJobNo order by vv.pname, vv.idx";
            HibernateUtil.beginTransaction();
            Session session = HibernateUtil.getSession();
            Query query = session.createQuery(hql);
            query.setInteger("gpJobNo", gpJobNo);
            final List<JobInputValue> records = query.list();
            
            final JobInput jobInput=new JobInput();
            for(final JobInputValue v : records) {
                final GroupId groupId=new GroupId(v.getGroupName());
                jobInput.addValue(v.getPname(), v.getPvalue(), groupId);
            }
            return jobInput;
        }
        catch (Throwable t) {
            //TODO: log error
            throw new Exception("Error getting records from job_input_value table, for gpJobNo="+gpJobNo, t);
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

}
