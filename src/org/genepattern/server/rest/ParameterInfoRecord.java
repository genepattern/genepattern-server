package org.genepattern.server.rest;

import java.util.HashMap;
import java.util.Map;

import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * Helper class for working with ParameterInfo records,
 * because the runtime instance of a parameter info can differ from the 
 * initial version for the TaskInfo, use this to hold onto both the initial aka formal state
 * as well as the actual value to use at runtime.
 * 
 * @author pcarr
 *
 */
public class ParameterInfoRecord {
    /**
     * Initialize a map of paramName to ParameterInfo 
     * @param taskInfo
     * @return
     */
    final static public Map<String,ParameterInfoRecord> initParamInfoMap(final TaskInfo taskInfo) {
        Map<String,ParameterInfoRecord> paramInfoMap=new HashMap<String,ParameterInfoRecord>();
        for(ParameterInfo pinfo : taskInfo.getParameterInfoArray()) {
            ParameterInfoRecord record = new ParameterInfoRecord(pinfo);
            paramInfoMap.put(pinfo.getName(), record);
        }
        return paramInfoMap;
    }

    private ParameterInfo formalParam;
    private ParameterInfo actualParam;

    public ParameterInfoRecord(ParameterInfo formalParam) {
        this.formalParam=formalParam;
        this.actualParam=ParameterInfo._deepCopy(formalParam);
    }

    public ParameterInfo getFormal() {
        return formalParam;
    }
    public ParameterInfo getActual() {
        return actualParam;
    }
}

