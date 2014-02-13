package org.genepattern.server.job.input;

import java.util.ArrayList;
import java.util.List;

import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.jobinput.ParameterInfoUtil;
import org.genepattern.webservice.ParameterInfo;

import com.google.common.collect.ImmutableList;

/**
 * Helper class for including additional job configuration parameters in the job input form.
 * @author pcarr
 *
 */
public class JobConfigParams {
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
    public static JobConfigParams initJobConfigParams(final Context taskContext) {
        //this implementation returns hard-coded values for all users and modules
        //TODO: use server config to determine the return value
        JobConfigParams jobConfig=new Builder()
            .addParameter("drm.queue")
            .addParameter("drm.walltime")
            .addParameter("pbs.host")
            .addParameter("pbs.mem")
            .addParameter("pbs.cput")
            .addParameter("pbs.vmem")
        .build();
        return jobConfig;
    }
    
    private final InputParamGroup jobConfigGroup;
    private final List<ParameterInfo> params;
    
    private JobConfigParams(final Builder in) {
        this.jobConfigGroup=in.builder.build();
        this.params=ImmutableList.copyOf(in.params);
    }
    
    public InputParamGroup getInputParamGroup() {
        return jobConfigGroup;
    }
    
    public List<ParameterInfo> getParams() {
        return params;
    }
    
    public static class Builder { 
        private final InputParamGroup.Builder builder;
        private final List<ParameterInfo> params=new ArrayList<ParameterInfo>();
        public Builder() {
            builder=new InputParamGroup.Builder("Advanced/Job Configuration")
                .description("Customize the job configuration parameters for this job")
                .hidden(true);
        }
        
        public Builder addParameter(final String pname) {
            builder.addParameter(pname);
            params.add(ParameterInfoUtil.initTextParam(pname, "", "", true));
            
            //TODO: check for duplicate param name in the actual module
            return this;
        }
        
        public JobConfigParams build() {
            return new JobConfigParams(this);
        }
    }

}
