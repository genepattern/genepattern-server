/*
 * TreeNodeFactory.java
 *
 * Created on March 30, 2003, 1:47 PM
 */

package org.genepattern.gpge.ui.tasks;

import javax.swing.tree.MutableTreeNode;

/**
 * Interface defines a method for transforming one node into another.
 * 
 * @author kohm
 */
public interface TreeNodeFactory {
	/** can create a MutableTreeNode wrapper from a MutableTreeNode */
	public MutableTreeNode createNode(final MutableTreeNode node);

	//fields
	/** does nothing just passes the node through */
	public static final TreeNodeFactory PASS_THOUGH = new TreeNodeFactory() {
		/** returns the specified node */
		public final MutableTreeNode createNode(final MutableTreeNode node) {
			return node;
		}
	};
}