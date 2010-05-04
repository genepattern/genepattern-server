package org.genepattern.server.executor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.server.UserAccountManager;
import org.genepattern.webservice.JobInfo;

/**
 * Helper class for managing configuration properties for the BasicCommandManager.
 * 
 * @author pcarr
 */
public class CommandManagerProperties {
    private static Logger log = Logger.getLogger(CommandManagerProperties.class);

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

    public Properties getCommandProperties(JobInfo jobInfo) {
        // 1) initialize from top level default properties
        Properties cmdProperties = new Properties();
        cmdProperties.putAll(this.rootProps.getDefaultProperties());
        
        if (jobInfo == null) {
            log.error("Unexpected null arg");
            return cmdProperties;
        }
        
        // 2) add/replace with executor default properties
        String cmdExecId = getCommandExecutorId(jobInfo);
        
        PropObj executorDefaultProps = executorPropertiesMap.get(cmdExecId);
        if (executorDefaultProps != null) {
            cmdProperties.putAll( executorDefaultProps.getDefaultProperties() );
        }
        
        // 3) add/replace with top level module properties ...
        cmdProperties.putAll(this.rootProps.getModuleProperties(jobInfo));
        
        // 4) add/replace with group properties
        String userId = jobInfo.getUserId();
        Set<String> groupIds = UserAccountManager.instance().getGroupMembership().getGroups(userId);
        if (groupIds != null) {
            if (groupIds.size() == 1) {
                //get first element from the set
                String groupId = groupIds.iterator().next();
                PropObj groupPropObj = groupPropertiesMap.get(groupId);
                if (groupPropObj != null) {
                    cmdProperties.putAll( groupPropObj.getDefaultProperties() );
                    cmdProperties.putAll( groupPropObj.getModuleProperties(jobInfo) );
                }
            }
            else {
                //special-case for a user who is in more than one group
                //must iterate through the groups in the same order as they appear in the config file
                for(Entry<String, PropObj> entry : groupPropertiesMap.entrySet()) {
                    if (groupIds.contains(entry.getKey())) {
                        PropObj groupPropObj = entry.getValue();
                        cmdProperties.putAll( groupPropObj.getDefaultProperties() );
                        cmdProperties.putAll( groupPropObj.getModuleProperties(jobInfo) );
                    }
                }
            }
        }
        
        // 5) add/replace with user properties
        PropObj userPropObj = this.userPropertiesMap.get(userId);
        if (userPropObj != null) {
            cmdProperties.putAll( userPropObj.getDefaultProperties() );
            cmdProperties.putAll( userPropObj.getModuleProperties(jobInfo) );
        }
        return cmdProperties;
    }

    public String getCommandExecutorId(JobInfo jobInfo) {
        final String key = "executor";
        //TODO: null check
        String userId = jobInfo.getUserId();
        Set<String> groupIds = UserAccountManager.instance().getGroupMembership().getGroups(userId);
        
        //1) check root properties
        String value = rootProps.getDefaultProperty(key);
        
        //2) check module.properties
        value = rootProps.getModuleProperty(jobInfo, key, value);
        
        //3) check group.properties
        if (groupIds != null) {
            if (groupIds.size() == 1) {
                //get first element from the set
                String groupId = groupIds.iterator().next();
                PropObj groupPropObj = groupPropertiesMap.get(groupId);
                if (groupPropObj != null) {
                    value = groupPropObj.getProperty(jobInfo, key, value);
                }
            }
            else {
                //special-case for a user who is in more than one group
                //must iterate through the groups in the same order as they appear in the config file
                for(Entry<String, PropObj> entry : groupPropertiesMap.entrySet()) {
                    if (groupIds.contains(entry.getKey())) {
                        value = entry.getValue().getProperty(jobInfo, key, value);
                    }
                }
            }
        }

        //5) check user.properties
        PropObj userPropObj = userPropertiesMap.get(userId);
        if (userPropObj != null) {
            value = userPropObj.getProperty(jobInfo, key, value);
        }
        return value;
    }
}