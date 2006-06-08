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
 * 
 * @author Joshua Gould
 * 
 */
public class MetaData {
    private HashMap metaDataName2Depth;

    private ArrayList metaData;

    private int size;

    /**
     * Creates a new <tt>MetaData</tt> instance
     * 
     * @param size
     *            the size
     */
    public MetaData(int size) {
        this.size = size;
        metaData = new ArrayList();
        metaDataName2Depth = new HashMap();
    }

    /**
     * Creates a new <tt>MetaData</tt> instance
     * 
     * @param copy
     */
    protected MetaData(MetaData copy) {
        this.size = copy.size;
        metaData = (ArrayList) copy.metaData.clone();
        for (int i = 0; i < metaData.size(); i++) {
            String[] obj = (String[]) metaData.get(i);
            metaData.set(i, obj.clone());
        }
        metaDataName2Depth = (HashMap) copy.metaDataName2Depth.clone();
    }

    /**
     * Constructs and returns a new <tt>MetaData</tt> instance that contains
     * the indicated cells. Indices can be in arbitrary order.
     * 
     * @param indices
     *            The indices
     * 
     * @return the new MetaData
     * 
     */
    public MetaData slice(int[] indices) {
        MetaData m = new MetaData(indices.length);
        m.metaDataName2Depth = (HashMap) this.metaDataName2Depth.clone();
        for (int i = 0; i < metaData.size(); i++) {
            String[] obj = (String[]) metaData.get(i);
            String[] copy = new String[indices.length];
            for (int j = 0; j < indices.length; j++) {
                copy[j] = obj[indices[j]];
            }
            m.metaData.set(i, copy);
        }
        return m;

    }

    /**
     * Sets the meta data at the given index
     * 
     * @param index
     *            The index
     * @param name
     *            The name of the meta data at the given index
     * @param value
     *            The value of the meta data at the given index
     */
    public void setMetaData(int index, String name, String value) {
        getArray(name)[index] = value;
    }

    /**
     * Sets the meta data for the given name
     * 
     * 
     * @param name
     *            The name of the meta data
     * @param values
     *            The values of the meta data
     */
    public void setMetaData(String name, String[] values) {
        if (values.length != size) {
            throw new IllegalArgumentException(
                    "Length of values must be equal to size (" + size + ")");
        }
        metaData.set(getDepth(name), values);
    }

    /**
     * Gets the meta data at the given index
     * 
     * @param index
     *            The index
     * @param name
     *            The name of the meta data at the given index
     * @return The value of the meta data at the given index
     */
    public String getMetaData(int index, String name) {
        return getArray(name)[index];
    }

    /**
     * Gets the meta data for the given name
     * 
     * 
     * @param name
     *            The name of the meta data
     * @return The values of the meta data
     */
    public String[] getMetaData(String name) {
        return getArray(name);
    }

    /**
     * 
     * @param name
     *            The name of the meta data
     * @return index in metaData list
     */
    protected int getDepth(String name) {
        Integer depth = (Integer) metaDataName2Depth.get(name);

        if (depth == null) {
            depth = new Integer(metaData.size());
            for (int i = 0, size = depth.intValue() + 1; i < size; i++) {
                metaData.add(null);
            }
            metaData.set(depth.intValue(), new String[size]);
            metaDataName2Depth.put(name, depth);
        }
        return depth.intValue();
    }

    /**
     * 
     * @param name
     *            The name of the meta data
     * @return the array for the given name
     */
    protected String[] getArray(String name) {
        return (String[]) metaData.get(getDepth(name));
    }

    /**
     * Tests whether this instance contains the given meta data
     * 
     * @param name
     *            The name of the meta data
     * @return <tt>true</tt> if found, <tt>false</tt> otherwise
     */
    public boolean contains(String name) {
        return metaDataName2Depth.containsKey(name);
    }

    public String[] getNames() {
        return (String[]) metaDataName2Depth.keySet().toArray(new String[0]);
    }
}
