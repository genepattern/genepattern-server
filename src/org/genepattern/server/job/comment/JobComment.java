package org.genepattern.server.job.comment;

import org.apache.log4j.Logger;
import org.hibernate.validator.Size;

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
    @Column(name="gp_job_no")
    private int gpJobNo;

    @Column(name="parent_id")
    private int parentId;

    @Column(name="posted_date", nullable=false)
    private Date postedDate;

    @Column(name="user_id", nullable=false, length=255)
    private String userId;

    @Column(name="comment_text", nullable=false, length=COMMENT_TEXT_LENGTH)
    @Size(max=COMMENT_TEXT_LENGTH)
    private String comment;

    public int getGpJobNo()
    {
        return gpJobNo;
    }

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

    public void setGpJobNo(final int gpJobNo)
    {
        this.gpJobNo = gpJobNo;
    }

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
}
