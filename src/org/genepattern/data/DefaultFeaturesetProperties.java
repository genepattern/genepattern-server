/*
 * DefaultFeaturesetProperties.java
 *
 * Created on January 17, 2003, 2:51 PM
 */

package org.genepattern.data;

import java.util.Map;
import java.util.Collections;

import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.*;

import org.genepattern.util.ArrayOArrays;
import org.genepattern.util.ArrayUtils;

//import org.genepattern.data.DataModel;
//import org.genepattern.io.encoder.FeaturesetPropertiesEncoder;

/**
 * Basic data object that can be saved to or loaded from an input stream.
 * @author  kohm
 */
public class DefaultFeaturesetProperties extends AbstractObject implements FeaturesetProperties {
     
    /** Creates a new instance of DefaultFeaturesetProperties.
     * Note the column classes if representing a primitive should use the class 
     * defined in the corresponding wrapper class TYPE variable (i.e. float
     * should be represented by Float.TYPE not Float.class same goes for the
     * other primitives - int should have class Integer.TYPE etc.)
     */
    public DefaultFeaturesetProperties(final String name, final String model, final String[] column_names, final String[] column_descriptions, final String[] row_names, final String[] row_descs, final Class[] column_classes, final ArrayOArrays arrays, final Map attributes) {
        super(name, false);
//        if( name == null )
//            throw new NullPointerException("The name of this object cannot be null!");
//        this.name = name.trim();
        if( model == null )
            throw new NullPointerException("The model name cannot be null!");
        this.data_model = model.trim();
        
        if( column_classes == null )
            throw new NullPointerException("column classes array cannot be null!");
        final int cnt = column_classes.length;
        this.column_count = cnt;
        final boolean check_cnt = (cnt > 0);
        if( column_names == null ) 
            throw new NullPointerException("Column names array cannot be null!");
        if(check_cnt && column_names.length != cnt) {
            throw new ArrayIndexOutOfBoundsException(
                "Column names array wrong size "
                +column_names.length+" != "+cnt);
        }
        
        if(check_cnt && column_descriptions != null && column_descriptions.length != cnt) {
            throw new ArrayIndexOutOfBoundsException(
                "Column descriptions array wrong size "
                +column_descriptions.length+" != "+cnt);
        }
        
//        if(column_classes.length != cnt) {
//            throw new ArrayIndexOutOfBoundsException(
//                "Column classes array wrong size "
//                +column_classes.length+" != "+cnt);
//        }
        if( arrays == null )
            throw new NullPointerException("ArrayOArrays cannot be null!");
        if(arrays.getArrayCount() != cnt) {
            throw new ArrayIndexOutOfBoundsException(
                "Number of Column arrays should be "+ cnt
                +" not "+arrays.getArrayCount());
        }
        this.column_descriptions = (ArrayUtils.arrayIsEmpty(column_descriptions)) ?
            null : column_descriptions;
        this.row_count = arrays.getDepth();
        this.column_names = (String[])column_names.clone();
        this.column_classes = column_classes;
        this.simple_objects = new SimpleObject[cnt];
        this.column_data = new Object[cnt];
        this.attributes = (attributes != null) ? new java.util.HashMap(attributes) :
            new java.util.HashMap(1);
        
        final String rnc = getToken(KW_ROW_NAMES_COLUMN);
        String value = (String)attributes.get(rnc);
        String[] r_names = null;
        //int skip_names = -1;
        if( value != null) {
            try {
                int col = Integer.parseInt(value);
                //skip_names = col;
                r_names = (String[])arrays.getArray(col);
            } catch (NumberFormatException ex) {
                System.err.println("Couldn't parse the row_names_column value: "+ex);
            }
        }
        if( r_names == null )
            r_names = (ArrayUtils.arrayIsEmpty(row_names)) ? null : (String[])row_names.clone();
        this.row_names = r_names;
        
        final String rnd = getToken(KW_ROW_DESCRIPTIONS_COLUMN);
        value = (String)attributes.get(rnd);
        String[] r_descrs = null;
        //int skip_descrs = -1;
        if( value != null) {
            try {
                final int col = Integer.parseInt(value);
                //skip_descrs = col;
                r_descrs = (String[])arrays.getArray(col);
            } catch (NumberFormatException ex) {
                System.err.println("Couldn't parse the row_descriptions_column value: "+ex);
            }
        }
        if( r_descrs == null )
            r_descrs = (ArrayUtils.arrayIsEmpty(row_descs)) ? null : (String[])row_descs.clone();
        this.row_descriptions = r_descrs;
//        if( row_names != null )
//            row_count = row_names.length;
//        else if( row_descs != null ) // thus row_names is null
//            row_count = row_descs.length;
//        if(row_descs != null && row_names != null && row_names.length != row_descs.length
//        && row_names.length > 0 && row_descs.length > 0) {
//            throw new ArrayIndexOutOfBoundsException(
//                "Number of Row labels and descriptions are not of equal length!\n"
//                +"labels="+row_names.length+" descriptions="+row_descs.length);
//        }
        
        // add the arrays
        for(int i = 0; i < cnt; i++) {
            //if( i == skip_names || i == skip_descrs )
            //    continue;
            column_data[i] = arrays.getArray(i);
        }
        
        // add the SimpleObject instances
        for(int i = 0; i < cnt; i++) {
            final Class clss = column_classes[i];
            //FIXME use a Map to get from Class to a SimpleObject instance
            if(clss.equals(Integer.class)) {
                simple_objects[i] = new SimpleInteger(i);
            } else if(clss.equals(Float.TYPE)) {
                simple_objects[i] = new SimpleFloat(i);
            } else if(clss.equals(Boolean.TYPE)) {
                simple_objects[i] = new SimpleBoolean(i);
            } else if(clss.equals(String.class)) {
                simple_objects[i] = new SimpleString(i);
            } else {
                throw new IllegalArgumentException("The class, "+clss
                    +", is not supported as a type!");
            }
      }
    }
    /** Creates a new instance of DefaultFeaturesetProperties
     * This has no body or main table of data.
     */
    public DefaultFeaturesetProperties(final String name, final String model, final String[] col_names, final String[] col_descs, final String[] row_names, final String[] row_descs, final Map attributes) {
        this(name, model, col_names, col_descs, row_names, row_descs, new Class[0], new ArrayOArrays(0, new Class[0]), attributes);
    }
    
    // helpers
    
    /** returns the token from the keyword */
    protected static final String getToken(final String kw) {
        final int len = kw.length() - 1;
        return kw.substring(0, len);
    }
    
    // FeaturesetProperties interface method signature
    
    /** Adds a listener to the list that is notified each time a change
     * to the data model occurs. Not implemented since no TableModelListener
     * objects are stored.
     *
     * @param	l		the TableModelListener
     *
     */
    public void addTableModelListener(TableModelListener l) {
    }
    
    /** Returns the most specific superclass for all the cell values
     * in the column.  This is used by the <code>JTable</code> to set up a
     * default renderer and editor for the column.
     *
     * @param columnIndex  the index of the column
     * @return the common ancestor class of the object values in the model.
     *
     */
    public Class getColumnClass(int columnIndex) {
        return column_classes[columnIndex];
    }
    
    /** Returns the number of columns in the model. A
     * <code>JTable</code> uses this method to determine how many columns it
     * should create and display by default.
     *
     * @return the number of columns in the model
     * @see #getRowCount
     *
     */
    public int getColumnCount() {
        return column_count;
    }
    
    /** Returns the name of the column at <code>columnIndex</code>.  This is used
     * to initialize the table's column header name.  Note: this name does
     * not need to be unique; two columns in a table can have the same name.
     *
     * @param	columnIndex	the index of the column
     * @return  the name of the column
     *
     */
    public String getColumnName(int columnIndex) {
        return column_names[columnIndex];
    }
    /** sets the array values to those of the Column Names */
    public String[] getColumnNames(final String[] array) {
        if( column_names == null )
            return array;
        return AbstractObject.arrayCopy(column_names, array);
    }
    
    /** Returns the number of rows in the model. A
     * <code>JTable</code> uses this method to determine how many rows it
     * should display.  This method should be quick, as it
     * is called frequently during rendering.
     *
     * @return the number of rows in the model
     * @see #getColumnCount
     *
     */
    public int getRowCount() {
        return row_count;
    }
    
    /** Returns the value for the cell at <code>columnIndex</code> and
     * <code>rowIndex</code>.  For columns that store primitives, a reused simple
     * object will be returned. The SimpleObject will contain the value of the
     * primitive that is stored at that row.
     *
     * @param	rowIndex	the row whose value is to be queried
     * @param	columnIndex 	the column whose value is to be queried
     * @return	the value Object at the specified cell
     * 
     */
    public Object getValueAt(final int rowIndex, final int columnIndex) {
//        final Object obj = simple_objects[columnIndex].get(rowIndex);
//        System.out.println(rowIndex+","+columnIndex+"="+obj);
//        return obj;
        return simple_objects[columnIndex].get(rowIndex);
    }
    
    /** Returns true if the cell at <code>rowIndex</code> and
     * <code>columnIndex</code>
     * is editable.  Otherwise, <code>setValueAt</code> on the cell will not
     * change the value of that cell.
     *
     * @param	rowIndex	the row whose value to be queried
     * @param	columnIndex	the column whose value to be queried
     * @return	true if the cell is editable
     * @see #setValueAt
     *
     */
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
    
    /** Removes a listener from the list that is notified each time a
     * change to the data model occurs.  Note there will be no changes to
     * the data since this is an immutable data class thus no listeners will ever
     * be notified of anything.
     *
     * @param	l		the TableModelListener
     *
     */
    public void removeTableModelListener(TableModelListener l) {
    }
    
    /** Sets the value in the cell at <code>columnIndex</code> and
     * <code>rowIndex</code> to <code>aValue</code>.
     *
     * @param	aValue		 the new value
     * @param	rowIndex	 the row whose value is to be changed
     * @param	columnIndex 	 the column whose value is to be changed
     * @see #getValueAt
     * @see #isCellEditable
     *
     */
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new UnsupportedOperationException("Cannot set the value");
    }
    
    // DataObjector interface signature methods
    //FIXME these methods are not fully implemented
    /** returns a DataModel that defines the type of model this implementation represents */
    public org.genepattern.data.DataModel getDataModel() {
        return DATA_MODEL;
    }
//    /** returns the name of this data object */
//    public String getName() {
//        return name;
//    }
//    /** returns false if this is an object that cannot have it's internal state changed */
//    public boolean isMutable() {
//        return false;
//    }
    /** dumps the data */
    public void dumpTo(final java.io.PrintStream out) throws java.io.IOException {
        out.println(toString());
        final char delim = '\t';
        final int limit = getColumnCount();
        for(int i = 0; i < limit; i++) { // column_names
            out.print(column_names[i]);
            out.print(delim);
        }
        out.println();
        for(int i = 0; i < limit; i++) { // column_descriptions
            out.print(column_descriptions[i]);
            out.print(delim);
        }
        out.println();
        for(int i = 0; i < limit; i++) { // column_classes
            out.print(column_classes[i]);
            out.print(delim);
        }
        out.println();
        
        final int cnt = getRowCount();
        for(int j = 0; j < cnt; j++) { // data
            for(int i = 0; i < limit; i++) {
                out.print(getValueAt(j, i));
                out.print(delim);
            }
            out.println();
        }
    }
    /** this is a reminder that data objects must override toString() */
    public String toString() {
        return "DefaultFeaturesetProperties "+getName()+" "+row_count+"x"+column_count;
    }
    /** this is a reminder that data objects must override equals(Object) */
    public boolean equals(Object obj) {
        if(!(obj instanceof FeaturesetProperties))
            return false;
        final FeaturesetProperties other = (FeaturesetProperties)obj;
        return row_count == other.getRowCount() && column_count == other.getColumnCount();
        //FIXME
            //&& column_classes.equals(other.column_classes) && column_names.equals(other.column_names)
            //&& column_data.equals(other.column_data);
    }
        
    /**
     * this is a reminer that classes that override equals must also 
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
     */
    public int hashCode() {
        if( hash_code == 0 ) {
            int result = 17; // prime number
            result = 37 * result + column_count;
            result = 37 * result + row_count;
            result = 37 * result + calcObjsHash(column_classes);
            result = 37 * result + data_model.hashCode();
            result = 37 * result + calcObjsHash(column_data);
            result = 37 * result + calcObjsHash(column_descriptions);
            result = 37 * result + calcObjsHash(column_names);
            result = 37 * result + calcObjsHash(row_descriptions);
            result = 37 * result + calcObjsHash(row_names);
            result = 37 * result + attributes.hashCode();
            hash_code = result;
        }

        return hash_code;
    }
    // other methods
    /** the description of the column  */
    public String getColumnDescription(int columnIndex) {
        return column_descriptions[columnIndex];
    }
    /** sets the array values to those of the Column Descriptions */
    public String[] getColumnDescriptions(final String[] array) {
        if( column_descriptions == null )
            return array;
        return AbstractObject.arrayCopy(column_descriptions, array);
    }
    
    /** returns true if there are column descriptions  */
    public boolean hasColumnDescriptions() {
        return (column_descriptions != null);
    }
    
    /** the misc other properties of the specific implementation model  */
    public Map getAttributes() {
        if(attributes_view == null)
            attributes_view = Collections.unmodifiableMap(attributes);
        return attributes_view;
    }
    
    /** returns true if there are row names specified  */
    public boolean hasRowNames() {
        return (row_names != null);
    }
    
    /** returns true if there are row descriptions specified  */
    public boolean hasRowDescriptions() {
        return (row_descriptions != null);
    }
    
    /** gets the name of the specified row  */
    public String getRowName(final int row) {
        return row_names[row];
    }
    /** sets the array values to those of the Row Names */
    public String[] getRowNames(final String[] array) {
        if( row_names == null )
            return array;
        return AbstractObject.arrayCopy(row_names, array);
    }
    
    /** gets the specified row description if none then returns null  */
    public String getRowDescription(final int row) {
        return row_descriptions[row];
    }
    /** sets the array values to those of the Row Descriptions
     * if the specified array is null creates a new String[]
     */
    public String[] getRowDescriptions(final String[] array) {
        if( row_descriptions == null )
            return array;
        return AbstractObject.arrayCopy(row_descriptions, array);
    }
    /** returns the Model that this data object represents  */
    public String getModel() {
        return this.data_model;
    }
    
    //fields
    /** each element is an array of some supported type
     * i.e. int[] String[] boolean[] float[] */ 
    private final Object[] column_data;
    /** the column names or labels */
    private final String[] column_names;
    /** the descriptions of each column */
    private final String[] column_descriptions;
    /** the Class that defines the type for the column of the same index */
    private final Class[] column_classes;
    /** one SimpleObject for each column this is returned by getValueAt(r,c) as a
     * wrapper for primitives
     */
    private final SimpleObject[] simple_objects;
    /** the index of which column has the row descriptions or -1 if none*/
    //private final int row_descr_column;
    /** the row descriptions or null if none */
    private final String[] row_descriptions;
    /** the row names or null if none */
    private final String[] row_names;
    /** the index of which column has the row names or -1 if none*/
    //private final int row_names_column;
    /** the number of rows */
    private final int row_count;
    /** the number of columns */
    private final int column_count;
    /** the other properties */
    private final Map attributes;
    /** the unmodifiable view of the attributes */
    private Map attributes_view;
    /** the model that this data object represents */
    private String data_model;
    /** the hash code or 0 if not calculated */
    private int hash_code = 0;
    
    // I N N E R   C L A S S E S
    
    public class SimpleInteger extends AbstractSimpleObject{
        SimpleInteger(final int column) { super(column); }
        /** returns an object that represents the value at the index  */
        public Object get(int i) {
            this.integer = ((int[])column_data[column])[i];
            return this;
        }
        /** returns a String representation of the value */
        public String toString() {
            return String.valueOf(integer);
        }
        // fields
        /** the int value */
        private int integer;
    }
    public class SimpleFloat extends AbstractSimpleObject{
        SimpleFloat(final int column) { super(column); }
        /** returns an object that represents the value at the index  */
        public final Object get(int i) {
            this.value = ((float [])column_data[column])[i];
            return this;
        }
        /** returns a String representation of the value */
        public final String toString() {
            return String.valueOf(value);
        }
        /** gets the value*/
        public final float getFloat() {
            return value;
        }
        // fields
        /** the float value */
        private float value;
    }
    public class SimpleBoolean extends AbstractSimpleObject{
        SimpleBoolean(final int column) { super(column); }
        /** returns an object that represents the value at the index  */
        public Object get(int i) {
            this.value = ((boolean [])column_data[column])[i];
            return this;
        }
        /** returns a String representation of the value */
        public String toString() {
            return (value) ? TRUE : FALSE;
        }
        // fields
        /** the float value */
        private boolean value;
        /** the true string */
        private final String TRUE  = Boolean.TRUE.toString();
        /** the false String */
        private final String FALSE = Boolean.FALSE.toString();
    }
    public class SimpleString extends AbstractSimpleObject{
        SimpleString(final int column) { super(column); }
        /** returns the String at the index  */
        public Object get(int i) {
            return ((String [])column_data[column])[i];
            
        }
        public String toString() {
            return null;
        }
    }
    
}
