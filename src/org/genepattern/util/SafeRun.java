/*
 * SafeRun.java  This class should be the subclass of any Thread or Runnable objects,
 * including event listeners.
 * Any Exceptions, Errors, or any other Throwable subclass will be caught and a message displayed 
 * to the user.  It is very important that all exceptions be treaded consistantly
 * And this class defines how to do that so it does not need to be replicated anywhere else.
 *
 * Created on January 30, 2002, 1:42 PM
 */

package org.genepattern.util;

import org.genepattern.util.Warning;

/**
 *
 * @author  KOhm
 * @version 1.2
 */
public abstract class SafeRun implements java.lang.Runnable {

    /** Creates new SafeRun */
    public SafeRun () {
    }

    /** 
     * here is where most everything happens
     * This method is "final" so that the subclasses cannot override it
     * @see doRun
     */
    public final void run () {
        try {
            before();
            
            runIt();
            
            after();
            
            return; // *** return from method here ***
            
        }catch (Throwable thw) {
            handleException(thw);
            // only get here if there was an exception
            error();
        }
//        } catch (Warning w) {
//            showWarning("Warning: ", w);
//        } catch (IllegalStateException ex) { // these are errors but the program may still be runnable
//            showError ("Error: ", ex);
//            ex.printStackTrace ();
////        } catch (AssertionError ex) {// these should not occur unless something is wrong with program
//            //This is a Java 1.4 feature
////            showError("Internal error: ", ex); 
////            ex.printStackTrace ();
//        } catch (RuntimeException ex) {
//            showError ("Internal Error! Should save work and restart program:\n", ex);
//            ex.printStackTrace (); // so assume something is wrong with program
//        } catch (java.lang.Error er) {
//            showError ("Severe Internal Error!\nSave work and restart program:\n", er);
//            er.printStackTrace (); // so assume something is wrong with program
//        } catch (Throwable t) { // don't know how serious the problem is but still shouldn't have happened
//            showError ("Internal error of unknown severity!\nAssume severe error - Save work and restart program:", t);
//            t.printStackTrace (); // so assume something is wrong with program
//        }
//        // only get here if there was an exception
//        error();
    }
    // abstract methods
    /** this is what is done just before running */
    abstract protected void before();
    /** this is where the subclasses implement whatever code they want to be run in a thread*/
    abstract protected void runIt() throws Throwable;
    /** this is what is done just after the running */
    abstract protected void after();
    /** this is only called if there was an error */
    abstract protected void error();
    
    // static methods
    /** Handles Exceptions by displaying them (if have GUI) and/or
     * printing/logging them
     * @param thw the exception or error - the throwable 
     */
    public static void handleException(final Throwable thw) {
        try {
            throw thw;
        } catch (Warning w) {
            showWarning("Warning: ", w);
        } catch (IllegalStateException ex) { // these are errors but the program may still be runnable
            showError ("Error: ", ex);
            ex.printStackTrace ();
//        } catch (AssertionError ex) {// these should not occur unless something is wrong with program
            //This is a Java 1.4 feature
//            showError("Internal error: ", ex); 
//            ex.printStackTrace ();
        } catch (RuntimeException ex) {
            showError ("Internal Error! Should save work and restart program:\n", ex);
            ex.printStackTrace (); // so assume something is wrong with program
        } catch (java.lang.Error er) {
            showError ("Severe Internal Error!\nSave work and restart program:\n", er);
            er.printStackTrace (); // so assume something is wrong with program
        } catch (Throwable t) { // don't know how serious the problem is but still shouldn't have happened
            showError ("Internal error of unknown severity!\nAssume severe error - Save work and restart program:", t);
            t.printStackTrace (); // so assume something is wrong with program
        }
    }
    /** these error messages are logged/displayed */
    private static void showError(final String msg, final Throwable ex) {
        REPORTER.showError(msg, ex);
    }
    /** these warning messages are logged/displayed */
    private static void showWarning(final String msg, final Warning w) {
        REPORTER.showWarning(msg, w);
    }
    // fields
    /** where to report warnings errors etc*/
    protected static final Reporter REPORTER = org.genepattern.util.AbstractReporter.getInstance();
}
