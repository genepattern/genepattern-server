/*
 * Annotation.java
 *
 * Created on November 12, 2002, 9:49 PM
 */

package org.genepattern.data.annotation;

import org.genepattern.data.DataModel;

/**
 * Classes that implement this interface will know how to 
 * annotate Genes by accession number or by other names
 *
 * @author  kohm
 */
public interface Annotator extends org.genepattern.data.DataObjector {
    /** returns the named gene's description based on the current capability
     * @param gene the feature to get the annotation for
     * @return String, the annotation
     */
    String getDescription(String gene);
    /** returns all the named gene's descriptions based on the current capability
     * @param gene the array of features to get annotations for
     * @return String[], the annotations
     */
    String[] getDescriptions(String[] gene);
    /** sets the Capability
     * @return true if the capability was set
     * @param cap the new capability
     */
    boolean setCapability(Capability cap);
    
//    // methods from Object  A reminder that these should be overridden
// 
//    /** gets the hash */
//    public int hashCode();
//    /** determines if this is equal to another Annotator */
//    public boolean equals(Object other);
    
    // fields
    /** the NoAnnotations Annotator */
    public static final Annotator NOANNOTATIONS = new NoAnnotations();
    /** Just returns the what was input */
    public static final Capability FULL_ANNOTATION = new Capability();
    /** the DataModel that should be returned from getDataModel() */
    public static final DataModel DATA_MODEL = new DataModel("Annotation", 5);
    // I N N E R    C L A S S E S
    
    /**
     * The annotation's description could be the full text or just parts
     * this is the capability
     */
    public static class Capability {
        /**
         * this is where it all happens
         * Takes the raw annotation and parses it to produce a subset of info
         *
         * Note that this class does no parsing - just returns the String
         *
         * @annot the input annotation
         * @returns the parsed string
         */
        final String parseAnnotation(final String annot) {
            return annot;
        }
        /** returns a String representation of this object
         * @return String, a representation of this
         */
        public String toString() {
            return "[Capability: no parsing]";
        }
        /** return the name
         * @return String the name
         */
        public String getName() {
            return toString();
        }
        /** returns the hash code
         * @return int the hash code
         */
        public int hashCode() {
            if(hashcode == 0) {
                hashcode = 17 * 37 + getClass().hashCode();
            }
            return hashcode;
        }
        // fields
        private static int hashcode = 0;
    } // end class Capability
    
    /**
     * This class just returns the empty string. This useful when there are
     * no annotations available;
     */
    static class NoAnnotations implements Annotator{
        
        /** returns the named gene's description based on the current capability
         * which in this case is the empty string ""
         * @param gene the feature to get the description of
         * @return String the description
         */
        public final String getDescription(String gene) {
            return empty;
        }
        /** returns all the named gene's descriptions based on the current capability
         * @param gene the array of features
         * @return String[] the array of descriptions
         */
        public String[] getDescriptions(String[] gene) {
            final String[] strings = new String[gene.length];
            java.util.Arrays.fill(strings, "");
            return strings;
        }
        
        /** sets the Capability
         * @return true if the capability was set
         * @param cap the new capability
         */
        public final boolean setCapability(Capability cap) {
            return false;
        }
        /** returns a DataModel that defines the type of model this implementation represents
         * @return DataModel the data model that this represents
         */
        public org.genepattern.data.DataModel getDataModel() {
            return DATA_MODEL;
        }
        /** determines if this is equal to another Annotator
         * @param other annotator to check
         * @return true if the other annotator is the same as this
         */
        public final boolean equals(final Object other) {
            return other instanceof NoAnnotations;
        }
        /** this is a reminder that data objects must override toString()
         * @return String, a string representation of this
         */
        public String toString() {
            return "[NoAnnotations: empty descriptions]";
        }
        /** this is a reminer that classes that override equals must also
         * create a working hash algorithm.
         * for example:
         *
         * given:
         * boolean b
         *  compute (b ? 0 : 1)
         * byte, char, short, or int i
         *  compute (int)i
         * long l
         *  compute (int)(l ^ (l >>> 32))
         * float f
         *  compute Float.floatToIntBits(f)
         * double d
         *  compute Double.doubleToLongBits(d) then compute as long
         *
         * Object just get it's hash or if null then 0
         *
         * Arrays compute for each element
         *
         * i.e.:
         * int result = 17; // prime number
         * result = 37 * result + (int)character;
         * result = 37 * result + Float.floatToIntBits(f);
         * etc..
         * return result;
         * @return int the hash code
         */
        public int hashCode() {
            return 17 * 37 + empty.hashCode();
        }
        /** this is not mutable
         * @return true if this is mutable
         */
        public final boolean isMutable() {
            return false;
        }
        
        /** returns the name of this data object
         * @return String, the name of this Annotator
         */
        public String getName() {
            return toString();
        }
        
        /** empty string */
        private static final String empty = "";
    } // end class NoAnnotations
}