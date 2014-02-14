package org.genepattern.server.job.input.dao;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import com.google.common.base.Objects;

@Embeddable
public class JobInputValuePk implements Serializable {
    @Column(name="gp_job_no")
    private Integer gpJobNo;
    private String pname;
    private Integer idx;
    
    public JobInputValuePk() {
    }

    public Integer getGpJobNo() {
        return gpJobNo;
    }

    public void setGpJobNo(Integer gpJobNo) {
        this.gpJobNo = gpJobNo;
    }

    public String getPname() {
        return pname;
    }

    public void setPname(String pname) {
        this.pname = pname;
    }

    public Integer getIdx() {
        return idx;
    }

    public void setIdx(Integer idx) {
        this.idx = idx;
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof JobInputValuePk)) {
            return false;
        }
        JobInputValuePk arg = (JobInputValuePk) obj;
        return Objects.equal(gpJobNo, arg.gpJobNo) &&
                Objects.equal(pname, arg.pname) &&
                Objects.equal(idx, arg.idx);
    }
    public int hashCode() {
        return Objects.hashCode(gpJobNo, pname, idx);
    }
}
