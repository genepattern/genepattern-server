/*
 * FileAnnotation.java
 *
 * Created on November 12, 2002, 10:13 PM
 */

package org.genepattern.data.annotation;

/**
 * Datasets that are loaded in as .res or .gct files may have their genes'
 * annotations in the file. This allows the saved annotations to be aquired from
 * the gene accession numbers.
 * 
 * Only requires that the genes and their associated annotations be known at
 * construction time.
 * 
 * @author kohm
 */
public class FileAnnotation implements Annotator {

	/** static initilizer */
	//    static {
	//        /* Thoughts:
	//         * This class can be persisted through XMLEncoder because the property
	//         * names, prop_names, match the variables. Also the order of the
	//         * variables (represented by the Strings) match one of the public
	// constructors
	//         * -- kwo
	//         */
	//        //create instance of abstract class
	//        final java.beans.Encoder encoder = new java.beans.Encoder() {};
	//        final String[] prop_names = new String [] {"genes", "annotations"};
	//        // defines how to create an immutable bean - via its' constructor
	//        encoder.setPersistenceDelegate(FileAnnotation.class,
	//                        new java.beans.DefaultPersistenceDelegate(prop_names));
	//    }
	/** Creates a new instance of FileAnnotation */
	FileAnnotation(final String[] features, final String[] annotations) {
		final int count = features.length;
		capability = FULL_ANNOTATION;
		if (count != annotations.length)
			throw new IllegalArgumentException(
					"The number of features does not"
							+ " equal the number of annotations (" + count
							+ "!=" + annotations.length + ")!");

		// If the initial capacity is greater than the maximum number of entries
		// divided by the load factor, no rehash operations will ever occur.
		// default load factor 0.75 = 3/4
		map = new java.util.HashMap(1 + (count * 4) / 3);
		final String empty = "";
		// map the gene to its' annotation
		// if the annotation is null,
		// then set the value to be "", the empty string
		for (int i = 0; i < count; i++) {
			final String annot = annotations[i];
			final String feature = features[i];
			// cannot have duplicate keys (nor values ??)
			final Object obj = map.put((feature != null) ? feature : empty, // key,
																			// value
					(annot != null) ? annot : empty);
			if (obj != null)
				throw new DuplicateException("Feature " + obj
						+ " has a duplicate!");
		}
	}

	/**
	 * returns the named gene's description based on the current capability
	 * 
	 * @param gene
	 *            the feature whose description is to be gotten
	 * @return String, the description
	 */
	public String getDescription(final String gene) {
		final String annot = (String) map.get(gene);
		return capability.parseAnnotation(annot);
	}

	/**
	 * returns all the named gene's descriptions based on the current capability
	 * 
	 * @param genes
	 *            array of features
	 * @return String[], an array of descriptions
	 */
	public String[] getDescriptions(final String[] genes) {
		final int cnt = genes.length;
		final String[] descs = new String[cnt];
		for (int i = 0; i < cnt; i++) {
			descs[i] = getDescription(genes[i]);
		}
		return descs;
	}

	/**
	 * sets the Capability
	 * 
	 * @return true if the capability was set
	 * @param cap
	 *            the new capability
	 */
	public boolean setCapability(final Capability cap) {
		this.capability = cap;
		// capability is not currently used in determining equality nor hashCode
		//hashcode = 0; // reset
		return true;
	}

	/**
	 * returns a DataModel that defines the type of model this implementation
	 * represents
	 * 
	 * @return DataModel, the data model that this data represents
	 */
	public org.genepattern.data.DataModel getDataModel() {
		return DATA_MODEL;
	}

	/**
	 * gets the String representation of this object
	 * 
	 * @return String, a text representation of this
	 */
	public String toString() {
		return "[FileAnnotation " + this.map.toString() + " genes/annotations "
				+ capability + "]";
	}

	/**
	 * gets the name
	 * 
	 * @return the name of this
	 */
	public String getName() {
		return toString();
	}

	/**
	 * determines if this is equal to another Object
	 * 
	 * @param other
	 *            some other FileAnnotation
	 * @return true if the other FileAnnotatino equals this
	 */
	public final boolean equals(final Object other) {
		// ignore the capability
		return (other instanceof FileAnnotation && ((FileAnnotation) other).map
				.equals(this.map));
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
	 * 
	 * @return int the has code
	 */
	public int hashCode() {
		if (hashcode == 0) {
			int result = 17;
			result = 37 * result + map.hashCode();
			//result = 37 * result + capability.hashCode();
			hashcode = result;
		}
		return hashcode;
	}

	/**
	 * returns false if this is an object that cannot have it's internal state
	 * changed
	 * 
	 * @return true if this is mutable
	 */
	public boolean isMutable() {
		return true;
	}

	// fields
	/** the current capability */
	private Capability capability;

	/** maps gene name to its' annotation */
	private final java.util.Map map;

	/** the hash code value or 0 if not calculated */
	private int hashcode = 0;
}