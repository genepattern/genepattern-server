/*
 * MutableFloat.java
 *
 * Created on March 30, 2002, 1:06 PM
 */

package org.genepattern.util;

/**
 *
 * @author  keith
 * 
 */
public class MutableFloat {

    /** Creates new MutableFloat */
    public MutableFloat() {
    }
    
    /** determines if this is equal to anothe MutableFloat */
    public final boolean equal(Object object) {
        if(object instanceof MutableFloat) {
            return (value == ((MutableFloat)object).value);
        }
        return false;
    }
    /** string rep. of this object */
    public String toString() {
        return String.valueOf(value);
    }
    
    // fields
    /** the float */
    public float value;
}
