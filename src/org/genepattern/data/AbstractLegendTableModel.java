/*
 * LegendTableModel.java
 *
 * Created on February 4, 2002, 2:24 PM
 */

package org.genepattern.data;

/**
 * This class is an AbstractTableModel subclass that defines the data that
 * LegendTable needs to display tool tips:  Column descriptions for the table
 * cells; column names for either clarifying the names of the individual column 
 * table headers, adding additional information,
 * or the long form of the header labels for example.
 *
 * @author  KOhm
 * @version 0.1
 * @see LegendTable
 */
public abstract class AbstractLegendTableModel extends javax.swing.table.AbstractTableModel implements LegendTableModel {

    /** Creates new LegendTableModel */
    public AbstractLegendTableModel() {
    }
    
    /** subclasses need to implement-returns an array of descriptions */
    abstract public String[] getColumnDescriptions();
    /** returns an array of Strings the names of the columns */
    abstract public String[] getColumnNames();
    /** the description of the column */
    public String getColumnDescription(int columnIndex) {
        return getColumnDescriptions()[columnIndex];
    }
    /** returns true if there are column descriptions */
    public boolean hasColumnDescriptions() {
        return (getColumnDescriptions() != null);
    }
    // methods defined by the TableModel interface
    abstract public int getColumnCount ();
    
    abstract public int getRowCount ();

    abstract public java.lang.Object getValueAt (int r, int c);
    
}
