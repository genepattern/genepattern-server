/*
 * DataModel.java
 *
 * Created on February 17, 2003, 2:10 PM
 */

package org.genepattern.data;

/**
 * DataModel defines the type of model that a DataObjector implementation is.
 * @author  keith
 */
public class DataModel extends org.genepattern.util.AbstractKonstant {
    
    /** Creates a new instance of DataModel
     * @param value the name of this data model
     * @param id some unique number
     */
    public DataModel(final String value, final int id) {
        super(value.trim(), id);
        final String name = value.trim();
        if( MODEL_LABEL.containsKey(name) ) {
            throw new IllegalArgumentException("cannot have two or more DataModel "
                +"objects with the same label ("+name+")!");
        }
        MODEL_LABEL.put(name, this);
        max_id = Math.max(max_id, id);
    }
    /** Creates a new instance of DataModel defining a unique id
     * @param value the name of this data model
     */
    public DataModel(final String value) {
        this(value, createID());
    }
    
    /** helper for constructor to find a unique id value
     * @return int the next ID value as a power of 2 (i.e. 2, 4, 8, 16, etc)
     */
    protected static final int createID() {
        final int x = (int)(Math.log((double)max_id)/LOG2) + 1;
        return (int)Math.pow(2.0, (double)x);
    }
    
//    public static final void main(String[] args) {
//        
//        for(int i = 1; i < 19; i++) {
//            final int x = (int)(Math.log((double)i)/LOG2) + 1;
//            final int result= (int)Math.pow(2.0, (double)x);
//            System.out.println(i+" next is "+result);
//        }
//    }
    
    /** overriden to only return the value
     * @return String
     */
    public String toString() {
        return (String)value;
    }
    /** returns the DataModel with the same exact name or null if not found
     * @param name the name of the model to search for
     * @return DataModel
     */
    public static final DataModel findModel(final String name) {
        // debug
     /*   System.out.println("Current DataModel keys (query is "+name+"):");
        for(final java.util.Iterator iter = MODEL_LABEL.keySet().iterator(); iter.hasNext(); ) {
            System.out.println(iter.next());
        }
        // end debug
		  */
        return (DataModel)MODEL_LABEL.get(name);
    }
    // fields
    /** the collection of all DataModels */
    private static final java.util.Map MODEL_LABEL = new java.util.HashMap();
    /** indicates an unknown DataModel */
    public static final DataModel UNKNOWN_MODEL = new DataModel("Other", -1);
    /** log base 10 of 2 */
    private static final double LOG2 = Math.log(2.0);
    /** the maximum value of id's */
    private static int max_id = 0;
    
}
