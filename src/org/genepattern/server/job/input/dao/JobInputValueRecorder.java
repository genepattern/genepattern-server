/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.dao;

import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateSessionManager;
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
    
    private final HibernateSessionManager mgr;
    
    public JobInputValueRecorder(final HibernateSessionManager mgr) {
        this.mgr=mgr;
    }

    public void saveJobInput(final Integer gpJobNo, final JobInput jobInput) throws Exception {
        final boolean inTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            int numRows=0;
            for(final Param param : jobInput.getParams().values()) {
                int idx=1; //Note, db index starts at 1 so that max(idx)==num values
                for(final Entry<GroupId,ParamValue> entry : param.getValuesAsEntries()) {
                    final GroupId groupId=entry.getKey();
                    final ParamValue paramValue=entry.getValue();
                    final JobInputValue j=JobInputValue.create(gpJobNo, param.getParamId(), idx, groupId, paramValue);
                    mgr.getSession().saveOrUpdate(j);
                    ++idx;
                    ++numRows;
                }
            }
            if (!inTransaction) {
                mgr.commitTransaction();
            }
            if (log.isDebugEnabled()) {
                log.debug("saved "+numRows+" rows to job_input_value table for gpJobNo="+gpJobNo);
            }
        }
        catch (Throwable t) {
            log.error("error saving entries to job_input_value table for gpJobNo="+gpJobNo, t);
            mgr.rollbackTransaction();
            throw new Exception("Error saving job_input_values for gpJobNo="+gpJobNo, t);
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    public JobInput fetchJobInput(final int gpJobNo) throws Exception {
        boolean inTransaction=mgr.isInTransaction();
        try {
            final String hql = "from "+JobInputValue.class.getName()+" vv where vv.gpJobNo = :gpJobNo order by vv.pname, vv.idx";
            mgr.beginTransaction();
            Session session = mgr.getSession();
            Query query = session.createQuery(hql);
            query.setInteger("gpJobNo", gpJobNo);
            @SuppressWarnings("unchecked")
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
                mgr.closeCurrentSession();
            }
        }
    }
    
    public List<Integer> fetchMatchingJobs(final String inputValue) throws Exception {
        boolean inTransaction=mgr.isInTransaction();
        try {
            final String hql = "select gpJobNo from "+JobInputValue.class.getName()+" where pvalue = :pvalue";
            mgr.beginTransaction();
            Session session = mgr.getSession();
            Query query = session.createQuery(hql);
            query.setString("pvalue", inputValue);
            @SuppressWarnings("unchecked")
            final List<Integer> rval = query.list();
            return rval;
        }
        catch (Throwable t) {
            String message="Error selecting matching jobs for input value="+inputValue;
            log.error(message, t);
            throw new Exception(message);
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    public List<String> fetchMatchingGroups(final String inputValue) throws Exception {
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            String sql="select distinct(jg.group_id) from job_input_value ji, job_group jg  "+
                    "where ji.pvalue = :pvalue and jg.job_no = ji.gp_job_no";
            
            mgr.beginTransaction();
            Session session = mgr.getSession();
            SQLQuery query=session.createSQLQuery(sql);
            query.setString("pvalue", inputValue);
            @SuppressWarnings("unchecked")
            final List<String> rval = query.list();
            return rval;
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

}
