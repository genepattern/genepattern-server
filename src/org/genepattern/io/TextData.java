/*
 * TextData.java
 *
 * Created on April 23, 2003, 1:26 PM
 */

package org.genepattern.io;

import org.genepattern.data.AbstractObject;
import org.genepattern.data.DataModel;

/**
 * 
 * @author kohm
 */
public class TextData extends AbstractObject {

	/** Creates a new instance of TextData */
	public TextData(final String name, final String text) {
		super(name);
		this.text = text;
	}

	/**
	 * returns a DataModel that defines the type of model this implementation
	 * represents
	 */
	public DataModel getDataModel() {
		return DATA_MODEL;
	}

	/** return the text */
	public final String getText() {
		return text;
	}

	/** this is a reminder that data objects must override toString() */
	public String toString() {
		return getName();
	}

	/** this is a reminder that data objects must override equals(Object) */
	public boolean equals(Object obj) {
		if (obj instanceof TextData)
			return text.equals(((TextData) obj).getText());
		return false;
	}

	/**
	 * this is a reminer that classes that override equals must also create a
	 * working hash algorithm. for example:
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
		return text.hashCode();
	}

	//fields
	/** the data model that reps this */
	public static final DataModel DATA_MODEL = new DataModel("Text");

	/** the text */
	private final String text;
}