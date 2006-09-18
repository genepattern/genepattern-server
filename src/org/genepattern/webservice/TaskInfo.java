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
 */

import java.io.Serializable;

public class TaskInfo implements Serializable {

    private int id = 0;

    private String name = "", description = "", parameterInfo = "";

    private int accessId = 0;

    private String userId;

    private TaskInfoAttributes taskInfoAttributes;

    private ParameterInfo[] parameterInfoArray;

    public TaskInfo() {
    }

    public TaskInfo(int taskID, String name, String description, String parameterInfo) {
        this(taskID, name, description, parameterInfo, null);
    }

    public TaskInfo(int taskID, String name, String description, String parameterInfo,
            TaskInfoAttributes taskInfoAttributes) {
        this(taskID, name, description, parameterInfo, taskInfoAttributes, null, 0);
    }

    public TaskInfo(int taskID, String name, String description, String parameterInfo,
            TaskInfoAttributes taskInfoAttributes, String userId, int accessId) {
        this.id = taskID;
        this.name = name;
        this.description = description;
        this.parameterInfo = parameterInfo;
        this.taskInfoAttributes = taskInfoAttributes;
        this.userId = userId;
        this.accessId = accessId;
    }

    /**
     * Checks to see if the TaskInfo contains a input file parameter field
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
        if (!(otherThing instanceof TaskInfo)) {
            return false;
        }
        TaskInfo other = (TaskInfo) otherThing;
        return getUserId().equals(other.getUserId())
                && getAccessId() == other.getAccessId()
                && ((getTaskInfoAttributes() == null && other.getTaskInfoAttributes() == null) || (getTaskInfoAttributes() != null
                        && other.getTaskInfoAttributes() != null && getTaskInfoAttributes().equals(
                        other.getTaskInfoAttributes())))
                && getName().equals(other.getName())
                && getId() == other.getId()
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

    /**
     * get parameter info jaxb string
     * 
     * @return parameterInfo
     */
    public String getParameterInfo() {
        return parameterInfo;
    }

    /**
     * set parameter info jaxb string
     * 
     */
    public void setParameterInfo(java.lang.String parameterInfo) {
        this.parameterInfo = parameterInfo;
    }

    public ParameterInfo[] getParameterInfoArray() {

        if (this.parameterInfoArray == null && this.parameterInfo != null) {
            ParameterFormatConverter converter = new ParameterFormatConverter();
            try {
                this.parameterInfoArray = converter.getParameterInfoArray(this.parameterInfo);
            }
            catch (OmnigeneException e) {
                e.printStackTrace();
            }
        }
        return this.parameterInfoArray;
    }

    public void setParameterInfoArray(ParameterInfo[] parameterInfoArray) {
        this.parameterInfoArray = parameterInfoArray;
    }

    public int getAccessId() {
        return accessId;
    }

    public void setAccessId(int accessId) {
        this.accessId = accessId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public TaskInfoAttributes getTaskInfoAttributes() {
        if (taskInfoAttributes == null) {
            taskInfoAttributes = new TaskInfoAttributes();
        }
        return taskInfoAttributes;
    }

    public void setTaskInfoAttributes(TaskInfoAttributes taskInfoAttributes) {
        this.taskInfoAttributes = taskInfoAttributes;
    }

    public TaskInfoAttributes giveTaskInfoAttributes() {
        return getTaskInfoAttributes();
    }

    public int getID() {
        return getId();
    }

}