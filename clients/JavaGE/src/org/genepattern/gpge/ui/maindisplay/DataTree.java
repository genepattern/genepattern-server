/*
 * DataTree.java
 *
 * Created on March 3, 2003, 12:48 PM
 */
package org.genepattern.gpge.ui.maindisplay;
 
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.genepattern.data.DataModel;
import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.io.AbstractDataSource;
import org.genepattern.gpge.io.DataObjectProxy;
import org.genepattern.gpge.io.DataSourceUpdateListener;
import org.genepattern.gpge.io.DefaultDataObjectProxy;
import org.genepattern.gpge.io.DirDataSource;
import org.genepattern.gpge.io.GroupDataSource;
import org.genepattern.gpge.ui.browser.BrowserPanel;
import org.genepattern.gpge.ui.graphics.SortTreeModel;
import org.genepattern.util.ExceptionHandler;
import org.genepattern.util.GPpropertiesManager;
import org.genepattern.util.StringUtils;
import org.genepattern.util.SwingWorker;




/**
 *  Manages the JTree's Data proxy nodes and/or Data source nodes.
 *
 *@author     kohm
 *@created    March 30, 2004
 */
public class DataTree implements org.genepattern.gpge.io.DataSourceManager {
	//fields

	/**  the DataSelectListener (Should be some sort of List for multiple listeners) */
	private DataSelectListener listener = null;
	/**  the root Node */
	private final DefaultMutableTreeNode root;
	/**  the collection of data source listeners keyed by their source */
	private final java.util.Map source_nodes_to_listener;
	/**  the tree */
	private final javax.swing.JTree data_tree;
	/**  a special DataSourceListener that is used by another instance of DataTree */
	private DataSourceListener special;
	/**  another DataTree that is dependent on the data gathered here */
	private final DataTree other;
	/**  the Map of the user object to a TreeNode in the tree */
	private final Map user_node;
	DataObjectBrowser dataObjectBrowser;

	/**
	 *  Creates a new instance of DataTree
	 *
	 *@param  data_tree  Description of the Parameter
	 */
	public DataTree(final JTree data_tree) {
		this(data_tree, null, false);
	}						
								
	void removeEmptyJobNodes() {
		javax.swing.SwingUtilities.invokeLater(new Thread() {
			public void run() {
				DefaultMutableTreeNode root = (DefaultMutableTreeNode) data_tree.getModel().getRoot();
				for(Enumeration enum = root.breadthFirstEnumeration(); enum.hasMoreElements(); ) {
					Object obj = enum.nextElement();	
					if(obj instanceof DefaultMutableTreeNode && ((DefaultMutableTreeNode)obj).getUserObject() instanceof org.genepattern.gpge.io.ServerJobDataSource ) {
						DefaultMutableTreeNode node = (DefaultMutableTreeNode) obj;
						org.genepattern.gpge.io.ServerJobDataSource ds = (org.genepattern.gpge.io.ServerJobDataSource) node.getUserObject();
						org.genepattern.webservice.AnalysisJob job = ds.getJob();
						
						if(node.getChildCount()==0) { // job has no output files
							if(node.getParent().getChildCount() == 1) { // remove task from tree
								dataObjectBrowser.removeJob(job);
								DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
								org.genepattern.gpge.io.ServerTaskDataSource pds = (org.genepattern.gpge.io.ServerTaskDataSource)parent.getUserObject();
								((DefaultTreeModel)data_tree.getModel()).removeNodeFromParent(parent);
								dataObjectBrowser.removeTask(pds.getName());
							} else {
								dataObjectBrowser.removeJob(job);
								((DefaultTreeModel)data_tree.getModel()).removeNodeFromParent(node);
							}
						}
					}
				}
			}
		});
	}

	/**
	 *  Creates a new instance of DataTree
	 *
	 *@param  data_tree     Description of the Parameter
	 *@param  other         Description of the Parameter
	 *@param  enable_popup  Description of the Parameter
	 */
	public DataTree(final JTree data_tree, final DataTree other, final boolean enable_popup) {
		this.data_tree = data_tree;
		this.root = new DefaultMutableTreeNode("Root that should not be seen!");
		this.source_nodes_to_listener = new java.util.HashMap();
		this.other = other;
		this.user_node = new HashMap();
		Runnable r =
			new Runnable() {
				public void run() {
					data_tree.setModel(new SortTreeModel(root, true));
					data_tree.setRootVisible(false);
					data_tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
				}
			};
		SwingUtilities.invokeLater(r);
		data_tree.addTreeSelectionListener(
			new TreeSelectionListener() {//FIXME needs to be protected by SafeRun
				public final void valueChanged(final TreeSelectionEvent e) {
					final DefaultMutableTreeNode node = (DefaultMutableTreeNode)
							data_tree.getLastSelectedPathComponent();

					if(node == null) {
						return;
					}

					final Object nodeInfo = node.getUserObject();
					if(nodeInfo instanceof DataObjectProxy) {
						notifyNodeSelectListeners((DataObjectProxy)nodeInfo, node);
					} else if(nodeInfo instanceof DataModel) {
						notifyNodeSelectListeners((DataModel)nodeInfo, node);
					} else if(nodeInfo instanceof GroupDataSource) {
						notifyNodeSelectListeners((GroupDataSource)nodeInfo, node);
					} else {
						notifyNodeSelectListenersUnkn("Unknown node type " + nodeInfo);
						System.err.println("Don't know how to process this node: " + nodeInfo);
					}
				}
			});
		if(enable_popup) {
			data_tree.addMouseListener(new PopupMenuHandler());
		}
		data_tree.addMouseListener(
			new MouseInputListener() {
				public final void mouseReleased(MouseEvent e) { }


				public final void mouseMoved(java.awt.event.MouseEvent me) { }


				public final void mouseClicked(MouseEvent e) { }


				public final void mouseDragged(java.awt.event.MouseEvent me) { }


				public final void mouseEntered(MouseEvent e) { }


				public final void mouseExited(MouseEvent e) { }


				public final void mousePressed(final MouseEvent e) {
					final int selRow = data_tree.getRowForLocation(e.getX(), e.getY());
					final TreePath selPath = data_tree.getPathForLocation(e.getX(), e.getY());

					if(selRow != -1) {
						try {
							if(e.getClickCount() == 1) {
								mySingleClick(selRow, selPath);
							} else if(e.getClickCount() == 2) {
								myDoubleClick(selRow, selPath);
							}
						} catch(Exception ex) {
							ExceptionHandler.handleException(ex);
						}
					}
				}


				/**
				 *@param  selRow         Description of the Parameter
				 *@param  selPath        Description of the Parameter
				 *@exception  Exception  Description of the Exception
				 */
				private void mySingleClick(final int selRow, final TreePath selPath) throws Exception {
				}


				/**
				 *@param  selRow         Description of the Parameter
				 *@param  path           Description of the Parameter
				 *@exception  Exception  Description of the Exception
				 */
				private void myDoubleClick(final int selRow, final TreePath path) throws Exception {
					final DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
					final Object user_obj = node.getUserObject();

					if(user_obj instanceof DataObjectProxy) {
						final DataObjectProxy proxy = (DataObjectProxy)user_obj;

						try {
							visualizeProxy(proxy);
						} catch(java.io.FileNotFoundException fnfe) {
							DefaultMutableTreeNode jobNode = null;
							try {
								jobNode = (DefaultMutableTreeNode)node.getParent().getParent();
							} catch(Exception e) {// user clicked in data objects view
								((DefaultTreeModel)data_tree.getModel()).removeNodeFromParent(node);
								return;
							}
							if(!(jobNode.getUserObject() instanceof org.genepattern.gpge.io.ServerJobDataSource)) {// user clicked in data objects view
								((DefaultTreeModel)data_tree.getModel()).removeNodeFromParent(node);
								return;
							}

							org.genepattern.gpge.io.ServerJobDataSource ds = (org.genepattern.gpge.io.ServerJobDataSource)jobNode.getUserObject();

							org.genepattern.webservice.AnalysisJob job = ds.getJob();// get the job for this
							org.genepattern.webservice.JobInfo jobInfo = job.getJobInfo();
							jobInfo.removeParameterInfo(user_obj.toString());
							DefaultMutableTreeNode dataTypeNode = (DefaultMutableTreeNode)node.getParent();
							if(dataTypeNode.getChildCount() == 1) {
								if(jobNode.getChildCount() == 1) {
									JOptionPane.showMessageDialog(GenePattern.getDialogParent(), "Job number " + jobInfo.getJobNumber() + " has been deleted from the server.");
									DefaultMutableTreeNode taskNode = (DefaultMutableTreeNode)jobNode.getParent();
									if(taskNode.getChildCount() == 1) {
										((DefaultTreeModel)data_tree.getModel()).removeNodeFromParent(taskNode);
										dataObjectBrowser.removeJob(job);
										org.genepattern.gpge.io.ServerTaskDataSource taskDS = (org.genepattern.gpge.io.ServerTaskDataSource)taskNode.getUserObject();
										dataObjectBrowser.removeTask(taskDS.getName());
									} else {
										((DefaultTreeModel)data_tree.getModel()).removeNodeFromParent(jobNode);
										dataObjectBrowser.removeJob(job);
									}
								} else {
									JOptionPane.showMessageDialog(GenePattern.getDialogParent(), user_obj + " has been deleted from the server.");
									((DefaultTreeModel)data_tree.getModel()).removeNodeFromParent(dataTypeNode);
								}
							} else {
								JOptionPane.showMessageDialog(GenePattern.getDialogParent(), user_obj + " has been deleted from the server.");
								((DefaultTreeModel)data_tree.getModel()).removeNodeFromParent(node);
							}
							dataObjectBrowser.persistJobs();
						} catch (java.net.ConnectException cne){
							// server disconencted.  Remove the server node
							// and then tell the dataObjectBrowser we are disconnected
							dataObjectBrowser.disconnectedFromServer();

						}
					}
				}
			});
	}

	/** 
	 * handle a disconnect by a server -JTL
	**/
	public void disconnectedFromServer(){
		TreeNode root = (TreeNode)((DefaultTreeModel)data_tree.getModel()).getRoot();
		for (Enumeration e = root.children(); e.hasMoreElements(); ){
			DefaultMutableTreeNode child =  (DefaultMutableTreeNode )e.nextElement();
			if (child.getUserObject().getClass() == org.genepattern.gpge.io.ServerSiteDataSource.class){
				((DefaultTreeModel)data_tree.getModel()).removeNodeFromParent(child);
				break;
			}
		}
	}
	

	/**
	 *  Adds a listener for single clicks on Nodes that represent DataObjectProxy
	 *  oor DataModel bjects.
	 *
	 *@param  listener                                 The feature to be added to
	 *      the DataSelectListener attribute
	 *@exception  java.util.TooManyListenersException  Description of the Exception
	 */
	public void addDataSelectListener(final DataSelectListener listener) throws java.util.TooManyListenersException {
		if(this.listener != null) {
			throw new java.util.TooManyListenersException("Currently only support one listener");
		}
		this.listener = listener;
	}


	/**
	 *  removes a listener for single clicks on Nodes that represent
	 *  DataObjectProxy or DataModel objects.
	 *
	 *@param  listener  Description of the Parameter
	 */
	public void removeDataSelectListener(final DataSelectListener listener) {
		if(this.listener == listener) {
			this.listener = null;
		}
	}


	/**
	 *  notifies the listeners that a DataObjectProxy node was selected
	 *
	 *@param  proxy  Description of the Parameter
	 *@param  node   Description of the Parameter
	 */
	protected void notifyNodeSelectListeners(final DataObjectProxy proxy, final MutableTreeNode node) {
		if(listener != null) {
			listener.dataProxySelected(proxy, node);
		}
	}


	/**
	 *  notifies the listeners that a DataModel node was selected
	 *
	 *@param  model  Description of the Parameter
	 *@param  node   Description of the Parameter
	 */
	protected void notifyNodeSelectListeners(final DataModel model, final MutableTreeNode node) {
		if(listener != null) {
			listener.dataModelSelected(model, node);
		}
	}


	/**
	 *  notifies the listeners that a GroupDataSource node was selected
	 *
	 *@param  source  Description of the Parameter
	 *@param  node    Description of the Parameter
	 */
	protected void notifyNodeSelectListeners(final GroupDataSource source, final MutableTreeNode node) {
		if(listener != null) {
			listener.dataSourceSelected(source, node);
		}
	}


	/**
	 *  notifies the listeners that an unknown node type was selected
	 *
	 *@param  source  Description of the Parameter
	 */
	protected void notifyNodeSelectListenersUnkn(final Object source) {
		if(listener != null) {
			listener.unknownSelected(source);
		}
	}


	/**
	 *  adds a new data source to the tree
	 *
	 *@param  source  The feature to be added to the DataSource attribute
	 */
	public void addDataSource(final GroupDataSource source) {
		//should check first that this source is unique before adding
		if(other == null) {
			System.out.println("addDataSource a: " + source.getClass());
			source_nodes_to_listener.put(source, new DataSourceListener(source));
		} else {
			System.out.println("addDataSource b: " + source.getClass());
			source_nodes_to_listener.put(source, new DataSourceListener(source, other.getSpecialDataSourceListener()));
		}
	}


	/**
	 *  removes an old data source from the tree
	 *
	 *@param  source  Description of the Parameter
	 */
	public void removeDataSource(final GroupDataSource source) {
		//throw new UnsupportedOperationException("Needs implementing!");
		//should check first that this source is present before removing
		final DataSourceListener dsl = (DataSourceListener)source_nodes_to_listener.get(source);

		
		if(dsl != null) {
			final DataObjectProxy[] proxies = source.getDataProxies();

	
			dsl.updateRemoveDataObjects(proxies, null);

			if(dsl.dependent != null) {
				dsl.dependent.updateRemoveDataObjects(proxies, null);
			}
		} else {
			throw new IllegalArgumentException("Trying to remove DataSource that is not present!\n" + source);
		}

	}

	/**
	 *  returns a special instance of the DataSourceListener that is not normaly
	 *  created
	 *
	 *@return    The specialDataSourceListener value
	 */
	private DataSourceListener getSpecialDataSourceListener() {
		if(special == null) {
			special = new DataSourceListener(this.root, null, null);
		}
		return special;
	}

	/**  causes the selected node to become reselected */
	void reselect() {
		Runnable r =
			new Runnable() {
				public void run() {
					data_tree.setSelectionPath(data_tree.getSelectionPath());
				}
			};
		SwingUtilities.invokeLater(r);
	}


	/**
	 *  visualize the proxy
	 *
	 *@param  proxy                         Description of the Parameter
	 *@return                               Description of the Return Value
	 *@exception  java.text.ParseException  Description of the Exception
	 *@exception  java.io.IOException       Description of the Exception
	 */
	public final boolean visualizeProxy(final DataObjectProxy proxy) throws java.text.ParseException, java.io.IOException {
		final DataModel model = proxy.getDataModel();
		
		final boolean use_browser = GPpropertiesManager.getBooleanProperty("open.files.system.browser", true);
		if(use_browser || handleWithNativeBrowser(proxy)) {
			showWithNativeBrowser(proxy);
		} else {// use java "Browser"
			Thread r =
				new Thread() {
					public void run() {
						try {
							showWithJavaBrowser(proxy);
						} catch(java.io.IOException ioe) {
							ioe.printStackTrace();
						}
					}
				};
			r.start();
		}

		return true;
	}


	/**
	 *  shows the file from the DataObjectProxy in the platform's native browser
	 *
	 *@param  proxy                    Description of the Parameter
	 *@exception  java.io.IOException  Description of the Exception
	 */
	protected static void showWithNativeBrowser(final DataObjectProxy proxy) throws java.io.IOException {
		final AbstractDataSource source = (AbstractDataSource)proxy.getDataSource();
		final java.io.File file = source.getAsLocalFile(proxy);
		System.out.println("Browser loading file: " + file);
		org.genepattern.util.BrowserLauncher.openURL("File://" + file.getAbsolutePath());
	}


	/**
	 *  shows the file from the DataObjectProxy in the Java browser
	 *
	 *@param  proxy                    Description of the Parameter
	 *@exception  java.io.IOException  Description of the Exception
	 */
	protected static void showWithJavaBrowser(final DataObjectProxy proxy) throws java.io.IOException {
		final javax.swing.JFrame frame = new javax.swing.JFrame();
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		final AbstractDataSource source = (AbstractDataSource)proxy.getDataSource();
		final BrowserPanel browser = new BrowserPanel(frame, proxy.toString());
		frame.getContentPane().add(browser);
		frame.pack();
		frame.show();
			new Thread() {
				public void run() {
					try {
						final java.io.File file = source.getAsLocalFile(proxy);
						File f = file.getAbsoluteFile();
						String sp = f.getPath();
						if (File.separatorChar != '/')
							sp = sp.replace(File.separatorChar, '/');
						if (!sp.startsWith("/"))
							sp = "/" + sp;
						
						if (sp.startsWith("//"))
							sp = "//" + sp;
						final java.net.URL url = new java.net.URL("file", "", sp);
						browser.loadURL(url);
					} catch(java.io.IOException ioe) {
						ExceptionHandler.handleException(ioe);
					} catch(OutOfMemoryError oome) {
						JOptionPane.showMessageDialog(GenePattern.getDialogParent(), "Not enough memory available to view " + proxy);
						frame.dispose();
					}
				}
			}.start();

	}


	/**
	 *  determines if this DataObjectProxy should be viewed in the native browser
	 *
	 *@param  proxy                    Description of the Parameter
	 *@return                          Description of the Return Value
	 *@exception  java.io.IOException  Description of the Exception
	 */
	protected static boolean handleWithNativeBrowser(final DataObjectProxy proxy) throws java.io.IOException {
		final String value = GPpropertiesManager.getProperty("system.browser.handle.exts", ".html, .htm, .jsp, .asp, .jpeg, .gif, .jpg, .png, .bmp, .tiff");
		if(value.equals("NONE")) {
			return false;
		}
		final AbstractDataSource source = (AbstractDataSource)proxy.getDataSource();
		final java.io.File file = source.getAsLocalFile(proxy);

		final String ext = org.genepattern.util.ExampleFileFilter.getExtension(file);
		if(ext == null) {
			return false;
		}
		final String[] exts = StringUtils.splitStrings(value.trim().toLowerCase(), ',');
		final int limit = exts.length;
		for(int i = 0; i < limit; i++) {
			if(exts[i].endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

	// I N N E R   C L A S S E S

	/**
	 *  listens for data source updates and expands the tree with proxies and
	 *  DataType objects
	 *
	 *@author     jgould
	 *@created    March 30, 2004
	 */
	protected class DataSourceListener implements DataSourceUpdateListener {
		// end interface methods

		// fields

		/**  the source node */
		private final DefaultMutableTreeNode source_node;
		/**  the Map of models to TreeNodes that display them */
		private final Map type_nodes;
		/**  the Map of the DataObjectProxy to the TreeNode */
		private final Map proxy_node;
		/**  another DataSourceListener that gets fed nodes */
		private final DataSourceListener dependent;


		protected DataSourceListener(final GroupDataSource source) {
			this(source, null);
		}


		protected DataSourceListener(final GroupDataSource source, final DataSourceListener dependent) {
			this(new DefaultMutableTreeNode(source, true /*can have children*/), source.getParent(), dependent);
			source.addDataSourceUpdateListener(this);
		}


		private DataSourceListener(final DefaultMutableTreeNode node, final GroupDataSource parent, final DataSourceListener dependent) {
			this.source_node = node;
			this.dependent = dependent;
			type_nodes = new HashMap();
			proxy_node = new HashMap();
			if(node != root) {
				final DefaultMutableTreeNode parent_node = (parent == null) ? root : getNodeFromUserObj(parent);
				Runnable r =
					new Runnable() {
						public void run() {
							((DefaultTreeModel)data_tree.getModel()).insertNodeInto(node, parent_node, parent_node.getChildCount());
						}
					};
				SwingUtilities.invokeLater(r);
				user_node.put(node.getUserObject(), node);
			}
		}


		private DefaultMutableTreeNode getNodeFromUserObj(final Object user) {
			final DefaultMutableTreeNode node = (DefaultMutableTreeNode)user_node.get(user);
			if(node == null) {
				throw new IllegalArgumentException("User object not associated with a node!\n" + user);
			}
			return node;
		}


		/**
		 *  adds a DataObject to the tree if a node for it's type doesn't exist is
		 *  created
		 *
		 *@param  proxy  The feature to be added to the DataObject attribute
		 */
		protected void addDataObject(final DataObjectProxy proxy) {
			if(proxy == null) {
				throw new NullPointerException("The DataObjectProxy cannot be null!");
			}
			final DataModel model = proxy.getDataModel();
			addChildNode(model, proxy);
			if(dependent != null) {
				dependent.addChildNode(model, proxy);
			}
		}


		/**
		 *  adds the node
		 *
		 *@param  model  The feature to be added to the ChildNode attribute
		 *@param  proxy  The feature to be added to the ChildNode attribute
		 */
		private void addChildNode(final DataModel model, final DataObjectProxy proxy) {
			final DefaultMutableTreeNode child = new DefaultMutableTreeNode(proxy, false);
			DefaultMutableTreeNode first_order_node = getTypeNode(model);

			if(first_order_node == null) {
				first_order_node = createTypeNode(model);
			}
			final DefaultMutableTreeNode finalFirstOrderNode = first_order_node;
			for(int i = 0, children = finalFirstOrderNode.getChildCount(); i < children; i++) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) finalFirstOrderNode.getChildAt(i);
				if(node.getUserObject().equals(child)) {
					return;	
				}
			}
			
			proxy_node.put(proxy, child);
			Runnable r =
				new Runnable() {
					public void run() {
						((DefaultTreeModel)data_tree.getModel()).insertNodeInto(child, finalFirstOrderNode, finalFirstOrderNode.getChildCount());
					}
				};
			SwingUtilities.invokeLater(r);
		}

		// helper methods
		/**
		 *  gets the TreeNode keyed by the DataModel, returns null if none
		 *
		 *@param  model  Description of the Parameter
		 *@return        The typeNode value
		 */
		protected DefaultMutableTreeNode getTypeNode(final DataModel model) {
			return (DefaultMutableTreeNode)type_nodes.get(model);
		}


		/**
		 *  creates and adds a DefaultMutableTreeNode to the type nodes
		 *
		 *@param  model  Description of the Parameter
		 *@return        Description of the Return Value
		 */
		protected DefaultMutableTreeNode createTypeNode(final DataModel model) {
			final DefaultMutableTreeNode node = new DefaultMutableTreeNode(model);
			type_nodes.put(model, node);
			Runnable r =
				new Runnable() {
					public void run() {
						try {
							((DefaultTreeModel)data_tree.getModel()).insertNodeInto(node, source_node, source_node.getChildCount());
						} catch(NullPointerException npe) {}// jgould fail silently
					}
				};
			SwingUtilities.invokeLater(r);
			return node;
		}

		// DataSourceUpdateListener interface methods

		/**
		 *  called when some data objects proxies are available
		 *
		 *@param  proxies  Description of the Parameter
		 *@param  parents  Description of the Parameter
		 */
		public void updateAddDataObjects(final DataObjectProxy[] proxies, final Object[] parents) {
			final int limit = proxies.length;
			if(limit == 0) {
				return;
			}
			final SwingWorker worker =
				new SwingWorker() {
					public Object construct() {
						//System.out.println("Adding "+limit+" DataObjectProxy objs to the tree");
						for(int i = 0; i < limit; i++) {
							addDataObject(proxies[i]);
						}
						
						return null;
					}
				};
			worker.start();
		}


		/**
		 *  called when some data objects proxies have become unavailable
		 *
		 *@param  proxies  Description of the Parameter
		 *@param  parents  Description of the Parameter
		 */
		public void updateRemoveDataObjects(final DataObjectProxy[] proxies, final Object[] parents) {
			if(proxies == null || proxies.length == 0) {
				return;
			}
			// debug
			final int limit = proxies.length;
			System.out.println("removeDO num proxies=" + limit);
			for(int i = 0; i < limit; i++) {
				System.out.println("removeDO " + proxies[i]);
			}
			//end debug code

			final SwingWorker worker =
				new SwingWorker() {
					public Object construct() {
						final int limit = proxies.length;
						System.out.println("Adding " + limit + " DataObjectProxy objs to the tree");
						for(int i = 0; i < limit; i++) {
							final DataObjectProxy proxy = proxies[i];
							final DefaultMutableTreeNode node = (DefaultMutableTreeNode)proxy_node.get(proxy);
							System.out.println("removeDO: proxy=" + proxy + " associated node=" + node);
							final DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
							parent.remove(node);
							if(parent.isLeaf()) {// empty project directory
								org.genepattern.data.DataModel model = (org.genepattern.data.DataModel)parent.getUserObject();
								type_nodes.remove(model);
								if(parent.getParent() != null) {
									((DefaultTreeModel)data_tree.getModel()).removeNodeFromParent(parent);
								}
							}
						}
						((DefaultTreeModel)data_tree.getModel()).nodeStructureChanged(source_node);
						
						return null;
					}
				};
			worker.start();
		}
	}// end DataSourceListener


	/**
	 *  listens for popup requests on the tree nodes and handles Popup menu
	 *  requests
	 *
	 *@author     jgould
	 *@created    March 30, 2004
	 */
	private final class PopupMenuHandler implements MouseInputListener {

		// fields
		/**  popup menu */
		private final JPopupMenu popup;
		private final JMenuItem copy;
		private final JMenuItem remove;
		/**  the paste MenuItem */
		private final JMenuItem paste;
		/**  the refresh MenuItem */
		private final JMenuItem refresh;
		private final JMenuItem saveas;
		private JMenuItem load;
		/**  current user object of TreeNode under popup menu */
		private Object user;
		/**  the object that was copied */
		private Object copied_object;


		PopupMenuHandler() {
			popup = new JPopupMenu();
			copy = popup.add("Copy");
			paste = popup.add("Paste");
			remove = popup.add("Remove");
			refresh = popup.add("Refresh");
			saveas = popup.add("Save As...");
			load = popup.add("Reload");

			final ActionListener aal =
				new ActionListener() {
					public final void actionPerformed(final java.awt.event.ActionEvent e) {
						final Object item = e.getSource();
						if(item == load) {
							org.genepattern.gpge.io.ServerJobDataSource ds = (org.genepattern.gpge.io.ServerJobDataSource)user;
							dataObjectBrowser.loadTask(ds.getJob());
						} else if(item == copy) {
							copied_object = user;
						} else if(item == saveas) {
							dataObjectBrowser.showSaveDialog((DefaultDataObjectProxy)user);
						} else if(item == paste) {
							System.out.println("user (" + user.getClass() + ")= " + user);
							System.out.println("item from e.getSource()=" + item);
							System.out.println("copied_object=" + copied_object);
							final DirDataSource dds = (DirDataSource)user;
							final DefaultDataObjectProxy proxy = (DefaultDataObjectProxy)copied_object;
								new Thread() {
									public void run() {
										try {
											final File file = ((AbstractDataSource)proxy.getDataSource()).getAsLocalFile(proxy);
											dds.paste(file, false);
										} catch(java.io.IOException ex) {
											ExceptionHandler.handleException(ex);
										}
									}
								}.start();
						} else if(item == remove) {// remove from list of files in gp properties file
							final DefaultMutableTreeNode selected_node = (DefaultMutableTreeNode)data_tree.getSelectionPath().getLastPathComponent();
							if(selected_node.getUserObject() instanceof DirDataSource) {// remove project directory
								final DirDataSource dds = (DirDataSource)selected_node.getUserObject();
								final int choice = JOptionPane.showConfirmDialog(GenePattern.getDialogParent(),
										"Do you want to remove \""
										 + dds + "\" from the list of Project Directories?",
										"Remove", JOptionPane.OK_CANCEL_OPTION,
										JOptionPane.QUESTION_MESSAGE);
								if(choice != JOptionPane.OK_OPTION) {
									return;
								}
								final String location = dds.getLocation();
								final String dir_prop = GPpropertiesManager.getProperty("gp.project.dirs");
								int index = dir_prop.indexOf(location + ',');
								if(index >= 0) {
									final String new_prop = StringUtils.replaceAll(dir_prop, location + ',', "");
									GPpropertiesManager.setProperty("gp.project.dirs", new_prop);
								} else {
									final String new_prop = StringUtils.replaceAll(dir_prop, location, "");
									GPpropertiesManager.setProperty("gp.project.dirs", new_prop);
								}

								DataTree.this.removeDataSource(dds);
								org.genepattern.gpge.io.DataSources.instance().removeDataSource(dds);
								((DefaultTreeModel)data_tree.getModel()).removeNodeFromParent(selected_node);
							} else {// remove job
								org.genepattern.gpge.io.ServerJobDataSource ds = (org.genepattern.gpge.io.ServerJobDataSource)selected_node.getUserObject();
								org.genepattern.webservice.AnalysisJob job = ds.getJob();
								int choice = JOptionPane.showConfirmDialog(GenePattern.getDialogParent(),
										"Do you want to remove job number "
										 + job + " from your job results?",
										"Remove", JOptionPane.OK_CANCEL_OPTION,
										JOptionPane.QUESTION_MESSAGE);
								if(choice != JOptionPane.OK_OPTION) {
									return;
								}

								dataObjectBrowser.removeJob(job);

								if(selected_node.getParent().getChildCount() == 1) {
									DefaultMutableTreeNode parent = (DefaultMutableTreeNode)selected_node.getParent();
									org.genepattern.gpge.io.ServerTaskDataSource pds = (org.genepattern.gpge.io.ServerTaskDataSource)parent.getUserObject();

									((DefaultTreeModel)data_tree.getModel()).removeNodeFromParent(parent);
									dataObjectBrowser.removeTask(pds.getName());
								} else {
									((DefaultTreeModel)data_tree.getModel()).removeNodeFromParent(selected_node);
								}
							}
						} else if(item == refresh) {
							final DefaultMutableTreeNode selected_node = (DefaultMutableTreeNode)data_tree.getSelectionPath().getLastPathComponent();
							final GroupDataSource gds = (GroupDataSource)selected_node.getUserObject();
							try {
								gds.refresh();
							} catch(java.io.IOException ex) {
								ExceptionHandler.handleException(ex);
							}
						} else {
							throw new IllegalStateException("Unknown menu item " + item);
						}
					}
				};
			copy.addActionListener(aal);
			paste.addActionListener(aal);
			remove.addActionListener(aal);
			refresh.addActionListener(aal);
			saveas.addActionListener(aal);
			load.addActionListener(aal);

		}


		public final void mousePressed(final MouseEvent e) {
			if(e.isPopupTrigger()) {
				//doPopup(e, selRow, selPath);
				doPopup(e);
			}
		}


		public final void mouseReleased(MouseEvent e) {
			if(e.isPopupTrigger()) {
				doPopup(e);
			}
		}


		public final void mouseDragged(java.awt.event.MouseEvent me) { }


		public final void mouseMoved(java.awt.event.MouseEvent me) { }


		public final void mouseClicked(MouseEvent e) { }


		public final void mouseEntered(MouseEvent e) { }


		public final void mouseExited(MouseEvent e) { }
		
		private void doPopup(final MouseEvent e) {
			final int sel_row = data_tree.getRowForLocation(e.getX(), e.getY());
			data_tree.setSelectionRow(sel_row);
			final TreePath path = data_tree.getPathForLocation(e.getX(), e.getY());
			if(path == null) {
				return;
			}

			final DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
			user = node.getUserObject();
			if(user instanceof org.genepattern.gpge.io.ServerJobDataSource) {
				load.setVisible(true);
			} else {
				load.setVisible(false);
			}
			if(user.getClass().equals(DirDataSource.class)) {// should be TargetDataSource or some other like interface
				showPopup(e, false /*copy*/, true /*pastable*/, true, true);
			} else if(user instanceof DataObjectProxy) {
				showPopup(e, true /*copy*/, false /*pastable*/, false, false);
			} else if(user.getClass().equals(GroupDataSource.class)) {
				showPopup(e, false /*copy*/, false /*pastable*/, true, true);
			} else if(user.getClass().equals(org.genepattern.gpge.io.ServerJobDataSource.class)) {
				showPopup(e, false, false, false, true);
			}
		}


		private void showPopup(final MouseEvent e, final boolean copy_enabled, final boolean paste_enabled, final boolean is_data_source, boolean enableRemove) {
			final boolean have_copy = copied_object != null;
			copy.setVisible(copy_enabled);
			paste.setVisible(paste_enabled && have_copy);
			remove.setVisible(enableRemove);
			saveas.setVisible(copy_enabled);
			refresh.setText((is_data_source) ? "Refresh" : "");
			refresh.setVisible(is_data_source);

			if(have_copy) {
				paste.setText("Paste " + copied_object);
			} else {
				paste.setText("Paste");
			}
			popup.show(e.getComponent(), e.getX(), e.getY());
		}
	}// End PopupMenuHandler
}
