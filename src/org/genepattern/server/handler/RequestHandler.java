/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/


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
