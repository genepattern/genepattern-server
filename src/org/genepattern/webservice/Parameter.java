/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.webservice;

import java.io.File;
import java.io.IOException;

/**
 * A job input parameter name-value pair.
 * 
 * @author Joshua Gould
 */
public class Parameter {
        // Changed name and value to be protected so that TaskParameter subclass
        // can change its values.
	protected String name;

	protected String value;

	/**
	 * Creates a new Parameter instance.
	 * 
	 * @param name
	 *            The parameter name.
	 * @param value
	 *            The parameter value, as a string.
	 */
	public Parameter(String name, String value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Creates a new Parameter instance.
	 * 
	 * @param name
	 *            The parameter name.
	 * @param value
	 *            The parameter value, as a double.
	 */
	public Parameter(String name, double value) {
		this.name = name;
		this.value = String.valueOf(value);
	}

	/**
	 * Creates a new Parameter instance.
	 * 
	 * @param name
	 *            The parameter name.
	 * @param value
	 *            The parameter value, as an integer.
	 */
	public Parameter(String name, int value) {
		this.name = name;
		this.value = String.valueOf(value);
	}

	/**
	 * Creates a new Parameter instance.
	 * 
	 * @param name
	 *            The parameter name.
	 * @param value
	 *            The parameter value, as a file. The argument is converted to a
	 *            string representation by invoking getCanonicalPath on the
	 *            argument.
	 * @exception IOException
	 *                If an I/O error occurs, which is possible because the
	 *                construction of the canonical pathname may require
	 *                filesystem queries.
	 * @exception SecurityException
	 *                If a required system property value cannot be accessed.
	 */
	public Parameter(String name, File value) throws IOException,
			SecurityException {
		this.name = name;
		this.value = value.getCanonicalPath();
	}

	/**
	 * Gets the name of this parameter.
	 * 
	 * @return The name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the value of this parameter.
	 * 
	 * @return The value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Returns a string representation of this parameter.
	 * 
	 * @return a string representation of this parameter.
	 */
	public String toString() {
		return name + "=" + value;
	}
}

