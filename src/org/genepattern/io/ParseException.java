package org.genepattern.io;

/**
 *  Encapsulate a general error or warning from parsing a file. <p>
 *
 *  This class can contain basic error or warning information from either the
 *  parser or the application: a parser writer or application writer can
 *  subclass it to provide additional functionality. Handlers may throw this
 *  exception or any exception subclassed from it.</p> <p>
 *
 *  If the application needs to pass through other types of exceptions, it must
 *  wrap those exceptions in a ParseException or an exception derived from a
 *  ParseException.</p> <p>
 *
 *
 *
 * @author    David Megginson
 * @author    Joshua Gould
 */
public class ParseException extends Exception {


	/**
	 *  Create a new ParseException.
	 *
	 * @param  message  The error or warning message.
	 */
	public ParseException(String message) {
		super(message);
	}


	/**
	 *  Create a new ParseException wrapping an existing exception. <p>
	 *
	 *  The existing exception will be embedded in the new one, and its message
	 *  will become the default message for the ParseException.</p>
	 *
	 * @param  e  The exception to be wrapped in a ParseException.
	 */
	public ParseException(Exception e) {
		super(e);
	}


	/**
	 *  Create a new ParseException from an existing exception. <p>
	 *
	 *  The existing exception will be embedded in the new one, but the new
	 *  exception will have its own message.</p>
	 *
	 * @param  message  The detail message.
	 * @param  e        The exception to be wrapped in a ParseException.
	 */
	public ParseException(String message, Exception e) {
		super(message, e);
	}

}

