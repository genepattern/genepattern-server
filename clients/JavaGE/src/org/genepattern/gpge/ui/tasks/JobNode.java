package org.genepattern.gpge.ui.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.activation.DataHandler;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.log4j.Category;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.util.RequestHandlerFactory;
import org.genepattern.webservice.WebServiceException;

/**
 * <p>Title: JobNode.java </p>
 * <p>Description: A tree node containing information about analysis job.</p>
 * @author Hui Gong
 * @version 1.0
 */

public class JobNode extends ResultTreeNode {
    private JTextArea _result;
    private static Category cat = Category.getInstance(JobNode.class.getName());
    private String _fileCache;

    public JobNode(AnalysisJob userObject, DataModel data, TreeModel treeModel, JTextArea textArea) {
        super((Object)userObject, data, treeModel);
        this._result = textArea;
    }

    /**
     * implements abstract method from <code>ResultTreeNode<code>
     * @return JPopupMenu
     */
    public JPopupMenu getPopup(){
        JPopupMenu menu = new JPopupMenu();
        if(JobNode.this.isLeaf()){
            JMenuItem open = new JMenuItem("Open");
            menu.add(open);
            open.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae){
                    AnalysisJob result =(AnalysisJob) JobNode.this.getUserObject();
                    displayResult(result);
                }
            });
            JMenuItem download = new JMenuItem("download");
            menu.add(download);
            download.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae){
                    AnalysisJob result =(AnalysisJob) JobNode.this.getUserObject();
                    downloadResult(result);
                }
            });
        }
        JMenuItem delete = new JMenuItem("Delete");
        menu.add(delete);
        delete.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ae){
                AnalysisJob nodeInfo =(AnalysisJob) JobNode.this.getUserObject();
                ResultTreeNode parent = (ResultTreeNode)JobNode.this.getParent();
                parent.remove(JobNode.this);
                ((DefaultTreeModel )JobNode.this._treeModel).nodeStructureChanged((TreeNode)JobNode.this);
                JobNode.this._dataModel.removeResult(nodeInfo);
            }
        });

        return menu;
    }

    /**
     * Displays the result.
     */
    public void display(){
        if(JobNode.this.isLeaf()){
            AnalysisJob result =(AnalysisJob) JobNode.this.getUserObject();
            displayResult(result);
        }
    }

    private void displayResult(AnalysisJob job){
        try{
            if(this._fileCache==null){
                String[] fileNames = RequestHandlerFactory.getInstance(job.getJobInfo().getUserId(), null).getRequestHandler(job.getServer()).getResultFiles(job.getJobInfo().getJobNumber());
                this._fileCache = fileNames[0];
            }
            BufferedReader in = new BufferedReader(new FileReader(this._fileCache));
            String line;
            this._result.setText("");
            while((line = in.readLine())!=null){
                this._result.append(line+"\n");
            }
        }
        catch(WebServiceException wse){
            cat.error("Error in getting result from server");
            this._result.setText("Error in retriving data from server");
        }
        catch(IOException io){
            cat.error("IO exception in reading the results", io);
            this._result.setText("Error in retriving data from server");
        }
    }

    private void downloadResult(AnalysisJob job){
    try{
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("."));
        int state = chooser.showSaveDialog(null);
        File file=chooser.getSelectedFile();
        if(file !=null && state==JFileChooser.APPROVE_OPTION){
            FileWriter writer = new FileWriter(file);
            if(this._fileCache==null){
                String[] fileNames = RequestHandlerFactory.getInstance(job.getJobInfo().getUserId(), null).getRequestHandler(job.getServer()).getResultFiles(job.getJobInfo().getJobNumber());
                this._fileCache = fileNames[0];
            }
            BufferedReader reader = new BufferedReader(new FileReader(this._fileCache));
            String line;
            while((line = reader.readLine())!=null){
                writer.write(line+"\n");
            }
            writer.flush();
            writer.close();
        }
    }
    catch(WebServiceException wse){
        cat.error("Error in getting result from server");
        this._result.setText("Error in retriving data from server");
    }
    catch(IOException io){
        cat.error("IO exception in reading the results", io);
        this._result.setText("Error in retriving data from server");
    }
    }
}