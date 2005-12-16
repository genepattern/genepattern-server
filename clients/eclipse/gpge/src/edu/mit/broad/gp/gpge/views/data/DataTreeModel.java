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


package edu.mit.broad.gp.gpge.views.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.event.EventListenerList;

import edu.mit.broad.gp.gpge.views.data.nodes.JobResultFileNode;
import edu.mit.broad.gp.gpge.views.data.nodes.JobResultNode;
import edu.mit.broad.gp.gpge.views.data.nodes.ProjectDirNode;
import edu.mit.broad.gp.gpge.views.data.nodes.ServerNode;
import edu.mit.broad.gp.gpge.views.data.nodes.TreeNode;
import org.genepattern.webservice.AnalysisJob;

public class DataTreeModel {
    DummyNode root = new DummyNode();

    protected EventListenerList listenerList = new EventListenerList();

    private static DataTreeModel instance = new DataTreeModel();

    private DataTreeModel() {
    }

    public void nodeChanged(TreeNode root) {
        notifyListeners(new DataTreeModelEvent(this, root));
    }
    
    public static DataTreeModel getInstance() {
        return instance;
    }

    public TreeNode getRoot() {
        return root;
    }

    public void addDataTreeModelListener(DataTreeModelListener l) {
        listenerList.add(DataTreeModelListener.class, l);
    }

    public void removeDataTreeModelListener(DataTreeModelListener l) {
        listenerList.remove(DataTreeModelListener.class, l);
    }
    
    public void removeFile(JobResultFileNode node) {
        JobResultNode parent = (JobResultNode) node.parent();
		parent.remove(node);
        notifyListeners(new DataTreeModelEvent(this, node));
    }

public void addJob(String taskName,
		AnalysisJob job) {
        JobResultNode jrn = new JobResultNode(taskName, job);
        ServerNode serverNode = this.getServerNode(job.getServer());
        // root.addServerNode(serverNode);
        serverNode.addJobResultNode(jrn);
        notifyListeners(new DataTreeModelEvent(this, serverNode));
    
    } 


private void notifyListeners(DataTreeModelEvent e) {
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == DataTreeModelListener.class) {
                ((DataTreeModelListener) listeners[i + 1]).dataTreeChanged(e);
            }
        }
    }

public void addProjectDirectory(File dir) {
        ProjectDirNode pdn = new ProjectDirNode(dir);
        root.addProjectDirNode(pdn);
        notifyListeners(new DataTreeModelEvent(this,  pdn));
    }    

public void removeProjectDir(ProjectDirNode dir) {
        root.removeProjectDir(dir);
        notifyListeners(new DataTreeModelEvent(this, dir));
    }

public ArrayList getProjectDirNodes(){
	ArrayList pdirs = new ArrayList();
	TreeNode[] nodes = root.children();
	for (int i=0; i < nodes.length; i++){
		if (nodes[i] instanceof ProjectDirNode){
			pdirs.add(nodes[i]);
		}
	}
	return pdirs;
}


public ServerNode getServerNode(String siteName){
    ServerNode server = null;
	TreeNode[] nodes = root.children();
	for (int i=0; i < nodes.length; i++){
		if (nodes[i] instanceof ServerNode){
		    ServerNode inst = (ServerNode)nodes[i];
		    if (inst.getServer().equals(siteName)){
		        return inst;
		    }
		}
	}
	server = new ServerNode(siteName,root);
	System.out.println("Adding server "+ server);
	root.addServerNode(server);
	notifyListeners(new DataTreeModelEvent(this, server));  
	return server;
}

    /**
     * @param node
     */
    public void removeJobResult(JobResultNode node) {
        //root.removeJobResult(node);
        System.out.println("Here");
        ServerNode nodeParent = (ServerNode)node.parent();
        nodeParent.removeJobResultNode(node);
        
        notifyListeners(new DataTreeModelEvent(this, nodeParent));
    }

    private static class DummyNode implements TreeNode {
        List projectNodes = new ArrayList();
        List serverNodes = new ArrayList();

        List jobNodes = new ArrayList();

        public TreeNode parent() {
            throw new UnsupportedOperationException();
        }

        /**
         * @param node
         */
        public void removeJobResult(TreeNode node) {
            jobNodes.remove(node);
        }

        public void addJobResultNode(TreeNode n) {
            jobNodes.add(n);
            Collections.sort(jobNodes);
        }

        public void removeProjectDir(ProjectDirNode n) {
            projectNodes.remove(n);
        }

        public void addProjectDirNode(ProjectDirNode n) {
            projectNodes.add(n);
            Collections.sort(projectNodes);
        }
        public void addServerNode(ServerNode n) {
            serverNodes.add(n);
            Collections.sort(serverNodes);
        }
        
        public TreeNode[] children() {
            TreeNode[] children = new TreeNode[projectNodes.size()
                    + jobNodes.size() + serverNodes.size()];
            int childIndex = 0;
            for (int i = 0, size = projectNodes.size(); i < size; i++, childIndex++) {
                children[childIndex] = (TreeNode) projectNodes.get(i);
            }
            for (int i = 0, size = jobNodes.size(); i < size; i++, childIndex++) {
                children[childIndex] = (TreeNode) jobNodes.get(i);
            }
            for (int i = 0, size = serverNodes.size(); i < size; i++, childIndex++) {
                children[childIndex] = (TreeNode) serverNodes.get(i);
            }
            return children;
        }

        public String getColumnText(int column) {
            throw new UnsupportedOperationException();
        }
    }

}