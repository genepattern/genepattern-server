package org.genepattern.gpge.ui.table;
import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *  A table cell renderer that alternates between a white and a light blue
 *  background
 *
 * @author    Joshua Gould
 */
public class AlternatingColorRenderer extends
      DefaultTableCellRenderer {

   private static Color color = new Color(239, 239, 255);


   public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if(isSelected) {
         return c;
      }
      if((row % 2) == 0) {
         setBackground(Color.white);
      } else {
         setBackground(color);
      }
      return c;
   }

}
