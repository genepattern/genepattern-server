/*
 * ParamRetrievor.java
 *
 * Created on March 26, 2003, 10:52 AM
 */

package org.genepattern.gpge.ui.tasks;
 
import org.genepattern.analysis.ParameterInfo;

/**
 * interface defines how to get entered parameter values.
 * Instances are created by the UIRenderer objects.
 * @author  kohm
 */
public interface ParamRetrievor {
    /** returns the value */
    public String getValue() throws java.io.IOException;
    /** returns true if the value should be in a file */
    public boolean isFile();
    /** returns the file name URI or null */
    public String getSourceName() throws java.io.IOException;
    /** returns the ParameterInfo clone */
    public ParameterInfo getParameterInfo();
}
