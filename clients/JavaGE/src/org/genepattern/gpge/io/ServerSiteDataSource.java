/*
 * DirDataSource.java
 *
 * Created on February 18, 2003, 4:48 PM
 */

package org.genepattern.gpge.io;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.genepattern.io.parsers.DataParser;
 
/**
 *  Creates ServerTasksDataSource objects and updates them with jobs from server
 * @author  keith
 */
public class ServerSiteDataSource extends GroupDataSource {
    
    /** Creates a new instance of ServerSiteDataSource 
     * This will be aware of the specified directory and all
     * data files there.
     */
    public ServerSiteDataSource(final org.genepattern.gpge.ui.tasks.DataModel analysis_model, final String site_name, final DataParser[] parsers, final GroupDataSource parent, final DataSourceManager manager) throws java.io.IOException, org.genepattern.analysis.PropertyNotFoundException {
        super(createSources(parsers), parent, manager, "Server ");
        this.analysis_model = analysis_model;
        if( site_name == null )
            throw new NullPointerException("The site name must not be null");
        this.site_name = site_name;
        final Properties p = org.genepattern.util.PropertyFactory.getInstance().getProperties("omnigene.properties");
        file_header_source_url = p.getProperty("result.file.header.source");
        if( file_header_source_url == null )
            throw new IllegalStateException("Cannot get the result file header source URL!");
        
        this.name = name;
        this.name_ref = "Server "+site_name+" (remote access)";
        this.task_source = new HashMap();
        // last thing
        final org.genepattern.modules.ui.listeners.SafeObserverListener observer = 
        new org.genepattern.modules.ui.listeners.SafeObserverListener() {
            /**
             * implements the method from interface Observer
             * @param o <code>Observable<code> object is the omiview.analysis
             *          package's <code>DataModel<code>
             * @param arg Unused...
             */
            protected final void updateIt(java.util.Observable observable, Object obj) throws java.io.IOException{
                System.out.println("updating observer ServerSiteDataSource obj ="+obj);
                if( obj instanceof org.genepattern.gpge.ui.tasks.DataModel.JobAndObserver ) {
                    final org.genepattern.gpge.ui.tasks.DataModel.JobAndObserver jao = (org.genepattern.gpge.ui.tasks.DataModel.JobAndObserver)obj;
                    System.out.println("JobAndObserver: "+jao);
                    final org.genepattern.analysis.JobInfo info = jao.job.getJobInfo();
                    System.out.println("JobInfo: "+info);
                    System.out.println("Has output file "+info.containsOutputFileParam());
                    System.out.println("Has input file "+info.containsInputFileParam());
                    System.out.println("result filename: \""+info.getResultFileName()+"\"");
                    System.out.println("input filename: \""+info.getInputFileName()+"\"");
                    updateList();
                }
                
            }
        };
        analysis_model.addObserver(observer);
        if( analysis_model.getResults().size() > 0 )
            updateList();
    }
	 
	 public String toString() {
		 String temp = super.toString();
		 if(temp.equals("Not named yet")) {
				return "Server"; 
		 }
		 return temp;
	 }
    
    /** helper for constructor */
    protected static final DataSource[] createSources(final DataParser[] parsers) {
        return new DataSource[0];
    }

    
    /** updates the list of data files previously found
     * Note: this should be run in a seperate thread
     */
    protected void updateList() throws java.io.IOException {
       // System.out.println("updateList for site");
        // create ServerTasks
        // FIXME should be a ServerSiteHandler class that creates new ServerSiteDataSource
        // instances when new sites appear.  It would also be the only listener for 
        // analysis.DataModel update(observer, Object) events
        final Map results = analysis_model.getResults();
        final Map jobsOnsite = (Map)results.get(site_name);
        if( jobsOnsite == null ) {
           // System.out.println("Server: "+site_name+" doesn't have jobs");
            return ; // nothing to do
        }
        
        for(final Iterator jobs_iter = jobsOnsite.keySet().iterator(); jobs_iter.hasNext(); ) {
            final String task_name = (String)jobs_iter.next();
            //getting jobs
            final List jobs = (List)jobsOnsite.get(task_name);
            
            ServerTaskDataSource task = (ServerTaskDataSource)task_source.get(task_name);
            if( task == null ) {
                task = createServerTaskDataSource(task_name);
					 System.out.println("createServerTaskDataSource " + task_name);
            }
            
            task.updateList(jobs);
        }
    }

	 public void removeServerTaskDataSource(String taskName) {
		 task_source.remove(taskName);	 
	 }
	 
    /** returns a description of the source- i.e. if it reads data files from a local
     * directory, it reads sdf files from OmniGene, etc
     */
     public String getDescription() {
         return DESCRIPTION + name;
     }
     
     /** returns a parser for specified data  */
     protected DataParser getParserToDecode(Object data) {
         throw new UnsupportedOperationException("FIXME Not implemented");
     }
     
     // helpers
     
     /** creates a ServerTaskDataSource and adds the listeners to it */
     protected final ServerTaskDataSource createServerTaskDataSource(final String task_name) {
         final ServerTaskDataSource task = new ServerTaskDataSource(task_name, parsers, this, manager);
         task_source.put(task_name, task);
         return task;
     }
     
     /** called when this should refresh/rescan its data and update  */
     public void refresh() {
     }
     
    // fields
    /** describes what this DataSource does */
    private static final String DESCRIPTION = "Retrives data from the server, ";
    /** the server site name */
    private final String site_name;
    /** maps task name to the ServerTaskDataSource object */
    private final Map task_source;
    /** the edu....omniview.analysis package's DataModel */
    protected final org.genepattern.gpge.ui.tasks.DataModel analysis_model;
    /** the URL of the header source jsp page */
    private final String file_header_source_url;
}
