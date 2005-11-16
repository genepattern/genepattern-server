package org.genepattern.gpge.ui.maindisplay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.PropertyManager;
import org.genepattern.gpge.message.ChangeViewMessage;
import org.genepattern.gpge.message.ChangeViewMessageRequest;
import org.genepattern.gpge.message.GPGEMessage;
import org.genepattern.gpge.message.GPGEMessageListener;
import org.genepattern.gpge.message.MessageManager;
import org.genepattern.gpge.message.RefreshMessage;
import org.genepattern.gpge.message.SuiteInstallMessage;
import org.genepattern.gpge.message.TaskInstallMessage;
import org.genepattern.gpge.ui.menu.MenuAction;
import org.genepattern.gpge.ui.menu.MenuItemAction;
import org.genepattern.gpge.ui.preferences.ChangeServerDialog;
import org.genepattern.gpge.ui.preferences.PreferenceKeys;
import org.genepattern.gpge.ui.preferences.SuitesPreferences;
import org.genepattern.gpge.ui.project.ProjectDirModel;
import org.genepattern.gpge.ui.project.ProjectDirectoryListener;
import org.genepattern.gpge.ui.project.ProjectEvent;
import org.genepattern.gpge.ui.suites.SuiteEditor;
import org.genepattern.gpge.ui.tasks.AnalysisServiceManager;
import org.genepattern.gpge.ui.tasks.AnalysisServiceUtil;
import org.genepattern.gpge.ui.tasks.FileInfoComponent;
import org.genepattern.gpge.ui.tasks.HistoryMenu;
import org.genepattern.gpge.ui.tasks.HistoryModel;
import org.genepattern.gpge.ui.tasks.JobMessage;
import org.genepattern.gpge.ui.tasks.JobModel;
import org.genepattern.gpge.ui.tasks.SemanticUtil;
import org.genepattern.gpge.ui.tasks.Sendable;
import org.genepattern.gpge.ui.tasks.TaskDisplay;
import org.genepattern.gpge.ui.treetable.SortableTreeTable;
import org.genepattern.gpge.ui.util.GUIUtil;
import org.genepattern.gpge.util.BuildProperties;
import org.genepattern.util.BrowserLauncher;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.AdminProxy;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.AnalysisWebServiceProxy;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskIntegratorProxy;
import org.genepattern.webservice.WebServiceException;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Main class for GPGE functionality
 * 
 * @author Joshua Gould
 */
public class GPGE {

	AnalysisServiceManager analysisServiceManager;

	private final static Color DEFAULT_AUTHORITY_MINE_COLOR = Color.MAGENTA;

	private final static Color DEFAULT_AUTHORITY_FOREIGN_COLOR = java.awt.Color
			.decode("0x0000FF");

	private final static Color DEFAULT_AUTHORITY_BROAD_COLOR = Color.black;

	static Color authorityMineColor, authorityForeignColor,
			authorityBroadColor;

	AnalysisMenu analysisMenu;

	AnalysisMenu visualizerMenu;

	AnalysisMenu pipelineMenu;

	HistoryMenu historyMenu;

	JPopupMenu jobPopupMenu;

	JPopupMenu jobResultFilePopupMenu;

	JPopupMenu projectDirPopupMenu;

	JPopupMenu projectFilePopupMenu;

	SortableTreeTable jobResultsTree;

	JobModel jobModel;

	SortableTreeTable projectDirTree;

	ProjectDirModel projectDirModel;

	DefaultMutableTreeNode selectedJobNode = null;

	DefaultMutableTreeNode selectedProjectDirNode = null;

	FileMenu fileMenu;

	JMenu windowMenu;

	Map inputTypeToMenuItemsMap;

	final JFrame frame;

	final static int MENU_SHORTCUT_KEY_MASK = Toolkit.getDefaultToolkit()
			.getMenuShortcutKeyMask();

	FileInfoComponent fileSummaryComponent = new FileInfoComponent();

	public static boolean RUNNING_ON_MAC = System.getProperty("mrj.version") != null
			&& javax.swing.UIManager.getSystemLookAndFeelClassName()
					.equals(
							javax.swing.UIManager.getLookAndFeel().getClass()
									.getName());

	public static boolean RUNNING_ON_WINDOWS = System.getProperty("os.name")
			.toLowerCase().startsWith("windows");

	private JMenuBar menuBar;

	Color blue = new Color(51, 0, 204);

	/** The key used for the parameter name for a 'send to' action */
	private static String PARAMETER_NAME = "PARAMETER_NAME";

	private static GPGE instance = new GPGE();

	// project file actions
	MenuAction projectFileSendToMenu;

	MenuAction projectFileOpenWithMenu;

	MenuAction projectFileViewModulesMenu;

	MenuItemAction projectFileDefaultAppMenuItem;

	MenuItemAction revealFileMenuItem;

	// project dir actions
	MenuItemAction refreshProjectMenuItem;

	MenuItemAction removeProjectMenuItem;

	// job result actions
	MenuItemAction reloadMenuItem;

	MenuItemAction deleteJobAction;

	MenuItemAction terminateJobAction;

	MenuAction viewCodeAction;

	// job result file actions
	MenuAction jobResultFileSendToMenu;

	MenuAction saveServerFileMenu;

	MenuAction jobResultFileViewModulesMenu;

	MenuItemAction saveToFileSystemMenuItem;

	MenuItemAction deleteFileMenuItem;

	MenuAction openWithMenu;

	MenuItemAction jobResultFileTextViewerMenuItem;

	MenuItemAction jobResultFileDefaultAppMenuItem;

	MenuItemAction deleteAllJobsAction;

	boolean runTaskViewShown = false;

	private MenuItemAction createPipelineMenuItem;

	private SuiteMenu suiteMenu;

	public static ParameterInfo copyParameterInfo(ParameterInfo toClone) {
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

	boolean disconnectedFromServer(WebServiceException wse) {
		return GenePattern.disconnectedFromServer(wse, analysisServiceManager
				.getServer());
	}

	private static boolean isPopupTrigger(MouseEvent e) {
		return (e.isPopupTrigger() || e.getModifiers() == MouseEvent.BUTTON3_MASK);
	}

	private static String fileToString(File file) throws IOException {
		StringBuffer sb = new StringBuffer();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			String s = null;
			while ((s = br.readLine()) != null) {
				sb.append(s);
				sb.append("\n");
			}
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException x) {
				}
			}
		}
		return sb.toString();
	}

	private void defaultApplication(TreeNode node) {
		if (node instanceof ProjectDirModel.FileNode) {
			try {
				ProjectDirModel.FileNode fn = (ProjectDirModel.FileNode) node;
				String filePath = fn.file.getCanonicalPath();
				if (RUNNING_ON_MAC) {
					String[] args = new String[] { "/usr/bin/open", filePath };
					Runtime.getRuntime().exec(args);
				} else {
					org.genepattern.util.BrowserLauncher.openURL(filePath);
				}
			} catch (IOException ioe) {
			}
		} else if (node instanceof JobModel.ServerFileNode) {
			final JobModel.ServerFileNode sn = (JobModel.ServerFileNode) node;

			File downloadDir = new File("tmp");
			if (!downloadDir.exists()) {
				downloadDir.mkdir();
			}
			String name = sn.name;
			int dotIndex = name.lastIndexOf(".");
			String baseName = name;
			String extension = "";
			if (dotIndex > 0) {
				baseName = name.substring(0, dotIndex);
				extension = name.substring(dotIndex, name.length());
			}
			File download = new File(downloadDir, name);
			int tries = 1;
			while (download.exists()) {
				String newName = baseName + "-" + tries + extension;
				download = new File(downloadDir, newName);
				tries++;
			}
			final File destination = download;
			destination.deleteOnExit();
			try {
				sn.download(destination);
				String filePath = destination.getCanonicalPath();
				if (RUNNING_ON_MAC) {
					String[] args = new String[] { "/usr/bin/open", filePath };
					Runtime.getRuntime().exec(args);
				} else {
					org.genepattern.util.BrowserLauncher.openURL(filePath);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	private void textViewer(TreeNode node) {
		File file = null;
		boolean deleteFile = false;
		String title = null;
		if (node instanceof JobModel.ServerFileNode) {
			JobModel.ServerFileNode jobResult = (JobModel.ServerFileNode) node;
			JobModel.JobNode jobNode = (JobModel.JobNode) jobResult.getParent();
			try {
				file = File.createTempFile("tmp", null);
				deleteFile = true;
				JobModel.downloadJobResultFile(jobNode.job, jobResult.index,
						file);
				title = JobModel.getJobResultFileName(jobNode.job,
						jobResult.index)
						+ " Job " + jobNode.job.getJobInfo().getJobNumber();
			} catch (IOException ioe) {
				ioe.printStackTrace();
				GenePattern
						.showErrorDialog("An error occurred while downloading "
								+ JobModel.getJobResultFileName(jobNode.job,
										jobResult.index));
				return;
			}
		} else if (node instanceof ProjectDirModel.FileNode) {
			ProjectDirModel.FileNode fileNode = (ProjectDirModel.FileNode) node;
			file = fileNode.file;
			title = file.getPath();
		}
		if (file != null) {
			String contents = null;
			try {
				contents = fileToString(file);
			} catch (IOException ioe) {
				ioe.printStackTrace();
				GenePattern
						.showErrorDialog("An error occurred while viewing the file");
				return;
			}
			if (deleteFile) {
				file.delete();
			}
			JDialog dialog = new CenteredDialog(frame);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setTitle(title);
			JTextArea textArea = new JTextArea(contents);
			textArea.setEditable(false);
			JScrollPane sp = new JScrollPane(textArea);
			dialog.getContentPane().add(sp, BorderLayout.CENTER);
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			dialog.setSize(screenSize.width / 2, screenSize.height / 2);
			dialog.setVisible(true);
		}
	}

	public void showSaveDialog(final JobModel.ServerFileNode node) {
		final File initiallySelectedFile = new File(node.toString());
		final File outputFile = GUIUtil.showSaveDialog(initiallySelectedFile);

		if (outputFile != null) {

			new Thread() {
				public void run() {
					try {
						node.download(outputFile);
						// } catch (WebServiceException wse) {
						// if(!disconnectedFromServer(wse)) {
						// GenePattern.showErrorDialog("An error occurred while
						// saving " + outputFile.getName() + ". Please try
						// again.");
						// }
					} catch (IOException ioe) {
						ioe.printStackTrace();
						GenePattern
								.showErrorDialog("An error occurred while saving "
										+ outputFile.getName() + ".");
					}
				}
			}.start();
		}
	}

	public void changeServer(final String server, final String username) {
		analysisServiceManager = AnalysisServiceManager.getInstance();

		analysisServiceManager.changeServer(server, username);
		MessageManager.notifyListeners(new ChangeViewMessageRequest(this,
				ChangeViewMessageRequest.SHOW_GETTING_STARTED_REQUEST));

		final boolean isLocalHost = analysisServiceManager.isLocalHost();
		if (isLocalHost) {
			MessageDialog
					.getInstance()
					.setText(
							"Retrieving modules and jobs from local GenePattern server");
		} else {
			MessageDialog.getInstance().setText(
					"Retrieving modules and jobs from " + server);
		}

		PropertyManager.setProperty(PreferenceKeys.SERVER, server);
		PropertyManager.setProperty(PreferenceKeys.USER_NAME, username);

		setChangeServerActionsEnabled(false);

		new Thread() {
			public void run() {

				try {
					Map serviceInfo = new org.genepattern.webservice.AdminProxy(
							analysisServiceManager.getServer(),
							analysisServiceManager.getUsername(), false)
							.getServiceInfo();
					String lsidAuthority = (String) serviceInfo
							.get("lsid.authority");
					String serverVersion = (String) serviceInfo
							.get("genepattern.version");
					String clientVersion = BuildProperties.FULL_VERSION;
					String[] serverVersionTokens = serverVersion.split("\\.");
					String[] clientVersionTokens = clientVersion.split("\\.");
					if (serverVersionTokens.length < 2
							|| clientVersionTokens.length < 2) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								GenePattern
										.showMessageDialog("Warning: This client version has not been tested with the version of the server that you are connecting to.");
							}
						});
					} else {
						if (!serverVersionTokens[0]
								.equals(clientVersionTokens[0])
								|| !serverVersionTokens[1]
										.equals(clientVersionTokens[1])) {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									GenePattern
											.showMessageDialog("Warning: This client version has not been tested with the version of the server that you are connecting to.");
								}
							});

						}
					}

					// make sure major and minor version of server is the same
					// as the client

					System.setProperty("lsid.authority", lsidAuthority);
					refreshJobs(false);
				} catch (WebServiceException wse) {
					wse.printStackTrace();
					// ignore the exception here, the user will be alerted in
					// refreshModules
				}
				refreshModules(false);

				displayServerStatus();
				MessageDialog.getInstance().setVisible(false);
			}
		}.start();

	}

	private String getServer() {
		return analysisServiceManager.isLocalHost() ? "Local"
				: analysisServiceManager.getServer();
	}

	private void displayServerStatus() {
		final String server = getServer();
		final String username = analysisServiceManager.getUsername();

		Thread changeStatusThread = new Thread() {
			public void run() {
				frame.setTitle("GPGE - Server: " + server + ",  Username: "
						+ username);

			}
		};
		SwingUtilities.invokeLater(changeStatusThread);
	}

	/**
	 * Loads a task with the parameters that were used in the specified job into
	 * the AnalysisTaskPanel
	 * 
	 * @param job
	 *            the job
	 */
	public void reload(AnalysisJob job) {
		String taskName = job.getTaskName();
		String lsid = job.getLSID();

		AnalysisService service = analysisServiceManager
				.getAnalysisService(lsid);

		if (service == null) {

			if (service == null) { // get latest version of lsid
				try {
					service = analysisServiceManager
							.getAnalysisService(new LSID(lsid)
									.toStringNoVersion());
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
			if (service == null) {
				GenePattern.showMessageDialog("The task " + taskName
						+ " does not exist.");
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
				String actualValue = null; // the value to set the parameter
				// for
				// the job we are about to submit

				if (savedParameterInfo != null) { // saved parameter exists in
					// installed task
					savedParamName2Param.remove(savedParameterInfo.getName());
					if (savedParameterInfo.isOutputFile()) {
						continue;
					}
					if (ParameterInfo.CACHED_INPUT_MODE
							.equals(savedParameterInfo.getAttributes().get(
									ParameterInfo.MODE))) { // input file is
						// result of
						// previous job on
						// server
						String fileNameOnServer = savedParameterInfo.getValue();
						ParameterInfo pi = new ParameterInfo(savedParameterInfo
								.getName(), "", savedParameterInfo
								.getDescription());
						int index1 = fileNameOnServer.lastIndexOf('/');
						int index2 = fileNameOnServer.lastIndexOf('\\');
						int index = (index1 > index2 ? index1 : index2);
						if (index != -1) {
							String fileName = fileNameOnServer.substring(
									index + 1, fileNameOnServer.length());
							String jobNumber = fileNameOnServer.substring(0,
									index);
							HashMap attrs = new HashMap(1);
							pi.setAttributes(attrs);
							pi.getAttributes().put(
									GPConstants.PARAM_INFO_DEFAULT_VALUE[0],
									"job #" + jobNumber + ", " + fileName);
							pi.setAsInputFile();
						}

						actualParams.add(pi);
						continue;
					} else if (savedParameterInfo.isInputFile()) { // input
						// file
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
			errorMessage.append("Ignoring now unused parameters ");
		} else if (savedParamName2Param.size() == 1) {
			errorMessage.append("Ignoring now unused parameter ");
		}

		for (Iterator iUnused = savedParamName2Param.keySet().iterator(); iUnused
				.hasNext();) {
			errorMessage.append(iUnused.next() + "\n");
		}

		if (errorMessage.length() > 0) {
			GenePattern.showMessageDialog(errorMessage.toString());
		}
		TaskInfo taskCopy = new TaskInfo(task.getID(), task.getName(), task
				.getDescription(), task.getParameterInfo(), task
				.giveTaskInfoAttributes(), task.getUserId(), task.getAccessId());

		taskCopy.setParameterInfoArray((ParameterInfo[]) actualParams
				.toArray(new ParameterInfo[0]));
		AnalysisService serviceCopy = new AnalysisService(service.getServer(),
				taskCopy);
		MessageManager.notifyListeners(new ChangeViewMessageRequest(this,
				ChangeViewMessageRequest.SHOW_RUN_TASK_REQUEST, serviceCopy));

	}

	public static List getOuputFileNames(AnalysisJob job) {
		ParameterInfo[] jobParameterInfo = job.getJobInfo()
				.getParameterInfoArray();
		List filenames = new ArrayList();
		for (int j = 0; j < jobParameterInfo.length; j++) {
			if (jobParameterInfo[j].isOutputFile()) {
				String fileName = jobParameterInfo[j].getValue();
				int index1 = fileName.lastIndexOf('/');
				int index2 = fileName.lastIndexOf('\\');
				int index = (index1 > index2 ? index1 : index2);
				if (index != -1) {
					fileName = fileName.substring(index + 1, fileName.length());
				}
				filenames.add(fileName);
			}
		}
		return filenames;
	}

	void enableSendToMenus(MenuAction sendToMenu, String kind) {
		for (int i = 0, length = sendToMenu.getItemCount(); i < length; i++) {
			Object obj = sendToMenu.getMenuComponent(i);
			if (obj instanceof SendToMenuItemAction) {
				SendToMenuItemAction mia = (SendToMenuItemAction) obj;
				mia.setEnabled(mia.isCorrectKind(kind));
			}
		}
	}

	public static GPGE getInstance() {
		return instance;
	}

	private GPGE() {
		this.frame = new JFrame();
	}

	public JFrame getFrame() {
		return frame;
	}

	public void startUp() {
		JWindow splash = GenePattern.showSplashScreen();
		splash.setVisible(true);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit()
				.getScreenSize();
		if (RUNNING_ON_MAC) {
			frame.setSize(0, 0);
			frame.setLocation(screenSize.width / 2, screenSize.height / 2);
			frame.show(); // on Mac OSX the dialog won't stay on top unless
			// the parent frame is visible when the dialog is
			// created
		}
		MessageDialog.init(frame);
		frame.setVisible(false);
		String username = PropertyManager.getProperty(PreferenceKeys.USER_NAME);

		if (username == null || username.trim().equals("")) {
			username = System.getProperty("user.name");
			if (username == null || username.trim().equals("")) {
				username = "anonymous";
			}
		}
		String server = PropertyManager.getProperty(PreferenceKeys.SERVER);
		if (server == null || server.equals("")) {
			try {
				Properties omnigeneProps = org.genepattern.util.PropertyFactory
						.getInstance().getProperties("omnigene.properties");
				String deprecatedServer = "http://"
						+ omnigeneProps
								.getProperty("analysis.service.site.name"); // omnigene
				// properties
				// are
				// deprecated
				server = deprecatedServer;
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (server == null) {
				server = "http://127.0.0.1:8080";
			}
		}
		try {
			new URL(server);
		} catch (MalformedURLException mfe) {
			server = "http://" + server;
		}
		authorityMineColor = PropertyManager
				.decodeColorFromProperties(PropertyManager
						.getProperty(PreferenceKeys.AUTHORITY_MINE_COLOR));
		if (authorityMineColor == null) {
			authorityMineColor = DEFAULT_AUTHORITY_MINE_COLOR;
		}

		authorityForeignColor = PropertyManager
				.decodeColorFromProperties(PropertyManager
						.getProperty(PreferenceKeys.AUTHORITY_FOREIGN_COLOR));
		if (authorityForeignColor == null) {
			authorityForeignColor = DEFAULT_AUTHORITY_FOREIGN_COLOR;
		}

		authorityBroadColor = PropertyManager
				.decodeColorFromProperties(PropertyManager
						.getProperty(PreferenceKeys.AUTHORITY_BROAD_COLOR));
		if (authorityBroadColor == null) {
			authorityBroadColor = DEFAULT_AUTHORITY_BROAD_COLOR;
		}

		PropertyManager.setProperty(PreferenceKeys.AUTHORITY_BROAD_COLOR,
				PropertyManager.encodeColorToProperty(authorityBroadColor));
		PropertyManager.setProperty(PreferenceKeys.AUTHORITY_FOREIGN_COLOR,
				PropertyManager.encodeColorToProperty(authorityForeignColor));
		PropertyManager.setProperty(PreferenceKeys.AUTHORITY_MINE_COLOR,
				PropertyManager.encodeColorToProperty(authorityMineColor));

		String showParameterDescriptions = PropertyManager
				.getProperty(PreferenceKeys.SHOW_PARAMETER_DESCRIPTIONS);
		if (showParameterDescriptions == null) {
			PropertyManager.setProperty(
					PreferenceKeys.SHOW_PARAMETER_DESCRIPTIONS, "true");
		}

		String alertOnJobDescription = PropertyManager
				.getProperty(PreferenceKeys.SHOW_JOB_COMPLETED_DIALOG);
		if (alertOnJobDescription == null) {
			PropertyManager.setProperty(
					PreferenceKeys.SHOW_JOB_COMPLETED_DIALOG, "true");
		}

		jobModel = JobModel.getInstance();

		jobResultsTree = new SortableTreeTable(jobModel);
		projectDirModel = ProjectDirModel.getInstance();
		projectDirTree = new SortableTreeTable(projectDirModel);

		createJobActions();
		createJobResultFileActions();
		createProjectDirActions();
		createProjectFileActions();

		createMenus();

		MessageManager.addGPGEMessageListener(new GPGEMessageListener() {
			public void receiveMessage(GPGEMessage message) {
				if (message instanceof JobMessage) {
					JobMessage e = (JobMessage) message;
					if (e.getType() == JobMessage.JOB_COMPLETED) {
						jobCompleted(e);
					}
				}
			}

			void jobCompleted(JobMessage e) {
				AnalysisJob job = e.getJob();
				int jobNumber = job.getJobInfo().getJobNumber();
				String taskName = job.getTaskName();
				String status = job.getJobInfo().getStatus();
				fileMenu.jobCompletedDialog.add(jobNumber, taskName, status);
				ParameterInfo[] params = job.getJobInfo()
						.getParameterInfoArray();
				int stderrIndex = -1;
				if (params != null) {
					for (int i = 0; i < params.length; i++) {
						if (params[i].isOutputFile()) {
							if (params[i].getValue().equals(
									jobNumber + "/stderr.txt")
									|| params[i].getValue().equals(
											jobNumber + "\\stderr.txt")) {
								stderrIndex = i;
								break;
							}
						}
					}
				}
				if (stderrIndex >= 0) {
					File stderrFile = null;
					try {
						stderrFile = File.createTempFile("stderr.txt", null);
						JobModel.downloadJobResultFile(job, stderrIndex,
								stderrFile);
						GenePattern.showModuleErrorDialog("Job " + jobNumber
								+ " Error", fileToString(stderrFile));
					} catch (IOException ioe) {
						ioe.printStackTrace();
					} finally {
						if (stderrFile != null) {
							stderrFile.delete();
						}
					}
				}
			}

		});

		changeServer(server, username);
		splash.dispose();

		jobResultsTree.setFocusable(true);
		jobResultsTree.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyPressed(java.awt.event.KeyEvent e) {
				if (e.getKeyCode() == java.awt.event.KeyEvent.VK_BACK_SPACE) {
					if (selectedJobNode instanceof JobModel.JobNode) {
						if (deleteJobAction.isEnabled()) {
							deleteJobAction.actionPerformed(new ActionEvent(
									this, ActionEvent.ACTION_PERFORMED, ""));
						}
					} else if (selectedJobNode instanceof JobModel.ServerFileNode) {
						deleteFileMenuItem.actionPerformed(new ActionEvent(
								this, ActionEvent.ACTION_PERFORMED, ""));
					}
				}
			}
		});

		jobResultsTree
				.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
					public void valueChanged(
							javax.swing.event.TreeSelectionEvent e) {
						DefaultMutableTreeNode newSelection = null;
						TreePath path = jobResultsTree.getSelectionPath();
						if (path == null) {
							newSelection = null;
						} else {
							newSelection = (DefaultMutableTreeNode) path
									.getLastPathComponent();
						}
						if (newSelection == null
								&& selectedJobNode == null
								|| (newSelection != null && newSelection
										.equals(selectedJobNode))) {
							return;
						}
						selectedJobNode = newSelection;
						boolean isJobNode = selectedJobNode instanceof JobModel.JobNode;
						boolean isJobResultFileNode = selectedJobNode instanceof JobModel.ServerFileNode;

						deleteJobAction.setEnabled(isJobNode);
						terminateJobAction.setEnabled(isJobNode);
						viewCodeAction.setEnabled(isJobNode);
						reloadMenuItem.setEnabled(isJobNode);
						jobResultFileViewModulesMenu.setEnabled(false);
						if (isJobNode) {
							JobModel.JobNode node = (JobModel.JobNode) selectedJobNode;
							deleteJobAction.setEnabled(node.isComplete());
							terminateJobAction.setEnabled(!node.isComplete());
						}

						jobResultFileSendToMenu.setEnabled(isJobResultFileNode
								&& runTaskViewShown);
						saveServerFileMenu.setEnabled(isJobResultFileNode);
						saveToFileSystemMenuItem
								.setEnabled(isJobResultFileNode);
						deleteFileMenuItem.setEnabled(isJobResultFileNode);
						createPipelineMenuItem.setEnabled(isJobResultFileNode);
						openWithMenu.setEnabled(isJobResultFileNode);
						jobResultFileViewModulesMenu.removeAll();
						if (selectedJobNode instanceof JobModel.ServerFileNode) {
							JobModel.ServerFileNode node = (JobModel.ServerFileNode) selectedJobNode;

							fileSummaryComponent.setText(node.name, node
									.getFileInfo());
							SemanticUtil.ModuleMenuItemAction[] mi = null;
							if (inputTypeToMenuItemsMap != null) {
								mi = (SemanticUtil.ModuleMenuItemAction[]) inputTypeToMenuItemsMap
										.get(node.getFileInfo().getKind());
							}
							if (mi != null) {
								for (int i = 0; i < mi.length; i++) {
									mi[i].setTreeNode(node, node.getFileInfo()
											.getKind());
									jobResultFileViewModulesMenu.add(mi[i]);

								}
							}
							jobResultFileViewModulesMenu.setEnabled(mi != null
									&& mi.length > 0);

							for (int i = 0, length = jobResultFileSendToMenu
									.getItemCount(); i < length; i++) {
								Object obj = jobResultFileSendToMenu
										.getMenuComponent(i);
								if (obj instanceof SendToMenuItemAction) {
									SendToMenuItemAction mia = (SendToMenuItemAction) obj;
									String kind = node.getFileInfo().getKind();
									mia.setEnabled(mia.isCorrectKind(kind));
								}
							}

							// if (connection.getResponseCode() ==
							// HttpURLConnection.HTTP_GONE) {
							// JobModel.JobNode jobNode = (JobModel.JobNode)
							// node.getParent();

							// JobInfo jobFromServer = new
							// AnalysisWebServiceProxy(server,
							// username).checkStatus(jobNode.job.getJobInfo().getJobNumber());

							// GenePattern.showMessageDialog(node.name
							// + " has been deleted from the server.");
							// jobModel.remove(node);
							// fileSummaryComponent.select(null);
							// return;
							// } else {

							// }

						} else {
							fileSummaryComponent.clear();

						}
					}
				});
		jobResultsTree.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() != 2 || isPopupTrigger(e)) {
					return;
				}
				final TreePath path = jobResultsTree.getPathForLocation(e
						.getX(), e.getY());

				if (path == null) {
					return;
				}

				final TreeNode node = (TreeNode) path.getLastPathComponent();
				new Thread() {
					public void run() {
						defaultApplication(node);
					}
				}.start();
			}

			public void mousePressed(MouseEvent e) {

				final TreePath path = jobResultsTree.getPathForLocation(e
						.getX(), e.getY());

				if (path == null) {
					selectedJobNode = null;
					return;
				}
				jobResultsTree.setSelectionPath(path);

				selectedJobNode = (DefaultMutableTreeNode) path
						.getLastPathComponent();

				if (!isPopupTrigger(e)) {
					return;
				}

				if (selectedJobNode instanceof JobModel.JobNode) {
					JobModel.JobNode node = (JobModel.JobNode) selectedJobNode;
					deleteJobAction.setEnabled(node.isComplete());
					terminateJobAction.setEnabled(!node.isComplete());
					jobPopupMenu.show(e.getComponent(), e.getX(), e.getY());
				} else if (selectedJobNode instanceof JobModel.ServerFileNode) {
					jobResultFilePopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
				}
			}

		});

		String projectDirsString = PropertyManager
				.getProperty(PreferenceKeys.PROJECT_DIRS);
		if (projectDirsString != null) {
			String[] projectDirs = projectDirsString.split(";");
			for (int i = 0; i < projectDirs.length; i++) {
				if (projectDirs[i] != null && !projectDirs[i].trim().equals("")) {
					projectDirModel.add(new File(projectDirs[i]));
				}
			}
		}

		projectDirTree
				.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
					public void valueChanged(
							javax.swing.event.TreeSelectionEvent e) {
						TreePath path = projectDirTree.getSelectionPath();
						DefaultMutableTreeNode newSelection = null;

						if (path == null) {
							newSelection = null;
						} else {
							newSelection = (DefaultMutableTreeNode) path
									.getLastPathComponent();
						}

						if (newSelection == null
								&& selectedProjectDirNode == null
								|| (newSelection != null && newSelection
										.equals(selectedProjectDirNode))) {
							return;
						}

						selectedProjectDirNode = newSelection;

						boolean projectNodeSelected = selectedProjectDirNode instanceof ProjectDirModel.ProjectDirNode;
						boolean projectFileSelected = selectedProjectDirNode instanceof ProjectDirModel.FileNode;

						projectFileViewModulesMenu.setEnabled(false);

						projectFileSendToMenu.setEnabled(projectFileSelected
								&& runTaskViewShown);

						projectFileOpenWithMenu.setEnabled(projectFileSelected);
						revealFileMenuItem.setEnabled(projectFileSelected);

						refreshProjectMenuItem.setEnabled(projectNodeSelected);
						removeProjectMenuItem.setEnabled(projectNodeSelected);

						if (projectFileSelected) {

							ProjectDirModel.FileNode node = (ProjectDirModel.FileNode) selectedProjectDirNode;
							ProjectDirModel.ProjectDirNode parent = (ProjectDirModel.ProjectDirNode) node
									.getParent();

							projectFileViewModulesMenu.removeAll();

							if (!new File(parent.directory, node.file.getName())
									.exists()) {
								projectDirModel.refresh(parent);
								fileSummaryComponent.clear();
								return;
							}

							fileSummaryComponent.setText(node.file.getName(),
									node.getFileInfo());

							SemanticUtil.ModuleMenuItemAction[] mi = null;
							if (inputTypeToMenuItemsMap != null) {
								mi = (SemanticUtil.ModuleMenuItemAction[]) inputTypeToMenuItemsMap
										.get(node.getFileInfo().getKind());
							}
							if (mi != null) {
								for (int i = 0; i < mi.length; i++) {
									mi[i].setTreeNode(node, node.getFileInfo()
											.getKind());
									projectFileViewModulesMenu.add(mi[i]);
								}
							}
							projectFileViewModulesMenu.setEnabled(mi != null
									&& mi.length > 0);

							String kind = node.getFileInfo().getKind();
							enableSendToMenus(projectFileSendToMenu, kind);

						} else {
							fileSummaryComponent.clear();

						}

					}
				});

		projectDirTree.addMouseListener(new MouseAdapter() {

			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() != 2 || isPopupTrigger(e)) {
					return;
				}
				final TreePath path = projectDirTree.getPathForLocation(e
						.getX(), e.getY());

				if (path == null) {
					return;
				}

				TreeNode node = (TreeNode) path.getLastPathComponent();
				defaultApplication(node);
			}

			public void mousePressed(MouseEvent e) {

				final TreePath path = projectDirTree.getPathForLocation(e
						.getX(), e.getY());

				if (path == null) {
					selectedProjectDirNode = null;
					return;
				}
				projectDirTree.setSelectionPath(path);

				selectedProjectDirNode = (DefaultMutableTreeNode) path
						.getLastPathComponent();

				if (!isPopupTrigger(e)) {
					return;
				}
				if (selectedProjectDirNode instanceof ProjectDirModel.ProjectDirNode) {
					projectDirPopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
				} else if (selectedProjectDirNode instanceof ProjectDirModel.FileNode) {
					projectFilePopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
				}
			}
		});
		MessageManager.addGPGEMessageListener(new GPGEMessageListener() {

			public void receiveMessage(GPGEMessage message) {
				if (!(message instanceof ChangeViewMessage)) {
					return;
				}

				ChangeViewMessage changeViewMessage = (ChangeViewMessage) message;
				jobResultFileSendToMenu.removeAll();
				projectFileSendToMenu.removeAll();

				if (changeViewMessage.getType() != ChangeViewMessage.RUN_TASK_SHOWN
						&& changeViewMessage.getType() != ChangeViewMessage.EDIT_PIPELINE_SHOWN) {
					jobResultFileSendToMenu.setEnabled(false);
					projectFileSendToMenu.setEnabled(false);
					runTaskViewShown = false;
					return;
				}
				runTaskViewShown = true;

				final TaskDisplay taskDisplay = (TaskDisplay) changeViewMessage
						.getComponent();
				Iterator inputFileTypes = taskDisplay.getInputFileTypes();
				for (Iterator it = taskDisplay.getInputFileParameters(); it
						.hasNext();) {
					final String name = (String) it.next();
					final String[] fileTypes = (String[]) inputFileTypes.next();
					MenuItemAction jobResultFileSendToMenuItem = new SendToMenuItemAction(
							name, fileTypes) {

						public void actionPerformed(ActionEvent e) {
							taskDisplay
									.sendTo(name, (Sendable) selectedJobNode);
						}
					};

					jobResultFileSendToMenu.add(jobResultFileSendToMenuItem);

					MenuItemAction projectMenuItem = new SendToMenuItemAction(
							name, fileTypes) {
						public void actionPerformed(ActionEvent e) {
							taskDisplay.sendTo(name,
									(Sendable) selectedProjectDirNode);
						}
					};
					projectMenuItem.putValue(GPGE.PARAMETER_NAME, name);
					projectFileSendToMenu.add(projectMenuItem);
				}

				if (selectedJobNode instanceof JobModel.ServerFileNode) {
					jobResultFileSendToMenu.setEnabled(true);
					String kind = ((JobModel.ServerFileNode) selectedJobNode)
							.getFileInfo().getKind();
					enableSendToMenus(jobResultFileSendToMenu, kind);
				}
				if (selectedProjectDirNode instanceof ProjectDirModel.FileNode) {
					projectFileSendToMenu.setEnabled(true);
					String kind = ((ProjectDirModel.FileNode) selectedProjectDirNode)
							.getFileInfo().getKind();
					enableSendToMenus(projectFileSendToMenu, kind);
				}

			}
		});

		JScrollPane projectSP = new JScrollPane(projectDirTree);
		JScrollPane jobSP = new JScrollPane(jobResultsTree);
		int width = (int) (screenSize.width * .9);
		int height = (int) (screenSize.height * .9);

		JPanel projectPanel = new JPanel(new BorderLayout());
		projectPanel.setMinimumSize(new Dimension(200, 200));
		projectPanel.setBackground(new Color(24, 48, 115));
		projectPanel.add(projectSP, BorderLayout.CENTER);
		if (RUNNING_ON_WINDOWS) {
			projectSP.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			jobSP.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		}

		JLabel l = new JLabel("Projects", JLabel.CENTER);
		l.setForeground(Color.white);
		l.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 14));
		projectPanel.add(l, BorderLayout.NORTH);

		JPanel jobPanel = new JPanel(new BorderLayout());
		jobPanel.setMinimumSize(new Dimension(200, 200));
		jobPanel.setBackground(new Color(24, 48, 115));
		jobPanel.add(jobSP, BorderLayout.CENTER);
		JLabel l2 = new JLabel("Results", JLabel.CENTER);
		l2.setForeground(Color.white);
		l2.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 14));
		jobPanel.add(l2, BorderLayout.NORTH);

		final JSplitPane leftPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				projectPanel, jobPanel);
		leftPane.setResizeWeight(0.5);
		if (RUNNING_ON_MAC) {
			leftPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		}

		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.setMinimumSize(new Dimension(200, 200));
		if (RUNNING_ON_WINDOWS) {
			leftPanel.setBackground(Color.white);
			final Border scrollBorder = UIManager
					.getBorder("ScrollPane.border");
			Border b = new Border() {
				public Insets getBorderInsets(Component c) {
					return new Insets(10, 10, 10, 10);
				}

				public boolean isBorderOpaque() {
					return scrollBorder.isBorderOpaque();
				}

				public void paintBorder(Component c, Graphics g, int x, int y,
						int w, int h) {
					scrollBorder.paintBorder(c, g, x, y, w, h);
				}
			};
			fileSummaryComponent.setBorder(b);
		}

		leftPanel.add(leftPane, BorderLayout.CENTER);
		leftPanel.add(fileSummaryComponent, BorderLayout.SOUTH);

		final JSplitPane splitPane = new JSplitPane(
				JSplitPane.HORIZONTAL_SPLIT, leftPanel, null);
		new ViewManager(splitPane);
		MessageManager.notifyListeners(new ChangeViewMessageRequest(this,
				ChangeViewMessageRequest.SHOW_GETTING_STARTED_REQUEST));
		splitPane.setResizeWeight(0.5);
		splitPane.setMinimumSize(new Dimension(400, 400));
		frame.getContentPane().add(splitPane, BorderLayout.CENTER);
		if (!RUNNING_ON_MAC) {
			frame.getContentPane().setBackground(Color.white);
		}
		int x = 0;
		int y = 0;
		int leftPaneDividerLocation = 0;
		int splitPaneDividerLocation = 0;
		boolean savedLayout = true;
		try {
			String[] tokens = PropertyManager.getProperty(
					PreferenceKeys.WINDOW_LAYOUT).split(",");
			width = Integer.parseInt(tokens[0]);
			height = Integer.parseInt(tokens[1]);
			x = Integer.parseInt(tokens[2]);
			y = Integer.parseInt(tokens[3]);
			leftPaneDividerLocation = Integer.parseInt(tokens[4]);
			splitPaneDividerLocation = Integer.parseInt(tokens[5]);
		} catch (Exception e) {
			e.printStackTrace();
			savedLayout = false;
		}
		if (!savedLayout) {
			x = (screenSize.width - width) / 2;
			y = 20;
			leftPaneDividerLocation = (int) (height * 0.4);
			splitPaneDividerLocation = (int) (width * 0.4);
		}

		frame.setSize(width, height);
		frame.setLocation(x, y);
		displayServerStatus();
		leftPane.setDividerLocation(leftPaneDividerLocation);
		splitPane.setDividerLocation(splitPaneDividerLocation);
		frame.setJMenuBar(menuBar);
		frame.show();
		frame.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent event) {
				frame.setSize(Math.max(100, frame.getWidth()), Math.max(100,
						frame.getHeight()));
			}
		});

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public final void run() {
				try {
					PropertyManager.setProperty(PreferenceKeys.WINDOW_LAYOUT,
							frame.getWidth() + "," + frame.getHeight() + ","
									+ frame.getLocation().x + ","
									+ frame.getLocation().y + ","
									+ leftPane.getDividerLocation() + ","
									+ splitPane.getDividerLocation());
					PropertyManager.saveProperties();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		});
	}

	void addToWindowMenu(String name, final java.awt.Component c) {
		JMenuItem mi = new JMenuItem(name);
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				c.setVisible(true);
				if (c instanceof JInternalFrame) {
					((JInternalFrame) c).toFront();
					try {
						((JInternalFrame) c).setSelected(true);
					} catch (java.beans.PropertyVetoException pe) {
					}

				}
			}
		});
		windowMenu.add(mi);
	}

	static class SendToMenuItemAction extends MenuItemAction {
		String[] fileFormats;

		public SendToMenuItemAction(String text, String[] fileFormats) {
			super(text);
			this.fileFormats = fileFormats;

		}

		public boolean isCorrectKind(String kind) {
			if (fileFormats == null || fileFormats.length == 0 || kind == null
					|| kind.equals("")) {
				return true;
			}
			for (int i = 0; i < fileFormats.length; i++) {
				if (fileFormats[i].equalsIgnoreCase(kind)) {
					return true;
				}
			}
			return false;
		}
	}

	static class MessageDialog extends CenteredDialog {
		private static MessageDialog instance;

		private JLabel label = new JLabel("      ");

		private JProgressBar progressBar = new JProgressBar();

		public static void init(java.awt.Frame parent) {
			instance = new MessageDialog(parent);
		}

		public static MessageDialog getInstance() {
			return instance;
		}

		private MessageDialog(java.awt.Frame parent) {
			super(parent);
			progressBar.setIndeterminate(true);
			label.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 14));
			label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			setTitle("");
			getContentPane().add(label);
			getContentPane().add(progressBar, BorderLayout.SOUTH);
			setResizable(false);
			pack();
		}

		public void setText(final String s) {
			Thread thread = new Thread() {
				public void run() {
					label.setText(s);
					pack();
					setVisible(true);
				}
			};
			if (SwingUtilities.isEventDispatchThread()) {
				thread.run();
			} else {
				SwingUtilities.invokeLater(thread);
			}
		}
	}

	public void refreshJobs(boolean displayMessage) {
		if (displayMessage) {
			MessageDialog.getInstance().setText("Retrieving your jobs.");
		}
		final List errors = new ArrayList();

		Thread updateJobs = new Thread() {
			public void run() {
				try {
					jobModel.getJobsFromServer();
				} catch (WebServiceException wse) {
					wse.printStackTrace();
					synchronized (errors) {
						if (errors.size() == 0) {
							if (!disconnectedFromServer(wse)) {
								GenePattern
										.showErrorDialog("An error occurred while retrieving your jobs.");
							}
							errors.add(new Object());
						}
					}
				}
			}
		};
		updateJobs.start();

		Thread updateHistory = new Thread() {
			public void run() {
				historyMenu.setEnabled(false);
				String server = AnalysisServiceManager.getInstance()
						.getServer();
				String username = AnalysisServiceManager.getInstance()
						.getUsername();
				try {
					HistoryModel.getInstance().updateHistory();
				} catch (WebServiceException wse) {
					wse.printStackTrace();
					synchronized (errors) {
						if (errors.size() == 0) {
							if (!disconnectedFromServer(wse)) {
								GenePattern
										.showErrorDialog("An error occurred while retrieving your history.");
							}
							errors.add(new Object());
						}
					}
				} finally {
					historyMenu.setEnabled(true);
				}
			}
		};
		updateHistory.start();

		try {
			updateJobs.join();
			updateHistory.join();

		} catch (InterruptedException x) {
		}
		if (displayMessage) {
			MessageDialog.getInstance().setVisible(false);
		}
	}

	private void createProjectFileActions() {
		projectFileSendToMenu = new MenuAction("Send To", IconManager
				.loadIcon(IconManager.SEND_TO_ICON));
		projectFileSendToMenu.setEnabled(false);

		projectFileViewModulesMenu = new MenuAction("Modules");
		projectFileViewModulesMenu.setEnabled(false);

		projectFileOpenWithMenu = new MenuAction("Open With");
		MenuItemAction projectFileTextViewerMenuItem = new MenuItemAction(
				"Text Viewer", IconManager.loadIcon(IconManager.TEXT_ICON)) {
			public void actionPerformed(ActionEvent e) {
				new Thread() {
					public void run() {
						textViewer(selectedProjectDirNode);
					}
				}.start();
			}
		};
		projectFileOpenWithMenu.add(projectFileTextViewerMenuItem);

		projectFileDefaultAppMenuItem = new MenuItemAction(
				"Default Application") {
			public void actionPerformed(ActionEvent e) {
				new Thread() {
					public void run() {
						defaultApplication(selectedProjectDirNode);
					}
				}.start();
			}
		};
		projectFileOpenWithMenu.add(projectFileDefaultAppMenuItem);

		if (RUNNING_ON_MAC) {
			revealFileMenuItem = new MenuItemAction("Show In Finder") {
				public void actionPerformed(ActionEvent e) {
					ProjectDirModel.FileNode fn = (ProjectDirModel.FileNode) selectedProjectDirNode;
					org.genepattern.gpge.util.MacOS.showFileInFinder(fn.file);
				}
			};

		} else if (System.getProperty("os.name").startsWith("Windows")) {
			revealFileMenuItem = new MenuItemAction("Show File Location") {
				public void actionPerformed(ActionEvent e) {
					try {
						ProjectDirModel.FileNode fn = (ProjectDirModel.FileNode) selectedProjectDirNode;
						BrowserLauncher.openURL(fn.file.getParentFile()
								.getCanonicalPath());
					} catch (IOException x) {
						x.printStackTrace();
					}
				}
			};

		}
	}

	private void createProjectDirActions() {
		refreshProjectMenuItem = new MenuItemAction("Refresh", IconManager
				.loadIcon(IconManager.REFRESH_ICON)) {
			public void actionPerformed(ActionEvent e) {
				projectDirModel
						.refresh((ProjectDirModel.ProjectDirNode) selectedProjectDirNode);
			}
		};

		removeProjectMenuItem = new MenuItemAction("Close Project", IconManager
				.loadIcon(IconManager.REMOVE_ICON)) {
			public void actionPerformed(ActionEvent e) {
				projectDirModel
						.remove((ProjectDirModel.ProjectDirNode) selectedProjectDirNode);
				PropertyManager.setProperty(PreferenceKeys.PROJECT_DIRS,
						projectDirModel.getPreferencesString());
			}
		};

	}

	private boolean showConfirmDialog(String message) {
		return GUIUtil.showConfirmDialog(message);
	}

	private void createJobActions() {
		reloadMenuItem = new MenuItemAction("Reload") {
			public void actionPerformed(ActionEvent e) {
				reload(((JobModel.JobNode) selectedJobNode).job);
			}
		};

		deleteJobAction = new MenuItemAction("Delete Job", IconManager
				.loadIcon(IconManager.DELETE_ICON)) {
			public void actionPerformed(ActionEvent e) {
				final JobModel.JobNode jobNode = (JobModel.JobNode) selectedJobNode;

				String message = "Are you sure you want to delete job number "
						+ jobNode.job.getJobInfo().getJobNumber() + "?";
				if (showConfirmDialog(message)) {
					new Thread() {
						public void run() {
							try {
								jobModel.delete(jobNode);
							} catch (WebServiceException wse) {
								wse.printStackTrace();
								if (!disconnectedFromServer(wse)) {
									GenePattern
											.showErrorDialog("An error occurred deleting job number "
													+ jobNode.job.getJobInfo()
															.getJobNumber()
													+ ".");
								}
							}
						}
					}.start();
				}

			}
		};

		deleteAllJobsAction = new MenuItemAction("Delete All Jobs") {
			public void actionPerformed(ActionEvent e) {

				String message = "Are you sure you want to delete all jobs?";
				if (showConfirmDialog(message)) {
					new Thread() {
						public void run() {
							try {
								jobModel.deleteAll();
							} catch (WebServiceException wse) {
								wse.printStackTrace();
								if (!disconnectedFromServer(wse)) {
									GenePattern
											.showErrorDialog("An error occurred while deleting all jobs.");
								}
							}
						}
					}.start();
				}
			}
		};

		terminateJobAction = new MenuItemAction("Terminate Job", IconManager
				.loadIcon(IconManager.STOP_ICON)) {
			public void actionPerformed(ActionEvent e) {
				final JobModel.JobNode jobNode = (JobModel.JobNode) selectedJobNode;
				new Thread() {
					public void run() {
						try {
							AnalysisWebServiceProxy p = new AnalysisWebServiceProxy(
									analysisServiceManager.getServer(),
									analysisServiceManager.getUsername(), false);
							p.terminateJob(jobNode.job.getJobInfo()
									.getJobNumber());
						} catch (WebServiceException wse) {
							wse.printStackTrace();
							if (!disconnectedFromServer(wse)) {
								GenePattern
										.showErrorDialog("An error occurred terminating job number "
												+ jobNode.job.getJobInfo()
														.getJobNumber() + ".");
							}
						}
					}
				}.start();
			}
		};

		viewCodeAction = new MenuAction("View Code");

		MenuItemAction viewJavaCodeAction = new MenuItemAction("Java") {
			public void actionPerformed(ActionEvent e) {
				viewCode(
						new org.genepattern.codegenerator.JavaPipelineCodeGenerator(),
						"Java");
			}
		};

		viewCodeAction.add(viewJavaCodeAction);

		MenuItemAction viewMATLABCodeAction = new MenuItemAction("MATLAB") {
			public void actionPerformed(ActionEvent e) {
				viewCode(
						new org.genepattern.codegenerator.MATLABPipelineCodeGenerator(),
						"MATLAB");
			}
		};
		viewCodeAction.add(viewMATLABCodeAction);

		MenuItemAction viewRCodeAction = new MenuItemAction("R") {
			public void actionPerformed(ActionEvent e) {
				viewCode(
						new org.genepattern.codegenerator.RPipelineCodeGenerator(),
						"R");
			}
		};
		viewCodeAction.add(viewRCodeAction);

	}

	/**
	 * Generates code for the selected job node
	 */
	private void viewCode(
			org.genepattern.codegenerator.TaskCodeGenerator codeGenerator,
			final String language) {
		JobModel.JobNode jobNode = (JobModel.JobNode) selectedJobNode;
		org.genepattern.gpge.ui.code.Util.viewCode(codeGenerator, jobNode.job,
				language);
	}

	private void createJobResultFileActions() {
		jobResultFileViewModulesMenu = new MenuAction("Modules");
		jobResultFileViewModulesMenu.setEnabled(false);

		jobResultFileSendToMenu = new MenuAction("Send To", IconManager
				.loadIcon(IconManager.SEND_TO_ICON));
		jobResultFileSendToMenu.setEnabled(false);

		saveServerFileMenu = new MenuAction("Save To");

		saveToFileSystemMenuItem = new MenuItemAction("Other...", IconManager
				.loadIcon(IconManager.SAVE_AS_ICON));
		saveToFileSystemMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showSaveDialog((JobModel.ServerFileNode) selectedJobNode);
			}
		});
		projectDirModel
				.addProjectDirectoryListener(new ProjectDirectoryListener() {
					public void projectAdded(ProjectEvent e) {
						final File dir = e.getDirectory();
						MenuItemAction menuItem = new MenuItemAction(dir
								.getPath(), IconManager
								.loadIcon(IconManager.SAVE_ICON)) {
							public void actionPerformed(ActionEvent e) {
								new Thread() {
									public void run() {
										JobModel.ServerFileNode node = (JobModel.ServerFileNode) selectedJobNode;
										File outputFile = new File(dir,
												node.name);

										try {
											if (GUIUtil
													.overwriteFile(outputFile)) {
												node.download(outputFile);

												projectDirModel.refresh(dir);
											}
										} catch (IOException ioe) {
											ioe.printStackTrace();
											GenePattern
													.showErrorDialog("An error occurred while saving the file "
															+ node.name + ".");
										}
										// } catch (WebServiceException wse) {
										// if(!disconnectedFromServer(wse)) {
										// GenePattern.showErrorDialog("An error
										// occurred while saving the file " +
										// node.name + ". Please try again.");
										// }
										// }
									}
								}.start();
							}
						};
						saveServerFileMenu.insert(menuItem, projectDirModel
								.indexOf(dir) + 1);
					}

					public void projectRemoved(ProjectEvent e) {
						File dir = e.getDirectory();
						for (int i = 0; i < saveServerFileMenu.getItemCount(); i++) {
							MenuItemAction m = (MenuItemAction) saveServerFileMenu
									.getMenuComponent(i);
							String text = (String) m
									.getValue(MenuItemAction.NAME);
							if (text.equals(dir.getPath())) {
								saveServerFileMenu.remove(i);
								break;
							}
						}
					}
				});
		saveServerFileMenu.add(saveToFileSystemMenuItem);

		deleteFileMenuItem = new MenuItemAction("Delete File", IconManager
				.loadIcon(IconManager.DELETE_ICON)) {
			public void actionPerformed(ActionEvent e) {
				JobModel.ServerFileNode serverFileNode = (JobModel.ServerFileNode) selectedJobNode;
				String message = "Are you sure you want to delete "
						+ serverFileNode.toString() + "?";
				try {
					if (showConfirmDialog(message)) {
						jobModel.delete(serverFileNode);
					}
				} catch (WebServiceException wse) {
					wse.printStackTrace();
					if (!disconnectedFromServer(wse)) {
						GenePattern
								.showErrorDialog("An error occurred while deleting the file "
										+ JobModel
												.getJobResultFileName(serverFileNode)
										+ ".");
					}
				}
			}
		};

		createPipelineMenuItem = new MenuItemAction("Create Pipeline") {
			public void actionPerformed(ActionEvent e) {
				final JobModel.ServerFileNode serverFileNode = (JobModel.ServerFileNode) selectedJobNode;
				final String pipelineName = JOptionPane.showInputDialog(frame,
						"Pipeline name", "GenePattern",
						JOptionPane.QUESTION_MESSAGE);

				if (pipelineName != null) {
					new Thread() {
						public void run() {
							try {
								AnalysisWebServiceProxy proxy = new AnalysisWebServiceProxy(
										analysisServiceManager.getServer(),
										analysisServiceManager.getUsername());
								System.out.println("creating pipeline for "
										+ serverFileNode.getURL());
								String lsid = proxy.createProvenancePipeline(
										serverFileNode.getURL().toString(),
										pipelineName);
								taskInstalled(new LSID(lsid));
								MessageManager
										.notifyListeners(new ChangeViewMessageRequest(
												this,
												ChangeViewMessageRequest.SHOW_VIEW_PIPELINE_REQUEST,
												analysisServiceManager
														.getAnalysisService(lsid)));
							} catch (WebServiceException wse) {
								wse.printStackTrace();
								if (!disconnectedFromServer(wse)) {
									GenePattern
											.showErrorDialog("An error occurred while creating the pipeline");
								}
							} catch (MalformedURLException e) {
								e.printStackTrace();
							}

						}
					}.start();

				}

			}
		};

		openWithMenu = new MenuAction("Open With");

		jobResultFileTextViewerMenuItem = new MenuItemAction("Text Viewer",
				IconManager.loadIcon(IconManager.TEXT_ICON)) {
			public void actionPerformed(ActionEvent e) {
				new Thread() {
					public void run() {
						textViewer(selectedJobNode);
					}
				}.start();
			}
		};
		openWithMenu.add(jobResultFileTextViewerMenuItem);

		jobResultFileDefaultAppMenuItem = new MenuItemAction(
				"Default Application") {
			public void actionPerformed(ActionEvent e) {
				new Thread() {
					public void run() {
						defaultApplication(selectedJobNode);
					}
				}.start();
			}
		};
		openWithMenu.add(jobResultFileDefaultAppMenuItem);
	}

	private void setChangeServerActionsEnabled(final boolean b) {
		Thread disableActions = new Thread() {
			public void run() {
				analysisMenu.setEnabled(b);
				visualizerMenu.setEnabled(b);
				pipelineMenu.setEnabled(b);
				fileMenu.changeServerActionsEnabled(b);
				historyMenu.setEnabled(b);
				suiteMenu.setEnabled(b);
			}
		};
		if (SwingUtilities.isEventDispatchThread()) {
			disableActions.run();
		} else {
			SwingUtilities.invokeLater(disableActions);
		}
	}

	public void rebuildTasksUI() {
		inputTypeToMenuItemsMap = SemanticUtil
				.getInputTypeToMenuItemsMap(analysisServiceManager
						.getLatestAnalysisServices());

		new Thread() {
			public void run() {
				SwingUtilities.invokeLater(new Thread() {
					public void run() {
						Map categoryToAnalysisServices = AnalysisServiceUtil
								.getCategoryToAnalysisServicesMap(analysisServiceManager
										.getLatestAnalysisServices());

						if (categoryToAnalysisServices.size() == 0) {
							showNoModulesInstalledDialog();
						}
						analysisMenu.rebuild(categoryToAnalysisServices);
						visualizerMenu.rebuild(categoryToAnalysisServices);
						pipelineMenu.rebuild(categoryToAnalysisServices);
					}
				});
			}
		}.start();
	}

	private void taskInstalled(LSID lsid) {
		analysisServiceManager.taskInstalled(lsid);
		rebuildTasksUI();
	}

	public void refreshModules(boolean displayMessage) {
		if (displayMessage) {
			MessageDialog.getInstance().setText("Retrieving modules");
		}

		setChangeServerActionsEnabled(false);
		Thread thread = new Thread() {
			public void run() {
				try {
					analysisServiceManager.refresh();
				} catch (WebServiceException wse) {
					wse.printStackTrace();
					if (!disconnectedFromServer(wse)) {
						GenePattern
								.showErrorDialog("An error occurred while retrieving the modules from the server.");
					}
				}

				final Collection latestTasks = analysisServiceManager
						.getLatestAnalysisServices();

				inputTypeToMenuItemsMap = SemanticUtil
						.getInputTypeToMenuItemsMap(latestTasks);
				MessageManager.notifyListeners(new RefreshMessage(this));
				SwingUtilities.invokeLater(new Thread() {
					public void run() {
						Map categoryToAnalysisServices = AnalysisServiceUtil
								.getCategoryToAnalysisServicesMap(latestTasks);
						if (categoryToAnalysisServices.size() == 0) {
							showNoModulesInstalledDialog();
						}
						analysisMenu.rebuild(categoryToAnalysisServices);
						visualizerMenu.rebuild(categoryToAnalysisServices);
						pipelineMenu.rebuild(categoryToAnalysisServices);
						setChangeServerActionsEnabled(true);
						suiteMenu.rebuild();
					}
				});
			}
		};
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException x) {
		}
		if (displayMessage) {
			MessageDialog.getInstance().setVisible(false);
		}
	}

	void showNoModulesInstalledDialog() {
		final JTextPane pane = new JTextPane();
		pane.setContentType("text/html");
		pane.addHyperlinkListener(new HyperlinkListener() {
			public void hyperlinkUpdate(HyperlinkEvent evt) {
				if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					URL url = evt.getURL();
					try {
						BrowserLauncher.openURL(url.toString());
					} catch (Exception e) {
					}
				} else if (evt.getEventType() == HyperlinkEvent.EventType.ENTERED) {
					pane.setCursor(Cursor
							.getPredefinedCursor(Cursor.HAND_CURSOR));
				} else if (evt.getEventType() == HyperlinkEvent.EventType.EXITED) {
					pane.setCursor(Cursor.getDefaultCursor());
				}
			}
		});

		try {
			String server = analysisServiceManager.getServer()
					+ "/gp/taskCatalog.jsp";
			String text = "<html><body><font face=\"Arial, Helvetica, sans-serif\">There are no modules installed on the server. Go to "
					+ "<a href="
					+ server
					+ ">"
					+ server
					+ "</a> to install modules.<br>After installing modules, click File > Refresh > Modules to retrieve the modules from the server.";

			pane.setText(text);
		} catch (Exception e) {
			e.printStackTrace();
		}
		pane.setMargin(new Insets(5, 5, 5, 5));
		pane.setEditable(false);
		JOptionPane.showMessageDialog(GenePattern.getDialogParent(), pane,
				"GenePattern", JOptionPane.WARNING_MESSAGE);
	}

	void createMenus() {
		menuBar = new JMenuBar();
		fileMenu = new FileMenu();
		menuBar.add(fileMenu);
		refreshProjectMenuItem.setEnabled(false);
		removeProjectMenuItem.setEnabled(false);
		projectFileSendToMenu.setEnabled(false);
		projectFileOpenWithMenu.setEnabled(false);
		if (revealFileMenuItem != null) {
			revealFileMenuItem.setEnabled(false);
		}
		reloadMenuItem.setEnabled(false);
		deleteJobAction.setEnabled(false);
		terminateJobAction.setEnabled(false);
		jobResultFileSendToMenu.setEnabled(false);
		saveServerFileMenu.setEnabled(false);
		deleteFileMenuItem.setEnabled(false);
		createPipelineMenuItem.setEnabled(false);
		openWithMenu.setEnabled(false);
		viewCodeAction.setEnabled(false);

		MenuAction projectsMenuAction = null;
		if (revealFileMenuItem != null) {
			projectsMenuAction = new MenuAction("Projects", new Object[] {
					refreshProjectMenuItem, removeProjectMenuItem,
					new JSeparator(), projectFileSendToMenu,
					projectFileOpenWithMenu, revealFileMenuItem,
					projectFileViewModulesMenu });
		} else {
			projectsMenuAction = new MenuAction("Projects", new Object[] {
					refreshProjectMenuItem, removeProjectMenuItem,
					new JSeparator(), projectFileSendToMenu,
					projectFileOpenWithMenu, projectFileViewModulesMenu });
		}

		menuBar.add(projectsMenuAction.createMenu());

		MenuAction jobResultsMenuAction = new MenuAction("Results",
				new Object[] { reloadMenuItem, deleteJobAction,
						deleteAllJobsAction, terminateJobAction,
						viewCodeAction, new JSeparator(),
						jobResultFileSendToMenu, saveServerFileMenu,
						deleteFileMenuItem, openWithMenu,
						jobResultFileViewModulesMenu, createPipelineMenuItem });

		menuBar.add(jobResultsMenuAction.createMenu());

		analysisMenu = new AnalysisMenu(AnalysisMenu.DATA_ANALYZERS);
		analysisMenu.setEnabled(false);
		menuBar.add(analysisMenu);
		visualizerMenu = new AnalysisMenu(AnalysisMenu.VISUALIZERS);
		visualizerMenu.setEnabled(false);
		menuBar.add(visualizerMenu);

		pipelineMenu = new AnalysisMenu(AnalysisMenu.PIPELINES);

		MessageManager.addGPGEMessageListener(new GPGEMessageListener() {

			public void receiveMessage(GPGEMessage message) {
				if (message instanceof TaskInstallMessage) {
					LSID lsid = ((TaskInstallMessage) message).getLsid();
					taskInstalled(lsid);
				} else if (message instanceof SuiteInstallMessage) {
					suiteMenu.rebuild();
				}
			}
		});
		pipelineMenu.setEnabled(false);
		menuBar.add(pipelineMenu);

		suiteMenu = new SuiteMenu();
		suiteMenu.setEnabled(false);
		menuBar.add(suiteMenu);

		historyMenu = new HistoryMenu();
		historyMenu.setEnabled(false);
		menuBar.add(historyMenu);

		JMenu helpMenu = new HelpMenu();
		menuBar.add(helpMenu);

		if (RUNNING_ON_MAC) {
			macos.MacOSMenuHelper.registerHandlers();
		}

		MenuAction jobPopupAction = new MenuAction("", new Object[] {
				reloadMenuItem, deleteJobAction, terminateJobAction,
				viewCodeAction });
		jobPopupMenu = jobPopupAction.createPopupMenu();

		MenuAction jobResultFilePopupAction = new MenuAction("", new Object[] {
				jobResultFileSendToMenu, saveServerFileMenu,
				deleteFileMenuItem, openWithMenu, jobResultFileViewModulesMenu,
				createPipelineMenuItem });
		jobResultFilePopupMenu = jobResultFilePopupAction.createPopupMenu();

		MenuAction projectDirPopupMenuAction = new MenuAction("", new Object[] {
				refreshProjectMenuItem, removeProjectMenuItem });
		projectDirPopupMenu = projectDirPopupMenuAction.createPopupMenu();

		MenuAction projectFilePopupMenuAction = new MenuAction("",
				new Object[] { projectFileSendToMenu, projectFileOpenWithMenu,
						revealFileMenuItem, projectFileViewModulesMenu });
		projectFilePopupMenu = projectFilePopupMenuAction.createPopupMenu();
	}

	public static class JobNumberComparator implements java.util.Comparator {
		public int compare(Object obj1, Object obj2) {
			Integer job1Number = new Integer(((AnalysisJob) obj1).getJobInfo()
					.getJobNumber());
			Integer job2Number = new Integer(((AnalysisJob) obj2).getJobInfo()
					.getJobNumber());
			return job2Number.compareTo(job1Number);

		}
	}

	class AnalysisMenu extends JMenu {
		int type;

		static final int VISUALIZERS = 1;

		static final int DATA_ANALYZERS = 2;

		static final int PIPELINES = 3;

		ActionListener serviceSelectedListener;

		public AnalysisMenu(int type) {
			if (type == VISUALIZERS) {
				setText("Visualization");
			} else if (type == DATA_ANALYZERS) {
				setText("Analysis");
			} else if (type == PIPELINES) {
				setText("Pipelines");
			} else {
				throw new IllegalArgumentException("Unknown type");
			}
			this.type = type;
			serviceSelectedListener = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					AnalysisMenuItem mi = (AnalysisMenuItem) e.getSource();
					MessageManager
							.notifyListeners(new ChangeViewMessageRequest(
									this,
									ChangeViewMessageRequest.SHOW_RUN_TASK_REQUEST,
									mi.svc));
				}
			};
		}

		private void add(JMenu menu, List services) {
			if (services == null) {
				return;
			}
			for (int i = 0; i < services.size(); i++) {
				AnalysisMenuItem mi = new AnalysisMenuItem(
						(AnalysisService) services.get(i));
				mi.addActionListener(serviceSelectedListener);
				menu.add(mi);
			}
		}

		public void rebuild(Map categoryToAnalysisServices) {
			removeAll();
			if (type == PIPELINES) {
				JMenuItem newPipelineItem = new JMenuItem("New");
				newPipelineItem.addActionListener(new ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent e) {
						MessageManager
								.notifyListeners(new ChangeViewMessageRequest(
										GPGE.this,
										ChangeViewMessageRequest.SHOW_EDIT_PIPELINE_REQUEST));
					}
				});
				add(newPipelineItem);
				add(new JSeparator());
			}

			if (type == DATA_ANALYZERS) {
				for (Iterator keys = categoryToAnalysisServices.keySet()
						.iterator(); keys.hasNext();) {
					String category = (String) keys.next();
					if (category
							.equalsIgnoreCase(GPConstants.TASK_TYPE_VISUALIZER)
							|| category.equalsIgnoreCase("Image Creators")
							|| category.equalsIgnoreCase("Pipeline")) {
						continue;
					}
					List services = (List) categoryToAnalysisServices
							.get(category);
					JMenu menu = new JMenu(category);
					add(menu, services);
					this.add(menu);
				}
			} else if (type == VISUALIZERS) {
				List visualizers = (List) categoryToAnalysisServices
						.get(GPConstants.TASK_TYPE_VISUALIZER);

				List imageCreators = (List) categoryToAnalysisServices
						.get("Image Creators");
				List all = new ArrayList();
				if (visualizers != null) {
					all.addAll(visualizers);
				}
				if (imageCreators != null) {
					all.addAll(imageCreators);
				}
				Collections
						.sort(
								all,
								AnalysisServiceUtil.CASE_INSENSITIVE_TASK_NAME_COMPARATOR);
				add(this, all);
			} else {
				List pipelines = (List) categoryToAnalysisServices
						.get("pipeline");
				add(this, pipelines);
			}

		}

	}

	static class AnalysisMenuItem extends JMenuItem {
		AnalysisService svc;

		public AnalysisMenuItem(AnalysisService svc) {
			String name = svc.getTaskInfo().getName();
			if (name.endsWith(".pipeline")) {
				name = name.substring(0, name.length() - ".pipeline".length());
			}
			setText(name);
			this.svc = svc;
			String lsid = (String) svc.getTaskInfo().getTaskInfoAttributes()
					.get(GPConstants.LSID);
			try {
				String authType = org.genepattern.util.LSIDUtil.getInstance()
						.getAuthorityType(new org.genepattern.util.LSID(lsid));

				if (authType
						.equals(org.genepattern.util.LSIDUtil.AUTHORITY_MINE)) {
					setForeground(authorityMineColor);
				} else if (authType
						.equals(org.genepattern.util.LSIDUtil.AUTHORITY_FOREIGN)) {
					setForeground(authorityForeignColor);
				} else { // Broad task
					setForeground(authorityBroadColor);
				}
			} catch (MalformedURLException mfe) {
				mfe.printStackTrace();
			}
		}
	}

	class HelpMenu extends JMenu {
		public HelpMenu() {
			super("Help");

			if (!RUNNING_ON_MAC) {
				JMenuItem aboutMenuItem = new JMenuItem("About");
				add(aboutMenuItem);
				aboutMenuItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						GenePattern.showAbout();
					}
				});
			}

			JMenuItem genePatternWebSiteMenuItem = new JMenuItem(
					"GenePattern Web Site");
			add(genePatternWebSiteMenuItem);
			genePatternWebSiteMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						BrowserLauncher.openURL("http://www.genepattern.org");
					} catch (IOException ioe) {
					}
				}
			});

			JMenuItem genePatternTutorialMenuItem = new JMenuItem(
					"GenePattern Tutorial");
			add(genePatternTutorialMenuItem);
			genePatternTutorialMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						BrowserLauncher
								.openURL("http://www.genepattern.org/tutorial");
					} catch (IOException ioe) {
					}
				}
			});

			JMenuItem genePatternHomeMenuItem = new JMenuItem(
					"GenePattern Server");
			add(genePatternHomeMenuItem);
			genePatternHomeMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						BrowserLauncher.openURL(AnalysisServiceManager
								.getInstance().getServer()
								+ "/gp/");
					} catch (IOException ioe) {
					}
				}
			});

			JMenuItem gettingStartedMenuItem = new JMenuItem("Getting Started");
			add(gettingStartedMenuItem);
			gettingStartedMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					MessageManager
							.notifyListeners(new ChangeViewMessageRequest(
									this,
									ChangeViewMessageRequest.SHOW_GETTING_STARTED_REQUEST));
				}
			});

			JMenuItem moduleColorKeyMenuItem = new JMenuItem("Module Color Key");
			add(moduleColorKeyMenuItem);
			moduleColorKeyMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JPanel p = new JPanel();
					p.setLayout(new GridLayout(3, 1));
					JLabel colorKeyLabel = new JLabel("color key:");
					JLabel yourTasksLabel = new JLabel("your modules");
					yourTasksLabel.setForeground(authorityMineColor);
					JLabel broadTasksLabel = new JLabel("Broad modules");
					broadTasksLabel.setForeground(authorityBroadColor);

					JLabel otherTasksLabel = new JLabel("other modules");
					otherTasksLabel.setForeground(authorityForeignColor);

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
							JOptionPane.INFORMATION_MESSAGE, GenePattern
									.getIcon());
				}
			});

			JMenuItem errorsMenuItem = new JMenuItem("Errors", IconManager
					.loadIcon(IconManager.ERROR_ICON));

			add(errorsMenuItem);
			errorsMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					GenePattern.showErrors();
				}
			});
		}
	}

	class SuiteMenu extends JMenu {

		public SuiteMenu() {
			super("Suites");

		}

		public void rebuild() {
			removeAll();
			JMenuItem createSuiteMenuItem = new JMenuItem("New");
			createSuiteMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					MessageManager
							.notifyListeners(new ChangeViewMessageRequest(
									GPGE.this,
									ChangeViewMessageRequest.SHOW_EDIT_SUITE_REQUEST));
				}
			});
			add(createSuiteMenuItem);

			JMenuItem suitesMenuItem = new JMenuItem("Filter...");
			suitesMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					new SuitesPreferences(GenePattern.getDialogParent());
				}
			});
			add(suitesMenuItem);

			add(new JSeparator());

			try {
				final AdminProxy proxy = new AdminProxy(AnalysisServiceManager
						.getInstance().getServer(), AnalysisServiceManager
						.getInstance().getUsername());
				SuiteInfo[] suites = proxy.getLatestSuites();
				Arrays.sort(suites, new Comparator() {
					public int compare(Object o1, Object o2) {
						SuiteInfo s1 = (SuiteInfo) o1;
						SuiteInfo s2 = (SuiteInfo) o2;
						return s1.getName().compareToIgnoreCase(s2.getName());
					}
				});

				ActionListener l = new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						SuiteMenuItem s = (SuiteMenuItem) e.getSource();
						try {
							MessageManager
									.notifyListeners(new ChangeViewMessageRequest(
											GPGE.this,
											ChangeViewMessageRequest.SHOW_EDIT_SUITE_REQUEST,
											proxy.getSuite(s.lsid)));
						} catch (WebServiceException e1) {
							e1.printStackTrace();
						}
					}
				};
				for (int i = 0; i < suites.length; i++) {
					JMenuItem mi = new SuiteMenuItem(suites[i].getName(),
							suites[i].getLsid());
					add(mi);
					mi.addActionListener(l);
				}
			} catch (WebServiceException e1) {
				e1.printStackTrace();
			}
		}

	}

	static class SuiteMenuItem extends JMenuItem {
		String lsid;

		public SuiteMenuItem(String name, String lsid) {
			super(name);
			this.lsid = lsid;
		}
	}

	class FileMenu extends JMenu {
		JobCompletedDialog jobCompletedDialog;

		JMenuItem changeServerMenuItem;

		JMenu refreshMenu;

		JMenuItem refreshJobsMenuItem;

		JMenuItem refreshModulesMenuItem;

		JMenuItem importTaskMenuItem;

		JMenuItem importSuiteMenuItem;

		JFileChooser projectDirFileChooser;

		public void changeServerActionsEnabled(boolean b) {
			changeServerMenuItem.setEnabled(b);
			refreshMenu.setEnabled(b);
			refreshJobsMenuItem.setEnabled(b);
			refreshModulesMenuItem.setEnabled(b);
		}

		public void importZip(final boolean isModule) {
			final JDialog d = new CenteredDialog(GenePattern.getDialogParent());
			d.setTitle("Import");
			JLabel label = new JLabel("Zip File:");
			JPanel filePanel = new JPanel();
			FormLayout f = new FormLayout(
					"left:pref:none, 3dlu, left:pref:none, left:pref:none",
					"pref, 5dlu, pref");
			filePanel.setLayout(f);
			final JTextField input = new JTextField(30);
			JButton btn = new JButton("Browse...");
			btn.setBackground(getBackground());
			btn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					File f = GUIUtil.showOpenDialog("Import Zip File");
					if (f != null) {
						try {
							input.setText(f.getCanonicalPath());
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
			});
			CellConstraints cc = new CellConstraints();
			filePanel.add(label, cc.xy(1, 1));
			filePanel.add(input, cc.xy(3, 1));
			filePanel.add(btn, cc.xy(4, 1));

			JLabel privacyLabel = new JLabel("Privacy:");
			final JComboBox privacyComboBox = new JComboBox(new String[] {
					"Public", "Private" });
			privacyComboBox.setSelectedIndex(1);
			filePanel.add(privacyLabel, cc.xy(1, 3));
			filePanel.add(privacyComboBox, cc.xy(3, 3));

			final JButton cancelBtn = new JButton("Cancel");
			final JButton importBtn = new JButton("Import");
			JPanel buttonPanel = new JPanel();
			buttonPanel.add(cancelBtn);
			buttonPanel.add(importBtn);

			ActionListener l = new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					final String s = input.getText().trim();
					final File file = new File(s);

					final int privacy = privacyComboBox.getSelectedIndex() == 0 ? GPConstants.ACCESS_PUBLIC
							: GPConstants.ACCESS_PRIVATE;

					if (e.getSource() == importBtn) {
						new Thread() {
							public void run() {
								try {
									String lsid = null;
									if (file.exists()) {

										lsid = new TaskIntegratorProxy(
												AnalysisServiceManager
														.getInstance()
														.getServer(),
												AnalysisServiceManager
														.getInstance()
														.getUsername())
												.importZip(file, privacy);
									} else {
										lsid = new TaskIntegratorProxy(
												AnalysisServiceManager
														.getInstance()
														.getServer(),
												AnalysisServiceManager
														.getInstance()
														.getUsername())
												.importZipFromURL(s, privacy);
									}
									String _message = null;
									if (isModule) {
										taskInstalled(new LSID(lsid));
										AnalysisService svc = AnalysisServiceManager
												.getInstance()
												.getAnalysisService(lsid);

										_message = "Successfully installed module "
												+ svc.getName() + ".";

									} else {
										MessageManager
												.notifyListeners(new SuiteInstallMessage(
														GPGE.this, lsid));
										_message = "Successfully installed suite.";
									}
									final String message = _message;
									SwingUtilities.invokeLater(new Thread() {
										public void run() {
											GenePattern
													.showMessageDialog(message);
										}
									});

								} catch (WebServiceException wse) {
									wse.printStackTrace();
									if (!disconnectedFromServer(wse)) {
										GenePattern
												.showErrorDialog("An error occurred while importing the zip file.");
									}
								} catch (MalformedURLException e) {
									e.printStackTrace();
								}
							}
						}.start();
					}
					d.dispose();
				}

			};
			cancelBtn.addActionListener(l);
			importBtn.addActionListener(l);
			d.getContentPane().add(filePanel);
			d.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
			d.pack();
			d.show();

		}

		public FileMenu() {
			super("File");

			JMenuItem openProjectDirItem = new JMenuItem(
					"Open Project Directory...", IconManager
							.loadIcon(IconManager.NEW_PROJECT_ICON));

			openProjectDirItem.setAccelerator(KeyStroke.getKeyStroke('O',
					MENU_SHORTCUT_KEY_MASK));
			openProjectDirItem.addActionListener(new ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					if (projectDirFileChooser == null) {
						projectDirFileChooser = new JFileChooser();
						projectDirFileChooser
								.setDialogTitle("Choose a Project Directory");
						projectDirFileChooser
								.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
						projectDirFileChooser
								.setApproveButtonText("Select Directory");
						projectDirFileChooser
								.setAccessory(new org.genepattern.gpge.ui.maindisplay.DirPreview(
										projectDirFileChooser));
					}
					if (projectDirFileChooser.showOpenDialog(GenePattern
							.getDialogParent()) == JFileChooser.APPROVE_OPTION) {
						File selectedFile = projectDirFileChooser
								.getSelectedFile();
						if (selectedFile == null) {
							GenePattern
									.showMessageDialog("No directory selected");
							return;
						}
						if (!projectDirModel.contains(selectedFile)) {
							projectDirModel.add(selectedFile);
							PropertyManager.setProperty(
									PreferenceKeys.PROJECT_DIRS,
									projectDirModel.getPreferencesString());
						}
					}
				}
			});

			add(openProjectDirItem);

			importTaskMenuItem = new JMenuItem("Import Module...", IconManager
					.loadIcon(IconManager.IMPORT_ICON));
			importTaskMenuItem.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					importZip(true);
				}

			});
			add(importTaskMenuItem);

			importSuiteMenuItem = new JMenuItem("Import Suite...", IconManager
					.loadIcon(IconManager.IMPORT_ICON));
			importSuiteMenuItem.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					importZip(false);
				}

			});
			add(importSuiteMenuItem);

			final javax.swing.JCheckBoxMenuItem showJobCompletedDialogMenuItem = new javax.swing.JCheckBoxMenuItem(
					"Alert On Job Completion");
			jobCompletedDialog = new JobCompletedDialog(frame,
					showJobCompletedDialogMenuItem);
			showJobCompletedDialogMenuItem.setSelected(jobCompletedDialog
					.isShowingDialog());
			add(showJobCompletedDialogMenuItem);
			showJobCompletedDialogMenuItem
					.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							jobCompletedDialog
									.setShowDialog(showJobCompletedDialogMenuItem
											.isSelected());
						}
					});

			changeServerMenuItem = new JMenuItem("Server...");
			changeServerMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {

					final ChangeServerDialog dialog = new ChangeServerDialog(
							frame);
					dialog.show(analysisServiceManager.getServer(),
							analysisServiceManager.getUsername(),
							new ActionListener() {
								public void actionPerformed(ActionEvent e) {
									dialog.dispose();
									String server = dialog.getServer();
									String username = dialog.getUsername();
									try {
										int port = Integer.parseInt(dialog
												.getPort());

										server = server + ":" + port;
										if (!server.toLowerCase().startsWith(
												"http://")) {
											server = "http://" + server;
										}
										if (!server
												.equals(analysisServiceManager
														.getServer())
												|| !username
														.equals(analysisServiceManager
																.getUsername())) {
											changeServer(server, username);
										}
									} catch (NumberFormatException nfe) {
										GenePattern
												.showMessageDialog("Invalid port. Please try again.");
									}
								}
							});

				}
			});

			add(changeServerMenuItem);
			changeServerMenuItem.setEnabled(false);

			refreshMenu = new JMenu("Refresh");
			add(refreshMenu);
			refreshModulesMenuItem = new JMenuItem("Modules");
			refreshModulesMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					new Thread() {
						public void run() {
							refreshModules(true);
						}
					}.start();
				}
			});
			refreshModulesMenuItem.setEnabled(false);
			refreshMenu.add(refreshModulesMenuItem);

			refreshJobsMenuItem = new JMenuItem("Jobs");
			refreshJobsMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					new Thread() {
						public void run() {
							refreshJobs(true);
						}
					}.start();
				}
			});
			refreshJobsMenuItem.setEnabled(false);
			refreshMenu.add(refreshJobsMenuItem);

			JMenuItem quitMenuItem = new JMenuItem("Quit");
			quitMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					System.exit(0);
				}
			});
			quitMenuItem.setAccelerator(KeyStroke.getKeyStroke('Q',
					MENU_SHORTCUT_KEY_MASK));

			if (!RUNNING_ON_MAC) {
				add(quitMenuItem);
			}

		}
	}
}