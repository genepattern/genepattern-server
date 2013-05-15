package org.genepattern.server.job.input;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.eula.GetTaskStrategy;
import org.genepattern.server.eula.GetTaskStrategyDefault;
import org.genepattern.server.job.JobInfoLoader;
import org.genepattern.server.job.JobInfoLoaderDefault;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * Helper class for reloading a job.
 * @author pcarr
 *
 */
public class ReloadJobHelper {
    final static private Logger log = Logger.getLogger(ReloadJobHelper.class);

    /**
     * Replace '<GenePatternURL>' substitution variable with the actual value.
     * 
     * @param in
     * @return
     */
    public static String replaceGpUrl(final String in) {
        if (!in.startsWith("<GenePatternURL>")) {
            return in;
        }
        URL gpURL=ServerConfiguration.instance().getGenePatternURL();
        String prefix=gpURL.toExternalForm();
        String suffix=in.substring("<GenePatternURL>".length());
        if (prefix.endsWith("/") && suffix.startsWith("/")) {
            return prefix + suffix.substring(1);
        }
        if (!prefix.endsWith("/") && !suffix.startsWith("/")) {
            return prefix + "/" + suffix;
        }
        return prefix +suffix;
    }


    final Context userContext;
    final GetTaskStrategy getTaskStrategy;
    final JobInfoLoader jobInfoLoader;
    
    public ReloadJobHelper(final Context userContext) {
        this(userContext, null);
    }
    public ReloadJobHelper(final Context userContext, final GetTaskStrategy getTaskStrategyIn) {
        this(userContext, null, null);
    }
    public ReloadJobHelper(final Context userContext, final GetTaskStrategy getTaskStrategyIn, final JobInfoLoader jobInfoLoaderIn) {
        if (userContext==null) {
            throw new IllegalArgumentException("userContext==null");
        }
        this.userContext=userContext;
        if (getTaskStrategyIn != null) {
            this.getTaskStrategy=getTaskStrategyIn;
        }
        else {
            this.getTaskStrategy=new GetTaskStrategyDefault();
        }
        if (jobInfoLoaderIn != null) {
            this.jobInfoLoader=jobInfoLoaderIn;
        }
        else {
            this.jobInfoLoader=new JobInfoLoaderDefault();
        }
    }

    /**
     * Get the original input values for a 'reloaded' job. This is a utility method 
     * for getting the input values from a (presumably completed) job.
     * Use this to initialize the jQuery job input form for a reloaded job.
     * 
     * @param userContext
     * @param jobId
     * @return
     */
    public JobInput getInputValues(final String reloadJobId) throws Exception {
        //final JobInfo jobInfo = initJobInfo(userContext, reloadJobId);
        final JobInfo jobInfo = jobInfoLoader.getJobInfo(userContext, reloadJobId);
        return getInputValues(jobInfo);
    }
    
    /**
     * Get the original input values for a 'reloaded' job.
     * @see #getInputValues(String)
     * 
     * @param reloadJob, must be non-null.
     * @return
     */
    private JobInput getInputValues(final JobInfo reloadJob) {
        if (reloadJob==null) {
            log.error("reloadJob==null");
            throw new IllegalArgumentException("reloadJob==null");
        }
        
        final JobInput jobInput=new JobInput();
        jobInput.setLsid(reloadJob.getTaskLSID());
        
        final ParameterInfo[] params = reloadJob.getParameterInfoArray();
        if (params==null) {
            log.error("reloadJob.parameterInfoArray == null");
            return jobInput;
        }
        if (params.length==0) {
            return jobInput;
        }

        //initialize a map of paramName to ParameterInfo 
        final TaskInfo taskInfo=getTaskStrategy.getTaskInfo(reloadJob.getTaskLSID());
        final Map<String,ParameterInfoRecord> paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskInfo);

        for (final ParameterInfo actualParam : params) {
            final String pname=actualParam.getName();
            ParameterInfo formalParam=null;
            if (paramInfoMap.containsKey(pname)) {
                formalParam=paramInfoMap.get(pname).getFormal();
            }
            //if formalParam is null, it's probably an output file, as opposed to an initial input param
            final List<String> values=getOriginalInputValues(formalParam, actualParam);
            if (values != null) {
                for(final String value : values) {
                    jobInput.addValue(pname, value);
                }
            }
        }
        return jobInput;
    }
    
    /**
     * Get the list of previous input values for the given parameter, this is to be called 
     * when reloading a job.
     * 
     * Has special-handling for parameter lists, when the original run has a list of values for the parameter.
     * Has special-handling for '<GenePatternURL>', when the formalParam is a File or Directory, always replace the
     *     '<GenePatternURL>' substitution variable with the actual GenePatternURL.
     * 
     * @param formalParam, the formal parameter as initialized from the TaskInfo (derived from the manifest for the module).
     *     Can be null.
     * @param paramValue, the actual parameter value as initialized from the JobInfo (the original run of the job).
     *     Must not be null
     * 
     * @return the list of zero or more values, if any, that were set for the original job.
     */
    private List<String> getOriginalInputValues(final ParameterInfo formalParam, final ParameterInfo paramValue) {
        if (paramValue==null) {
            throw new IllegalArgumentException("paramValue == null");
        }
        HashMap<?,?> attrs=paramValue.getAttributes();
        if (attrs==null) {
            log.error("paramValue.attributes==null");
            return Collections.emptyList();
        }
        
        if (paramValue.isOutputFile()) {
            //ignore output files
            log.debug("pinfo.isOutputFile == true");
            return null;
        }
        
        //extract all 'values_' 
        SortedMap<Integer,String> valuesMap=new TreeMap<Integer,String>();
        for(Entry<?,?> entry : attrs.entrySet()) {
            String key=entry.getKey().toString();
            if (key.startsWith("values_")) {
                try {
                    int idx=Integer.parseInt( key.split("_")[1] );
                    String value=entry.getValue().toString();
                    value=replaceGpUrl(value);
                    valuesMap.put(idx, value);
                }
                catch (Throwable t) {
                    log.error("Can't parse pinfo.attribute, key="+key, t);
                }
            }
        }
        if (valuesMap.size() > 0) {
            List<String> values=new ArrayList<String>(valuesMap.values());
            return values;
        }
        
        List<String> values=new ArrayList<String>();
        //if necessary, replace <GenePatternURL> with actual value
        String value=paramValue.getValue();
        //special-case for input files and directories, if necessary replace actual URL with '<GenePatternURL>'
        if (formalParam != null) {
            if (formalParam.isInputFile() || formalParam._isDirectory()) {
                value=replaceGpUrl(paramValue.getValue());
            }
        }
        values.add(value);
        return values;
    }

}
