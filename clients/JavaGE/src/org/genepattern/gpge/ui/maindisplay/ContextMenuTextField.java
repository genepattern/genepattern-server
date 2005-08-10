package org.genepattern.gpge.ui.maindisplay;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

/**
 * A text field that has a popup menu with Cut, Copy, and Paste actions
 * 
 * @author Joshua Gould
 * 
 */
public class ContextMenuTextField extends JTextField {
	protected JPopupMenu popupMenu;

	protected JMenuItem cutItem;

	protected JMenuItem copyItem;

	protected JMenuItem pasteItem;

	public ContextMenuTextField(int columns) {
		this(null, columns);
	}

	public ContextMenuTextField(String text, int columns) {
		super(text, columns);
		popupMenu = createPopupMenu();
	}

	public void processMouseEvent(MouseEvent e) {
		if (e.isPopupTrigger()) {
			showPopup(e);
		} else {
			super.processMouseEvent(e);
		}
	}

	protected void showPopup(MouseEvent e) {
		boolean enabledCopyCut = getSelectedText() != null;
		copyItem.setEnabled(enabledCopyCut);
		cutItem.setEnabled(enabledCopyCut);
		pasteItem.setEnabled(getToolkit().getSystemClipboard()
				.getContents(null) != null);
		popupMenu.show(e.getComponent(), e.getX(), e.getY());
	}

	protected JPopupMenu createPopupMenu() {
		JPopupMenu popupMenu = new JPopupMenu();
		cutItem = new JMenuItem("Cut");
		copyItem = new JMenuItem("Copy");
		pasteItem = new JMenuItem("Paste");
		ActionListener listener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Object source = e.getSource();
				if (source == cutItem) {
					cut();
				} else if (source == copyItem) {
					copy();
				} else {
					paste();
				}
			}

		};
		cutItem.addActionListener(listener);
		copyItem.addActionListener(listener);
		pasteItem.addActionListener(listener);
		popupMenu.add(cutItem);
		popupMenu.add(copyItem);
		popupMenu.add(pasteItem);
		return popupMenu;
	}

}
