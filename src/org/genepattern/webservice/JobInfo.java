
package org.genepattern.webservice;

import java.util.*;
import java.io.*;




/**
 * Used to hold information about particular job
 *
 * @author Rajesh Kuttan, Hui Gong
 * @version $Revision 1.7$
 */

public class JobInfo implements Serializable {

    private int jobNo=0;
    private int taskID=0;
    private String status="";
    private String inputFileName="";
    private ParameterInfo[] parameterInfoArray= null;
    private String resultFileName="";

    private Date submittedDate=null;
    private Date completedDate=null;


    private String userId = null; 
    
    public JobInfo(){
	parameterInfoArray = new ParameterInfo[0];
    }

    /**
     * @param jobNo
     * @param taskID
     * @param status
     * @param submittedDate
     * @param completedDate
     * @param parameter_info
     * @param userId
     */

    public JobInfo(int jobNo, int taskID, String status,  Date submittedDate,Date completedDate, ParameterInfo[] parameters, String userId) {
        this.jobNo = jobNo;
        this.taskID = taskID;
        this.status=status;
        this.submittedDate = submittedDate;
        this.completedDate = completedDate;
        this.parameterInfoArray = parameters;
        this.userId = userId;
    }

	 
	 /**  
	 * Removes all parameters with the given name.
	 * @param parameterInfoName the parameter name.
	 * @return true if the parameter was found; false otherwise.
	 */
	 public boolean removeParameterInfo(String parameterInfoName) {
		 if(parameterInfoArray==null) {
				return false; 
		 }
		 java.util.List newParameterInfoList = new java.util.ArrayList();
		 int sizeBeforePossibleRemoval = parameterInfoArray.length;
		 
		 for(int i = 0, length = parameterInfoArray.length; i < length; i++) {
			 if(!parameterInfoArray[i].getName().equals(parameterInfoName)) {
				 newParameterInfoList.add(parameterInfoArray[i]);
			 } 
		 }
		 
		 parameterInfoArray = (ParameterInfo[]) newParameterInfoList.toArray(new ParameterInfo[0]);
		 return(parameterInfoArray.length < sizeBeforePossibleRemoval); 
			
	 }

    /**
     * @param jobNo
     * @param taskID
     * @param parameter_info
     * @param inputFileName  */
    public JobInfo(int jobNo, int taskID, ParameterInfo[] parameters,String user_id) {
        this.jobNo=jobNo;
        this.taskID=taskID;
        this.parameterInfoArray = parameters;
        this.userId = user_id;
    }


    /**
     * @return  */
    public int getJobNumber() {
        return jobNo;
    }

    /**
     * @param jobNo  */
    public void setJobNumber(int jobNo) {
        this.jobNo= jobNo;
    }

    /**
     * @return  */
    public String getStatus() {
        return status;
    }

    /**
     * @param status  */
    public void setStatus(String status) {
        this.status= status;
    }



    /**
     * @return  */
    public int getTaskID() {
        return taskID;
    }

    /**
     * @param taskID  */
    public void setTaskID(int taskID) {
        this.taskID=taskID;
    }


    /**
     * @return  */
    public Date  getDateSubmitted() {
        return submittedDate;
    }

    /**
     * @param submittedDate  */
    public void setDateSubmitted(Date  submittedDate) {
        this.submittedDate=submittedDate;
    }

    /**
     * @return  */
    public Date getDateCompleted() {
        return completedDate;
    }

    /**
     * @param completedDate  */
    public void setDateCompleted(Date  completedDate) {
        this.completedDate=completedDate;
    }

    /**
     * @return  
     * @deprecated
     */
    public String getInputFileName() {
        return inputFileName;
    }

    /**
     * @param inputFileName  
     * @deprecated
     */
    public void setInputFileName(String  inputFileName) {
        this.inputFileName=inputFileName;
    }


    /**
     * get parameter info jaxb string
     * @return  */
    public String getParameterInfo() throws OmnigeneException{
        String parameter_info="";
        ParameterFormatConverter converter = new ParameterFormatConverter();
        if(this.parameterInfoArray!=null){
            parameter_info = converter.getJaxbString(this.parameterInfoArray); 
        }
        return parameter_info;
    }



    /**
     * get <CODE>ParameterInfo</CODE> array
     * @return  */
    public ParameterInfo[] getParameterInfoArray() {
        return parameterInfoArray;
    }

    /**
     * set <CODE>ParameterInfo</CODE> array
     * @param parameterInfoArray  */
    public void setParameterInfoArray(ParameterInfo[]  parameterInfoArray) {
        this.parameterInfoArray=parameterInfoArray;
    }

    /**
     * Add new ParameterInfo into the JobInfo
     * @param param
     */
    public void addParameterInfo(ParameterInfo param){
        int size=0;
        if(this.parameterInfoArray!=null) size = this.parameterInfoArray.length;
        ParameterInfo[] params = new ParameterInfo[size+1];
        for(int i=0; i< size; i++){
            params[i]=this.parameterInfoArray[i];
        }
        params[size] = param;
        this.parameterInfoArray = params;
    }

    /**
     * @return  
     * @deprecated
     */
    public String getResultFileName() {
        return resultFileName;
    }

    /**
     * @param resultFileName  
     * @deprecated
     */
    public void setResultFileName(String  resultFileName) {
        this.resultFileName=resultFileName;
    }

    /**
     * Checks to see if the JobInfo contains a input file parameter field
     * @return true if it contains a <code>ParamterInfo<code> object with TYPE as FILE and MODE as INPUT
     */
    public boolean containsInputFileParam(){
        if(this.parameterInfoArray != null){
            for(int i=0; i<this.parameterInfoArray.length; i++){
                if(this.parameterInfoArray[i].isInputFile()){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks to see if the JobInfo contains a output file parameter field
     * @return true if it contains a <code>ParamterInfo<code> object with TYPE as FILE and MODE as OUTPUT
     */
    public boolean containsOutputFileParam(){
        if(this.parameterInfoArray != null){
            for(int i=0; i<this.parameterInfoArray.length; i++){
                if(this.parameterInfoArray[i].isOutputFile()){
                    return true;
                }
            }
        }
        return false;
    }
    
    public void setUserId(String userId) { 
        this.userId = userId; 
    }
    
    public String getUserId() { 
        return this.userId;
    }
    
    /** standard method */
    public String toString() {
        return "JobInfo[jobNo="+jobNo+" taskID="+taskID
            +" status="+status+" inputFileName="+inputFileName
            +" resultFileName="+resultFileName
            +" submittedDate="+submittedDate
            +" completedDate="+completedDate
            +" userId="+userId
            +" parameterInfoArray="+parameterInfoArray+"]";
    }
    
}
