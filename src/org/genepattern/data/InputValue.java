/*
 * InputValue.java
 *
 * Created on August 21, 2002, 9:52 PM
 */

package org.genepattern.data;

/**
 *
 * @author  kohm
 */
abstract class InputValue {
    
    /** Creates a new instance of InputValue */
    protected InputValue(int value) {
        this.value = value;
    }
    
    /** gets the value */
    public final int getValue(){
        return value;
    } 
  
    /** returns a string rep */
    public String toString() {
        return String.valueOf(value);
    }
    // fields
    /** the value */
    public final int value;
    
}
