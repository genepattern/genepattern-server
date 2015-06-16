/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
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
