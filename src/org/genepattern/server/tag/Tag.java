/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.tag;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import javax.persistence.*;
import javax.validation.constraints.Size;

import java.util.Date;

/**
 * Created by nazaire on 10/7/14.
 */
@Entity
@Table(name="tag")
public class Tag
{
    private static final Logger log = Logger.getLogger(Tag.class);
    /** DB max length of the status_message column, e.g. varchar2(2000) in Oracle. */
    public static final int TAG_LENGTH=511;

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
    @Column(name="tag_id")
    private int id;

    @Column(name="tag", nullable=false, length=TAG_LENGTH)
    @Size(max=TAG_LENGTH)
    private String tag;

    @Column(name="date_added", nullable=false)
    private Date dateAdded;

    @Column(name="user_id", nullable=false, length=255)
    private String userId;

    @Column(name="public_tag", nullable=false)
    boolean publicTag;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) { this.tag = truncate(tag, TAG_LENGTH); }

    public boolean isPublicTag() { return publicTag; }

    public void setPublicTag(boolean publicTag) { this.publicTag = publicTag; }

    public Date getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(Date date) {
        this.dateAdded = date;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public JSONObject toJSON() throws JSONException
    {
        JSONObject tag = new JSONObject();
        tag.put("id", getId());
        tag.put("user_id", getUserId());
        tag.put("date_added", getDateAdded());
        tag.put("tag", getTag());
        tag.put("is_public", isPublicTag());
        return tag;
    }
}

