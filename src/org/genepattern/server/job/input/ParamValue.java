package org.genepattern.server.job.input;

public class ParamValue {
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
