package edu.mit.broad.gp.ws;

/**
 *  Encapsulates a general error a web service throws when it encounters
 * a problem.
 *
 * @author     David Turner
 * @author     Joshua Gould
 */

public class WebServiceException extends Exception {
	

	/**
	 *  Create a new WebServiceException.
	 *
	 * @param  message  The error or warning message.
	 */
	public WebServiceException(String message) {
		super(message);
	}


	/**
	 *  Create a new WebServiceException wrapping an existing cause. <p>
	 *
	 *  The existing cause will be embedded in the new one, and its message
	 *  will become the default message for the WebServiceException.</p>
	 *
	 * @param  cause  The cause to be wrapped in a WebServiceException.
	 */
	public WebServiceException(Throwable cause) {
		super(cause);
	}


	/**
	 *  Create a new WebServiceException from an existing cause. <p>
	 *
	 *  The existing cause will be embedded in the new one, but the new
	 *  cause will have its own message.</p>
	 *
	 * @param  message  The detail message.
	 * @param  cause    The cause to be wrapped in a WebServiceException.
	 */
	public WebServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}

