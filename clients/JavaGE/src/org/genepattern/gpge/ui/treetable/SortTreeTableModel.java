package org.genepattern.gpge.ui.treetable;

public interface SortTreeTableModel extends
		org.jdesktop.swing.treetable.TreeTableModel {
	public void sortOrderChanged(SortEvent e);
}