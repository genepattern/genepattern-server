package org.genepattern.server.job.input.choice;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.server.webapp.rest.api.v1.task.TasksResource;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author pcarr
 *
 */
public class ChoiceInfoHelper {
    final static private Logger log = Logger.getLogger(ChoiceInfoHelper.class);
    
    final static public boolean isSet(final String in) {
        if (in == null || in.length()==0) {
            return false;
        }
        return true;
    }

    final static public ChoiceInfo initChoiceInfo(final ParameterInfo pinfo) {
        try {
            return ChoiceInfo.getChoiceInfoParser().initChoiceInfo(pinfo);
        }
        catch (Throwable t) {
            log.error(t);
            return null;
        }
    }
    
    /**
     * Get the JSON representation for the given choiceInfo.
     * 
     * GET /v1/tasks/{lsid}/params/{paramName}
     * <pre>
     * 200 OK
     * {
     *   ...
     *   choice: {
     *     href: "/v1/tasks/{lsid}/params/{paramName}/choices"
     *   }
     * }
     * </pre>
     * 
     * GET /v1/tasks/{lsid}/params/{paramName}/choices
     * <pre>
     * 200 OK
     * {
     * href: "/v1/tasks/{lsid}/params/{paramName}/choiceInfo",
     * ftpDir: "ftp://ftp.broadinstitute.org/pub/genepattern/rna_seq/referenceAnnotation/gtf",
     * status: {
     *     flag: "OK" | "WARNING" | "ERROR"
     *     message: ""
     * },
     * choices: [
     *   {
     *     label: "Arabidopsis_thaliana_Ensembl_TAIR10.gtf",
     *     value: "ftp://ftp.broadinstitute.org/pub/genepattern/rna_seq/referenceAnnotation/gtf/Arabidopsis_thaliana_Ensembl_TAIR10.gtf"
     *   },
     *   { label: "", value: "" },
     *   ...
     *   { label: "", value: "" }
     * ]
     * }
     * </pre>
     * 
     * Example status messages,
     *     OK, Initialized from values param (old way)
     *     OK, Initialized from choices param (new way, not dynamic)
     *     OK, Initialized from remote server (url=, date=)
     *     WARN, Initialized from cache, problem connecting to remote server
     *     ERROR, Error in module manifest, didn't initialize choices.
     *     ERROR, Connection error to remote server (url)
     *     ERROR, Timeout waiting for listing from remote server (url, timeout)
     * 
     * @param pinfo
     * @return
     */
    final static public JSONObject initChoiceInfoJson(final HttpServletRequest request, final TaskInfo taskInfo, final ChoiceInfo choiceInfo) throws JSONException {
        if (choiceInfo==null) {
            throw new IllegalArgumentException("choiceInfo==null");
        }
        final JSONObject json=new JSONObject();
        if (request != null && taskInfo != null && isSet(choiceInfo.getParamName())) {
            final String href=TasksResource.getChoiceInfoPath(request, taskInfo, choiceInfo.getParamName());
            json.put("href", href);
        }
        if (isSet(choiceInfo.getFtpDir())) {
            json.put("ftpDir", choiceInfo.getFtpDir());
        }
        if (choiceInfo.getStatus() != null) {
            final JSONObject statusObj=new JSONObject();
            statusObj.put("flag", choiceInfo.getStatus().getFlag().name());
            statusObj.put("message", choiceInfo.getStatus().getMessage());
            json.put("status", statusObj);
        }
        final JSONArray choices=initChoiceJson(choiceInfo);
        json.put("choices", choices);
        return json;
    }
    
    final static public JSONArray initChoiceJson(final ChoiceInfo choiceInfo) {
        if (choiceInfo==null) {
            log.error("Unexpected null value");
            return null;
        }
        //create a json array of the choices
        try {
            JSONArray choices=new JSONArray();
            for(final Choice choice : choiceInfo.getChoices()) {
                JSONObject choiceObj=new JSONObject();
                choiceObj.put("label", choice.getLabel());
                choiceObj.put("value", choice.getValue());
                choices.put(choiceObj);
            }
            return choices;
        }
        catch (Throwable t) {
            log.error(t);
            return null;
        }
    }
}
