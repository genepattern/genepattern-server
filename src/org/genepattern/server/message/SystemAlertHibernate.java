package org.genepattern.server.message;

import java.util.Date;
import java.util.List;

import org.genepattern.server.database.HibernateUtil;
import org.hibernate.HibernateException;
import org.hibernate.Query;

/**
 * Implement ISystemAlert using Hibernate Data Access classes.
 * @author pcarr
 */
class SystemAlertHibernate implements ISystemAlert {
    //Move this validation code into an abstract parent class if we ever create a second implementation of ISystemAlert
    private void validate(SystemMessage message) throws Exception {
        if (message == null) {
            throw new Exception("System Message not sent: server error (SystemMessage is null).");           
        }
        else if (message.getMessage() == null) {
            throw new Exception("System Message not sent: server error (SystemMessage.message is null).");
            
        }
        else if (message.getMessage().trim().length() == 0) {
            throw new Exception("System Message not sent: no message entered.");
        }

        //if the start time is null (silently) set it to now
        if (message.getStartTime() == null) {
            message.setStartTime(new Date());
        }
    }

    public void setSystemAlertMessage(SystemMessage message) throws Exception {
        validate(message);
        //insert or update SYSTEM_MESSAGE
        try {
            HibernateUtil.beginTransaction();
            HibernateUtil.getSession().saveOrUpdate(message);
            HibernateUtil.commitTransaction();
        }
        catch (HibernateException e) {
            HibernateUtil.rollbackTransaction();
            throw e; // or display error message
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }
    
    public void deleteSystemAlertMessage() {
        //delete from SYSTEM_MESSAGE
        try {
            HibernateUtil.beginTransaction();
            Query query = HibernateUtil.getSession().createQuery("delete from SystemMessage");
            query.executeUpdate();
            HibernateUtil.commitTransaction();
        }
        catch (HibernateException e) {
            HibernateUtil.rollbackTransaction();
            throw e; // or display error message
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }
    
    public void deleteOnRestart() throws Exception {
        //delete from SYSTEM_MESSAGE where deleteOnRestart is true
        try {
            HibernateUtil.beginTransaction();
            Query query = HibernateUtil.getSession().createQuery("delete from SystemMessage where deleteOnRestart != 0");
            query.executeUpdate();
            HibernateUtil.commitTransaction();
        }
        catch (HibernateException e) {
            HibernateUtil.rollbackTransaction();
            throw e; // or display error message
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
        
    }

    public SystemMessage getSystemMessage() {
        //select * from SYSTEM_MESSAGE
        try {
            HibernateUtil.beginTransaction();
            Query q = HibernateUtil.getSession().createQuery("from SystemMessage as m order by m.startTime");
            List<SystemMessage> l = q.list();
            if (l.size() > 0) {
                return l.get(l.size()-1);
            }
            return null;
        }
        catch (HibernateException e) {
            throw e;
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }

    public SystemMessage getSystemMessage(Date date) {
        try {
            HibernateUtil.beginTransaction();
            Query q = HibernateUtil.getSession().createQuery(
                    "from SystemMessage as m where m.startTime <= :now and m.endTime is null or m.endTime >= :now order by m.startTime");
            q.setTimestamp("now", date);
            List<SystemMessage> l = q.list();
            if (l.size() > 0) {
                return l.get(l.size()-1);
            }
            return null;
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }
}

