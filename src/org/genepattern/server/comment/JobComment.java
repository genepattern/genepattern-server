package org.genepattern.server.comment;

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
    private long id;

    //this is a foreign key to the analysis_job table
    @Id
    @Column(name="gp_job_no")
    private int gpJobNo;

    @Column(name="parent_id")
    private long parentId;

    @Column(name="posted_date", nullable=false)
    private Date postedDate;

    @Column(name="user_id", nullable=false, length=255)
    private String userId;

    @Column(name="comment_text", nullable=false, length=COMMENT_TEXT_LENGTH)
    @Size(max=COMMENT_TEXT_LENGTH)
    private String comment;


    private JobComment(final Builder builder)
    {
        this.id=builder.id;
        this.gpJobNo=builder.gpJobNo;
        this.parentId=builder.parentId;
        this.postedDate=builder.postedDate;
        this.userId=builder.userId;
        this.comment=builder.comment;
    }

    public static final class Builder
    {
        private int gpJobNo;
        private long id;
        private long parentId;
        private Date postedDate;
        private String userId="";
        private String comment=""; // null means 'not set'


        public Builder()
        {
        }

        public Builder(final JobComment in)
        {
            this.gpJobNo=in.gpJobNo;
            this.gpJobNo=in.gpJobNo;
            this.id=in.id;
            this.parentId=in.parentId;
            this.postedDate=in.postedDate;
            this.userId=in.userId;
            this.comment=in.comment;
        }


        public Builder gpJobNo(int gpJobNo)
        {
            this.gpJobNo=gpJobNo;
            return this;
        }

        public Builder id(final int id)
        {
            this.id=id;
            return this;
        }

        public Builder parentId(final Integer parentId)
        {
            this.parentId=parentId;
            return this;
        }

        public Builder postedDate(final Date postedDate)
        {
            this.postedDate=postedDate;
            return this;
        }

        public Builder userId(final String userId)
        {
            this.userId = userId;
            return this;
        }


        public Builder comment(final String commentIn)
        {
            this.comment=truncate(commentIn, COMMENT_TEXT_LENGTH);
            if (log.isDebugEnabled() && commentIn.length() > COMMENT_TEXT_LENGTH)
            {
                log.warn("truncating comment because it is greater than max DB length="+COMMENT_TEXT_LENGTH);
            }
            return this;
        }

        public JobComment build()
        {
            return new JobComment(this);
        }
    }

    public Integer getGpJobNo()
    {
        return gpJobNo;
    }

    public long getId()
    {
        return id;
    }

    public long getParentId()
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

    //private no-arg constructor for hibernate
    private JobComment()
    {
    }

    //private setters for hibernate, making this class 'kind-of' immutable
    private void setGpJobNo(final int gpJobNo)
    {
        this.gpJobNo = gpJobNo;
    }

    private void setCommentId(final long commentId)
    {
        this.id = commentId;
    }

    private void setParentId(final long parentId)
    {
        this.parentId = parentId;
    }

    private void setPostedDate(final Date postedDate)
    {
        this.postedDate = postedDate;
    }

    private void setUserId(final String userId)
    {
        this.userId = userId;
    }

    private void setComment(final String comment)
    {
        this.comment = comment;
    }
}
