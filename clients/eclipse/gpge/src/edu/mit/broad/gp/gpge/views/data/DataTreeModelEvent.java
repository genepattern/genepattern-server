/*
 * Created on Jun 19, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.broad.gp.gpge.views.data;

import java.util.EventObject;

import edu.mit.broad.gp.gpge.views.data.nodes.TreeNode;

/**
 * @author jgould
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class DataTreeModelEvent extends EventObject {
    TreeNode element;
    
    public DataTreeModelEvent(Object source, TreeNode element) {
        super(source);
    }
    
    public TreeNode getElement() {
        return element;
    }

}
