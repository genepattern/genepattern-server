/*
 * DataObjector.java
 *
 * Created on November 20, 2002, 12:17 PM
 */

package org.genepattern.data;

/**
 * All data object interfaces should extend this one. This defines basic methods
 * that all data objects should have
 *
 * @author  kohm
 */
public interface DataObjector {
    /** returns the name of this data object
     * @return String
     */
    public String getName();
    /** returns a DataModel that defines the type of model this implementation represents
     * @return DataModel
     */
    public DataModel getDataModel();
    /** returns false if this is an object that cannot have it's internal state changed
     * @return true if this data object is mutable
     */
     public boolean isMutable();
     /** this is a reminder that data objects must override toString()
      * @return String
      */
     public String toString();
     /** this is a reminder that data objects must override equals(Object)
      * @param obj the other object
      * @return true if this is equal to the other data object
      */
     public boolean equals(Object obj);
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
      * @return int
      */
    public int hashCode();
    
}
