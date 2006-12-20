/*******************************************************************************
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright (2003-2006) by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are
 * reserved.
 *  
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 *  
 *******************************************************************************/
package org.genepattern.server.webservice.server;

/**
 * An interface for receving status messages.
 * 
 * @author jgould
 * 
 */
public interface Status {

    /**
     * Sets the status message
     * 
     * @param message
     *            The message to display
     */
    public void statusMessage(String message);

    public void endProgress();

    public void beginProgress(String string);

    public void continueProgress(int percent);

}