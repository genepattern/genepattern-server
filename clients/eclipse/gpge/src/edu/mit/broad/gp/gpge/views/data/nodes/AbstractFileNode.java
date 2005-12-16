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
 * Created on Jul 28, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.broad.gp.gpge.views.data.nodes;


/**
 * @author genepattern
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public abstract class AbstractFileNode implements TreeNode {

    
    public abstract String getFileName();
    
    public String getFileExtension(){
        String name = getFileName();
		int dotIndex = name.lastIndexOf(".");
		if (dotIndex != -1) {
			String ext = name.substring(dotIndex + 1, name.length());
			return ext;
		}
		return "";
    }
    
    

   
	
	public TreeNode[] children() {
		return new TreeNode[0];
	}
}
