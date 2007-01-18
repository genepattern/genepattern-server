/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

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
        StringBuffer buf = new StringBuffer();
        buf.append("Errors: ");
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