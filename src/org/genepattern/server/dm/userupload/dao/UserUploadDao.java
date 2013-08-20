package org.genepattern.server.dm.userupload.dao;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFilePath;
import org.hibernate.Query;

public class UserUploadDao extends BaseDAO {
    final private static Logger log = Logger.getLogger(UserUploadDao.class);
    
    /**
     * This is the name of root directory relative to a given user's upload tab 
     * for adding temporary files.
     * We use this for adding input files via the job input form, hiding tmp files from the GUI, 
     * and purging tmp files from a 'cron' job.
     */
    final public static String TMP_DIR="tmp";
    
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
     * @param includeTempFiles Whether temp files should be included in the list or not
     * @return
     */
    public List<UserUpload> selectAllUserUpload(String userId, boolean includeTempFiles) {
        Date olderThanDate=null;
        return selectAllUserUpload(userId, includeTempFiles, olderThanDate);
    }
    
    public List<UserUpload> selectAllUserUpload(final String userId, final boolean includeTempFiles, final Date olderThanDate) {
        if (userId == null) return Collections.emptyList();
        //String hqlOrig = "from "+UserUpload.class.getName()+" uu where uu.userId = :userId order by uu.path";
        String hql = "from "+UserUpload.class.getName()+" uu where uu.userId = :userId ";
        if (!includeTempFiles) {
            hql += "and uu.path not like '"+TMP_DIR+"/%' and uu.path not like '"+TMP_DIR+"' ";
        }
        if (olderThanDate != null) {
            hql += " and uu.lastModified < :olderThanDate ";
        }
        hql += " order by uu.path";
        Query query = HibernateUtil.getSession().createQuery( hql );
        query.setString("userId", userId);
        if (olderThanDate != null) {
            query.setTimestamp("olderThanDate", olderThanDate);
        }
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
        List<UserUpload> rval = query.list();
        return rval; 
    }

    /**
     * Get all of the tmp user upload files for the given user which are scheduled to be purged.
     * 
     * @param userId, requires a valid user id, otherwise will return an empty list.
     * @param olderThanDate, requires a non-null cutoff date, otherwise will return an empty list.
     * 
     * @return a list of UserUpload instances which should be deleted, ordered by path
     */
    public List<UserUpload> selectTmpUserUploadsToPurge(final String userId, final Date olderThanDate) {
        if (userId==null) {
            log.error("userId==null");
            return Collections.emptyList();
        }
        if (olderThanDate==null) {
            log.debug("olderThanDate==null");
            return Collections.emptyList();
        }
        
        String hql = "from "+UserUpload.class.getName()+" uu where uu.userId = :userId "+
                "and uu.path like '"+TMP_DIR+"/%' "+
                "and uu.lastModified < :olderThanDate order by uu.path";        
        Query query = HibernateUtil.getSession().createQuery( hql );
        query.setString("userId", userId);
        query.setTimestamp("olderThanDate", olderThanDate);
        List<UserUpload> rval = query.list();
        return rval;
    }
    
    public int deleteUserUploadRecursive(String userId, GpFilePath gpFileObj) {
        String relativePath = gpFileObj.getRelativePath();
        
        // Delete the file itself
        int numDeleted = deleteUserUpload(userId, gpFileObj);

        // Delete child files
        String hql = "delete from " + UserUpload.class.getName() + " where user_id = '" + userId + "' and path like '" + relativePath + "/%'"; //delete "+UserUpload.class.getName()+" uu where uu.userId = :userId and uu.path like :path";
        Query query = HibernateUtil.getSession().createQuery( hql );
        numDeleted += query.executeUpdate();
        
        
        
        return numDeleted;
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
