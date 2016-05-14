/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.job;

import java.util.ArrayList;
import java.util.List;
import org.genepattern.server.job.input.JobInput;

public class JobInputValues {
    /**
     * By default, remove empty ("") values.
     */
    public static final JobInput parseJobInput(JobInputValues jobInputValues) { 
        return parseJobInput(jobInputValues, true);
    }

    /**
     * FIXME: Hack to handle empty strings with optional file params
     * 
     * Create a new JobInput instance as a copy of the JobInputValues.
     * 
     * Remove empty values before adding the job, e.g.
     *     { "name": "input.file", "values": [ "" ] }
     * 
     * @param jobInputValues
     * @param removeEmptyValues, treat empty ("") values as 'not set'
     * @return
     */
    public static final JobInput parseJobInput(final JobInputValues jobInputValues, final boolean removeEmptyValues) {
        final JobInput jobInput=new JobInput();
        jobInput.setLsid(jobInputValues.lsid);
        for(final Param param : jobInputValues.params) {
            for(final String value : param.values) { 
                if (!removeEmptyValues || !"".equals(value)) {
                    jobInput.addValue(param.name, value, param.groupId, param.batchParam);
                }
            }
        }
        return jobInput;
    }
    
    public static class Param {
        public String name;
        public List<String> values;
        public boolean batchParam=false;
        public String groupId;
    }

    public String lsid;
    public List<Param> params;
    public List<String> tags = new ArrayList<String>();
    
    public JobInputValues() {
    }

    public List<String> getTags() {
        return tags;
    }
}
