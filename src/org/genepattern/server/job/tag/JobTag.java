package org.genepattern.server.job.tag;

import org.apache.log4j.Logger;
import javax.persistence.*;
import java.util.Date;

/**
 * Created by nazaire on 10/7/14.
 */
@Entity
@Table(name="job_tag")
public class JobTag
{
    private static final Logger log = Logger.getLogger(JobTag.class);

    @Id
    @GeneratedValue
    private int id;

    //this is a foreign key to the tag table
    @Column(name="tag_id", nullable=false)
    private int tagId;

    //this is a foreign key to the analysis_job table
    @Column(name="gp_job_no", nullable=false)
    private int gpJobNo;

    @Column(name="date", nullable=false)
    private Date date;

    @Column(name="user_id", nullable=false, length=255)
    private String userId;

    public int getId() { return id; }

    public void setId(int id) {
        this.id = id;
    }

    public int getTagId() {
        return tagId;
    }

    public void setTagId(int tagId) {
        this.tagId = tagId;
    }

    public int getGpJobNo() {
        return gpJobNo;
    }

    public void setGpJobNo(int gpJobNo) {
        this.gpJobNo = gpJobNo;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
