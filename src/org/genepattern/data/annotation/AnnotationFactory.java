/*
 * AnnotationFactory.java
 *
 * Created on November 12, 2002, 11:42 PM
 */

package org.genepattern.data.annotation;

import java.util.HashSet;

import org.genepattern.util.ArrayUtils;

/**
 * This class produces instances of Annotator.
 * All datasets should have an instance of this.
 *
 * FIXME there should be a Primary Annotation Factory that creates this one 
 * and creates the Annotation instances, preventing duplicates from being stored.
 * 
 * @author  kohm
 */
public class AnnotationFactory implements Cloneable{
    
//    /** static initilizer */
//    static {
//        /* Thoughts:
//         * This class can be persisted through XMLEncoder because the property
//         * names, prop_names, match the variables.  Also the order of the
//         * variables (represented by the Strings) match one of the public constructors
//         *    -- kwo
//         */
//        //create instance of abstract class
//        final java.beans.Encoder encoder = new java.beans.Encoder() {};
//        final String[] prop_names = new String [] {"annotators"};
//        // defines how to create an immutable bean - via its' constructor
//        encoder.setPersistenceDelegate(AnnotationFactory.class,
//                        new java.beans.DefaultPersistenceDelegate(prop_names));       
//    }
    
    /** Creates a new instance of AnnotationFactory.
     * If the Annotator array is null or has no elements
     * sets the current Annotator to NoAnnotations.  Otherwise it is set
     * to the first element in the Annotator array.
     *
     * Note perhaps "annots" should be a List of Annotator objects, instead of an
     * array, so that the list can be maintained by the PrimeAnnotationFactory
     * (mainly updated with new Annotators as they become available)
     * @param annots array of annotations
     * @param current current annotation
     */
    protected AnnotationFactory(Annotator[] annots, Annotator current) {
        this.annotators = new HashSet();
        if(annots != null && annots.length > 0) {
            ArrayUtils.addToCollection(annots, annotators);
            this.current = (current == null) ? annots[0] : current;
        } else {
            this.current = Annotator.NOANNOTATIONS;
        }
    }
    
    /** returns the current Annotator
     * @return Annotator the current one
     */
    public Annotator getCurrent() {
        return current;
    }
    /** adds an Annotator to the list of Annotators
     * @return true if the Annotator was added; false if it was null or already present
     * @param annot the annotator to add
     */
    public boolean add(Annotator annot) {
        return (annot != null && annotators.add(annot));
    }
    /** adds an Annotator and sets it to be the current one
     * @param annot the annotator to add and set as the current
     * @return true if the annotator was added
     */
    public boolean addAndSet(final Annotator annot) {
        if(annot != null) {
            add(annot);
            current = annot;
            return true;
        }
        return false;
    }
    /** sets the current Annotator
     * Note the Annotator does need to be one of the available Annotators
     * @return true if the Annotator was set; false otherwise
     * @param annot the annotator to set as the current
     */
    public boolean setAnnotator(Annotator annot) {
        if(annot != null) {
            annotators.add(annot);
            return true;
        }
        return false;
    }
    // Cloneable interface method signature
    /** returns a shallow clone
     * @return AnnotationFactory a clone of this one
     */
    public Object clone() {
        final Annotator[] annots = new Annotator[annotators.size()];
        annotators.toArray(annots);
        final AnnotationFactory factory = new AnnotationFactory(annots, current);
        return factory;
    }
    /** returns a String representation of this
     * @return String, a String representation of this
     */
    public String toString() {
        return "[AnnotationFactory current="+getCurrent()+"]";
    }
    // fields
    
    /** the current Annotator */
    private Annotator current;
    /** the list of Annotator instances */
    private java.util.HashSet annotators;
}
