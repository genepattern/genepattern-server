/*
 * PrimeAnnotationFactory.java
 *
 * Created on November 15, 2002, 3:49 PM
 */

package org.genepattern.data.annotation;

import java.util.ArrayList;
import java.util.List;

/**
 * This factory class creates and keeps track of AnnotationFactory subclasses
 * 
 * @author kohm
 */
public class PrimeAnnotationFactory {

	/**
	 * Creates a new instance of PrimeAnnotationFactory private constructor
	 * keeps this class from being instantiated
	 */
	private PrimeAnnotationFactory() {
	}

	/**
	 * this factory method creates an annotation factory from two arrays of
	 * strings it is most likely to be created from a res or gct file
	 * 
	 * @param names
	 *            the array of features
	 * @param descriptions
	 *            the corresponding array of descriptions
	 * @return AnnotationFactory, the annotation factory that has the names and
	 *         descriptions
	 */
	public static final AnnotationFactory createAnnotationFactory(
			final String[] names, final String[] descriptions) {
		if (descriptions == null || descriptions.length == 0) {
			return EMPTY_ANNOTATION_FACTORY;
		}
		Annotator annot = add(new FileAnnotation(names, descriptions));
		final AnnotationFactory factory = new AnnotationFactory(
				(Annotator[]) annotators.toArray(new Annotator[size()]), annot);
		return factory;
	}

	/**
	 * helper method that only adds the Annotator if it is unique
	 * 
	 * @returns either the specified annotator or it's equivalent if found
	 */
	private static final Annotator add(final Annotator annot) {
		final int index = annotators.indexOf(annot);
		if (index < 0) { // it's unique so add it and return it
			annotators.add(annot);
			return annot;
		} else
			// already have one like it so return the old one
			return (Annotator) annotators.get(index);

	}

	/**
	 * returns the number of Annotator objects
	 * 
	 * @return int the size
	 */
	public static final int size() {
		return annotators.size();
	}

	// fields
	private static final AnnotationFactory EMPTY_ANNOTATION_FACTORY = new AnnotationFactory(
			null, null);

	/** The list of annotators */
	private static List annotators = new ArrayList(10);
}