/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.choice;


/**
 * Exception thrown when initializing the ChoiceInfo for a module input parameter.
 * @author pcarr
 *
 */
public class ChoiceInfoException extends Exception {

    public ChoiceInfoException(final Throwable t) {
        status=new ChoiceInfo.Status(ChoiceInfo.Status.Flag.ERROR, t.getLocalizedMessage());
    }
    
    public ChoiceInfoException(final String message) {
        status=new ChoiceInfo.Status(ChoiceInfo.Status.Flag.ERROR, message);
    }
    
    final ChoiceInfo.Status status;
    public ChoiceInfo.Status getStatus() {
        return status;
    }

}
