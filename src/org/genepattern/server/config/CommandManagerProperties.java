/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helper class for parsing the configuration yaml file.
 * 
 * @author pcarr
 */
public class CommandManagerProperties {

    private PropObj rootProps = new PropObj();
    private Map<String, PropObj> executorPropertiesMap = new LinkedHashMap<String, PropObj>();
    private Map<String,PropObj> groupPropertiesMap = new LinkedHashMap<String, PropObj>();
    private Map<String,PropObj> userPropertiesMap = new LinkedHashMap<String, PropObj>();
    
    public PropObj getTop() {
        return rootProps;
    }

    public void clear() {
        rootProps.clearDefaultProperties();
        rootProps.clearModuleProperties();
        this.executorPropertiesMap.clear();
        this.groupPropertiesMap.clear();
        this.userPropertiesMap.clear();
    }

    //initializes and puts the PropObj into the executorPropertiesMap
    public PropObj getPropsForExecutor(String cmdExecId) {
        PropObj propObj = executorPropertiesMap.get(cmdExecId);
        if (propObj == null) {
            propObj = new PropObj();
            executorPropertiesMap.put(cmdExecId, propObj);
        }
        return propObj;
    }

    //initializes and puts the PropObj into the groupPropertiesMap if necessary
    public PropObj getPropsForGroup(String groupId) {
        PropObj propObj = groupPropertiesMap.get(groupId);
        if (propObj == null) {
            propObj = new PropObj();
            groupPropertiesMap.put(groupId, propObj);
        }
        return propObj;
    }
    
    //initializes and puts the PropObj into the userPropertiesMap if necessary
    public PropObj getPropsForUser(String userId) {
        PropObj propObj = userPropertiesMap.get(userId);
        if (propObj == null) {
            propObj = new PropObj();
            userPropertiesMap.put(userId, propObj);
        }
        return propObj;
    }
}