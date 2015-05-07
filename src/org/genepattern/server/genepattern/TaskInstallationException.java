/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.genepattern;

import java.util.Vector;

public class TaskInstallationException extends Exception {
    private Vector<String> errors;

    /**
     * Creates a new <tt>TaskInstallationException</tt> instance
     * 
     * @param errors
     *            a vector of error messages
     */
    public TaskInstallationException(Vector<String> errors) {
        this.errors = errors;
    }

    /**
     * Returns the vector of error messages.
     * 
     * @return the error messages.
     */
    public Vector<String> getErrors() {
        return errors;
    }

    @Override
    public String getMessage() {
	return getMessage("Errors");
    }
    
    public String getWarningMessage() {
	return getMessage("Warnings");
    }
    
    private String getMessage(String type) {
	StringBuffer buf = new StringBuffer();
        buf.append(type+": ");
        for (int i = 0, size = errors.size(); i < size; i++) {
            if (i > 0) {
                buf.append(" ");
            }
            buf.append(String.valueOf(i + 1));
            buf.append(". ");
            buf.append(errors.get(i));
        }
        return buf.toString();
    }
}
