/*
 * Created on Jul 21, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.broad.gp.gpge;

import java.util.HashSet;
import java.util.Iterator;

import edu.mit.broad.gp.core.GPGECorePlugin;
import edu.mit.broad.gp.core.ServiceManager;
import edu.mit.broad.gp.gpge.job.JobEventListener;
import edu.mit.genome.gp.ui.analysis.AnalysisJob;
import edu.mit.wi.omnigene.framework.analysis.JobInfo;

/**
 * @author genepattern
 *
 * This class is the central point for keeping track of jobs that
 * have been submitted to a server and notifying any listeners to
 * take action when a job is submitted.  E.g. when a job is submitted  
 * to a server, we need to update both the DataView and also the
 * LogOfRunsView.
 * 
 * This class will also be responsible for remembering what previously run jobs 
 * should be remembered when GenePattern restarts after a shutdown and notify
 * any views as necessary 
 */
public class JobSubmissionRegistry {
    private static JobSubmissionRegistry defaultInstance = null;
    
    private HashSet jobEventListeners = new HashSet();
    
    /**
     * create the singleton instance
     *
     */
    private JobSubmissionRegistry(){
       
    }
    
    public void rememberOldJobs(){
        String[] oldjobs = GPGECorePlugin.getDefault().getPreferenceArray(GPGECorePlugin.OLD_JOBS_PREFERENCE);
        for (int i=0; i < oldjobs.length; i++){
            String jobbyjob = oldjobs[i];
            int idx = jobbyjob.lastIndexOf(':');
            String serverId = jobbyjob.substring(0,idx);
            String jobid = jobbyjob.substring(idx+1);
            System.out.println("Retrieving: " + serverId +" " + jobid);
            ServiceManager serviceManager = new ServiceManager(serverId, "");
           
         	try {
         	    JobInfo info =  serviceManager.getRequestHandler().checkStatus(Integer.parseInt(jobid));
         	    String svc = serviceManager.getServiceName(info.getTaskID());
        	    
         	    AnalysisJob job = new AnalysisJob(serverId, svc, info);
         	    jobCompleted(job, false);	
         	    System.out.println("Found " + info.getStatus());
            } catch (Exception e){
                e.printStackTrace();
                String jobKey = serverId + ":" + jobid;
                GPGECorePlugin.getDefault().removeFromPreferenceArray(GPGECorePlugin.OLD_JOBS_PREFERENCE, jobKey);
            
            }
     
        }
    }
    
    public void forget(AnalysisJob job){
        String jobKey = getJobKey(job);
        GPGECorePlugin.getDefault().removeFromPreferenceArray(GPGECorePlugin.OLD_JOBS_PREFERENCE, jobKey);
        
    }
    
    public String getJobKey(AnalysisJob job){
        return job.getSiteName() + ":" + job.getJobInfo().getJobNumber();
    }
    
    public static JobSubmissionRegistry getDefault(){
        if (defaultInstance == null) defaultInstance = new JobSubmissionRegistry();
        return defaultInstance;
    }
    
    /**
     * Notify any interested parties that a job has been run
     * 
     * TODO Change this to a publish/subscribe event mechanism
     * 
     * @param job
     */
    public void jobCompleted(AnalysisJob job, boolean remember){
        for (Iterator iter = jobEventListeners.iterator(); iter.hasNext(); ){
            JobEventListener jel = (JobEventListener)iter.next();
            jel.jobFinished(job);
        }
        if (remember)
            GPGECorePlugin.getDefault().addToPreferenceArray(GPGECorePlugin.OLD_JOBS_PREFERENCE, getJobKey(job));
    }
    public void jobCompleted(AnalysisJob job){
        jobCompleted(job, true);
    }
    
    public void jobStatusChange(JobInfo jobInfo){
        for (Iterator iter = jobEventListeners.iterator(); iter.hasNext(); ){
            JobEventListener jel = (JobEventListener)iter.next();
            jel.jobStatusChange(jobInfo);
        }
    }
    
    public void addJobEventListener(JobEventListener jel){
        jobEventListeners.add(jel);
    }
    
    public void removeJobEventListener(JobEventListener jel){
        jobEventListeners.remove(jel);
    }
    
    
    
}
