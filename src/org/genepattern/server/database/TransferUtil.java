package org.genepattern.server.database;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

import org.genepattern.server.domain.*;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;

/**
 * Utility class for transferring contents between 2 databases via hibernate
 * 
 */
public class TransferUtil {

    private static final SessionFactory sessionFactoryFrom;
    private static final SessionFactory sessionFactoryTo;

    static {
        try {

            sessionFactoryFrom = (new Configuration()).configure("hibernate.cfg.xml").buildSessionFactory();
            sessionFactoryTo = (new Configuration()).configure("oracle.cfg.xml").buildSessionFactory();
        }
        catch (Throwable ex) {
            // Make sure you log the exception, as it might be swallowed
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static Session getFromSession() {
        Session s = sessionFactoryFrom.getCurrentSession();
        if (!s.getTransaction().isActive()) {
            s.beginTransaction();
        }
        return s;
    }

    private static Session getToSession() {
        Session s = sessionFactoryTo.getCurrentSession();
        if (!s.getTransaction().isActive()) {
            s.beginTransaction();
        }
        return s;
    }

    private static void transferAll() {

        Class[] classes = { JobStatus.class, Lsid.class, Props.class, Sequence.class, TaskAccess.class,
                AnalysisJob.class, TaskMaster.class, Suite.class };

        for (Class c : classes) {
            transferClassData(c);
        }

    }

    private static void transferClassData(Class aClass) {

        try {
            Query q = getFromSession().createQuery("from " + aClass.getName());
            q.setFetchSize(100);
            List results = q.list();

            Session toSession = getToSession();
            for (Object obj : results) {
                toSession.replicate(obj, ReplicationMode.OVERWRITE);
            }
            getFromSession().getTransaction().rollback();
            getToSession().getTransaction().commit();
            
        }
        catch (Exception e) {
            e.printStackTrace();
            getFromSession().getTransaction().rollback();
            getToSession().getTransaction().rollback();
        }
    }
    
    private static boolean verifyTransfer() {
        List<JobStatus> fromJS= getFromSession().createQuery("from " + JobStatus.class.getName() + " order by statusId ").list();
        List<JobStatus> toJS =getToSession().createQuery("from " + JobStatus.class.getName() + " order by statusId ").list();
        if(fromJS.size() != toJS.size()) {
            return false;
        }
        for(int i=0; i<fromJS.size(); i++) {
            if(fromJS.get(i).getStatusId().intValue() != toJS.get(i).getStatusId().intValue()) {
                System.out.println("JS id differs");
                return false;
            }
        }
        
        
        List<AnalysisJob> fromAJ= getFromSession().createQuery("from " + AnalysisJob.class.getName() + " order by jobNo ").list();
        List<AnalysisJob> toAJ =getToSession().createQuery("from " + AnalysisJob.class.getName() + " order by jobNo ").list();
        if(fromJS.size() != toJS.size()) {
            return false;
        }
        for(int i=0; i<fromAJ.size(); i++) {
            if(fromAJ.get(i).getJobNo().intValue() != toAJ.get(i).getJobNo().intValue()) {
                System.out.println("AJ id differs");
                return false;
            }
            if( !fromAJ.get(i).getParameterInfo().equals(toAJ.get(i).getParameterInfo())) {
                System.out.println("AJ parameter info differs");
                return false;
            }
       }
        return true;

    }
    
    //@TODO -- fix sequences
    /*GPPORTAL.TASK_MASTER_SEQ GPPORTAL.ANALYSIS_JOB_SEQ SUITE_MODULES_SEQ SUITE_SEQ 
     * 
     */
   private static void updateOracleSequences() {
        
        // Task master
        int maxId = ((BigDecimal) getToSession().createSQLQuery("select max(task_id) from task_master").uniqueResult()).intValue();
        Query seqQuery = getToSession().createSQLQuery("select task_master_seq.nextval from dual");
        int seq = 0;
        while(seq < maxId) {
            seq = ((BigDecimal) seqQuery.uniqueResult()).intValue();
        }
        
        maxId = ((BigDecimal) getToSession().createSQLQuery("select max(job_no) from ANALYSIS_JOB").uniqueResult()).intValue();
        seqQuery = getToSession().createSQLQuery("select ANALYSIS_JOB_SEQ.nextval from dual");
        seq = 0;
        while(seq < maxId) {
            seq = ((BigDecimal) seqQuery.uniqueResult()).intValue();
        }
       
        maxId = ((BigDecimal) getToSession().createSQLQuery("select max(suite_id) from SUITE").uniqueResult()).intValue();
        seqQuery = getToSession().createSQLQuery("select SUITE_SEQ.nextval from dual");
        seq = 0;
        while(seq < maxId) {
            seq = ((BigDecimal) seqQuery.uniqueResult()).intValue();
        }
 
        maxId = ((BigDecimal) getToSession().createSQLQuery("select max(module_id) from SUITE_MODULES").uniqueResult()).intValue();
        seqQuery = getToSession().createSQLQuery("select SUITE_MODULES_SEQ.nextval from dual");
        seq = 0;
        while(seq < maxId) {
            seq = ((BigDecimal) seqQuery.uniqueResult()).intValue();
        }
    }
    

    public static void main(String[] args) {
 //       transferAll();
 //       System.out.println(verifyTransfer());
        updateOracleSequences();

    }
}
