/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.webservice;

/**
 * Defines a general exception a web service can throw when it encounters
 * problems.
 *
 * @author David Turner
 * @version $Version$
 */

public class WebServiceException extends Exception {
    private Throwable rootCause;

    /**
     * Constructs a new web service exeption.
     */
    public WebServiceException() {
        super();
    }

    /**
     * Constructs a new web service exeption with the specified message.
     *
     * @param message
     *            a <code>String</code> specifying the text of the exception
     *            message
     */
    public WebServiceException(String message) {
        super(message);
    }

    /**
     * Constructs a new web service exeption when the service needs to throw an
     * exeption and include a message about the "root cause" exception.
     *
     * @param message
     *            a <code>String</code> specifying the text of the exception
     *            message
     * @param rootCause
     *            the <code>Throwable</code> exception that caused the
     *            problem, making this service exeption necessary.
     */
    public WebServiceException(String message, Throwable rootCause) {
        super(message);
        this.rootCause = rootCause;
    }

    /**
     * Constructs a new web service exeption when the service needs to throw an
     * exeption and include a message about the "root cause" exception. The
     * exception's message is based on the localized message of the underlying
     * exception.
     * <p>
     * This method calls the <code>getLocalizedMessage</code> method on the
     * <code>Throwable</code> exception to get the localized exception
     * message.
     *
     * @param rootCause
     *            the <code>Throwable</code> exception that caused the
     *            problem, making this service exeption necessary.
     */
    public WebServiceException(Throwable rootCause) {
        super(rootCause.getLocalizedMessage());
        this.rootCause = rootCause;
    }

    /**
     * Returns the exception that caused this web service exception.
     *
     * @return the <code>Throwable</code> that caused this service exception
     */
    public Throwable getRootCause() {
        return this.rootCause;
    }

    public void setRootCause(Throwable t) {
        this.rootCause = t;
    }

    public String toString() {
        StringBuffer sbMessage = new StringBuffer();
        Throwable rc = getRootCause();
        String m = getMessage();
        if (m != null && m.length() > 0) {
            sbMessage.append(m);
            if (rc != null) {
                sbMessage.append(": ");
            }
        }
        if (rc != null) {
            sbMessage.append(rc.getMessage());
        }
        return sbMessage.toString();
    }

    public void printStackTrace() {
        if (getRootCause() != null) {
            getRootCause().printStackTrace();
        } else {
            super.printStackTrace();
        }
    }
}
