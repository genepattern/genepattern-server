/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.server.webservice;

import org.genepattern.webservice.WebService;

/**
 * Defines a generic web service.
 * <p>
 * <code>GenericWebService</code> implements the <code>WebService</code>
 * interface. <code>GenericWebService</code> may be directly extended for more
 * specific domain web services.
 * 
 * @author David Turner, Brian Gilman
 * @version $Version
 */

public abstract class GenericWebService implements WebService {
	private float schemeVersion;

	private String schemeName;

	/**
	 * Default constructor.
	 * <p>
	 * Constructs a <code>GenericWebService</code> object.
	 */
	public GenericWebService() {
	}

	/**
	 * Constructs a GenericWebService object with the specified scheme name and
	 * scheme version.
	 * 
	 * @param scheme
	 *            a <code>String</code> object containing the scheme name
	 * @param version
	 *            a <code>float</code> containing the scheme version
	 */
	public GenericWebService(String scheme, float version) {
		setEncodingScheme(scheme);
		setEncodingSchemeVersion(version);
	}

	/**
	 * Returns the name of the web service.
	 * 
	 * @return a <code>String</code> containing the name of this web service.
	 */
	public String getWebServiceName() {
		return "";
	}

	/**
	 * Returns information about the web service such as description, version,
	 * and author.
	 * <p>
	 * By default, this method returns an empty string. Override this method to
	 * have it return a meaningful value.
	 * 
	 * @return a <code>String</code> containing information about this web
	 *         service.
	 */
	public String getWebServiceInfo() {
		return "";
	}

	/**
	 * Sets the encoding scheme of the web service. The encoding scheme is the
	 * XML format that the data will be returned as (i.e. AGAVE, BSML).
	 * 
	 * @param scheme
	 *            a <code>String</code> object containing the encoding scheme
	 *            name
	 */
	public void setEncodingScheme(String scheme) {
		this.schemeName = scheme;
	}

	/**
	 * Returns the encoding scheme name of the web service.
	 * 
	 * @return a <code>String</code> object containing the encoding scheme
	 *         name
	 */
	public String getEncodingScheme() {
		return this.schemeName;
	}

	/**
	 * Returns the encoding scheme version of this web service.
	 * 
	 * @return a <code>float</code> containing the scheme version value
	 */
	public float getEncodingSchemeVersion() {
		return schemeVersion;
	}

	/**
	 * Sets the encoding scheme version.
	 * 
	 * @param version
	 *            a <code>float</code> containing the encoding scheme version
	 */
	public void setEncodingSchemeVersion(float version) {
		this.schemeVersion = version;
	}

	/**
	 * Ping the service to see if it's available.
	 * 
	 * @return a String that contains 'OK' if the service is available.
	 */
	public String ping() {
		return "OK";
	}
}
