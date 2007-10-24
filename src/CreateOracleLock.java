/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2008) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.genepattern.server.AnalysisManager;
import org.genepattern.server.AnalysisTask;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.domain.AnalysisJobDAO;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.OmnigeneException;
import org.hibernate.Query;

import java.util.Vector;


public class CreateOracleLock {
	static Logger log = Logger.getLogger(CreateOracleLock.class);
	private Object jobQueueWaitObject = new Object();
	private Vector jobQueue = new Vector();
	   
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		loadProperties();

		//AnalysisManager.getInstance();
		//AnalysisTask.startQueue();
		 //HibernateUtil.beginTransaction();
//		 GenePatternAnalysisTask genepattern = new GenePatternAnalysisTask();
//         Vector jobQueue = genepattern.getWaitingJobs();
//         
//         log.debug("About to commit after getWaitingJobs()");
//         HibernateUtil.commitTransaction();
//         log.debug("After commit");
		
		AnalysisTask at = new AnalysisTask(20);
		at.run();
		
		
		CreateOracleLock t = new CreateOracleLock();
		t.generateLock();
		
		System.out.println("Done");
	}

	
	 private AnalysisJobDAO dao = new AnalysisJobDAO();
		
	    public void updateJobStatusToProcessing(Vector<JobInfo> jobs){
	    	int maxJobCount = 20;
	    	int i=0;
	    	
	    	JobStatus newStatus = (JobStatus) HibernateUtil.getSession().get(JobStatus.class, JobStatus.JOB_PROCESSING);
	    	Iterator iter = jobs.iterator();
	        while (iter.hasNext() && i++ <= maxJobCount) {
	            JobInfo jobby = (JobInfo) iter.next();
	            
	            AnalysisJob aJob = dao.findById(jobby.getJobNumber());
	            aJob.setStatus(newStatus);
	        }
	    }
	
	    public Vector getWaitingJobs() throws OmnigeneException {
	        Vector jobVector = new Vector();

	        String hql = "from org.genepattern.server.domain.AnalysisJob "
	                + " where jobStatus.statusId = :statusId order by submittedDate ";
	        Query query = HibernateUtil.getSession().createQuery(hql);
	        query.setInteger("statusId", JobStatus.JOB_PENDING);

	        List results = query.list();
	        Iterator iter = results.iterator();
	        
	        while (iter.hasNext()) {
	            AnalysisJob aJob = (AnalysisJob) iter.next();
	            jobVector.add(new JobInfo(aJob));
	        }

	        return jobVector;

	    }
	    
	   public void generateLock() {
	        log.debug("Starting AnalysisTask thread");
	         
	        // Load input data to input queue
	        synchronized (jobQueueWaitObject) {

	        	// Fetch another batch of jobs.
	        	try {
	        		HibernateUtil.beginTransaction();

	        		jobQueue =  this.getWaitingJobs();
	        		log.debug("Here " + jobQueue.size());
	        		
	        		
	        		this.updateJobStatusToProcessing(jobQueue);
	        		
	        		//   this.getWaitingJobs_LOCKSUP(NUM_THREADS);
	        		
	        		HibernateUtil.commitTransaction();

	        		
	        	} catch(RuntimeException e) {
	        		HibernateUtil.rollbackTransaction();
	        		log.error("Error getting waiting jobs" , e);
	        	}
	            
	            
	        }    
	    }
	   
	   
    protected static void loadProperties() throws Exception {
    	File propFile = null;
    	FileInputStream fis = null;
    	
    	try {
    	    
    	    Properties sysProps = System.getProperties();
    	    String dir = "/Users/liefeld/projects/oracle_test";
    	    
    	    propFile = new File(dir, "genepattern.properties");
    	    Properties props = new Properties();

    	    fis = new FileInputStream(propFile);
    	    props.load(fis);
    	    log.debug("loaded GP properties from " + propFile.getCanonicalPath());

    	   
    	    // copy all of the new properties to System properties
    	    for (Iterator iter = props.keySet().iterator(); iter.hasNext();) {
	    		String key = (String) iter.next();
	    		String val = (String) props.getProperty(key);
	    		if (val.startsWith(".")) {
	    		    val = new File(val).getAbsolutePath();
	    		}
	    		sysProps.setProperty(key, val);
	    	}

    	    propFile = new File(dir, "build.properties");
    	    fis = new FileInputStream(propFile);
    	    props.load(fis);
    	    fis.close();
    	    fis = null;
    	    // copy all of the new properties to System properties
    	    for (Iterator iter = props.keySet().iterator(); iter.hasNext();) {
	    		String key = (String) iter.next();
	    		String val = (String) props.getProperty(key);
	    		if (val.startsWith(".")) {
	    		    val = new File(val).getCanonicalPath();
	    		}
	    		sysProps.setProperty(key, val);
    	    }

    	  
    	} catch (IOException ioe) {
    	    ioe.printStackTrace();
    	    String path = null;
    	    try {
    		path = propFile.getCanonicalPath();
    	    } catch (IOException ioe2) {
    	    }
    	   
    	} finally {
    	    try {
    		if (fis != null)
    		    fis.close();
    	    } catch (IOException ioe) {
    	    }
    	}
        }

}
