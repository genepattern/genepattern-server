/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.webservice;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jfree.util.Log;

/**
 * @author Jim Lerner
 *  
 */
public class TaskInfoAttributes extends HashMap implements Serializable {


	private static String NAME_START = "<void property=\"";

	private static String NAME_END = "\">";

	private static String VALUE_START = "<object>";

	private static String VALUE_END = "</object>\n</void>";

	/**
	 * method to encode a TaskInfoAttributes object into a serializable form for
	 * persisting in the database
	 * 
	 * @author Jim Lerner
	 * @return serialized version of the TaskInfoAttributes object
	 * @see java.io.ObjectOutputStream, java.io.Serializable
	 *  
	 */
	public String encode() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(baos);
		Map.Entry entry = null;
		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		out.println("<java version=\"1.0\" class=\"java.beans.XMLDecoder\">");
		out.println("<object class=\"" + getClass().getName() + "\">");
		for (Iterator items = entrySet().iterator(); items.hasNext();) {
			entry = (Map.Entry) items.next();
			out.println(NAME_START + (String) entry.getKey() + NAME_END);
			String val = (String) entry.getValue();
			if (val == null)
				val = "";
			val = replace(val, "\"", "\"\"");
			val = replace(val, "&", "&amp;");
			val = replace(val, "<", "&lt;");
			out.println(VALUE_START + val + VALUE_END);
		}
		out.println("</object>");
		out.println("</java>");
		return baos.toString();
	}

	/**
	 * factory method to create a TaskInfoAttributes object from an encoded XML
	 * stream of one that was previously saved
	 * 
	 * @author Jim Lerner
	 * @param taskInfoAttributesXml
	 *            XMLEncoded string of previously saved object
	 * @return TaskInfoAttributes object with the data restored via
	 *         serialization readObject
	 * @see #encode
	 * @see java.io.Serializable
	 *  
	 */

	public static TaskInfoAttributes decode(String taskInfoAttributesXML) {
		TaskInfoAttributes tia = null;
		if (taskInfoAttributesXML != null && taskInfoAttributesXML.length() > 0) {
			tia = new TaskInfoAttributes();
			String key = null;
			String value = null;
			// replace doubled quote with a single one
			String serializedData = taskInfoAttributesXML; // replace(taskInfoAttributesXML,
														   // "\"\"", "\"");
			int nameStart = 0;
			int nameEnd = 0;
			int valueStart = 0;
			int valueEnd = 0;
			while (true) {
				nameStart = serializedData.indexOf(NAME_START, valueEnd);
				if (nameStart == -1) {
					break;
				}
				nameEnd = serializedData.indexOf(NAME_END, nameStart
						+ NAME_START.length());
				if (nameEnd == -1) {
					System.err.println("no NAME_END after "
							+ serializedData.substring(nameStart));
					break;
				}
				key = serializedData.substring(nameStart + NAME_START.length(),
						nameEnd);

				valueStart = serializedData.indexOf(VALUE_START, nameEnd
						+ NAME_END.length());
				if (valueStart == -1) {
					break;
				}
				valueEnd = serializedData.indexOf(VALUE_END, valueStart
						+ VALUE_START.length());
				if (valueEnd == -1) {
					System.err.println("no VALUE_END after "
							+ serializedData.substring(valueStart));
					break;
				}
				value = serializedData.substring(valueStart
						+ VALUE_START.length(), valueEnd);
				value = replace(value, "\"\"", "\"");
				value = replace(value, "&lt;", "<");
				value = replace(value, "&amp;", "&");
				// System.out.println(key + "=" + value);
				tia.put(key, value);
				valueEnd = valueEnd + VALUE_END.length(); // for next iteration
			}
		}
		return tia;
	}

	public TaskInfoAttributes(Map map) {
		super(map);
	}

	public TaskInfoAttributes() {
		super();
	}

	public String toString() {
		StringBuffer sbOut = new StringBuffer();
		Map.Entry entry = null;
		for (Iterator items = entrySet().iterator(); items.hasNext();) {
			entry = (Map.Entry) items.next();
			if (sbOut.length() > 0)
				sbOut.append("\n");
			sbOut.append(entry.getKey());
			sbOut.append(": ");
			sbOut.append(entry.getValue());
		}
		return sbOut.toString();
	}

    /**
     * Convenience method that converts a get returning null into one returning
     * an empty string and that returns a String rather than an Object 
     * 
     * @author Jim Lerner 
     * @param name Name of parameter to look up in HashMap 
     * @return String value of parameter if it exists, otherwise empty string.
     */
	public String get(String name) {
	    Object obj = super.get(name);
        if (obj == null) {
            return "";
        }
        if (obj instanceof String) {
            return (String)obj;
        }
        Log.error("Unexpected type in TaskInfoAttributes.get("+name+"): '"+ obj.getClass().getName()+"'. Returning empty string");
        return obj.toString();
        //return "";
	}
	
	public Object put(Object key, Object value) {
	    if (value instanceof java.lang.Integer) {
	        Log.warn("Adding Integer value to TaskInfoAttributes. key="+key);
	    }
	    return super.put(key, value);
	}

	/*
	 * replace all instances of "find" in "original" and substitute "replace"
	 * for them
	 */
	public static final String replace(String original, String find,
			String replace) {
		StringBuffer res = new StringBuffer();
		int idx = 0;
		int i = 0;
		while (true) {
			i = idx;
			idx = original.indexOf(find, idx);
			if (idx == -1) {
				res.append(original.substring(i));
				break;
			} else {
				res.append(original.substring(i, idx));
				res.append(replace);
				idx += find.length();
			}
		}
		return res.toString();
	}

}
