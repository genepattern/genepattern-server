package org.genepattern.ui;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;

import org.genepattern.data.expr.IExpressionData;
import org.genepattern.data.expr.IResExpressionData;
/**
 *  A JTable for displaying an IExpressionData instance.
 *
 * @author    Joshua Gould
 */
public class ExpressionDataTable extends JTable {
	IExpressionData expressionData;
	final static Color LIGHT_BLUE = new Color(204, 204, 255);


	public ExpressionDataTable(IExpressionData expressionData) {
		super(new ExpressionDataTableModel(expressionData));
		this.expressionData = expressionData;
		setColumnSelectionAllowed(true);
		setGridColor(Color.blue);
		setSelectionBackground(LIGHT_BLUE);
		JTableHeader header = getTableHeader();
		header.setForeground(Color.blue);// set text color
		setSelectionForeground(Color.red);// foreground color for selected cells
		setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		setColumnSelectionAllowed(true);
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		setDefaultRenderer(String.class, getTableHeader().getDefaultRenderer());// make row names look the same as column names
		//setDefaultRenderer(Double.class, new DataRenderer());
	}


	protected JTableHeader createDefaultTableHeader() {
		return
			new JTableHeader(columnModel) {
				public String getToolTipText(java.awt.event.MouseEvent e) {
					Point p = e.getPoint();
					int colIndex = columnAtPoint(p);
					int realColumnIndex = convertColumnIndexToModel(colIndex);
					realColumnIndex--;
					if(realColumnIndex >= 0 && realColumnIndex < expressionData.getColumnCount()) {
						return (String) expressionData.getColumnDescription(realColumnIndex);
					}
					return null;
				}
			};
	}


	public String getToolTipText(MouseEvent e) {
		String tip = null;
		Point p = e.getPoint();
		int rowIndex = rowAtPoint(p);
		int colIndex = columnAtPoint(p);
		int realColumnIndex = convertColumnIndexToModel(colIndex);

		if(realColumnIndex == 0) {
			tip = (String) expressionData.getRowDescription(rowIndex);
		} else if(expressionData instanceof IResExpressionData) {
			IResExpressionData resExpressionData = (IResExpressionData) expressionData;
			realColumnIndex--;
			int call = resExpressionData.getCall(rowIndex, realColumnIndex);
			if(call == IResExpressionData.PRESENT) {
				tip = "P";
			} else if(call == IResExpressionData.ABSENT) {
				tip = "A";
			} else {
				tip = "M";
			}
		}

		return tip;
	}

	/*
	    private class DataRenderer extends DefaultTableCellRenderer {
	    Color absentColor = new Color(230, 230, 230);
	    Color presentColor = Color.white;
	    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
	    int row, int column) {
	    Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	    column = convertColumnIndexToModel(column);
	    column--;
	    if(expressionData.getCall(row, column) == ResExpressionData.PRESENT) {
	    comp.setBackground(presentColor);
	    } else {
	    comp.setBackground(absentColor);
	    }
	    return comp;
	    }
	    }
	 */
}

