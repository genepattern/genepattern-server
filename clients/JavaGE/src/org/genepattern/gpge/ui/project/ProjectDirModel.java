package org.genepattern.gpge.ui.project;

import java.io.File;
import java.io.FileFilter;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Vector;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import org.genepattern.gpge.ui.tasks.*;
import org.genepattern.webservice.*;
import org.genepattern.gpge.ui.treetable.*;
import org.genepattern.gpge.ui.table.*;
import org.genepattern.gpge.ui.maindisplay.FileInfoUtil;
import org.genepattern.gpge.ui.maindisplay.AscendingComparator;
import org.genepattern.gpge.ui.maindisplay.SendableTreeNode;

/**
 * Project directory model
 * 
 * @author Joshua Gould
 */
public class ProjectDirModel extends AbstractSortableTreeTableModel {
	String[] columnNames = { "Name", "Kind", "Date Modified" };

	Class[] columnClasses = {
			org.jdesktop.swing.treetable.TreeTableModel.class, String.class,
			String.class };

	static ProjectDirModel instance = new ProjectDirModel();

	RootNode root = new RootNode();

	final ProjectDirComparator PROJECT_NAME_COMPARATOR = new ProjectDirComparator();

	final ProjectDateComparator PROJECT_DATE_COMPARATOR = new ProjectDateComparator();

	private ProjectComparator projectComparator = PROJECT_NAME_COMPARATOR;

	final FileNameComparator FILE_NAME_COMPARATOR = new FileNameComparator();

	final FileKindComparator FILE_KIND_COMPARATOR = new FileKindComparator();

	final FileDateComparator FILE_DATE_COMPARATOR = new FileDateComparator();

	/** current comparator for sorting files */
	private FileComparator fileComparator = FILE_NAME_COMPARATOR;

	private java.text.DateFormat dateFormat = java.text.DateFormat
			.getDateTimeInstance(java.text.DateFormat.SHORT,
					java.text.DateFormat.SHORT);

	private ProjectDirModel() {

	}

	public void addProjectDirectoryListener(ProjectDirectoryListener l) {
		listenerList.add(ProjectDirectoryListener.class, l);
	}

	/**
	 * Gets the index of the given project directory in this model's sorted list
	 * of project directories
	 * 
	 * @return the index
	 */
	public int indexOf(File projectDirectory) {
		List children = root.getChildren();
		if (children == null) {
			return -1;
		}
		for (int i = 0; i < children.size(); i++) {
			ProjectDirNode p = (ProjectDirNode) children.get(i);
			if (p.directory.equals(projectDirectory)) {
				return i;
			}
		}
		return -1;
	}

	public boolean contains(File projectDir) {
		return indexOf(projectDir) >= 0;
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
				new ProjectDirNode(projectDirectory), projectComparator);
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

	private static class ProjectDirComparator implements ProjectComparator {
		boolean ascending = true;

		public void setAscending(boolean b) {
			ascending = b;
		}

		public int compare(Object obj1, Object obj2) {
			ProjectDirNode n1 = (ProjectDirNode) obj1;
			ProjectDirNode n2 = (ProjectDirNode) obj2;
			return ascending ? n1.directory.getName().compareToIgnoreCase(
					n2.directory.getName()) : n2.directory.getName()
					.compareToIgnoreCase(n1.directory.getName());
		}

	}

	private static class ProjectDateComparator implements ProjectComparator {
		boolean ascending = true;

		public void setAscending(boolean b) {
			ascending = b;
		}

		public int compare(Object obj1, Object obj2) {
			ProjectDirNode n1 = (ProjectDirNode) obj1;
			ProjectDirNode n2 = (ProjectDirNode) obj2;
			Date d1 = new Date(n1.directory.lastModified());
			Date d2 = new Date(n2.directory.lastModified());
			return ascending ? d1.compareTo(d2) : d2.compareTo(d1);
		}

	}

	private static class FileNameComparator implements FileComparator {
		boolean ascending = true;

		public void setAscending(boolean b) {
			ascending = b;
		}

		public int compare(Object obj1, Object obj2) {
			FileNode n1 = (FileNode) obj1;
			FileNode n2 = (FileNode) obj2;
			int result = ascending ? n1.file.getName().compareToIgnoreCase(
					n2.file.getName()) : n2.file.getName().compareToIgnoreCase(
					n1.file.getName());
			return result;
		}
	}

	private static class FileKindComparator implements FileComparator {
		boolean ascending = true;

		public void setAscending(boolean b) {
			ascending = b;
			ProjectDirModel.getInstance().FILE_NAME_COMPARATOR.setAscending(b);
		}

		public int compare(Object obj1, Object obj2) {
			FileNode n1 = (FileNode) obj1;
			FileNode n2 = (FileNode) obj2;
			int result = ascending ? n1.fileInfo.getKind().compareToIgnoreCase(
					n2.fileInfo.getKind()) : n2.fileInfo.getKind()
					.compareToIgnoreCase(n1.fileInfo.getKind());
			if (result == 0) {
				return ProjectDirModel.getInstance().FILE_NAME_COMPARATOR
						.compare(n1, n2);
			}
			return result;
		}
	}

	private static class FileDateComparator implements FileComparator {
		boolean ascending = true;

		public void setAscending(boolean b) {
			ascending = b;
			ProjectDirModel.getInstance().FILE_NAME_COMPARATOR.setAscending(b);
		}

		public int compare(Object obj1, Object obj2) {
			FileNode n1 = (FileNode) obj1;
			FileNode n2 = (FileNode) obj2;
			Date d1 = new Date(n1.file.lastModified());
			Date d2 = new Date(n2.file.lastModified());

			int result = ascending ? d1.compareTo(d2) : d2.compareTo(d1);
			if (result == 0) {
				return ProjectDirModel.getInstance().FILE_NAME_COMPARATOR
						.compare(n1, n2);
			}
			return result;
		}
	}

	public void add(File projectDir) {
		ProjectDirNode child = new ProjectDirNode(projectDir);
		List children = root.getChildren();
		int insertionIndex = 0;
		if (children != null) {
			insertionIndex = Collections.binarySearch(children, child,
					projectComparator);
		}
		if (insertionIndex < 0) {
			insertionIndex = -insertionIndex - 1;
		}

		// keep children sorting alphabetically
		root.insert(child, insertionIndex);
		nodesWereInserted(root, new int[] { insertionIndex });
		notifyProjectAdded(projectDir);
	}

	public void sortOrderChanged(SortEvent e) {
		int column = e.getColumn();
		boolean ascending = e.isAscending();
		List children = root.getChildren();
		PROJECT_NAME_COMPARATOR.setAscending(ascending);
		if (column == 0) {

			FILE_NAME_COMPARATOR.setAscending(ascending);
			fileComparator = FILE_NAME_COMPARATOR;

			projectComparator = PROJECT_NAME_COMPARATOR;
		} else if (column == 1) {

			FILE_KIND_COMPARATOR.setAscending(ascending);
			fileComparator = FILE_KIND_COMPARATOR;

			projectComparator = PROJECT_NAME_COMPARATOR;
		} else {
			FILE_DATE_COMPARATOR.setAscending(ascending);
			fileComparator = FILE_DATE_COMPARATOR;

			PROJECT_DATE_COMPARATOR.setAscending(ascending);
			projectComparator = PROJECT_DATE_COMPARATOR;
		}

		if (children != null) {
			Collections.sort(children, projectComparator);

			for (int i = 0; i < children.size(); i++) {
				ProjectDirNode node = (ProjectDirNode) children.get(i);
				if (node.getChildren() != null) {
					Collections.sort(node.getChildren(), fileComparator);
				}
			}
			nodeStructureChanged(root);
		}

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
			case 1:
				return "Project";
			case 2:
				return dateFormat.format(new Date(p.directory.lastModified()));
			default:
				return null;
			}
		} else if (node instanceof FileNode) {
			FileNode f = (FileNode) node;
			switch (column) {
			case 0:
				return f.toString();
			case 1:
				return f.fileInfo.getKind();
			case 2:
				return dateFormat.format(new Date(f.file.lastModified()));
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

		public Vector getChildren() {
			return children;
		}

		public ProjectDirNode(File directory) {
			this.directory = directory;
			refresh();
		}

		public void refresh() {
			File[] files = null;
			if (directory.exists() && directory.isDirectory()) {
				try {
					files = directory.listFiles(new FileFilter() {
						public boolean accept(File f) {
							String name = f.getName();
							return !f.isDirectory() && !name.endsWith("~")
									&& !name.startsWith(".");
						}
					});
				} catch (SecurityException se) {
				}
			}

			if (files != null) {

				FileComparator c = ProjectDirModel.getInstance().fileComparator;

				for (int i = 0; i < files.length; i++) {

					FileNode child = new FileNode(files[i]);
					int insertionIndex = 0;
					if (children != null) {
						insertionIndex = Collections.binarySearch(children,
								child, c);
					}
					if (insertionIndex < 0) {
						insertionIndex = -insertionIndex - 1;
					}

					insert(child, insertionIndex);
				}

			}
		}

		public String toString() {
			String name = directory.getName();
			File parent = directory.getParentFile();
			return parent != null ? name + " (" + parent.getPath() + ")" : name;
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
	public static class FileNode extends SendableTreeNode {

		public final File file;

		private FileInfoUtil.FileInfo fileInfo;

		public FileNode(File file) {
			this.file = file;
			this.fileInfo = FileInfoUtil.getInfo(file);
			if (fileInfo == null) {
				fileInfo = new FileInfoUtil.FileInfo();
			}
		}

		public FileInfoUtil.FileInfo getFileInfo() {
			return fileInfo;
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

		public String toUIString() {
			return file.getPath();
		}

	}

	private static class RootNode extends DefaultMutableTreeNode {
		public Vector getChildren() {
			return children;
		}

		public String toString() {
			return "Root";
		}
	}

	private static interface FileComparator extends AscendingComparator {

	}

	private static interface ProjectComparator extends AscendingComparator {

	}

}