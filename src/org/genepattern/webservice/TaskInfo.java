/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.webservice;

/**
 * Used to hold information about particular Task
 * 
 * @author Rajesh Kuttan
 * @version 1.0
 */

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.genepattern.util.GPConstants;

public class TaskInfo implements Serializable {

    // The maximum size of the "short name"
    private static int shortNameLimit = 37;

    private int taskID = 0;
    private String taskName = "";
    private String description = "";
    private String parameter_info = "";
    private String shortName = null;
    private int accessId = 0;
    private String userId = null;

    // JL
    private Map taskInfoAttributes = null;
    private HashMap attributes = null;
    private ParameterInfo[] parameterInfoArray = null;

    /** Creates new TaskInfo */
    public TaskInfo() {
    }

    public TaskInfo(int taskID, String taskName, String description, String parameter_info) {
        this(taskID, taskName, description, parameter_info, null);
    }

    public TaskInfo(int taskID, String taskName, String description, String parameter_info,
            TaskInfoAttributes taskInfoAttributes) {
        this(taskID, taskName, description, parameter_info, taskInfoAttributes, null, 0);
    }

    public TaskInfo(int taskID, String taskName, String description, String parameter_info,
            TaskInfoAttributes taskInfoAttributes, String userId, int accessId) {
        this.taskID = taskID;
        this.taskName = taskName;
        this.description = description;
        this.parameter_info = parameter_info;
        this.taskInfoAttributes = taskInfoAttributes;
        this.userId = userId;
        this.accessId = accessId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setAccessId(int accessId) {
        this.accessId = accessId;
    }

    public int getAccessId() {
        return this.accessId;
    }

    public TaskInfoAttributes getTaskInfoAttributes() {
        // JL
        return (TaskInfoAttributes) taskInfoAttributes;
    }

   
    public TaskInfoAttributes giveTaskInfoAttributes() {
        // JL
        if (taskInfoAttributes == null) taskInfoAttributes = new TaskInfoAttributes();
        return (TaskInfoAttributes) taskInfoAttributes;
    }

    public void setTaskInfoAttributes(Map taskInfoAttributes) {
        if (taskInfoAttributes instanceof TaskInfoAttributes) {
            this.taskInfoAttributes = (TaskInfoAttributes) taskInfoAttributes;
        }
        else {
            this.taskInfoAttributes = new TaskInfoAttributes(taskInfoAttributes);
        }
    }

/** the following get/set attributes is a hack for Axis 1.4.  Axis is unable to deal
    with TaskInfoAttributes as a subclass of HashMap and since we dare not break the 
	existing API adding this additional instvar allows the attributes to get passed 
	down properly to SOAP clients without having to write custom (de/)serializers
	for the TaskInfoAttributes classes.  Ted 11/22/06
**/
    public HashMap getAttributes() {
	  if (attributes == null) attributes = (HashMap)taskInfoAttributes;        

        return  attributes;
    }

    public void setAttributes(HashMap taskInfoAttributes) {
	  setTaskInfoAttributes(taskInfoAttributes);
        this.attributes = taskInfoAttributes;
        
    }


    public String getName() {
        return taskName;
    }

    public String getShortName() {
        if (shortName == null) {
            if (taskName.length() <= shortNameLimit) {
                shortName = taskName;
            }
            else {
                int stop1 = shortNameLimit / 2;
                int start2 = taskName.length() - stop1 + 3;
                shortName = taskName.substring(0, stop1) + "..." + taskName.substring(start2);
            }
        }
        return shortName;

    }

    public void setName(java.lang.String taskName) {
        this.taskName = taskName;
    }

    public int getID() {
        return taskID;
    }

    public void setID(int taskID) {
        this.taskID = taskID;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(java.lang.String description) {
        this.description = description;
    }

    /**
     * get parameter info jaxb string
     * 
     * @return parameter_info
     */
    public String getParameterInfo() {
        return parameter_info;
    }

    /**
     * get <CODE>ParameterInfo</CODE> array
     * 
     */
    public void setParameterInfo(java.lang.String parameter_info) {
        this.parameter_info = parameter_info;
    }

    /**
     * get <CODE>ParameterInfo</CODE> array
     * 
     * @return parameterInfoArray
     */
    public ParameterInfo[] getParameterInfoArray() {
        // add checking in here --Hui
        if (this.parameterInfoArray == null && this.parameter_info != null) {
            ParameterFormatConverter converter = new ParameterFormatConverter();
            try {
                this.parameterInfoArray = converter.getParameterInfoArray(this.parameter_info);
            }
            catch (OmnigeneException e) {
            }
        }
        return this.parameterInfoArray;
    }

    /**
     * set <CODE>ParameterInfo</CODE> array
     * 
     * @param parameterInfoArray
     */
    public void setParameterInfoArray(ParameterInfo[] parameterInfoArray) {
        this.parameterInfoArray = parameterInfoArray;
    }

    /**
     * Checks to see if the TaslInfo contains a input file parameter field
     * 
     * @return true if it contains a
     *         <code>ParamterInfo<code> object with TYPE as FILE and MODE as INPUT
     */
    public boolean containsInputFileParam() {
        // add convinent checking method --Hui
        if (this.parameterInfoArray != null) {
            for (int i = 0; i < this.parameterInfoArray.length; i++) {
                if (this.parameterInfoArray[i].isInputFile()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean equals(Object otherThing) {
        if (!(otherThing instanceof TaskInfo)) return false;
        TaskInfo other = (TaskInfo) otherThing;
        return getUserId().equals(other.getUserId())
                && getAccessId() == other.getAccessId()
                && ((getTaskInfoAttributes() == null && other.getTaskInfoAttributes() == null) || (getTaskInfoAttributes() != null
                        && other.getTaskInfoAttributes() != null && getTaskInfoAttributes().equals(
                        other.getTaskInfoAttributes())))
                && getName().equals(other.getName())
                && getID() == other.getID()
                && getDescription().equals(other.getDescription())
                && ((getParameterInfo() == null && other.getParameterInfo() == null) || (getParameterInfo() != null
                        && other.getParameterInfo() != null && getParameterInfo().equals(other.getParameterInfo())));
    }

    public int hashCode() {
        if (this.getTaskInfoAttributes() == null) {
            return super.hashCode();
        }
        return this.getTaskInfoAttributes().get("LSID").hashCode();
    }

    public String getLsid() {
        return (String) giveTaskInfoAttributes().get("LSID");
    }

    public boolean isPipeline() {
        Map tia = getTaskInfoAttributes();
        if (tia == null) return false; // default to false if unknown

        String type = (String) tia.get(GPConstants.TASK_TYPE);
        if (type == null) return false; // default to false if unknown

        return type.endsWith("pipeline");

    }
    

}