/*
 * ProgressObservable.java
 *
 * Created on April 15, 2003, 5:14 PM
 */

package org.genepattern.util;

/**
 * long tasks should implement this interface so that a periodicProgressObjserver
 * can show it's progress
 *
 * @author  kohm
 */
public interface ProgressObservable {
    /**
     * @return int negative if not ready or non-negative when total has been calculated
     */    
    int getTotal();
    /**
     * @return int the current state of progress of the task relative to the total
     */    
    int getCurrent();
}
