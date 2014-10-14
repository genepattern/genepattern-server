package org.genepattern.server.tag;

import org.apache.log4j.Logger;
import org.genepattern.server.tag.dao.TagDao;

import java.util.Date;
import java.util.List;

/**
 * Created by nazaire on 10/13/14.
 */
public class TagManager
{
    private static Logger log = Logger.getLogger(TagManager.class);

    static public List<Tag> selectAllJobTags(String userId, boolean isPublic)
    {
        TagDao tagDao  = new TagDao();

        return tagDao.selectTagsAvailableToUser(userId, isPublic);
    }

}
