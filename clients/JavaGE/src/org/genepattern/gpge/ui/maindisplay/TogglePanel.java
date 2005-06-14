package org.genepattern.gpge.ui.maindisplay;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TogglePanel extends JPanel {
    
    public TogglePanel(String text, JComponent c) {
        setLayout(new BorderLayout());
        ToggleLabel label = new ToggleLabel(text, c);
        add(label, BorderLayout.CENTER);
        add(c, BorderLayout.SOUTH);
    }

    private static class ToggleLabel extends JLabel {
        final Icon collapsedIcon = (Icon) UIManager.get("Tree.collapsedIcon");
        final Icon expandedIcon = (Icon) UIManager.get("Tree.expandedIcon");
        private boolean collapsed;
        private JComponent component;

       
        public ToggleLabel(String text, JComponent c) {
            super(text);
            this.component = c;
            component.setVisible(false);
            int left = this.getIconTextGap() + expandedIcon.getIconWidth();
            component.setBorder(BorderFactory.createEmptyBorder(0, left, 0, 0));
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (collapsed) {
                        setIcon(expandedIcon);
                    } else {
                        setIcon(collapsedIcon);
                    }
                    collapsed = !collapsed;
                    component.setVisible(!collapsed);
                }
            });
            setIcon(collapsedIcon);
            collapsed = true;
        }
    }
}
