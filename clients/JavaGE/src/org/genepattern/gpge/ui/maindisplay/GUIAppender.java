/*
 * GUIAppender.java
 *
 * Created on November 25, 2003, 2:20 PM
 */

package org.genepattern.gpge.ui.maindisplay;

/**
 * Writes error messages to the Reporter
 * @author  kohm
 */
public class GUIAppender extends org.apache.log4j.AppenderSkeleton {
    
    /** Creates a new instance of GUIAppender */
    public GUIAppender() {
        super.setThreshold(org.apache.log4j.Priority.ERROR);
    }
    
    protected void append(org.apache.log4j.spi.LoggingEvent loggingEvent) {
        final String[] strg_rep = loggingEvent.getThrowableStrRep();
		  if(strg_rep==null) return;
        final String mesg = org.genepattern.util.ArrayUtils.toString(strg_rep, strg_rep.length, "\n");
        org.genepattern.util.AbstractReporter.getInstance().showError(mesg);
        //edu.mit.genome.gp.util.AbstractReporter.getInstance().showError(loggingEvent.getRenderedMessage());
        
    }
    
    public void close() {
    }
    
    public boolean requiresLayout() {
        return false;
    }
    public void setThreshold(org.apache.log4j.Priority threshold) {
        super.setThreshold(org.apache.log4j.Priority.ERROR);
    }
    
}
