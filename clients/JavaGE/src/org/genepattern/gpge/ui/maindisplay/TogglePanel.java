package org.genepattern.gpge.ui.maindisplay;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TogglePanel extends JPanel {
    ToggleLabel label;
    JPanel labelPanel;
    
    public TogglePanel(String text, JComponent toggleComponent) {
        setLayout(new BorderLayout());
        label = new ToggleLabel(text, toggleComponent);
        add(label, BorderLayout.NORTH);
        add(toggleComponent, BorderLayout.CENTER);
    }

    public TogglePanel(String text, JComponent rightComponent, JComponent toggleComponent) {
    	 	setLayout(new BorderLayout());
         label = new ToggleLabel(text, toggleComponent);
         labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
         labelPanel.add(label);
         labelPanel.setBackground(getBackground());
         labelPanel.add(rightComponent);
         add(labelPanel, BorderLayout.NORTH);
         add(toggleComponent, BorderLayout.CENTER);
    }
    
    public void setBackground(Color c) {
    		super.setBackground(c);
    		if(labelPanel!=null) {
    			labelPanel.setBackground(c);
    		}
    		
    }
    
    public void setExpanded(boolean b) {
        label.setExpanded(b);
    }
    
    public boolean isExpanded() {
        return label.expanded;
    }
    
    private static class ToggleLabel extends JLabel {
        final Icon collapsedIcon = (Icon) UIManager.get("Tree.collapsedIcon");
        final Icon expandedIcon = (Icon) UIManager.get("Tree.expandedIcon");
        boolean expanded = false;
        private JComponent component;

        public void setExpanded(boolean expanded) {
            this.expanded = expanded;
            if (!expanded) {
                setIcon(collapsedIcon);
            } else {
                setIcon(expandedIcon);
            }
            component.setVisible(expanded);
        }
        public void toggleState() {
            expanded = !expanded;
            setExpanded(expanded);
        }
       
        public ToggleLabel(String text, JComponent c) {
            super(text);
            this.component = c;
            component.setVisible(false);
            int left = this.getIconTextGap() + expandedIcon.getIconWidth();
            component.setBorder(BorderFactory.createEmptyBorder(0, left, 0, 0));
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    toggleState();
                }
            });
            setIcon(collapsedIcon);
        }
    }
}
