
package org.genepattern.server.analysis.handler;


import org.genepattern.server.analysis.*;
import org.genepattern.server.analysis.ejb.*;
import org.genepattern.server.handler.*;
import org.genepattern.server.jaxb.analysis.job.*;
import org.genepattern.server.jaxb.analysis.parameter.*;
import org.genepattern.server.util.*;

//import edu.mit.wi.omnigene.omnidas.*;


/**
 * Class used to Remove existing task
 *
 * @author  rajesh kuttan
 * @version 1.0
 */

public class RemoveTaskHandler extends RequestHandler {

    private int taskID=0;


    public RemoveTaskHandler() {
    }

    /**
     * Constructor accepts taskID
     *
     * @param taskID
     */
    public RemoveTaskHandler(int taskID) {
        this.taskID=taskID;
    }


     /**
      * Removes task based on taskID and returns no. of deleted records
      * @throws TaskIDNotFoundException
      * @throws OmnigeneException OmnigeneException
      * @return No.of records deleted
      */
    public int executeRequest() throws OmnigeneException,TaskIDNotFoundException {
        int recordDeleted=0;
        try {

            //Get EJB reference
            AnalysisJobDataSource ds = BeanReference.getAnalysisJobDataSourceEJB();
            //Invoke EJB function
            recordDeleted = ds.deleteTask(taskID);
        }catch (TaskIDNotFoundException taskEx) {
            System.out.println("RemoveTaskRequest(executeRequest): TaskIDNotFoundException " + taskID);
            throw taskEx;
        }catch (Exception ex) {
            System.out.println("RemoveTaskRequest(execute): Error " + ex.getMessage());
            ex.printStackTrace();
            throw new OmnigeneException(ex.getMessage());
        }

        return recordDeleted;
    }


}
