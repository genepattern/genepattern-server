package org.genepattern.server.job.input.choice;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Java bean representation of a single item in the choice menu for a module input parameter.
 * 
 * @author pcarr
 *
 */
public class Choice {
    /**
     * An optional display value.
     */
    final private String label;
    /**
     * The actual value.
     */
    final private String value;
    /**
     * flag indicating the value is a remote directory.
     * @param value
     */
    final private boolean remoteDir;
    
    private final HashCodeBuilder hcb;

    public Choice(final String value) {
        this(value, value);
    }
    
    public Choice(final String label, final String value) {
        this(label,value,false);
    }
    
    public Choice(final String label, final String value, final boolean isRemoteDir) {
        this.label=label;
        this.value=value;
        this.remoteDir=isRemoteDir;
        this.hcb = new HashCodeBuilder(17,31).
                append(label).
                append(value).
                append(remoteDir);
    }
    
    public String getLabel() {
        return label;
    }
    
    public String getValue() {
        return value;
    }
    
    public boolean isRemoteDir() {
        return remoteDir;
    }
    
    public String toString() {
        if (label != null) {
            return label+"="+value + ", isDir="+remoteDir;
        }
        return value+", isDir="+remoteDir;
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof Choice)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        Choice rhs = (Choice) obj;
        return new EqualsBuilder().
                append(label, rhs.label).
                append(value, rhs.value).
                append(remoteDir, rhs.remoteDir).
                isEquals();
    }
    
    public int hashCode() {
        return hcb.toHashCode();
    }
}
