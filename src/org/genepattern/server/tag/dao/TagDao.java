/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.tag.dao;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.tag.Tag;
import org.hibernate.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nazaire on 10/8/14.
 */
public class TagDao
{
    private static final Logger log = Logger.getLogger(TagDao.class);
    
    private final HibernateSessionManager mgr;
    
    public TagDao(final HibernateSessionManager mgr) {
        this.mgr=mgr;
    }

    public void insertTag(final Tag tag)
    {
        if (tag==null) {
            log.error("No entry to update");
            return;
        }

        final boolean isInTransaction= mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            mgr.getSession().save(tag);

            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
        catch (Throwable t)
        {
            log.error("Error adding tag=" + tag.getTag(), t);
            mgr.rollbackTransaction();
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

    public Tag selectTagById(final int tagId) {
        Tag tag = null;
        final boolean isInTransaction= mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            tag = (Tag)mgr.getSession().get(Tag.class, tagId);

            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error("Error getting tag with id=" + tagId,t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }

        return tag;
    }

    @SuppressWarnings("unchecked")
    public List<Tag> selectTagsAvailableToUser(final String userId, final boolean includePublicTags) {
        final boolean isInTransaction= mgr.isInTransaction();
        try {
            mgr.beginTransaction();

            String hql = "from "+ Tag.class.getName()+" jt where jt.userId = :userId";
            if(includePublicTags)
            {
                hql += " or jt.publicTag = :publicTag";
            }

            Query query = mgr.getSession().createQuery( hql );
            query.setString("userId", userId);

            if(includePublicTags)
            {
                query.setBoolean("publicTag", includePublicTags);
            }

            final List<Tag> rval=query.list();
            return rval;
        }
        catch (Throwable t) {
            log.error("Error getting tags available to user " + userId,t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }

        final List<Tag> tagList = new ArrayList<Tag>();
        return tagList;
    }

    public boolean deleteTag(final int id)
    {
        boolean deleted = false;
        final boolean isInTransaction= mgr.isInTransaction();
        try {
            mgr.beginTransaction();

            Tag tag = (Tag)mgr.getSession().get(Tag.class, Integer.valueOf(id));
            if(tag == null)
            {
                //log error and do nothing
                log.error("Error retrieving tag with id="+id);
                return deleted;
            }

            mgr.getSession().delete(tag);

            if (!isInTransaction) {
                mgr.commitTransaction();
            }

            deleted = true;
        }
        catch (Throwable t) {
            log.error("Error deleting tag wih id="+id, t);
            mgr.rollbackTransaction();
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }

        return deleted;
    }
}
