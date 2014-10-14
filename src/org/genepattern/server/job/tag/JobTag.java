package org.genepattern.server.job.tag;

import org.apache.log4j.Logger;
import org.genepattern.server.tag.Tag;

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

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "tag_id", nullable=false)
    private Tag tagObj;

    //this is a foreign key to the analysis_job table
    @Column(name="gp_job_no", nullable=false)
    private int gpJobNo;

    @Column(name="date_tagged", nullable=false)
    private Date dateTagged;

    @Column(name="user_id", nullable=false, length=255)
    private String userId;

    public int getId() { return id; }

    public void setId(int id) {
        this.id = id;
    }

    public Tag getTagObj() { return tagObj; }

    public void setTagObj(Tag tag) {
        this.tagObj = tag;
    }

    public int getGpJobNo() {
        return gpJobNo;
    }

    public void setGpJobNo(int gpJobNo) {
        this.gpJobNo = gpJobNo;
    }

    public Date getDateTagged() {
        return dateTagged;
    }

    public void setDateTagged(Date date) {
        this.dateTagged = date;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
