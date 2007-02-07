package org.genepattern.server.webapp.jsf;

public class KeyValuePair {
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
