package org.genepattern.server.dm.jobresult;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.AnalysisJob;

@Entity
@Table(name="job_result", uniqueConstraints = {@UniqueConstraint(columnNames={"job_id", "name"})})
public class JobResult {
    
    @Id
    @GeneratedValue
    private long id;
    @Column(name = "job_id")
    private int jobId;
    private String name;
    private String path;
    private boolean log = false;
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public int getJobId() {
        return jobId;
    }
    
    public void setJobId(int job) {
        this.jobId = job;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isLog() {
        return log;
    }

    public void setLog(boolean log) {
        this.log = log;
    }
}
