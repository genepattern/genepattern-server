/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.genepattern.server.webapp.ParameterInfoWrapper;
import org.genepattern.util.GPConstants;

public class TaskInfo implements Serializable {
    // The maximum size of the "short name"
    private static int shortNameLimit = 33;

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
            try {
                this.parameterInfoArray = ParameterFormatConverter.getParameterInfoArray(this.parameter_info);
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

    private transient Set<String> _inputFileTypes = null;
    private transient Map<String, List<ParameterInfoWrapper>> _kindToParameterInfoMap = null;
    
    /**
     * Get the map of kinds to relevant parameters
     * 
     * Note: To preserve compatibility with earlier versions of the SOAP client,
     *     This method deliberately named using a non JavaBean naming convention so that the axis serializer does not
     *     include the inputFileTypes parameter in the serialized bean.
     */
    public Map<String, List<ParameterInfoWrapper>> _getKindToParameterInfoMap() {
        //initialize
        _getInputFileTypes();
        
        return Collections.unmodifiableMap(_kindToParameterInfoMap);
    }
    
    /**
     * Get the list of input file types that this module accepts, by getting the input file types for each of its 
     * input file parameters.
     * 
     * Note: To preserve compatibility with earlier versions of the SOAP client,
     *     This method deliberately named using a non JavaBean naming convention so that the axis serializer does not
     *     include the inputFileTypes parameter in the serialized bean.
     * 
     * @return an unmodifiable set
     */
    public Set<String> _getInputFileTypes() {
        if (_inputFileTypes == null) {
            _inputFileTypes = initInputFileTypes();
        }
        return Collections.unmodifiableSet(_inputFileTypes);
    }
    
    /**
     * Get the list of parameters for a given file type.
     * Use this to generate a popup menu from a given file to a given input parameter in this taskInfo.
     * 
     * @param inputFileType
     * @return
     */
    public List<ParameterInfoWrapper> _getSendToParameterInfos(String inputFileType) {
        //initialize
        _getInputFileTypes();
        List<ParameterInfoWrapper> rval = _kindToParameterInfoMap.get(inputFileType);
        if (rval == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(rval);
    }
    
    private Set<String> initInputFileTypes() {
        Set<String> inputFileTypes = new HashSet<String>(); 
        _kindToParameterInfoMap = new HashMap<String,List<ParameterInfoWrapper>>();

        ParameterInfo[] paramInfos = getParameterInfoArray();
        if (paramInfos == null) {
            return Collections.emptySet();
        }
        for(ParameterInfo paramInfo : paramInfos) {
            if (paramInfo.isInputFile()) {
                //ParameterInfo info = p[i];
                String fileFormatsString = (String) paramInfo.getAttributes().get(GPConstants.FILE_FORMAT);
                if (fileFormatsString == null || fileFormatsString.equals("")) {
                    continue;
                }
                StringTokenizer st = new StringTokenizer(fileFormatsString, GPConstants.PARAM_INFO_CHOICE_DELIMITER);
                while (st.hasMoreTokens()) {
                    String type = st.nextToken();
                    inputFileTypes.add(type);
                    
                    List<ParameterInfoWrapper> pinfosForMap = _kindToParameterInfoMap.get(type);
                    if (pinfosForMap == null) {
                        pinfosForMap = new ArrayList<ParameterInfoWrapper>();
                        _kindToParameterInfoMap.put(type, pinfosForMap);
                    }
                    pinfosForMap.add(new ParameterInfoWrapper(paramInfo));
                }
            }
            if (paramInfo._isDirectory()) {
                inputFileTypes.add("directory");
                List<ParameterInfoWrapper> pinfosForMap = _kindToParameterInfoMap.get("directory");
                if (pinfosForMap == null) {
                    pinfosForMap = new ArrayList<ParameterInfoWrapper>();
                    _kindToParameterInfoMap.put("directory", pinfosForMap);
                }
                pinfosForMap.add(new ParameterInfoWrapper(paramInfo));
            }
        }
        return inputFileTypes;
    }

    private boolean eq(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1 != null) {
            return o1.equals(o2);
        }
        return false;
    }

    public boolean equals(Object otherThing) {
        if (otherThing == null) {
            return false;
        }
        if (!(otherThing instanceof TaskInfo)) {
            return false;
        }
        TaskInfo other = (TaskInfo) otherThing;
        return eq(this.userId, other.userId)
            && eq(this.accessId, other.accessId)
            && eq(this.taskInfoAttributes, other.taskInfoAttributes)
            && eq(this.taskName, other.taskName)
            && eq(this.taskID, other.taskID)
            && eq(this.description, other.description)
            && eq(this.parameter_info, other.parameter_info);
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
        TaskInfoAttributes tia = getTaskInfoAttributes();
        if (tia == null) {
            return false; // default to false if unknown
        }
        String type = (String) tia.get(GPConstants.TASK_TYPE);
        if (type == null) {
            return false; // default to false if unknown
        }
        return type.endsWith("pipeline");
    }
    
    public static boolean isVisualizer(TaskInfoAttributes tia) {
        if (tia == null) {
            return false;
        }
        return GPConstants.TASK_TYPE_VISUALIZER.equalsIgnoreCase(tia.get(GPConstants.TASK_TYPE));
    }

    public static boolean isJavascript(TaskInfoAttributes tia) {
        if (tia == null) {
            return false;
        }

        return (tia.get(GPConstants.CATEGORIES).contains(GPConstants.TASK_CATEGORY_JSVIEWER));
    }

}
