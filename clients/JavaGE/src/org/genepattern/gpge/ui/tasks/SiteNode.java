package org.genepattern.gpge.ui.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

/**
 * <p>
 * Title: SiteNode.java
 * </p>
 * <p>
 * Description: A tree node for analysis service site.
 * </p>
 * 
 * @author Hui Gong
 * @version 1.0
 */

public class SiteNode extends ResultTreeNode {

	public SiteNode(String site_name, DataModel data, TreeModel model) {
		super(site_name, data, model);
	}

	public JPopupMenu getPopup() {
		JPopupMenu menu = new JPopupMenu();
		JMenuItem delete = new JMenuItem("Delete");
		menu.add(delete);
		delete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String siteName = (String) SiteNode.this.getUserObject();
				DefaultMutableTreeNode parent = (DefaultMutableTreeNode) SiteNode.this
						.getParent();
				if (SiteNode.this.getChildCount() != 0) {
					int type = JOptionPane.showConfirmDialog(null,
							"Do you want to delete all results under "
									+ siteName + "?");
					if (type == JOptionPane.OK_OPTION) {
						SiteNode.this._dataModel.removeSiteFromResult(siteName);
						parent.remove(SiteNode.this);
						((DefaultTreeModel) SiteNode.this._treeModel)
								.nodeStructureChanged(SiteNode.this);
					}
				} else {
					SiteNode.this._dataModel.removeSiteFromResult(siteName);
					parent.remove(SiteNode.this);
					((DefaultTreeModel) SiteNode.this._treeModel)
							.nodeStructureChanged(SiteNode.this);
				}
			}
		});
		return menu;
	}

	public void display() {
	}
}