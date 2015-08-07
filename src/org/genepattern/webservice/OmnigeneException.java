/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


/*
 * OmnigeneException.java   05/08/2001
 * 
 * Whitehead Institute/MIT Center for Genome Research
 *
 */

package org.genepattern.webservice;

import java.util.Date;

/**
 * <code>OmnigeneException</code> is the superclass of all Omnigene exceptions
 * that can be thrown during normal operation of the Omnigene system.
 * 
 * @author DTurner,RKuttan
 * @version 1.0, 05/08/2001
 * @version 1.1, 11/06/2001
 */

public class OmnigeneException extends RuntimeException {

	private int errno = 0;

    
    public OmnigeneException(Exception cause) {
        super(cause);
    }

	/**
	 * Constructs a <code>OmnigeneException</code> with no detail message.
	 */
	public OmnigeneException() {
		super();
	}

	/**
	 * Constructs a <code>OmnigeneException</code> with the specified detail
	 * message.
	 * 
	 * @param s
	 *            the detail message.
	 *  
	 */
	public OmnigeneException(String s) {
		super(s);
	}

	/**
	 * Constructs an <code>OmnigeneException</code> class with the specified
	 * detail message.
	 * 
	 * @param errno
	 *            the Omnigene error number.
	 */
	public OmnigeneException(int errno) {
		super();
		this.errno = errno;
	}

	/**
	 * Constructs an <code>OmnigeneException</code> class with the specified
	 * detail message.
	 * 
	 * @param errno
	 *            the Omnigene error number.
	 * @param message
	 *            the detail message associated with this error number.
	 */
	public OmnigeneException(int errno, String message) {
		super(message);
		this.errno = errno;
	}

	/**
	 * Return the Omnigene errno
	 *  
	 */
	public int getOmnigeneErrno() {
		return this.errno;
	}

	/**
	 * Return the Omnigene error message that corresponds to the errno passed
	 * in.
	 */
	public String getOmnigeneMessage() {
		return OmnigeneErrorCode.getMessage(this.errno);

	}

	/**
	 * 
	 * Writes the exception message and a timestamp to the System.out file.
	 *  
	 */
	public void log() {
		StringBuffer sb = new StringBuffer();
		sb.append("<OMNIGENE ERROR : ");
		sb.append(new Date());
		sb.append(">");
		sb.append(this.getMessage());
		sb.append("</OMNIGENE ERROR>");
		System.out.println(sb);
	}
}
