/*
 * AbstractKonstant.java
 *
 * Created on March 27, 2002, 1:36 PM
 */

package org.genepattern.util;

/**
 * Subclasses will create instances of all the konstants for a given set This is
 * better than using just any old object because it is more flexable and less
 * error prone.
 * 
 * @author kohm
 * @version 1.2
 */
public class AbstractKonstant {

	/** Creates new AbstractKonstant */
	protected AbstractKonstant(Object value, int key) {
		this.value = value;
		this.key = key;
	}

	/** returns a string representation of this obect */
	public String toString() {
		return getClass() + " [" + (value != null ? value.toString() : "null")
				+ ", " + key + "]";
	}

	/** determines if the <code>other</code> object is equal to this one */
	public boolean equals(Object other) {
		if (other instanceof AbstractKonstant) {
			return equals((AbstractKonstant) other);
		}
		return false;
	}

	/** faster version of equals assumes <code>other</code> is not null */
	public boolean equals(AbstractKonstant other) {
		return ((key == other.key) && (getClass() == other.getClass()) && (value == value || value != null
				&& value.equals(other.value)));
	}

	// fields
	/** this is the value that helps identify the constant but it can be null */
	public final Object value;

	/** this key helps identify the constant also */
	public final int key;
}