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



public class FileNode extends AbstractFileNode {
	File f;
	ProjectDirNode parent;
	
	public File getFile() {
	    return f;
	}
	
	public String getFileName() {
	    return f.getName();
	}
	
	public FileNode(ProjectDirNode parent, File f) {
		this.parent = parent;
		this.f = f;
	}
	
	public TreeNode getParent() {
		return parent;
	}
	
	public String getColumnText(int column) {	
		if (column == 0) {
			return f.getName();
		} else if (column == 1) {
			return getFileExtension();
		} else {
			return DateFormat.getInstance().format(new Date(f.lastModified()));
		}
	}
	
		
	public TreeNode parent() {
		return parent;
	}
	
}