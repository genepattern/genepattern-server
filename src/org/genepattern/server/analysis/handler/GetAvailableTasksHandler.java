
package org.genepattern.server.analysis.handler;

import java.util.Vector;

import org.genepattern.analysis.OmnigeneException;
import org.genepattern.analysis.ParameterFormatConverter;
import org.genepattern.analysis.TaskInfo;
import org.genepattern.server.analysis.NoTaskFoundException;
import org.genepattern.server.analysis.ejb.AnalysisJobDataSource;
import org.genepattern.server.util.BeanReference;

//import edu.mit.wi.omnigene.omnidas.*;

/**
 * Class used to get available tasks
 *
 * @author  rajesh kuttan
 * @version 1.0
 */

public class GetAvailableTasksHandler extends RequestHandler {

    private String userId=null; 
    
    /** Creates new GetAvailableTasksHandler */
    public GetAvailableTasksHandler() {
        super();
    }
    
    
    public GetAvailableTasksHandler(String userId) {
        this.userId = userId;
    }
    
    /**
     * Fetches information abouts tasks
     * @throws NoTaskFoundException
     * @throws OmnigeneException
     * @return Vector of <CODE>TaskInfo</CODE>
     */
    public Vector executeRequest() throws OmnigeneException,NoTaskFoundException {
        Vector tasksVector=null;
        try {

            //Get EJB reference
            AnalysisJobDataSource ds = BeanReference.getAnalysisJobDataSourceEJB();
            //Invoke EJB function
            tasksVector = ds.getTasks(userId);
            if (tasksVector !=null) {
                TaskInfo taskInfo=null;
                for (int i=0;i<tasksVector.size();i++) {
                    taskInfo=(TaskInfo)tasksVector.get(i);
                    ParameterFormatConverter pfc = new ParameterFormatConverter();
                    taskInfo.setParameterInfoArray(pfc.getParameterInfoArray(taskInfo.getParameterInfo()));
                }
            }
            else {
                throw new OmnigeneException("GetAvailableTasksRequest:executeRequest  null value returned for TaskInfo");
            }

        } catch (NoTaskFoundException ex) {
            System.out.println("GetAvailableTasksRequest(executeRequest) NoTaskFoundException...");
            throw ex;
        } catch (Exception ex) {
            System.out.println("GetAvailableTasksRequest(executeRequest): Error " + ex.getMessage());
            ex.printStackTrace();
            throw new OmnigeneException(ex.getMessage());
        }
        return tasksVector;
    }




    public static void main(String args[]) {
        GetAvailableTasksHandler arequest = new GetAvailableTasksHandler("");
        try {
            Vector taskVector = arequest.executeRequest();
            System.out.println("Size " + taskVector.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}












