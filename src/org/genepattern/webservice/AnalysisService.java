package org.genepattern.webservice;


/**
 * <p>Title: AnalysisService.java </p>
 * <p>Description: includes all the information about an analysis Service</p>
 * @author Hui Gong
 * @version $Revision 1.2 $
 */

public class AnalysisService {
    private String _url;
    private TaskInfo _task;
    private String _name;

    public AnalysisService(String name, String url, TaskInfo task){
        this._name = name;
        this._url = url;
        this._task = task;
    }

    public String toString(){
        String display = this._task.getName()+" ( "+this._name+" )";
        return display;
    }

    public String getURL(){
        return this._url;
    }

    public TaskInfo getTaskInfo(){
        return this._task;
    }

    public String getName(){
        return this._name;
    }
}
