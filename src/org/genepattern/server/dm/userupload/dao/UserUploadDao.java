package org.genepattern.server.dm.userupload.dao;

import java.util.*;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;
import org.genepattern.drm.Memory;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.cache.FileCache;
import org.genepattern.server.quota.DiskInfo;
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

        // Escape single quotes so they don't cause SQL problems
        relativePath = relativePath.replaceAll("'", Matcher.quoteReplacement("''"));

        // Delete child files
        String hql = "delete from " + UserUpload.class.getName() + " where user_id = '" + userId + "' and path like '" + relativePath + "/%'"; //delete "+UserUpload.class.getName()+" uu where uu.userId = :userId and uu.path like :path";
        Query query = HibernateUtil.getSession().createQuery( hql );
        numDeleted += query.executeUpdate();



        return numDeleted;
    }

    public int renameUserUpload(GpContext context, GpFilePath oldFilePath, GpFilePath newFilePath) {
        String hql = "update " + UserUpload.class.getName() + " uu set uu.name = :newName, uu.kind = :newKind, uu.extension = :newExtension, uu.path = :newPath where uu.userId = :userId and uu.path = :path";
        Query query = HibernateUtil.getSession().createQuery( hql );
        query.setString("newName", newFilePath.getName());
        query.setString("newKind", newFilePath.getKind());
        query.setString("newExtension", newFilePath.getExtension());
        query.setString("newPath", newFilePath.getRelativePath());
        query.setString("userId", context.getUserId());
        query.setString("path", oldFilePath.getRelativePath());
        int renamed = query.executeUpdate();
        return renamed;
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

    /**
     * For the admin page, get the list of all DiskInfo for each registered user,
     * sorted by amount of disk usage in descending order.
     * @return
     */
    public List<DiskInfo> allDiskInfo() {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        final boolean isInTransaction = HibernateUtil.isInTransaction();
        final List<DiskInfo> diskInfoList=new ArrayList<DiskInfo>();

        try {
            HibernateUtil.beginTransaction();
            final String hql = "SELECT uu.userId, SUM(uu.fileLength) FROM " + UserUpload.class.getName()
                    + " uu where uu.userId != :cacheUserId "
                    + "and uu.path not like '"+TMP_DIR+"/%' and uu.path not like '"+TMP_DIR+"' "
                    + "GROUP BY uu.userId";
            final Query query = HibernateUtil.getSession().createQuery(hql);
            query.setString("cacheUserId", FileCache.CACHE_USER_ID);
            query.setReadOnly(true);

            final List<Object[]> results = query.list();
            for (final Object[] result : results) {
                final String userId= (String) result[0];
                final long numBytes= (Long) result[1];
                final DiskInfo diskInfo=DiskInfo.createDiskInfo(gpConfig, userId, numBytes);
                diskInfoList.add(diskInfo);
            }
            
            // sort by size, descending, use name as the tie-breaker
            Collections.sort(diskInfoList, new Comparator<DiskInfo>() {
                @Override
                public int compare(DiskInfo o1, DiskInfo o2) {
                    long n1=o1.getDiskUsageFilesTab().getNumBytes();
                    long n2=o2.getDiskUsageFilesTab().getNumBytes();
                    if (n1==n2) {
                        // sort by userId as tie-breaker
                        return o1.getUserId().toLowerCase().compareTo(o2.getUserId().toLowerCase());
                    }
                    else if (n1<n2) {
                        // reverse sort
                        return 1;
                    }
                    return -1;
                }
            });
        }
        catch (Throwable t) {
            log.error(t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
        return diskInfoList;
    }
    
    /**
     * Get the total size of files for the given user.
     *
     * @param userId, requires a valid user id, otherwise will return null.
     * @param includeTempFiles, whether to include temp files when retrieving the total file size.
     *
     * @return a Memory object containing the total size of files
     */
    public Memory sizeOfAllUserUploads(String userId, boolean includeTempFiles) {
        Memory size = null;
        if (userId == null) return size;

        final boolean isInTransaction = HibernateUtil.isInTransaction();

        try {
            HibernateUtil.beginTransaction();

            String hql = "SELECT SUM(uu.fileLength) FROM " + UserUpload.class.getName() + " uu WHERE uu.userId = :userId";

            hql += " and uu.kind not like 'directory'";
            if (!includeTempFiles) {
                hql += " and uu.path not like '"+TMP_DIR+"/%' and uu.path not like '"+TMP_DIR+"' ";
            }

            Query query = HibernateUtil.getSession().createQuery(hql);
            query.setString("userId", userId);

            List<Long> sizeList = query.list();

            //should just return a list of 1 item
            for(int i =0; i < sizeList.size();i++) {
                Long sizeInBytes = sizeList.get(i);

                //if this is null assume that the size is 0
                if(sizeInBytes != null)
                {
                    size = Memory.fromSizeInBytes(sizeList.get(i));
                }
                else
                {
                    size = Memory.fromSizeInBytes(0);
                }
            }
        }
        catch (Throwable t)
        {
            //log error
            log.error(t);
            HibernateUtil.rollbackTransaction();
        }
        finally
        {
            if (!isInTransaction)
            {
                HibernateUtil.closeCurrentSession();
            }
        }

        return size;
    }
}
