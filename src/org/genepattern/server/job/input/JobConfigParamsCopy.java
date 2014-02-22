package org.genepattern.server.job.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ConfigurationException;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.config.Value;
import org.genepattern.server.job.input.choice.ChoiceInfo;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.ParameterInfo;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

/**
 * Helper class for including additional job configuration parameters in the job input form.
 * @author pcarr
 *
 */
public class JobConfigParamsCopy {
    final static private Logger log = Logger.getLogger(JobConfigParamsCopy.class);

    public static final String PROP_ENABLE_JOB_CONFIG_PARAMS="enableJobConfigParams";
    
    /*
     * Example json format
[
{
  name: "Basic/Required", 
  description: "This contains the required parameters", 
  hidden: false, 
  parameters: [
    "basic.required.parameter.1", 
    "basic.required.parameter.2"]
}, 
{
  name: "Advanced", 
  description: "This contains the advanced parameters", 
  hidden: true, 
  parameters: [
    "advanced.parameter.1", 
    "advanced.parameter.2"]
}, 
{
  name: "Basic", 
  parameters: [
    "basic.parameter.1", 
    "basic.parameter.2"]
}
]
     */
    
    /**
     * At runtime, when loading the job input form for a given module for a given user
     * (both specified in the taskContext), initialize a JobConfigParams object
     * which can be used to get additional input parameters to add to the job input form
     * as well as the grouping information for those parameters.
     * 
     * @param taskContext
     * @return
     */
    public static JobConfigParamsCopy initJobConfigParams(final GpContext taskContext) {
        Value jobConfigYaml=ServerConfigurationFactory.instance().getValue(taskContext, "jobConfigParams");
        
        //this implementation returns hard-coded values for all users and modules
        //TODO: use server config to determine the return value
        final boolean optional=true;
        final String defaultValue="";
        JobConfigParamsCopy jobConfig=new Builder()
            .addChoiceParameter("drm.queue", "queue", "", optional, defaultValue, new String[] {"hour", "week", "bhour", "bweek", "priority", "interactive", "preview"})
            .addTextParameter("drm.walltime", "walltime", "The max wall clock time limit for the job, format in days-hh:mm:ss", optional, "02:00:00")
            .addTextParameter("pbs.host", "host", "The host name for pbs jobs", optional, defaultValue)
            .addTextParameter("pbs.mem", "mem", "The memory limit for pbs jobs, for example '8gb'", optional, "2gb")
            .addParameter("pbs.cput")
            .addParameter("pbs.vmem")
        .build();
        return jobConfig;
    }
    
    private static JobConfigParamsCopy initFromYaml(final Value value) throws ConfigurationException {
        if (value==null) {
            log.debug("value is null");
            return null;
        }
        if (!value.isFromCollection()) {
            log.debug("value is not a collection");
            return null;
        }
        
        Builder builder=new Builder();
        for(final String entry : value.getValues()) {
            final Value entryValue=Value.parse(entry);
            final ParameterInfo pinfo=ParamEntryParser.fromMapEntry(entryValue);
            if (pinfo!=null) {
                builder.addParameter(pinfo);
            }            
        }
        return builder.build();
    }
    
    
    /**
     * This implementation returns hard-coded values for all users and modules.
     * @return
     */
    private static JobConfigParamsCopy initDefaultJobConfigParams() {
        //this implementation returns hard-coded values for all users and modules
        final boolean optional=true;
        final String defaultValue="";
        JobConfigParamsCopy jobConfig=new Builder()
            .addChoiceParameter("drm.queue", "queue", "", optional, defaultValue, new String[] {"hour", "week", "bhour", "bweek", "priority", "interactive", "preview"})
            .addTextParameter("drm.walltime", "walltime", "The max wall clock time limit for the job, format in days-hh:mm:ss", optional, "02:00:00")
            .addTextParameter("pbs.host", "host", "The host name for pbs jobs", optional, defaultValue)
            .addTextParameter("pbs.mem", "mem", "The memory limit for pbs jobs, for example '8gb'", optional, "2gb")
            .addParameter("pbs.cput")
            .addParameter("pbs.vmem")
        .build();
        return jobConfig;

    }

    private final InputParamGroup jobConfigGroup;
    private final List<ParameterInfo> params;
    
    private JobConfigParamsCopy(final Builder in) {
        this.jobConfigGroup=in.paramGroupBuilder.build();
        this.params=ImmutableList.copyOf(in.params);
    }
    
    public InputParamGroup getInputParamGroup() {
        return jobConfigGroup;
    }
    
    public List<ParameterInfo> getParams() {
        return params;
    }
    
    public static class Builder { 
        private final InputParamGroup.Builder paramGroupBuilder;
        private final List<ParameterInfo> params=new ArrayList<ParameterInfo>();
        public Builder() {
            paramGroupBuilder=new InputParamGroup.Builder("Advanced/Job Configuration")
                .description("Customize the job configuration parameters for this job")
                .hidden(true);
        }
        
        public Builder addParameter(final ParameterInfo pInfo) {
            if (pInfo==null) {
                throw new IllegalArgumentException("pInfo==null");
            }
            paramGroupBuilder.addParameter(pInfo.getName());
            params.add(pInfo);
            return this;
        }
        
        public Builder addParameter(final String pname) {
            return addTextParameter(pname, null, "", true, "");
        }
        public Builder addTextParameter(final String pname, final String altName, final String description, boolean optional, String defaultValue) {
            final ParamBuilder pbuilder=new ParamBuilder(pname)
                .altName(altName)
                .description(description)
                .optional(optional)
                .defaultValue(defaultValue);
            return addParameter(pbuilder.build());
        }
        
        public Builder addChoiceParameter(final String pname, final String altName, final String description, boolean optional, String defaultValue, final String[] choices) {
            if (choices==null || choices.length==0) {
                throw new IllegalArgumentException("choices array must be non-null and non-empty");
            }
            final ParamBuilder pbuilder=new ParamBuilder(pname)
                .altName(altName)
                .description(description)
                .optional(optional)
                .defaultValue(defaultValue);
            for(final String choice : choices) {
                pbuilder.addChoice(choice);
            }
            addParameter(pbuilder.build());
            return this;
        }
        
        public JobConfigParamsCopy build() {
            return new JobConfigParamsCopy(this);
        }
    }

    private static class ParamEntryParser {
        public static ParameterInfo fromMapEntry(final Value mapEntry) {
            if (mapEntry==null) {
                return null;
            }
            if (!mapEntry.isMap()) {
                return null;
            }
            return fromMap( (Map<String,?>) mapEntry.getMap());
        }
        
        public static ParameterInfo fromMap(final Map<String,?> map) {
            ParamBuilder builder=new ParamBuilder((String)map.get("name"));
            return builder.build();
        }
    }
    
    public static class ParamBuilder {
        private final String name;
        private final String value="";
        private String altName=null;
        private String description="";
        private String defaultValue="";
        private boolean optional=true;
        private List<String> choices=null;
        
        public ParamBuilder(final String name) {
            this.name=name;
        }
        public ParamBuilder altName(final String altName) {
            this.altName=altName;
            return this;
        }
        public ParamBuilder description(final String description) {
            this.description=description;
            return this;
        }
        public ParamBuilder optional(final boolean optional) {
            this.optional=optional;
            return this;
        }
        public ParamBuilder defaultValue(final String defaultValue) {
            this.defaultValue=defaultValue;
            return this;
        }
        public ParamBuilder addChoice(final String value) {
            if (choices==null) {
                choices=new ArrayList<String>();
            }
            choices.add(value);
            return this;
        }
        
        @SuppressWarnings("unchecked")
        public ParameterInfo build() {
            final ParameterInfo pinfo=new ParameterInfo(name, value, description);
            pinfo.setAttributes(new HashMap<String,String>());
            if (altName!=null) {
                pinfo.getAttributes().put(GPConstants.ALTNAME, altName);
            }
            pinfo.getAttributes().put("MODE", "");
            pinfo.getAttributes().put("TYPE", "TEXT");
            pinfo.getAttributes().put("default_value", defaultValue);
            if (optional) {
                pinfo.getAttributes().put("optional", "on");
            }
            else {
                pinfo.getAttributes().put("optional", "");
            }
            pinfo.getAttributes().put("type", "java.lang.String");
            if (choices!=null) {
                Joiner joiner=Joiner.on(";").useForNull("");
                String choicesStr=joiner.join(choices);
                pinfo.getAttributes().put(ChoiceInfo.PROP_CHOICE, choicesStr);
            }
            return pinfo;
        }
    }

}
