/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp.jsf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.genepattern.server.dm.UrlUtil;

public class KeyValuePair implements Serializable {
    private static final long serialVersionUID = 7245950250797266267L;

    /**
     * Create a new List of KeyValuePair items from a Properties instance.
     * @param props
     * @return
     */
    public static final List<KeyValuePair> createListFromProperties(final Properties props) {
        final List<KeyValuePair> customSettings = new ArrayList<KeyValuePair>();
        for(final String key : props.stringPropertyNames()) {
            customSettings.add(new KeyValuePair(key, props.getProperty(key)));
        }
        return customSettings;        
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
