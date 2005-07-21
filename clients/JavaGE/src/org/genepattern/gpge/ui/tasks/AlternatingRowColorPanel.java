package org.genepattern.gpge.ui.tasks;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

import com.jgoodies.forms.debug.FormDebugUtils;
import com.jgoodies.forms.layout.FormLayout;


/**
 * A panel that paints grid bounds if and only if the panel's layout manager 
 * is a {@link FormLayout}. You can tweak the debug paint process by setting
 * a custom grid color, painting optional diagonals and painting the grid
 * in the background.<p>
 * 
 * This class is not intended to be extended. However, it is not
 * marked as <code>final</code> to allow users to subclass it for 
 * debugging purposes. In general it is recommended to <em>use</em> JPanel
 * instances, not <em>extend</em> them. You can see this implementation style
 * in the Forms tutorial classes. Rarely there's a need to extend JPanel; 
 * for example if you provide a custom behavior for 
 * <code>#paintComponent</code> or <code>#updateUI</code>.  
 *
 * @author  Karsten Lentzsch
 * @version $Revision$
 * 
 * @see     FormDebugUtils
 */
public class AlternatingRowColorPanel extends JPanel {
    /** 
     * The default color used to paint the form's debug grid. 
     */
    private static final Color DEFAULT_GRID_COLOR = new Color(239, 239, 255);

    /**
     * Holds the color used to paint the debug grid.
     */
    private Color gridColor = DEFAULT_GRID_COLOR;

    
   
    /**
     * Constructs a FormDebugPanel on the given FormLayout instance 
     * that paints the grid in the foreground and paints no diagonals.
     * 
     * @param layout  the panel's FormLayout instance 
     */
    public AlternatingRowColorPanel(FormLayout layout) {
    		setLayout(layout);
    }




    /**
     * Sets the debug grid's color.
     * 
     * @param color  the color used to paint the debug grid
     */
    public void setGridColor(Color color) { 
        gridColor = color; 
    }


    // Painting *************************************************************

    /**
     * Paints the component and - if background painting is enabled - the grid
     * 
     * @param g   the Graphics object to paint on 
     */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        paintGrid(g);
    }
    
    
    /**
     * Paints the form's grid lines and diagonals.
     * 
     * @param g    the Graphics object used to paint
     */
    private void paintGrid(Graphics g) {
        if (!(getLayout() instanceof FormLayout)) {
            return;
        }
        FormLayout.LayoutInfo layoutInfo = FormDebugUtils.getLayoutInfo(this);
        int left   = layoutInfo.getX();
        int top    = layoutInfo.getY();
        int width  = layoutInfo.getWidth();
        int height = layoutInfo.getHeight();

        int gridHeight = height/layoutInfo.rowOrigins.length;
        g.setColor(gridColor);
        // Paint the row bounds.
        for (int row = 0; row < layoutInfo.rowOrigins.length; row+=2) {
            g.fillRect(left, layoutInfo.rowOrigins[row], width, gridHeight);
        }

    }
    
    
}