/*
 * AbstractReporter.java
 *
 * Created on May 6, 2003, 1:11 PM
 */

package org.genepattern.util;

/**
 *
 * @author  kohm
 */
abstract public class AbstractReporter implements Reporter {
    
    /** Creates a new instance of AbstractReporter */
    protected AbstractReporter() {
    }
    
    /** returns the current Reporter instance */
    public static final Reporter getInstance() {
        return REPORTER;
    }
    
    // fields
    /** if reporting is turned on */
    protected static final boolean VERBOSE = true;
    /** the Reporter */
    protected static final Reporter REPORTER; 
    
    /** static initializer */
    static {
        final boolean is_graphical = Boolean.getBoolean("gp.graphical");
        if( is_graphical )
            REPORTER = new ReporterWithGUI();
        else
            REPORTER = new ReporterNoGUI();
    }
}
