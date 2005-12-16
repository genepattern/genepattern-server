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


public class SubDirNode extends ProjectDirNode {

	public SubDirNode(File dir) {
		super(dir);
	}
	public SubDirNode(ProjectDirNode parent, File dir) {
		super(parent, dir);
	}
	public int compareTo(Object other) {
	    SubDirNode node = (SubDirNode) other;
	    return this.file.getName().compareTo(node.file.getName());
	}
	public String getColumnText(int column){
		if(column==0) {
			return file.getName();
		} else if(column==1) {
			return "";
		} else {
			return DateFormat.getInstance().format(new Date(file.lastModified()));
		}
	}
	
	public TreeNode parent() {
		return parent;
	}
	
}