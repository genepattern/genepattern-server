/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.webservice;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.AnalysisJob;

/**
 * Used to hold information about particular job
 * 
 * @author Rajesh Kuttan, Hui Gong
 * @version $Revision 1.7$
 */

public class JobInfo implements Serializable {
    public static Logger log = Logger.getLogger(JobInfo.class);

    private int jobNo = 0;
    private int parentJobNo = -1;
    private int taskID = 0;
    private String status = "";
    private ParameterInfo[] parameterInfoArray = null;
    private Date submittedDate = null;
    private Date completedDate = null;
    private String userId = null;
    private String lsid = null;
    private String taskName;
    private boolean deleted = false;

    public JobInfo() {
        parameterInfoArray = new ParameterInfo[0];
    }
    
    /**
     * @param jobNo
     * @param taskID
     * @param status
     * @param submittedDate
     * @param completedDate
     * @param parameters
     * @param userId
     * @param lsid
     */
    public JobInfo(int jobNo, int taskID, String status, Date submittedDate, Date completedDate,
            ParameterInfo[] parameters, String userId, String lsid, String taskName) {
        this.jobNo = jobNo;
        this.taskID = taskID;
        this.status = status;
        this.submittedDate = submittedDate;
        this.completedDate = completedDate;
        this.parameterInfoArray = parameters;
        this.userId = userId;
        this.lsid = lsid;
        this.taskName = taskName;
    }

    
    /**
     * Construct a JobInfo object from an AnalysisJob.
     * @param aJob
     * @throws IllegalArgumentException if the AnalysisJob is missing a required parameter.
     */
    public JobInfo(AnalysisJob aJob) {
        if (aJob == null) {
            throw new IllegalArgumentException("Invalid null arg in constructor, JobInfo(AnalysisJob)");
        }
        if (aJob.getJobNo() == null) {
            throw new IllegalArgumentException("aJob.jobNo is null");
        }

        String statusName = "";
        if (aJob.getJobStatus() == null) {
            log.error("aJob.jobStatus is null, jobNo="+aJob.getJobNo());
            if (log.isDebugEnabled()) {
                log.debug(new Exception("aJob.jobStatus is null, jobNo="+aJob.getJobNo()));
            }
        }
        else {
            statusName = aJob.getJobStatus().getStatusName();
        }
        
        this.jobNo = aJob.getJobNo().intValue();
        this.taskID = aJob.getTaskId();
        this.status = statusName;
        this.submittedDate = aJob.getSubmittedDate();
        this.completedDate = aJob.getCompletedDate();
        this.parameterInfoArray = ParameterFormatConverter.getParameterInfoArray(aJob.getParameterInfo());
        this.userId = aJob.getUserId();
        this.lsid = aJob.getTaskLsid();
        this.taskName = aJob.getTaskName();

        this.parentJobNo = aJob.getParent();
        this.deleted = aJob.getDeleted();
    }

    /**
     * Removes all parameters with the given name.
     * 
     * @param parameterInfoName, the parameter name.
     * @return true if the parameter was found; false otherwise.
     */
    public boolean removeParameterInfo(String parameterInfoName) {
        if (parameterInfoArray == null) {
            return false;
        }
        List<ParameterInfo> newParameterInfoList = new ArrayList<ParameterInfo>();
        int sizeBeforePossibleRemoval = parameterInfoArray.length;

        for (int i = 0, length = parameterInfoArray.length; i < length; i++) {
            if (!parameterInfoArray[i].getName().equals(parameterInfoName)) {
                newParameterInfoList.add(parameterInfoArray[i]);
            }
        }

        parameterInfoArray = (ParameterInfo[]) newParameterInfoList.toArray(new ParameterInfo[0]);
        return (parameterInfoArray.length < sizeBeforePossibleRemoval);

    }

    /**
     * @return jobNo
     */
    public int getJobNumber() {
        return jobNo;
    }

    /**
     * @param jobNo
     */
    public void setJobNumber(int jobNo) {
        this.jobNo = jobNo;
    }

    /**
     * Note: To preserve compatibility with earlier versions of the SOAP client,
     *     This method deliberately named using a non JavaBean naming convention so that the axis serializer does not
     *     include the parentJobNo in the serialized bean.
     * @return the jobNumber of the parent job, or a number less than zero if the job has no parent.
     */
    public int _getParentJobNumber() {
        return this.parentJobNo;
    }

    /**
     * @return status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return taskID
     */
    public int getTaskID() {
        return taskID;
    }

    /**
     * @param taskID
     */
    public void setTaskID(int taskID) {
        this.taskID = taskID;
    }

    /**
     * @return taskLSID
     */
    public String getTaskLSID() {
        return lsid;
    }

    /**
     * @param taskLSID
     */
    public void setTaskLSID(String lsid) {
        this.lsid = lsid;
    }

    /**
     * @return the task name
     */
    public String getTaskName() {
        return taskName;
    }

    /**
     * @param taskName
     */
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    /**
     * @return submittedDate
     */
    public Date getDateSubmitted() {
        return submittedDate;
    }

    /**
     * @param submittedDate
     */
    public void setDateSubmitted(Date submittedDate) {
        this.submittedDate = submittedDate;
    }

    /**
     * @return completedDate
     */
    public Date getDateCompleted() {
        return completedDate;
    }

    /**
     * @param completedDate
     */
    public void setDateCompleted(Date completedDate) {
        this.completedDate = completedDate;
    }

    /**
     * get parameter info jaxb string
     * 
     * @return parameter_info
     */
    public String getParameterInfo() throws OmnigeneException {
        String parameter_info = "";
        if (this.parameterInfoArray != null) {
            parameter_info = ParameterFormatConverter.getJaxbString(this.parameterInfoArray);
        }
        return parameter_info;
    }

    /**
     * get <CODE>ParameterInfo</CODE> array
     * 
     * @return parameterInfoArray
     */
    public ParameterInfo[] getParameterInfoArray() {
        return parameterInfoArray;
    }

    /**
     * set <CODE>ParameterInfo</CODE> array
     * 
     * @param parameterInfoArray
     */
    public void setParameterInfoArray(ParameterInfo[] parameterInfoArray) {
        this.parameterInfoArray = parameterInfoArray;
    }

    /**
     * Add new ParameterInfo into the JobInfo
     * 
     * @param param
     */
    public void addParameterInfo(ParameterInfo param) {
        int size = 0;
        if (this.parameterInfoArray != null) size = this.parameterInfoArray.length;
        ParameterInfo[] params = new ParameterInfo[size + 1];
        for (int i = 0; i < size; i++) {
            params[i] = this.parameterInfoArray[i];
        }
        params[size] = param;
        this.parameterInfoArray = params;
    }

    /**
     * Checks to see if the JobInfo contains a input file parameter field
     * 
     * @return true if it contains a
     *         <code>ParamterInfo<code> object with TYPE as FILE and MODE as INPUT
     */
    public boolean containsInputFileParam() {
        if (this.parameterInfoArray != null) {
            for (int i = 0; i < this.parameterInfoArray.length; i++) {
                if (this.parameterInfoArray[i].isInputFile()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks to see if the JobInfo contains a output file parameter field
     * 
     * @return true if it contains a
     *         <code>ParamterInfo<code> object with TYPE as FILE and MODE as OUTPUT
     */
    public boolean containsOutputFileParam() {
        if (this.parameterInfoArray != null) {
            for (int i = 0; i < this.parameterInfoArray.length; i++) {
                if (this.parameterInfoArray[i].isOutputFile()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return this.userId;
    }

    /** standard method */
    public String toString() {
        return "JobInfo[jobNo=" + jobNo + " taskID=" + taskID + " status=" + status + " submittedDate=" + submittedDate
                + " completedDate=" + completedDate + " userId=" + userId + " parameterInfoArray=" + parameterInfoArray
                + "]";
    }

}
