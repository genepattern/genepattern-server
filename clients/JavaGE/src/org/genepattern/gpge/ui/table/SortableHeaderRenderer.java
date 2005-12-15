/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/


package org.genepattern.gpge.ui.table;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableModel;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 *  Description of the Class
 *
 * @author    Joshua Gould
 */
public class SortableHeaderRenderer implements TableCellRenderer {
   public final static int DESCENDING = -1;

   public final static int NOT_SORTED = 0;

   public final static int ASCENDING = 1;
   private TableCellRenderer tableCellRenderer;
   private JTable table;
   private ColumnSorter model;
   private List sortingColumns = new ArrayList();

   private static Directive EMPTY_DIRECTIVE = new Directive(-1, NOT_SORTED);


   public SortableHeaderRenderer(JTable table, ColumnSorter model) {
      this.table = table;
      this.model = model;
      tableCellRenderer = new JTable().getTableHeader()
            .getDefaultRenderer();
      table.getTableHeader().setDefaultRenderer(this);
      table.getTableHeader().addMouseListener(new MouseHandler());
      
   }


   private void cancelSorting() {
      sortingColumns.clear();
      sortingStatusChanged();
   }


   private void sortingStatusChanged() {
     // clearSortingState();
      //  fireTableDataChanged();
      if(table.getTableHeader()!=null) {
         table.getTableHeader().repaint();
      }
   }


   public void setSortingStatus(int column, int status) {
      Directive directive = getDirective(column);
      if(directive != EMPTY_DIRECTIVE) {
         sortingColumns.remove(directive);
      }
      if(status != NOT_SORTED) {
         sortingColumns.add(new Directive(column, status));
      }
      sortingStatusChanged();
      model.sortOrderChanged(new SortEvent(this, column,
         status != DESCENDING));
   }


   public Component getTableCellRendererComponent(JTable table,
         Object value, boolean isSelected, boolean hasFocus, int row,
         int column) {
      Component c = tableCellRenderer.getTableCellRendererComponent(
            table, value, isSelected, hasFocus, row, column);
      if(c instanceof JLabel) {
         JLabel l = (JLabel) c;
         l.setHorizontalTextPosition(JLabel.LEFT);
         int modelColumn = table.convertColumnIndexToModel(column);
         l.setIcon(getHeaderRendererIcon(modelColumn, l.getFont()
               .getSize()));
      }
      return c;
   }


   public int getSortingStatus(int column) {
      return getDirective(column).direction;
   }


   protected Icon getHeaderRendererIcon(int column, int size) {
      Directive directive = getDirective(column);
      if(directive == EMPTY_DIRECTIVE) {
         return null;
      }
      return new Arrow(directive.direction == DESCENDING, size,
            sortingColumns.indexOf(directive));
   }


   private Directive getDirective(int column) {
      for(int i = 0; i < sortingColumns.size(); i++) {
         Directive directive = (Directive) sortingColumns.get(i);
         if(directive.column == column) {
            return directive;
         }
      }
      return EMPTY_DIRECTIVE;
   }


   private String getStatusString(int status) {
      switch (status) {
       case NOT_SORTED:
          return "NOT_SORTED";
       case ASCENDING:
          return "ASCENDING";
       case DESCENDING:
          return "DESCENDING";
       default:
          return null;
      }
   }



   private static class Directive {
      private int column;

      private int direction;


      public Directive(int column, int direction) {
         this.column = column;
         this.direction = direction;
      }
   }


   private static class Arrow implements Icon {
      private boolean descending;

      private int size;

      private int priority;


      public Arrow(boolean descending, int size, int priority) {
         this.descending = descending;
         this.size = size;
         this.priority = priority;
      }


      public void paintIcon(Component c, Graphics g, int x, int y) {
         Color color = c == null ? Color.GRAY : c.getBackground();
         // In a compound sort, make each succesive triangle 20%
         // smaller than the previous one.
         int dx = (int) (size / 2 * Math.pow(0.8, priority));
         int dy = descending ? dx : -dx;
         // Align icon (roughly) with font baseline.
         y = y + 5 * size / 6 + (descending ? -dy : 0);
         int shift = descending ? 1 : -1;
         g.translate(x, y);

         // Right diagonal.
         g.setColor(color.darker());
         g.drawLine(dx / 2, dy, 0, 0);
         g.drawLine(dx / 2, dy + shift, 0, shift);

         // Left diagonal.
         g.setColor(color.brighter());
         g.drawLine(dx / 2, dy, dx, 0);
         g.drawLine(dx / 2, dy + shift, dx, shift);

         // Horizontal line.
         if(descending) {
            g.setColor(color.darker().darker());
         } else {
            g.setColor(color.brighter().brighter());
         }
         g.drawLine(dx, 0, 0, 0);

         g.setColor(color);
         g.translate(-x, -y);
      }


      public int getIconWidth() {
         return size;
      }


      public int getIconHeight() {
         return size;
      }
   }


   private class MouseHandler extends MouseAdapter {
      public MouseHandler() {
      }


      public void mouseClicked(MouseEvent e) {
         JTableHeader h = (JTableHeader) e.getSource();
         TableColumnModel columnModel = h.getColumnModel();
         int viewColumn = columnModel.getColumnIndexAtX(e.getX());
         int column = columnModel.getColumn(viewColumn).getModelIndex();

         if(column != -1) {
            int status = getSortingStatus(column);
            if(!e.isControlDown()) {
               cancelSorting();
            }

            switch (status) {
             case NOT_SORTED:
                status = ASCENDING;
                break;
             case ASCENDING:
                status = DESCENDING;
                break;
             case DESCENDING:
                status = ASCENDING;
                break;
             default:
                break;
            }
            setSortingStatus(column, status);
         }
      }
   }
}
