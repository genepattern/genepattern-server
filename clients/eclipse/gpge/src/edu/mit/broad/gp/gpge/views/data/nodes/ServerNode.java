/*
 * Created on Jun 19, 2004
 * 
 * TODO To change the template for this generated file go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
package edu.mit.broad.gp.gpge.views.data.nodes;

import java.util.ArrayList;
import java.util.List;

public class ServerNode implements TreeNode, Comparable {
    String site;
    TreeNode parent;
    List children;

    // TreeNode parent;
     
    public int compareTo(Object other) {
        ServerNode node = (ServerNode) other;
        return this.site.compareTo(node.site);
    }

    public void refresh() {
        for(int i = 0, size = children.size(); i < size; i++) {
            JobResultNode node = (JobResultNode) children.get(i);
            node.refresh();
        }
    }
    
    public ServerNode(String siteName, TreeNode parent) { // TreeNode parent) {
        this.site = siteName;
        this.parent = parent;
        this.children = new ArrayList();
    }

    public TreeNode parent() {
        return null;
    }

    public TreeNode[] children() {
        return (TreeNode[]) children.toArray(new TreeNode[0]);
    }

    public String getColumnText(int column) {
        if (column == 0)
            return site;
        else if (column == 1) {
            return "Server";
        } else {
            return "";
        }
    }
        /**
     * @param jrfn
     */
    public void remove(JobResultNode jrfn) {
        children.remove(jrfn);
    }
    
 
    /**
     * @return
     */
    public String getSiteName() {
        return site;
    }
    
    public void addJobResultNode(JobResultNode child){
        children.add(child);
        child.setParent(this);
    }
    
    public void removeJobResultNode(JobResultNode child){
        children.remove(child);
    }
}