package org.genepattern.gpge.ui.tasks;


import java.io.File;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Category;
import org.genepattern.analysis.TaskInfo;
import org.genepattern.analysis.WebServiceException;
import org.genepattern.client.AnalysisService;
import org.genepattern.client.RequestHandlerFactory;

/**
 * PanelGenerator.java
 * <p>Description: Creates all the panels related to analysis service.</p>
 * @author Hui Gong
 * @version $Revision$
 */

public class PanelGenerator {
    private DataModel _dataModel;
    private AnalysisTasksPanel _tasks;
    private HistoryPanel _history;
    //private ResultsPanel _results;
    private final String _filename = "analysisJobs";
    private static Category cat = Category.getInstance(PanelGenerator.class.getName());

    public PanelGenerator() throws WebServiceException{
        this(null, null, null, null, null, null, -1, null, null);
    }

    public PanelGenerator(String username, String password) throws WebServiceException{
        this(null, null, null, null, null, null, -1, username, password);
    }

    public PanelGenerator(final DataModel data_model, ServicesFilter fltr, final ListTypeAdapter list_type, final java.awt.event.ActionListener listener, final TaskSubmitter[] submitters, final OVExceptionHandler exception_handler, final int polling_delay) throws WebServiceException{
        this(data_model, fltr, list_type, listener, submitters, exception_handler, polling_delay, null, null);
    }

    public PanelGenerator(final DataModel data_model, ServicesFilter fltr, final ListTypeAdapter list_type, final java.awt.event.ActionListener listener, final TaskSubmitter[] submitters, final OVExceptionHandler exception_handler, final int polling_delay, String username, String password) throws WebServiceException{
        _dataModel = (data_model != null) ? data_model : this.loadLocalData();
        final ServicesFilter filter = ( fltr != null ) ? fltr : ServicesFilter.SORTED_SERVICES_FILTER;
		  Vector services = null;
		  try {
			  org.genepattern.util.PropertyFactory property = org.genepattern.util.PropertyFactory.getInstance();
			  Properties p = property.getProperties("omnigene.properties");
			  String url = p.getProperty("analysis.service.URL");
			  String siteName = p.getProperty("analysis.service.site.name", "Broad Institute");
			  TaskInfo[] tasks = new org.genepattern.analysis.AdminProxy(siteName, username, false).getLatestTasks();
			  services = new Vector();
			  for(int i = 0; i < tasks.length; i++) {
				  services.add(new AnalysisService(siteName, url, tasks[i]));
			  }
			  services = filter.processServices(services);
		  } catch(Throwable t) {
			  t.printStackTrace();
			  services = filter.processServices(RequestHandlerFactory.getInstance(username, password).getAllServices());
		  }
        //_dataModel.RequestHandlerFactory.getInstance(username, password).getAllServices();
        _dataModel.setAnalysisServices(services);
       System.out.println("PanelGenerator list_type="+list_type+" Exception handler="+exception_handler);
		 
        if( list_type != null ) 
            _tasks = new AnalysisTasksPanel(_dataModel, list_type, listener, submitters, exception_handler, username);
        else
            _tasks = new AnalysisTasksPanel(_dataModel, username);
        _history = new HistoryPanel(_dataModel);
        //_results = new ResultsPanel(_dataModel);

        _dataModel.addObserver(_tasks);
        _dataModel.addObserver(_history);
        //_dataModel.addObserver(_results);

        StatusFinder finder = (polling_delay > 0 ) ?
                new StatusFinder(_dataModel, polling_delay) :
                    new StatusFinder(_dataModel);
        _dataModel.addObserver(finder);
        Thread t = new Thread(finder);
        t.start();
    }

    /** gets the HistoryPanel */
    public HistoryPanel getHistoryPanel() {
        return _history;
    }
    /** gets the ResultsPanel */
   // public ResultsPanel getResultsPanel() {
   //     return _results;
   // }
    /** gets the AnalysisTasksPanel */
    public AnalysisTasksPanel getAnalysisTasksPanel() {
        return _tasks;
    }
    /** gets the DataModel */
    public DataModel getDataModel() {
        return _dataModel;
    }
    
    private DataModel loadLocalData(){
        try{
            File file = new File(_filename);
            if(file.exists()){
                return DataHandler.loadData(file);
            }
        }catch(Exception e){
            cat.error("Error in loading the local job data.", e);
        }
        return new DataModel();
    }
    /**
     * Saves the data inside data model.
     */
    public void saveDataModel(){
        try{
            DataHandler.saveData(this._dataModel, this._filename);
        }
        catch(Exception oe){
            cat.error("Unable to save local data.", oe);
        }
    }

}
