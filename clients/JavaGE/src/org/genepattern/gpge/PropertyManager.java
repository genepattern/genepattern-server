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


package org.genepattern.gpge;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Properties;

/**
 * Manages ~/gp/gp.properties file
 * 
 * @author Joshua Gould
 */
public class PropertyManager {
	private static Properties properties = new Properties();

	private static Properties defaults = new Properties();

	private static File propertiesFile;

	private static File propertiesFileTemp;

	private PropertyManager() {
	}

	static {
		String fs = File.separator;
		String path = System.getProperty("user.home") + fs + "gp" + fs;
		propertiesFile = new File(path + "gp.properties");
		propertiesFileTemp = new File(path + "#save#gp.properties#");
		createPropertiesFile();
		try {
			FileInputStream in = new FileInputStream(propertiesFile);
			loadProperties(properties, in);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private static void createPropertiesFile() {
		if (!propertiesFile.exists()) {
			File parent = propertiesFile.getParentFile();
			if (!parent.exists()) {
				parent.mkdirs();
			}
			try {
				propertiesFile.createNewFile();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	public static void saveProperties() throws IOException {
		// FIXME make backup of properties in case the following fails
		FileOutputStream out = new FileOutputStream(propertiesFileTemp);
		saveProperties(out);
		propertiesFile.delete();
		propertiesFileTemp.renameTo(propertiesFile);
	}

	private static void saveProperties(OutputStream out) throws IOException {
		createPropertiesFile();
		properties.store(out, "GenePattern properties");
		out.close();
	}

	public static String getProperty(String name) {
		String value = properties.getProperty(name);
		if (value != null) {
			return value;
		} else {
			return getDefaultProperty(name);
		}
	}

	/**
	 * Sets the given property. The change is not persisted until
	 * saveProperties is called. If <tt>value</tt> is <tt>null</tt>, then the 
	 * default value for the given property is used.
	 * 
	 * @param name the property name
	 * @param value the property value
	 */
	public static void setProperty(String name, String value) {
		if (value == null) {
			value = getDefaultProperty(name);
		}
		properties.setProperty(name, value);
	}

	public static String getDefaultProperty(String name) {
		return defaults.getProperty(name);
	}

	public static void setDefaultProperty(String name, String value) {
		defaults.setProperty(name, value);
	}

	private static void loadProperties(Properties into, InputStream in)
			throws IOException {
		try {
			into.load(in);
		} finally {
			in.close();
		}
	}

	public static boolean getBooleanProperty(String p) {
		return Boolean.valueOf(getProperty(p)).booleanValue();
	}
	
	public static int getIntProperty(String p) {
		try {
			return Integer.parseInt(getProperty(p));
		} catch(Exception e) {
			return Integer.MAX_VALUE;
		}
	}

	public static Color decodeColorFromProperties(String prop) {
		if (prop == null) {
			return null;
		}
		String[] rgbString = prop.split(",");
		int[] rgb = new int[3];
		int rgbIndex = 0;
		for (int i = 0; i < rgbString.length; i++) {
			if ("".equals(rgbString[i])) {
				continue;
			}
			try {
				rgb[rgbIndex] = Integer.parseInt(rgbString[i]);
				if (rgb[rgbIndex] < 0 || rgbIndex > 255) {
					return null;
				}
				rgbIndex++;
			} catch (Exception e) {
				return null;
			}
		}
		return new Color(rgb[0], rgb[1], rgb[2]);
	
	}

	public static String encodeColorToProperty(Color c) {
		if (c == null) {
			return null;
		}
		return c.getRed() + "," + c.getGreen() + "," + c.getBlue();
	}
	
}