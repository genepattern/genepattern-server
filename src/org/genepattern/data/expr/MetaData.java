/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.data.expr;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Joshua Gould
 */
public class MetaData {
    private HashMap metaDataName2Depth;

    private ArrayList metaDataList;

    private int capacity;

    /**
     * Creates a new <tt>MetaData</tt> instance
     *
     * @param capacity the capacity
     */
    public MetaData(int capacity) {
        this.capacity = capacity;
        metaDataList = new ArrayList();
        metaDataName2Depth = new HashMap();
    }

    /**
     * Creates a new <tt>MetaData</tt> instance
     *
     * @param copy
     */
    protected MetaData(MetaData copy) {
        this.capacity = copy.capacity;
        metaDataList = (ArrayList) copy.metaDataList.clone();
        for (int i = 0; i < metaDataList.size(); i++) {
            String[] obj = (String[]) metaDataList.get(i);
            metaDataList.set(i, obj.clone());
        }
        metaDataName2Depth = (HashMap) copy.metaDataName2Depth.clone();
    }

    /**
     * Constructs and returns a new <tt>MetaData</tt> instance that contains
     * the indicated cells. Indices can be in arbitrary order.
     *
     * @param indices The indices
     * @return the new MetaData
     */
    public MetaData slice(int[] indices) {
        MetaData slicedMetaData = new MetaData(indices.length);
        slicedMetaData.metaDataName2Depth = (HashMap) this.metaDataName2Depth.clone();
        slicedMetaData.metaDataList = new ArrayList();
        for (int i = 0, size = this.metaDataList.size(); i < size; i++) {
            String[] obj = (String[]) this.metaDataList.get(i);
            String[] copy = new String[indices.length];
            for (int j = 0, length = indices.length; j < length; j++) {
                copy[j] = obj[indices[j]];
            }
            slicedMetaData.metaDataList.add(copy);
        }
        return slicedMetaData;
    }


    /**
     * Sets the meta data at the given index
     *
     * @param index The index
     * @param name  The name of the meta data at the given index
     * @param value The value of the meta data at the given index
     */
    public void setMetaData(int index, String name, String value) {
        getArray(name)[index] = value;
    }

    /**
     * Sets the meta data for the given name
     *
     * @param name   The name of the meta data
     * @param values The values of the meta data
     */
    public void setMetaData(String name, String[] values) {
        if (values.length != capacity) {
            throw new IllegalArgumentException(
                    "Length of values must be equal to capacity (" + capacity + ")");
        }
        metaDataList.set(getDepth(name), values);
    }

    /**
     * Gets the meta data at the given index
     *
     * @param index The index
     * @param name  The name of the meta data at the given index
     * @return The value of the meta data at the given index
     */
    public String getMetaData(int index, String name) {
        return getArray(name)[index];
    }

    /**
     * Gets the meta data for the given name
     *
     * @param name The name of the meta data
     * @return The values of the meta data
     */
    public String[] getMetaData(String name) {
        return getArray(name);
    }

    /**
     * @param name The name of the meta data
     * @return index in metaDataList list
     */
    protected int getDepth(String name) {
        Integer depth = (Integer) metaDataName2Depth.get(name);
        int _depth;
        if (depth == null) {
            depth = new Integer(metaDataList.size());
            _depth = depth.intValue();
            for (int i = metaDataList.size(); i <= _depth; i++) {
                metaDataList.add(new String[capacity]);
            }
            metaDataList.set(_depth, new String[capacity]);
            metaDataName2Depth.put(name, depth);
        } else {
            _depth = depth.intValue();
        }
        return _depth;
    }

    /**
     * @param name The name of the meta data
     * @return the array for the given name
     */
    protected String[] getArray(String name) {
        return (String[]) metaDataList.get(getDepth(name));
    }

    /**
     * Tests whether this instance contains the given meta data
     *
     * @param name The name of the meta data
     * @return <tt>true</tt> if found, <tt>false</tt> otherwise
     */
    public boolean contains(String name) {
        return metaDataName2Depth.containsKey(name);
    }

    public String[] getNames() {
        return (String[]) metaDataName2Depth.keySet().toArray(new String[0]);
    }
}
