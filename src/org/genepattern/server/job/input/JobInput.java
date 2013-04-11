package org.genepattern.server.job.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

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
    final static private Logger log = Logger.getLogger(JobInput.class);

    /**
     * Unique identifier for a step in a pipeline.
     * @author pcarr
     */
    public static class StepId {
        private String id;
        
        public StepId(final String id) {
            this.id=id;
        }
        
        //copy constructor
        public StepId(final StepId in) {
            this.id=in.id;
        }
        
        public String getId() {
            return id;
        }
        
        public int hashCode() {
            if (id==null) {
                return "".hashCode();
            }
            return id.hashCode();
        }
        public boolean equals(Object obj) {
            if (!(obj instanceof StepId)) {
                return false;
            }
            if (id==null) {
                return ((StepId)obj).id==null;
            }
            return id.equals(((StepId)obj).id);
        }
    }

    public static class Param {
        public Param(final ParamId id, final boolean batchParam) {
            this.id=id;
            this.batchParam=batchParam;
        }
        public void addValue(ParamValue val) {
            if (values==null) {
                values=new ArrayList<ParamValue>();
            }
            values.add(val);
        }
        private ParamId id;
        private List<ParamValue> values;
        private boolean batchParam=false;
        
        public ParamId getParamId() {
            return id;
        }
        
        public List<ParamValue> getValues() {
            return values;
        }
        
        public int getNumValues() {
            if (values==null) {
                return 0;
            }
            return values.size();
        }
        
        public void setBatchParam(boolean batchParam) {
            this.batchParam=batchParam;
        }
        
        public boolean isBatchParam() {
            return batchParam;
        }
    }

    /**
     * Unique identifier for a parameter in a module.
     * @author pcarr
     *
     */
    public static class ParamId {
        transient int hashCode;
        private String fqName;
        public ParamId(final String fqName) {
            if (fqName==null) {
                throw new IllegalArgumentException("fqName==null");
            }
            if (fqName.length()==0) {
                throw new IllegalArgumentException("fqName is empty");
            }
            this.fqName=fqName;
            this.hashCode=fqName.hashCode();
        }
        public String getFqName() {
            return fqName;
        }
        
        public boolean equals(Object obj) {
            if (obj instanceof ParamId) {
                return fqName.equals( ((ParamId) obj).fqName );
            }
            return false;
        }
        public int hashCode() {
            return hashCode;
        }
    }

    public static class ParamValue {
        public ParamValue(String val) {
            this.value=val;
        }
        private String value;
        public String getValue() {
            return value;
        }
    }
    
    /**
     * The lsid for a module installed on the GP server.
     */
    private String lsid;
    public void setLsid(final String lsid) {
        this.lsid=lsid;
    }
    public String getLsid() {
        return this.lsid;
    }

    /**
     * The list of user-supplied parameter values.
     */
    private Map<ParamId, Param> params;
    public Map<ParamId, Param> getParams() {
        if (params==null) {
            log.debug("params==null, returning emptyMap");
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(params);
    }
    
    /**
     * Replaces the value for the given parameter with a new value.
     * @param name
     * @param value
     */
    public void addOrReplaceValue(final String name, final String value) {
        addOrReplaceValue(name, value, false);
    }
    public void addOrReplaceValue(final String name, final String value, final boolean batchParam) {
        if (name==null) {
            throw new IllegalArgumentException("name==null");
        }
        if (value==null) {
            throw new IllegalArgumentException("value==null");
        }
        if (params != null) {
            ParamId id = new ParamId(name);
            params.remove(id);
        }
        addValue(name, value, batchParam);
    }

    /**
     * Add a value for the param.
     * @param name, the parameter name, which is a unique id. Cannot be null.
     * @param value, the user provided input value, cannot be null.
     */
    public void addValue(final String name, final String value) {
        addValue(name, value, false);
    }

    public void addValue(final String name, final String value, final boolean batchParam) {
        if (name==null) {
            throw new IllegalArgumentException("name==null");
        }
        if (value==null) {
            throw new IllegalArgumentException("value==null");
        }
        ParamId id = new ParamId(name);
        Param param;
        if (params==null) {
            params=new HashMap<ParamId, Param>();
        }
        if (params.containsKey(id)){
            param=params.get(id);
        }
        else {
            param=new Param(id, batchParam);
            params.put(id, param);
        }
        param.addValue(new ParamValue(value));
    }

    /**
     * @param id - the unique id is the name of the parameter, as declared in the manifest
     * @return the Param for the given id, or null of none is found.
     */
    public Param getParam(final String id) {
        if (params==null) {
            return null;
        }
        final ParamId paramId=new ParamId(id);
        final Param param=params.get(paramId);
        return param;
    }
    
    public List<ParamValue> getParamValues(final String id) {
        if (params==null) {
            return null;
        }
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
            if (param.batchParam) {
                return true;
            }
        }
        return false;
    }
    
    public List<Param> getBatchParams() {
        if (this.params == null) {
            return Collections.emptyList();
        }
        List<Param> batchParams = new ArrayList<Param>();
        for (final Param param : params.values()) {
            if (param.batchParam) {
                batchParams.add(param);
            }
        }
        return batchParams;
    }
    
}
