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

import org.genepattern.util.ArrayUtils;
import org.genepattern.util.XLogger;

/**
 * Object representing a continous RangeInt of ints..
 * 
 * @author Aravind Subramanian
 * @version %I%, %G%
 */

public class RangeInt implements java.lang.Comparable {

	/**
	 * Constructs a new RangeInt with fuzz of zero
	 */
	public RangeInt(int min, int max) {
		this(min, max, 0);
	}

	/**
	 * Constructs a new RangeInt with non-negative fuzz note that the min must
	 * be the lesser of specified min and max also the max must be the greater
	 * of the specified min and max
	 */
	public RangeInt(final int min, final int max, int fuzz) {
		if (min > max) // this could be corrected but really something somewhere
					   // went amiss
			throw new IllegalArgumentException("Error: min (" + min
					+ ") is greater than max (" + max + ")!");
		if (fuzz < 0) // positive and negative 'fuzz' mean the same thing
			fuzz = -fuzz;
		this.min = min - fuzz;
		this.max = max + fuzz;
		this.cnt = max - min + 1;
		this.mean = (this.max + this.min) / 2;
		this.fuzz = fuzz;

	}

	/**
	 * Factory method:for creating an array of <CODE>RangeInt</CODE> objects.
	 * For example createRanges(5, 0, 100, 1) does ?? Min and max are INclusive
	 */
	public static RangeInt[] createSpread(final int spread, final int min,
			final int max, final int fuzz) {
		List ranges = new ArrayList();

		for (int i = min; i <= max;) {
			int rmax = i + spread - 1;
			if (rmax > max)
				rmax = max;
			ranges.add(new RangeInt(i, rmax, fuzz));
			i += spread;
		}

		return (RangeInt[]) ranges.toArray(new RangeInt[ranges.size()]);
	}

	/**
	 * Factory method: For example: between 0 and 1000 makes 20 ranges. Need to
	 * calc the range size. Min and max are INclusive
	 */
	public static RangeInt[] createRanges(final int nranges, final int min,
			final int max, final int fuzz) {
		RangeInt[] ranges = new RangeInt[nranges];
		int spread = (max - min) / nranges;

		int runningmin = min;

		for (int i = 0; i < nranges; i++) {
			ranges[i] = new RangeInt(runningmin, runningmin + (int) spread,
					fuzz);
			runningmin += spread;
		}

		return ranges;
	}

	/**
	 * Factory method: Simply iterates through the values[] array assigning
	 * density number of elements to each RangeInt. The values array is not
	 * processed in any way. Often it makes sense to process -- see matrix.discz
	 * fuzz is not applicable so always 0.
	 * 
	 * @throws IllegalArgumentException
	 *             If (values.length % density != 0)
	 */
	public static RangeInt[] createRanges(final int density, final int[] values) {

		// dont -- see below
		//if ( (values.length % density) != 0) throw new
		// IllegalArgumentException("Not exactly divisible, remainder: " +
		// (values.length % density));

		RangeInt[] ranges = new RangeInt[(values.length / density)];

		/*
		 * int cnt = 0; for (int i=0; i < ranges.length; i+= density) { int[]
		 * dest = new int[density]; System.arraycopy(values, i, dest, 0,
		 * density); Vector v = new Vector(dest); ranges[cnt++] = new
		 * RangeInt(v.min(), v.max()); }
		 */
		final int[] dest = new int[density];
		int startpos = 0;
		for (int i = 0; i < ranges.length; i++) {
			System.arraycopy(values, startpos, dest, 0, density);
			ArrayUtils.MinMax mm = ArrayUtils.getMinMax(dest);
			ranges[i] = new RangeInt(mm.min, mm.max);
			startpos += density;
		}

		// expand the final range to include the last value it not exactly div
		// by
		log.debug("Expanding final range from: "
				+ ranges[ranges.length - 1].max() + " to: "
				+ values[values.length - 1]);
		ranges[ranges.length - 1] = new RangeInt(ranges[ranges.length - 1]
				.min(), values[values.length - 1]);

		/*
		 * int cnt = 0; for (int i=0; i < values.length / density; i+= density) {
		 * int[] dest = new int[density]; System.arraycopy(values, i, dest, 0,
		 * density); Vector v = new Vector(dest); ranges[cnt++] = new
		 * RangeInt(v.min(), v.max()); }
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

	/**
	 * always inclusive
	 */
	public boolean isMember(final int value) {
		if ((value >= min) && (value <= max))
			return true;
		else
			return false;
	}

	public int max() {
		return max;
	}

	public int min() {
		return min;
	}

	public int fuzz() {
		return fuzz;
	}

	public int spread() {
		return max - min + 1; // because both are inclusive
	}

	public int getCount() {
		return cnt;
	}

	public int getMean() {
		return mean;
	}

	public String getRangeName() {
		return new StringBuffer().append(min).append('-').append(max)
				.toString();
	}

	// String reps methods
	/**
	 * truncates the floats into ints
	 */
	public String getPrettyRangeName() {
		return new StringBuffer().append((int) min).append('-').append(
				(int) max).toString();
	}

	public String toString() {
		return getRangeName();
	}

	public String getSummary() {
		StringBuffer buf = new StringBuffer("");
		buf.append("Data Max: ").append(max);
		buf.append("Data Min: ").append(min);
		buf.append("Spread  : ").append(spread());
		return buf.toString();
	}

	// other methods

	/** determines if one RangeInt overlaps another */
	public final boolean overlaps(final RangeInt other) {
		final int o_min = other.min(), o_max = other.max();
		// easier to determine if they don't overlap and return the oposite
		return !((o_min > min && o_min > max) || (o_max < min && o_max < max));
	}

	/**
	 * creates a new RangeInt from the min value to the max value using the max
	 * fuzz
	 */
	public static final RangeInt createMerger(final RangeInt r1,
			final RangeInt r2) {
		final int less = Math.min(r1.min(), r2.min());
		final int more = Math.max(r1.max(), r2.max());
		final int fuz = Math.max(r1.fuzz(), r2.fuzz());

		return new RangeInt(less, more, fuz);
	}

	// Comparable interface method signature

	/**
	 * Compares this object with the specified object for order. Returns a
	 * negative integer, zero, or a positive integer as this object is less
	 * than, equal to, or greater than the specified object.
	 * <p>
	 * 
	 * In the foregoing description, the notation <tt>sgn(</tt> <i>expression
	 * </i> <tt>)</tt> designates the mathematical <i>signum </i> function,
	 * which is defined to return one of <tt>-1</tt>,<tt>0</tt>, or
	 * <tt>1</tt> according to whether the value of <i>expression </i> is
	 * negative, zero or positive.
	 * 
	 * The implementor must ensure <tt>sgn(x.compareTo(y)) ==
	 * -sgn(y.compareTo(x))</tt>
	 * for all <tt>x</tt> and <tt>y</tt>. (This implies that
	 * <tt>x.compareTo(y)</tt> must throw an exception iff
	 * <tt>y.compareTo(x)</tt> throws an exception.)
	 * <p>
	 * 
	 * The implementor must also ensure that the relation is transitive:
	 * <tt>(x.compareTo(y)&gt;0 &amp;&amp; y.compareTo(z)&gt;0)</tt> implies
	 * <tt>x.compareTo(z)&gt;0</tt>.
	 * <p>
	 * 
	 * Finally, the implementer must ensure that <tt>x.compareTo(y)==0</tt>
	 * implies that <tt>sgn(x.compareTo(z)) == sgn(y.compareTo(z))</tt>, for
	 * all <tt>z</tt>.
	 * <p>
	 * 
	 * It is strongly recommended, but <i>not </i> strictly required that
	 * <tt>(x.compareTo(y)==0) == (x.equals(y))</tt>. Generally speaking, any
	 * class that implements the <tt>Comparable</tt> interface and violates
	 * this condition should clearly indicate this fact. The recommended
	 * language is "Note: this class has a natural ordering that is inconsistent
	 * with equals."
	 * 
	 * Compares the mins first then if equal compares the maxs.
	 * 
	 * @param o
	 *            the Object to be compared.
	 * @return a negative integer, zero, or a positive integer as this object is
	 *         less than, equal to, or greater than the specified object.
	 * 
	 * @throws ClassCastException
	 *             if the specified object's type prevents it from being
	 *             compared to this Object.
	 *  
	 */
	public final int compareTo(Object o) {
		final RangeInt other = (RangeInt) o;
		final int other_min = other.min;
		if (this.min == other_min) {
			return this.max - other.max;
		}
		return this.min - other_min;
	}

	// fields

	private static final XLogger log = XLogger.getLogger(RangeInt.class);

	private final int min;

	private final int max;

	private final int cnt;

	private final int mean;

	private final int fuzz;

} // End RangeInt

