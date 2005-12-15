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
