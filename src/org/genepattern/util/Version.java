/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


/*
 * Version.java
 *
 * Created on October 4, 2002, 9:39 AM
 */

package org.genepattern.util;

import java.text.DateFormat;
import java.util.Calendar;

/**
 * 
 * @author kohm
 */
public class Version {

	/** Creates a new instance of Version */
	public Version(String release, int major, int minor, int revision,
			Calendar date, long build) {
		if (release == null)
			throw new NullPointerException("The release cannot be null");
		if (date == null)
			throw new NullPointerException("The date (Calendar) cannot be null");
		this.release = release;
		this.major = major;
		this.minor = minor;
		this.revision = revision;
		this.date = date;
		this.build = build;
		final DateFormat formater = DateFormat.getDateInstance();
		this.text = release + " " + major + "." + minor + "." + revision
				+ " build=" + build + " on " + formater.format(date.getTime());
	}

	//    /** test it */
	//    public static final void main(String[] args) {
	//        Version cal = new Version("Alpha",1,2,3,Calendar.getInstance(), 1234);
	//        Version other = new Version("Alpha",1,1,0,Calendar.getInstance(), 1234);
	//        
	//        Object cal_ob = cal;
	//        Object other_ob = other;
	//        System.out.println("same Object == Cal? "+cal.equals(cal_ob));
	//        System.out.println("diff Object == Cal? "+cal.equals(other_ob));
	//        System.out.println("same Cal == Cal? "+cal.equals(cal));
	//        System.out.println("diff Cal == Cal? "+cal.equals(other));
	//        System.out.println("cal="+cal);
	//        System.out.println("other cal="+other);
	//    }

	/** determines if another object is the same as this one */
	public final boolean equals(Object other) {
		return (other instanceof Version ? equals((Version) other) : false);
	}

	/** determines if another version is the same as this one */
	public final boolean equals(Version other) {
		if (other != null) {
			return (this.major == other.major && this.minor == other.minor
					&& this.revision == other.revision
					&& this.build == other.build
					&& this.release.equals(other.release) && this.date
					.equals(other.date));
		}
		return false;
	}

	/** returns a String represention of this Version */
	public final String toString() {
		return text;
	}

	// fields
	/** The release which is usually Alpha, Beta, FCS, or Final */
	public final String release;

	/**
	 * the major release number - change here indicating major changes in
	 * functionality or internal design
	 */
	public final int major;

	/**
	 * the minor release number - change here indicates new features as well as
	 * bug fixes and possibly some changes in internal design and implementation
	 */
	public final int minor;

	/** the revision number - change here indicates bug fixes no new features */
	public final int revision;

	/** this is a number that represents the build */
	public final long build;

	/** the date of the build */
	public final Calendar date;

	/** the String representation of this Version */
	public final String text;
}
