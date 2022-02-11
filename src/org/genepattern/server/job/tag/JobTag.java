/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.tag;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.tag.Tag;

import javax.persistence.*;
import java.util.Date;

import org.hibernate.annotations.Cascade;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nazaire on 10/7/14.
 */
@Entity
@Table(name="job_tag")
public class JobTag
{
    //private static final Logger log = Logger.getLogger(JobTag.class);

    @Id
    @GeneratedValue
    private int id;

    @OneToOne(fetch = FetchType.EAGER)
    @Cascade(value=org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    @JoinColumn(name = "tag_id", nullable=false)
    private Tag tagObj;

    //this is a foreign key to the analysis_job table
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gp_job_no", nullable=false)
    private AnalysisJob analysisJob;

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

    public AnalysisJob getAnalysisJob() { return analysisJob; }

    public void setAnalysisJob(AnalysisJob analysisJob) {
        this.analysisJob = analysisJob;
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

    public JSONObject toJson() throws JSONException
    {
        JSONObject jobTag = new JSONObject();
        jobTag.put("id", getId());
        jobTag.put("user_id", getUserId());
        jobTag.put("date_tagged", getDateTagged());
        jobTag.put("tag", getTagObj().toJSON());
        return jobTag;
    }
}
