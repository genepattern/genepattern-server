/*
 * PasteException.java
 * This exception is thrown when an object is not permitted to be pasted into
 * a component.  This indicates an error condition and indicates that the 
 * pasting operation should fail.
 *
 * Created on August 28, 2001, 3:22 PM
 */

//package edu.mit.genome.expresso.ui.graphics;
package org.genepattern.modules.ui.graphics;
/**
 *
 * @author  kohm
 * @version 
 */
public class PasteException extends PasteWarning {
   /**
     * Constructs an <code>PasteException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public PasteException(String msg) {
        super(msg);
    }
}


