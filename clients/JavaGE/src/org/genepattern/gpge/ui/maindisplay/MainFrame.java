package org.genepattern.gpge.ui.maindisplay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.beans.*;

import javax.swing.*;
import javax.swing.JDesktopPane;
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
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.border.Border;
import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.ui.graphics.draggable.*;
import org.genepattern.gpge.ui.preferences.*;
import org.genepattern.gpge.ui.tasks.*;
import org.genepattern.gpge.ui.project.*;
import org.genepattern.gpge.ui.treetable.*;
import org.genepattern.gpge.util.BuildProperties;
import org.genepattern.modules.ui.graphics.*;
import org.genepattern.util.*;
import org.genepattern.gpge.ui.table.*;
import org.genepattern.webservice.*;
import org.genepattern.gpge.ui.menu.*;
import org.genepattern.gpge.PropertyManager;

/**
 * Description of the Class
 *
 * @author Joshua Gould
 */
public class MainFrame extends JFrame {

	AnalysisServiceDisplay analysisServicePanel;

	AnalysisServiceManager analysisServiceManager;

   private final static Color lightBlue = new Color(239, 239, 255);
	private final static Color DEFAULT_AUTHORITY_MINE_COLOR = Color.MAGENTA;

	private final static Color DEFAULT_AUTHORITY_FOREIGN_COLOR = java.awt.Color
			.decode("0x0000FF");
         
   private final static Color DEFAULT_AUTHORITY_BROAD_COLOR = Color.black;
   
   static Color authorityMineColor, authorityForeignColor, authorityBroadColor;
      
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

	JFileChooser saveAsFileChooser;

	FileMenu fileMenu;
   JMenu windowMenu;
   
   Map inputTypeToMenuItemsMap;
   
	final static int MENU_SHORTCUT_KEY_MASK = Toolkit.getDefaultToolkit()
			.getMenuShortcutKeyMask();

	FileInfoComponent fileSummaryComponent = new FileInfoComponent();

    public static boolean RUNNING_ON_MAC = System.getProperty("mrj.version") != null
			&& javax.swing.UIManager.getSystemLookAndFeelClassName()
					.equals(
							javax.swing.UIManager.getLookAndFeel().getClass()
									.getName());
   public static boolean RUNNING_ON_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");

   private JMenuBar menuBar;
   Color blue = new Color(51,0,204);
	/** The key used for the parameter name for a 'send to' action */
	private static String PARAMETER_NAME = "PARAMETER_NAME";
	
    
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
      return GenePattern.disconnectedFromServer(wse, analysisServiceManager.getServer());
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
         while((s=br.readLine())!=null) {
            sb.append(s);
            sb.append("\n");
         }
      } finally {
         if(br!=null) {
            try {
               br.close();
            } catch(IOException x){}
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
               String[] args = new String[] { "/usr/bin/open",
                     filePath };
               Runtime.getRuntime().exec(args);
            } else {
               org.genepattern.util.BrowserLauncher
                     .openURL(filePath);
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
            String filePath = destination
                  .getCanonicalPath();
            if (RUNNING_ON_MAC) {
               String[] args = new String[] {
                     "/usr/bin/open", filePath };
               Runtime.getRuntime().exec(args);
            } else {
               org.genepattern.util.BrowserLauncher
                     .openURL(filePath);
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
      if(node instanceof JobModel.ServerFileNode) {
         JobModel.ServerFileNode jobResult = (JobModel.ServerFileNode) node;
         JobModel.JobNode jobNode = (JobModel.JobNode) jobResult.getParent();
         try {
            file = File.createTempFile("tmp", null);
            deleteFile = true;
            JobModel.downloadJobResultFile(jobNode.job, jobResult.index, file);
            title = JobModel.getJobResultFileName(jobNode.job, jobResult.index) + " Job " + jobNode.job.getJobInfo().getJobNumber();
         } catch(IOException ioe) {
            ioe.printStackTrace();
            GenePattern.showErrorDialog("An error occurred while downloading " + JobModel.getJobResultFileName(jobNode.job, jobResult.index));
            return;
         }
      } else if(node instanceof ProjectDirModel.FileNode){
         ProjectDirModel.FileNode fileNode = (ProjectDirModel.FileNode) node;
         file = fileNode.file;
         title = file.getPath();
      }
      if(file!=null) {
         String contents = null;
         try {
            contents = fileToString(file);
         } catch(IOException ioe) {
            ioe.printStackTrace();
            GenePattern.showErrorDialog("An error occurred while viewing the file");
            return;
         }
         if(deleteFile) {
            file.delete();
         }
         JDialog dialog = new CenteredDialog(this);
         dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
         dialog.setTitle(title);
         JTextArea textArea = new JTextArea(contents);
         textArea.setEditable(false);
         JScrollPane sp = new JScrollPane(textArea);
         dialog.getContentPane().add(sp, BorderLayout.CENTER);
         Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
         dialog.setSize(screenSize.width/2, screenSize.height/2);
         dialog.setVisible(true);
      }
   }

	public void showSaveDialog(final JobModel.ServerFileNode node) {
		final File initiallySelectedFile = new File(node.toString());
		if(saveAsFileChooser == null) {
         saveAsFileChooser = new JFileChooser();
      }
      saveAsFileChooser.setSelectedFile(initiallySelectedFile);

		if (saveAsFileChooser.showSaveDialog(GenePattern.getDialogParent()) == JFileChooser.APPROVE_OPTION) {
			final File outputFile = saveAsFileChooser.getSelectedFile();
			if (!overwriteFile(outputFile)) {
				return;
			}

			new Thread() {
				public void run() {
					try {
						node.download(outputFile);
					//} catch (WebServiceException wse) {
                //  if(!disconnectedFromServer(wse)) {
                 //    GenePattern.showErrorDialog("An error occurred while saving " + outputFile.getName() + ". Please try again.");
                 // }
					} catch(IOException ioe) {
                  ioe.printStackTrace();
                  GenePattern.showErrorDialog("An error occurred while saving " + outputFile.getName() + ".");
               }
				}
			}.start();
		}
	}

	public void changeServer(final String server, final String username) {
      analysisServiceManager = AnalysisServiceManager.getInstance();
      
      analysisServiceManager.changeServer(server, username);
      
      final boolean isLocalHost = analysisServiceManager.isLocalHost();
      if(isLocalHost) {
         MessageDialog.getInstance().setText("Retrieving modules and jobs from local GenePattern server");
      } else {
         MessageDialog.getInstance().setText("Retrieving modules and jobs from " + server);
      }
    
	   historyMenu.clear();
		PropertyManager.setProperty(PreferenceKeys.SERVER, server);
		PropertyManager.setProperty(PreferenceKeys.USER_NAME, username);
		
      setChangeServerActionsEnabled(false);
		if(analysisServicePanel!= null && analysisServicePanel.isShowingAnalysisService()) {
			analysisServicePanel.showGettingStarted();
		}
      
		new Thread() {
			public void run() {

				try {
					String lsidAuthority = (String) new org.genepattern.webservice.AdminProxy(
							analysisServiceManager.getServer(),
							analysisServiceManager.getUsername(), false)
							.getServiceInfo().get("lsid.authority");
					System.setProperty("lsid.authority", lsidAuthority);
               refreshJobs(false);
				} catch (WebServiceException wse) {
               wse.printStackTrace();
               // ignore the exception here, the user will be alerted in refreshModules
				}
				refreshModules(false);
            displayServerStatus();
            MessageDialog.getInstance().setVisible(false);
			}
		}.start();


	}
   
   private String getServer() {
      final boolean isLocalHost = analysisServiceManager.isLocalHost();
      if(isLocalHost) {
         return "Local";  
      }
      return analysisServiceManager.getServer();
   }
   
   private void displayServerStatus() {
      final String server = getServer();
      final String username = analysisServiceManager.getUsername();
     
      Thread changeStatusThread = new Thread() {
         public void run() {
            setTitle("GPGE - Server: " + server + ",  Username: "
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
		String key = lsid != null ? lsid : taskName;
		//this won't reload old jobs b/c they have no lsid
		AnalysisService service = analysisServiceManager
				.getAnalysisService(key);

		if (service == null) {
			if (lsid != null) {
				service = analysisServiceManager.getAnalysisService(lsid); // see
																		   // if
																		   // old
																		   // version
																		   // of
																		   // task
																		   // exists
			}
			if (service == null) { // get task by name
				service = analysisServiceManager.getAnalysisService(taskName);
			}
			if (service == null) {
				GenePattern.showMessageDialog(
						"The task " + taskName + " does not exist.");
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
			errorMessage.append("Ignoring now unused parameters ");
		} else if (savedParamName2Param.size() == 1) {
			errorMessage.append("Ignoring now unused parameter ");
		}

		for (Iterator iUnused = savedParamName2Param.keySet().iterator(); iUnused
				.hasNext();) {
			errorMessage.append(iUnused.next() + "\n");
		}

		if (errorMessage.length() > 0) {
			GenePattern.showMessageDialog(
					errorMessage.toString());
		}
		TaskInfo taskCopy = new TaskInfo(task.getID(), task.getName(), task
				.getDescription(), task.getParameterInfo(), task.giveTaskInfoAttributes(), task
				.getUserId(), task.getAccessId());

		taskCopy.setParameterInfoArray((ParameterInfo[]) actualParams
				.toArray(new ParameterInfo[0]));
		AnalysisService serviceCopy = new AnalysisService(service.getServer(),
				taskCopy);
		analysisServicePanel.loadTask(serviceCopy);

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
               fileName = fileName.substring(index + 1, fileName
                     .length());
            }
            filenames.add(fileName);
         }
      }
      return filenames;
   }

   private Color decodeColorFromProperties(String prop) {
      if(prop==null) {
         return null;  
      }
      String[] rgbString = prop.split(",");
      int[] rgb = new int[3];
      int rgbIndex = 0;
      for(int i = 0; i < rgbString.length; i++) {
         if("".equals(rgbString[i])) {
            continue;  
         }
         try {
            rgb[rgbIndex] = Integer.parseInt(rgbString[i]);
            if(rgb[rgbIndex] < 0 || rgbIndex > 255) {
               return null;
            }
            rgbIndex++;
         } catch(Exception e) {
            return null;  
         }
      }
      return new Color(rgb[0], rgb[1], rgb[2]);
      
   }
   
   private String encodeColorToProperty(Color c) {
      if(c==null) {
         return null;  
      }
      return c.getRed() + "," + c.getGreen() + "," + c.getBlue(); 
   }
   
   
   // String kind = node.getFileInfo().getKind();
   void enableSendToMenus(MenuAction sendToMenu, String kind) {
      for(int i = 0, length = sendToMenu.getItemCount(); i < length; i++) {
         Object obj = sendToMenu.getMenuComponent(i);
         if(obj instanceof SendToMenuItemAction) {
            SendToMenuItemAction mia = (SendToMenuItemAction) obj;
            mia.setEnabled(mia.isCorrectKind(kind));
         }
      }   
   }
   
	public MainFrame() {
		
		
      JWindow splash = GenePattern.showSplashScreen();
		splash.setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit()
				.getScreenSize();
      if(RUNNING_ON_MAC) {
         setSize(0,0);
         setLocation(screenSize.width/2, screenSize.height/2);     
         show(); // on Mac OSX the dialog won't stay on top unless the parent frame is visible when the dialog is created
      }
      MessageDialog.init(this);
		setVisible(false); 
		String username = PropertyManager
				.getProperty(PreferenceKeys.USER_NAME);

		if (username == null || username.trim().equals("")) {
         username = System.getProperty("user.name");
         if(username==null || username.trim().equals("")) {
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
      } catch(MalformedURLException mfe) {
         server = "http://" + server;
      }
      authorityMineColor = decodeColorFromProperties(PropertyManager.getProperty(PreferenceKeys.AUTHORITY_MINE_COLOR));
      if(authorityMineColor==null) {
         authorityMineColor = DEFAULT_AUTHORITY_MINE_COLOR;
      }
      
      authorityForeignColor = decodeColorFromProperties(PropertyManager.getProperty(PreferenceKeys.AUTHORITY_FOREIGN_COLOR));
      if(authorityForeignColor==null) {
         authorityForeignColor = DEFAULT_AUTHORITY_FOREIGN_COLOR;
      }
      
      authorityBroadColor = decodeColorFromProperties(PropertyManager.getProperty(PreferenceKeys.AUTHORITY_BROAD_COLOR));
      if(authorityBroadColor==null) {
         authorityBroadColor = DEFAULT_AUTHORITY_BROAD_COLOR;
      }
     
      PropertyManager.setProperty(PreferenceKeys.AUTHORITY_BROAD_COLOR, encodeColorToProperty(authorityBroadColor));
      PropertyManager.setProperty(PreferenceKeys.AUTHORITY_FOREIGN_COLOR, encodeColorToProperty(authorityForeignColor));
      PropertyManager.setProperty(PreferenceKeys.AUTHORITY_MINE_COLOR, encodeColorToProperty(authorityMineColor));
      
      String showParameterDescriptions = PropertyManager.getProperty(PreferenceKeys.SHOW_PARAMETER_DESCRIPTIONS);
      if(showParameterDescriptions==null) {
         PropertyManager.setProperty(PreferenceKeys.SHOW_PARAMETER_DESCRIPTIONS, "true");
      }
      
      String alertOnJobDescription = PropertyManager.getProperty(PreferenceKeys.SHOW_JOB_COMPLETED_DIALOG);
      if(alertOnJobDescription==null) {
         PropertyManager.setProperty(PreferenceKeys.SHOW_JOB_COMPLETED_DIALOG, "true");
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
      
		jobModel.addJobListener(new JobListener() {
			public void jobStatusChanged(JobEvent e) {
            
			}

			public void jobAdded(JobEvent e) {
         
			}

			public void jobCompleted(JobEvent e) {
				AnalysisJob job = e.getJob();
				int jobNumber = job.getJobInfo().getJobNumber();
				String taskName = job.getTaskName();
				String status = job.getJobInfo().getStatus();
				fileMenu.jobCompletedDialog.add(jobNumber, taskName, status);
            ParameterInfo[] params = job.getJobInfo().getParameterInfoArray();
            int stderrIndex = -1;
            if(params!=null) {
               for(int i = 0; i <  params.length; i++) {
                  if(params[i].isOutputFile()) {
                     if(params[i].getValue().equals(jobNumber + "/stderr.txt") || params[i].getValue().equals(jobNumber + "\\stderr.txt")) {
                        stderrIndex = i;
                        break;
                     }
                  }
               }
            }
            if(stderrIndex >= 0) {
               File stderrFile = null;
               try {
                  stderrFile = File.createTempFile("stderr.txt", null);
                  JobModel.downloadJobResultFile(job, stderrIndex, stderrFile);
                  GenePattern.showModuleErrorDialog("Job " + jobNumber + " Error", fileToString(stderrFile));
               } catch(IOException ioe) {
                  ioe.printStackTrace();
               } finally {
                  if(stderrFile!=null) {
                     stderrFile.delete();
                  }
               }
            }
			}
		});

      
		changeServer(server, username);
      splash.dispose();
		analysisServicePanel = new AnalysisServiceDisplay();

      jobResultsTree.setFocusable(true);
      jobResultsTree.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyPressed(java.awt.event.KeyEvent e) {
            if(e.getKeyCode()==java.awt.event.KeyEvent.VK_BACK_SPACE) {
               if(selectedJobNode instanceof JobModel.JobNode) {
						if(deleteJobAction.isEnabled()) {
							deleteJobAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
						}
               } else if(selectedJobNode instanceof JobModel.ServerFileNode) {
                  deleteFileMenuItem.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
               }
            }
         }
      });


      jobResultsTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
         public void valueChanged(javax.swing.event.TreeSelectionEvent e) {
            DefaultMutableTreeNode newSelection = null;
            TreePath path = jobResultsTree.getSelectionPath();
            if(path==null) {
               newSelection = null;
            } else {
               newSelection = (DefaultMutableTreeNode) path
						.getLastPathComponent();
            }
            if(newSelection==null && selectedJobNode==null || ( newSelection!=null && newSelection.equals(selectedJobNode))) {
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
            if(isJobNode) {
               JobModel.JobNode node = (JobModel.JobNode) selectedJobNode;
					deleteJobAction.setEnabled(node.isComplete());
               terminateJobAction.setEnabled(!node.isComplete());
            }

            jobResultFileSendToMenu.setEnabled(isJobResultFileNode && analysisServicePanel.isShowingAnalysisService());
            saveServerFileMenu.setEnabled(isJobResultFileNode);
            saveToFileSystemMenuItem.setEnabled(isJobResultFileNode);
            deleteFileMenuItem.setEnabled(isJobResultFileNode);
            openWithMenu.setEnabled(isJobResultFileNode);
            jobResultFileViewModulesMenu.removeAll();
            if (selectedJobNode instanceof JobModel.ServerFileNode) {
					JobModel.ServerFileNode node = (JobModel.ServerFileNode) selectedJobNode;

					
                  fileSummaryComponent.setText(node.name, node.getFileInfo());
                  SemanticUtil.ModuleMenuItemAction[] mi = null;
                  if(inputTypeToMenuItemsMap!=null) {
                     mi = (SemanticUtil.ModuleMenuItemAction[]) inputTypeToMenuItemsMap.get(node.getFileInfo().getKind());
                  }
                  if(mi!=null) {
                     for(int i = 0; i < mi.length; i++) {
                        mi[i].setTreeNode(node, node.getFileInfo().getKind());
                        jobResultFileViewModulesMenu.add(mi[i]);
                        
                     }
                  }
                  jobResultFileViewModulesMenu.setEnabled(mi!=null && mi.length > 0);
						
						for(int i = 0, length = jobResultFileSendToMenu.getItemCount(); i < length; i++) {
							Object obj = jobResultFileSendToMenu.getMenuComponent(i);
							if(obj instanceof SendToMenuItemAction) {
								SendToMenuItemAction mia = (SendToMenuItemAction) obj;
								String kind = node.getFileInfo().getKind();
								mia.setEnabled(mia.isCorrectKind(kind));
							}
						}
                  
						//if (connection.getResponseCode() == HttpURLConnection.HTTP_GONE) {
                    // JobModel.JobNode jobNode = (JobModel.JobNode) node.getParent();
                     
                     //JobInfo jobFromServer = new AnalysisWebServiceProxy(server, username).checkStatus(jobNode.job.getJobInfo().getJobNumber());
                     
					//		GenePattern.showMessageDialog(node.name
						//			+ " has been deleted from the server.");
						//	jobModel.remove(node);
						//	fileSummaryComponent.select(null);
                   //  return;
						//} else {
							
						//}

					

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
            if(projectDirs[i]!=null && !projectDirs[i].trim().equals("")) {
               projectDirModel.add(new File(projectDirs[i]));
            }
			}
		}

      projectDirTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
         public void valueChanged(javax.swing.event.TreeSelectionEvent e) {
            TreePath path = projectDirTree.getSelectionPath();
            DefaultMutableTreeNode newSelection = null;
            
            if(path==null) {
               newSelection = null;
            } else {
               newSelection = (DefaultMutableTreeNode) path
                  .getLastPathComponent();
            }
          
            if(newSelection==null && selectedProjectDirNode==null || ( newSelection!=null && newSelection.equals(selectedProjectDirNode))) {
               return;  
            }
            
            selectedProjectDirNode = newSelection;
            
				
            boolean projectNodeSelected = selectedProjectDirNode instanceof ProjectDirModel.ProjectDirNode;
            boolean projectFileSelected = selectedProjectDirNode instanceof ProjectDirModel.FileNode;

            projectFileViewModulesMenu.setEnabled(false);
            
            projectFileSendToMenu.setEnabled(projectFileSelected&& analysisServicePanel.isShowingAnalysisService());

            projectFileOpenWithMenu.setEnabled(projectFileSelected);
            revealFileMenuItem.setEnabled(projectFileSelected);

            refreshProjectMenuItem.setEnabled(projectNodeSelected);
            removeProjectMenuItem.setEnabled(projectNodeSelected);


            if (projectFileSelected) {
					ProjectDirModel.FileNode node = (ProjectDirModel.FileNode) selectedProjectDirNode;
					ProjectDirModel.ProjectDirNode parent = (ProjectDirModel.ProjectDirNode) node
							.getParent();
					
               projectFileViewModulesMenu.removeAll();
					
               if(!new File(parent.directory, node.file.getName()).exists()) {
                  projectDirModel.refresh(parent);
                  fileSummaryComponent.clear();
                  return;
               }
               
               fileSummaryComponent.setText(node.file.getName(), node.getFileInfo());
               
               SemanticUtil.ModuleMenuItemAction[] mi = null;
               if(inputTypeToMenuItemsMap!=null) {
                  mi = (SemanticUtil.ModuleMenuItemAction[]) inputTypeToMenuItemsMap.get(node.getFileInfo().getKind());
               }
               if(mi!=null) {
                  for(int i = 0; i < mi.length; i++) {
                     mi[i].setTreeNode(node, node.getFileInfo().getKind());
                     projectFileViewModulesMenu.add(mi[i]);
                  }
               }
               projectFileViewModulesMenu.setEnabled(mi!=null && mi.length > 0);
					
				   String kind = node.getFileInfo().getKind();
               enableSendToMenus(projectFileSendToMenu,  kind);

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
		analysisServicePanel
				.addAnalysisServiceSelectionListener(new AnalysisServiceSelectionListener() {

					public void valueChanged(AnalysisServiceSelectionEvent e) {

						jobResultFileSendToMenu.removeAll();
						projectFileSendToMenu.removeAll();

               
                  if(!analysisServicePanel.isShowingAnalysisService()) {
                     jobResultFileSendToMenu.setEnabled(false);
                     projectFileSendToMenu.setEnabled(false);
                     return;
                  }
                  
						for (Iterator it = analysisServicePanel
								.getInputFileParameters(); it.hasNext();) {
						  final ParameterInfo pi = (ParameterInfo) it.next();
						  final String name = pi.getName();
                    final String displayName = AnalysisServiceDisplay.getDisplayString(name);
						  MenuItemAction jobResultFileSendToMenuItem = new SendToMenuItemAction(displayName, pi) {
								public void actionPerformed(ActionEvent e) {
									analysisServicePanel.setInputFile(name,
											selectedJobNode);
								}
							};
							jobResultFileSendToMenu.add(jobResultFileSendToMenuItem);


							MenuItemAction projectMenuItem = new SendToMenuItemAction(displayName, pi) {
                        public void actionPerformed(
                              ActionEvent e) {
                           analysisServicePanel.setInputFile(
                                 name,
                                 selectedProjectDirNode);
                        }
							};
							projectMenuItem.putValue(MainFrame.PARAMETER_NAME, name);
							projectFileSendToMenu.add(projectMenuItem);
						}
                  if(selectedJobNode instanceof JobModel.ServerFileNode) {
                     jobResultFileSendToMenu.setEnabled(true);  
                     String kind = ((JobModel.ServerFileNode)selectedJobNode).getFileInfo().getKind();
                     enableSendToMenus(jobResultFileSendToMenu,  kind);
                  }
                  if(selectedProjectDirNode instanceof ProjectDirModel.FileNode) {
                     projectFileSendToMenu.setEnabled(true);  
                    
                     String kind = ((ProjectDirModel.FileNode)selectedProjectDirNode).getFileInfo().getKind();
                     enableSendToMenus(projectFileSendToMenu,  kind);
                  }

					}
				});


      JScrollPane projectSP = new JScrollPane(projectDirTree);
      JScrollPane jobSP = new JScrollPane(jobResultsTree);
      int width = (int) (screenSize.width * .9);
      int height = (int) (screenSize.height * .9);

      JPanel projectPanel = new JPanel(new BorderLayout());
      projectPanel.setMinimumSize(new Dimension(200, 200));
      projectPanel.setBackground(new Color(24,48,115));
      projectPanel.add(projectSP, BorderLayout.CENTER);
      if(RUNNING_ON_WINDOWS) {
         projectSP.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
         jobSP.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
      }

      JLabel l = new JLabel("Projects", JLabel.CENTER);
      l.setForeground(Color.white);
      l.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 14));
      projectPanel.add(l, BorderLayout.NORTH);

      
      JPanel jobPanel = new JPanel(new BorderLayout());
      jobPanel.setMinimumSize(new Dimension(200, 200));
      jobPanel.setBackground(new Color(24,48,115));
      jobPanel.add(jobSP, BorderLayout.CENTER);
      JLabel l2 = new JLabel("Results", JLabel.CENTER);
      l2.setForeground(Color.white);
      l2.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 14));
      jobPanel.add(l2, BorderLayout.NORTH);

      final JSplitPane leftPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
		projectPanel, jobPanel);
      leftPane.setResizeWeight(0.5);
      if(RUNNING_ON_MAC) {
         leftPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
      }

      JPanel leftPanel = new JPanel(new BorderLayout());
      leftPanel.setMinimumSize(new Dimension(200, 200));
      if(RUNNING_ON_WINDOWS) {
       leftPanel.setBackground(Color.white);
       final Border scrollBorder = UIManager.getBorder("ScrollPane.border");
       Border b = new Border() {
         public Insets getBorderInsets(Component c) {
            return new Insets(10,10,10,10);
         }

         public boolean isBorderOpaque() {
            return scrollBorder.isBorderOpaque();
         }

         public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            scrollBorder.paintBorder(c,g,x,y,w,h);
         }
       };
       fileSummaryComponent.setBorder(b);
    }

      leftPanel.add(leftPane, BorderLayout.CENTER);
      leftPanel.add(fileSummaryComponent, BorderLayout.SOUTH);
     
      analysisServicePanel.setMinimumSize(new Dimension(200, 200));
      final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
         leftPanel, analysisServicePanel);
      splitPane.setResizeWeight(0.5);
      splitPane.setMinimumSize(new Dimension(400,400));
      getContentPane().add(splitPane, BorderLayout.CENTER);
      if(!RUNNING_ON_MAC) {
         getContentPane().setBackground(Color.white);
      }
		int x = 0;
		int y = 0;
		int leftPaneDividerLocation = 0;
		int splitPaneDividerLocation = 0;
		boolean savedLayout = true;
		try {
			String[] tokens = PropertyManager.getProperty(PreferenceKeys.WINDOW_LAYOUT).split(",");
			width = Integer.parseInt(tokens[0]);
			height = Integer.parseInt(tokens[1]);
			x = Integer.parseInt(tokens[2]);
			y = Integer.parseInt(tokens[3]);
			leftPaneDividerLocation = Integer.parseInt(tokens[4]);
			splitPaneDividerLocation = Integer.parseInt(tokens[5]);
		} catch(Exception e){
			e.printStackTrace();
			savedLayout = false;
		}
		if(!savedLayout) {
			x = (screenSize.width - width) / 2;
			y = 20;	
			leftPaneDividerLocation = (int) (height * 0.4);
			splitPaneDividerLocation = (int) (width * 0.4);
		}
		
      setSize(width, height);
      setLocation(x, y);
      displayServerStatus();
      leftPane.setDividerLocation(leftPaneDividerLocation);
      splitPane.setDividerLocation(splitPaneDividerLocation);
      setJMenuBar(menuBar);
      show();
      addComponentListener(new ComponentAdapter() {
         public void componentResized(ComponentEvent event) {
            setSize(
            Math.max(100, getWidth()),
            Math.max(100, getHeight()));
         }
      });
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public final void run() {
				try {
					PropertyManager.setProperty(PreferenceKeys.WINDOW_LAYOUT, 
						getWidth() + "," + getHeight() + "," + getLocation().x + "," 
						+ getLocation().y + "," + 
						leftPane.getDividerLocation() + "," + 
						splitPane.getDividerLocation());
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
            if(c instanceof JInternalFrame) {
               ((JInternalFrame)c).toFront();
               try {
                  ((JInternalFrame)c).setSelected(true);
               } catch(java.beans.PropertyVetoException pe){}

            }
         }
      });
      windowMenu.add(mi);
   }

	static class SendToMenuItemAction extends MenuItemAction {
		List fileFormats;
		
		public SendToMenuItemAction(String text, ParameterInfo info) {
			super(text);
			String fileFormatsString = (String) info.getAttributes().get(GPConstants.FILE_FORMAT);
			fileFormats = new ArrayList();
			if(fileFormatsString==null || fileFormatsString.equals("")) {
				return;
			}
			java.util.StringTokenizer st = new java.util.StringTokenizer(fileFormatsString, GPConstants.PARAM_INFO_CHOICE_DELIMITER);
			while(st.hasMoreTokens()) {
				fileFormats.add(st.nextToken().toLowerCase());
			}
		}
		
		public boolean isCorrectKind(String kind) {
			if(fileFormats.size()==0 || kind==null || kind.equals("")) {
				return true;	
			}
			return fileFormats.contains(kind.toLowerCase());
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
         label.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
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
         if(SwingUtilities.isEventDispatchThread()) {
            thread.run();
         } else {
            SwingUtilities.invokeLater(thread);   
         } 
      }
   }
	
	public void refreshJobs(boolean displayMessage) {
      if(displayMessage) {
         MessageDialog.getInstance().setText("Retrieving your jobs.");
      }
      final List errors = new ArrayList();
      
		Thread updateJobs = new Thread() {
			public void run() {
            try {
               jobModel.getJobsFromServer();
            } catch(WebServiceException wse) {
               wse.printStackTrace();
               synchronized(errors) {
                  if(errors.size()==0) {
                     if(!disconnectedFromServer(wse)) {
                        GenePattern.showErrorDialog("An error occurred while retrieving your jobs.");
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
            historyMenu.clear();
            String server = AnalysisServiceManager.getInstance().getServer();
            String username = AnalysisServiceManager.getInstance().getUsername();
            try {
               AnalysisWebServiceProxy proxy = new AnalysisWebServiceProxy(server, username);
               JobInfo[] jobs = proxy.getJobs(username, true);
               for(int i = 0; i < jobs.length; i++) {
                  historyMenu.add(new AnalysisJob(server, jobs[i]));
               }
            } catch(WebServiceException wse) {
               wse.printStackTrace();
               synchronized(errors) {
                  if(errors.size()==0) {
                     if(!disconnectedFromServer(wse)) {
                        GenePattern.showErrorDialog("An error occurred while retrieving your history.");
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
         
      } catch(InterruptedException x){}
      if(displayMessage) {
         MessageDialog.getInstance().setVisible(false);
      }
	}

   private void createProjectFileActions() {
      projectFileSendToMenu = new MenuAction("Send To", IconManager.loadIcon(IconManager.SEND_TO_ICON));
      projectFileSendToMenu.setEnabled(false);
      
      projectFileViewModulesMenu = new MenuAction("Modules");
      projectFileViewModulesMenu.setEnabled(false);

      projectFileOpenWithMenu = new MenuAction("Open With");
      MenuItemAction projectFileTextViewerMenuItem = new MenuItemAction("Text Viewer", IconManager.loadIcon(IconManager.TEXT_ICON)) {
			public void actionPerformed(ActionEvent e) {
            new Thread() {
               public void run() {
                  textViewer(selectedProjectDirNode);
               }
            }.start();
			}
		};
      projectFileOpenWithMenu.add(projectFileTextViewerMenuItem);

      projectFileDefaultAppMenuItem = new MenuItemAction("Default Application") {
         public void actionPerformed(ActionEvent e) {
            new Thread() {
               public void run() {
                  defaultApplication(selectedProjectDirNode);
               }
            }.start();
			}
		};
      projectFileOpenWithMenu.add(projectFileDefaultAppMenuItem);


      if(RUNNING_ON_MAC) {
          revealFileMenuItem = new MenuItemAction("Show In Finder") {
             public void actionPerformed(ActionEvent e) {
                ProjectDirModel.FileNode fn = (ProjectDirModel.FileNode) selectedProjectDirNode;
                org.genepattern.gpge.util.MacOS.showFileInFinder(fn.file);
             }
          };


      } else if(System.getProperty("os.name").startsWith("Windows")) {
         revealFileMenuItem = new MenuItemAction("Show File Location") {
             public void actionPerformed(ActionEvent e) {
                try {
                   ProjectDirModel.FileNode fn = (ProjectDirModel.FileNode) selectedProjectDirNode;
                   BrowserLauncher.openURL(fn.file.getParentFile().getCanonicalPath());
                } catch(IOException x){
                   x.printStackTrace();
                }
             }
          };

      }
   }

   private void createProjectDirActions() {
      refreshProjectMenuItem = new MenuItemAction("Refresh", IconManager.loadIcon(IconManager.REFRESH_ICON)) {
         public void actionPerformed(ActionEvent e) {
				projectDirModel
						.refresh((ProjectDirModel.ProjectDirNode) selectedProjectDirNode);
			}
		};


      removeProjectMenuItem = new MenuItemAction("Close Project", IconManager.loadIcon(IconManager.REMOVE_ICON)) {
         public void actionPerformed(ActionEvent e) {
				projectDirModel
						.remove((ProjectDirModel.ProjectDirNode) selectedProjectDirNode);
				PropertyManager.setProperty(PreferenceKeys.PROJECT_DIRS,
						projectDirModel.getPreferencesString());
			}
		};


   }

   private boolean showConfirmDialog(String message) {
      return showConfirmDialog(this, message);     
   }
   
   private boolean showConfirmDialog(java.awt.Component parent, String message) {
        if (JOptionPane.showOptionDialog(parent,
            message, null, JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE, GenePattern.getIcon(), new Object[] {
            "Yes", "No" }, "Yes")==JOptionPane.YES_OPTION) {
               return true;
        }   
        return false;
   }
   
   private void createJobActions() {
      reloadMenuItem = new MenuItemAction("Reload") {
			public void actionPerformed(ActionEvent e) {
				reload(((JobModel.JobNode) selectedJobNode).job);
			}
		};

      deleteJobAction = new MenuItemAction(
      "Delete Job", IconManager.loadIcon(IconManager.DELETE_ICON)) {
         public void actionPerformed(ActionEvent e) {
            final JobModel.JobNode jobNode = (JobModel.JobNode) selectedJobNode;
            
				String message = "Are you sure you want to delete job number " + jobNode.job.getJobInfo().getJobNumber() + "?";
				if(showConfirmDialog(message)) {
					new Thread() {
						public void run() {
							try {
								jobModel.delete(jobNode);
							} catch(WebServiceException wse) {
								wse.printStackTrace();
								if(!disconnectedFromServer(wse)) {
									GenePattern.showErrorDialog("An error occurred deleting job number " + jobNode.job.getJobInfo().getJobNumber() + ".");
								}
							}
						}
					}.start();
				}
            
         }
      };

      deleteAllJobsAction = new MenuItemAction(
      "Delete All Jobs") {
         public void actionPerformed(ActionEvent e) {
           
              String message = "Are you sure you want to delete all jobs?";
              if(showConfirmDialog(message)) {
                 new Thread() {
                    public void run() {
                       try {
                          jobModel.deleteAll();
                       } catch(WebServiceException wse) {
                          wse.printStackTrace();
                          if(!disconnectedFromServer(wse)) {
                             GenePattern.showErrorDialog("An error occurred while deleting all jobs.");
                          }
                       }
                    }
                 }.start();   
              }
         }
      };

      terminateJobAction = new MenuItemAction("Terminate Job", IconManager.loadIcon(IconManager.STOP_ICON)) {
			public void actionPerformed(ActionEvent e) {
            JobModel.JobNode jobNode = (JobModel.JobNode) selectedJobNode;
            try {
               AnalysisWebServiceProxy p = new AnalysisWebServiceProxy(analysisServiceManager.getServer(), analysisServiceManager.getUsername(), false);
               p.terminateJob(jobNode.job.getJobInfo().getJobNumber());
            } catch(WebServiceException wse) {
                wse.printStackTrace();
                if(!disconnectedFromServer(wse)) {
                  GenePattern.showErrorDialog("An error occurred terminating job number " + jobNode.job.getJobInfo().getJobNumber() + ".");
                }
            }
			}
		};
      
     
      viewCodeAction = new MenuAction("View Code");
      
      MenuItemAction viewJavaCodeAction = new MenuItemAction("Java") {
         public void actionPerformed(ActionEvent e) {
            viewCode(new org.genepattern.codegenerator.JavaPipelineCodeGenerator(), "Java");
         }
      };

      viewCodeAction.add(viewJavaCodeAction);
      
       
      MenuItemAction viewMATLABCodeAction = new MenuItemAction("MATLAB") {
         public void actionPerformed(ActionEvent e) {
            viewCode(new org.genepattern.codegenerator.MATLABPipelineCodeGenerator(), "MATLAB");
         }
      };
      viewCodeAction.add(viewMATLABCodeAction);
      
       MenuItemAction viewRCodeAction = new MenuItemAction("R") {
         public void actionPerformed(ActionEvent e) {
            viewCode(new org.genepattern.codegenerator.RPipelineCodeGenerator(), "R");
         }
      };
      viewCodeAction.add(viewRCodeAction);
      
   }

   /**
   * Generates code for the selected job node
   */
   private void viewCode(org.genepattern.codegenerator.TaskCodeGenerator codeGenerator, final String language) {
      JobModel.JobNode jobNode = (JobModel.JobNode) selectedJobNode; 
      org.genepattern.gpge.ui.code.Util.viewCode(codeGenerator, jobNode.job, language);
   }
   
   
         
   private boolean overwriteFile(File f) {
      if(!f.exists()) {
         return true;
      }
      String message = "An item named " + f.getName() + " already exists in this location.\nDo you want to replace it with the one that you are saving?";
      if(JOptionPane.showOptionDialog(this, message, null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, GenePattern.getIcon(), new Object[]{"Replace", "Cancel"}, "Cancel") != JOptionPane.YES_OPTION) {
         return false;
      }
      return true;
   }
   
   private void createJobResultFileActions() {
      jobResultFileViewModulesMenu = new MenuAction("Modules");
      jobResultFileViewModulesMenu.setEnabled(false);
      
      jobResultFileSendToMenu = new MenuAction("Send To", IconManager.loadIcon(IconManager.SEND_TO_ICON));
      jobResultFileSendToMenu.setEnabled(false);

		saveServerFileMenu = new MenuAction("Save To");

      saveToFileSystemMenuItem = new MenuItemAction("Other...", IconManager.loadIcon(IconManager.SAVE_AS_ICON));
		saveToFileSystemMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showSaveDialog((JobModel.ServerFileNode) selectedJobNode);
			}
		});
		projectDirModel
				.addProjectDirectoryListener(new ProjectDirectoryListener() {
					public void projectAdded(ProjectEvent e) {
						final File dir = e.getDirectory();
						MenuItemAction menuItem = new MenuItemAction(dir.getPath(), IconManager.loadIcon(IconManager.SAVE_ICON)) {
							public void actionPerformed(ActionEvent e) {
								new Thread() {
									public void run() {
                             JobModel.ServerFileNode node = (JobModel.ServerFileNode) selectedJobNode;
                             File outputFile = new File(dir,
													node.name);
										try {
                                 if(overwriteFile(outputFile)) {
                                    node.download(outputFile);
                                    projectDirModel.refresh(dir);
                                 }
                              } catch(IOException ioe) {
                                 ioe.printStackTrace();
                                 GenePattern.showErrorDialog("An error occurred while saving the file " + node.name  + ".");
                              }
                             //	} catch (WebServiceException wse) {
                            //      if(!disconnectedFromServer(wse)) {
                            //         GenePattern.showErrorDialog("An error occurred while saving the file " + node.name  + ". Please try again.");
                           //       }
										//}
									}
								}.start();
							}
						};
                 saveServerFileMenu.insert(menuItem, projectDirModel.indexOf(dir)+1);
					}

					public void projectRemoved(ProjectEvent e) {
						File dir = e.getDirectory();
						for (int i = 0; i < saveServerFileMenu.getItemCount(); i++) {
							MenuItemAction m = (MenuItemAction) saveServerFileMenu
									.getMenuComponent(i);
                     String text = (String) m.getValue(MenuItemAction.NAME);
                     if (text.equals(dir.getPath())) {
								saveServerFileMenu.remove(i);
								break;
							}
						}
					}
				});
		saveServerFileMenu.add(saveToFileSystemMenuItem);

      deleteFileMenuItem = new MenuItemAction("Delete File", IconManager.loadIcon(IconManager.DELETE_ICON)) {
         public void actionPerformed(ActionEvent e) {
            JobModel.ServerFileNode serverFileNode = (JobModel.ServerFileNode) selectedJobNode;
            String message = "Are you sure you want to delete " + serverFileNode.toString() + "?";
            try {
               if(showConfirmDialog(message)) {
                  jobModel.delete(serverFileNode);
               }
            } catch(WebServiceException wse) {
               wse.printStackTrace();
               if(!disconnectedFromServer(wse)) {
                  GenePattern.showErrorDialog("An error occurred while deleting the file " + JobModel.getJobResultFileName(serverFileNode) + ".");
               }
            }
			}
		};

      openWithMenu = new MenuAction("Open With");

      jobResultFileTextViewerMenuItem = new MenuItemAction("Text Viewer", IconManager.loadIcon(IconManager.TEXT_ICON)){
         public void actionPerformed(ActionEvent e) {
            new Thread() {
               public void run() {
                  textViewer(selectedJobNode);
               }
            }.start();
			}
		};
      openWithMenu.add(jobResultFileTextViewerMenuItem);

      jobResultFileDefaultAppMenuItem = new MenuItemAction("Default Application") {
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
			}
		};
		if (SwingUtilities.isEventDispatchThread()) {
			disableActions.run();
		} else {
			SwingUtilities.invokeLater(disableActions);
		}
   }

   
	public void refreshModules(boolean displayMessage) {
      if(displayMessage) {
         MessageDialog.getInstance().setText("Retrieving modules");
      }
     
		setChangeServerActionsEnabled(false);
      Thread thread = new Thread() {
         public void run() {
            try {
               analysisServiceManager.refresh();
            } catch (WebServiceException wse) {
               wse.printStackTrace();
               if(!disconnectedFromServer(wse)) {
                  GenePattern.showErrorDialog("An error occurred while retrieving the modules from the server.");
               }
            }
      
            final Collection latestTasks = analysisServiceManager
                  .getLatestAnalysisServices();
            
            inputTypeToMenuItemsMap = SemanticUtil.getInputTypeToMenuItemsMap(latestTasks, analysisServicePanel);
            SwingUtilities.invokeLater(new Thread() {
               public void run() {
                  analysisMenu.removeAll();
                  visualizerMenu.removeAll();
                  pipelineMenu.removeAll();
                  Map categoryToAnalysisServices = AnalysisServiceUtil
                  .getCategoryToAnalysisServicesMap(latestTasks);
                  analysisMenu.init(categoryToAnalysisServices);
                  visualizerMenu.init(categoryToAnalysisServices);
                  pipelineMenu.init(categoryToAnalysisServices);
                  setChangeServerActionsEnabled(true);
               }
            });
         }
      };
      thread.start();
      try {
         thread.join();  
      } catch(InterruptedException x){}
      if(displayMessage) {
         MessageDialog.getInstance().setVisible(false);
      }
	}


	void createMenus() {
	   menuBar = new JMenuBar();
		fileMenu = new FileMenu();
		menuBar.add(fileMenu);
      refreshProjectMenuItem.setEnabled(false);
      removeProjectMenuItem.setEnabled(false);
      projectFileSendToMenu.setEnabled(false);
      projectFileOpenWithMenu.setEnabled(false);
      if(revealFileMenuItem!=null) {
         revealFileMenuItem.setEnabled(false);
      }
      reloadMenuItem.setEnabled(false);
      deleteJobAction.setEnabled(false);
      terminateJobAction.setEnabled(false);
      jobResultFileSendToMenu.setEnabled(false);
      saveServerFileMenu.setEnabled(false);
      deleteFileMenuItem.setEnabled(false);
      openWithMenu.setEnabled(false);
      viewCodeAction.setEnabled(false);
      
      MenuAction projectsMenuAction = null;
      if(revealFileMenuItem!=null) {
         projectsMenuAction = new MenuAction("Projects", new Object[]{refreshProjectMenuItem,  removeProjectMenuItem, new JSeparator(), projectFileSendToMenu, projectFileOpenWithMenu, revealFileMenuItem, projectFileViewModulesMenu});
      } else {
          projectsMenuAction = new MenuAction("Projects", new Object[]{refreshProjectMenuItem,  removeProjectMenuItem, new JSeparator(), projectFileSendToMenu, projectFileOpenWithMenu, projectFileViewModulesMenu});
      }

      menuBar.add(projectsMenuAction.createMenu());

      MenuAction jobResultsMenuAction = new MenuAction("Results", new Object[]{reloadMenuItem, deleteJobAction, deleteAllJobsAction, terminateJobAction, viewCodeAction, new JSeparator(), jobResultFileSendToMenu, saveServerFileMenu, deleteFileMenuItem, openWithMenu, jobResultFileViewModulesMenu});

      menuBar.add(jobResultsMenuAction.createMenu());

		analysisMenu = new AnalysisMenu(AnalysisMenu.DATA_ANALYZERS);
		analysisMenu.setEnabled(false);
		menuBar.add(analysisMenu);
		visualizerMenu = new AnalysisMenu(AnalysisMenu.VISUALIZERS);
		visualizerMenu.setEnabled(false);
		menuBar.add(visualizerMenu);

      pipelineMenu = new AnalysisMenu(AnalysisMenu.PIPELINES);
		pipelineMenu.setEnabled(false);
		menuBar.add(pipelineMenu);

      historyMenu = new HistoryMenu();
      historyMenu.setEnabled(false);
      menuBar.add(historyMenu);


		JMenu helpMenu = new HelpMenu();
      menuBar.add(helpMenu);


      if(RUNNING_ON_MAC) {
         macos.MacOSMenuHelper.registerHandlers();
      }


      MenuAction jobPopupAction = new MenuAction("", new Object[]{reloadMenuItem, deleteJobAction, terminateJobAction, viewCodeAction});
      jobPopupMenu = jobPopupAction.createPopupMenu();

      MenuAction jobResultFilePopupAction = new MenuAction("", new Object[]{jobResultFileSendToMenu, saveServerFileMenu, deleteFileMenuItem, openWithMenu, jobResultFileViewModulesMenu});
      jobResultFilePopupMenu = jobResultFilePopupAction.createPopupMenu();

      MenuAction projectDirPopupMenuAction = new MenuAction("", new Object[]{refreshProjectMenuItem,  removeProjectMenuItem});
      projectDirPopupMenu = projectDirPopupMenuAction.createPopupMenu();

      MenuAction projectFilePopupMenuAction = new MenuAction("", new Object[]{projectFileSendToMenu, projectFileOpenWithMenu, revealFileMenuItem, projectFileViewModulesMenu});
      projectFilePopupMenu = projectFilePopupMenuAction.createPopupMenu();
	}

   
   public static class JobNumberComparator implements java.util.Comparator {
      public int compare(Object obj1, Object obj2) {
         Integer job1Number = new Integer(((AnalysisJob)obj1).getJobInfo().getJobNumber());
         Integer job2Number = new Integer(((AnalysisJob)obj2).getJobInfo().getJobNumber());
         return job2Number.compareTo(job1Number);

      }
   }

   public static class AnalysisJobMenuItem extends JMenuItem {
         AnalysisJob job;

         public AnalysisJobMenuItem(AnalysisJob job) {
            super(job.getJobInfo().getTaskName() + " (" + job.getJobInfo().getJobNumber() + ")");
            this.job = job;
         }

   }
	
	

	class HistoryMenu extends JMenu {
		ActionListener reloadJobActionListener;
		/**
		 *  list of AnalyisJobs, sorted by JobNumberComparator, the menu displays the
		 *  first JOBS_IN_MENU jobs in the list
		 */
		List jobs = new ArrayList();
		
		/** list of AnalysisJobs, sorted by one of several options */
		List sortedJobs = new ArrayList();
		
		
		JobNumberComparator jobNumberComparator = new JobNumberComparator();
      /** current comparator */
		java.util.Comparator comparator = jobNumberComparator;
      
		JMenuItem historyMenuItem = new JMenuItem("View All");
		static final int JOBS_IN_MENU = 10;
		HistoryTableModel historyTableModel = new HistoryTableModel();
		JDialog historyDialog;
		
		private void addToMenu(int insertionIndex) {
			
			if (insertionIndex < JOBS_IN_MENU) {
				AnalysisJob job = (AnalysisJob) jobs.get(insertionIndex);
				AnalysisJobMenuItem menuItem = new AnalysisJobMenuItem(job);
				menuItem.addActionListener(reloadJobActionListener);
				insert(menuItem, insertionIndex);
				if (getItemCount() == (JOBS_IN_MENU + 3)) {
					remove(JOBS_IN_MENU - 1);
					// separator is at JOBS_IN_MENU index
				}
			}
		}
		
		private void removeFromMenu(int deletionIndex) {
			
			if (deletionIndex < JOBS_IN_MENU) {
				remove(deletionIndex);
				AnalysisJob job = (AnalysisJob) jobs.get(JOBS_IN_MENU-1);
				AnalysisJobMenuItem menuItem = new AnalysisJobMenuItem(job);
				menuItem.addActionListener(reloadJobActionListener);
				add(menuItem, JOBS_IN_MENU-1);
			}
		}
	
		public void clear() {
			super.removeAll();
			add(new JSeparator());
			add(historyMenuItem);
			jobs.clear();
			sortedJobs.clear();
		}
	
		public void add(AnalysisJob job) {
			int insertionIndex = Collections.binarySearch(jobs, job, jobNumberComparator);
	
			if (insertionIndex < 0) {
				insertionIndex = -insertionIndex - 1;
			}
	
			jobs.add(insertionIndex, job);
			addToMenu(insertionIndex);
			
			
			insertionIndex = Collections.binarySearch(sortedJobs, job, comparator);
	
			if (insertionIndex < 0) {
				insertionIndex = -insertionIndex - 1;
			}
	
			sortedJobs.add(insertionIndex, job);
	
			historyTableModel.fireTableRowsInserted(insertionIndex, insertionIndex);
		}
	
	
		private void purge(int row) {
			AnalysisJob job = (AnalysisJob) sortedJobs.get(row);
			String message = "Are you sure you want to purge job number " + job.getJobInfo().getJobNumber() + "?";
			if (!showConfirmDialog(historyDialog, message)) {
				return;
			}
			try {
				AnalysisWebServiceProxy proxy = new AnalysisWebServiceProxy(AnalysisServiceManager.getInstance().getServer(), AnalysisServiceManager.getInstance().getUsername());
				proxy.purgeJob(job.getJobInfo().getJobNumber());
				sortedJobs.remove(row);
				historyTableModel.fireTableRowsDeleted(row, row);
				
				int index = Collections.binarySearch(jobs, job, jobNumberComparator);
				jobs.remove(index);
				removeFromMenu(index);
            JobModel.getInstance().remove(job.getJobInfo().getJobNumber());
			} catch (WebServiceException wse) {
				wse.printStackTrace();
				if (!disconnectedFromServer(wse)) {
					GenePattern.showErrorDialog("An error occurred while removing job number " + job.getJobInfo().getJobNumber());
				}
			}
		}
	
	
		public HistoryMenu() {
			super("History");
			clear();
	
			reloadJobActionListener =
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						AnalysisJobMenuItem menuItem = (AnalysisJobMenuItem) e.getSource();
						AnalysisJob job = menuItem.job;
						reload(job);
					}
				};
	
			historyMenuItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						historyDialog.setVisible(true);
					}
				});
	
			historyDialog = new CenteredDialog((java.awt.Frame) GenePattern.getDialogParent());
			historyDialog.setTitle("History");
			final AlternatingColorTable table = new AlternatingColorTable(historyTableModel);
         table.setShowCellFocus(false);
          
			JPanel toolBar = new JPanel();
			JButton reloadButton = new JButton("Reload");
			reloadButton.setToolTipText("Reload the job");
			reloadButton.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						int row = table.getSelectedRow();
						if (row == -1) {
							return;
						}
						AnalysisJob job = (AnalysisJob) sortedJobs.get(row);
						reload(job);
					}
				});
	
			JButton purgeButton = new JButton("Purge");
			purgeButton.setToolTipText("Purge the job from your history");
			purgeButton.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						int row = table.getSelectedRow();
						if (row == -1) {
							return;
						}
						purge(row);
					}
				});
	
			toolBar.add(reloadButton);
			toolBar.add(purgeButton);
			table.setShowGrid(true);
			table.setShowVerticalLines(true);
			table.setShowHorizontalLines(false);
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			table.addMouseListener(
				new MouseAdapter() {
					public void mouseClicked(MouseEvent e) {
						if (e.getClickCount() == 2 && !e.isPopupTrigger()) {
							int row = table.getSelectedRow();
							if (row == -1) {
								return;
							}
							AnalysisJob job = (AnalysisJob) jobs.get(row);
							reload(job);
						}
					}
				});
			SortableHeaderRenderer r = new SortableHeaderRenderer(table, historyTableModel);
			historyDialog.getContentPane().add(toolBar, BorderLayout.NORTH);
			historyDialog.getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
			historyDialog.pack();
			historyDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	
		}
	
	
		class HistoryTableModel extends javax.swing.table.AbstractTableModel implements SortTableModel, JobListener {
	
			public HistoryTableModel() {
				JobModel.getInstance().addJobListener(this);
			}
	
			public void jobStatusChanged(JobEvent e) {
				fireTableStructureChanged();
			}
	
			public void jobAdded(JobEvent e) {
				add(e.getJob());
			}
	
	
			public void jobRetrievedFromServer(AnalysisJob job) {
				add(job);
			}
	
			public void jobCompleted(JobEvent e) {
				fireTableStructureChanged();
			}
	
			public void sortOrderChanged(SortEvent e) {
				int column = e.getColumn();
				boolean ascending = e.isAscending();
			
				if (column == 0) {
					JobModel.TaskNameComparator c = new JobModel.TaskNameComparator();
					c.setAscending(ascending);
					comparator = c;
				} else if (column == 1) {
					JobModel.TaskCompletedDateComparator c = new JobModel.TaskCompletedDateComparator();
					c.setAscending(ascending);
					comparator = c;
				} else {
					JobModel.TaskSubmittedDateComparator c = new JobModel.TaskSubmittedDateComparator();
					c.setAscending(ascending);
					comparator = c;
				}
	
				Collections.sort(sortedJobs, comparator);
				fireTableStructureChanged();
			}
	
	
			public Object getValueAt(int r, int c) {
				AnalysisJob job = (AnalysisJob) sortedJobs.get(r);
				JobInfo jobInfo = job.getJobInfo();
				boolean complete = JobModel.isComplete(job);
				switch (c) {
					case 0:
						return JobModel.jobToString(job);
               
					case 1:
						if (!complete) {
							return jobInfo.getStatus();
						}
						return java.text.DateFormat.getDateTimeInstance(
								java.text.DateFormat.SHORT, java.text.DateFormat.SHORT)
								.format(jobInfo.getDateCompleted());
               case 2:
                  return java.text.DateFormat.getDateTimeInstance(
								java.text.DateFormat.SHORT, java.text.DateFormat.SHORT)
								.format(jobInfo.getDateSubmitted());
					default:
						return null;
				}
			}
	
	
			public Class getColumnClass(int j) {
				return String.class;
			}
	
	
			public int getRowCount() {
				return sortedJobs.size();
			}
	
	
			public int getColumnCount() {
				return 3;
			}
	
	
			public String getColumnName(int c) {
				switch (c) {
								case 0:
									return "Name";
								case 1:
									return "Completed";
								case 2:
									return "Submitted";
								default:
									return null;
				}
			}
		}
	
	
	}




	class AnalysisMenu extends JMenu {
		int type;
      static final int VISUALIZERS = 1;
      static final int DATA_ANALYZERS = 2;
      static final int PIPELINES = 3;



		ActionListener serviceSelectedListener;

		public AnalysisMenu(int type) {
			if (type==VISUALIZERS) {
				setText("Visualization");
			} else if(type==DATA_ANALYZERS) {
				setText("Analysis");
			} else if(type==PIPELINES) {
            setText("Pipelines");
         } else {
            throw new IllegalArgumentException("Unknown type");
         }
			this.type = type;
			serviceSelectedListener = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					AnalysisMenuItem mi = (AnalysisMenuItem) e.getSource();
					analysisServicePanel.loadTask(mi.svc);
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

		public void init(Map categoryToAnalysisServices) {
			if(type==DATA_ANALYZERS) {
				for (Iterator keys = categoryToAnalysisServices.keySet()
						.iterator(); keys.hasNext();) {
					String category = (String) keys.next();
               if(category.equalsIgnoreCase(GPConstants.TASK_TYPE_VISUALIZER) || category.equalsIgnoreCase("Image Creators") || category.equalsIgnoreCase("Pipeline")) {
                  continue;
               }
					List services = (List) categoryToAnalysisServices
							.get(category);
					JMenu menu = new JMenu(category);
					add(menu, services);
					this.add(menu);
				}
			} else if(type==VISUALIZERS){
				List visualizers = (List) categoryToAnalysisServices
						.get(GPConstants.TASK_TYPE_VISUALIZER);

				List imageCreators = (List) categoryToAnalysisServices
						.get("Image Creators");
            List all = new ArrayList();
            if(visualizers!=null) {
               all.addAll(visualizers);
            }
            if(imageCreators!=null) {
               all.addAll(imageCreators);
            }
            Collections.sort(all, AnalysisServiceUtil.CASE_INSENSITIVE_TASK_NAME_COMPARATOR);
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
			super(svc.getTaskInfo().getName());
			this.svc = svc;
         String lsid = (String) svc.getTaskInfo().getTaskInfoAttributes().get(
					GPConstants.LSID);
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
         } catch(MalformedURLException mfe) {
            mfe.printStackTrace();
         }
		}
	}

	class HelpMenu extends JMenu {
		public HelpMenu() {
			super("Help");

         
         if(!RUNNING_ON_MAC) {
            JMenuItem aboutMenuItem = new JMenuItem("About");
            add(aboutMenuItem);
            aboutMenuItem.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                  GenePattern.showAbout();
               }
            });
         }
         
         JMenuItem genePatternWebSiteMenuItem = new JMenuItem("GenePattern Web Site");
			add(genePatternWebSiteMenuItem);
			genePatternWebSiteMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
               try {
                  BrowserLauncher.openURL("http://www.genepattern.org");
               } catch(IOException ioe){}
            }
         });
         
         
         JMenuItem genePatternTutorialMenuItem = new JMenuItem("GenePattern Tutorial");
			add(genePatternTutorialMenuItem);
			genePatternTutorialMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
               try {
                  BrowserLauncher.openURL("http://www.genepattern.org/tutorial");
               } catch(IOException ioe){}
            }
         });
         

         JMenuItem gettingStartedMenuItem = new JMenuItem("Getting Started");
			add(gettingStartedMenuItem);
			gettingStartedMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
               analysisServicePanel.showGettingStarted();
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
							JOptionPane.INFORMATION_MESSAGE, GenePattern.getIcon());
				}
			});

         JMenuItem errorsMenuItem = new JMenuItem("Errors", IconManager.loadIcon(IconManager.ERROR_ICON));

			add(errorsMenuItem);
         errorsMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					GenePattern.showErrors();
				}
			});
		}
	}

	class FileMenu extends JMenu {
		JobCompletedDialog jobCompletedDialog;

		JMenuItem changeServerMenuItem;

		JMenu refreshMenu;

		JMenuItem refreshJobsMenuItem;

		JMenuItem refreshModulesMenuItem;

		JFileChooser projectDirFileChooser;

		public void changeServerActionsEnabled(boolean b) {
			changeServerMenuItem.setEnabled(b);
			refreshMenu.setEnabled(b);
			refreshJobsMenuItem.setEnabled(b);
			refreshModulesMenuItem.setEnabled(b);
		}

		public FileMenu() {
			super("File");
			JMenuItem openProjectDirItem = new JMenuItem(
					"Open Project Directory...", IconManager.loadIcon(IconManager.NEW_PROJECT_ICON));

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
									.showMessageDialog(
											"No directory selected");
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
			
			final javax.swing.JCheckBoxMenuItem showJobCompletedDialogMenuItem = new javax.swing.JCheckBoxMenuItem(
					"Alert On Job Completion");
         jobCompletedDialog = new JobCompletedDialog(MainFrame.this, showJobCompletedDialogMenuItem);
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
							MainFrame.this);
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
												.showMessageDialog(
														"Invalid port. Please try again.");
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

         if(!RUNNING_ON_MAC) {
            add(quitMenuItem);
         }

		}
	}
}