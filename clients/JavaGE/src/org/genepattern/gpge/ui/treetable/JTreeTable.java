/*
    @(#)JTreeTable.java	1.2 98/10/27
    Copyright 1997, 1998 by Sun Microsystems, Inc.,
    901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
    All rights reserved.
    This software is the confidential and proprietary information
    of Sun Microsystems, Inc. ("Confidential Information").  You
    shall not disclose such Confidential Information and shall use
    it only in accordance with the terms of the license agreement
    you entered into with Sun.
  */
  
package org.genepattern.gpge.ui.treetable;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.*;
import java.util.EventObject;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import org.jdesktop.swing.treetable.TreeTableModel;

/**
 *  This example shows how to create a simple JTreeTable component, by using a
 *  JTree as a tree (and editor) for the cells in a particular column in the
 *  JTable.
 *
 * @author     Philip Milne
 * @author     Scott Violet
 * @version    1.2 10/27/98
 */
public class JTreeTable extends JTable {
   /**  A subclass of JTree. */
   protected TreeTableCellRenderer tree;
   TreeTableModelAdapter tableModel;

   public JTreeTable(TreeTableModel treeTableModel) {
      super();

      // Create the tree. It will be used as a tree and editor.
      tree = new TreeTableCellRenderer(treeTableModel);
      tree.setRootVisible(false);

      // Install a tableModel representing the visible rows in the tree.
      tableModel = new TreeTableModelAdapter(treeTableModel, tree);
      super.setModel(tableModel);

      // Force the JTable and JTree to share their row selection models.
      ListToTreeSelectionModelWrapper selectionWrapper = new
            ListToTreeSelectionModelWrapper();
      tree.setSelectionModel(selectionWrapper);
      setSelectionModel(selectionWrapper.getListSelectionModel());
      // Install the tree editor tree and editor.
      setDefaultRenderer(TreeTableModel.class, tree);
      setDefaultEditor(TreeTableModel.class, new TreeTableCellEditor());
      
      // No grid.
      setShowGrid(false);

      // No intercell spacing
      setIntercellSpacing(new Dimension(0, 0));

      // And update the height of the trees row to match that of
      // the table.
      if(tree.getRowHeight() < 1) {
         // Metal looks better like this.
         setRowHeight(18);
      }
      NoHighlightRenderer r = new NoHighlightRenderer();
      defaultRenderersByColumnClass.put(String.class, r);
     
    //  addMouseListener(new MouseAdapter() {
      //   public void mouseClicked(MouseEvent e) {
        //    expandOrCollapseNode(e);
         //}
      //});
   }
   
   public static class NoHighlightRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table,
            Object value,
            boolean isSelected, boolean hasFocus,
            int r, int c) {
        return super.getTableCellRendererComponent(table,
             value,
             isSelected, false,
             r,  c);
      }
   }


   public void addTreeSelectionListener(javax.swing.event.TreeSelectionListener l) {
      tree.addTreeSelectionListener(l);
   }
   
   public TreePath getSelectionPath() {
      return tree.getSelectionPath();
   }

   public boolean editCellAt(int row, int column, EventObject e) {
      expandOrCollapseNode(e);// RG: Fix Issue 49!
      boolean canEdit = super.editCellAt(row, column, e);
      if(canEdit && isHierarchical(column)) {
         repaint(getCellRect(row, column, false));
      }
      return canEdit;
   }


   /**
    *  Overridden to message super and forward the method to the tree. Since the
    *  tree is not actually in the component hieachy it will never receive this
    *  unless we forward it in this manner.
    */
   public void updateUI() {
      super.updateUI();
      if(tree != null) {
         tree.updateUI();
      }
      // Use the tree's default foreground and background colors in the
      // table.
      LookAndFeel.installColorsAndFont(this, "Tree.background",
            "Tree.foreground", "Tree.font");
   }

   private void expandOrCollapseNode(EventObject e) {
      if(e instanceof MouseEvent) {
         MouseEvent me = (MouseEvent) e;
         // If the modifiers are not 0 (or the left mouse button),
         // tree may try and toggle the selection, and table
         // will then try and toggle, resulting in the
         // selection remaining the same. To avoid this, we
         // only dispatch when the modifiers are 0 (or the left mouse
         // button).
         if(me.getModifiers() == 0 ||
               me.getModifiers() == java.awt.event.InputEvent.BUTTON1_MASK) {
            int count = getColumnCount();
           
            for(int i = count - 1; i >= 0; i--) {
               if(isHierarchical(i)) {
                  int savedHeight = tree.getRowHeight();
                  tree.setRowHeight(getRowHeight());
                  MouseEvent pressed = new MouseEvent
                        (tree,
                        me.getID(),
                        me.getWhen(),
                        me.getModifiers(),
                        me.getX() - getCellRect(0, i, false).x,
                        me.getY(),
                        me.getClickCount(),
                        me.isPopupTrigger());
                  tree.dispatchEvent(pressed);
                  // For Mac OS X, we need to dispatch a MOUSE_RELEASED as well
                  MouseEvent released = new MouseEvent
                        (tree,
                        java.awt.event.MouseEvent.MOUSE_RELEASED,
                        pressed.getWhen(),
                        pressed.getModifiers(),
                        pressed.getX(),
                        pressed.getY(),
                        pressed.getClickCount(),
                        pressed.isPopupTrigger());
                  tree.dispatchEvent(released);
                  tree.setRowHeight(savedHeight);
                  break;
               }
            }
         }
      }
   }


   /**
    *  Selects the node identified by the specified path. If any component of
    *  the path is hidden (under a collapsed node), and getExpandsSelectedPaths
    *  is true it is exposed (made viewable).
    *
    * @param  path  The new selectionPath value
    */
   public void setSelectionPath(TreePath path) {
      tree.setSelectionPath(path);
   }


   /**
    *  Overridden to pass the new rowHeight to the tree.
    *
    * @param  rowHeight  The new rowHeight value
    */
   public void setRowHeight(int rowHeight) {
      super.setRowHeight(rowHeight);
      if(tree != null && tree.getRowHeight() != rowHeight) {
         tree.setRowHeight(getRowHeight());
      }
   }


   /**
    *  Determines if the specified column contains hierarchical nodes.
    *
    * @param  column  zero-based index of the column
    * @return         true if the class of objects in the specified column
    *      implement the {@link javax.swing.tree.TreeNode} interface; false
    *      otherwise.
    */
   public boolean isHierarchical(int column) {
      return TreeTableModel.class.isAssignableFrom(
            getColumnClass(column));
   }


   public boolean isExpanded(TreePath path) {
      return tree.isExpanded(path);
   }


   /**
    *  Returns the TreePath for a given x,y location.
    *
    * @param  x  x value
    * @param  y  y value
    * @return    the <code>TreePath</code> for the givern location.
    */
   public TreePath getPathForLocation(int x, int y) {
      int row = rowAtPoint(new java.awt.Point(x, y));
      if(row == -1) {
         return null;
      }
      return tree.getPathForRow(row);
   }


   /*
       Workaround for BasicTableUI anomaly. Make sure the UI never tries to
       paint the editor. The UI currently uses different techniques to
       paint the trees and editors and overriding setBounds() below
       is not the right thing to do for an editor. Returning -1 for the
       editing row in this case, ensures the editor is never painted.
     */
   public int getEditingRow() {
      return (getColumnClass(editingColumn) == TreeTableModel.class) ? -1 :
            editingRow;
   }


   /**
    *  Returns the tree that is being shared between the model.
    *
    * @return    The tree value
    */
   public JTree getTree() {
      return tree;
   }


   /**
    *  A TreeCellRenderer that displays a JTree.
    *
    * @author    Joshua Gould
    */
   public class TreeTableCellRenderer extends JTree implements
         TableCellRenderer {
      /**  Last table/tree row asked to tree. */
      protected int visibleRow;


      public TreeTableCellRenderer(TreeModel model) {
         super(model);
      }


      /**
       *  updateUI is overridden to set the colors of the Tree's tree to match
       *  that of the table.
       */
      public void updateUI() {
         super.updateUI();
         // Make the tree's cell tree use the table's cell selection
         // colors.
         TreeCellRenderer tcr = getCellRenderer();
         if(tcr instanceof DefaultTreeCellRenderer) {
            DefaultTreeCellRenderer dtcr = ((DefaultTreeCellRenderer) tcr);
            // For 1.1 uncomment this, 1.2 has a bug that will cause an
            // exception to be thrown if the border selection color is
            // null.
            // dtcr.setBorderSelectionColor(null);
            dtcr.setTextSelectionColor(UIManager.getColor
                  ("Table.selectionForeground"));
            dtcr.setBackgroundSelectionColor(UIManager.getColor
                  ("Table.selectionBackground"));
         }
      }


      /**
       *  Sublcassed to translate the graphics such that the last visible row
       *  will be drawn at 0,0.
       *
       * @param  g  Description of the Parameter
       */
      public void paint(Graphics g) {
         g.translate(0, -visibleRow * getRowHeight());
         super.paint(g);
      }


      /**
       *  Sets the row height of the tree, and forwards the row height to the
       *  table.
       *
       * @param  rowHeight  The new rowHeight value
       */
      public void setRowHeight(int rowHeight) {
         if(rowHeight > 0) {
            super.setRowHeight(rowHeight);
            if(JTreeTable.this != null &&
                  JTreeTable.this.getRowHeight() != rowHeight) {
               JTreeTable.this.setRowHeight(getRowHeight());
            }
         }
      }


      /**
       *  This is overridden to set the height to match that of the JTable.
       *
       * @param  x  The new bounds value
       * @param  y  The new bounds value
       * @param  w  The new bounds value
       * @param  h  The new bounds value
       */
      public void setBounds(int x, int y, int w, int h) {
         super.setBounds(x, 0, w, JTreeTable.this.getHeight());
      }


      /**
       *  TreeCellRenderer method. Overridden to update the visible row.
       *
       * @param  table       Description of the Parameter
       * @param  value       Description of the Parameter
       * @param  isSelected  Description of the Parameter
       * @param  hasFocus    Description of the Parameter
       * @param  row         Description of the Parameter
       * @param  column      Description of the Parameter
       * @return             The tableCellRendererComponent value
       */
      public Component getTableCellRendererComponent(JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row, int column) {
         if(isSelected) {
            setBackground(table.getSelectionBackground());
         } else {
            setBackground(table.getBackground());
         }

         visibleRow = row;
         return this;
      }
   }


   /**
    *  TreeTableCellEditor implementation. Component returned is the JTree.
    *
    * @author    Joshua Gould
    */
   public class TreeTableCellEditor extends AbstractCellEditor implements
         TableCellEditor {
      public Component getTableCellEditorComponent(JTable table,
            Object value,
            boolean isSelected,
            int r, int c) {
         return tree;
      }


      /**
       *  Overridden to return false, and if the event is a mouse event it is
       *  forwarded to the tree.<p>
       *
       *  The behavior for this is debatable, and should really be offered as a
       *  property. By returning false, all keyboard actions are implemented in
       *  terms of the table. By returning true, the tree would get a chance to
       *  do something with the keyboard events. For the most part this is ok.
       *  But for certain keys, such as left/right, the tree will
       *  expand/collapse where as the table focus should really move to a
       *  different column. Page up/down should also be implemented in terms of
       *  the table. By returning false this also has the added benefit that
       *  clicking outside of the bounds of the tree node, but still in the tree
       *  column will select the row, whereas if this returned true that
       *  wouldn't be the case. <p>
       *
       *  By returning false we are also enforcing the policy that the tree will
       *  never be editable (at least by a key sequence).
       *
       * @param  e  Description of the Parameter
       * @return    The cellEditable value
       */
      public boolean isCellEditable(EventObject e) {
         if(e instanceof MouseEvent) {
            for(int counter = getColumnCount() - 1; counter >= 0;
                  counter--) {
               if(getColumnClass(counter) == TreeTableModel.class) {
                  MouseEvent me = (MouseEvent) e;
                  MouseEvent newME = new MouseEvent(tree, me.getID(),
                        me.getWhen(), me.getModifiers(),
                        me.getX() - getCellRect(0, counter, true).x,
                        me.getY(), me.getClickCount(),
                        me.isPopupTrigger());
                  tree.dispatchEvent(newME);
                  break;
               }
            }
         }
         return false;
      }
   }


   /**
    *  ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel to
    *  listen for changes in the ListSelectionModel it maintains. Once a change
    *  in the ListSelectionModel happens, the paths are updated in the
    *  DefaultTreeSelectionModel.
    *
    * @author    Joshua Gould
    */
   class ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel {
      /**  Set to true when we are updating the ListSelectionModel. */
      protected boolean updatingListSelectionModel;


      public ListToTreeSelectionModelWrapper() {
         super();
         getListSelectionModel().addListSelectionListener
               (createListSelectionListener());
      }


      /**
       *  This is overridden to set <code>updatingListSelectionModel</code> and
       *  message super. This is the only place DefaultTreeSelectionModel alters
       *  the ListSelectionModel.
       */
      public void resetRowSelection() {
         if(!updatingListSelectionModel) {
            updatingListSelectionModel = true;
            try {
               super.resetRowSelection();
            } finally {
               updatingListSelectionModel = false;
            }
         }
         // Notice how we don't message super if
         // updatingListSelectionModel is true. If
         // updatingListSelectionModel is true, it implies the
         // ListSelectionModel has already been updated and the
         // paths are the only thing that needs to be updated.
      }


      /**
       *  Creates and returns an instance of ListSelectionHandler.
       *
       * @return    Description of the Return Value
       */
      protected ListSelectionListener createListSelectionListener() {
         return new ListSelectionHandler();
      }


      /**
       *  If <code>updatingListSelectionModel</code> is false, this will reset
       *  the selected paths from the selected rows in the list selection model.
       */
      protected void updateSelectedPathsFromSelectedRows() {
         if(!updatingListSelectionModel) {
            updatingListSelectionModel = true;
            try {
               // This is way expensive, ListSelectionModel needs an
               // enumerator for iterating.
               int min = listSelectionModel.getMinSelectionIndex();
               int max = listSelectionModel.getMaxSelectionIndex();

               clearSelection();
               if(min != -1 && max != -1) {
                  for(int counter = min; counter <= max; counter++) {
                     if(listSelectionModel.isSelectedIndex(counter)) {
                        TreePath selPath = tree.getPathForRow
                              (counter);

                        if(selPath != null) {
                           addSelectionPath(selPath);
                        }
                     }
                  }
               }
            } finally {
               updatingListSelectionModel = false;
            }
         }
      }


      /**
       *  Returns the list selection model. ListToTreeSelectionModelWrapper
       *  listens for changes to this model and updates the selected paths
       *  accordingly.
       *
       * @return    The listSelectionModel value
       */
      ListSelectionModel getListSelectionModel() {
         return listSelectionModel;
      }


      /**
       *  Class responsible for calling updateSelectedPathsFromSelectedRows when
       *  the selection of the list changse.
       *
       * @author    Joshua Gould
       */
      class ListSelectionHandler implements ListSelectionListener {
         public void valueChanged(ListSelectionEvent e) {
            updateSelectedPathsFromSelectedRows();
         }
      }
   }
}
