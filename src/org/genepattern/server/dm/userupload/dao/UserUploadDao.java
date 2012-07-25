package org.genepattern.server.dm.userupload.dao;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFilePath;
import org.hibernate.Query;

public class UserUploadDao extends BaseDAO {
    
    /**
     * Get the user_upload record from the DB for the given GpFilePath.
     * 
     * @param gpFileObj
     * @return a managed UserUpload instance or null of no matching path was found for the user.
     */
    public UserUpload selectUserUpload(String userId, GpFilePath gpFileObj) {
        String relativePath = gpFileObj.getRelativePath();
        return selectUserUpload(userId, relativePath);
    }
    
    private UserUpload selectUserUpload(String userId, String relativePath) {
        String hql = "from "+UserUpload.class.getName()+" uu where uu.userId = :userId and path = :path";
        Query query = HibernateUtil.getSession().createQuery( hql );
        query.setString("userId", userId);
        query.setString("path", relativePath);
        List<UserUpload> rval = query.list();
        if (rval != null && rval.size() == 1) {
            return rval.get(0);
        }
        return null;
    }

    /**
     * Get all user upload files for the given user.
     * @param userId
     * @return
     */
    public List<UserUpload> selectAllUserUpload(String userId) {
        if (userId == null) return Collections.emptyList();
        String hql = "from "+UserUpload.class.getName()+" uu where uu.userId = :userId order by uu.path";
        Query query = HibernateUtil.getSession().createQuery( hql );
        query.setString("userId", userId);
        List<UserUpload> rval = query.list();
        return rval;
    }
    
    /**
     * Get all user upload files for the given user whose path is prefixed with the given parentPath.
     * 
     * @param userId
     * @param parentPath
     * 
     * @return
     */
    public List<UserUpload> selectAllUserUpload(String userId, String parentPath) {
        if (userId == null) return Collections.emptyList();
        String hql = "from "+UserUpload.class.getName()+" uu where uu.userId = :userId and uu.path like :path order by uu.path";
        Query query = HibernateUtil.getSession().createQuery( hql );
        query.setString("userId", userId);
        parentPath = parentPath + "%";
        query.setString("path", parentPath);
        List<UserUpload> rval = query.list();
        return rval;
    }

    /**
     * Get all of the stalled partial uploads for the given user.
     * A stalled partial upload is a file record in the DB which has not finished being uploaded,
     * which has not been modified since the given olderThanDate.
     * 
     * @param userId
     * @param olderThanDate
     * @return
     */
    public List<UserUpload> selectStalledPartialUploadsForUser(String userId, Date olderThanDate) {
        if (userId == null) return Collections.emptyList();
        final String hql = "from "+UserUpload.class.getName()+
                " uu where uu.userId = :userId and uu.numParts != uu.numPartsRecd and uu.lastModified < :olderThanDate";
        Query query = HibernateUtil.getSession().createQuery( hql );
        query.setString("userId", userId);
        query.setTimestamp("olderThanDate", olderThanDate);
        //query.setDate("olderThanDate", olderThanDate);
        List<UserUpload> rval = query.list();
        return rval; 
    }
    
    
    public int deleteUserUpload(String userId, GpFilePath gpFileObj) {
        String relativePath = gpFileObj.getRelativePath();

        String hql = "delete "+UserUpload.class.getName()+" uu where uu.userId = :userId and uu.path = :path";
        Query query = HibernateUtil.getSession().createQuery( hql );
        query.setString("userId", userId);
        query.setString("path", relativePath);
        int numDeleted = query.executeUpdate();
        return numDeleted;
    }

    /**
     * Delete all the entries in the USER_UPLOAD table for the given user.
     * 
     * @param userId
     * @return
     */
    public int deleteAllUserUpload(String userId) {
        String hql = "delete "+UserUpload.class.getName()+" uu where uu.userId = :userId";
        Query query = HibernateUtil.getSession().createQuery( hql );
        query.setString("userId", userId);
        int numDeleted = query.executeUpdate();
        return numDeleted;
    }

}
