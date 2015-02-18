package org.genepattern.server.job.comment;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.webapp.rest.api.v1.DateUtil;
import org.hibernate.validator.Size;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.persistence.*;
import java.util.Date;


/**
 * A record (for the the DB or in a runtime cache) used by the DrmLookup class for recording the comments for an
 * external job.
 *
 * @author nazaire
 *
 */
@Entity
@Table(name="job_comment")
public class JobComment {
    private static final Logger log = Logger.getLogger(JobComment.class);

    /** DB max length of the status_message column, e.g. varchar2(2000) in Oracle. */
    public static final int COMMENT_TEXT_LENGTH=1023;

    /**
     * Truncate the string so that it is no longer than MAX characters.
     * @param in
     * @param MAX
     * @return
     */
    public static String truncate(String in, int MAX)
    {
        if (in==null)
        {
            return in;
        }
        if (in.length() <= MAX)
        {
            return in;
        }
        if (MAX<0)
        {
            log.error("expecting value >0 for MAX="+MAX);
        }
        return in.substring(0, MAX);
    }

    @Id
    @GeneratedValue
    private int id;

    //this is a foreign key to the analysis_job table
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gp_job_no", nullable=false)
    private AnalysisJob analysisJob;

    @Column(name="parent_id")
    private int parentId;

    @Column(name="posted_date", nullable=false)
    private Date postedDate;

    @Column(name="user_id", nullable=false, length=255)
    private String userId;

    @Column(name="comment_text", nullable=false, length=COMMENT_TEXT_LENGTH)
    @Size(max=COMMENT_TEXT_LENGTH)
    private String comment;

    public AnalysisJob getAnalysisJob() { return analysisJob; }

    public int getId()
    {
        return id;
    }

    public int getParentId()
    {
        return parentId;
    }

    public Date getPostedDate()
    {
        return postedDate;
    }

    public String getUserId()
    {
        return userId;
    }

    public String getComment()
    {
        return comment;
    }

    public void setAnalysisJob(AnalysisJob analysisJob) { this.analysisJob = analysisJob; }

    public void setCommentId(final int commentId)
    {
        this.id = commentId;
    }

    public void setParentId(final int parentId)
    {
        this.parentId = parentId;
    }

    public void setPostedDate(final Date postedDate)
    {
        this.postedDate = postedDate;
    }

    public void setUserId(final String userId)
    {
        this.userId = userId;
    }

    public void setComment(final String comment)
    {
        this.comment = comment;
    }

    public JSONObject toJson() throws JSONException
    {
        JSONObject jobComment = new JSONObject();
        jobComment.put("comment_id", getId());
        jobComment.put("parent_id", getParentId());
        jobComment.put("created_by", getUserId());
        jobComment.put("fullname", getUserId());
        jobComment.put("posted_date", DateUtil.toTimeAgoUtc(getPostedDate()));
        jobComment.put("text", getComment());
        jobComment.put("childrens", new JSONArray());

        return jobComment;
    }
}
