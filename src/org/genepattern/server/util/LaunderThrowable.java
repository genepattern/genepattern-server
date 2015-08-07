/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.util;

/**                                                                                                                                  
 * StaticUtilities                                                                                                                   
 *                                                                                                                                   
 * @author Brian Goetz and Tim Peierls                                                                                               
 */
public class LaunderThrowable {

    /**                                                                                                                              
     * Coerce an unchecked Throwable to a RuntimeException                                                                           
     * <p/>                                                                                                                          
     * If the Throwable is an Error, throw it; if it is a                                                                            
     * RuntimeException return it, otherwise throw IllegalStateException                                                             
     */
    public static RuntimeException launderThrowable(Throwable t) {
        if (t instanceof RuntimeException)
            return (RuntimeException) t;
        else if (t instanceof Error)
            throw (Error) t;
        else
            throw new IllegalStateException("Not unchecked", t);
    }
}
