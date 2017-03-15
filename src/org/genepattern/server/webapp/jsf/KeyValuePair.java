/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp.jsf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.genepattern.server.dm.UrlUtil;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

public class KeyValuePair implements Serializable {
    private static final long serialVersionUID = 7245950250797266267L;
    
    /** sort by key, ignore case, treat null key as empty string */
    public static final Comparator<KeyValuePair> sortByKeyIgnoreCase = new Comparator<KeyValuePair>() {
        @Override
        public int compare(final KeyValuePair o1, final KeyValuePair o2) {
            final String k1 = o1 == null ? "" : Strings.nullToEmpty(o1.getKey());
            final String k2 = o2 == null ? "" : Strings.nullToEmpty(o2.getKey());
            return k1.compareToIgnoreCase(k2);
        } 
    };

    /** sort by key then by value, ignore case, treat null as empty */
    public static final Comparator<KeyValuePair> sortByKeyValueIgnoreCase = new Comparator<KeyValuePair>() {
        public int compare(KeyValuePair arg0, KeyValuePair arg1) {
            final String k0 = arg0 == null ? "" : Strings.nullToEmpty( arg0.getKey() );
            final String k1 = arg1 == null ? "" : Strings.nullToEmpty( arg1.getKey() );
            //sort by key then by value
            int c = k0.compareToIgnoreCase(k1);
            if (c == 0) {
                final String v0 = arg0 == null ? "" : arg0.getValue();
                final String v1 = arg1 == null ? "" : arg1.getValue();
                c = v0.compareToIgnoreCase(v1);
            }
            return c;
        }
    };

    protected static SortedSet<KeyValuePair> setFromProperties(final Properties props) {
        return setFromProperties(props, sortByKeyIgnoreCase);
    }
    
    /**
     * Create a new SortedSet of KeyValuePair entries from a Properties instance.
     */
    protected static SortedSet<KeyValuePair> setFromProperties(final Properties props, final Comparator<KeyValuePair> c) {
        final SortedSet<KeyValuePair> s = new TreeSet<KeyValuePair>(c);
        for(final String key : props.stringPropertyNames()) {
            s.add(new KeyValuePair(key, props.getProperty(key)));
        }
        return s;
    }

    /**
     * Create a new List of KeyValuePair items from a Properties instance.
     * @param props
     * @return
     */
    public static final List<KeyValuePair> fromProperties(final Properties props) {
        Set<KeyValuePair> set=setFromProperties(props, sortByKeyIgnoreCase);
        final List<KeyValuePair> list = new ArrayList<KeyValuePair>();
        list.addAll(set);
        return list;
    }

    public static ImmutableList<KeyValuePair> listFromProperties(final Properties props) { 
        return listFromProperties(props, sortByKeyIgnoreCase);
    }

    public static ImmutableList<KeyValuePair> listFromProperties(final Properties props, final Comparator<KeyValuePair> c) { 
        Set<KeyValuePair> set=setFromProperties(props, c);
        return ImmutableList.copyOf( set );
    }
    
    private String altKey;
    private String key;
    private String value;

    public KeyValuePair() {
    }

    public KeyValuePair(String key, String value) {
        this.key = key;
        this.value = value;
        this.altKey = key;
    }

    public KeyValuePair(String key, String altKey, String value) {
        this.key = key;
        this.value = value;
        this.altKey = altKey;
    }

    public String getAltKey() {
        return altKey;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
    
    public String getEncodedValue() {
        return UrlUtil.encodeURIcomponent(value);
    }

    public void setAltKey(String altKey) {
        this.altKey = altKey;
    }

    public void setKey(String n) {
        key = n;
    }

    public void setValue(String n) {
        value = n;
    }

    @Override
    public String toString() {
        return "Key=" + getKey() + ", Alt. Key=" + getAltKey() + ", Value=" + getValue();
    }

}
