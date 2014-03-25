package org.genepattern.server.job.input.configparam;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.drm.JobRunner;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpConfigLoader;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;
import org.genepattern.server.dm.jobinput.ParameterInfoUtil;
import org.genepattern.server.job.input.InputParamGroup;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskExecutor;

import com.google.common.collect.ImmutableList;

/**
 * Helper class for including additional job configuration parameters in the job input form.
 * @author pcarr
 *
 */
public class JobConfigParams {
    private static Logger log = Logger.getLogger(JobConfigParams.class);

    public static final String PROP_ENABLE_EXECUTOR_INPUT_PARAMS="enableExecutorInputParams";
    
    /**
     * At runtime, when loading the job input form for a given module for a given user
     * (both specified in the taskContext), initialize a JobConfigParams object
     * which can be used to get additional input parameters to add to the job input form
     * as well as the grouping information for those parameters.
     * 
     * @param taskContext
     * @return
     */
    public static JobConfigParams initJobConfigParams(final GpConfig gpConfig, final GpContext taskContext) {
        if (gpConfig==null) {
            log.debug("gpConfig==null");
            return null;
        }
        final String jobConfigParamsStr=gpConfig.getGPProperty(taskContext, JobRunner.PROP_EXECUTOR_INPUT_PARAMS);
        if (jobConfigParamsStr != null) {
            //figure out how to cache the results of parsing the config file
            try {
                final File jobConfigParamsFile = GpConfigLoader.initFileFromStr(
                        gpConfig.getResourcesDir(), 
                        jobConfigParamsStr);
                JobConfigParams jobConfigParams=JobConfigParamsParserYaml.parse(jobConfigParamsFile);
                replaceDefaults(gpConfig, taskContext, jobConfigParams);
                return jobConfigParams;
            }
            catch (Throwable t) {
                log.error("Error initializing job config params, jobConfigParams="+jobConfigParamsStr, t);
            }
            return null;
        }
        return null;
    }
    
    private static void replaceDefaults(final GpConfig gpConfig, final GpContext taskContext, final JobConfigParams jobConfigParams) {
        for(final ParameterInfo pinfo : jobConfigParams.getParams()) {
            final String pname=pinfo.getName();
            final Value customValue=gpConfig.getValue(taskContext, pname);
            if (customValue != null) {
                setDefaultValue(pinfo, customValue.getValue());
            }
        }
    }

    /**
     * 
     * @param pinfo
     * @param defaultValue
     */
    @SuppressWarnings("unchecked")
    private static void setDefaultValue(final ParameterInfo pinfo, final String defaultValue) {
        pinfo.getAttributes().put(TaskExecutor.PARAM_INFO_DEFAULT_VALUE[0], defaultValue);
    }
    
    private final InputParamGroup jobConfigGroup;
    private final List<ParameterInfo> params;
    private final Map<String,ParameterInfo> paramInfoMap;
    
    private JobConfigParams(final Builder in) {
        InputParamGroup.Builder paramGroupBuilder=new InputParamGroup.Builder(in.paramGroupName)
            .description(in.paramGroupDescription)
            .hidden(in.paramGroupHidden);
        for(final ParameterInfo pInfo : in.params.values()) {
            paramGroupBuilder.addParameter(pInfo.getName());
        }
        this.jobConfigGroup=paramGroupBuilder.build();
        this.params=ImmutableList.copyOf(in.params.values());
        this.paramInfoMap=new HashMap<String,ParameterInfo>();
        for(final ParameterInfo param : params) {
            paramInfoMap.put(param.getName(), param);
        }
    }
    
    public InputParamGroup getInputParamGroup() {
        return jobConfigGroup;
    }
    
    public List<ParameterInfo> getParams() {
        return params;
    }
    
    public ParameterInfo getParam(final String pname) {
        return paramInfoMap.get(pname);
    }
    
    public static class Builder { 
        private String paramGroupName="";
        private String paramGroupDescription="";
        private boolean paramGroupHidden=true;
        private final Map<String,ParameterInfo> params=new LinkedHashMap<String,ParameterInfo>();
        public Builder() {
        }
        
        /**
         * Set the name of this grouping of input parameters.
         * @param paramGroupName
         * @return
         */
        public Builder name(final String paramGroupName) {
            this.paramGroupName=paramGroupName;
            return this;
        }

        /**
         * Set an optional description of this grouping pof input parameters.
         * @param description
         * @return
         */
        public Builder description(final String description) {
            this.paramGroupDescription=description;
            return this;
        }
        
        public Builder hidden(final boolean hidden) {
            this.paramGroupHidden=hidden;
            return this;
        }
        
        public Builder addParameter(final String pname) {
            final ParameterInfo pInfo=ParameterInfoUtil.initTextParam(pname, "", "", true);
            addParameter(pInfo);
            return this;
        }
        
        public Builder addParameter(final ParameterInfo pInfo) {
            //TODO: check for duplicate param name in the actual module
            params.put(pInfo.getName(), pInfo);
            return this;
        }
        
        public JobConfigParams build() {
            return new JobConfigParams(this);
        }
    }

}
