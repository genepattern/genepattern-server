/*
 * PropertiesTableModel.java
 *
 * Created on April 24, 2003, 3:35 PM
 */

package org.genepattern.gpge.ui.propertyviewer;

import java.util.Properties;

/**
 * Thin wrapper for a Properties object.  Thus properties
 * can be displayed in a table, sorted, and optionaly the values can be editted.
 * @author  kohm
 */
public class PropertiesTableModel extends javax.swing.table.AbstractTableModel {
    
    /** Creates a new instance of PropertiesTableModel */
    public PropertiesTableModel(final Properties properties, final boolean is_value_editable) {
        this.properties = properties;
        this.is_value_editable = is_value_editable;
        final int rowCount = properties.size();
        this.row_count = rowCount;
        final String[] keys = new String[rowCount];

        // get the keys 
        properties.keySet().toArray(keys);

        // sort them
        java.util.Arrays.sort(keys);
        this.keys = keys;
    }
    
    public final int getColumnCount() {
        return 2;
    }
    
    public int getRowCount() {
        return row_count;
    }
    
    public final Object getValueAt(int row, int column) {
        final String key = keys[row];
        if( column == 0 )
            return key;
        return properties.get(key);
    }
    /** determines if a cell is editable 
     * only the value column could be
     */
    public final boolean isCellEditable(final int row, final int column){
        return (column == 1 && is_value_editable);
    }
    /** sets the value for the property name at the specified row */
    public final void setValueAt(final Object value, final int row, final int column) {
        properties.setProperty(keys[row], value.toString());
    }
    /** the names of the specified column is returned */
    public final String getColumnName(final int column) {
        return HEADERS[column];
    }
    // fields 
    /** the column headers */
    protected final static String[] HEADERS = new String[] {"Name", "Value"};
    /** the Properties that this is a thin wrapper for */
    private final Properties properties;
    /** the array of keys */
    private final String[] keys;
    /** the row count */
    private final int row_count;
    /** true if the value column is editable */
    private final boolean is_value_editable;
}
