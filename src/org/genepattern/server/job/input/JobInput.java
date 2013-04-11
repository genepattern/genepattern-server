package org.genepattern.server.job.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
        private ParamId id;
        private List<ParamValue> values=new ArrayList<ParamValue>();
        private boolean batchParam=false;

        public Param(final ParamId id, final boolean batchParam) {
            this.id=id;
            this.batchParam=batchParam;
        }
        //copy constructor
        public Param(final Param in) {
            this.id=new ParamId(in.id);
            this.batchParam=in.batchParam;
            
            //clone the list of values
            this.values=new ArrayList<ParamValue>();
            for(final ParamValue inValue : in.values) {
                this.values.add(new ParamValue(inValue));
            }
        }
        
        public void addValue(ParamValue val) {
            values.add(val);
        }
        
        public ParamId getParamId() {
            return id;
        }
        
        public List<ParamValue> getValues() {
            return values;
        }
        
        public int getNumValues() {
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
        //copy constructor
        public ParamId(final ParamId in) {
            this.fqName=in.fqName;
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
        private String value;
        public ParamValue(final String value) {
            this.value=value;
        }
        //copy constructor
        public ParamValue(final ParamValue in) {
            this.value=in.value;
        }
        public String getValue() {
            return value;
        }
    }
    
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
     * Replaces the value for the given parameter with a new value.
     * @param name
     * @param value
     */
    public void addOrReplaceValue(final String name, final String value) {
        addOrReplaceValue(name, value, false);
    }

    /**
     * Replaces the value for the given parameter with a new value,
     * also declaring the batchParam flag.
     * 
     * @param name
     * @param value
     * @param batchParam
     */
    public void addOrReplaceValue(final String name, final String value, final boolean batchParam) {
        if (name==null) {
            throw new IllegalArgumentException("name==null");
        }
        if (value==null) {
            throw new IllegalArgumentException("value==null");
        }
        params.remove(new ParamId(name));
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
        if (params.containsKey(id)){
            param=params.get(id);
        }
        else {
            param=new Param(id, batchParam);
            params.put(id, param);
        }
        param.addValue(new ParamValue(value));
    }
    
    public void setValue(final ParamId paramId, final Param param) {
        params.put(paramId, param);
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
