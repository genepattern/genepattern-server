package org.genepattern.server.jobqueue;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;


@Entity
@Table(name="job_queue", uniqueConstraints = {@UniqueConstraint(columnNames={"job_no"})})
public class JobQueue {

    //TODO: this is a foreign key to ANALYSIS_JOB.JOB_NO
    @Id
    @Column(name = "job_no")
    private int jobNo;
    
    @Column(name = "parent_job_no")
    private int parentJobNo = -1;
    
    @Column(name = "status")
    private String status;
    
    
    //private String path;
    //private String name;
    
    @Column(name = "date_submitted")
    private Date dateSubmitted;

    //@Column(name = "file_length")
    //private long fileLength;
    //private String extension;
    //private String kind;
    
    //@Column(name = "num_parts")
    //private int numParts = 1;
    
    //@Column(name = "num_parts_recd")
    //private int numPartsRecd = 0;
    
    
//    public long getId() {
//        return id;
//    }
//
//    public void setId(long id) {
//        this.id = id;
//    }

    public int getJobNo() {
        return jobNo;
    }

    public void setJobNo(int jobNo) {
        this.jobNo = jobNo;
    }
    
    public int getParentJobNo() {
        return parentJobNo;
    }
    public void setParentJobNo(int i) {
        this.parentJobNo = i;
    }
    
    public String getStatus() {
        return this.status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public Date getDatSubmitted() {
        return dateSubmitted;
    }

    public void setDateSubmitted(Date date) {
        this.dateSubmitted = date;
    }


}
