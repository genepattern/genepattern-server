/*
 * NamesPanel.java
 *
 * Created on November 14, 2002, 3:52 PM
 */

package org.genepattern.data;

import org.genepattern.data.DataModel;

/**
 * The row or column in a dataset including it's description or gene's
 * annotation.
 * 
 * @author kohm
 */
public interface NamesPanel extends org.genepattern.data.DataObjector,
		Cloneable {
	// fields
	/** the DataModel that should be returned from getDataModel() */
	public static final DataModel DATA_MODEL = new DataModel("NamesPanel", 4);

	// method signature
	/**
	 * the number of names
	 * 
	 * @return int
	 */
	public int getCount();

	/**
	 * gets the name at the specified index
	 * 
	 * @param index
	 *            the index to the name
	 * @return String
	 */
	public String getName(final int index);

	/**
	 * gets the names as a List
	 * 
	 * @return List
	 */
	public java.util.List getListOfNames();

	/**
	 * gets the index of the name or -1 if not found
	 * 
	 * @param name
	 *            the name to get the index of
	 * @return int
	 */
	public int getIndex(final String name);

	/**
	 * gets the names as an array
	 * 
	 * @return String[]
	 */
	public String[] getNames();

	/**
	 * gets the annotation or description at the specified index
	 * 
	 * @param index
	 *            the index to the annotation
	 * @return String
	 */
	public String getAnnotation(final int index);

	/**
	 * gets the Annotator
	 * 
	 * @return Annotator
	 */
	public org.genepattern.data.annotation.Annotator getAnnotator();

	/**
	 * Creates a new instance of NamesPanel that is a subset of this one
	 * 
	 * @param indices
	 *            array of indices to keep
	 * @return NamesPanel
	 */
	public NamesPanel createSubSet(final int[] indices);

	/**
	 * creates a shallow clone of this Object
	 * 
	 * @return NamesPanel
	 */
	public Object clone();
}