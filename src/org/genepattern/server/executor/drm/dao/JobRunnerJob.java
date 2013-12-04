package org.genepattern.server.executor.drm.dao;

import java.io.File;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.genepattern.server.executor.drm.DrmJobStatus;
import org.genepattern.server.executor.drm.JobState;
import org.genepattern.webservice.JobInfo;

/**
 * An record (for the the DB or in a runtime cache) used by the DrmLookup class for recording the status of an
 * external job.
 * 
 * @author pcarr
 *
 */
@Entity
@Table(name="job_runner_job",
       uniqueConstraints=@UniqueConstraint(columnNames={"jr_classname", "jr_name", "ext_job_id"}))
public class JobRunnerJob {
    //this is a foreign key to the analysis_job table
    @Id
    @Column(name="gp_job_no")
    private Integer gpJobNo;

    @Column(name="jr_classname", nullable=false)
    private String jobRunnerClassname;

    @Column(name="jr_name", nullable=false)
    private String jobRunnerName;

    @Column(name="ext_job_id", nullable=false)
    private String extJobId;
    
    @Column(name="job_state", nullable=true)
    private String jobState;

    @Column(name="status_message", nullable=true)
    private String statusMessage;
    
    @Column(name="status_date", nullable=false)
    private Date statusDate=new Date();

    @Column(name="working_dir", nullable=false)
    private String workingDir;

    public JobRunnerJob(final String jobRunnerClassname, final String jobRunnerId, final File workingDir, final JobInfo jobInfo) {
        this(jobRunnerClassname, jobRunnerId, workingDir, jobInfo.getJobNumber());
    }
    
    public JobRunnerJob(final String jobRunnerClassname, final String jobRunnerName, final File workingDir, final Integer gpJobNo) {
        this.jobRunnerClassname=jobRunnerClassname;
        this.jobRunnerName=jobRunnerName;
        this.gpJobNo=gpJobNo;
        this.workingDir=workingDir.getAbsolutePath();
        this.extJobId="";
        this.jobState=JobState.UNDETERMINED.name();
        this.statusMessage=jobState.toString();
    }
    
    public JobRunnerJob(final JobRunnerJob in, final DrmJobStatus updated) {
        this.jobRunnerClassname=in.jobRunnerClassname;
        this.jobRunnerName=in.jobRunnerName;
        this.gpJobNo=in.gpJobNo;
        this.workingDir=in.workingDir;
        this.extJobId=updated.getDrmJobId();
        this.jobState=updated.getJobState().name();
        this.statusMessage=updated.getJobStatusMessage();
    }
    
    public Integer getGpJobNo() {
        return gpJobNo;
    }

    public String getJobRunnerClassname() {
        return jobRunnerClassname;
    }

    public String getJobRunnerName() {
        return jobRunnerName;
    }

    public String getExtJobId() {
        return extJobId;
    }

    public String getJobState() {
        return jobState;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public Date getStatusDate() {
        return statusDate;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    //private no-arg constructor for hibernate
    private JobRunnerJob() {
    }
    
    //private setters for hibernate, making this class 'kind-of' immutable 
    private void setGpJobNo(Integer gpJobNo) {
        this.gpJobNo = gpJobNo;
    }

    private void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    private void setJobRunnerClassname(String jobRunnerClassname) {
        this.jobRunnerClassname = jobRunnerClassname;
    }

    private void setJobRunnerName(String jobRunnerName) {
        this.jobRunnerName = jobRunnerName;
    }

    private void setExtJobId(String extJobId) {
        this.extJobId = extJobId;
    }

    private void setJobState(String jobState) {
        this.jobState = jobState;
    }

    private void setStatusMessage(String extJobStatusMessage) {
        this.statusMessage = extJobStatusMessage;
    }

    private void setStatusDate(Date timestamp) {
        this.statusDate = timestamp;
    }
    
}