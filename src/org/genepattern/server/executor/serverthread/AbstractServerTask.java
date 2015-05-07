/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.serverthread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

public abstract class AbstractServerTask implements ServerTask {
    //serverTask properties
    protected String userId;
    protected int jobId = -1;
    protected String[] args = null;

    final public void setUserId(String userId) {
        this.userId = userId;
    }

    final public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    final public void setArgs(String[] args) {
        this.args = args;
    }
    

    /**
     * Split command line args into groups of parameter names and parameter values.
     * 
     * This is to support idiosyncratic usage where a control-flow task wants to use
     * the same input values for more than one input parameter.
     * 
     * @param arg
     * @param delim
     * @return
     * @throws Exception
     */
    static public SplitArg splitSplit(String arg, char delim) throws Exception {
        String[] entry = splitArg(arg, delim);
        
        String paramNameStr = entry[0];
        String valuesStr = entry[1];
        
        String paramNameArray[] = paramNameStr.split("(?<!\\\\),");
        String valuesArray[] = valuesStr.split("(?<!\\\\),");
       
        SplitArg splitArg = new SplitArg();
        for(String paramName : paramNameArray) {
            splitArg.addName(paramName);
        }
        for(String value : valuesArray) {
            splitArg.addValue(value);
        }
        return splitArg;
    }
     
    /**
     * Split an entry into two tokens, pname and pvalue, using the given delimiter.
     *     <pname><delimiter><pvalue>; the delimiter char can be escaped with the '\' character. 
     * 
     * Examples:
     *     name=value --> [name, value]
     *     name\=b=value--> [name=b, value]
     * 
     * @param arg
     * @return
     * @throws Exception
     */
    static public String[] splitArg(String arg, char delim) throws Exception {
        String array[] = arg.split("(?<!\\\\)"+delim);
        
        if (array == null) {
            throw new Exception("split returned null");
        }
        if (array.length != 2) {
            throw new Exception("split returned "+array.length+" values");
        }
        return array;
    }

    static public class SplitArg {
        final private List<String> names = new ArrayList<String>();
        final private List<String> values = new ArrayList<String>();
        
        public void addName(String name) {
            names.add(name);
        }
        public void addValue(String value) {
            values.add(value);
        }
        
        public List<String> getNames() {
            return Collections.unmodifiableList(names);
        }
        public List<String> getValues() {
            return Collections.unmodifiableList(values);
        }
    }
    
    static public class BatchParams {
        private static Logger log = Logger.getLogger(BatchParams.class);

        private int numJobsToSubmit = 0;
        private Map<String,List<String>> map = new HashMap<String,List<String>>();
        private Set<String> loopParams = new HashSet<String>();
        
        /**
         * Add an entry of the form:
         *     <name0>,<name1>,...,<nameM-1>=<val0>,<val1>,...,<valN-1>
         * Examples:
         *     //add three entries for the 'db' parameter
         *     db=BWA1,BWA1,BWA2
         *     //add one entry for the 'e' parameter
         *     e=0.1
         *     //add three entries for the 'db2' parameter, using the same value for two different parameter names
         *     param1,param2=BWA1,BWA2,BWA1
         * 
         * @param paramName
         * @param valuesStr
         */
        public void addEntries(String paramNameStr, String valuesStr) {
            String paramNameArray[] = paramNameStr.split("(?<!\\\\),");
            String valuesArray[] = valuesStr.split("(?<!\\\\),");
            List<String> paramNameList = new ArrayList<String>();
            for(String paramName : paramNameArray) {
                paramNameList.add(paramName);
            }
            List<String> valuesList = new ArrayList<String>();
            for(String value : valuesArray) {
                valuesList.add(value);
            }
            
            for(String paramName : paramNameList) {
                //at the moment, ignore empty lists
                for(String value : valuesList) {
                    addEntry(paramName, value);
                }
            } 
        }
        
        public void addEntry(String paramName, String paramValue) {
            List<String> values = map.get(paramName);
            if (values == null) {
                values = new ArrayList<String>();
                map.put(paramName, values);
            }
            values.add(paramValue);
        }
        
        public void setLoopParam(String paramName) {
            this.loopParams.add(paramName);
        }
        
        public void validate() throws Exception {
            //require that each entry has either one value, or the same number of values as the rest of the batch jobs
            //e.g.
            // param_A: valA1,valA2,valA3
            // param_B: valB1
            // param_C: valC1,valC2,valC3
            // param_D: valD1
            // this set means, there are 3 jobs, but param_B and param_D use the same value for each run
            int numParamValues = 1;
            for(List<String> valueList : map.values()) {
                if (valueList.size() == 1) {
                    //ignore
                }
                else {
                    if (numParamValues == 1) {
                        //initialize
                        numParamValues = valueList.size();
                    }
                    else {
                        //validate
                        if (numParamValues != valueList.size()) {
                            throw new Exception("each parameter list must contain the same number of comma-delimited values");
                        }
                    }
                }
            }
            numJobsToSubmit = numParamValues;
        }
        
        public int getNumJobsToSubmit() {
            return numJobsToSubmit;
        }

        public String get(final String paramName, int idx) throws IndexOutOfBoundsException {
            boolean matchLast = false;
            return get(paramName, idx, matchLast);
        }

        public String get(final String paramName, int idx, boolean matchLast) throws IndexOutOfBoundsException {    
            List<String> values = map.get(paramName);
            if (values == null && matchLast) {
                for(String key : map.keySet()) {
                    if (paramName.endsWith(key)) {
                        values = map.get(key);
                        break;
                    }
                }
            }
            if (values == null) {
                // no matching list for paramName
                return null;
            }
            if (values.size() == 1) {
                //only 1 item in list, use it
                return values.get(0);
            }
            return values.get(idx);
        }
    }
    
    /**
     * Flag indicating the parent job to use when creating the newJob.
     * What is the parent of the newJob being added to the queue?
     * The 'thisJob' is the thisJob and 'newJob' is the job being added.
     * 
     * 1: add_to_parent, newJob.parent = thisJob.parent
     * 2: add_to_job, newJob.parent = thisJob.jobId
     * 3: add_to_root, newJob.parent = thisJob.root
     * 4: add_to_queue, newJob.parent = null (means add directly to the queue).
     */
    public enum AddToFlag {
        add_to_parent, //add the job to the parent of this job (aka as next sibling), newJob.parent = jobId.parent
        add_to_child, //add the job as the child of this job newJob.parent = jobId
        add_to_root, //add the job as a child of the root of this job
        add_to_queue //add directly to the queue, parent==null
    }
}
