package org.genepattern.gpge.ui.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.tree.TreeModel;

import org.apache.log4j.Category;
import org.genepattern.util.RequestHandlerFactory;
import org.genepattern.webservice.WebServiceException;

/**
 * <p>Title: FileNode.java </p>
 * <p>Description: This tree node containing information about a result file</p>
 * @author Hui Gong
 * @version 1.0
 */

public class FileNode extends ResultTreeNode {
    private JTextArea _result;
    private static Category cat = Category.getInstance(FileNode.class.getName());
    private String[] _filesCache = null;
    private String username;
    private String password;

    public FileNode(ResultFile userObject, DataModel data, TreeModel treeModel, JTextArea textArea){
	this(userObject, data, treeModel, textArea, null, null);
    }

    public FileNode(ResultFile userObject, DataModel data, TreeModel treeModel, JTextArea textArea, String username, String password){
        super(userObject, data, treeModel, false);
        this._result = textArea;
	this.username = username;
	this.password = password;
    }

    public JPopupMenu getPopup(){
        JPopupMenu menu = new JPopupMenu();
        JMenuItem open = new JMenuItem("Open");
        menu.add(open);
        open.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ae){
                ResultFile result =(ResultFile) FileNode.this.getUserObject();
                displayResult(result);
            }
        });

        JMenuItem download = new JMenuItem("download");
        menu.add(download);
        download.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ae){
                ResultFile result =(ResultFile)FileNode.this.getUserObject();
                downloadResult(result);
            }
        });
        return menu;
    }

    public void display(){
        ResultFile result =(ResultFile) FileNode.this.getUserObject();
        displayResult(result);
    }

    private void displayResult(ResultFile result){
        try{
            if(this._filesCache==null){
                System.out.println("storing cache for displaying.");
                this._filesCache = RequestHandlerFactory.getInstance(username, password).getRequestHandler(result.getSite()).getResultFiles(result.getJobID());
            }
            for(int i=0; i< _filesCache.length; i++){
                String filename = this._filesCache[i];
                if(filename.substring(filename.indexOf("__")+2).equals(result.getFileName())){
                    BufferedReader reader = new BufferedReader(new FileReader(filename));
                    String line;
                    this._result.setText("");
                    while((line = reader.readLine())!=null){
                        this._result.append(line+"\n");
                    }
                }
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

    private void downloadResult(ResultFile result){
        try{
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("."));
            int state = chooser.showSaveDialog(null);
            File file=chooser.getSelectedFile();
            if(file !=null && state==JFileChooser.APPROVE_OPTION){
                FileWriter writer = new FileWriter(file);
                if(this._filesCache==null){
                    System.out.println("storing cache for downloading.");
                    this._filesCache = RequestHandlerFactory.getInstance(username, password).getRequestHandler(result.getSite()).getResultFiles(result.getJobID());
                }
                for(int i=0; i< _filesCache.length; i++){
                    String filename = this._filesCache[i];
                    if(filename.substring(filename.indexOf("__")+2).equals(result.getFileName())){
                        BufferedReader reader = new BufferedReader(new FileReader(filename));
                        String line;
                        while((line = reader.readLine())!=null){
                            writer.write(line+"\n");
                        }
                        writer.flush();
                        writer.close();
                    }
                }
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