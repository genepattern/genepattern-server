package org.genepattern.gpge.ui.maindisplay;

import javax.swing.tree.DefaultMutableTreeNode;

import org.genepattern.gpge.ui.tasks.Sendable;

/**
 * TreeNode that can be "Sent To" an input file
 * @author Joshua Gould
 * 
 */
public abstract class SendableTreeNode extends DefaultMutableTreeNode implements
		Sendable {

}
