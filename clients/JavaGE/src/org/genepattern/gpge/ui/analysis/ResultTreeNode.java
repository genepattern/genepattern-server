package org.genepattern.gpge.ui.analysis;

import javax.swing.tree.*;
import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * <p>Title: ResultTreeNode.java </p>
 * <p>Description:  Class ResultTreeNode is the super class for all tree node in the result tree.</p>
 * @author Hui Gong
 * @version 1.0
 */

public abstract class ResultTreeNode extends DefaultMutableTreeNode{
    protected DataModel _dataModel;
    protected TreeModel _treeModel;

    public ResultTreeNode(Object userObject, DataModel data, TreeModel treeModel) {
        this(userObject, data, treeModel, true);
    }
    public ResultTreeNode(Object userObject, DataModel data, TreeModel treeModel, final boolean allows_childern){
        super(userObject, allows_childern);
        this._dataModel = data;
        this._treeModel = treeModel;
    }


    public abstract JPopupMenu getPopup();

    public abstract void display();
}