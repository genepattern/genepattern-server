package org.genepattern.ui;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.genepattern.data.expr.IExpressionData;

class ExpressionDataTableModel implements TableModel {

	/**  List of listeners */
	EventListenerList listenerList = new EventListenerList();

	/**
	 *  index of 1st data column in table view, 0th column index in table view is
	 *  row name
	 */
	final static int COLUMN_OFFSET = 1;

	/**  whether the data in this expressionData is editable in a table view */
	boolean editable = false;
	IExpressionData expressionData;


	public ExpressionDataTableModel(IExpressionData expressionData) {
		this.expressionData = expressionData;
	}


	/**
	 *  Adds a listener to the list that is notified when a change in the
	 *  expressionData occurs by programatically invoking setValueAt or when the
	 *  user edits a cell in a table view.
	 *
	 * @param  t  the TableModelListener
	 */
	public void addTableModelListener(TableModelListener t) {
		listenerList.add(TableModelListener.class, t);
	}


	/**
	 *  Removes a listener to the list that is notified when a change in this
	 *  expressionData occurs by programatically invoking setValueAt or when the
	 *  user edits a cell in a table view.
	 *
	 * @param  t  the TableModelListener
	 */
	public void removeTableModelListener(TableModelListener t) {
		listenerList.remove(TableModelListener.class, t);
	}



	/**
	 *  Forwards the given notification event to all <code>TableModelListeners</code>
	 *  that registered themselves as listeners for this expressionData.
	 *
	 * @param  e  the event to be forwarded
	 * @see       #addTableModelListener
	 */
	protected void fireTableChanged(TableModelEvent e) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for(int i = listeners.length - 2; i >= 0; i -= 2) {
			if(listeners[i] == TableModelListener.class) {
				((TableModelListener) listeners[i + 1]).tableChanged(e);
			}
		}
	}



	/**
	 *  Sets whether the user can edit data in a table view
	 *
	 * @param  value   The new valueAt value
	 * @param  row     The new valueAt value
	 * @param  column  The new valueAt value
	 */
//	public void setEditable(boolean b) {
//		editable = b;
//	}


	public void setValueAt(Object value, int row, int column) {
//		Double d = (Double) value;
//		expressionData.set(row, column - COLUMN_OFFSET, d.doubleValue());
		//	fireTableChanged(new TableModelEvent(this, row, row, column, TableModelEvent.UPDATE));
	}


	public Class getColumnClass(int column) {
		switch (column) {
			case 0:
				return String.class;
			default:
				return Double.class;
		}
	}


	public int getColumnCount() {
		return expressionData.getColumnCount() + COLUMN_OFFSET;
	}


	public String getColumnName(int column) {
		switch (column) {
			case 0:
				return "Row";
			default:
				return expressionData.getColumnName(column - COLUMN_OFFSET);
		}
	}


	public int getRowCount() {
		return expressionData.getRowCount();
	}


	public Object getValueAt(int row, int column) {
		switch (column) {
			case 0:
				return expressionData.getRowName(row);
			default:
				return new Double(expressionData.getValue(row, column - COLUMN_OFFSET));
		}
	}


	public boolean isCellEditable(int row, int column) {
		if(column > 0) {
			return editable;
		}
		return false;
	}

}

