package org.genepattern.gpge.ui.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;


/**
 * <p>Title: TaskNode.java </p>
 * <p>Description: A ResultTreeNode representing a specific task.</p>
 * @author Hui Gong
 * @version 1.0
 */

public class TaskNode extends ResultTreeNode {

    public TaskNode(String task_name, DataModel data, TreeModel model) {
        super(task_name, data, model);
    }

    public JPopupMenu getPopup(){
        JPopupMenu menu = new JPopupMenu();
        JMenuItem open = new JMenuItem("Delete");
        menu.add(open);
        open.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ae){
                String task =(String)TaskNode.this.getUserObject();
                ResultTreeNode parent = (ResultTreeNode)TaskNode.this.getParent();
                String siteName = (String)parent.getUserObject();
                if(TaskNode.this.getChildCount()!=0){
                    int type = JOptionPane.showConfirmDialog(null, "Do you want to delete all results under "+task+"?");
                    if(type==JOptionPane.OK_OPTION){
                        TaskNode.this._dataModel.removeTaskFromResult(siteName, task);
                        parent.remove(TaskNode.this);
                        ((DefaultTreeModel)TaskNode.this._treeModel).nodeStructureChanged(TaskNode.this);
                    }
                }
                else{
                    TaskNode.this._dataModel.removeTaskFromResult(siteName, task);
                    parent.remove(TaskNode.this);
                    ((DefaultTreeModel)TaskNode.this._treeModel).nodeStructureChanged(TaskNode.this);
                }
            }
        });
        return menu;
    }

    public void display(){
    }
}