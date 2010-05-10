/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2009) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.
 
  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
 */

package org.genepattern.server;

/**
 * AnalysisManager - Manager for AnalysisTask Runnable adapter
 *
 * @version $Revision 1.4$
 * @author Rajesh Kuttan, Hui Gong
 */

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.OmnigeneException;

public class AnalysisManager {
    private static Logger log = Logger.getLogger(AnalysisManager.class);
    
    public static int defPriority = Thread.NORM_PRIORITY;
    private static AnalysisManager analysisManager = null;
    
    // To make Singleton
    private AnalysisManager() {
    }
    
    //Function to get singleton instance
    public static synchronized AnalysisManager getInstance() {
        if (analysisManager == null) {
            analysisManager = new AnalysisManager();
            
            //TODO: re-implement handling runtime exec jobs which need to be restarted
            //      this code belongs in the RuntimeExecCmdExecSvc class
            try {
                HibernateUtil.beginTransaction();
                AnalysisDAO ds = new AnalysisDAO();
                // were there interrupted jobs that need to be restarted?
                if (ds.resetPreviouslyRunningJobs()) {
                    System.out.println("There were previously running tasks, notifying threads.");
                    // yes, notify the threads to start processing
                    synchronized (ds) {
                        System.out.println("notifying ds of job to run");
                        ds.notify();
                    }
                }
                
                HibernateUtil.commitTransaction();
                
            } catch (OmnigeneException oe) {
                log.error(oe);
                HibernateUtil.rollbackTransaction();
            }
        }
        return analysisManager;
    }
    
}
