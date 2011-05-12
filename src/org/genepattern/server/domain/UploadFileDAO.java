package org.genepattern.server.domain;

import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.Query;

public class UploadFileDAO extends BaseDAO {
    private static final Logger log = Logger.getLogger(UploadFileDAO.class);


    public UploadFile findByPath(String path) { 
        UploadFile rval = (UploadFile) HibernateUtil.getSession().get(UploadFile.class, path);
        return rval;
    }
    
    public int deleteByPath(String path) {
        log.debug("deleting uploadfile: "+path);
        UploadFile u = (UploadFile) HibernateUtil.getSession().get(UploadFile.class, path);
        if (u != null) {
            HibernateUtil.getSession().delete(u);            
            return 1;
        }
        log.debug("No entry in table");
        return 0;
    }

    public List<UploadFile> findByUserId(String userId) {
        Query query = HibernateUtil.getSession().getNamedQuery("getUploadFilesForUser");
        query.setString("userId", userId);
        return query.list();
    }

}
