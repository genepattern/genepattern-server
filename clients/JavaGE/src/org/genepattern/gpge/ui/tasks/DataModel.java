package org.genepattern.gpge.ui.tasks;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import org.apache.log4j.Category;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.JobStatus;

/**
 * <p>Title: DataModel.java</p>
 * <p>Description: includes all the data for the the analysis service.</p>
 * @author Hui Gong
 * @version $Revision$
 */

public class DataModel extends Observable implements Serializable{
    //a list of AnalysisJob
    private Vector _jobs;
    //using siteName (data source name) as key, object is a Hashtable which
    //contains taskname as key and Vector of JobInfo as object
    private Hashtable _results;
    //web service source, using site name as key and url as object
    private Hashtable _source;
    private transient Vector _analysisServices;
    private static Category cat = Category.getInstance(DataModel.class.getName());
    public final static String OBSERVER_TASK = "task";
    //data includes both history and result
    public final static String OBSERVER_DATA = "data";
    public final static String OBSERVER_HISTORY = "history";
    public final static String OBSERVER_RESULT = "result";

	 public void addObserver(Observer o) {
		super.addObserver(o);
		System.out.println("addObserver " + o.getClass());
	 }
    public DataModel() {
        _source = new Hashtable();
        _jobs = new Vector();
        _results = new Hashtable();
        _analysisServices = new Vector();
    }

    /**
     * Adds data source information.
     */
    public void addDataSource(String name, String URL){
        this._source.put(name, URL);
    }

    /**
     * Gets the data source URL from the site name
     */
    public String getDataSourceURL(String name){
        return (String)this._source.get(name);
    }

    /**
     * Resets the data inside of the model.
     */
    public void resetData(Vector jobs, Hashtable results){
        System.out.println("DataModel resetting data:\n # jobs="+jobs.size()+" # sites="+results.size());
        this._jobs = jobs;
        this._results = results;
        this.setChanged();
        notifyObservers(this.OBSERVER_DATA);
    }

    /**
     * Sets all analysis service.
     */
    public void setAnalysisServices(Vector services){
        this._analysisServices = services;
        this.setChanged();
        this.notifyObservers(this.OBSERVER_TASK);
    }

    /**
     * Gets all analysis services.
     */
    public Vector getAnalysisServices(){
        return this._analysisServices;
    }

    /**
     * Gets all saved jobs sumbitted so far.
     */
    public Vector getJobs(){
        return this._jobs;
    }

    /**
     * Adds a new job.
     */
    public void addJob(AnalysisJob job){
        this._jobs.add(job);
        cat.debug("added new job");
        this.setChanged();
        notifyObservers(new JobAndObserver(job,this.OBSERVER_HISTORY));
    }
    
    /** gets the Observer constant from the object */
    public static String demangleObserverType(final Object arg) {
        if( arg instanceof String )
            return (String)arg;
        else
            return ((JobAndObserver)arg).observer_type;
    }
    /**
     * Updates the status of a job.
     */
    public void updateStatus(AnalysisJob job){
        // new code
        final String status = job.getJobInfo().getStatus();
        cat.debug("Job status: "+ status);
        if(status.equals(JobStatus.PROCESSING)){
            updateHistory(job);
            this.setChanged();
            this.notifyObservers(new JobAndObserver(job, this.OBSERVER_HISTORY));
        }
        else if(status.equals(JobStatus.FINISHED)){
            updateHistory(job);
            addResult(job);
            this.setChanged();
            this.notifyObservers(new JobAndObserver(job, this.OBSERVER_DATA));
        } else if( status.equals(JobStatus.ERROR) || status.equals(JobStatus.TIMEOUT) ) {
            //added for GP 
            updateHistory(job);
            addResult(job);
            // end added for GP
            this.setChanged();
            this.notifyObservers(new JobAndObserver(job, this.OBSERVER_DATA));
        } else 
            throw new IllegalArgumentException("Unknown status: "+status);
        
    }

    /**
     * Updates the history of a job.
     */
    private void updateHistory(AnalysisJob aJob){
        JobInfo job = aJob.getJobInfo();
        String siteName = aJob.getServer();
        int id = job.getJobNumber();
        String status = job.getStatus();
        for(int i=0; i<_jobs.size(); i++){
            AnalysisJob aInfo = (AnalysisJob)_jobs.get(i);
            JobInfo info = aInfo.getJobInfo();
            if(aInfo.getServer().equals(siteName)&& info.getJobNumber()== id && !info.getStatus().equals(status)){
                _jobs.setElementAt(aJob, i);
                break;
            }
        }
    }

    /**
     * Removes a record from the history, and updates observers
     */
    public void removeHistory(final int location){
        final AnalysisJob job = (AnalysisJob)this._jobs.remove(location);
	removeResult(job);
        this.setChanged();
        this.notifyObservers(new JobAndObserver(job, this.OBSERVER_HISTORY));//this.OBSERVER_HISTORY);
    }

    /**
     * Removes a list of records from the history, and updates observers
     */
    public final void removeHistorys(final int[] locations){
        final int num = locations.length;
        for(int i=0; i<num; i++){
            final AnalysisJob job = (AnalysisJob)_jobs.remove(locations[i]-i);
	    removeResult(job);
        }
        this.setChanged();
        this.notifyObservers(this.OBSERVER_HISTORY);
    }

    /**
     * Removes a record from the result
     */
    public final void removeResult(final AnalysisJob job){
        final String siteName = job.getServer();
        final String taskName = job.getTaskName();
        final Hashtable siteResult = (Hashtable)this._results.get(siteName);
        final Vector jobs = (Vector)siteResult.get(taskName);
        jobs.removeElement(job);
        if(jobs.isEmpty() ) // clean up
            siteResult.remove(siteName);
    }
    

	/**
		Removes the job from the list of jobs with the with job number equal to job.getJobInfo().getJobNumber() and server name equal to job.getServer()
		@param job the job to remove
	*/
    public void removeJobById(AnalysisJob job) {
		boolean found = false;
		
		for(int i=0; i<_jobs.size(); i++){
			AnalysisJob temp = (AnalysisJob) _jobs.get(i); 
			if(temp.getJobInfo().getJobNumber()==job.getJobInfo().getJobNumber() && temp.getServer().equals(job.getServer())) {
				 removeResult((AnalysisJob)_jobs.remove(i));
				found = true;
			}
		}
		if(!found) {
			throw new IllegalArgumentException("Job number " + job.getJobInfo().getJobNumber() + " on the server " + job.getServer() + " not found.");	
		}
		String taskName = job.getTaskName();
		String siteName = job.getServer();
		Hashtable siteJobs = (Hashtable)this._results.get(siteName);
		if(siteJobs.containsKey(taskName)){
			Vector jobs = (Vector)siteJobs.get(taskName);
			if(jobs!=null && jobs.size()==0) {
				siteJobs.remove(taskName);
			}
			
		}
		this.setChanged();
		this.notifyObservers(this.OBSERVER_HISTORY);
	}
	
    /**
     * Removes all results having the specified taskname.
     * @param parent of the task, it's a site name.
     * @param taskname specified the task name for deletion.
     */
     public void removeTaskFromResult(String parent, String taskname){
         Hashtable siteResult = (Hashtable)this._results.get(parent);
         siteResult.remove(taskname);
     }

     /**
      * Removes all results under the site name
      * @param siteName the name of the site which hosts the tasks
      */
      public void removeSiteFromResult(String siteName){
          this._results.remove(siteName);
      }

    /**
     * Adds a new result
     */
    private void addResult(AnalysisJob job){
        cat.debug("adding result for site:"+job.getServer()+" task: "+job.getTaskName());
        String siteName = job.getServer();
        String taskName = job.getTaskName();
        Hashtable siteJobs;
        Vector jobs;
        //if result includes jobs from this site
        if(this._results.containsKey(siteName)){
            siteJobs = (Hashtable)this._results.get(siteName);
            if(siteJobs.containsKey(taskName)){
                jobs = (Vector)siteJobs.get(taskName);
                jobs.add(job);
            }
            else{
                jobs = new Vector();
                jobs.add(job);
            }
            siteJobs.put(taskName, jobs);
            _results.put(siteName, siteJobs);
        }
        else{
            siteJobs = new Hashtable();
            jobs = new Vector();
            jobs.add(job);
            siteJobs.put(taskName, jobs);
            _results.put(siteName, siteJobs);
        }
        //printResult();
    }

    public void printResult(){
        Enumeration e = this._results.keys();
        while(e.hasMoreElements()){
            String site = (String)e.nextElement();
            cat.debug("site: "+site);
            Hashtable task = (Hashtable)this._results.get(site);
            Enumeration e2 = task.keys();
            while(e2.hasMoreElements()){
                String taskName = (String)e2.nextElement();
                cat.debug("task: "+taskName);
                Vector jobs = (Vector) task.get(taskName);
                Iterator i = jobs.iterator();
                while(i.hasNext()){
                    AnalysisJob job = (AnalysisJob)i.next();
                    cat.debug("job: "+job.toString());
                }
            }
        }
    }

    /**
     * Gets all saved results.
     * @return Hashtable contains all job info with task name as key.
     */
    public Hashtable getResults(){
        return this._results;
    }


    public String marshal(){
        String xml="";
        return xml;
    }

    public static DataModel unmarshal(){
        DataModel model = new DataModel();
        return model;
    }
    
    /** returns a String reprentation of this */
    public String toString() {
        return "DataModel [# jobs="+_jobs.size()+" # sites="+_results.size()
            +" # analysis services="+_analysisServices.size()+']';
    }
    
    // I N N E R  C L A S S E S 
    /** what job has it's status updated and which observer should be 
     * paying attention
     */
    public static final class JobAndObserver {
        /** the job */
        public final AnalysisJob job;
        /** The observer that should be interested in this */
        public final String observer_type;
        
        /**  */
        public JobAndObserver(final AnalysisJob job, final String observer_type) {
            this.job = job;
            this.observer_type = observer_type;
        }
        
        /** standard overridden method */
        public final String toString() {
            return "[Observer "+observer_type+", Job "+job+"]";
        }
    }
}
