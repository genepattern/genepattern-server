package org.genepattern.gpge.ui.project;

import java.io.File;
import java.io.FileFilter;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;
import java.util.List;

import javax.swing.event.TreeModelEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import org.genepattern.gpge.ui.tasks.*;
import org.genepattern.webservice.*;
import org.genepattern.gpge.ui.treetable.*;

/**
 *  Description of the Class
 *
 * @author    Joshua Gould
 */

/**
 * Description of the Class
 * 
 * @author Joshua Gould
 */
public class ProjectDirModel extends AbstractSortableTreeTableModel {
	String[] columnNames = { "Name" };

	Class[] columnClasses = { org.jdesktop.swing.treetable.TreeTableModel.class };

	static ProjectDirModel instance = new ProjectDirModel();

	RootNode root = new RootNode();

	static final ProjectDirComparator PROJECT_DIR_COMPARATOR = new ProjectDirComparator();

	private ProjectDirModel() {
	}

	public void addProjectDirectoryListener(ProjectDirectoryListener l) {
		listenerList.add(ProjectDirectoryListener.class, l);
	}

	public void removeProjectDirectoryListener(ProjectDirectoryListener l) {
		listenerList.remove(ProjectDirectoryListener.class, l);
	}

	protected void notifyProjectAdded(File dir) {
		Object[] listeners = listenerList.getListenerList();
		ProjectEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ProjectDirectoryListener.class) {
				// Lazily create the event:
				if (e == null) {
					e = new ProjectEvent(this, dir);
				}

				((ProjectDirectoryListener) listeners[i + 1]).projectAdded(e);
			}
		}
	}

	protected void notifyProjectRemoved(File dir) {
		Object[] listeners = listenerList.getListenerList();
		ProjectEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ProjectDirectoryListener.class) {
				// Lazily create the event:
				if (e == null) {
					e = new ProjectEvent(this, dir);
				}

				((ProjectDirectoryListener) listeners[i + 1]).projectRemoved(e);
			}
		}
	}

	public String getPreferencesString() {
		List children = root.getChildren();
		if (children == null) {
			return null;
		}
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < children.size(); i++) {
			ProjectDirNode node = (ProjectDirNode) children.get(i);
			if (i > 0) {
				sb.append(";");
			}
			try {
				sb.append(node.directory.getCanonicalPath());
			} catch (java.io.IOException ioe) {
			}
		}
		return sb.toString();
	}

	public boolean contains(File projectDir) {
		Vector children = root.getChildren();
		if (children == null) {
			return false;
		}
		return Collections.binarySearch(children,
				new ProjectDirNode(projectDir), PROJECT_DIR_COMPARATOR) >= 0;
	}

	/**
	 * Deletes the all the output files for a job from the server
	 * 
	 * @param node
	 *            Description of the Parameter
	 */
	public void remove(ProjectDirNode node) {
		int index = root.getIndex(node);
		root.remove(index);
		nodesWereRemoved(root, new int[] { index }, new Object[] { node });
		notifyProjectRemoved(node.directory);
	}

	public void refresh(File projectDirectory) {
		int index = Collections.binarySearch(root.getChildren(),
				new ProjectDirNode(projectDirectory), PROJECT_DIR_COMPARATOR);
		ProjectDirNode node = (ProjectDirNode) root.getChildAt(index);
		node.removeAllChildren();
		node.refresh();
		nodeStructureChanged(node);
	}

	public void refresh(ProjectDirNode node) {
		node.removeAllChildren();
		node.refresh();
		nodeStructureChanged(node);
	}

	private static class ProjectDirComparator implements Comparator {
		public int compare(Object obj1, Object obj2) {
			ProjectDirNode n1 = (ProjectDirNode) obj1;
			ProjectDirNode n2 = (ProjectDirNode) obj2;
			return n1.directory.compareTo(n2.directory);
		}

		public boolean equals(Object obj1, Object obj2) {
			ProjectDirNode n1 = (ProjectDirNode) obj1;
			ProjectDirNode n2 = (ProjectDirNode) obj2;
			return n1.directory.equals(n2.directory);
		}
	}

	public void add(File projectDir) {
		ProjectDirNode child = new ProjectDirNode(projectDir);
		List children = root.getChildren();
		int insertionIndex = 0;
		if (children != null) {
			insertionIndex = Collections.binarySearch(children, child,
					PROJECT_DIR_COMPARATOR);
		}
		if (insertionIndex < 0) {
			insertionIndex = -insertionIndex - 1;
		}

		// keep children sorting alphabetically
		root.insert(child, insertionIndex);
		int[] newIndexs = { insertionIndex };

		Object[] p1 = { root };
		Object[] kids = { child };

		Object[] path = getPathToRoot(root);
		final TreeModelEvent e = new TreeModelEvent(this, path);
		nodeStructureChanged(root);
		notifyProjectAdded(projectDir);
	}

	public void sortOrderChanged(SortEvent e) {

	}

	public Class getColumnClass(int column) {
		return columnClasses[column];
	}

	public Object getRoot() {
		return root;
	}

	public static ProjectDirModel getInstance() {
		return instance;
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public String getColumnName(int column) {
		return columnNames[column];
	}

	public Object getValueAt(Object node, int column) {
		if (node instanceof ProjectDirNode) {
			ProjectDirNode p = (ProjectDirNode) node;
			switch (column) {
			case 0:
				return p.toString();
			default:
				return null;
			}
		} else if (node instanceof FileNode) {
			FileNode f = (FileNode) node;
			switch (column) {
			case 0:
				return f.toString();
			default:
				return null;
			}
		}
		return null;
	}

	/**
	 * Description of the Class
	 * 
	 * @author Joshua Gould
	 */
	public static class ProjectDirNode extends DefaultMutableTreeNode {

		public final File directory;

		public ProjectDirNode(File directory) {
			this.directory = directory;
			refresh();
		}

		public void refresh() {
			File[] files = directory.listFiles(new FileFilter() {
				public boolean accept(File f) {
					String name = f.getName();
					return !f.isDirectory() && !name.endsWith("~")
							&& !name.startsWith(".");
				}
			});
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					add(new FileNode(files[i]));
				}
			}
		}

		public String toString() {
			return directory.getName() + " ("
					+ directory.getParentFile().getPath() + ")";
		}

		public boolean getAllowsChildren() {
			return true;
		}

		public boolean isLeaf() {
			return false;
		}

	}

	/**
	 * Description of the Class
	 * 
	 * @author Joshua Gould
	 */
	public static class FileNode extends DefaultMutableTreeNode {

		public final File file;

		public FileNode(File file) {
			this.file = file;

		}

		public String toString() {
			return file.getName();
		}

		public boolean getAllowsChildren() {
			return false;
		}

		public boolean isLeaf() {
			return true;
		}

	}

	private static class RootNode extends DefaultMutableTreeNode {
		public Vector getChildren() {
			return children;
		}
	}

}