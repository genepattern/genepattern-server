package org.genepattern.gpge.ui.maindisplay;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A panel that allows the user the ability to toggle the of visibility of a
 * group of components
 * 
 * @author Joshua Gould
 * 
 */
public class GroupPanel extends JPanel {

	final Icon collapsedIcon = (Icon) UIManager.get("Tree.collapsedIcon");

	final Icon expandedIcon = (Icon) UIManager.get("Tree.expandedIcon");

	boolean expanded = false;

	private List components = new ArrayList();

	private JLabel majorLabel;

	private JComponent minorComponent;

	public GroupPanel(String major, String minor) {
		this(major, new JLabel(minor));
	}
	public GroupPanel(String major, JComponent minor) {
		setOpaque(false);
		majorLabel = new JLabel(major);
		this.minorComponent = minor;
		FormLayout layout = new FormLayout("pref, 6px, pref", "pref");
		setLayout(layout);
		CellConstraints cc = new CellConstraints();
		add(majorLabel, cc.xy(1, 1));
		add(minorComponent, cc.xy(3, 1));
		Font f = majorLabel.getFont();
		majorLabel.setFont(f.deriveFont(f.getSize2D() + 2));
		majorLabel.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if(e.isPopupTrigger() || e.getModifiers() == MouseEvent.BUTTON3_MASK) {
					return;
				}
				toggleState();
			}
		});
		majorLabel.setIcon(collapsedIcon);
	}
	
	public JLabel getMajorLabel() {
		return majorLabel;
	}

	public void addToggleComponent(Component c) {
		components.add(c);
	}

	public int getToggleComponentCount() {
		return components.size();
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
			majorLabel.setIcon(collapsedIcon);
		} else {
			majorLabel.setIcon(expandedIcon);
		}
		setComponentsVisible(expanded);
	}

	public void toggleState() {
		expanded = !expanded;
		setExpanded(expanded);
	}

	public void setMajorLabelForeground(Color c) {
		majorLabel.setForeground(c);
	}

}
