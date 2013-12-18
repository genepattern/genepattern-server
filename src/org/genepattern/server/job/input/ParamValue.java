package org.genepattern.server.job.input;

public class ParamValue {
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
