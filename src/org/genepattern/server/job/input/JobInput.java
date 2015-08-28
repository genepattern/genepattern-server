/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.genepattern.server.rest.GpServerException;

/**
 * Representation of user-supplied input parameters for a new job to be added to the GP server.
 * 
 * 
 * Example:
 *     lsid: urn:lsid:broad.mit.edu:cancer.software.genepattern.module.test.analysis:00006:0.1
 *     inputFiles: [
 *         <GenePatternURL>users/admin/tutorial/all_aml_test.gct,
 *         <GenePatternURL>users/admin/tutorial/all_aml_train.gct,
 *     ]
 * @author pcarr
 *
 */
public class JobInput {
    //private static final Logger log = Logger.getLogger(JobInput.class);
    
    private String lsid;
    private Map<ParamId, Param> params=new LinkedHashMap<ParamId, Param>();

    public JobInput() {
    }
    /**
     * Copy constructor.
     */
    public JobInput(final JobInput in) {
        this.lsid=in.lsid;
        
        //clone the map
        for(Entry<ParamId, Param> entry : in.params.entrySet()) {
            ParamId paramId=new ParamId(entry.getKey());
            Param param=new Param(entry.getValue());
            params.put(paramId, param);
        }
    }
    
    /**
     * The lsid for a module installed on the GP server.
     */
    public void setLsid(final String lsid) {
        this.lsid=lsid;
    }
    public String getLsid() {
        return this.lsid;
    }

    /**
     * The list of user-supplied parameter values.
     */
    public Map<ParamId, Param> getParams() {
        return Collections.unmodifiableMap(params);
    }

    /**
     * Add a value for the param.
     * @param name, the parameter name, which is a unique id. Cannot be null.
     * @param value, the user provided input value, cannot be null.
     */
    public void addValue(final String name, final String value) {
        addValue(new ParamId(name), value, GroupId.EMPTY);
    }
    
    public void addValue(final ParamId paramId, final ParamValue value) {
        addValue(paramId, value, GroupId.EMPTY);
    }
    
    public void addValue(final ParamId paramId, final ParamValue value, final GroupId groupId) {
        addValue(paramId, value, groupId, false);
    } 
    
    public void addValue(final String name, final String value, final GroupId groupId) {
        addValue(new ParamId(name), value, groupId);
    }
    
    public void addValue(final ParamId paramId, final String value, final GroupId groupId) {
        addValue(paramId, value, groupId, false);
    }

    public void addValue(final ParamId paramId, final String value, final boolean batchParam) {
        addValue(paramId, value, GroupId.EMPTY, batchParam);
    }

    public void addValue(final String name, final String value, final boolean batchParam) {
        if (name==null) {
            throw new IllegalArgumentException("name==null");
        }
        final ParamId paramId = new ParamId(name);
        addValue(paramId, value, GroupId.EMPTY, batchParam);
    }
    
    public void addValue(final String name, final String value, final String groupName, final boolean batchParam) {
        GroupId groupId;
        if (groupName==null) {
            groupId=GroupId.EMPTY;
        }
        else {
            groupId=new GroupId(groupName);
        }
        addValue(new ParamId(name), value, groupId, batchParam);
    }

    public void addValue(final ParamId id, final String value, final GroupId groupId, final boolean batchParam) {
        addValue(id, new ParamValue(value), groupId, batchParam);
    }

    public void addValue(final ParamId id, final ParamValue value, final GroupId groupId, final boolean batchParam) {
        if (id==null) {
            throw new IllegalArgumentException("id==null");
        }
        if (value==null) {
            throw new IllegalArgumentException("value==null");
        }
        if (groupId==null) {
            throw new IllegalArgumentException("groupId==null");
        }
        Param param;
        if (params.containsKey(id)){
            param=params.get(id);
        }
        else {
            param=new Param(id, batchParam);
            params.put(id, param);
        }
        param.addValue(groupId, value);
    }

    
    public void setValue(final ParamId paramId, final Param param) {
        params.put(paramId, param);
    }
    
    public void removeValue(final ParamId paramId) {
        params.remove(paramId);
    }

    /**
     * @param id - the unique id is the name of the parameter, as declared in the manifest
     * @return the Param for the given id, or null if none is found.
     */
    public Param getParam(final String id) {
        final ParamId paramId=new ParamId(id);
        final Param param=params.get(paramId);
        return param;
    }
    
    public Param getParam(final ParamId paramId) {
        final Param param=params.get(paramId);
        return param;
    }

    public List<ParamValue> getParamValues(final String id) {
        final ParamId paramId=new ParamId(id);
        final Param param=params.get(paramId);
        if (param==null) {
            return null;
        }
        return param.getValues();
    }
    
    /**
     * Is there a value set for the given parameter name?
     * 
     * A value is not set when:
     * a) the value is an empty list, or
     * b) the value is a 1-item list with a null value, or
     * c) the value is a 1-item list with an empty String value.
     * 
     * 
     * @param id
     * @return
     */
    public boolean hasValue(final String id) {
        final List<ParamValue> paramValues=getParamValues(id);
        if (paramValues==null) {
            return false;
        }
        //special-cases:
        //1) an empty list
        if (paramValues.size()==0) {
            return false;
        }
        //2) a one-item list, where the item is null
        if (paramValues.size()==1) {
            if (paramValues.get(0) == null) {
                return false;
            }
            else if (paramValues.get(0).getValue() == null) {
                return false;
            }
            //3) a one-item list, where the item is the empty string
            else if (paramValues.get(0).getValue() == "") {
                return true;
            }
        }
        return true;
    }
    
    public void setBatchParam(final String id, final boolean batchParam) {
        if (params==null) {
            throw new IllegalArgumentException("params==null");
        }
        Param param=params.get(new ParamId(id));
        if (param == null) {
            throw new IllegalArgumentException("no parameter matches id="+id);
        }
        param.setBatchParam(batchParam);
    }

    public boolean isBatchJob() {
        if (this.params == null) {
            return false;
        }
        for (final Param param : params.values()) {
            if (param.isBatchParam()) {
                return true;
            }
        }
        return false;
    }
    
    public Set<Param> getBatchParams() {
        if (this.params == null) {
            return Collections.emptySet();
        }
        Set<Param> batchParams = new LinkedHashSet<Param>();
        for (final Param param : params.values()) {
            if (param.isBatchParam()) {
                batchParams.add(param);
            }
        }
        return batchParams;
    }
    
    /**
     * Get the number of batch jobs to run, account for multiple batch parameters.
     * 
     * @return
     * @throws GpServerException
     */
    public int getNumBatchJobs() throws GpServerException {
        int numJobs=1; //always run at least one job
        for(final Param param : getBatchParams()) {
            if (numJobs==1) {
                numJobs=param.getNumValues();
            }
            else if (param.getNumValues()>1) {
                //validate
                if (numJobs != param.getNumValues()) {
                    //error
                    throw new GpServerException("Number of batch parameters doesn't match");
                }
            }
            return numJobs;
        }
        return numJobs;
    }
    
}
