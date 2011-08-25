package org.genepattern.server.dm.userupload.dao;

import java.util.Collections;
import java.util.List;

import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFilePath;
import org.hibernate.Query;
import org.hibernate.criterion.Example;

public class UserUploadDao extends BaseDAO {
    
    /**
     * @param userId
     * @param gpFileObj
     * @return a managed UserUpload instance or null of no matching path was found for the user.
     */
    public UserUpload selectUserUpload(String userId, GpFilePath gpFileObj) {
        String hql = "from "+UserUpload.class.getName()+" uu where uu.userId = :userId and path = :path";
        Query query = HibernateUtil.getSession().createQuery( hql );
        query.setString("userId", userId);
        query.setString("path", gpFileObj.getRelativePath());
        List rval = query.list();
        if (rval != null && rval.size() == 1) {
            return (UserUpload) rval.get(0);
        }
        //TODO: log error
        return null;
    }

    /**
     * Get all user upload files for the given user.
     * @param userId
     * @return
     */
    public List<UserUpload> selectAllUserUpload(String userId) {
        if (userId == null) return Collections.emptyList();
        
        UserUpload ex = new UserUpload();
        ex.setUserId( userId );
        List results = HibernateUtil.getSession().createCriteria( UserUpload.class ).add( Example.create( ex ) ).list();
        return results;
//        
//        
//        String hql = "from "+UserUpload.class.getName()+" uu where uu.userId = :userId";
//        Query query = HibernateUtil.getSession().createQuery( hql );
//        query.setString("userId", userId);
//        
//        List rval = query.list();
//        if (rval == null) {
//            //TODO: log error
//            return Collections.emptyList();
//        }
//        return rval;
    }
    
    public int deleteUserUpload(String userId, GpFilePath gpFileObj) {
        String hql = "delete UploadFile uf where uf.userId = :userId and uf.path = :path";
        Query query = HibernateUtil.getSession().createQuery( hql );
        query.setString("userId", userId);
        query.setString("path", gpFileObj.getRelativePath());
        int numDeleted = query.executeUpdate();
        return numDeleted;
    }

}
