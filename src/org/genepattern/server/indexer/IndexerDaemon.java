/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.indexer;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.server.webservice.server.dao.HibernateUtil;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;

public class IndexerDaemon implements Runnable {
    
    private static Logger log = Logger.getLogger(IndexerDaemon.class);

    public static Object indexLock = new Object();
    protected File indexDir = null;
    protected Indexer indexer = new Indexer(System.out);
    protected static IndexerDaemon daemon = null;
    public static boolean DEBUG = false;
    protected static IndexWriter writer = null;
    public static final String DISABLE_GP_INDEXING = "disable.gp.indexing";

    public static void main(String[] args) {
        try {
            System.setProperty("genepattern.properties", System.getProperty("genepattern.properties",
                    "C:/Program Files/GenePattern/Tomcat"));
            IndexerDaemon daemon = IndexerDaemon.getDaemon();
            Thread t = new Thread(daemon, "IndexerDaemon");
            t.setPriority(Thread.NORM_PRIORITY - 3);
            t.start();
            if (DEBUG) {
                System.out.println("notifying daemon");
            }
            synchronized (daemon.indexLock) {
                daemon.indexLock.notify();
            }
            if (DEBUG) {
                System.out.println("notifying daemon again");
            }
            synchronized (daemon.indexLock) {
                daemon.indexLock.notify();
            }
            if (DEBUG) {
                System.out.println("IndexerDaemon done");
            }
        }
        catch (Throwable t) {
            System.err.println(t);
            t.printStackTrace();
        }
    }

    public IndexerDaemon() throws IOException {
        this.indexDir = Indexer.getIndexDir();
        // if an old lock file was left around, delete it
        try {
            indexer.createIfNecessary(indexDir);
        }
        catch (Exception e) {
            System.err.println(e + " during IndexerDaemon creation");
        }
        FSDirectory.getDirectory(indexDir, false).makeLock(IndexWriter.WRITE_LOCK_NAME).release();
    }

    public static IndexerDaemon getDaemon() throws IOException {
        if (daemon == null) {
            daemon = new IndexerDaemon();
        }
        return daemon;
    }

    public static boolean isIndexingEnabled() {
        return !"true".equalsIgnoreCase(System.getProperty(DISABLE_GP_INDEXING));
    }

    public void run() {
        if (!isIndexingEnabled()) {
            return;
        }
        
        try {
            indexer.createIfNecessary(indexDir);
            AnalysisDAO ds = new AnalysisDAO();

            String taskQuery = "select task_id from task_master where isIndexed is null";
            String jobQuery = "select job_no from analysis_job where isIndexed is null and status_id in (3,4)"; // Finished,
            // Error
            String taskIndexedUpdate = "update task_master set isIndexed=true where task_id=";
            String jobIndexedUpdate = "update analysis_job set isIndexed=true where job_no=";
            int taskID = 0;
            int jobID = 0;
            boolean didWork = false;
            if (DEBUG) {
                System.out.println("IndexerDaemon running on " + Thread.currentThread().getName() + " at priority "
                        + Thread.currentThread().getPriority());
            }
            while (true) {
                do {
                    didWork = false;

                    HibernateUtil.getSession().beginTransaction();
                    ResultSet rs = null;
                    
                    
                    try {
                        // index tasks
                        rs = ds.executeSQL(taskQuery);
                        while (rs.next()) {
                            synchronized (Indexer.getConcurrencyLock()) {
                                writer = Indexer.getWriter();
                                try {
                                    taskID = rs.getInt("task_id");
                                    indexer.indexTask(writer, taskID);
                                    ds.executeUpdate(taskIndexedUpdate + taskID);
                                    didWork = true;
                                }
                                catch (Throwable t) {
                                    System.err.println(t + " in IndexerDaemon.run while processing task " + taskID);
                                    t.printStackTrace();
                                }
                                writer = Indexer.releaseWriter();
                            }
                        }
                        rs.close();
                        rs = null;
  
                        // index jobs
                        rs = ds.executeSQL(jobQuery);
                        while (rs.next()) {
                            synchronized (Indexer.getConcurrencyLock()) {
                                writer = Indexer.getWriter();
                                try {
                                    jobID = rs.getInt("job_no");
                                    indexer.indexJob(writer, jobID);
                                    ds.executeUpdate(jobIndexedUpdate + jobID);
                                    didWork = true;
                                }
                                catch (Throwable t) {
                                    System.err.println(t + " in IndexerDaemon.run while processing job " + jobID);
                                    t.printStackTrace();
                                }
                                writer = Indexer.releaseWriter();
                            } // end sychronized block
                        }
                        rs.close();
                        rs = null;
                        HibernateUtil.getSession().getTransaction().commit();
                    }
                    catch (Exception e) {
                        log.error(e);
                        HibernateUtil.getSession().getTransaction().rollback();
                    }
                    finally {
                        if(rs != null) rs.close();
                        if(HibernateUtil.getSession().isOpen()) {
                            HibernateUtil.getSession().close();
                        }
                    }
                }
                while (didWork);

                // sleep until notified that there is more to do
                synchronized (indexLock) {
                    if (DEBUG) {
                        System.out.println("IndexerDaemon: waiting for notification on lock " + indexLock.hashCode());
                    }
                    indexLock.wait();
                    if (DEBUG) {
                        System.out.println("IndexerDaemon: awakening from notification");
                    }
                }
            }
        }
        catch (InterruptedException ie) {
            System.err.println("IndexerDaemon shutting down");
        }
        catch (Throwable t) {
            System.err.println(t);
            t.printStackTrace();
        }
    }

    public static void notifyJobComplete(int jobID) {
        notifyDaemon();
    }

    public static void notifyTaskUpdate(int taskID) {
        notifyDaemon();
    }

    protected static void notifyDaemon() {
        if (!isIndexingEnabled()) {
            return;
        }
        try {
            Object lock = getDaemon().indexLock;
            if (DEBUG) {
                System.out.println("IndexerDaemon.notifyDaemon: waking up daemon for indexing, using lock "
                        + lock.hashCode());
            }
            synchronized (lock) {
                lock.notify();
            }
            if (DEBUG) {
                System.out.println("IndexerDaemon.notifyDaemon: woke up daemon for indexing");
            }
        }
        catch (IOException ioe) {
            System.err.println(ioe + " while notifying IndexerDaemon");
        }
    }
}