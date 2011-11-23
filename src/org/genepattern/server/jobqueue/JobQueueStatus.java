package org.genepattern.server.jobqueue;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * So as to not break compatibility with the SOAP interface, 
 * I created this class to represent new status codes needed
 * for pipeline execution.
 * 
 * @author pcarr
 *
 */
@Entity
@Table(name="job_queue_status")
public class JobQueueStatus {
    public enum Status {
        WAITING,
        PENDING,
        DISPATCHING
    }
    
    @Column(name = "job_no", unique = true)
    private int jobNo;
    
    @Column(name = "parent_job_no")
    private int parentJobNo;

    @Column(name = "submitted_date")
    private Date submittedDate;
    
    @Column(name = "status")
    private String status = Status.PENDING.toString();
    
    public JobQueueStatus() {
    }

    public void setJobNo(int jobNo) {
        this.jobNo = jobNo;
    }
    public int getJobNo() {
        return jobNo;
    }
    
    public void setParentJobNo(int parentJobNo) {
        this.parentJobNo = parentJobNo;
    }
    public int getParentJobNo() {
        return this.parentJobNo;
    }

    public void setStatus(Status statusId) {
        this.status = statusId.toString();
    }
    public String getStatus() {
        return status;
    }
    
    public void setSubmittedDate(Date date) {
        this.submittedDate = date;
    }
    public Date getSubmittedDate() {
        return this.submittedDate;
    }
}
