package org.genepattern.gpge.ui.treetable;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import org.genepattern.webservice.*;
import org.jdesktop.swing.*;
import org.jdesktop.swing.treetable.*;
import org.genepattern.gpge.ui.tasks.*;

/**
 *  Description of the Class
 *
 * @author    Joshua Gould
 */
public class SortableTreeTable extends JXTreeTable implements DragSourceListener, DragGestureListener {
   public final static int DESCENDING = -1;
   public final static int NOT_SORTED = 0;
   public final static int ASCENDING = 1;

   JTree tree;
   private TreePath _pathSource;// The path being dragged
   private BufferedImage _imgGhost;// The 'drag image'
   private Point _ptOffset = new Point();// Where, in the drag image, the mouse was clicked
   private boolean inDrag = false;
   private List sortingColumns = new ArrayList();

   private static Directive EMPTY_DIRECTIVE = new Directive(-1, NOT_SORTED);
   SortTreeTableModel model;
   
   public SortableTreeTable(SortTreeTableModel m) {
      this(m, true);
   }
   
   public SortableTreeTable(SortTreeTableModel m, boolean enableSorting) {
      super(m);
      this.model = m;
      setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION); 
      DragSource dragSource = DragSource.getDefaultDragSource();
      dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, this);
      tree = (JTree) getDefaultRenderer(TreeTableModel.class);
      tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      if(enableSorting) {
         getTableHeader().setDefaultRenderer(
                    new SortableHeaderRenderer(new JTable().getTableHeader().getDefaultRenderer())); // FIXME
                    getTableHeader().addMouseListener(new MouseHandler());
      }
   }


   public final void dragGestureRecognized(final DragGestureEvent e) {
      final Point ptDragOrigin = e.getDragOrigin();
      int row = this.getSelectedRow();

      if(getSelectedColumn() != 0) {
         return;
      }

      final TreePath path = getPathForLocation(ptDragOrigin.x, ptDragOrigin.y);
      if(path == null) {
         return;
      }
      if(isRootPath(path)) {
         return;
      }// Ignore user trying to drag the root node
      Object comp = path.getLastPathComponent();
      if(!tree.getModel().isLeaf(comp)) {
         return;
      }

      // Work out the offset of the drag point from the TreePath bounding rectangle origin
      final Rectangle raPath = tree.getPathBounds(path);
      //_ptOffset.setLocation(ptDragOrigin.x-raPath.x, ptDragOrigin.y-raPath.y);
      _ptOffset.setLocation(raPath.x - ptDragOrigin.x, raPath.y - ptDragOrigin.y);

      // Get the cell renderer (which is a JLabel) for the path being dragged
      final JLabel lbl = (JLabel) tree.getCellRenderer().getTreeCellRendererComponent
            (
            tree, // tree
      path.getLastPathComponent(), // value
      false, // isSelected	(dont want a colored background)
      isExpanded(path), // isExpanded
      tree.getModel().isLeaf(path.getLastPathComponent()), // isLeaf
      0, // row			(not important for rendering)
      false// hasFocus		(dont want a focus rectangle)
      );
      lbl.setSize((int) raPath.getWidth(), (int) raPath.getHeight());// <-- The layout manager would normally do this

      // Get a buffered image of the selection for dragging a ghost image
      _imgGhost = new BufferedImage((int) raPath.getWidth(), (int) raPath.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
      final Graphics2D g2 = _imgGhost.createGraphics();

      // Ask the cell renderer to paint itself into the BufferedImage
      g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 0.5f));// Make the image ghostlike
      lbl.paint(g2);

      // Now paint a gradient UNDER the ghosted JLabel text (but not under the icon if any)
      // Note: this will need tweaking if your icon is not positioned to the left of the text
      final Icon icon = lbl.getIcon();
      final int nStartOfText = (icon == null) ? 0 : icon.getIconWidth() + lbl.getIconTextGap();
      g2.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OVER, 0.5f));// Make the gradient ghostlike
      g2.setPaint(new GradientPaint(nStartOfText, 0, SystemColor.controlShadow,
            getWidth(), 0, new Color(255, 255, 255, 0)));
      g2.fillRect(nStartOfText, 0, getWidth(), _imgGhost.getHeight());

      g2.dispose();

      tree.setSelectionPath(path);// Select this path in the tree
      // Wrap the path being transferred into a Transferable object
      final Transferable transferable = new TransferableTreePath(path);

      // Remember the path being dragged (because if it is being moved, we will have to delete it later)
      _pathSource = path;

      // We pass our drag image just in case it IS supported by the platform
      //e.startDrag(null, _imgGhost, new Point(5,5), transferable, this);
      //e.startDrag(null, _imgGhost, _ptOffset, transferable, this);
      e.startDrag(java.awt.Cursor.getDefaultCursor(),
            _imgGhost, _ptOffset, transferable, this);

   }



   // Interface: DragSourceListener
   public final void dragEnter(DragSourceDragEvent e) {
      inDrag = true;
   }


   public final void dragOver(final DragSourceDragEvent e) {
      inDrag = true;
   }


   public final void dragExit(DragSourceEvent e) {

      inDrag = false;
      java.awt.Window window = javax.swing.SwingUtilities.getWindowAncestor(this);
      if(window != null) {
         window.repaint();
      }
   }


   public final void dropActionChanged(DragSourceDragEvent e) {
   }


   public final void dragDropEnd(final DragSourceDropEvent e) {
      inDrag = false;
      if(e.getDropSuccess()) {
         final int nAction = e.getDropAction();
         if(nAction == DnDConstants.ACTION_MOVE) {// The dragged item (_pathSource) has been inserted at the target selected by the user.
            // Now it is time to delete it from its original location.

            // .
            // .. ask your TreeModel to delete the node
            // .

            _pathSource = null;
         }
      }
   }


   void clearSortingState() { }


   private void cancelSorting() {
      sortingColumns.clear();
      sortingStatusChanged();
   }


   private void sortingStatusChanged() {
      clearSortingState();
      //  fireTableDataChanged();
      if(tableHeader != null) {
         tableHeader.repaint();
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
      model.sortOrderChanged(new SortEvent(this, column, status!=DESCENDING));
      
   }


   public int getSortingStatus(int column) {
      return getDirective(column).direction;
   }


   protected Icon getHeaderRendererIcon(int column, int size) {
      Directive directive = getDirective(column);
      if(directive == EMPTY_DIRECTIVE) {
         return null;
      }
      return new Arrow(directive.direction == DESCENDING, size, sortingColumns.indexOf(directive));
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


   private boolean isRootPath(TreePath path) {
      return tree.isRootVisible() && tree.getRowForPath(path) == 0;
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


   private String getStatusString(int status) {
      switch(status) {
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
   private class MouseHandler extends MouseAdapter {
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
          
            switch(status) {
               case NOT_SORTED:
                  status = DESCENDING;
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
               
            // Cycle the sorting states through {NOT_SORTED, ASCENDING, DESCENDING} or
            // {NOT_SORTED, DESCENDING, ASCENDING} depending on whether shift is pressed.
           // status = status + (e.isShiftDown() ? -1 : 1);
           // status = (status + 4) % 3 - 1;// signed mod, returning {-1, 0, 1}
            setSortingStatus(column, status);
         }
      }
   }


   private class SortableHeaderRenderer implements TableCellRenderer {
      private TableCellRenderer tableCellRenderer;


      public SortableHeaderRenderer(TableCellRenderer tableCellRenderer) {
         this.tableCellRenderer = tableCellRenderer;

      }


      public Component getTableCellRendererComponent(JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) {
         Component c = tableCellRenderer.getTableCellRendererComponent(table,
               value, isSelected, hasFocus, row, column);
         if(c instanceof JLabel) {
            JLabel l = (JLabel) c;
            l.setHorizontalTextPosition(JLabel.LEFT);
            int modelColumn = table.convertColumnIndexToModel(column);
            l.setIcon(getHeaderRendererIcon(modelColumn, l.getFont().getSize()));
         }
         return c;
      }
   }
}
