/*
 * SampleLabel.java
 *
 * Created on May 12, 2003, 1:41 PM
 */

package org.genepattern.data;

/**
 * Represents a Sample by name. This class is comparable to others. It also has
 * a special instance that is equal to any other instance. This last attribute
 * is needed because cls files do not specify the samples that they represent.
 * 
 * @author kohm
 */
public class SampleLabel extends AbstractObject implements java.lang.Comparable {

	/** Creates a new instance of SampleLabel */
	private SampleLabel(final String label) {
		super(label);
	}

	/** factory method */
	public static final SampleLabel create(final String label) {
		if (label == null)
			return ANY_SAMPLE;
		final String lab = label.trim();
		if (lab.length() == 0 || lab.equals(ANY_SAMPLE.getName()))
			return ANY_SAMPLE;
		return new SampleLabel(lab);
	}

	/**
	 * returns a DataModel that defines the type of model this implementation
	 * represents
	 */
	public final DataModel getDataModel() {
		return DATA_MODEL;
	}

	/** this is a reminder that data objects must override toString() */
	public final String toString() {
		return getName();
	}

	/** this is a reminder that data objects must override equals(Object) */
	public boolean equals(final Object obj) {
		if (obj instanceof SampleLabel) {
			final SampleLabel sl = (SampleLabel) obj;
			return (sl == ANY_SAMPLE || getName().equals(sl.getName()));
		}
		return false;
	}

	/**
	 * This is a reminder that classes that override equals must also create a
	 * working hash algorithm. 
	 * 
	 * for example:
	 * 
	 * given: boolean b compute (b ? 0 : 1) byte, char, short, or int i compute
	 * (int)i long l compute (int)(l ^ (l >>> 32)) float f compute
	 * Float.floatToIntBits(f) double d compute Double.doubleToLongBits(d) then
	 * compute as long
	 * 
	 * Object just get it's hash or if null then 0
	 * 
	 * Arrays compute for each element
	 * 
	 * i.e.: int result = 17; // prime number result = 37 * result +
	 * (int)character; result = 37 * result + Float.floatToIntBits(f); etc..
	 * return result;
	 */
	public int hashCode() {
		return getName().hashCode() + 17;
	}

	// Comparable interface method signature

	public int compareTo(final Object obj) {
		return getName().compareTo(obj);
	}

	//fields
	/** the DataModel */
	public static final DataModel DATA_MODEL = new DataModel("SampleLabel", 23);

	/** this instance is equal to any other SampleLabel instance */
	public static final SampleLabel ANY_SAMPLE = new SampleLabel("*") {
		/** equal to any other SampleLabel instance */
		public final boolean equals(final Object obj) {
			return (obj instanceof SampleLabel);
		}
	};
}