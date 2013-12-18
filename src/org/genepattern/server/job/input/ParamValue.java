package org.genepattern.server.job.input;

import com.google.common.base.Objects;

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
    
    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public boolean equals(final Object obj){
        if (obj==null) {
            return false;
        }
        if (!(obj instanceof ParamValue)) {
            return false;
        }
        final ParamValue other = (ParamValue) obj;
        final boolean eq = Objects.equal(value, other.value);
        return eq;
    }

}
