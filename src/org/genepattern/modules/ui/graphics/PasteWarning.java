/*
 * PasteException.java
 * This exception is thrown when an object is not permitted to be pasted into
 * a component.  It is a warning and thus should not cause the rest of the 
 * operation to fail.
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
public class PasteWarning extends org.genepattern.util.Warning /* used to extend Exception*/{
   /**
     * Constructs an <code>PasteException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public PasteWarning(String msg) {
        super(msg);
    }
}


