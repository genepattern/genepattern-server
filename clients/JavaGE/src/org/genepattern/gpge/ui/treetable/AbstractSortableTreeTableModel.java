package org.genepattern.gpge.ui.treetable;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.util.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import javax.swing.tree.DefaultMutableTreeNode;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.gpge.ui.tasks.*;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.*;
import org.jdesktop.swing.*;
import org.jdesktop.swing.treetable.*;
/**
 *  Description of the Class
 *
 * @author    Joshua Gould
 */

/**
 *  Description of the Class
 *
 * @author    Joshua Gould
 */
public abstract class AbstractSortableTreeTableModel implements SortTreeTableModel {

   protected EventListenerList listenerList = new EventListenerList();


   public void addTreeModelListener(TreeModelListener l) {
      listenerList.add(TreeModelListener.class, l);
   }


   public void removeTreeModelListener(TreeModelListener l) {
      listenerList.remove(TreeModelListener.class, l);
   }


   public void valueForPathChanged(TreePath path, Object newValue) {

   }



   public abstract void sortOrderChanged(SortEvent e);


   /**
    *  Invoke this method after you've inserted some TreeNodes into node.
    *  childIndices should be the index of the new elements and must be sorted
    *  in ascending order.
    *
    * @param  node          Description of the Parameter
    * @param  childIndices  Description of the Parameter
    */
   public void nodesWereInserted(TreeNode node, int[] childIndices) {
      if(listenerList != null && node != null && childIndices != null
             && childIndices.length > 0) {
         int cCount = childIndices.length;
         Object[] newChildren = new Object[cCount];

         for(int counter = 0; counter < cCount; counter++) {
            newChildren[counter] = node.getChildAt(childIndices[counter]);
         }
         fireTreeNodesInserted(this, getPathToRoot(node), childIndices,
               newChildren);
      }
   }


   /**
    *  Invoke this method after you've removed some TreeNodes from node.
    *  childIndices should be the index of the removed elements and must be
    *  sorted in ascending order. And removedChildren should be the array of
    *  the children objects that were removed.
    *
    * @param  node             Description of the Parameter
    * @param  childIndices     Description of the Parameter
    * @param  removedChildren  Description of the Parameter
    */
   public void nodesWereRemoved(TreeNode node, int[] childIndices,
         Object[] removedChildren) {
      if(node != null && childIndices != null) {
         fireTreeNodesRemoved(this, getPathToRoot(node), childIndices,
               removedChildren);
      }
   }


   /**
    *  Invoke this method after you've changed how the children identified by
    *  childIndicies are to be represented in the tree.
    *
    * @param  node          Description of the Parameter
    * @param  childIndices  Description of the Parameter
    */
   public void nodesChanged(TreeNode node, int[] childIndices) {
      if(node != null) {
         if(childIndices != null) {
            int cCount = childIndices.length;

            if(cCount > 0) {
               Object[] cChildren = new Object[cCount];

               for(int counter = 0; counter < cCount; counter++) {
                  cChildren[counter] = node.getChildAt
                        (childIndices[counter]);
               }
               fireTreeNodesChanged(this, getPathToRoot(node),
                     childIndices, cChildren);
            }
         } else if(node == getRoot()) {
            fireTreeNodesChanged(this, getPathToRoot(node), null, null);
         }
      }
   }


   /**
    *  Invoke this method if you've totally changed the children of node and
    *  its childrens children... This will post a treeStructureChanged event.
    *
    * @param  node  Description of the Parameter
    */
   public void nodeStructureChanged(TreeNode node) {
      if(node != null) {
         fireTreeStructureChanged(this, getPathToRoot(node), null, null);
      }
   }


   /*
       Notify all listeners that have registered interest for
       notification on this event type.  The event instance
       is lazily created using the parameters passed into
       the fire method.
       @see EventListenerList
     */
   protected void fireTreeNodesChanged(Object source, Object[] path,
         int[] childIndices, Object[] children) {
      // Guaranteed to return a non-null array
      Object[] listeners = listenerList.getListenerList();
      TreeModelEvent e = null;
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for(int i = listeners.length - 2; i >= 0; i -= 2) {
         if(listeners[i] == TreeModelListener.class) {
            // Lazily create the event:
            if(e == null) {
               e = new TreeModelEvent(source, path,
                     childIndices, children);
            }
            ((TreeModelListener) listeners[i + 1]).treeNodesChanged(e);
         }
      }
   }

   /*
       Notify all listeners that have registered interest for
       notification on this event type.  The event instance
       is lazily created using the parameters passed into
       the fire method.
       @see EventListenerList
     */
   protected void fireTreeNodesInserted(Object source, Object[] path,
         int[] childIndices, Object[] children) {
      // Guaranteed to return a non-null array
      Object[] listeners = listenerList.getListenerList();
      TreeModelEvent e = null;
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for(int i = listeners.length - 2; i >= 0; i -= 2) {
         if(listeners[i] == TreeModelListener.class) {
            // Lazily create the event:
            if(e == null) {
               e = new TreeModelEvent(source, path,
                     childIndices, children);
            }
            ((TreeModelListener) listeners[i + 1]).treeNodesInserted(e);
         }
      }
   }


   /*
       Notify all listeners that have registered interest for
       notification on this event type.  The event instance
       is lazily created using the parameters passed into
       the fire method.
       @see EventListenerList
     */
   protected void fireTreeNodesRemoved(Object source, Object[] path,
         int[] childIndices, Object[] children) {
      // Guaranteed to return a non-null array
      Object[] listeners = listenerList.getListenerList();
      TreeModelEvent e = null;
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for(int i = listeners.length - 2; i >= 0; i -= 2) {
         if(listeners[i] == TreeModelListener.class) {
            // Lazily create the event:
            if(e == null) {
               e = new TreeModelEvent(source, path,
                     childIndices, children);
            }
            ((TreeModelListener) listeners[i + 1]).treeNodesRemoved(e);
         }
      }
   }


   /*
       Notify all listeners that have registered interest for
       notification on this event type.  The event instance
       is lazily created using the parameters passed into
       the fire method.
       @see EventListenerList
     */
   protected void fireTreeStructureChanged(Object source, Object[] path,
         int[] childIndices,
         Object[] children) {
      // Guaranteed to return a non-null array
      Object[] listeners = listenerList.getListenerList();
      TreeModelEvent e = null;
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for(int i = listeners.length - 2; i >= 0; i -= 2) {
         if(listeners[i] == TreeModelListener.class) {
            // Lazily create the event:
            if(e == null) {
               e = new TreeModelEvent(source, path,
                     childIndices, children);
            }
            ((TreeModelListener) listeners[i + 1]).treeStructureChanged(e);
         }
      }
   }


   protected void fireTreeStructureChanged(Object source, Object[] path) {
      //      int[] childIndices, Object[] children) {
      // Guaranteed to return a non-null array
      Object[] listeners = listenerList.getListenerList();
      TreeModelEvent e = null;
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for(int i = listeners.length - 2; i >= 0; i -= 2) {
         if(listeners[i] == TreeModelListener.class) {
            // Lazily create the event:
            if(e == null) {
               e = new TreeModelEvent(source, path);
               //childIndices, children);
            }
            ((TreeModelListener) listeners[i + 1]).treeStructureChanged(e);
         }
      }
   }



   public void setValueAt(Object value, Object node, int column) {

   }


   /**
    *  Builds the parents of node up to and including the root node, where the
    *  original node is the last element in the returned array. The length of
    *  the returned array gives the node's depth in the tree.
    *
    * @param  aNode  the TreeNode to get the path for
    * @return        The pathToRoot value
    */
   public Object[] getPathToRoot(TreeNode aNode) {
      return getPathToRoot(aNode, 0);
   }


   /**
    *  Returns an array of all the tree model listeners registered on this
    *  model.
    *
    * @return    all of this model's <code>TreeModelListener</code>s or an
    *      empty array if no tree model listeners are currently registered
    * @see       #addTreeModelListener
    * @see       #removeTreeModelListener
    * @since     1.4
    */
   public TreeModelListener[] getTreeModelListeners() {
      return (TreeModelListener[]) listenerList.getListeners(
            TreeModelListener.class);
   }



   public boolean isCellEditable(Object node, int column) {
      return false;
   }


   public int getChildCount(Object parent) {
      return ((DefaultMutableTreeNode) parent).getChildCount();
   }


   public int getIndexOfChild(Object parent, Object child) {
      return ((DefaultMutableTreeNode) parent).getIndex((DefaultMutableTreeNode) child);
   }


   public abstract Class getColumnClass(int column);


   public abstract Object getRoot();


   public boolean isLeaf(Object node) {
      return ((DefaultMutableTreeNode) node).isLeaf();
   }


   public Object getChild(Object parent, int index) {
      return ((DefaultMutableTreeNode) parent).getChildAt(index);
   }


   public abstract int getColumnCount();


   public abstract String getColumnName(int column);


   public abstract Object getValueAt(Object node, int column);


   /**
    *  Builds the parents of node up to and including the root node, where the
    *  original node is the last element in the returned array. The length of
    *  the returned array gives the node's depth in the tree.
    *
    * @param  aNode  the TreeNode to get the path for
    * @param  depth  an int giving the number of steps already taken towards
    *      the root (on recursive calls), used to size the returned array
    * @return        an array of TreeNodes giving the path from the root to the
    *      specified node
    */
   protected Object[] getPathToRoot(TreeNode aNode, int depth) {
      Object[] retNodes;
      // This method recurses, traversing towards the root in order
      // size the array. On the way back, it fills in the nodes,
      // starting from the root and working back to the original node.

      /*
          Check for null, in case someone passed in a null node, or
          they passed in an element that isn't rooted at root.
        */
      if(aNode == null) {
         if(depth == 0) {
            return null;
         } else {
            retNodes = new Object[depth];
         }
      } else {
         depth++;
         if(aNode == getRoot()) {
            retNodes = new Object[depth];
         } else {
            retNodes = getPathToRoot(aNode.getParent(), depth);
         }
         retNodes[retNodes.length - depth] = aNode;
      }
      return retNodes;
   }

}
