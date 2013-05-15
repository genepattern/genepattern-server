/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp.jsf;

import java.io.Serializable;

import org.genepattern.server.dm.UrlUtil;

public class KeyValuePair implements Serializable {
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
