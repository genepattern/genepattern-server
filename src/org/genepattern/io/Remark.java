/*
 * Remark.java
 *
 * Created on February 4, 2003, 12:02 PM
 */

package org.genepattern.io;

import org.genepattern.data.AbstractObject;
import org.genepattern.data.DataModel;

/**
 * 
 * @author kohm
 */
public class Remark extends AbstractObject {

	/** Creates a new instance of Remark */
	public Remark(final String note, final String next_kw) {
		this(note, next_kw, null);
	}

	/** Creates a new instance of Remark */
	public Remark(final String note, final String next_kw, final String marker) {
		super(removeMarker(note, marker));
		this.next_kw = next_kw;
	}

	// getters

	/** returns the next key word or line from where the remark was found */
	public final String getLocation() {
		return next_kw;
	}

	/** helper for constructor removes the marker from the rest of the remark */
	protected static final String removeMarker(final String note,
			final String marker) {
		if (note == null)
			throw new NullPointerException(
					"The text of the note cannot be null!");
		if (marker == null || marker.length() == 0)
			return note.trim();
		final int where = note.indexOf(marker);
		return note.substring(where).trim();
	}

	// implementation of abstract methods from AbstractObject
	/**
	 * returns a DataModel that defines the type of model this implementation
	 * represents
	 */
	public org.genepattern.data.DataModel getDataModel() {
		return DATA_MODEL;
	}

	/** this is a reminder that data objects must override toString() */
	public String toString() {
		//return "Remark [\""+getName() + "\" Location: \"" +
		// getLocation()+"\"]";
		return getName();
	}

	/** this is a reminder that data objects must override equals(Object) */
	public boolean equals(Object obj) {
		if (!(obj instanceof Remark))
			return false;
		Remark other = (Remark) obj;
		return other.getName().equals(getName())
				&& other.getLocation().equals(getLocation());
	}

	/**
	 * This is a reminer that classes that override equals must also create a
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
		int result = 17; // prime number
		result = 37 * result + calcObjsHash(getName());
		result = 37 * result + calcObjsHash(getLocation());
		return result;
	}

	// fields
	/** the DataModel that should be returned from getDataModel() */
	public static final DataModel DATA_MODEL = new DataModel("Remark", 11);

	/** the next key word or line above which this marker was found */
	private final String next_kw;
}