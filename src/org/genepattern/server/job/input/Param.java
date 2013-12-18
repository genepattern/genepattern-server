package org.genepattern.server.job.input;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * The list of zero or more input values for a given parameter for a given job submission.
 * Also store batch and group information.
 * @author pcarr
 *
 */
public class Param {
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
