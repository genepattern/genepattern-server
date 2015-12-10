/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.junitutil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamListHelper;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.hibernate.Query;

public class AnalysisJobUtil {
    public static boolean isSet(final String in) {
        return in != null && in.length()>0;
    }
    
    public static Integer addJobToDb(final HibernateSessionManager mgr, final GpContext taskContext, final JobInput jobInput, final boolean initDefault) throws Exception {
        return addJobToDb(mgr, ServerConfigurationFactory.instance(), taskContext, jobInput, -1, initDefault);
    }

    /**
     * Add a record to the ANALYSIS_JOB table for the given job. This is a close approximation
     * (based on code in the JobInputApiLegacy.java file) to how jobs are submitted to the server
     * circa GP 3.8.0 and earlier.
     * 
     * 
     * @param taskContext, requires a non-null context with a userId and taskInfo.
     * @param jobInput, the jobInput must match the taskContext.taskInfo (lsid and parameter names).
     * @return
     * @throws Exception
     */
    public static Integer addJobToDb(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext taskContext, final JobInput jobInput, final Integer parentJobNumber, final boolean initDefault) throws Exception {
        if (taskContext==null) {
            throw new IllegalArgumentException("taskContext==null");
        }
        if (!isSet(taskContext.getUserId())) {
            throw new IllegalArgumentException("taskContext.userId must be set");
        }
        if (taskContext.getTaskInfo()==null) {
            throw new IllegalArgumentException("taskContext.taskInfo must be set");
        }
        final ParameterInfo[] parameterInfoArray=initParameterValues(mgr, gpConfig, taskContext, jobInput, taskContext.getTaskInfo(), initDefault);
        return addJobToDb(mgr, taskContext.getUserId(), taskContext.getTaskInfo(), parameterInfoArray, parentJobNumber); 
    }
    
    public JobInfo fetchJobInfoFromDb(final HibernateSessionManager mgr, final int jobNumber) {
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            AnalysisDAO dao = new AnalysisDAO(mgr);
            JobInfo jobInfo = dao.getJobInfo(jobNumber);
            return jobInfo;
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    public static int deleteJobFromDb(final HibernateSessionManager mgr, final int jobNo) throws DbException {
        final boolean isInTransaction=mgr.isInTransaction();
        boolean doClose=!isInTransaction;
        try {
            mgr.beginTransaction();
            final String hqlDelete = "delete "+AnalysisJob.class.getName()+" a where a.jobNo = :jobNo";
            int deletedEntities = mgr.getSession().createQuery( hqlDelete )
                    .setInteger( "jobNo", jobNo )
                    .executeUpdate();
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
            return deletedEntities;
        }
        catch (Throwable t) {
            doClose=true;
            throw new DbException("Error deleting job from db: "+jobNo, t);
        }
        finally {
            if (doClose) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    /**
     * Based on JobInputApiLegacy code; this method does file transfers for external url values for file list parameters.
     * @param userContext
     * @param jobInput
     * @param taskInfo
     * @return
     * @throws Exception
     */
    public static ParameterInfo[] initParameterValues(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext userContext, final JobInput jobInput, final TaskInfo taskInfo, final boolean initDefault) throws Exception {
        if (jobInput==null) {
            throw new IllegalArgumentException("jobInput==null");
        }
        if (jobInput.getParams()==null) {
            throw new IllegalArgumentException("jobInput.params==null");
        }

        //initialize a map of paramName to ParameterInfo 
        final Map<String,ParameterInfoRecord> paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskInfo);

        //for each formal input parameter ... set the actual value to be used on the command line
        for(Entry<String,ParameterInfoRecord> entry : paramInfoMap.entrySet()) {
            // validate num values
            // and initialize input file (or parameter) lists as needed
            Param inputParam=jobInput.getParam( entry.getKey() );
            ParamListHelper plh=new ParamListHelper(mgr, gpConfig, userContext, entry.getValue(), jobInput, inputParam, initDefault);
            plh.validateNumValues();
            plh.validateRange();
            plh.updatePinfoValue();
        }
        
        List<ParameterInfo> actualParameters = new ArrayList<ParameterInfo>();
        for(ParameterInfoRecord pinfoRecord : paramInfoMap.values()) {
            actualParameters.add( pinfoRecord.getActual() );
        }
        ParameterInfo[] actualParams = actualParameters.toArray(new ParameterInfo[0]);
        return actualParams;
    }
    
    public static Integer addJobToDb(HibernateSessionManager mgr) throws DbException {
        return addJobToDb(mgr, GpContext.getServerContext());
    }
    
    public static Integer addJobToDb(final HibernateSessionManager mgr, final GpContext userContext) throws DbException {
        final int parentJobId=-1;
        return addJobToDb(mgr, userContext, parentJobId);
    }

    /**
     * Create a new entry in the analysis_job table, used primarily to generate a new job_id.
     * @param userContext, optionally set userId, taskName, and taskLsid
     * @param parentJobId, when parentJobId >= 0 set this as a child step in a pipeline
     * @return
     */
    public static Integer addJobToDb(final HibernateSessionManager mgr, final GpContext userContext, final int parentJobId) 
    throws DbException
    {
        final boolean isInTransaction=mgr.isInTransaction();
        boolean doClose=!isInTransaction;
        Integer jobId = null;
        try {
            String parameter_info = ""; //empty CLOB

            AnalysisJob aJob = new AnalysisJob();
            aJob.setSubmittedDate(new Date());
            aJob.setParameterInfo(parameter_info);
            if (userContext != null) {
                aJob.setUserId(userContext.getUserId());
            }
            if (userContext != null && userContext.getTaskInfo() != null) {
                aJob.setTaskName(userContext.getTaskInfo().getName());
                aJob.setTaskLsid(userContext.getTaskInfo().getLsid());
            }
            if (parentJobId >= 0) {
                aJob.setParent(parentJobId);
            }
            else {
                aJob.setParent(-1);
            }

            JobStatus js = new JobStatus();
            js.setStatusId(JobStatus.JOB_PENDING);
            js.setStatusName(JobStatus.PENDING);
            aJob.setJobStatus(js);

            mgr.beginTransaction();
            jobId = (Integer) mgr.getSession().save(aJob);
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
        catch (Throwable t) {
            doClose=true;
            throw new DbException("Error adding new job to db, parentJobId="+parentJobId, t);
        }
        finally {
            if (doClose) {
                mgr.closeCurrentSession();
            }
        }
        
        return jobId;
    }
    
    public static Integer addJobToDb(final HibernateSessionManager mgr, final String userId, final TaskInfo taskInfo, final ParameterInfo[] parameterInfoArray, final Integer parentJobNumber) throws Exception {
        if (taskInfo.getID() < 0) {
            //force arbitrary task_id
            taskInfo.setID(1);
        }
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            AnalysisDAO ds = new AnalysisDAO(mgr);
            Integer jobNo = ds.addNewJob(userId, taskInfo, parameterInfoArray, parentJobNumber);
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
            return jobNo;
        }
        catch (Exception e) {
            throw e;
        }
        catch (Throwable t) {
            throw new Exception(t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    public static void setStatusInDb(final HibernateSessionManager mgr, final int jobNo, final int statusId) throws Exception {
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            Query query = mgr.getSession().createQuery("update org.genepattern.server.domain.AnalysisJob set status_id = :statusId where job_no = :jobNo");
            query.setInteger("jobNo", jobNo);
            query.setInteger("statusId", statusId);
            int result = query.executeUpdate();
            if (result != 1) {
                throw new Exception("Failed to update status to"+statusId+" for jobNo="+jobNo);
            }

            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
        catch (Exception e) {
            throw e;
        }
        catch (Throwable t) {
            throw new Exception(t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
}
