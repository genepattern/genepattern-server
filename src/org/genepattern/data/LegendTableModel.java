/*
 * LegendTableModel.java
 *
 * Created on January 20, 2003, 2:24 PM
 */

package org.genepattern.data;

/**
 * 
 * @author kohm
 */
public interface LegendTableModel extends javax.swing.table.TableModel {
	/** the description of the column */
	public String getColumnDescription(int columnIndex);

	/** returns true if there are column descriptions */
	public boolean hasColumnDescriptions();
}