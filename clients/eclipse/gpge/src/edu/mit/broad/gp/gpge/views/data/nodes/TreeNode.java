/*
 * Created on Jun 19, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.broad.gp.gpge.views.data.nodes;


public interface TreeNode {
	public TreeNode parent();
	public TreeNode[] children();
	public String getColumnText(int column);
}