/*
 * DuplicateKeyException.java
 *
 * Created on November 12, 2002, 10:53 PM
 */

package org.genepattern.data.annotation;

/**
 * This exception is thrown if an annotation key or value is duplicated
 *
 * @author  kohm
 */
public class DuplicateException extends java.lang.RuntimeException {
    
    /**
     * Creates a new instance of <code>DuplicateKeyException</code> without detail message.
     */
    public DuplicateException() {
    }
    
    
    /**
     * Constructs an instance of <code>DuplicateKeyException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public DuplicateException(String msg) {
        super(msg);
    }
}
