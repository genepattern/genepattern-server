package org.genepattern.gpge.ui.treetable;

import java.io.File;

 
/* A FileNode is a derivative of the File class - though we delegate to 
 * the File object rather than subclassing it. It is used to maintain a 
 * cache of a directory's children and therefore avoid repeated access 
 * to the underlying file system during rendering. 
 */
public final class FileNode { 
    File     file; 
    Object[] children; 

    public FileNode(File file) { 
    	// below is a hack to deal with JDK 1.3.1 on XP.  For some
    	// reason the file passed in cannot see its children on that 
    	// combination therefore we refresh it by creating
    	// a new file instance on the same path and using that one.
    	// JTL 10/23/03
		this.file = new File(file.getAbsolutePath()); 
    }

    // Used to sort the file names.
    static private MergeSort  fileMS = new MergeSort() {
	public int compareElementsAt(int a, int b) {
	    return ((String)toSort[a]).compareTo((String)toSort[b]);
	}
    };

    /**
     * Returns the the string to be used to display this leaf in the JTree.
     */
    public String toString() { 
	return file.getName();
    }

    public File getFile() {
	return file; 
    }

    /**
     * Loads the children, caching the results in the children ivar.
     */
    protected Object[] getChildren() {
	if (children != null) {
	    return children; 
	}
	try {
		String[] files = file.list(new java.io.FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.charAt(0) != '.';	// filter hidden files
			}
		});	
	    if(files != null) {
		fileMS.sort(files); 
		children = new FileNode[files.length]; 
		String path = file.getPath();
		for(int i = 0; i < files.length; i++) {
		    File childFile = new File(path, files[i]); 
			children[i] = new FileNode(childFile);
		}
	    }
	} catch (SecurityException se) {}
	return children; 
    }
}
