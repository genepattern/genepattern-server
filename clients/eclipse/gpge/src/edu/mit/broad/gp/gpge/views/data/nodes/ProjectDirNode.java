/*
 * Created on Jun 19, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.broad.gp.gpge.views.data.nodes;


import java.io.File;
import java.text.DateFormat;
import java.util.Date;

import edu.mit.broad.gp.core.GPGECorePlugin;


public class ProjectDirNode implements TreeNode, Comparable {
	File file;
	TreeNode parent = null;
	
	public ProjectDirNode(File dir) {
		file = dir;
	}
	public ProjectDirNode(ProjectDirNode parent, File dir) {
		file = dir;
		this.parent = parent;
	}
	
	public File getFile(){
	    return file;
	}
	
	public int compareTo(Object other) {
	    ProjectDirNode node = (ProjectDirNode) other;
	    return this.file.getName().compareTo(node.file.getName());
	}
	
	public String getColumnText(int column){
		if(column==0) {
			return file.getName();
		} else if(column==1) {
			return "Project";
		} else {
			return DateFormat.getInstance().format(new Date(file.lastModified()));
		}
	}
	
	public TreeNode parent() {
		return parent;
	}
	
	public TreeNode[] children() {
		boolean showSubdirs = GPGECorePlugin.getDefault().getPreferenceBoolean(GPGECorePlugin.PROJ_DIRS_SUBDIR_PREFERENCE);
		File[] files = null;
		if (showSubdirs){
			files = file.listFiles(HiddenFileFilter.FILE_FILTER);
		} else {
			files = file.listFiles(FilesOnlyFileFilter.FILE_FILTER);
		}
	
		if(files == null) {
			return new TreeNode[0];
		}
				
		TreeNode[] nodes = new TreeNode[files.length];
		for(int i = 0, length = files.length; i < length; i++) {
			if (files[i].isDirectory()){
			    nodes[i] = new SubDirNode(this, files[i]);
			} else {
				nodes[i] = new FileNode(this, files[i]);
			}
		}
		return nodes;
	}
}