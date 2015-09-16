/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.rest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
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
    private static final Logger log = Logger.getLogger(ParameterInfoRecord.class);

    /**
     * Initialize a map of paramName to ParameterInfo 
     * @param taskInfo
     * @return
     */
    public static final Map<String,ParameterInfoRecord> initParamInfoMap(final TaskInfo taskInfo) {
        if (taskInfo==null) {
            log.error("taskInfo==null");
            return Collections.emptyMap();
        }
        return initParamInfoMap(taskInfo.getParameterInfoArray());
    }
    
    public static final Map<String,ParameterInfoRecord> initParamInfoMap(final ParameterInfo[] formalParams) {
        final Map<String,ParameterInfoRecord> paramInfoMap=new LinkedHashMap<String,ParameterInfoRecord>();
        for(final ParameterInfo formalParam : formalParams) {
            final ParameterInfoRecord record=new ParameterInfoRecord(formalParam);
            paramInfoMap.put(formalParam.getName(), record);            
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

