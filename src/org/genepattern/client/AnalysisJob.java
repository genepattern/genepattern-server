package org.genepattern.client;


import java.io.Serializable;

import org.genepattern.analysis.JobInfo;

/**
 * <p>Title: AnalysisJob.java</p>
 * <p>Description: Includes site name, task name and JobInfo about a analysis job. Can be serialized. </p>
 * @author Hui Gong
 * @version $Revision 1.2 $
 */

public class AnalysisJob implements Serializable{
    private String _siteName;
    private String _taskName;
    private JobInfo _job;
	 private String lsid;
	 
    public AnalysisJob(String siteName, String taskName, JobInfo job){
        this._siteName = siteName;
        this._taskName = taskName;
        this._job = job;
    }

    public void setJobInfo(JobInfo job){
        this._job = job;
    }

    public String getSiteName(){
        return this._siteName;
    }
	 
	 

    public String getTaskName(){
        return this._taskName;
    }

    public JobInfo getJobInfo(){
        return this._job;
    }

    public String toString(){
        return String.valueOf(_job.getJobNumber());
    }
	 
	 public String getLSID() {
		return lsid; 
	 }
	 
	 public void setLSID(String lsid) {
		this.lsid = lsid; 
	 }	
}
