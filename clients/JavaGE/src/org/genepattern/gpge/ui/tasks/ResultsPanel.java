package org.genepattern.gpge.ui.tasks;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.apache.log4j.Category;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.ParameterInfo;

/**
 * ResultsPanel.java
 * <p>
 * Description: Displays the result of analysis jobs.
 * </p>
 * 
 * @author Hui Gong
 * @version $Revision 1.5 $
 */

public class ResultsPanel extends JPanel implements Observer {
	private DataModel _dataModel;

	private JTextArea _result;

	private JTree _tree;

	private DefaultMutableTreeNode _root;

	private DefaultTreeModel _treeModel;

	private static Category cat = Category.getInstance(ResultsPanel.class
			.getName());

	public ResultsPanel(DataModel model) {
		this._dataModel = model;

		//creating result pane
		JPanel resultPane = new JPanel();
		resultPane.setLayout(new BorderLayout());

		//top
		JPanel labelPane = new JPanel();
		labelPane.setLayout(new FlowLayout(FlowLayout.LEFT));
		JLabel resultLabel = new JLabel("Result:");
		labelPane.add(resultLabel);
		//labelPane.add(resultLabel, FlowLayout.LEADING);

		//center
		_result = new JTextArea();
		Font f = new Font("Courier New", Font.PLAIN, 12);
		_result.setFont(f);
		JScrollPane resultView = new JScrollPane(_result);

		//bottom
		JPanel buttonPane = new JPanel();
		JButton save = new JButton("Save");
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				try {
					JFileChooser chooser = new JFileChooser();
					chooser.setCurrentDirectory(new File("."));
					int state = chooser.showSaveDialog(null);
					File file = chooser.getSelectedFile();
					if (file != null && state == JFileChooser.APPROVE_OPTION) {
						FileWriter writer = new FileWriter(file);
						writer.write(ResultsPanel.this._result.getText());
						writer.flush();
						writer.close();
					}
				} catch (IOException e) {
					cat.error("", e);
				}
			}
		});
		buttonPane.add(save);

		resultPane.add(labelPane, BorderLayout.NORTH);
		resultPane.add(resultView, BorderLayout.CENTER);
		resultPane.add(buttonPane, BorderLayout.SOUTH);

		//treeView
		_root = new DefaultMutableTreeNode("My Jobs");
		_treeModel = new DefaultTreeModel(_root);
		createTreeNodes();
		_tree = new JTree(_treeModel);
		_tree.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int selRow = _tree.getRowForLocation(e.getX(), e.getY());
				TreePath selPath = _tree.getPathForLocation(e.getX(), e.getY());
				if (selPath == null)
					return;
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath
						.getLastPathComponent();
				if (node == null)
					return;
				if (node.isRoot())
					return;
				if (SwingUtilities.isRightMouseButton(e)) {
					JPopupMenu popup = ((ResultTreeNode) node).getPopup();
					popup.show((JComponent) e.getSource(), e.getX(), e.getY());
				} else if (selRow != -1 && e.getClickCount() == 2) {
					((ResultTreeNode) node).display();
				}
			}
		});
		_tree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.SINGLE_TREE_SELECTION);
		JScrollPane treeView = new JScrollPane(_tree);

		//create split pan to put treeView and result pane
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setDividerLocation(0.25);
		splitPane.setLeftComponent(treeView);
		splitPane.setRightComponent(resultPane);
		splitPane.setOneTouchExpandable(true);

		this.setLayout(new BorderLayout());
		this.add(splitPane, BorderLayout.CENTER);

	}

	/**
	 * implements the method from interface Observer
	 * 
	 * @param o
	 *            <code>Observable<code> object, it's <code>DataModel<code> here
	 * @param arg
	 */
	public void update(Observable o, Object arg) {
		cat.debug("updating observer ResultsPanel");
		_dataModel = (DataModel) o;
		reloadTree();
	}

	private void reloadTree() {
		_root.removeAllChildren();
		createTreeNodes();
		_treeModel.reload();
	}

	private void createTreeNodes() {
		createTreeNodes(_root, TreeNodeFactory.PASS_THOUGH);
	}

	public void createTreeNodes(final DefaultMutableTreeNode the_root,
			final TreeNodeFactory node_factory) {
		this._dataModel.printResult();//debug stuff
		final Hashtable results = this._dataModel.getResults();
		for (final Enumeration e = results.keys(); e.hasMoreElements();) {
			final String siteName = (String) e.nextElement();
			final ResultTreeNode site = new SiteNode(siteName, this._dataModel,
					this._treeModel);
			final Hashtable jobsOnsite = (Hashtable) results.get(siteName);

			for (final Enumeration enu = jobsOnsite.keys(); enu
					.hasMoreElements();) {
				final String jobType = (String) enu.nextElement();
				cat.debug("jobType: " + jobType);
				final ResultTreeNode category = new TaskNode(jobType,
						this._dataModel, this._treeModel);
				//getting jobs
				final Vector jobs = (Vector) jobsOnsite.get(jobType);
				for (final Iterator iterator = jobs.iterator(); iterator
						.hasNext();) {
					final AnalysisJob job = (AnalysisJob) iterator.next();
					final ResultTreeNode jobNode = new JobNode(job,
							this._dataModel, this._treeModel, this._result);
					if (job.getJobInfo().containsOutputFileParam()) {
						ParameterInfo[] params = job.getJobInfo()
								.getParameterInfoArray();
						for (int i = 0; i < params.length; i++) {
							if (params[i].isOutputFile()) {
								final String file_path = params[i].getValue();
								System.out.println("file path: " + file_path);
								//final String filename =
								// file_path.substring(file_path.lastIndexOf("__")+2);
								final String filename = file_path;
								final ResultFile file = new ResultFile(
										siteName, job.getJobInfo()
												.getJobNumber(), filename);
								//final ResultTreeNode fileNode = new
								// FileNode(file, this._dataModel,
								// this._treeModel, this._result);
								final MutableTreeNode fileNode = node_factory
										.createNode(new FileNode(file,
												_dataModel, _treeModel,
												_result, job.getJobInfo()
														.getUserId(), null));
								jobNode.add(fileNode);
							}
						}
					}
					category.add(jobNode);
				}
				site.add(category);
			}
			the_root.add(site);
		}
	}
	//    private void createTreeNodes(){
	//        this._dataModel.printResult();
	//        Hashtable results = this._dataModel.getResults();
	//        Enumeration e = results.keys();
	//        while(e.hasMoreElements()){
	//            String siteName = (String)e.nextElement();
	//            ResultTreeNode site = new SiteNode(siteName, this._dataModel,
	// this._treeModel);
	//            Hashtable jobsOnsite = (Hashtable)results.get(siteName);
	//            Enumeration enu = jobsOnsite.keys();
	//
	//            while(enu.hasMoreElements()){
	//                String jobType = (String)enu.nextElement();
	//                cat.debug("jobType: "+jobType);
	//                ResultTreeNode category = new TaskNode(jobType, this._dataModel,
	// this._treeModel);
	//                //getting jobs
	//                Vector jobs = (Vector)jobsOnsite.get(jobType);
	//                Iterator iterator = jobs.iterator();
	//                while(iterator.hasNext()){
	//                    AnalysisJob job = (AnalysisJob)iterator.next();
	//                    ResultTreeNode jobNode = new JobNode(job, this._dataModel,
	// this._treeModel, this._result);
	//                    if(job.getJobInfo().containsOutputFileParam()){
	//                        ParameterInfo[] params = job.getJobInfo().getParameterInfoArray();
	//                        for(int i=0; i< params.length; i++){
	//                            if(params[i].isOutputFile()){
	//                                String file_path = params[i].getValue();
	//                                System.out.println("file path: "+file_path);
	//                                String filename = file_path.substring(file_path.lastIndexOf("__")+2);
	//                                ResultFile file = new ResultFile(siteName,
	// job.getJobInfo().getJobNumber(), filename);
	//                                ResultTreeNode fileNode = new FileNode(file, this._dataModel,
	// this._treeModel, this._result);
	//                                jobNode.add(fileNode);
	//                            }
	//                        }
	//                    }
	//                    category.add(jobNode);
	//                }
	//                site.add(category);
	//            }
	//            _root.add(site);
	//        }
	//    }

}