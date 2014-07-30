package org.genepattern.server.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.drm.JobRunner;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.executor.CommandProperties;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.ParamValue;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.server.config.Value;
import org.genepattern.webservice.JobInfo;

/**
 * Helper class for managing properties loaded from the configuration yaml file.
 * 
 * @author pcarr
 */
public class ConfigYamlProperties {
    private static Logger log = Logger.getLogger(ConfigYamlProperties.class);

    private final IGroupMembershipPlugin groupInfo;
    private final JobConfigObj jobConfigObj;
    private PropObj rootProps = new PropObj();
    private Map<String, PropObj> executorPropertiesMap = new LinkedHashMap<String, PropObj>();
    private Map<String,PropObj> groupPropertiesMap = new LinkedHashMap<String, PropObj>();
    private Map<String,PropObj> userPropertiesMap = new LinkedHashMap<String, PropObj>();
    
    public ConfigYamlProperties(final JobConfigObj jobConfigObj, final IGroupMembershipPlugin groupInfo) {
        this.jobConfigObj=jobConfigObj;
        this.groupInfo=groupInfo;
    }
    
    public JobConfigObj getJobConfiguration() {
        return jobConfigObj;
    }
    
    public PropObj getTop() {
        return rootProps;
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

    private Value getExecutorPropsKey(final GpContext context, final String cmdExecId) {
        return getValue(context, "executor.props", null, cmdExecId);
    }

    public String getProperty(final GpContext context, final String key) {
        Value value = getValue(context, key);
        if (value == null) {
            return null;
        }
        if (value.getNumValues() > 1) {
            log.error("returning first item of a "+value.getNumValues()+" item list");
        }
        return value.getValue();
    }

    public Value getValue(final GpContext context, final String key) {
        if (key==null) {
            log.debug("key == null");
            return null;
        }
        Value drmCustomProps=null;
        String cmdExecId=null;
        if (!key.equals("executor")) {
            final Value cmdExecutorId=getValue(context, "executor");
            if (cmdExecutorId != null) {
                cmdExecId=cmdExecutorId.getValue();
            }
        }
        if (!key.equals("executor.props")) {
            final Value drmCustomPropsKey=getExecutorPropsKey(context, cmdExecId);
            if (drmCustomPropsKey != null) {
                Map<?,?> executorPropertiesMap=
                        jobConfigObj.getExecutorPropertiesMap();
                if (executorPropertiesMap != null &&
                        executorPropertiesMap.containsKey(drmCustomPropsKey.getValue())) {
                    try {
                        drmCustomProps=Value.parse(executorPropertiesMap.get(drmCustomPropsKey.getValue()));
                    }
                    catch (Throwable t) {
                        log.error(t);
                    }
                }
            }
        }
        return getValue(context, key, drmCustomProps, cmdExecId);
    }

    private Value getValue(final GpContext context, final String key, final Value drmCustomProps, final String cmdExecId) {
        Value rval = null;
        // 0) initialize from system properties and legacy properties files
        //    only if specified by the context
        //rval = ServerProperties.instance().getValue(context, key);

        // 1) replace with default properties set in the job_configuration yaml file ...
        if (this.rootProps.getDefaultProperties().containsKey(key)) {
            rval = this.rootProps.getDefaultValue(key);
        }
        
        if (context==null) {
            log.error("context==null");
            return rval;
        }
        
        // 2) replace with executor default properties ...
        if (cmdExecId != null) {
            //if (context.getJobInfo() != null) {
                final PropObj executorDefaultProps = executorPropertiesMap.get(cmdExecId);
                if (executorDefaultProps != null) {
                    if (executorDefaultProps.getDefaultProperties().containsKey(key)) {
                        rval = executorDefaultProps.getDefaultValue(key);
                    }
                }
            //}
        }

        // 2a) replace with executor custom properties
        if (drmCustomProps != null) {
            if (drmCustomProps.isMap()) {
                if (drmCustomProps.getMap().containsKey(key)) {
                    try {
                        rval=Value.parse(drmCustomProps.getMap().get(key));
                    }
                    catch (Throwable t) {
                        log.error("Error parsing custom value for key="+key, t);
                    }
                }
            }
        }
        
        // 2b) special-case, check for 'job.*' prefixed properties in the module manifest file
        if (context.getTaskInfo() != null 
            &&
            key.startsWith(JobRunner.PROP_PREFIX)
            &&
            context.getTaskInfo().giveTaskInfoAttributes() != null
            &&
            context.getTaskInfo().giveTaskInfoAttributes().containsKey(key)
        ) {
            rval=new Value(context.getTaskInfo().giveTaskInfoAttributes().get(key));
        }
        
        // 3) replace with top level module properties ...
        //    use either or both of context.jobInfo and context.taskInfo, when both are set, jobInfo takes precedence
        if (context.getJobInfo() == null && context.getTaskInfo() != null) {
            if (this.rootProps.getModuleProperties(context.getTaskInfo()).containsKey(key)) {
                rval = this.rootProps.getModuleProperties(context.getTaskInfo()).get(key);
            }
        }
        if (context.getJobInfo() != null) {
            if (this.rootProps.getModuleProperties(context.getJobInfo()).containsKey(key)) {
                rval = this.rootProps.getModuleProperties(context.getJobInfo()).get(key);
            }
        }

        // 4) replace with group properties ...
        if (context.getUserId() != null) {
            Set<String> groupIds = null;
            if (groupPropertiesMap != null && groupPropertiesMap.size() > 0) {
                if (groupInfo != null) {
                    groupIds = groupInfo.getGroups(context.getUserId());
                }
                else {
                    groupIds = UserAccountManager.instance().getGroupMembership().getGroups(context.getUserId());
                }
            }
            if (groupIds != null) {
                if (groupIds.size() == 1) {
                    // get first element from the set
                    String groupId = groupIds.iterator().next();
                    PropObj groupPropObj = groupPropertiesMap.get(groupId);
                    if (groupPropObj != null) {
                        if (groupPropObj.getDefaultProperties().containsKey(key)) {
                            rval = groupPropObj.getDefaultValue(key);
                        }
                        if (context.getTaskInfo() != null) {
                            if (groupPropObj.getModuleProperties(context.getTaskInfo()).containsKey(key)) {
                                rval = groupPropObj.getModuleProperties(context.getTaskInfo()).get(key);
                            }
                        }
                        if (context.getJobInfo() != null) {
                            if (groupPropObj.getModuleProperties(context.getJobInfo()).containsKey(key)) {
                                rval = groupPropObj.getModuleProperties(context.getJobInfo()).get(key);
                            }
                        }
                    }
                }
                else {
                    // special-case for a user who is in more than one group
                    // must iterate through the groups in the same order as they
                    // appear in the config file
                    for (Entry<String, PropObj> entry : groupPropertiesMap.entrySet()) {
                        if (groupIds.contains(entry.getKey())) {
                            PropObj groupPropObj = entry.getValue();
                            if (groupPropObj.getDefaultProperties().containsKey(key)) {
                                rval = groupPropObj.getDefaultProperties().get(key);
                            }
                            if (context.getJobInfo() == null && context.getTaskInfo() != null) {
                                if(groupPropObj.getModuleProperties(context.getTaskInfo()).containsKey(key)) {
                                    rval = groupPropObj.getModuleProperties(context.getTaskInfo()).get(key);
                                } 
                            }
                            if (context.getJobInfo() != null) {
                                if(groupPropObj.getModuleProperties(context.getJobInfo()).containsKey(key)) {
                                    rval = groupPropObj.getModuleProperties(context.getJobInfo()).get(key);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 5) replace with user properties ...
        if (context.getUserId() != null) {
            PropObj userPropObj = this.userPropertiesMap.get(context.getUserId());
            if (userPropObj != null) {
                if (userPropObj.getDefaultProperties().containsKey(key)) {
                    rval = userPropObj.getDefaultValue(key);
                }
                if (context.getJobInfo() == null && context.getTaskInfo() != null) {
                    if (userPropObj.getModuleProperties(context.getTaskInfo()).containsKey(key)) {
                        rval = userPropObj.getModuleProperties(context.getTaskInfo()).get(key);
                    }
                }
                if (context.getJobInfo() != null) {
                    if (userPropObj.getModuleProperties(context.getJobInfo()).containsKey(key)) {
                        rval = userPropObj.getModuleProperties(context.getJobInfo()).get(key);
                    }
                }
            }
        }
        
        // 6) TODO: from taskInfo, replace with module command line parameters with default values ...
        
        // 7) TODO: from jobInfo, replace with job command line parameters supplied by the user

        // 8) replace with job input values
        JobInput jobInput=context.getJobInput();
        if (jobInput != null && jobInput.hasValue(key)) {
            List<String> values=new ArrayList<String>();
            for(final ParamValue pval : jobInput.getParamValues(key)) {
                values.add( pval.getValue() );
            }
            rval=new Value(values);
        }
        return rval;
    }

    /**
     * @deprecated, use getValue(context,key) instead.
     * @param jobInfo
     * @return
     */
    public CommandProperties getCommandProperties(final JobInfo jobInfo) {
        CommandProperties cmdProperties = new CommandProperties();
        
        if (jobInfo == null) {
            log.error("Unexpected null arg");
            return cmdProperties;
        }
        
        // 1) initialize from top level default properties
        cmdProperties.putAll(this.rootProps.getDefaultProperties());
        
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
        if (userId != null) {
            Set<String> groupIds = null;
            if (groupPropertiesMap != null && groupPropertiesMap.size() > 0) { 
                if (groupInfo != null) {
                     groupIds = groupInfo.getGroups(userId);
                }
                else {
                    groupIds = UserAccountManager.instance().getGroupMembership().getGroups(userId);
                }
            }
            
            if (groupIds != null) {
                if (groupIds.size() == 1) {
                    // get first element from the set
                    String groupId = groupIds.iterator().next();
                    PropObj groupPropObj = groupPropertiesMap.get(groupId);
                    if (groupPropObj != null) {
                        cmdProperties.putAll(groupPropObj.getDefaultProperties());
                        if (jobInfo != null) {
                            cmdProperties.putAll(groupPropObj.getModuleProperties(jobInfo));
                        }
                    }
                }
                else {
                    // special-case for a user who is in more than one group
                    // must iterate through the groups in the same order as they
                    // appear in the config file
                    for (Entry<String, PropObj> entry : groupPropertiesMap.entrySet()) {
                        if (groupIds.contains(entry.getKey())) {
                            PropObj groupPropObj = entry.getValue();
                            cmdProperties.putAll(groupPropObj.getDefaultProperties());
                            if (jobInfo != null) {
                                cmdProperties.putAll(groupPropObj.getModuleProperties(jobInfo));
                            }
                        }
                    }
                }
            }
        }
        
        // 5) add/replace with user properties
        if (userId != null) {
            PropObj userPropObj = this.userPropertiesMap.get(userId);
            if (userPropObj != null) {
                cmdProperties.putAll( userPropObj.getDefaultProperties() );
                if (jobInfo != null) {
                    cmdProperties.putAll( userPropObj.getModuleProperties(jobInfo) );
                }
            }
        }
        return cmdProperties;
    }

    /**
     * @deprecated, does not allow setting the executorId in the 'executor.props' entry,
     *     does not allow setting the executorId from the job input form.
     *     
     * @param jobInfo
     * @return
     */
    public String getCommandExecutorId(JobInfo jobInfo) {
        final String key = "executor";
        //TODO: null check
        String userId = jobInfo.getUserId();
        
        //1) check root properties
        String value = rootProps.getDefaultProperty(key);
        
        //2) check module.properties
        value = rootProps.getModuleProperty(jobInfo, key, value);
        
        //3) check group.properties
        Set<String> groupIds = null;
        if (groupPropertiesMap != null && groupPropertiesMap.size() > 0) {
            groupIds = UserAccountManager.instance().getGroupMembership().getGroups(userId);
        }
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