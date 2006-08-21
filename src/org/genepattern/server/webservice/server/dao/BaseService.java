package org.genepattern.server.webservice.server.dao;

import org.hibernate.Session;


public class BaseService {

    protected Session getSession() {
        return HibernateUtil.getSession();
    }

    protected void cleanupSession() {
        if (getSession().isOpen() && !getSession().getTransaction().isActive()) {
            getSession().close();
        }
    }
    

    public static String filenameFromURL(String url) {
        int idx = url.lastIndexOf("/");
        if (idx >= 0)
            return url.substring(idx + 1);
        else
            return url;
    }


}
