/*
 * DefaultExceptionHandler.java
 *
 * Created on September 16, 2003, 2:05 PM
 */

package org.genepattern.gpge.ui.maindisplay;

import org.genepattern.util.AbstractReporter;
 
/**
 *
 * @author  kohm
 */
public final class DefaultExceptionHandler implements org.genepattern.gpge.ui.tasks.OVExceptionHandler {
    
    /** Creates a new instance of DefaultExceptionHandler */
    private DefaultExceptionHandler() {
    }
    /** gets the instance */
    public static DefaultExceptionHandler instance() {
        return INSTANCE;
    }
    public void setError(final String title, final String message, final Throwable thw) {
        AbstractReporter.getInstance().showError(title, message, thw);
    }    
    
    public void setWarning(final String title, final String message, final Exception ex) {
        AbstractReporter.getInstance().showWarning(title, message, ex);
    }
    
    // fields
    private static final DefaultExceptionHandler INSTANCE = new DefaultExceptionHandler();
    
}
