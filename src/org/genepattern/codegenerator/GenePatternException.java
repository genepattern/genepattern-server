/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.codegenerator;

import java.util.Enumeration;
import java.util.Vector;

public class GenePatternException extends Exception {

	protected String what = null;

	protected Vector vItems = null;

	protected Throwable underlyingThrowable = null;

	protected boolean AS_STRING = true;

	protected boolean AS_HTML = false;

	public GenePatternException(String what) {
		this.what = what;
	}

	public GenePatternException(String what, Vector vItems) {
		this.what = what;
		this.vItems = vItems;
	}

	public GenePatternException(String what, Throwable underlyingThrowable) {
		this.what = what;
		this.underlyingThrowable = underlyingThrowable;
	}

	public String toString() {
		return format(AS_STRING);
	}

	public String toHTML() {
		return format(AS_HTML);
	}

	protected String format(boolean asString) {
		StringBuffer s = new StringBuffer();
		String EOL = asString ? "\n" : "<br>\n";
		s.append(what);
		if (vItems.size() > 0) {
			for (Enumeration eItems = vItems.elements(); eItems
					.hasMoreElements();) {
				s.append(EOL);
				s.append(eItems.nextElement());
			}
		}
		if (underlyingThrowable != null) {
			s.append(EOL);
			s.append("underlying throwable: " + EOL);
			if (!asString)
				s.append("<pre>");
			s.append(underlyingThrowable.toString());
			if (!asString)
				s.append("</pre><br>\n");
		}
		return s.toString();
	}

	public static void main(String args[]) {
		System.out.println("self-test for GenePatternException class");

		GenePatternException gpe = new GenePatternException(
				"simple exception without vector or throwable");
		System.out.println(gpe);
		System.out.println("as HTML: " + gpe.toHTML());

		Vector v = new Vector();
		v.add("problem 1");
		v.add("problem 2");
		gpe = new GenePatternException("exception with vector of problems", v);
		System.out.println(gpe);
		System.out.println("as HTML: " + gpe.toHTML());

		Throwable t = new Throwable("this is the underlying throwable");
		gpe = new GenePatternException("exception with throwable", t);
		System.out.println(gpe);
		System.out.println("as HTML: " + gpe.toHTML());

		System.out.println("done");
		System.exit(1);
	}
}
