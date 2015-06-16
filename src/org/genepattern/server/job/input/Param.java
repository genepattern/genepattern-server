/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
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

    /**
     * Get the ordered list of values for this parameter, based on the order in which
     * the values were added.
     * @return
     */
    public List<ParamValue> getValues() {
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

    /**
     * Get the distinct list of groups, in the order in which the groups were added.
     * @return
     */
    public List<GroupId> getGroups() {
        return new ArrayList<GroupId>( groupedValues.keySet() );
    }
    
    /**
     * Get the nth value in the given group, indexed by 0, based on the order
     * in which the values were added to the group.
     */
    public ParamValue getValue(final GroupId groupId, final int idx) {
        return groupedValues.get(groupId).get(idx);
    }
    
    /**
     * Get the ordered list of values in the given group.
     * @param groupId
     * @return
     */
    public List<ParamValue> getValuesInGroup(final GroupId groupId) {
        return groupedValues.get(groupId);
    }

    /**
     * Get the collection of all values as an Entry so that you can get both the groupId
     * and the paramValue.
     * 
     * This collection is ordered by the order in which the values were added.
     * 
     * @return
     */
    public Collection<Map.Entry<GroupId,ParamValue>> getValuesAsEntries() {
        return groupedValues.entries();
    }
}
