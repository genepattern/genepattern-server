/*
 * ListTypeAdapter.java
 *
 * Created on March 27, 2003, 1:46 AM
 */

package org.genepattern.gpge.ui.tasks;

import java.awt.event.ActionListener;

/**
 * Two or more way adaptor. Make a JComboBox and JMenu look similar etc...
 * 
 * @author kohm
 */
public interface ListTypeAdapter {
	public void addActionListener(ActionListener listener);

	public void removeActionListener(ActionListener listener);

	public Object getItemSelected();

	public String getSelectedAsString();

	public void removeAll();

	public void remove(Object item);

	public void remove(int index);

	public void addItem(Object item, ActionListener listener);

	public void addItem(Object item);

	//    public void addAll(Object[] items);
	//    public void addAll(List items);
	public void insert(Object item, int index);

	public int getItemCount();

	public boolean isLocalComponent();

	public java.awt.Component getComponent();
}