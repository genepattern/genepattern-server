/*
 * WHITEHEAD INSTITUTE
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2001 by the
 * Whitehead Institute for Biomedical Research.  All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever.  The Whitehead Institute can not be responsible for its
 * use, misuse, or functionality.
 */

package org.genepattern.data;

import java.util.ArrayList;
import java.util.List;

import org.genepattern.util.XLogger;

/**
 * Object representing a continous Range of floats..
 * 
 * @author Aravind Subramanian
 * @version %I%, %G%
 */

public class Range {

	private static final XLogger log = XLogger.getLogger(Range.class);

	private float min;

	private float max;

	private int cnt;

	private float fuzz;

	/**
	 * Class constructor
	 * 
	 * @param min
	 *            min value
	 * @param max
	 *            maximum value
	 */
	public Range(float min, float max) {
		this.min = min;
		this.max = max;
		this.cnt = 0;
	}

	/**
	 * Class constructor.
	 * 
	 * @param min
	 *            min value
	 * @param max
	 *            max value
	 * @param fuzz
	 *            the fuzz value
	 */
	public Range(float min, float max, float fuzz) {
		this.min = min - fuzz;
		this.max = max + fuzz;
		this.cnt = 0;
		this.fuzz = fuzz;
	}

	/**
	 * Factory method: for creating an array of <CODE>Range</CODE> objects.
	 * For example createRanges(5, 0, 100, 1) will create 20 <CODE>Range
	 * </CODE> objects 0-4, 5-9, etc al with a fuzz of 1. min and max are
	 * Inclusive
	 * 
	 * @param spread
	 *            the spread for each Range
	 * @param min
	 *            min value
	 * @param max
	 *            max value
	 * @param fuzz
	 *            the fuzz for each <CODE>Range</CODE>
	 * @return Range[] the array of ranges
	 */
	public static Range[] createRanges(final float spread, final float min,
			final float max, final float fuzz) {
		List ranges = new ArrayList();

		for (float i = min; i <= max;) {
			float rmax = i + spread - 1;
			if (rmax > max)
				rmax = max;
			ranges.add(new Range(i, rmax, fuzz));
			i += spread;
		}

		return (Range[]) ranges.toArray(new Range[ranges.size()]);
	}

	/**
	 * Factory method: For example: between 0 and 1000 makes 20 ranges. Need to
	 * calc the range size. Min and max are INclusive
	 * 
	 * @param nranges
	 *            number of ranges
	 * @param min
	 *            min value for first range
	 * @param max
	 *            max value for last range
	 * @param fuzz
	 *            the fuzz for all ranges
	 * @return Range[] and array of ranges
	 */
	public static Range[] createRanges(final int nranges, final float min,
			final float max, final float fuzz) {
		Range[] ranges = new Range[nranges];
		float spread = (max - min) / nranges;

		float runningmin = min;

		for (int i = 0; i < nranges; i++) {
			ranges[i] = new Range(runningmin, runningmin + (float) spread, fuzz);
			runningmin += spread;
		}

		return ranges;
	}

	/**
	 * Factory method: Simply iterates through the values[] array assigning
	 * density number of elements to each Range. The values array is not
	 * processed in any way. Often it makes sense to process -- see matrix.discz
	 * fuzz is not applicable so always 0.
	 * 
	 * @param density
	 *            desnsity values to assign to each <CODE>Range</CODE>
	 * @param values
	 *            used to create each <CODE>Range</CODE>
	 * @return Range[] an array of <CODE>Range</CODE> s
	 */
	public static Range[] createRanges(final int density, final float[] values) {

		// dont -- see below
		//if ( (values.length % density) != 0) throw new
		// IllegalArgumentException("Not exactly divisible, remainder: " +
		// (values.length % density));

		Range[] ranges = new Range[(values.length / density)];

		/*
		 * int cnt = 0; for (int i=0; i < ranges.length; i+= density) { float[]
		 * dest = new float[density]; System.arraycopy(values, i, dest, 0,
		 * density); Vector v = new Vector(dest); ranges[cnt++] = new
		 * Range(v.min(), v.max()); }
		 */

		int startpos = 0;
		for (int i = 0; i < ranges.length; i++) {
			float[] dest = new float[density];
			System.arraycopy(values, startpos, dest, 0, density);
			FloatVector v = new FloatVector(dest);
			ranges[i] = new Range(v.min(), v.max());
			startpos += density;
		}

		// expand the final range to include the last value it not exactly div
		// by
		log.debug("Expanding final range from: "
				+ ranges[ranges.length - 1].max() + " to: "
				+ values[values.length - 1]);
		ranges[ranges.length - 1] = new Range(ranges[ranges.length - 1].min(),
				values[values.length - 1]);

		/*
		 * int cnt = 0; for (int i=0; i < values.length / density; i+= density) {
		 * float[] dest = new float[density]; System.arraycopy(values, i, dest,
		 * 0, density); Vector v = new Vector(dest); ranges[cnt++] = new
		 * Range(v.min(), v.max()); }
		 */

		/*
		 * Makes no sense to do this -- for equal density, must be exactly
		 * divisible dont delete for (int i=0; i < values.length;) { int maxindx =
		 * i + density - 1; if (maxindx > values.length) maxindx =
		 * values.length; Vector vector = new Vector(maxindx - i + 1); // not
		 * always the dnesity size for (int a=0; a < density; a++)
		 */

		return ranges;
	}

	/** increments this range */
	public void increment() {
		cnt++;
	}

	/**
	 * always inclusive
	 * 
	 * @param value
	 *            the value to check
	 * @return true if value is within the range
	 */
	public boolean isMember(final float value) {
		if ((value >= min) && (value <= max))
			return true;
		else
			return false;
	}

	/**
	 * retruns the max value represented by this <CODE>Range</CODE>
	 * 
	 * @return float the max value
	 */
	public float max() {
		return max;
	}

	/**
	 * retruns the in value represented by this <CODE>Range</CODE>
	 * 
	 * @return float the max value
	 */
	public float min() {
		return min;
	}

	/**
	 * The fuzz value
	 * 
	 * @return float the fuzz
	 */
	public float fuzz() {
		return fuzz;
	}

	/**
	 * The spread that this range represents including the fuzz
	 * 
	 * @return float the fuzz value
	 */
	public float spread() {
		return max - min + 1; // because both are inclusive
	}

	/**
	 * @return int the count
	 */
	public int getCount() {
		return cnt;
	}

	/**
	 * the mean of this <CODE>Range</CODE>
	 * 
	 * @return float the mean of this range
	 */
	public float getMean() {
		return ((min + max) / 2);
	}

	/**
	 * The label or name of this range
	 * 
	 * @return String the name of this range
	 */
	public String getRangeName() {
		return new StringBuffer().append(min).append('-').append(max)
				.toString();
	}

	/**
	 * truncates the floats into ints
	 * 
	 * @return String A prettier version of <CODE>getRangeName</CODE>
	 */
	public String getPrettyRangeName() {
		return new StringBuffer().append((int) min).append('-').append(
				(int) max).toString();
	}

	/**
	 * Returns a String representation of this
	 * 
	 * @return String representing this
	 */
	public String toString() {
		return getRangeName();
	}

	/**
	 * Gets the summary
	 * 
	 * @return String the summary
	 */
	public String getSummary() {
		StringBuffer buf = new StringBuffer("");
		buf.append("Data Max: ").append(max);
		buf.append("Data Min: ").append(min);
		buf.append("Spread  : ").append(spread());
		return buf.toString();
	}

} // End Range

