
package org.genepattern.server.analysis.handler;

import org.genepattern.server.analysis.TaskIDNotFoundException;
import org.genepattern.server.analysis.ejb.AnalysisJobDataSource;
import org.genepattern.server.util.BeanReference;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.ParameterInfo;

//import edu.mit.wi.omnigene.omnidas.*;

/**
 * AddNewJobHandler to submit a job request and get back <CODE>JobInfo</CODE>
 *
 * @author  rajesh kuttan
 * @version 1.0
 */

public class AddNewJobHandler extends RequestHandler {

    private int taskID = 1;
    private String parameter_info="",inputFileName="";
    private ParameterInfo[] parameterInfoArray=null;
    private String userID; 
    /** Creates new GetAvailableTasksHandler */
    public AddNewJobHandler() {
        super();
    }

    /**
     * Constructor with taskID, ParameterInfo[] and inputFileName
     * @param taskID taskID from <CODE>TaskInfo</CODE>
     * @param parameterInfoArray <CODE>ParameterInfo</CODE>
     * @param inputFileName String
     */
    public AddNewJobHandler(int taskID,String userID, ParameterInfo[] parameterInfoArray,String inputFileName) {
        this.taskID=taskID;
        this.userID=userID;
        this.parameterInfoArray=parameterInfoArray;
        this.inputFileName=inputFileName;
    }

    /**
     * Creates job. Call this fun. if you need JobInfo object
     * @throws TaskIDNotFoundException TaskIDNotFoundException
     * @throws OmnigeneException
     * @return <CODE>JobIndo</CODE>
     */
    public JobInfo executeRequest() throws OmnigeneException,TaskIDNotFoundException {
        JobInfo ji = null;
        try {
            ParameterFormatConverter pfc = new ParameterFormatConverter();
            parameter_info = pfc.getJaxbString(parameterInfoArray);
            //Get EJB reference
            AnalysisJobDataSource ds = BeanReference.getAnalysisJobDataSourceEJB();
            //Invoke EJB function
            ji = ds.addNewJob(taskID,userID,parameter_info,inputFileName);
            //Checking for null
            if (ji==null)
                throw new OmnigeneException("AddNewJobRequest:executeRequest Operation failed, null value returned for JobInfo");

	    synchronized(ds) {
	    	//System.out.println("AddNewJobHandler: notifying ds about new job to run");
	    	ds.notify();
	    }
            //Reparse parameter_info before sending to client
            ji.setParameterInfoArray(pfc.getParameterInfoArray(parameter_info));
        } catch (TaskIDNotFoundException taskEx) {
            System.out.println("AddNewJob(executeRequest): TaskIDNotFoundException " + taskID);
            throw taskEx;
        } catch (Exception ex) {
            System.out.println("AddNewJob(executeRequest): Error " + ex.getMessage());
            ex.printStackTrace();
            throw new OmnigeneException(ex.getMessage());
        }

        return ji;
    }


    public static void main(String args[]) {
        ParameterInfo[] parameterArray = new ParameterInfo[] { new ParameterInfo("-p","1000","description")};
        AddNewJobHandler arequest = new AddNewJobHandler(1,"omnigene_user",parameterArray,"filename");
        try {
          JobInfo ji = arequest.executeRequest();


          System.out.println("Execute Result " + ji.getJobNumber() + ji.getParameterInfo());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}












