/*
 * DataObjectBrowser.java
 *
 * Created on February 17, 2003, 10:50 AM
 */
package org.genepattern.gpge.ui.maindisplay;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;

import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.io.AbstractDataSource;
import org.genepattern.gpge.io.DataObjectProxy;
import org.genepattern.gpge.io.DataSources;
import org.genepattern.gpge.io.DefaultDataObjectProxy;
import org.genepattern.gpge.io.ServerSiteDataSource;
import org.genepattern.gpge.ui.browser.BrowserPanel;
import org.genepattern.gpge.ui.graphics.draggable.DnDTree;
import org.genepattern.gpge.ui.preferences.PreferencesPanel;
import org.genepattern.gpge.ui.tasks.AnalysisTasksPanel;
import org.genepattern.gpge.ui.tasks.DataModel;
import org.genepattern.gpge.ui.tasks.ListTypeAdapter;
import org.genepattern.gpge.ui.tasks.PanelGenerator;
import org.genepattern.gpge.ui.tasks.RendererFactory;
import org.genepattern.gpge.ui.tasks.ServicesFilter;
import org.genepattern.gpge.ui.tasks.TaskSubmitter;
import org.genepattern.gpge.ui.tasks.UIRenderer;
import org.genepattern.modules.ui.graphics.PeriodicProgressObserver;
import org.genepattern.util.GPConstants;
import org.genepattern.util.GPpropertiesManager;
import org.genepattern.util.StringUtils;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.JobStatus;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * @author keith
 */
public class DataObjectBrowser extends javax.swing.JPanel implements
		java.util.Observer {
	final JTree src_tree;

	DataModel dataModel;

	/** the server, e.g. http://cedar:8080 */
	ServerSiteDataSource serverSiteDataSource;

	AnalysisTasksPanel myAnalysisPanel;

	MenuItemListTypeAdapter list_type;

	CardLayout card;

	ServerPanel server_panel;

	static final String task_id = "Analysis";

	static boolean promptForUserName = true;

	static String username;

	JobCompletedDialog jobCompletedDialog;

	JFileChooser saveAsFileChooser;

	static final Color AUTHORITY_MINE_COLOR = java.awt.Color.decode("0xFF00FF");

	static final Color AUTHORITY_FOREIGN_COLOR = java.awt.Color
			.decode("0x0000FF");

	private static ParameterInfo copyParameterInfo(ParameterInfo toClone) {
		ParameterInfo pi = new ParameterInfo(toClone.getName(), toClone
				.getValue(), toClone.getDescription());
		HashMap attrs = toClone.getAttributes();
		if (attrs != null) {
			attrs = (HashMap) attrs.clone();
		} else {
			attrs = new HashMap(1);
		}
		pi.setAttributes(attrs);
		return pi;
	}

	public void disconnectedFromServer() {
		this.disconnectFromServer();
	}

	private void refresh() {
		promptForUserName = false;
		Thread refreshThread = new Thread() {
			public void run() {
				DataSources.reset(); // clear data sources
				javax.swing.JFrame f = (javax.swing.JFrame) getTopLevelAncestor();
				f.setMenuBar(null);
				f.getContentPane().remove(DataObjectBrowser.this);
				DataObjectBrowser browser = new DataObjectBrowser(f);
				f.getContentPane().add(browser);
				f.setJMenuBar(browser.getMenuBar());
				f.invalidate();
				f.validate();
			}
		};
		javax.swing.SwingUtilities.invokeLater(refreshThread);
	}

	// call if the server has disconnected us
	public void disconnectFromServer() {
		try {

			org.genepattern.gpge.io.DataSources.instance().removeDataSource(
					serverSiteDataSource);
			data_tree.removeDataSource(serverSiteDataSource);
			data_tree.disconnectedFromServer();

			serverSiteDataSource = null;
			System.out.println("Disconnected from server");

			Properties prop = org.genepattern.util.PropertyFactory
					.getInstance().getProperties("omnigene.properties");
			final String site_name = prop
					.getProperty("analysis.service.site.name");
			analysis_menu.setEnabled(false);
			visualizer_menu.setEnabled(false);
			message_textField.setText("Disconnected from Server " + site_name);
			((CardLayout) server_panel.getLayout()).show(server_panel, "log");
			GenePattern
					.showWarning(
							this,
							"The server "
									+ site_name
									+ " has disconected.\nPlease restart the server if necessary and then restart the client.");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public AnalysisService getAnalysisService(String lsidOrTaskName) {
		if (lsidOrTaskName == null) {
			return null;
		}
		AnalysisService service = (AnalysisService) list_type.name_object
				.get(lsidOrTaskName);
		if (service == null) {
			try {
				Properties prop = org.genepattern.util.PropertyFactory
						.getInstance().getProperties("omnigene.properties");
				String site_name = prop
						.getProperty("analysis.service.site.name");

				TaskInfo task = new org.genepattern.webservice.AdminProxy(
						site_name, username, false).getTask(lsidOrTaskName); // old
																			 // servers
																			 // don't
																			 // have
																			 // this
																			 // method
				service = new AnalysisService(site_name, task);
			} catch (Throwable t) {
			}
		}
		return service;
	}

	void loadAnalysisService(AnalysisService svc) {
		myAnalysisPanel.loadTask(svc);
		card.show(server_panel, task_id);
	}

	/**
	 * Loads a task with the parameters that were used in the specified job into
	 * the AnalysisTaskPanel
	 * 
	 * @param job
	 *            the job
	 */
	public void loadTask(AnalysisJob job) {
		String taskName = job.getTaskName();
		String lsid = job.getLSID();
		String key = lsid != null ? lsid : taskName;
		//this won't reload old jobs b/c they have no lsid
		AnalysisService service = (AnalysisService) list_type.name_object
				.get(key);

		if (service == null) {
			if (lsid != null) {
				service = getAnalysisService(lsid); // see if old version of
													// task exists
			}
			if (service == null) { // get task by name
				service = getAnalysisService(taskName);
			}
			if (service == null) {
				JOptionPane.showMessageDialog(GenePattern.getDialogParent(),
						taskName + " does not exist.");
				return;
			}
		}

		TaskInfo task = service.getTaskInfo();
		org.genepattern.webservice.JobInfo savedJobInfo = job.getJobInfo();
		ParameterInfo[] savedParameters = savedJobInfo.getParameterInfoArray();

		ParameterInfo[] formalParams = task.getParameterInfoArray();
		java.util.List actualParams = new java.util.ArrayList();
		Map savedParamName2Param = new HashMap();
		for (int i = 0; savedParameters != null && i < savedParameters.length; i++) {
			if (!savedParameters[i].isOutputFile()) {
				savedParamName2Param.put(savedParameters[i].getName(),
						savedParameters[i]);
			}
		}
		StringBuffer errorMessage = new StringBuffer();
		if (formalParams != null) {
			Map formalParamName2Param = new HashMap(formalParams.length);
			for (int i = 0, length = formalParams.length; i < length; i++) {
				formalParamName2Param.put(formalParams[i].getName(),
						formalParams[i]);
			}

			for (int i = 0, length = formalParams.length; i < length; i++) {
				// check to see that the saved parameters are the same as the
				// parameters for the current installed task

				ParameterInfo formalParameterInfo = formalParams[i];
				String sOptional = (String) formalParameterInfo.getAttributes()
						.get(GPConstants.PARAM_INFO_OPTIONAL[0]);
				boolean optional = (sOptional != null && sOptional.length() > 0);
				ParameterInfo savedParameterInfo = (ParameterInfo) savedParamName2Param
						.get(formalParams[i].getName());

				String sDefault = (String) formalParameterInfo.getAttributes()
						.get(GPConstants.PARAM_INFO_DEFAULT_VALUE[0]);

				if (savedParameterInfo == null && !optional) { // XXX do a more
															   // stringent
															   // check
					errorMessage.append(formalParameterInfo.getName()
							+ " seems to be a new or renamed parameter.\n");
					ParameterInfo actualParameterInfo = copyParameterInfo(formalParameterInfo);
					actualParams.add(actualParameterInfo);
					continue;
				}
				String actualValue = null; // the value to set the parameter for
										   // the job we are about to submit

				if (savedParameterInfo != null) { // saved parameter exists in
												  // installed task
					savedParamName2Param.remove(savedParameterInfo.getName());
					if (savedParameterInfo.isOutputFile()) {
						continue;
					}
					if (ParameterInfo.CACHED_INPUT_MODE
							.equals(savedParameterInfo.getAttributes().get(
									ParameterInfo.MODE))) { //  input file is
															// result of
															// previous job on
															// server
						String fileNameOnServer = savedParameterInfo.getValue();
						ParameterInfo pi = new ParameterInfo(savedParameterInfo
								.getName(), "", savedParameterInfo
								.getDescription());
						HashMap attrs = new HashMap(1);
						pi.setAttributes(attrs);
						pi.getAttributes().put(
								GPConstants.PARAM_INFO_DEFAULT_VALUE[0],
								fileNameOnServer);
						pi.setAsInputFile();
						actualParams.add(pi);
						continue;
					} else if (savedParameterInfo.isInputFile()) { // input file
																   // is local
																   // file
						actualValue = (String) savedParameterInfo
								.getAttributes()
								.get(GPConstants.PARAM_INFO_CLIENT_FILENAME[0]);
					} else {
						actualValue = savedParameterInfo.getValue();
					}
				}

				if (actualValue == null) { // new parameter in installed task
					if (sDefault != null && sDefault.indexOf(";") != -1) {
						actualValue = sDefault; // use default value for
												// installed param
					} else {
						actualValue = formalParameterInfo.getValue();
					}

				}
				if (actualValue != null) {
					ParameterInfo submitParam = copyParameterInfo(formalParameterInfo);
					submitParam.getAttributes().put(
							GPConstants.PARAM_INFO_DEFAULT_VALUE[0],
							actualValue);
					actualParams.add(submitParam);
				}
			}
		}

		if (savedParamName2Param.size() > 1) { // whatever is left is an
											   // un-recycled parameter. Let the
											   // user know.
			errorMessage.append("Ignoring now-unused parameters ");
		} else if (savedParamName2Param.size() == 1) {
			errorMessage.append("Ignoring now-unused parameter ");
		}

		for (Iterator iUnused = savedParamName2Param.keySet().iterator(); iUnused
				.hasNext();) {
			errorMessage.append(iUnused.next() + "\n");
		}

		if (errorMessage.length() > 0) {
			JOptionPane.showMessageDialog(GenePattern.getDialogParent(),
					errorMessage.toString());
		}
		TaskInfo taskCopy = new TaskInfo(task.getID(), task.getName(), task
				.getDescription(), task.getParameterInfo(), task.giveTaskInfoAttributes(), task
				.getUserId(), task.getAccessId());
		taskCopy.setParameterInfoArray((ParameterInfo[]) actualParams
				.toArray(new ParameterInfo[0]));
		AnalysisService serviceCopy = new AnalysisService(service.getServer(),
				taskCopy);
		myAnalysisPanel.loadTask(serviceCopy);
		card.show(server_panel, task_id);
		System.out.println("actualParams " + actualParams);

	}

	public void showSaveDialog(final DataObjectProxy proxy) {
		final File initiallySelectedFile = new File(proxy.toString());
		saveAsFileChooser.setSelectedFile(initiallySelectedFile);

		if (saveAsFileChooser.showSaveDialog(DataObjectBrowser.this) == JFileChooser.APPROVE_OPTION) {
			final File outputFile = saveAsFileChooser.getSelectedFile();
			if (outputFile.exists()) {
				String message = "An item named "
						+ outputFile.getName()
						+ " already exists in this location. Do you want to replace it with the one that you are saving?";
				if (JOptionPane.showOptionDialog(GenePattern.getDialogParent(),
						message, null, JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE, null, new Object[] {
								"Replace", "Cancel" }, "Cancel") != JOptionPane.YES_OPTION) {
					return;
				}

			}

			new Thread() {
				public void run() {
					try {
						File sourceFile = ((AbstractDataSource) proxy
								.getDataSource()).getAsLocalFile(proxy);
						FileInputStream in = new FileInputStream(sourceFile);
						org.genepattern.io.StorageUtils.writeToFile(outputFile,
								in);
						in.close();
					} catch (Exception e) {
						GenePattern.showError(DataObjectBrowser.this,
								"Error saving file", e);
					}
				}
			}.start();
		}
	}

	/**
	 * Creates new form DataObjectBrowser this constuctor is just for the GUI
	 * builders
	 * 
	 * @see DataObjectBrowser(String)
	 */
	public DataObjectBrowser() {
		initComponents();
		saveAsFileChooser = new JFileChooser() {
			public void setSelectedFile(File f) {
				if (f != null && !f.isDirectory()) {
					super.setSelectedFile(f);
				} else {
					super.setSelectedFile(null);
				}
			}
		};
		saveAsFileChooser.setFileHidingEnabled(true); // don't show hidden files
		final JTree classic_tree = new DnDTree();
		classic = new DataTree(classic_tree);
		classic.dataObjectBrowser = this;
		// tree_TabbedPane.addTab("Data Objects", new TreePanel(classic_tree));

		src_tree = new DnDTree() {
			public void processMouseEvent(java.awt.event.MouseEvent e) {
				try {
					super.processMouseEvent(e);
				} catch (Exception ex) {
				} // fail silently
			}
		};

		src_tree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(final TreeSelectionEvent e) {
				projectTreeSelectionChanged();
			}
		});

		saveAsMenuItem = new JMenuItem("Save As...");
		saveAsMenuItem.setEnabled(false);
		saveAsMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) src_tree
						.getLastSelectedPathComponent();
				DefaultDataObjectProxy proxy = (DefaultDataObjectProxy) node
						.getUserObject();
				showSaveDialog(proxy);
			}
		});

		data_tree = new DataTree(src_tree, classic, true);
		data_tree.dataObjectBrowser = this;
		tree_TabbedPane.addTab("Data", new TreePanel(src_tree));
		// tree_TabbedPane.setSelectedIndex(1);

	}

	public void removeJob(AnalysisJob job) {
		try {
			dataModel.removeJobById(job);
		} catch (IllegalArgumentException e) {
		} // job no longer is in DataModel FIXME why??
		persistJobs();
	}

	public void removeTask(String taskName) {
		serverSiteDataSource.removeServerTaskDataSource(taskName);
	}

	private void projectTreeSelectionChanged() {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) src_tree
				.getLastSelectedPathComponent();
		if (node == null) {
			saveAsMenuItem.setEnabled(false);
			return;
		}
		final Object nodeInfo = node.getUserObject();
		if (nodeInfo instanceof DataObjectProxy) {
			saveAsMenuItem.setEnabled(true);
		} else {
			saveAsMenuItem.setEnabled(false);
		}
	}

	/**
	 * Creates new form DataObjectBrowser this constuctor is just for the GUI
	 * builders
	 * 
	 * @see DataObjectBrowser(String)
	 */
	public DataObjectBrowser(final javax.swing.JFrame parent) {
		this();
		//load_monitor = new PeriodicProgressObserver(parent, "Loading Project
		// Directory", 1, true, 50, false);
		this.setMinimumSize(new java.awt.Dimension(500, 400));
		// Data Analysis Menu
		analysis_menu = new JMenu("Data Analysis");
		final LogDisplayAction log_action = new LogDisplayAction('L');
		analysis_menu.add(log_action);
		analysis_menu.addSeparator();

		visualizer_menu = new JMenu("Visualizers");

		final PropertiesTable summarypropertiesTable = new PropertiesTable();
		infoScrollPane.setViewportView(summarypropertiesTable);
		try {
			data_tree.addDataSelectListener(summarypropertiesTable);
			classic.addDataSelectListener(summarypropertiesTable);
		} catch (java.util.TooManyListenersException ex) {
			GenePattern.showError(this,
					"Could not connect the listeners to the views (UI broken)",
					ex);
		}
		//sources.addDataSourceUpdateListener(data_tree);
		tree_TabbedPane.addChangeListener(new ChangeListener() {
			public final void stateChanged(ChangeEvent e) {
				if (tree_TabbedPane.getSelectedIndex() == 0) {
					data_tree.reselect();
				} else {
					summarypropertiesTable.clearTable();
				}

				if (tree_TabbedPane.getSelectedIndex() == 1) {
					projectTreeSelectionChanged();
				} else {
					saveAsMenuItem.setEnabled(false);
				}

			}
		});

		new Thread(new org.genepattern.util.RunLater() {
			public final void runIt() {
				Thread.yield();
				init(log_action, parent);
			}
		}).start();
	}

	private void init(final LogDisplayAction log_action,
			final javax.swing.JFrame parent) {

		// read our gp properties and load in the Project Directories
		jobCompletedDialog = new JobCompletedDialog();
		/*
		 * String showCompletedJobsDialog =
		 * GPpropertiesManager.getProperty(JobCompletedDialog.SHOW_DIALOG_PROPERTY);
		 * if(showCompletedJobsDialog==null) {
		 * GPpropertiesManager.setProperty(JobCompletedDialog.SHOW_DIALOG_PROPERTY,
		 * "true"); }
		 */

		final String directories = GPpropertiesManager
				.getProperty("gp.project.dirs");

		GPpropertiesManager.setProperty("gp.project.dirs", ""); // clear it
		System.out.println("Directories property=" + directories);
		final org.genepattern.gpge.io.DataSources sources = org.genepattern.gpge.io.DataSources
				.instance();
		if (directories != null && directories.trim().length() > 0) {
			final String[] dirs = StringUtils.splitStrings(directories, ',');

			// must have correct count right away
			final int real_count = StringUtils.countNonEmpty(dirs);
			final int limit = dirs.length;
			System.out.println("There are " + real_count + " directories:");
			final String mon_label1 = "Loading ";
			final String mon_label2 = " project directories";
			final PeriodicProgressObserver monitor = new PeriodicProgressObserver(
					parent, mon_label1 + limit + mon_label2, real_count, false,
					100, true);
			monitor.start();

			new Thread(new org.genepattern.util.RunLater() {
				public final void runIt() {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
					}
					try {
						for (int i = 0; i < limit; i++) {
							final String dir = dirs[i];
							if (dir == null || dir.trim().length() == 0) {
								continue;
							}
							System.out.println(dir);
							Exception exception = null;
							try {
								monitor.comment("Loading (" + (i + 1) + "/"
										+ limit + ") " + dir.toString());
								File projectDir = new java.io.File(dir);
								if (projectDir.exists()) {
									sources.addDirectory(projectDir, data_tree,
											monitor);
								} else {
									JOptionPane
											.showMessageDialog(
													GenePattern
															.getDialogParent(),
													"Unable to load project directory "
															+ projectDir
																	.getCanonicalPath(),
													"Error",
													JOptionPane.ERROR_MESSAGE);
								}
							} catch (java.io.IOException ex) {
								exception = ex;
							} catch (java.text.ParseException ex) {
								exception = ex;
							} catch (RuntimeException ex) {
								exception = ex;
							}
							if (exception != null) {
								GenePattern.showError(DataObjectBrowser.this,
										"Couldn't load the directory +" + dir,
										exception);
							}

						}
					} finally {
						try {// pregnant pause
							Thread.sleep(500);
						} catch (InterruptedException e) {
						}
						update(dataModel, DataModel.OBSERVER_DATA);
						monitor.finnish();
						System.out.println("End dirs");
						sources.startPeriodicUpdate();
					}
					repaint();

				}
			}).start();
		}
		dataModel = loadData();
		//this can be slow so run it in a thread (but not a swing thread)...
		new Thread(new org.genepattern.util.RunLater() {
			public final void runIt() {
				try {
					final Properties prop = org.genepattern.util.PropertyFactory
							.getInstance().getProperties("omnigene.properties");

					int n = 0;
					// FIXME add loop here to connect to each server
					final String site_name = prop
							.getProperty("analysis.service.site.name");
					final ServerPanel server_panel = new ServerPanel(n++);
					serverTabbedPane.addTab("Loading...", server_panel);

					connectTo(site_name, server_panel, dataModel, log_action);

					// done after setting status variable since connected at
					// this point
					// will create the ServerSiteDataSource instance
					serverSiteDataSource = sources.setAnalysisDataModel(
							site_name, dataModel, data_tree);
					data_tree.removeEmptyJobNodes();
					// end loop
				} catch (IOException ioe) {
					GenePattern.showError(DataObjectBrowser.this, ioe
							.getMessage());
				} catch (org.genepattern.webservice.PropertyNotFoundException pnfe) {
					GenePattern.showError(DataObjectBrowser.this, pnfe
							.getMessage());
				} catch (org.genepattern.webservice.WebServiceException wse) {
					Throwable rootCause = wse.getRootCause();

					if (rootCause != null
							&& rootCause instanceof java.net.ConnectException) {

						GenePattern
								.showWarning(
										DataObjectBrowser.this,
										"Unable to connect to server. Please start the server and then restart the client.\n\nCause: "
												+ rootCause);

					} else {
						GenePattern.showError(DataObjectBrowser.this, wse
								.getMessage());
					}
				}
			}
		}).start();

	}

	/**
	 * connects to the server specified in properties files in the resource_path
	 * 
	 * @param resource_path
	 *            where to fine the properties files
	 * @return a PanelGenerator
	 */
	protected final PanelGenerator connectTo(final String site_name,
			final ServerPanel _server_panel,
			final org.genepattern.gpge.ui.tasks.DataModel data_model,
			final LogDisplayAction log_action)
			throws org.genepattern.webservice.PropertyNotFoundException,
			org.genepattern.webservice.WebServiceException {
		final String log_id = "log";
		server_panel = _server_panel;
		card = (CardLayout) server_panel.getLayout();
		PanelGenerator panel_gen = null;
		boolean worked = false;
		String host = null, s_name = null;
		try {
			//Start the OmniView stuff
			final MyVisualizerSubmitter vis_task_submitter = new MyVisualizerSubmitter(
					DataObjectBrowser.this);
			final String server_name = vis_task_submitter.host + ':'
					+ vis_task_submitter.port;
			s_name = server_name;
			host = vis_task_submitter.host;
			serverTabbedPane.setTitleAt(server_panel.getIndex(),
					"Connecting to " + server_name);
			message_textField.setText("Connecting to server, " + server_name
					+ "...");

			list_type = new MenuItemListTypeAdapter() {
				public final void actionPerformed(
						final java.awt.event.ActionEvent actionEvent) {
					final JMenuItem item = (JMenuItem) actionEvent.getSource();
					item_selected = item;
					card.show(server_panel, task_id);

				}
			};
			final int polling_delay = GPpropertiesManager.getIntProperty(
					"gp.polling.server.millisecs", 3000);

			if (promptForUserName) {
				username = GPpropertiesManager.getProperty("gp.user.name");
				username = (String) JOptionPane
						.showInputDialog(
								GenePattern.getDialogParent(),
								"Please enter your username to identify task ownership.\nWe recommend using your email address.",
								"Username", JOptionPane.QUESTION_MESSAGE, null,
								null, username);
				if (username == null || username.trim().equals("")) {
					username = "anonymous";
				}
				GPpropertiesManager.setProperty("gp.user.name", username);
			}
			try {
				String lsidAuthority = (String) new org.genepattern.webservice.AdminProxy(
						site_name, username, false).getServiceInfo().get(
						"lsid.authority");
				System.setProperty("lsid.authority", lsidAuthority);
			} catch (Throwable x) {
			}

			/*
			 * try { jgould- should save properties now but for some reason
			 * doing this doesn't persist changes to project directories
			 * GPpropertiesManager.saveGenePatternProperties(); }
			 * catch(java.io.IOException ioe) { ioe.printStackTrace(); }
			 */
			String password = null;

			panel_gen = new PanelGenerator(data_model, services_filter,
					list_type, (java.awt.event.ActionListener) list_type,
					new TaskSubmitter[] { vis_task_submitter },
					DefaultExceptionHandler.instance(), polling_delay,
					username, password);

			panel_gen.getHistoryPanel().setSiteName(site_name);
			panel_gen.getHistoryPanel().update(panel_gen.getDataModel(),
					DataModel.OBSERVER_DATA);
			panel_gen.getDataModel().addObserver(DataObjectBrowser.this);
			log_action.setContainerAndLabel(server_panel, log_id);

			server_panel.add(panel_gen.getHistoryPanel(), log_id);

			myAnalysisPanel = panel_gen.getAnalysisTasksPanel(); // jgould

			final AnalysisTasksPanel analysis_panel = panel_gen
					.getAnalysisTasksPanel();
			server_panel.add(analysis_panel, task_id);
			analysis_panel.init(this, site_name);
			analysis_panel.addRendererFactory(new RendererFactory() {
				/**
				 * returns an UIRenderer array for rendering the params or null
				 * if couldn't process any params. After returning the input
				 * java.util.List will contain any remaining ParameterInfo
				 * objects that were not processed. Note the params can be run
				 * through the next RendererFactory to produce more Renderers.
				 *  
				 */
				public UIRenderer createRenderer(final AnalysisService service,
						java.util.List params) {
					return new DefaultUIRenderer();
				}
			});

			//Connect the analysis data to the Data Source
			final org.genepattern.gpge.ui.tasks.DataModel analysis_model = panel_gen
					.getDataModel();

			worked = true;
		} finally {
			analysis_menu.setEnabled(true);
			visualizer_menu.setEnabled(true);
			if (worked) {
				message_textField.setText("Connected to " + s_name);
			} else
				message_textField.setText("Failed to connect to " + s_name);
			serverTabbedPane.invalidate();
			serverTabbedPane.setTitleAt(server_panel.getIndex(), s_name);
			serverTabbedPane.validate();

			card.first(server_panel);
			server_tab_panel.repaint();
		}

		return panel_gen;
	}

	/**
	 * saves the job results to disk
	 */
	public synchronized void persistJobs() {
		final String anal_data_file_name = GPpropertiesManager
				.getProperty("gp.analysis.jobs.file");
		File anal_data_file = null;
		if (anal_data_file_name == null || anal_data_file_name.length() == 0) {
			anal_data_file = new File(GPpropertiesManager.GP_HOME,
					"analysis_jobs.xml");
			GPpropertiesManager.setProperty("gp.analysis.jobs.file",
					anal_data_file.getAbsolutePath());
		} else {
			anal_data_file = new File(anal_data_file_name);
		}

		final String data_model_file_name = anal_data_file.getAbsolutePath();
		final File save_data_file = new File(data_model_file_name + "_new");
		if (save_data_file.exists())
			save_data_file.delete();
		try {
			DataHandlerOmniView.saveData(dataModel, save_data_file);
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}

		if (anal_data_file.exists())
			anal_data_file.delete(); // so delete it first
		save_data_file.renameTo(anal_data_file);
	}

	/** loads in the data model */
	protected final org.genepattern.gpge.ui.tasks.DataModel loadData() {
		// load the analysis jobs
		final String anal_data_file_name = GPpropertiesManager
				.getProperty("gp.analysis.jobs.file");
		File anal_data_file = null;
		if (anal_data_file_name == null || anal_data_file_name.length() == 0) {
			anal_data_file = new File(GPpropertiesManager.GP_HOME,
					"analysis_jobs.xml");
			GPpropertiesManager.setProperty("gp.analysis.jobs.file",
					anal_data_file.getAbsolutePath());
		} else {
			anal_data_file = new File(anal_data_file_name);
		}
		final File analysis_data_file = anal_data_file;
		org.genepattern.gpge.ui.tasks.DataModel dat_model = null;

		try {
			if (anal_data_file.exists()) {
				dat_model = DataHandlerOmniView.loadData(anal_data_file);
			}
		} catch (Exception e) {
			GenePattern.logWarning("Could not load analysis jobs file \""
					+ anal_data_file + "\" due to error:\n" + e);
		} catch (NoClassDefFoundError e) {
			GenePattern
					.showError(
							DataObjectBrowser.this,
							"Could not find a class.\n"
									+ "Some library must be missing or not on the class path.",
							e);
			System.err.println("****\n\n Could not find class "
					+ e.getMessage());
			e.printStackTrace();
			System.err.println("\n****");
			// edu.mit.genome.debug.JWhich.which(e.getMessage());
		}
		if (dat_model == null) {
			System.out.println("     DataModel NOT loaded!");
			dat_model = new org.genepattern.gpge.ui.tasks.DataModel();
		}
		return dat_model;
	}

	JMenu fileMenu;

	/** gets the menu for the enclosing Frame, JFrame, JInterFrame, etc. */
	public JMenuBar getMenuBar() {
		final JMenuBar menuBar = new JMenuBar();
		// File
		fileMenu = new JMenu("File");

		menuBar.add(fileMenu);

		fileMenu.add(new OpenAction());

		final javax.swing.JCheckBoxMenuItem showJobCompletedDialogMenuItem = new javax.swing.JCheckBoxMenuItem(
				"Alert On Job Completion");
		showJobCompletedDialogMenuItem.setSelected(jobCompletedDialog
				.isShowingDialog());
		fileMenu.add(showJobCompletedDialogMenuItem);
		showJobCompletedDialogMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jobCompletedDialog.setShowDialog(showJobCompletedDialogMenuItem
						.isSelected());
			}
		});
		fileMenu.add(new javax.swing.AbstractAction("Server...") {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				javax.swing.JFrame f = (javax.swing.JFrame) getTopLevelAncestor();
				new org.genepattern.gpge.ui.preferences.ChangeServerDialog(f);

			}
		});

		fileMenu.add(new javax.swing.AbstractAction("Refresh") {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				refresh();
			}
		});

		fileMenu.add(saveAsMenuItem);
		fileMenu.add(new QuitAction());

		JMenu editMenu = new JMenu("Edit");
		Action action = new SmartCutAction();
		action.setEnabled(false);
		editMenu.addMenuListener((SmartCutAction) action);
		action.putValue(Action.NAME, "Cut");
		editMenu.add(action);
		action = new SmartCopyAction();
		action.setEnabled(false);
		editMenu.addMenuListener((SmartCopyAction) action);
		action.putValue(Action.NAME, "Copy");
		editMenu.add(action);
		action = new SmartPasteAction();
		action.setEnabled(false);
		editMenu.addMenuListener((SmartPasteAction) action);
		action.putValue(Action.NAME, "Paste");
		editMenu.add(action);

		// Data Analysis
		//already taken care of in the constructor

		menuBar.add(analysis_menu);
		analysis_menu.setEnabled(false);

		// Visualizer menu
		menuBar.add(visualizer_menu);
		visualizer_menu.setEnabled(false);

		// Help
		JMenu helpMenu = new JMenu("Help");
		try {
			menuBar.setHelpMenu(helpMenu);
		} catch (Throwable ex) { // setHelpMenu is not implemented on
			menuBar.add(helpMenu); // some platform/Java versions
		}
		helpMenu.add(new AboutAction());

		JMenuItem moduleColorKeyMenuItem = new JMenuItem("Module Color Key");
		helpMenu.add(moduleColorKeyMenuItem);
		moduleColorKeyMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JPanel p = new JPanel();
				p.setLayout(new GridLayout(3, 1));
				JLabel colorKeyLabel = new JLabel("color key:");
				JLabel yourTasksLabel = new JLabel("your tasks");
				yourTasksLabel.setForeground(AUTHORITY_MINE_COLOR);
				JLabel broadTasksLabel = new JLabel("Broad tasks");

				JLabel otherTasksLabel = new JLabel("other tasks");
				otherTasksLabel.setForeground(AUTHORITY_FOREIGN_COLOR);

				p.add(yourTasksLabel);
				p.add(broadTasksLabel);
				p.add(otherTasksLabel);
				Container c = new JPanel();
				c.setLayout(new BorderLayout());
				// c.add(colorKeyLabel, BorderLayout.NORTH);
				c.add(p, BorderLayout.CENTER);

				javax.swing.JScrollPane sp = new javax.swing.JScrollPane(c);
				javax.swing.JOptionPane.showMessageDialog(GenePattern
						.getDialogParent(), sp, "Module Color Key",
						JOptionPane.INFORMATION_MESSAGE);
			}
		});

		helpMenu.add(new ErrorMsgsAction());
		helpMenu.add(new WarnMsgsAction());
		final String url_string = GPpropertiesManager
				.getProperty("gp.help.ref_guide");
		if (url_string != null && url_string.length() > 0)
			helpMenu.add(new ViewGuideAction());
		return menuBar;
	}

	/** opens a dir dialog */
	protected void openDirDialog() throws java.io.IOException {
		JFileChooser file_chooser = new JFileChooser();
		file_chooser.setDialogTitle("Choose a Project Directory");
		file_chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		file_chooser.setApproveButtonText("Select Directory");
		try {
			if (GPpropertiesManager
					.getBooleanProperty("gp.project.dialog.show_files")) {
				if (file_chooser.getAccessory() == null)
					file_chooser
							.setAccessory(new org.genepattern.gpge.ui.maindisplay.DirPreview(
									file_chooser));
			} else {
				if (file_chooser.getAccessory() != null)
					file_chooser.setAccessory(null);
			}
		} catch (java.text.ParseException ex) {
			GenePattern.logWarning(ex.getMessage());
		}
		// Note: source for ExampleFileFilter can be found in FileChooserDemo,
		// under the demo/jfc directory in the Java 2 SDK, Standard Edition.
		//        ExampleFileFilter filter = new ExampleFileFilter();
		//        filter.addExtension("jpg");
		//        filter.addExtension("gif");
		//        filter.setDescription("JPG & GIF Images");
		//        chooser.setFileFilter(filter);

		final int returnVal = file_chooser.showDialog(this,
				"Select Project Directory");

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			final java.io.File file = file_chooser.getSelectedFile();
			System.out.println("You chose to open this file: " + file);
			if (file != null) {

				final PeriodicProgressObserver loadProjectMonitor = new PeriodicProgressObserver(
						getTopLevelAncestor(), "Loading Project Directory", 1,
						false, 100, true);
				loadProjectMonitor.start();
				new Thread(new org.genepattern.util.RunLater() {
					public final void runIt() throws Exception {
						boolean ok = false;
						try {
							org.genepattern.gpge.io.DataSources sources = org.genepattern.gpge.io.DataSources
									.instance();
							sources.addDirectory(file, data_tree,
									loadProjectMonitor); // added to data tree
							ok = true;

							// tree_TabbedPane.setSelectedIndex(1); // change to
							// Project Dirs tab
							// open new node in tree
							//jg
						} catch (java.text.ParseException ex) {
							GenePattern.logWarning(ex.getMessage());
						} catch (org.genepattern.util.Warning warning) {
							GenePattern.logWarning(warning.getMessage());
							setMessage(warning.getMessage());
						} finally {
							loadProjectMonitor.finnish();

						}
					}
				}).start();
			}

		}

	}

	/** sets the message */
	public final void setMessage(final String text) {
		if (text != null)
			message_textField.setText(text);
		else
			message_textField.setText("");
	}

	/** gets the current message */
	public final String getMessage() {
		return message_textField.getText();
	}

	// interface Observer method
	/**
	 * omniview.analysis.DataModel calls this whenever there is a change to the
	 * model
	 */
	public void update(java.util.Observable observable, Object arg) {
		System.out
				.println("DataObjectBrowser was notified of an update from DataModel");
		final org.genepattern.gpge.ui.tasks.DataModel analysis_model = (org.genepattern.gpge.ui.tasks.DataModel) observable;

		if (arg != null)
			System.out.println("arg is a " + arg.getClass());
		if (arg instanceof DataModel.JobAndObserver) {
			final DataModel.JobAndObserver jao = (DataModel.JobAndObserver) arg;
			final String task_name = jao.job.getTaskName();

			System.out.println("JOB is a " + task_name);

			final String status = jao.job.getJobInfo().getStatus();
			this.message_textField.setText("Task: " + task_name + "  Status: "
					+ status);
			if (status.equals(JobStatus.FINISHED)
					|| status.equals(JobStatus.ERROR)) {
				new Thread() {
					public final void run() {
						persistJobs();
						jobCompletedDialog.add(jao.job.getJobInfo()
								.getJobNumber(), task_name, status);
					}
				}.start();
			}
		}

	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	private void initComponents() {//GEN-BEGIN:initComponents
		jSplitPane = new javax.swing.JSplitPane();
		browser_SplitPane = new javax.swing.JSplitPane();
		tree_TabbedPane = new javax.swing.JTabbedPane();
		infoScrollPane = new javax.swing.JScrollPane();
		server_tab_panel = new javax.swing.JPanel();
		serverTabbedPane = new javax.swing.JTabbedPane();
		message_textField = new javax.swing.JTextField();

		setLayout(new java.awt.BorderLayout());

		setPreferredSize(new java.awt.Dimension(600, 200));
		setMinimumSize(new java.awt.Dimension(600, 200));
		jSplitPane.setPreferredSize(new java.awt.Dimension(0, 0));
		jSplitPane.setMinimumSize(new java.awt.Dimension(510, 620));
		browser_SplitPane.setResizeWeight(0.75);
		browser_SplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
		browser_SplitPane.setPreferredSize(new java.awt.Dimension(0, 0));
		browser_SplitPane.setMinimumSize(new java.awt.Dimension(300, 510));
		// tree_TabbedPane.setTabPlacement(javax.swing.JTabbedPane.BOTTOM);
		tree_TabbedPane.setPreferredSize(new java.awt.Dimension(0, 0));
		tree_TabbedPane.setMinimumSize(new java.awt.Dimension(100, 300));
		browser_SplitPane.setLeftComponent(tree_TabbedPane);

		infoScrollPane.setPreferredSize(new java.awt.Dimension(0, 0));
		infoScrollPane.setMinimumSize(new java.awt.Dimension(100, 200));
		browser_SplitPane.setRightComponent(infoScrollPane);

		jSplitPane.setLeftComponent(browser_SplitPane);

		server_tab_panel.setLayout(new java.awt.BorderLayout());

		server_tab_panel.setPreferredSize(new java.awt.Dimension(0, 0));
		server_tab_panel.setMinimumSize(new java.awt.Dimension(100, 100));
		server_tab_panel.add(serverTabbedPane, java.awt.BorderLayout.CENTER);

		jSplitPane.setRightComponent(server_tab_panel);

		add(jSplitPane, java.awt.BorderLayout.CENTER);

		message_textField.setEditable(false);
		add(message_textField, java.awt.BorderLayout.SOUTH);

	}//GEN-END:initComponents

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JSplitPane browser_SplitPane;

	private javax.swing.JScrollPane infoScrollPane;

	private javax.swing.JSplitPane jSplitPane;

	private javax.swing.JTextField message_textField;

	private javax.swing.JTabbedPane serverTabbedPane;

	private javax.swing.JPanel server_tab_panel;

	private javax.swing.JTabbedPane tree_TabbedPane;

	// End of variables declaration//GEN-END:variables

	// Fields
	JMenuItem saveAsMenuItem;

	/** the open file Dialog */
	private final DataTree data_tree;

	/** the class that handles the the classic view of the Tree of data objects */
	private final DataTree classic;

	/** the analysis menu */
	private JMenu analysis_menu;

	/** the Visualizer menu */
	private JMenu visualizer_menu;

	/** filters out the non-gp services */
	private final ServicesFilter services_filter = new ServicesFilter() {
		/** returns the Vector with only the services of interest */
		public java.util.Vector processServices(final java.util.Vector services) {
			
			java.util.Collections.sort(services, ServicesFilter.COMPARE);
			return services;
		}
	};

	// I N N E R C L A S S E S

	/** open action */
	class OpenAction extends
			org.genepattern.modules.ui.listeners.ReportErrorAbstractAction {

		OpenAction() {
			super("Open Project Directory...", 'O');
		}

		/**
		 * subclasses will implement this method to know when an event occured
		 * This is pertty much the only method that the developer needs to
		 * bother with
		 *  
		 */
		protected final void doAction(java.awt.event.ActionEvent actionEvent)
				throws Throwable {
			openDirDialog();
		}

	}

	/** quit action */
	class QuitAction extends
			org.genepattern.modules.ui.listeners.ReportErrorAbstractAction {

		QuitAction() {
			super(
					(System.getProperty("mrj.version") == null ? "Exit"
							: "Quit"), 'Q');
		}

		/**
		 * subclasses will implement this method to know when an event occured
		 * This is pertty much the only method that the developer needs to
		 * bother with
		 *  
		 */
		protected final void doAction(java.awt.event.ActionEvent actionEvent)
				throws Throwable {
			System.exit(0);
		}

	}

	/** SaveConfig action */
	class SaveConfigAction extends
			org.genepattern.modules.ui.listeners.ReportErrorAbstractAction {

		SaveConfigAction() {
			super("Save Configuration", 'S');
		}

		/**
		 * subclasses will implement this method to know when an event occured
		 * This is pertty much the only method that the developer needs to
		 * bother with
		 *  
		 */
		protected final void doAction(java.awt.event.ActionEvent actionEvent)
				throws Throwable {

		}
	}

	/** Preferences action */
	class PreferencesAction extends
			org.genepattern.modules.ui.listeners.ReportErrorAbstractAction {

		PreferencesAction() {
			super("Preferences...");
		}

		/**
		 * subclasses will implement this method to know when an event occured
		 * This is pertty much the only method that the developer needs to
		 * bother with
		 *  
		 */
		protected final void doAction(java.awt.event.ActionEvent actionEvent)
				throws Throwable {
			javax.swing.JFrame frame = new javax.swing.JFrame("Preferences");
			final PreferencesPanel prefs = GenePattern
					.createGpPreferencesPanel();
			frame.getContentPane().add(prefs);

			frame.pack();
			frame.show();
		}
	}

	/** logdisplay action */
	class LogDisplayAction extends
			org.genepattern.modules.ui.listeners.ReportErrorAbstractAction {

		LogDisplayAction(final char accel) {
			super("Log of runs", accel);
		}

		/**
		 * subclasses will implement this method to know when an event occured
		 * This is pertty much the only method that the developer needs to
		 * bother with
		 *  
		 */
		protected final void doAction(java.awt.event.ActionEvent actionEvent)
				throws Throwable {
			((CardLayout) cont.getLayout()).show(cont, id);
		}

		/** sets the paramters needed to switch the panels */
		protected void setContainerAndLabel(final Container cont,
				final String id) {
			this.cont = cont;
			this.id = id;
		}

		// fields
		private Container cont;

		private String id;
	}

	/** About action */
	class AboutAction extends
			org.genepattern.modules.ui.listeners.ReportErrorAbstractAction {

		AboutAction() {
			super("About GenePattern");
		}

		/**
		 * subclasses will implement this method to know when an event occured
		 * This is pertty much the only method that the developer needs to
		 * bother with
		 *  
		 */
		protected final void doAction(java.awt.event.ActionEvent actionEvent)
				throws Throwable {
			GenePattern.showAbout();
		}
	}

	/** ErrorMsgs action */
	class ErrorMsgsAction extends
			org.genepattern.modules.ui.listeners.ReportErrorAbstractAction {

		ErrorMsgsAction() {
			super("Error Messages");
		}

		/**
		 * subclasses will implement this method to know when an event occured
		 * This is pertty much the only method that the developer needs to
		 * bother with
		 *  
		 */
		protected final void doAction(java.awt.event.ActionEvent actionEvent)
				throws Throwable {
			GenePattern.showErrors();
		}
	}

	/** WarnMsgs action */
	class WarnMsgsAction extends
			org.genepattern.modules.ui.listeners.ReportErrorAbstractAction {

		WarnMsgsAction() {
			super("Warnings");
		}

		/**
		 * subclasses will implement this method to know when an event occured
		 * This is pertty much the only method that the developer needs to
		 * bother with
		 *  
		 */
		protected final void doAction(java.awt.event.ActionEvent actionEvent)
				throws Throwable {
			GenePattern.showWarnings();
		}
	}

	/** Guide document action */
	class ViewGuideAction extends
			org.genepattern.modules.ui.listeners.ReportErrorAbstractAction {

		ViewGuideAction() {
			super("GenePattern Guide");
		}

		/**
		 * subclasses will implement this method to know when an event occured
		 * This is pertty much the only method that the developer needs to
		 * bother with
		 *  
		 */
		protected final void doAction(java.awt.event.ActionEvent actionEvent)
				throws Throwable {
			final String url_string = GPpropertiesManager
					.getProperty("gp.help.ref_guide");
			if (url_string != null && url_string.length() > 0) {
				javax.swing.JFrame frame = new javax.swing.JFrame();
				final BrowserPanel browser = new BrowserPanel(frame,
						"GenePattern Reference Guide");
				frame.getContentPane().add(browser);
				frame.pack();
				frame.show();
				java.net.URL url = new java.net.URL(url_string);
				browser.loadURL(url);
			} else {
				GenePattern
						.showWarning(null, "Cannot get reference guide URL!");
			}
		}
	}

	static class TaskMenuItem extends JMenuItem {
		String key;

		public TaskMenuItem(String s, String key) {
			super(s);
			this.key = key;
		}
	}

	/** a ListTypeAdapter that supports JMenuItems */
	private abstract class MenuItemListTypeAdapter implements ListTypeAdapter,
			java.awt.event.ActionListener {
		/** constructor */
		MenuItemListTypeAdapter() {
			menu_blocks[2] = new MenuBlock("Image", null);
			menu_blocks[1] = new MenuBlock("Image Creators", menu_blocks[2]);
			menu_blocks[0] = new MenuBlock("Visualizers", menu_blocks[1]);
		}

		public void addActionListener(java.awt.event.ActionListener listener) {
			throw new UnsupportedOperationException("Not implemented");
		}

		public void removeActionListener(java.awt.event.ActionListener listener) {
			throw new UnsupportedOperationException("Not implemented");
		}

		public Object getItemSelected() {
			if (item_selected instanceof TaskMenuItem) {
				return name_object.get(((TaskMenuItem) (item_selected)).key);
			}
			return name_object.get(item_selected.getText());
		}

		public String getSelectedAsString() {
			return item_selected.getText();
		}

		public void removeAll() {
			final int limit = analysis_menu.getMenuComponentCount();
			for (int i = init_menu_cnt - 1; i < limit; i++) {
				analysis_menu.remove(i);
			}
			visualizer_menu.removeAll();
			name_object.clear();
		}

		public void remove(Object item) {
			throw new UnsupportedOperationException("Not implemented");
		}

		public void remove(int index) {
			throw new UnsupportedOperationException("Not implemented");
		}

		public void addItem(Object item, java.awt.event.ActionListener listener) {
			final AnalysisService an_serv = (AnalysisService) item;
			final JMenuItem menu_item = createAddMenuItem(an_serv);
			menu_item.addActionListener(listener);
		}

		public void addItem(Object item) {
			final AnalysisService an_serv = (AnalysisService) item;
			final JMenuItem menu_item = createAddMenuItem(an_serv);
		}

		public void insert(Object item, int index) {
			throw new UnsupportedOperationException("Not implemented");
		}

		public int getItemCount() {
			return analysis_menu.getMenuComponentCount()
					+ visualizer_menu.getMenuComponentCount() - init_menu_cnt;
		}

		public boolean isLocalComponent() {
			return false;
		}

		public java.awt.Component getComponent() {
			return null;
		}

		// helper methods
		/** adds a JMenuItem from an AnalysisService object */
		private JMenuItem createAddMenuItem(final AnalysisService an_serv) {
			final TaskInfo task_info = an_serv.getTaskInfo();

			String label = LSIDUtil.getTaskString(task_info, false, false);
			String lsid = (String) task_info.getTaskInfoAttributes().get(
					GPConstants.LSID);
			final String key = LSIDUtil.getTaskId(task_info);
			;

			JMenuItem menu_item = new TaskMenuItem(label, key);

			try {
				String authType = org.genepattern.util.LSIDUtil.getInstance()
						.getAuthorityType(new org.genepattern.util.LSID(lsid));

				if (authType
						.equals(org.genepattern.util.LSIDUtil.AUTHORITY_MINE)) {
					menu_item.setForeground(AUTHORITY_MINE_COLOR);
				} else if (authType
						.equals(org.genepattern.util.LSIDUtil.AUTHORITY_FOREIGN)) {
					menu_item.setForeground(AUTHORITY_FOREIGN_COLOR);
				}
			} catch (java.net.MalformedURLException x) {
			}
			name_object.put(key, an_serv);

			final String task_type = (String) task_info.getTaskInfoAttributes()
					.get(GPConstants.TASK_TYPE);
			if (VisualizerTaskSubmitter.VISUALIZER.equalsIgnoreCase(task_type)) {
				menu_blocks[0].add(visualizer_menu, menu_item);
			} else if ("Image Creators".equalsIgnoreCase(task_type)) {
				menu_blocks[1].add(visualizer_menu, menu_item);
			} else if ("image".equalsIgnoreCase(task_type)) {
				menu_blocks[2].add(visualizer_menu, menu_item);
				//visualizer_menu.add(menu_item);
			} else {
				menu_item.setToolTipText(task_info.getDescription());
				final String type = (String) task_info.getTaskInfoAttributes()
						.get(GPConstants.TASK_TYPE);
				final JMenu submenu = getMenu(type, analysis_menu);
				submenu.add(menu_item);

			}
			if (item_selected == null)
				item_selected = menu_item;
			return menu_item;
		}

		// Interface ActionListener method signature

		abstract public void actionPerformed(
				final java.awt.event.ActionEvent actionEvent);

		// helper methods

		/**
		 * gets the submenu keyed by the type or creates one first if the first
		 * time the type has been requested. The newly created submenus are
		 * added to the analysis_menu.
		 */
		private JMenu getMenu(String type, final JMenu analysis_menu) {
			if (type == null || type.length() == 0)
				type = "Unclassified";
			JMenu menu = (JMenu) name_submenu.get(type);
			if (menu == null) {
				final char upper = Character.toUpperCase(type.charAt(0));
				menu = new JMenu(upper + type.substring(1));
				name_submenu.put(type, menu);
				//////////////////////
				// insert the menus in alphabetical order leaving
				// log of runs and the separator at the top

				boolean inserted = false;
				for (int i = 2; i < analysis_menu.getItemCount(); i++) {
					JMenuItem sub = (JMenuItem) analysis_menu
							.getMenuComponent(i);
					String subName = sub.getText();
					int comp = type.compareToIgnoreCase(subName);
					if (comp < 0) {
						analysis_menu.insert(menu, i);
						inserted = true;
						break;
					}
				}
				if (!inserted)
					analysis_menu.add(menu);
				///////////////////

			}
			return menu;
		}

		// fields
		/** names mapped to their submenu */
		private final Map name_submenu = new HashMap();

		/** the last item selected */
		protected JMenuItem item_selected;

		/** the initial number of items in the menu */
		private final int init_menu_cnt = analysis_menu.getMenuComponentCount();

		/** the map between object name and object */
		final java.util.Map name_object = new java.util.HashMap();

		/** the MenuBlock of the visualizer */
		private MenuBlock[] menu_blocks = new MenuBlock[3];
	}

	/**
	 * Defines a block of menu items beginning with a Ghosted separator keeps
	 * track of where to insert the next menu item
	 */
	static class MenuBlock {
		/**
		 * constructor
		 * 
		 * @param label
		 *            the ghosted separator
		 * @param next
		 *            the next MenuBlock - where to insert menu items
		 */
		protected MenuBlock(final String label, final MenuBlock next) {
			this.label = label;
			this.next = next;
		}

		/** the previous block inserted a menu item so must advance the index */
		protected void advance() {
			if (index >= 0)
				index++;
			if (next != null)
				next.advance();
		}

		/** inserts a menu item to the menu and advances the next menu block */
		protected void add(final JMenu menu, final JMenuItem item) {
			if (index < 0) {
				final JMenuItem ghosted = new JMenuItem(label);
				ghosted.setEnabled(false);
				java.awt.Font font = ghosted.getFont();
				font = font.deriveFont(java.awt.Font.BOLD,
						font.getSize2D() * 1.2f);
				ghosted.setFont(font);
				ghosted.setForeground(java.awt.Color.blue);

				if (next != null && next.index >= 0) {
					index = next.index;
					menu.insert(ghosted, index);
					next.advance();

				} else {
					index = menu.getItemCount();
					menu.add(ghosted);
				}
			}
			if (next != null && next.index >= 0) {
				menu.insert(item, next.index);
				next.advance();
			} else {
				menu.add(item);
			}

		}

		// fields
		/** the label for the separator */
		private final String label;

		/** the next menu block or null if at the end */
		private final MenuBlock next;

		/** the current index or not yet set if negative */
		private int index = -1;
	} // end MenuBlock

	/**
	 * Determines if the MenuItem should be enabled only when the Menu is
	 * selected
	 */
	final static class SmartPasteAction extends
			javax.swing.text.DefaultEditorKit.PasteAction implements
			javax.swing.event.MenuListener {
		/** enable paste only if the focus is on an editable JTextComponent */
		public void menuSelected(javax.swing.event.MenuEvent e) {
			final JComponent focused = this.getFocusedComponent();
			if (focused instanceof JTextComponent) {
				JTextComponent text_comp = (JTextComponent) focused;
				setEnabled(text_comp.isEditable());
			} else
				setEnabled(false);
		}

		public void menuCanceled(javax.swing.event.MenuEvent e) {
		}

		public void menuDeselected(javax.swing.event.MenuEvent e) {
		}
	} //end SmartPasteAction

	/**
	 * Determines if the MenuItem should be enabled only when the Menu is
	 * selected
	 */
	final static class SmartCutAction extends
			javax.swing.text.DefaultEditorKit.CutAction implements
			javax.swing.event.MenuListener {
		/**
		 * enable paste only if the focus is on an editable JTextComponent with
		 * selected text
		 */
		public void menuSelected(javax.swing.event.MenuEvent e) {
			final JComponent focused = this.getFocusedComponent();
			if (focused instanceof JTextComponent) {
				JTextComponent text_comp = (JTextComponent) focused;
				final boolean is_selected = (text_comp.getSelectionEnd() != text_comp
						.getSelectionStart());
				setEnabled(text_comp.isEditable() && is_selected);
			} else
				setEnabled(false);
		}

		public void menuCanceled(javax.swing.event.MenuEvent e) {
		}

		public void menuDeselected(javax.swing.event.MenuEvent e) {
		}
	} // end SmartCutAction

	/**
	 * Determines if the MenuItem should be enabled only when the Menu is
	 * selected
	 */
	final static class SmartCopyAction extends
			javax.swing.text.DefaultEditorKit.CopyAction implements
			javax.swing.event.MenuListener {
		/**
		 * enable paste only if the focus is on a JTextComponent with selected
		 * text
		 */
		public void menuSelected(javax.swing.event.MenuEvent e) {
			final JComponent focused = this.getFocusedComponent();
			if (focused instanceof JTextComponent) {
				JTextComponent text_comp = (JTextComponent) focused;
				final boolean is_selected = (text_comp.getSelectionEnd() != text_comp
						.getSelectionStart());
				setEnabled(is_selected);
			} else
				setEnabled(false);
		}

		public void menuCanceled(javax.swing.event.MenuEvent e) {
		}

		public void menuDeselected(javax.swing.event.MenuEvent e) {
		}
	}// end SmartCopyAction
}// end DataObjectBrowser
