package org.genepattern.server.webservice.server.dao;

import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.util.*;

import org.apache.log4j.Logger;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.*;
import org.hibernate.Transaction;

public class AnalysisDataService extends BaseService {

    private static Logger log = Logger.getLogger(AnalysisDataService.class);

    private static AnalysisDataService theInstance = null;

    private AnalysisDAO analysisDAO = new AnalysisDAO();

    private AdminDAO adminDAO = new AdminDAO();

    public static synchronized AnalysisDataService getInstance() {
        if (theInstance == null) {
            theInstance = new AnalysisDataService();
        }
        return theInstance;

    }

    /**
     * Create a new job and save it to the database
     * 
     * @param taskID -
     *            the task id or UNPROCESSABLE_TASKID if the task is a temporary
     *            pipeline
     * @param user_id
     * @param parameter_info
     * @param taskName -
     *            the task name if the task is a temporary pipeline
     * @param parentJobNumber -
     *            the parent job number of <tt>null</tt> if the job has no
     *            parent
     * @throws OmnigeneException
     * 
     * @return Job ID
     */
    public JobInfo addNewJob(int taskID, String user_id, String parameter_info) throws OmnigeneException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            Integer jobNo = analysisDAO.addNewJob(taskID, user_id, parameter_info, null, null, null);
            JobInfo newJob = analysisDAO.getJobInfo(jobNo);

            if (transaction != null) {
                transaction.commit();
            }

            return newJob;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }

    /**
     * 
     */
    public JobInfo addNewJob(int taskID, String user_id, String parameter_info, int parentJobNumber)
            throws OmnigeneException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            Integer jobNo = analysisDAO.addNewJob(taskID, user_id, parameter_info, null, new Integer(parentJobNumber),
                    null);
            JobInfo newJob = analysisDAO.getJobInfo(jobNo);

            if (transaction != null) {
                transaction.commit();
            }
            return newJob;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error("AnalysisHypersonicDAO:addNewJob failed " + e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }
    }

    public String getTemporaryPipelineName(int jobNumber) throws OmnigeneException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            String name = analysisDAO.getTemporaryPipelineName(jobNumber);

            if (transaction != null) {
                transaction.commit();
            }
            return name;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }
    }

    /**
     * Creates an Omnigene database entry in the analysis_job table. Unlike
     * other entries (except visualizers), this one is not dispatchable to any
     * known analysis task because it has a bogus taskID. Since it is a
     * pipeline, it is actually being invoked by a separate process, but is
     * using the rest of the infrastructure to get input files, store output
     * files, and retrieve status and result files.
     * 
     * 
     * @param userID
     *            user who owns this pipeline data instance
     * @param parameterInfo
     *            ParameterInfo array containing pipeline data file output
     *            entries
     * @param pipelineName
     *            a name for the temporary pipeline
     * @param lsid
     *            lsid of the pipeline (if it has one)
     * @throws OmnigeneException
     *             if thrown by Omnigene
     * @throws RemoteException
     *             if thrown by Omnigene
     * 
     * 
     */
    public JobInfo createTemporaryPipeline(String user_id, String parameter_info, String pipelineName, String lsid)
            throws OmnigeneException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            Integer jobNo = analysisDAO.addNewJob(BaseDAO.UNPROCESSABLE_TASKID, user_id, parameter_info, pipelineName,
                    null, lsid);
            JobInfo job = analysisDAO.getJobInfo(jobNo);

            if (transaction != null) {
                transaction.commit();
            }
            return job;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error("AnalysisHypersonicDAO:addNewJob failed " + e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }
    }

    /**
     * 
     * @param jobNo
     * @return
     */
    public JobInfo getJobInfo(int jobNo) {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            JobInfo job = analysisDAO.getJobInfo(jobNo);

            if (transaction != null) {
                transaction.commit();
            }
            return job;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error("AnalysisHypersonicDAO:addNewJob failed " + e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }
    }

    /**
     * 
     * @param date
     * @return
     * @throws OmnigeneException
     */
    public JobInfo[] getJobInfo(java.util.Date date) throws OmnigeneException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            JobInfo[] jobs = analysisDAO.getJobInfo(date);

            if (transaction != null) {
                transaction.commit();
            }

            return jobs;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }


    /**
     * 
     * @param jobId
     */
    public void deleteJob(int jobId) {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            analysisDAO.deleteJob(jobId);

            if (transaction != null) {
                transaction.commit();
            }

        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }

    public boolean resetPreviouslyRunningJobs() throws OmnigeneException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            boolean retValue = analysisDAO.resetPreviouslyRunningJobs();

            if (transaction != null) {
                transaction.commit();
            }

            return retValue;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }

    /**
     * 
     * @param sql
     * @return
     */
    public ResultSet executeSQL(String sql) {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            ResultSet resultSet = analysisDAO.executeSQL(sql);

            if (transaction != null) {
                transaction.commit();
            }

            return resultSet;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }

    /**
     * 
     * @param sql
     * @return
     */
    public int executeUpdate(String sql) {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            int ret = analysisDAO.executeUpdate(sql);

            if (transaction != null) {
                transaction.commit();
            }

            return ret;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }

    /**
     * 
     * @param jobId
     * @return
     */
    public JobInfo[] getChildren(int jobId) {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            JobInfo[] children = analysisDAO.getChildren(jobId);

            if (transaction != null) {
                transaction.commit();
            }

            return children;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }

    /**
     * 
     * @param username
     * @param maxJobNumber
     * @param maxEntries
     * @param allJobs
     * @return
     * @throws OmnigeneException
     */
    public JobInfo[] getJobs(String username, int maxJobNumber, int maxEntries, boolean allJobs)
            throws OmnigeneException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            JobInfo[] jobs = analysisDAO.getJobs(username, maxJobNumber, maxEntries, allJobs);

            if (transaction != null) {
                transaction.commit();
            }

            return jobs;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }

    /**
     * 
     */
    public void setJobDeleted(int jobNumber, boolean deleted) throws OmnigeneException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            analysisDAO.setJobDeleted(jobNumber, deleted);

            if (transaction != null) {
                transaction.commit();
            }

        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }
    }

    /**
     * 
     * @param jobId
     * @return
     * @throws OmnigeneException
     */
    public JobInfo getParent(int jobId) throws OmnigeneException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            JobInfo parent = analysisDAO.getParent(jobId);

            if (transaction != null) {
                transaction.commit();
            }

            return parent;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }
    }

    /**
     * Update job information like status and resultfilename
     * 
     * @param jobNo
     * @param jobStatusID
     * @param outputFilename
     * @throws OmnigeneException
     * @throws RemoteException
     * @return record count of updated records
     */
    public int updateJobStatus(int jobNo, int jobStatusID) throws OmnigeneException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            int count = analysisDAO.updateJobStatus(jobNo, jobStatusID);

            if (transaction != null) {
                transaction.commit();
            }

            return count;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }

    /**
     * Update job info with paramter infos and status
     * 
     * @param jobNo
     * @param parameters
     * @param jobStatusID
     * @return number of record updated
     * @throws OmnigeneException
     * @throws RemoteException
     */
    public int updateJob(int jobNo, String parameters, int jobStatusID) throws OmnigeneException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            int count = analysisDAO.updateJob(jobNo, parameters, jobStatusID);

            if (transaction != null) {
                transaction.commit();
            }

            return count;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }



    public int getNextLSIDIdentifier(String namespace) throws OmnigeneException {
        if (GPConstants.TASK_NAMESPACE.equals(namespace)) {
            return analysisDAO.getNextTaskLSIDIdentifier();
        }
        else if (GPConstants.SUITE_NAMESPACE.equals(namespace)) {
            return analysisDAO.getNextSuiteLSIDIdentifier();
        }
        else {
            throw new OmnigeneException("unknown Namespace for LSID: " + namespace);
        }

    }

    /**
     * get the next available LSID version for a given identifer from the
     * database
     * 
     * @throws OmnigeneException
     * @throws RemoteException
     * @return int next version in sequence
     */
    public String getNextLSIDVersion(LSID lsid) throws OmnigeneException {
        String namespace = lsid.getNamespace();
        if (GPConstants.SUITE_NAMESPACE.equals(namespace)) {
            return analysisDAO.getNextSuiteLSIDVersion(lsid);
        }
        else {
            return analysisDAO.getNextTaskLSIDVersion(lsid);
        }
    }

    /**
     * 
     * @param maxJobCount
     *            max. job count
     * @throws OmnigeneException
     * @throws RemoteException
     * @return JobInfo Vector
     */
    public Vector getWaitingJob(int maxJobCount) throws OmnigeneException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            Vector jobs = analysisDAO.getWaitingJob(maxJobCount);

            if (transaction != null) {
                transaction.commit();
            }

            return jobs;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }

    /**
     * 
     */
    public JobInfo recordClientJob(int taskID, String user_id, String parameter_info) throws OmnigeneException {
        return recordClientJob(taskID, user_id, parameter_info, -1);
    }

    /**
     * 
     */
    public JobInfo recordClientJob(int taskID, String user_id, String parameter_info, int parentJobNumber)
            throws OmnigeneException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            Integer jobNo = analysisDAO.recordClientJob(taskID, user_id, parameter_info, parentJobNumber);
            JobInfo clientJob = analysisDAO.getJobInfo(jobNo);

            if (transaction != null) {
                transaction.commit();
            }

            return clientJob;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }

}
