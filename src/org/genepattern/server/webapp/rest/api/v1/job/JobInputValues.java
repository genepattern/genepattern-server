/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.job;

import java.util.ArrayList;
import java.util.List;
import org.genepattern.server.job.input.JobInput;

public class JobInputValues {
    public static interface ValueFilter {
        /**
         * @return true if the filter accepts the value
         */
        boolean accept(final Param param, final String value);
    }
    
    /**
     * Helper for handling empty strings
     * @author pcarr
     */
    public static enum Filter implements ValueFilter {
        /**
         * Accept all non-empty values (including null). 
         * By default, remove empty ("") values, e.g.
         *      { "name": "input.file", "values": [ "" ] }
         */
        NON_EMPTY {
            @Override
            public boolean accept(final Param param, final String value) {
                // Note: null is accepted
                return !"".equals(value);
            }
        },
        /** accept all values, including null and empty string */
        ALL {
            @Override
            public boolean accept(final Param param, final String value) {
                return true;
            }
        }
        ;
    }

    /** 
     * FIXME: Hack to handle empty strings with optional file params
     * by default remove empty ("") values. 
     */
    public static final JobInput parseJobInput(JobInputValues jobInputValues) { 
        return parseJobInput(jobInputValues, Filter.NON_EMPTY);
    }

    /**
     * Create a new JobInput instance as a copy of the JobInputValues, 
     * by copying all values which are accepted by the filter.
     * 
     * @param jobInputValues
     * @param filter
     */
    public static final JobInput parseJobInput(final JobInputValues jobInputValues, final ValueFilter filter) {
        final JobInput jobInput=new JobInput();
        jobInput.setLsid(jobInputValues.lsid);
        for(final Param param : jobInputValues.params) {
            for(final String value : param.values) { 
                if (filter.accept(param, value)) {
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
