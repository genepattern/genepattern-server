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
