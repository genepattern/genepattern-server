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

    /**
     * Create a new Choice, by default the label and the value will be the same.
     * @param valueIn
     */
    public Choice(final String valueIn) {
        this(valueIn, valueIn);
    }

    /**
     * Create a new Choice with the given label and value.
     * @param labelIn
     * @param valueIn
     */
    public Choice(final String labelIn, final String valueIn) {
        this(labelIn,valueIn,false);
    }
    
    /**
     * Create a new Choice instance with the given label, value, and isRemoteDir flag.
     * You must explicitly declare when you are creating a directory selection.
     * 
     * If necessary append a trailing slash ('/') to directory values.
     * For example, the value 'ftp://hostname.com/input.dir/A' will be converted to 'ftp://hostname.com/input.dir/A/'
     * when isRemoteDir is true.
     * 
     * @param labelIn
     * @param valueIn
     * @param isRemoteDir
     */
    public Choice(final String labelIn, final String valueIn, final boolean isRemoteDir) {
        //convert null label to empty string
        this.label= labelIn==null ? "" : labelIn;
        this.value=initValue(valueIn, isRemoteDir);
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
    
    private String initValue(final String valueIn, final boolean isRemoteDirIn) {
        //convert null value to empty string
        if (valueIn==null) {
            return "";
        }

        //special-case, always append a '/' character to directory values
        if (!isRemoteDirIn) {
            return valueIn;
        }
        if (valueIn.endsWith("/")) {
            return valueIn;
        }
        return valueIn+"/";
    }
    
    /**
     * For example,
     *     "A" == "A",
     *     "A" == "A/"
     *     "A/" == "A"
     *     "A/" == "A/"
     *     "" == "/"
     * @param lvalueIn
     * @param rvalueIn
     * @return
     */
    public static final boolean equalsIgnoreTrailingSlash(final String lvalueIn, final String rvalueIn, final boolean ignoreCase) {
        final String lvalue;
        final String rvalue;
        if (!lvalueIn.endsWith("/")) {
            lvalue=lvalueIn+"/";
        }
        else {
            lvalue=lvalueIn;
        }
        if (!rvalueIn.endsWith("/")) {
            rvalue=rvalueIn+"/";
        }
        else {
            rvalue=rvalueIn;
        }
        if (ignoreCase) {
            return lvalue.equalsIgnoreCase(rvalue);
        }
        else {
            return lvalue.equals(rvalue);
        }
    }
    
    public String toString() {
        if (label != null) {
            return label+"="+value + ", isDir="+remoteDir;
        }
        return value+", isDir="+remoteDir;
    }
    
    /**
     * Equality is based on the value and the isRemoteDir flag. The label is ignored.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Choice)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        Choice rhs = (Choice) obj;
        return new EqualsBuilder().
                append(value, rhs.value).
                append(remoteDir, rhs.remoteDir).
                isEquals();
    }
    
    public int hashCode() {
        return hcb.toHashCode();
    }
}
