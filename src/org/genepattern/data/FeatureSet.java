/*
 * FeatureSet.java
 *
 * Created on August 7, 2002, 5:01 PM
 */

package org.genepattern.data;

/**
 * A collection of features (columns?)
 * 
 * @author kohm
 */
public class FeatureSet /* implements DataObjector */{
	// fields
	/** the DataModel that should be returned from getDataModel() */
	public static final DataModel DATA_MODEL = new DataModel("FeatureSet", 9);

	// method signature
	/** Creates a new instance of FeatureSet */
	//public FeatureSet(Dataset dataset, WeightVector weights)
	public FeatureSet() {
	}

	public int getSize() {
		return 0;
	}

	public float getWeight(int i) {
		return 0;
	}
}