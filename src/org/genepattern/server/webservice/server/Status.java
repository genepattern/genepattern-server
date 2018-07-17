/*******************************************************************************
 * Copyright (c) 2003-2018 Regents of the University of California and Broad Institute. All rights reserved.
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
