/*
 * OVExceptionHandler.java
 *
 * Created on September 16, 2003, 1:34 PM
 */

package org.genepattern.gpge.ui.analysis;

/**
 *
 * @author  kohm
 */
public interface OVExceptionHandler {
    /** sets the error 
     *
     */
    public void setError(final String title, final String message, final Throwable thw);
    /** sets the warning
     */
    public void setWarning(final String title, final String message, final Exception ex);
    
}
