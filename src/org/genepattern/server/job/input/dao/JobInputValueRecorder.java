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
import org.hibernate.SQLQuery;
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
                String pvalue=v.getPvalue();
                if (pvalue==null) {
                    //assume it's the empty string
                    pvalue="";
                }
                jobInput.addValue(v.getPname(), pvalue, groupId);
            }
            return jobInput;
        }
        catch (Throwable t) {
            final String message="Error getting records from job_input_value table, for gpJobNo="+gpJobNo;
            log.error(message, t);
            throw new Exception(message, t);
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    public List<Integer> fetchMatchingJobs(final String inputValue) throws Exception {
        boolean inTransaction=HibernateUtil.isInTransaction();
        try {
            final String hql = "select gpJobNo from "+JobInputValue.class.getName()+" where pvalue = :pvalue";
            HibernateUtil.beginTransaction();
            Session session = HibernateUtil.getSession();
            Query query = session.createQuery(hql);
            query.setString("pvalue", inputValue);
            return (List<Integer>) query.list();
        }
        catch (Throwable t) {
            String message="Error selecting matching jobs for input value="+inputValue;
            log.error(message, t);
            throw new Exception(message);
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    public List<String> fetchMatchingGroups(final String inputValue) throws Exception {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            String sql="select distinct(jg.group_id) from job_input_value ji left outer join job_group jg on ji.gp_job_no = jg.job_no "+
                    "where ji.pvalue = :pvalue";
            
            //String hql="select distinct(jg.groupId) from "+JobInputValue.class.getName()+" as ji left outer join "+
            //        JobGroup.class.getName()+" as jg on ji.gpJobNo = jg.jobNo  where ji.pvalue = :pvalue";
            HibernateUtil.beginTransaction();
            Session session = HibernateUtil.getSession();
            SQLQuery query=session.createSQLQuery(sql);
            //Query query = session.createQuery(hql);
            query.setString("pvalue", inputValue);
            return (List<String>) query.list();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

}
