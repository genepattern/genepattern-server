/*
 * SortTreeModel.java
 *
 * Created on August 20, 2003, 3:44 PM
 */

package org.genepattern.gpge.ui.graphics;

import java.util.Enumeration;

import javax.swing.tree.TreeNode;
import javax.swing.tree.MutableTreeNode;

/**
 * 
 * @author  kohm
 */
public class SortTreeModel extends javax.swing.tree.DefaultTreeModel {
    
    /** Creates a new instance of SortTreeModel */
    public SortTreeModel(final TreeNode root, final boolean asksAllowsChildren) {
        super(root, asksAllowsChildren);
    }
    
    /**
     * Invoked this to insert newChild into parent and it will be sorted.
     * Note that the <CODE>index</CODE> parameter is ignored
     * Use the insertNodeInto(MutableTreeNode newChild, MutableTreeNode parent) 
     * instead
     */
    public void insertNodeInto(MutableTreeNode newChild, MutableTreeNode parent, int index) {
        insertNodeInto(newChild, parent);
    }
    /**
     * Invoked this to insert a newChild node into the parent and its' position will 
     * be proper for being sorted.
     */
    public void insertNodeInto(final MutableTreeNode newChild, final MutableTreeNode parent) {
        final String child = newChild.toString();
        int i = 0;
        for(final Enumeration enum = parent.children(); enum.hasMoreElements(); i++) {
            final String other = enum.nextElement().toString();
            final int cmpr = child.compareTo(other);
            if(cmpr <= 0 )
                break;
        }
        super.insertNodeInto(newChild, parent, i);
    }
}
