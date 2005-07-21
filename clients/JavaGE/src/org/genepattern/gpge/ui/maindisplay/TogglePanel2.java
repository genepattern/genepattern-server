package org.genepattern.gpge.ui.maindisplay;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

public class TogglePanel2 extends JLabel {

		final Icon collapsedIcon = (Icon) UIManager.get("Tree.collapsedIcon");

		final Icon expandedIcon = (Icon) UIManager.get("Tree.expandedIcon");

		boolean expanded = false;

		private List components = new ArrayList();

		public void addComponent(Component c) {
			components.add(c);
		}
		
		public boolean isExpanded() {
			return expanded;
		}
		
		private void setComponentsVisible(boolean b) {
			for (int i = 0; i < components.size(); i++) {
				Component c = (Component) components.get(i);
				c.setVisible(b);
			}
		}

		
		public void setExpanded(boolean expanded) {
			this.expanded = expanded;
			if (!expanded) {
				setIcon(collapsedIcon);
			} else {
				setIcon(expandedIcon);
			}
			setComponentsVisible(expanded);
		}

		public void toggleState() {
			expanded = !expanded;
			setExpanded(expanded);
		}

		public TogglePanel2(String text) {
			super(text);
			Font f = getFont();
			setFont(f.deriveFont(f.getSize2D()+2));
			addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					toggleState();
				}
			});
			setIcon(collapsedIcon);
		}
}
