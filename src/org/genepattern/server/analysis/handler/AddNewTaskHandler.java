
package org.genepattern.server.analysis.handler;

import java.util.Vector;

import org.genepattern.analysis.OmnigeneException;
import org.genepattern.analysis.ParameterFormatConverter;
import org.genepattern.analysis.ParameterInfo;
import org.genepattern.analysis.TaskInfo;
import org.genepattern.server.analysis.AnalysisManager;
import org.genepattern.server.analysis.ejb.AnalysisJobDataSource;
import org.genepattern.server.util.BeanReference;

//import edu.mit.wi.omnigene.omnidas.*;

/**
 * AddNewTaskHandler to submit a job request and get back <CODE>TaskInfo</CODE>
 *
 * @author  rajesh kuttan
 * @version 1.0
 */

public class AddNewTaskHandler extends RequestHandler {


    private String taskName="",description="",parameter_info="",className="", taskInfoAttributes=null;
    private ParameterInfo[] parameterInfoArray=null ;

    private String userId; 
    private int accessId;
    
    /** Creates new GetAvailableTaskHandler */
    public AddNewTaskHandler() {
        super();
    }

    /**
     * Constructor with Task parameters
     * @param taskName
     * @param description
     * @param parameterInfoArray
     * @param className
     */
   public AddNewTaskHandler(String userId, int accessId,String taskName,String description, ParameterInfo[] parameterInfoArray, String className, String taskInfoAttributes) {
        this.userId=userId;
        this.accessId=accessId;
        this.taskName=taskName;
        this.description=description;
        this.parameterInfoArray=parameterInfoArray;
        this.className=className;
	this.taskInfoAttributes = taskInfoAttributes;
    }


    /**
     * Adds new Task and returns <CODE>TaskInfo</CODE>
     * @throws OmnigeneException
     * @return taskID
     */
    public int executeRequest() throws OmnigeneException {
         int taskID = 0;

         try {

            //Get EJB reference
            AnalysisJobDataSource ds = BeanReference.getAnalysisJobDataSourceEJB();
	    AnalysisManager analysisManager = AnalysisManager.getInstance();

	    GetAvailableTasksHandler th = new GetAvailableTasksHandler();
	    Vector vTasks = th.executeRequest();
            if (vTasks !=null) {
                TaskInfo taskInfo=null;
                for (int i=0;i<vTasks.size();i++) {
                    taskInfo=(TaskInfo)vTasks.get(i);
		    if (taskInfo.getName().equalsIgnoreCase(this.taskName)) {
			    taskID = taskInfo.getID();
			    System.out.println("updating existing task ID: " + taskID);
			    analysisManager.stop(taskInfo.getName());
			    ds.deleteTask(taskID);
			    break;
		    }
                }
            }

            ParameterFormatConverter pfc = new ParameterFormatConverter();
            parameter_info = pfc.getJaxbString(parameterInfoArray);

            //Invoke EJB function

            taskID = ds.addNewTask(taskName,userId,accessId,description,parameter_info,className,taskInfoAttributes);
	    analysisManager.startNewAnalysisTask(taskID);

        } catch (Exception ex) {
            System.out.println("AddNewTaskRequest(execute): Error " + ex.getMessage());
            ex.printStackTrace();
            throw new OmnigeneException(ex.getMessage());
        }
        return taskID;
    }



    public static void main(String args[]) {
        AddNewTaskHandler arequest = new AddNewTaskHandler();
        try {
          System.out.println("Execute Result " + arequest.executeRequest());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
