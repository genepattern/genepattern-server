/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.webservice;

/**
 * Defines an exception that a web service throws to indicate that it is
 * permanently or temporarily unavailable.
 * <p>
 * When a web service is permanently unavailable, something is wrong with it,
 * and it cannot handle requests until action is taken. For example, a web
 * service might be configured incorrectly.
 * <p>
 * A web service is temporarily unavailable if it cannont handle requests
 * momentarily due to some system-wide problem. For example, a database server
 * might be down or insufficient memory to handle request.
 * <p>
 * Web services containers can safely handle both types of unavailable
 * exceptions in the same way. Treating temporary unavailable exceptions makes
 * the container more robust. The web services container can block RPC calls to
 * a web service instead of rejecting them until the container restarts.
 * 
 * @author David Turner
 * @version $Version$
 */

public class UnavailableException extends WebServiceException {
	private WebService webService;

	private boolean permanent;

	private int time;

	/**
	 * Constructs a new web service exeption with the specified message
	 * indicating that the web service is permanently unavailable.
	 * 
	 * @param message
	 *            a <code>String</code> specifying the text of the exception
	 *            message
	 */
	public UnavailableException(String message) {
		super(message);
		this.permanent = true;
	}

	/**
	 * Constructs a new web service exeption with the specified message
	 * indicating that the web service is permanently unavailable along with the
	 * root cause exception.
	 * 
	 * @param message
	 *            a <code>String</code> specifying the text of the exception
	 *            message
	 * @param rootCause
	 *            the <code>Throwable</code> exception that caused the
	 *            problem, making this service exeption necessary.
	 */
	public UnavailableException(String message, Throwable rootCause) {
		super(message, rootCause);
		this.permanent = true;
	}

	/**
	 * Constructs a new unavialable exeption with the specified message.
	 * 
	 * @param webService
	 *            a <code>WebService</code> that is unavailable
	 * @param message
	 *            a <code>String</code> specifying the text of the exception
	 *            message
	 */
	public UnavailableException(WebService webService, String message) {
		super(message);
		this.webService = webService;
		this.permanent = true;
	}

	/**
	 * Constructs a new unavialable exeption with the specified message.
	 * 
	 * @param webService
	 *            a <code>WebService</code> that is unavailable
	 * @param message
	 *            a <code>String</code> specifying the text of the exception
	 *            message
	 * @param rootCause
	 *            the <code>Throwable</code> exception that caused the
	 *            problem, making this service exeption necessary.
	 */
	public UnavailableException(WebService webService, String message,
			Throwable rootCause) {
		super(message, rootCause);
		this.webService = webService;
		this.permanent = true;
	}

	/**
	 * Constructs a new unavialable exeption for a web service with the
	 * specified message and the time in seconds it's unavialable.
	 * 
	 * @param webService
	 *            a <code>WebService</code> that is unavailable
	 * @param message
	 *            a <code>String</code> specifying the text of the exception
	 *            message
	 * @param time
	 *            a <code>int</code> specifying the time in seconds the web
	 *            service is unavailable
	 */
	public UnavailableException(WebService webService, String message, int time) {
		super(message);
		this.webService = webService;
		this.permanent = false;
		if (time <= 0)
			this.time = -1;
		else
			this.time = time;
	}

	/**
	 * Constructs a new unavialable exeption for a web service with the
	 * specified message and the time in seconds it's unavialable.
	 * 
	 * @param webService
	 *            a <code>WebService</code> that is unavailable
	 * @param message
	 *            a <code>String</code> specifying the text of the exception
	 *            message
	 * @param time
	 *            a <code>int</code> specifying the time in seconds the web
	 *            service is unavailable
	 * @param rootCause
	 *            the <code>Throwable</code> exception that caused the
	 *            problem, making this service exeption necessary.
	 */
	public UnavailableException(WebService webService, String message,
			int time, Throwable rootCause) {
		super(message, rootCause);
		this.webService = webService;
		this.permanent = false;
		if (time <= 0)
			this.time = -1;
		else
			this.time = time;
	}

	/**
	 * Returns a <code>boolean</code> indicating whether this web service is
	 * permanently unavailable. If it is then something is wrong with the web
	 * service and requires system administration.
	 * 
	 * @return <code>true</code> if the web service is permanently
	 *         unavailable; <code>false</code> if the web service is available
	 *         or temporarily unavailable
	 */
	public boolean isPermanent() {
		return permanent;
	}

	/**
	 * Returns the web service that is unavailable.
	 * 
	 * @return the <code>WebService</code> object that is throwing the
	 *         <code>UnavailableException</code>
	 */
	public WebService getWebService() {
		return webService;
	}

	/**
	 * Returns the time in seconds that the web service is temporarily
	 * unavailable.
	 * <p>
	 * If this method returs a -1 this means that the web service is permanently
	 * unavailable. Otherwise, the time is returned that the web service will be
	 * unavailable.
	 * 
	 * @return an integer that represents the time, in seconds, that the web
	 *         service will be unavailable. A -1 will be returned if the service
	 *         is permanently unavailable.
	 */
	public int getUnavailableTime() {
		return permanent ? -1 : this.time;
	}
}
