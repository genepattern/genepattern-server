package org.genepattern.junitutil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamListHelper;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

public class AnalysisJobUtil {
    public static boolean isSet(final String in) {
        return in != null && in.length()>0;
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
    public Integer addJobToDb(final GpContext taskContext, final JobInput jobInput) throws Exception {
        final boolean initDefaultDefault=false;
        return addJobToDb(taskContext, jobInput, initDefaultDefault);
    }
    
    public Integer addJobToDb(final GpContext taskContext, final JobInput jobInput, final boolean initDefault) throws Exception {
        if (taskContext==null) {
            throw new IllegalArgumentException("taskContext==null");
        }
        if (!isSet(taskContext.getUserId())) {
            throw new IllegalArgumentException("taskContext.userId must be set");
        }
        if (taskContext.getTaskInfo()==null) {
            throw new IllegalArgumentException("taskContext.taskInfo must be set");
        }
        //no support for pipelines, yet
        final Integer parentJobNumber=-1;
        final ParameterInfo[] parameterInfoArray=initParameterValues(taskContext, jobInput, taskContext.getTaskInfo(), initDefault);
        return addJobToDb(taskContext.getUserId(), taskContext.getTaskInfo(), parameterInfoArray, parentJobNumber); 
    }
    
    /**
     * Based on JobInputApiLegacy code; this method does file transfers for external url values for file list parameters.
     * @param userContext
     * @param jobInput
     * @param taskInfo
     * @return
     * @throws Exception
     */
    public ParameterInfo[] initParameterValues(final GpContext userContext, final JobInput jobInput, final TaskInfo taskInfo, final boolean initDefault) throws Exception {
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
            ParamListHelper plh=new ParamListHelper(userContext, entry.getValue(), inputParam, initDefault);
            plh.validateNumValues();
            plh.updatePinfoValue();
        }
        
        List<ParameterInfo> actualParameters = new ArrayList<ParameterInfo>();
        for(ParameterInfoRecord pinfoRecord : paramInfoMap.values()) {
            actualParameters.add( pinfoRecord.getActual() );
        }
        ParameterInfo[] actualParams = actualParameters.toArray(new ParameterInfo[0]);
        return actualParams;
    }

    
    public Integer addJobToDb(final String userId, final TaskInfo taskInfo, final ParameterInfo[] parameterInfoArray, final Integer parentJobNumber) throws Exception {
        if (taskInfo.getID() < 0) {
            //force arbitrary task_id
            taskInfo.setID(1);
        }
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            AnalysisDAO ds = new AnalysisDAO();
            Integer jobNo = ds.addNewJob(userId, taskInfo, parameterInfoArray, parentJobNumber);
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
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
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    public void deleteJobFromDb(final int jobId) throws Exception {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            AnalysisJob aJob = (AnalysisJob) HibernateUtil.getSession().get(AnalysisJob.class, jobId);
            HibernateUtil.getSession().delete(aJob);
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
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
                HibernateUtil.closeCurrentSession();
            }
        }
    }
}
