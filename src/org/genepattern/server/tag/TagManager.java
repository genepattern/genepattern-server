/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.tag;

import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.tag.dao.TagDao;

import java.util.List;

/**
 * Created by nazaire on 10/13/14.
 */
public class TagManager
{

    public static List<Tag> selectAllJobTags(final HibernateSessionManager mgr, final String userId, final boolean isPublic)
    {
        TagDao tagDao  = new TagDao(mgr);
        return tagDao.selectTagsAvailableToUser(userId, isPublic);
    }

}
