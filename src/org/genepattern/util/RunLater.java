/*
 * RunLater.java
 *
 * Created on January 30, 2002, 8:54 PM
 */

package org.genepattern.util;

/**
 * This is good for the SwingUtilities.invokeLater() SwingWorker etc.
 * The only method that needs to be implemented is runIt().
 *
 * @author  KOhm
 * @version 1.2
 */
abstract public class RunLater extends SafeRun {

    /** Creates new RunLater */
    public RunLater () {
    }

    /** this is where the subclasses implement whatever code they want to be run in a thread */
    abstract protected void runIt () throws Throwable;
    
    /** this is what is done just after the running  */
    protected void after ()  {   }
    /** this is what is done just before running  */
    protected void before () {   }
    /** this is only called if there was an error  */
    protected void error ()  {   }
    
}
