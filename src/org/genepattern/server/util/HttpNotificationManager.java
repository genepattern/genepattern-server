/*******************************************************************************
 * Copyright (c) 2003-2021 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/

package org.genepattern.server.util;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;

import com.google.common.base.Strings;


/**
 * A clone of the EmailNotificationManager but this one sends a URL get to some URL after a job completes.
 * Intended for use by portals and requested initially by FlowGate
 * 
 * @author liefeld
 *
 */

public class HttpNotificationManager {
    private static final Logger log = Logger.getLogger(HttpNotificationManager.class);

    private static HttpNotificationManager instance = null;

    private ArrayList<JobWaitThread2> threads = new ArrayList<JobWaitThread2>();

    private HttpNotificationManager() {

    }

    public static HttpNotificationManager getInstance() {
	if (instance == null) {
	    instance = new HttpNotificationManager();
	}
	return instance;
    }

    public void addWaitingUser(String anUrl, String user, String jobID) {
        JobWaitThread2 waiter = new JobWaitThread2(anUrl, user, jobID);
        threads.add(waiter);
        waiter.start();
    }

    public void removeWaitingUser(String email, String user, String jobID) {
	for (JobWaitThread2 aJobThread : threads) {
	    if (aJobThread.matchJobAndUser(user, jobID)) {
    		aJobThread.quietStop();
    		threads.remove(aJobThread);
    		return;
	    }
	}
    }

    protected void threadFinished(JobWaitThread2 aThread) {
        threads.remove(aThread);
    }

    public void sendJobCompletionGet(String anUrl, String user, String jobId) {
        String status = "status unknown";
        try {
            HibernateUtil.beginTransaction();
            JobInfo jobInfo = (new AnalysisDAO()).getJobInfo(Integer.parseInt(jobId));
            HibernateUtil.commitTransaction();
            status = jobInfo.getStatus();
        } catch (Exception e) {
            // swallow it and send the link without status
            log.error("Failed getting status for job completion URL call ", e);
            status = "Finished";
        }

        // DO THE GET XXX
        StringBuffer finalUrl = new StringBuffer(anUrl);
        // add status and jobId params to the url
        if (anUrl.indexOf('?') > 0){
            // already some param there, add with an '&'
            finalUrl.append("&");
        } else {
            finalUrl.append("?");
        }
        finalUrl.append("jobId=");
        finalUrl.append(jobId);
        finalUrl.append("&status=");
        finalUrl.append(status);
        
        try {
            URL obj = new URL(finalUrl.toString());
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
    
            // optional default is GET
            con.setRequestMethod("GET");
    
            //add request header
            con.setRequestProperty("User-Agent", "GenePattern/4.0");
    
            int responseCode = con.getResponseCode();
            log.info("Sent job completion get request for job: "+ jobId + " user: "+ user +" url: " + anUrl + "  STATUS="+responseCode);
        } catch (Exception e){
            log.error("Failed sending job completion URL call ", e);
        }
    }

    public void sendJobStatusChangeGet(String anUrl, String user, String jobId, String oldStatus, String status) {
        

        // DO THE GET XXX
        StringBuffer finalUrl = new StringBuffer(anUrl);
        // add status and jobId params to the url
        if (anUrl.indexOf('?') > 0){
            // already some param there, add with an '&'
            finalUrl.append("&");
        } else {
            finalUrl.append("?");
        }
        finalUrl.append("jobId=");
        finalUrl.append(jobId);
        finalUrl.append("&status=");
        finalUrl.append(status);
        finalUrl.append("&prevStatus=");
        finalUrl.append(oldStatus);
        
        try {
            URL obj = new URL(finalUrl.toString());
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
    
            // optional default is GET
            con.setRequestMethod("GET");
    
            //add request header
            con.setRequestProperty("User-Agent", "GenePattern/4.0");
    
            int responseCode = con.getResponseCode();
            log.info("Sent job status change get request for job: "+ jobId + " user: "+ user +" url: " + anUrl + "  STATUS="+responseCode);
        } catch (Exception e){
            log.error("Failed sending job status change URL call ", e);
        }
    }

    

}

class JobWaitThread2 extends Thread {
    String user = null;
    String anUrl = null;

    int jobID;

    int initialSleep = 5000;

    int maxTries = 100;

    boolean stopQuietly = false;

    public JobWaitThread2(String anUrl, String user, String jobID) {
    	this.user = user;
    	this.anUrl = anUrl;
    	this.jobID = Integer.parseInt(jobID);
    }

    public void quietStop() {
	stopQuietly = true;
    }

    public void run() {
	try {
	    String status = "Undetermined";
	    String prevStatus = "Undetermined";
	    JobInfo info = null;
	    int count = 0;
	    int sleep = initialSleep; // wouldn't be here if it was fast
    	while (!(status.equalsIgnoreCase("ERROR") || (status.equalsIgnoreCase("Finished") || stopQuietly))) {
    		count++;
    		try {
    		    Thread.sleep(sleep);
    		} catch (InterruptedException ie) {
    		}
    
    		HibernateUtil.beginTransaction();
    		info = (new AnalysisDAO()).getJobInfo(jobID);
    		HibernateUtil.commitTransaction();
    
    		prevStatus = status;
    		status = info.getStatus();

    		if ((! (prevStatus.equalsIgnoreCase(status))) && (!stopQuietly)){
                // We have a status change, send out a notification
                HttpNotificationManager.getInstance().sendJobStatusChangeGet(anUrl, user, "" + jobID, prevStatus, status);
    		}
    		
    		sleep = incrementSleep(initialSleep, maxTries, count);
    		
    	    }
	    // the job is done. Send an email to the user
	    //if (!stopQuietly) {
	    //    HttpNotificationManager.getInstance().sendJobStatusChangeGet(anUrl, user, "" + jobID, status, prevStatus);
	    // }
	} catch (Exception e) {
	    // problem getting status. Send an email indicating this and
	    // end the thread
	    e.printStackTrace();

	    HttpNotificationManager em = HttpNotificationManager.getInstance();
	    
	} finally {
	    HttpNotificationManager.getInstance().threadFinished(this);
	}
    }

    public int getJobID() {
	return jobID;
    }

    public String getUser() {
	return user;
    }

    /**
     * make the sleep time go up as it takes longer to exec. eg for 100 tries of 1000ms (1 sec) first 20 are 1 sec each
     * next 20 are 2 sec each next 20 are 4 sec each next 20 are 8 sec each any beyond this are 16 sec each
     * 
     * @param init
     *                Description of the Parameter
     * @param maxTries
     *                Description of the Parameter
     * @param count
     *                Description of the Parameter
     * @return Description of the Return Value
     */
    private static int incrementSleep(int init, int maxTries, int count) {
	if (count < (maxTries * 0.2)) {
	    return init;
	}
	if (count < (maxTries * 0.4)) {
	    return init * 2;
	}
	if (count < (maxTries * 0.6)) {
	    return init * 4;
	}
	if (count < (maxTries * 0.8)) {
	    return init * 8;
	}
	return init * 16;
    }

    public boolean matchJobAndUser(String user, String job) {
	if (!user.equals(this.user))
	    return false;
	try {
	    int jobAsInt = Integer.parseInt(job);
	    return (jobAsInt == jobID);
	} catch (Exception e) {
	    return false;
	}
    }
}
