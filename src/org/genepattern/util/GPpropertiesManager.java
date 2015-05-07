/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


/*
 * GPpropertiesManager.java
 *
 * Created on May 6, 2003, 12:36 PM
 */

package org.genepattern.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Handles GenePattern properties such as loading, saving, interpreting, etc.
 * 
 * @author kohm
 */
public class GPpropertiesManager {
	
	/** private access prevents instantiation of GPpropertiesManager */
	private GPpropertiesManager() {
	}

	/** gets the property if it exists otherwise sets the property to null */
	public static final String getProperty(final String key) {
		return getProperty(key, null);
	}

	/**
	 * gets the property if it exists otherwise sets the property to the
	 * specified default value and returns the default value
	 */
	public static final String getProperty(final String key,
			final String defaults) {
		final String prop = PROPERTIES.getProperty(key);
		if (prop == null || prop.trim().length() == 0) {
			PROPERTIES.setProperty(key, (defaults != null) ? defaults : "");
			return defaults;
		}
		return prop;
	}

	/**
	 * returns a true if the property is set to "true" (case insensitive) false
	 * if the property is null, "" (empty String), or "false"
	 * 
	 * @exception if
	 *                the value is not "true", "false", null, or empty string
	 */
	public static final boolean getBooleanProperty(final String key)
			throws java.text.ParseException {
		return getBooleanProperty(key, false);
	}

	/**
	 * returns a true if the property is set to "true" (case insensitive) false
	 * if the property is "false" if the Property is not defined ("", the empty
	 * String; or null) returns the default_value
	 * 
	 * @exception if
	 *                the value is not "true", "false", null, or empty string
	 */
	public static final boolean getBooleanProperty(final String key,
			final boolean default_value) throws java.text.ParseException {
		final String value = getProperty(key.trim(), String
				.valueOf(default_value));
		if (value == null || value.length() == 0) // undefined property key
			return default_value;

		if (value.equalsIgnoreCase("false"))
			return false;
		if (value.equalsIgnoreCase("true"))
			return true;
		throw new java.text.ParseException(
				"Not a string that represents a boolean value \"" + value
						+ "\" for propery \"" + key + "\"", 0);
	}

	/**
	 * returns the int value of the property if the Property is not defined ("",
	 * the empty String; or null) returns integer zero
	 * 
	 * @exception if
	 *                the value is not parsable as an int
	 */
	public static final int getIntProperty(final String key)
			throws NumberFormatException {
		return getIntProperty(key, 0);
	}

	/**
	 * returns the int value of the property if the Property is not defined ("",
	 * the empty String; or null) returns the default_value
	 * 
	 * @exception if
	 *                the value is not parsable as an int
	 */
	public static final int getIntProperty(final String key,
			final int default_value) throws NumberFormatException {
		final String value = getProperty(key.trim(), String
				.valueOf(default_value));
		if (value == null || value.length() == 0) // undefined property key
			return default_value;

		return Integer.parseInt(value);
	}

	/**
	 * returns the float value of the property if the Property is not defined
	 * ("", the empty String; or null) returns zero 0.0
	 * 
	 * @exception if
	 *                the value is not parsable as a float
	 */
	public static final float getFloatProperty(final String key)
			throws NumberFormatException {
		return getFloatProperty(key, 0.0f);
	}

	/**
	 * returns the float value of the property if the Property is not defined
	 * ("", the empty String; or null) returns the default_value
	 * 
	 * @exception if
	 *                the value is not parsable as a float
	 */
	public static final float getFloatProperty(final String key,
			final float default_value) throws NumberFormatException {
		final String value = getProperty(key.trim(), String
				.valueOf(default_value));
		if (value == null || value.length() == 0) // undefined property key
			return default_value;

		return Float.parseFloat(value);
	}

	public static final void setProperty(final String key, final String value) {
		PROPERTIES.setProperty(key, value);
	}

	/** saves the GenePattern Properties */
	public static final void saveGenePatternProperties()
			throws java.io.IOException {
		final File props_file = getGpPropFile();
		final FileOutputStream out = new FileOutputStream(props_file);
		PROPERTIES.store(out, "GenePattern properties");
		out.close();
	}

	/** gets the GenePattern property file or creates one if it doesn't exist */
	protected final static File getGpPropFile() throws java.io.IOException {
		final File file = new File(PROP_FILE_NAME);
		boolean create_parent = false;
		try {
			if (!file.exists()) {
				final File parent = file.getParentFile();
				if (!parent.exists()) { // create it
					create_parent = true;
					parent.mkdirs();
				}
				create_parent = false;
				file.createNewFile();
			}
		} catch (IOException ioe) {
			System.err
					.println("While getting the properties file the following error occured:");
			ioe.printStackTrace();
			throw new IOException("While trying to create the "
					+ ((create_parent) ? "application directory"
							: "properties file") + " an error occurred:\n"
					+ ioe.getMessage());
		}
		return file;
	}

	//    /** creates an editor panel */
	//    public static final
	// edu.mit.genome.gp.ui.propertyviewer.PropertyViewerPanel
	// createEditorPanel(final boolean editable) {
	//        return new
	// edu.mit.genome.gp.ui.propertyviewer.PropertyViewerPanel(PROPERTIES,
	// editable);
	//    }
	/**
	 * gets the property object Note this should never be a static method and
	 * only used in special cases
	 */
	protected final Properties getInternal() {
		return PROPERTIES;
	}

	/**
	 * //fields /** the GenePattern properties
	 */
	private static final Properties PROPERTIES;

	/** where the gp.properties file is located */
	private static final String PROP_FILE_NAME;

	/** the GenePattern home dir (~/gp/) */
	public static final File GP_HOME;
	/** static initializer */
	static {
		
		PROPERTIES = new java.util.Properties();
		File home = null;
		//boolean got_gp = false;
		final String fs = File.separator;
		PROP_FILE_NAME = System.getProperty("user.home") + fs + "gp" + fs
				+ "gp.properties";
		try {
			final File props_file = getGpPropFile();
			if (props_file != null && props_file.exists()) {
				//got_gp = true;
				home = props_file.getParentFile();
				final FileInputStream in = new FileInputStream(props_file);
				PROPERTIES.load(in);
				in.close();
			} else {
				final File parent = props_file.getParentFile();
				if (!parent.exists())
					parent.createNewFile();
				props_file.createNewFile();
			}
		} catch (IOException ioe) {
			
			System.err.println(
					"Problems reading or creating the properties file");
		}
		GP_HOME = (home != null) ? home : new File(System
				.getProperty("user.home")
				+ File.separator + "gp");
		
		
	}
}
