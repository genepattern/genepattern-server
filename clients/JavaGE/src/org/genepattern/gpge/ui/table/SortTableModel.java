package org.genepattern.gpge.ui.table;

public interface SortTableModel extends
		ColumnSorter {
	public void sortOrderChanged(SortEvent e);
}