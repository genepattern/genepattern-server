/*
 * FloatsSource.java
 *
 * Created on August 12, 2002, 11:17 AM
 */

package org.genepattern.data;

/**
 * Classes that implement these methods have multiple accessable float values
 * 
 * @author kohm
 */
public interface FloatsSource {
	/** gets the float value at the specified index */
	public float getElement(int i);

	/** gets the number of available floats */
	public int getSize();

}