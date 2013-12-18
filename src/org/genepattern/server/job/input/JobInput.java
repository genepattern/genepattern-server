package org.genepattern.server.job.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Collection;
import java.util.Set;


//import org.apache.log4j.Logger;
import org.genepattern.server.rest.GpServerException;

import com.google.common.base.Objects;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;

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
    //final static private Logger log = Logger.getLogger(JobInput.class);

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
        private ListMultimap<GroupId,ParamValue> groupedValues=LinkedListMultimap.create(1);
        private boolean batchParam=false;

        public Param(final ParamId id, final boolean batchParam) {
            this.id=id;
            this.batchParam=batchParam;
        }
        //copy constructor
        public Param(final Param in) {
            this.id=new ParamId(in.id);
            this.batchParam=in.batchParam;
            this.groupedValues=LinkedListMultimap.create(in.groupedValues);
        }
        
        public void addValue(final ParamValue val) {
            addValue(GroupId.EMPTY, val);
        }
        
        public void addValue(final GroupId groupId, final ParamValue val) {
            groupedValues.put(groupId, val);
        }
        
        public ParamId getParamId() {
            return id;
        }
        
        public List<ParamValue> getValues() {
            //return allValues;
            return Collections.unmodifiableList( new ArrayList<ParamValue>( groupedValues.values() ) );
        }
        
        public int getNumValues() {
            return groupedValues.size();
        }
        
        public int getNumGroups() {
            return groupedValues.keySet().size();
        }
        
        public void setBatchParam(boolean batchParam) {
            this.batchParam=batchParam;
        }
        
        public boolean isBatchParam() {
            return batchParam;
        }
        
        public Map<GroupId,Collection<ParamValue>> getGroupedValues() {
            return groupedValues.asMap();
        }
        
        public List<GroupId> getGroups() {
            return new ArrayList<GroupId>( groupedValues.keySet() );
        }
        public ParamValue getValue(final GroupId groupId, final int idx) {
            return groupedValues.get(groupId).get(idx);
        }
        public List<ParamValue> getValues(final GroupId groupId) {
            return groupedValues.get(groupId);
        }
    }

    /**
     * Unique identifier for a group of input ParamValue.
     * @author pcarr
     *
     */
    public static class GroupId {
        public static final GroupId EMPTY=new GroupId();
        
        private final String name;
        private final String groupId;
        private GroupId() {
            this.name="";
            this.groupId="";
        }
        public GroupId(final String nameIn) {
            if (nameIn==null || nameIn.length()==0) {
                throw new IllegalArgumentException("name not set");
            }
            this.name=nameIn.trim();
            this.groupId=this.name.toLowerCase();
        }

        //copy constructor
        public GroupId(final GroupId in) {
            this.name=in.name;
            this.groupId=in.groupId;
        }
        
        @Override
        public int hashCode() {
            return Objects.hashCode(groupId);
        }

        @Override
        public boolean equals(final Object obj){
            if (obj==null) {
                return false;
            }
            if (!(obj instanceof GroupId)) {
                return false;
            }
            final GroupId other = (GroupId) obj;
            final boolean eq = Objects.equal(groupId, other.groupId);
            return eq;
        }

    }
    
    /**
     * Unique identifier for a parameter in a module.
     * @author pcarr
     *
     */
    public static class ParamId {
        transient int hashCode;
        private final String fqName;
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
        private String lsid;
        public ParamValue(final String value, final String lsid){
            this.value=value;
            this.lsid = lsid;
        }
        //copy constructor
        public ParamValue(final ParamValue in) {
            this.value=in.value;
            this.lsid = in.lsid;
        }
        public String getValue() {
            return value;
        }

        public String getLSID()
        {
            return lsid;
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
        addValue(GroupId.EMPTY, new ParamId(name), value);
    }
    
    public void addValue(final GroupId groupId, final String name, final String value) {
        addValue(groupId, new ParamId(name), value);
    }
    
    public void addValue(final GroupId groupId, final ParamId paramId, final String value) {
        addValue(groupId, paramId, value, false);
    }
    
    public void addValue(final String name, final String value, final boolean batchParam) {
        if (name==null) {
            throw new IllegalArgumentException("name==null");
        }
        final ParamId paramId = new ParamId(name);
        addValue(GroupId.EMPTY, paramId, value, batchParam);
    }

    public void addValue(final GroupId groupId, final ParamId id, final String value, final boolean batchParam) {
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
        param.addValue(groupId, new ParamValue(value, lsid));
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
            if (param.batchParam) {
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
            if (param.batchParam) {
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
