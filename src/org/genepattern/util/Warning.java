/*
 * Warning.java
 *
 * Created on February 28, 2003, 11:09 AM
 */

package org.genepattern.util;

/**
 * This is a RuntimeException that is only thrown to stop a particular execution
 * thread.  It represents an unusual, or undesirable situation that is not a problem.
 * This situation requires the current execution thread to stop but should not be 
 * reported to the user.
 *
 * @author  kohm
 */
public class Warning extends java.lang.RuntimeException {
    
    /**
     * Constructs an instance of <code>Warning</code> with the specified detail message.
     * @param msg the detail message.
     */
    public Warning(String msg) {
        super(msg);
    }
}
