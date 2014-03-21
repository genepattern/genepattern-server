package org.genepattern.server.dm;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.genepattern.server.config.GpContext;


/**
 * A node in a file listing tree.
 * 
 * @author pcarr
 */
public class GpDirectoryNode extends Node<GpFilePath> {
    private Map<String, Node<GpFilePath>> allElements = new HashMap<String, Node<GpFilePath>>();
    static private Comparator<Node<GpFilePath>> comparator = new Comparator<Node<GpFilePath>>() {
        public int compare(Node<GpFilePath> o1, Node<GpFilePath> o2) {
            //directories come before files
            final boolean o1IsDir=o1.getValue().isDirectory();
            final boolean o2IsDir=o2.getValue().isDirectory();
            if (o1IsDir) {
                if (!o2IsDir) {
                    return -1;
                }
            }
            else if (o2IsDir) {
                if (!o1IsDir) {
                    return 1;
                }
            }
            return o1.getValue().getRelativeUri().getPath().compareTo( o2.getValue().getRelativeUri().getPath() );
        }
    };
    
    public GpDirectoryNode(GpFilePath val) {
        super(comparator, val);
    }
    
    //TODO: cache this list
    public List<GpFilePath> getAllFilePaths() {
        List<GpFilePath> rval = new ArrayList<GpFilePath>();
        GpFilePath root = this.getValue();
        if (root != null) {
            rval.add(root);
        }
        Collection<Node<GpFilePath>> children = getChildren();
        if (children == null) {
            return rval;
        }
        if (children.size() == 0) {
            return rval;
        }
        appendChildren( rval, children );
        return rval;
    }
    
    private void appendChildren(List<GpFilePath> to, Collection<Node<GpFilePath>> children) {
        if (children == null) {
            return;
        }
        if (children.size() == 0) {
            return;
        }
        for(Node<GpFilePath> child : children) {
            to.add( child.getValue() );
            Collection<Node<GpFilePath>> grandChildren = child.getChildren();
            if (grandChildren != null && grandChildren.size() > 0) {
                appendChildren(to, grandChildren);
            }
        }
    }
    
    //helper methods for constructing the tree from an arbitrarily ordered list of GpFilePath
    
    /**
     * Kind of like mkdirs() this will add all necessary nodes to the tree 
     * @param gpFilePath
     * @return
     * @throws Exception
     */
    public Node<GpFilePath> add(GpContext userContext, GpFilePath gpFilePath) throws Exception {
        String relativePath = gpFilePath.getRelativePath();
        Node<GpFilePath> node = allElements.get( relativePath );
        if (node != null) {
            //TODO: log this, ignoring duplicate add
            return node;
        }
        String[] parts = relativePath.split("/");
        //if necessary add parent directories
        Node<GpFilePath> parentNode = this;
        
        boolean createMissingSubDirs = false;
        if (createMissingSubDirs) {
            String key = "";
            for(int i = 0; i<parts.length - 1; ++i) {
                key = key + parts[i] + "/";
                Node<GpFilePath> subNode = allElements.get( key );
                if (subNode == null) {
                    GpFilePath subFilePath = GpFileObjFactory.getUserUploadFile(userContext, new File(key));
                    subNode = parentNode.addChild(subFilePath);
                    allElements.put(key, subNode);
                }
                parentNode = subNode; 
            }
        }
        Node<GpFilePath> childNode = parentNode.addChild(gpFilePath);
        allElements.put(relativePath, childNode);
        return childNode;
    }
}
