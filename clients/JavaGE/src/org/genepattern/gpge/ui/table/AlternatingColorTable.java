package org.genepattern.gpge.ui.table;
import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.TableModel;
import javax.swing.table.TableCellRenderer;

/**
 *  A table that alternates between a white and a light blue
 *  background
 *
 * @author    Joshua Gould
 */
 
public class AlternatingColorTable extends JTable {
	private static final Color ODD_COLOR = new Color(239, 239, 255);
	private static final Color EVEN_COLOR = Color.white;
	
	public AlternatingColorTable(TableModel m) {
		super(m);	
	}
	
	public Component prepareRenderer(TableCellRenderer renderer,
			int row, int column) {
		Object value = getValueAt(row, column);
		boolean isSelected = isCellSelected(row, column);
		boolean rowIsAnchor =
				(selectionModel.getAnchorSelectionIndex() == row);
		boolean colIsAnchor =
				(columnModel.getSelectionModel().getAnchorSelectionIndex() == column);
		boolean hasFocus = (rowIsAnchor && colIsAnchor) && hasFocus();

		Component r = renderer.getTableCellRendererComponent(this,
				value,
				isSelected,
				hasFocus,
				row, column);

		Color odd = ODD_COLOR;
		// make sure you have an odd color
		Color even = EVEN_COLOR;
		// make sure you have an even color

		if (isSelected) {
			// do nothing if selected.
		} else if (hasFocus && isCellEditable(row, column)) {
			// do nothing if we're focused & editting.
		} else if (even.equals(odd)) {
			// do nothing if backgrounds are the same.
		} else if (row % 2 != 0) {
			r.setBackground(odd);
		} else {
			r.setBackground(even);
		}

		return r;
	}

}
