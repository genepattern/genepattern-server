/*
 * FeaturesetProperties.java
 *
 * Created on January 17, 2003, 2:45 PM
 */

package org.genepattern.data;

import java.util.Map;

/**
 * FeaturesetProperties defines a way to implement a class that has a list of 
 * genes and the individual genes' properties.  This could also define a list
 * of samples and the individual samples' properties.
 *
 * @author  kohm
 */
public interface FeaturesetProperties extends org.genepattern.data.LegendTableModel, DataObjector {
    /** the String representations of the supported column types */
    public static final String[] SUPPORTED_TYPES =
        new String[] {"int", "float", "boolean", "String"};
    /** array of supported classes that coorespond to the SUPPORTED_TYPES */
    public static final Class[] SUPPORTED_CLASSES = 
        new Class[] {Integer.TYPE, Float.TYPE, Boolean.TYPE, String.class};
    // fields
    /** the DataModel that should be returned from getDataModel() */
    public static final DataModel DATA_MODEL = new DataModel("FeaturesetProperties");

    // method signature
    /** the misc other properties of the specific implementation model
     * @return Map
     */
    public Map getAttributes();
    /** returns the Model that this data object represents
     * @return String
     */
    public String getModel();
    /** sets the array values to those of the Column Names
     * if the specified array is null creates a new String[]
     * @param array an array to set or null
     * @return String[]
     */
    public String[] getColumnNames(final String[] array);
    /** sets the specified array values to those of the Column Descriptions
     * if the specified array is null creates a new String[]
     * @param array an array to set or null
     * @return String[]
     */
    public String[] getColumnDescriptions(final String[] array);
    /** returns true if there are row names specified
     * @return boolean
     */
    public boolean hasRowNames();
    /** gets the name of the specified row if none then returns null
     * @param row the index of the row
     * @return String
     */
    public String getRowName(int row);
    /** sets the array values to those of the Row Names
     * if the specified array is null creates a new String[]
     * @param array the array to set or null
     * @return String[]
     */
    public String[] getRowNames(final String[] array);
    /** returns true if there are row descriptions specified
     * @return boolean
     */
    public boolean hasRowDescriptions();
    /** gets the specified row description if none then returns null
     * @param row the index to the row
     * @return String
     */
    public String getRowDescription(int row);
    /** sets the array values to those of the Row Descriptions
     * if the specified array is null creates a new String[]
     * @param array the array to set or null
     * @return String[]
     */
    public String[] getRowDescriptions(final String[] array);
    
    //fields
    /** The index of the column where row descriptions are */
    public static final String KW_ROW_DESCRIPTIONS_COLUMN = "RowDescriptionsColumn=" ;
    /** The index of the column where row names are */
    public static final String KW_ROW_NAMES_COLUMN = "RowNamesColumn=";
    
    // I N N E R   C L A S S E S
    
    /** Proxy for a primitive */
    public interface SimpleObject {
        /** returns an object that represents the value at the index */
        public Object get(int r);
        /** returns a String representation of the value */
        public String toString();
    }
    abstract class AbstractSimpleObject implements SimpleObject {
        protected AbstractSimpleObject(final int column) {
            this.column = column;
        }
        /** returns an object that represents the value at the row */
        abstract public Object get(int r);
        /** returns a String representation of the value */
        abstract public String toString();
        // fields
        protected final int column;
    }
    

}
