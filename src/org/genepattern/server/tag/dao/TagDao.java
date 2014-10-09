package org.genepattern.server.tag.dao;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
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

    public void insertTag(final Tag tag)
    {
        final boolean isInTransaction= HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            HibernateUtil.getSession().save(tag);

            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {

            String tagText = null;
            if(tag != null)
            {
                tagText = tag.getTag();
            }

            log.error("Error adding tag=" + tagText, t);
            HibernateUtil.rollbackTransaction();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

    public Tag selectTagById(int tagId) {
        Tag tag = null;
        final boolean isInTransaction= HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            tag = (Tag)HibernateUtil.getSession().get(Tag.class, tagId);

            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error("Error getting tag with id=" + tagId,t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }

        return tag;
    }

    public List<Tag> selectAllTags(boolean includePrivate) {
        List<Tag> tagList = new ArrayList();
        final boolean isInTransaction= HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();

            String hql = "from "+Tag.class.getName()+" jt";
            if(!includePrivate)
            {
                hql += " where jt.privateTag = :includePrivate";
            }

            Query query = HibernateUtil.getSession().createQuery( hql );
            tagList = query.list();
        }
        catch (Throwable t) {
            log.error("Error getting tags",t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }

        return tagList;
    }

    public List<Tag> selectTagsAvailableToUser(String userId, boolean includePublicTags) {
        List<Tag> tagList = new ArrayList();
        final boolean isInTransaction= HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();

            String hql = "from "+ Tag.class.getName()+" jt where jt.userId = :userId";
            if(includePublicTags)
            {
                hql += " or jt.publicTag = :publicTag";
            }

            Query query = HibernateUtil.getSession().createQuery( hql );
            query.setString("userId", userId);

            if(includePublicTags)
            {
                query.setBoolean("publicTag", includePublicTags);
            }

            tagList = query.list();
        }
        catch (Throwable t) {
            log.error("Error getting tags available to user " + userId,t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }

        return tagList;
    }

    public boolean deleteTag(int id)
    {
        boolean deleted = false;
        final boolean isInTransaction= HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();

            Tag tag = (Tag)HibernateUtil.getSession().get(Tag.class, Integer.valueOf(id));
            if(tag == null)
            {
                //log error and do nothing
                log.error("Error retrieving tag with id="+id);
                return deleted;
            }

            HibernateUtil.getSession().delete(tag);

            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }

            deleted = true;
        }
        catch (Throwable t) {
            log.error("Error deleting tag wih id="+id, t);
            HibernateUtil.rollbackTransaction();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }

        return deleted;
    }
}
