package org.genepattern.server.job.output;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import com.google.common.base.Objects;

@Embeddable
public class JobOutputFilePk implements Serializable {

    @Column(name="gp_job_no")
    private Integer gpJobNo;
    private String path;
    
    public JobOutputFilePk() {
    }
    public JobOutputFilePk(Integer gpJobNo, String path) {
        this.gpJobNo=gpJobNo;
        this.path=path;
    }

    public Integer getGpJobNo() {
        return gpJobNo;
    }

    public void setGpJobNo(Integer gpJobNo) {
        this.gpJobNo = gpJobNo;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof JobOutputFilePk)) {
            return false;
        }
        JobOutputFilePk arg = (JobOutputFilePk) obj;
        return Objects.equal(gpJobNo, arg.gpJobNo) &&
                Objects.equal(path, arg.path);
    }
    
    public int hashCode() {
        return Objects.hashCode(gpJobNo, path);
    }
}
