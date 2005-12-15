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


package org.genepattern.server.handler;

/**
 * RequestHandler.java
 * 
 * @author rajesh kuttan
 * @version
 */

public abstract class RequestHandler {

	private java.io.Serializable clientRequest = null;

	public RequestHandler() {
	}

	public void setClientRequest(java.io.Serializable clientRequest) {
		this.clientRequest = clientRequest;
	}

	/*
	 * public java.io.Serializable executeRequest() throws OmnigeneException {
	 * throw new OmnigeneException("RequestHandler:execute .."); }
	 */

}