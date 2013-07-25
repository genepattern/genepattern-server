package org.genepattern.server.job.input.choice;

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
    final private String value; //actual value
    
    public Choice(final String value) {
        this(value, value);
    }
    
    public Choice(final String label, final String value) {
        this.label=label;
        this.value=value;
    }
    
    public String getLabel() {
        return label;
    }
    
    public String getValue() {
        return value;
    }
    
    public String toString() {
        if (label != null) {
            return label+"="+value;
        }
        return value;
    }
}
