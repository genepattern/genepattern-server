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
import org.genepattern.gpge.*;
import org.genepattern.gpge.io.*;
import org.genepattern.gpge.ui.browser.*;
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

/**
 * Description of the Class
 * 
 * @author Joshua Gould
 */
public class MainFrame extends JFrame {
       
	AnalysisServiceDisplay analysisServicePanel;

	JLabel messageLabel = new JLabel("", JLabel.CENTER);

	AnalysisServiceManager analysisServiceManager;

	public final static Color AUTHORITY_MINE_COLOR = java.awt.Color.decode("0xFF00FF");

	public final static Color AUTHORITY_FOREIGN_COLOR = java.awt.Color
			.decode("0x0000FF");

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
   
	final static int MENU_SHORTCUT_KEY_MASK = Toolkit.getDefaultToolkit()
			.getMenuShortcutKeyMask();

	FileInfoComponent fileSummaryComponent = new FileInfoComponent();

    public static boolean RUNNING_ON_MAC = System.getProperty("mrj.version") != null
			&& javax.swing.UIManager.getSystemLookAndFeelClassName()
					.equals(
							javax.swing.UIManager.getLookAndFeel().getClass()
									.getName());
   public static boolean RUNNING_ON_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");
   
   private static short WINDOW_STYLE_ONE_FRAME = 0;
   public static short WINDOW_STYLE_FRAMES = 1;
   public static short WINDOW_STYLE_MDI = 2;
   public static short windowStyle = System.getProperty("mdi")!=null?WINDOW_STYLE_MDI:WINDOW_STYLE_ONE_FRAME;
   private JMenuBar menuBar;
   Color blue = new Color(51,0,204);
   MenuAction projectFileSendToMenu;
   MenuAction projectFileOpenWithMenu;
   
   MenuItemAction projectFileDefaultAppMenuItem;
   MenuItemAction revealFileMenuItem;
   MenuItemAction refreshProjectMenuItem;
   MenuItemAction removeProjectMenuItem;
   
   MenuItemAction reloadMenuItem;
   MenuItemAction deleteJobAction;
   MenuItemAction terminateJobAction;
   
   MenuAction jobResultFileSendToMenu;
   MenuAction saveServerFileMenu;
   MenuItemAction saveToFileSystemMenuItem;
   MenuItemAction deleteFileMenuItem;
   MenuAction openWithMenu;
   MenuItemAction jobResultFileTextViewerMenuItem;
   MenuItemAction jobResultFileDefaultAppMenuItem;
      
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
         JDialog dialog = new JDialog(this);
         dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
         dialog.setTitle(title);
         JTextArea textArea = new JTextArea(contents);
         textArea.setEditable(false);
         JScrollPane sp = new JScrollPane(textArea);
         dialog.getContentPane().add(sp, BorderLayout.CENTER);
         Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
         dialog.setSize(screenSize.width/2, screenSize.height/2);
         dialog.show();
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
						node.download(outputFile);
					//} catch (WebServiceException wse) {
                //  if(!disconnectedFromServer(wse)) {
                 //    GenePattern.showErrorDialog("An error occurred while saving " + outputFile.getName() + ". Please try again.");
                 // }
					} catch(IOException ioe) {
                  ioe.printStackTrace();
                  GenePattern.showErrorDialog("An error occurred while saving " + outputFile.getName() + ". Please try again.");
               }
				}
			}.start();
		}
	}

	public void changeServer(final String server, final String username) {
      Thread messageThread = new Thread() {
			public void run() {
				messageLabel.setText("Retrieving modules and jobs from " + server + "...");
            historyMenu.removeAll();
			}
		};
		SwingUtilities.invokeLater(messageThread);
      
		GPpropertiesManager.setProperty(PreferenceKeys.SERVER, server);
		GPpropertiesManager.setProperty(PreferenceKeys.USER_NAME, username);
		analysisServiceManager = AnalysisServiceManager.getInstance();
      analysisServiceManager.changeServer(server, username);
      setChangeServerActionsEnabled(false);
      
      
		new Thread() {
			public void run() {
            
				try {
					String lsidAuthority = (String) new org.genepattern.webservice.AdminProxy(
							analysisServiceManager.getServer(),
							analysisServiceManager.getUsername(), false)
							.getServiceInfo().get("lsid.authority");
					System.setProperty("lsid.authority", lsidAuthority);
               refreshJobs();
				} catch (WebServiceException wse) {
               wse.printStackTrace();
               // ignore the exception here, the user will be alerted in refreshTasks
				}
				refreshTasks();
            
            Thread changeStatusThread = new Thread() {
               public void run() {
                  messageLabel.setText("Server: " + server + "   Username: "
                        + username);
               }
            };
            SwingUtilities.invokeLater(changeStatusThread);
			}
		}.start();
      
		
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

	public MainFrame() {
      JWindow splash = GenePattern.showSplashScreen();
		splash.setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		String username = GPpropertiesManager
				.getProperty(PreferenceKeys.USER_NAME);

		if (username == null || username.trim().equals("")) {
         username = System.getProperty("user.name");
         if(username==null || username.trim().equals("")) {
            username = "anonymous";
         }
		}
		String server = GPpropertiesManager.getProperty(PreferenceKeys.SERVER);
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
      jobModel = JobModel.getInstance();      
      jobResultsTree = new SortableTreeTable(jobModel);
      projectDirModel = ProjectDirModel.getInstance();
      projectDirTree = new SortableTreeTable(projectDirModel, false);
		
      
      createJobActions();
      createJobResultFileActions();
      createProjectDirActions();
      createProjectFileActions();
     
		createMenus();
		
      
		jobModel.addJobListener(new JobListener() {
			public void jobStatusChanged(JobEvent e) {
			}

			public void jobAdded(JobEvent e) {
            // add to history
            final AnalysisJob job = e.getJob();
            
            Runnable doInsert = new Thread() {
               public void run() {
                  historyMenu.add(job);
               }
            };
            if(SwingUtilities.isEventDispatchThread()) {
               doInsert.run();  
            } else {
               SwingUtilities.invokeLater(doInsert);
            }
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
                  GenePattern.showError(GenePattern.getDialogParent(), fileToString(stderrFile));  
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
		analysisServicePanel = new AnalysisServiceDisplay();

		
		
      jobResultsTree.setFocusable(true);
      jobResultsTree.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyPressed(java.awt.event.KeyEvent e) {
            if(e.getKeyCode()==java.awt.event.KeyEvent.VK_BACK_SPACE) {
               if(selectedJobNode instanceof JobModel.JobNode) {
                  JobModel.JobNode jobNode = (JobModel.JobNode) selectedJobNode;
                  try {
                     jobModel.delete(jobNode);
                  } catch(WebServiceException wse) {
                     wse.printStackTrace();
                     if(!disconnectedFromServer(wse)) {
                        GenePattern.showErrorDialog("An error occurred deleting job number " + jobNode.job.getJobInfo().getJobNumber() + ". Please try again.");
                     }   
                  }
               } else if(selectedJobNode instanceof JobModel.ServerFileNode) {
                  JobModel.ServerFileNode serverFileNode = (JobModel.ServerFileNode) selectedJobNode;
                  try {
                     jobModel.delete(serverFileNode);
                  } catch(WebServiceException wse) {
                     wse.printStackTrace();
                     if(!disconnectedFromServer(wse)) {
                        GenePattern.showErrorDialog("An error occurred while deleting the file " + JobModel.getJobResultFileName(serverFileNode) + ". Please try again.");
                     }  
                  }
               }
            }
         }
      });
      
      
     
      
      jobResultsTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
         public void valueChanged(javax.swing.event.TreeSelectionEvent e) {
            TreePath path = jobResultsTree.getSelectionPath();
            if(path==null) {
               selectedJobNode = null;
            } else {
               selectedJobNode = (DefaultMutableTreeNode) path
						.getLastPathComponent();
            }
            boolean isJobNode = selectedJobNode instanceof JobModel.JobNode;
           
            deleteJobAction.setEnabled(isJobNode);
            terminateJobAction.setEnabled(isJobNode);
            reloadMenuItem.setEnabled(isJobNode);
            if(isJobNode) {
               JobModel.JobNode node = (JobModel.JobNode) selectedJobNode;
					 deleteJobAction.setEnabled(node.isComplete());
               terminateJobAction.setEnabled(!node.isComplete());
            }
            
            if(selectedJobNode==null) {
               isJobNode = true;
            }
            jobResultFileSendToMenu.setEnabled(!isJobNode); // FIXME
            saveServerFileMenu.setEnabled(!isJobNode);
            saveToFileSystemMenuItem.setEnabled(!isJobNode);
            deleteFileMenuItem.setEnabled(!isJobNode);
            openWithMenu.setEnabled(!isJobNode);
           
            if (selectedJobNode instanceof JobModel.ServerFileNode) {
					JobModel.ServerFileNode node = (JobModel.ServerFileNode) selectedJobNode;

					try {
						HttpURLConnection connection = (HttpURLConnection) node
								.getURL().openConnection();
						if (connection.getResponseCode() == HttpURLConnection.HTTP_GONE) {
							GenePattern.showMessageDialog(node.name
									+ " has been deleted from the server.");
							jobModel.remove(node);
							fileSummaryComponent.select(null);
                     return;
						} else {
							fileSummaryComponent.select(connection, node.name);
						}

					} catch (IOException ioe) {
                  ioe.printStackTrace();
					}

				} else {
					try {
						fileSummaryComponent.select(null);
					} catch (IOException x) {}
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
		
		String projectDirsString = GPpropertiesManager
				.getProperty(PreferenceKeys.PROJECT_DIRS);
		if (projectDirsString != null) {
			String[] projectDirs = projectDirsString.split(";");
			for (int i = 0; i < projectDirs.length; i++) {
				projectDirModel.add(new File(projectDirs[i]));
			}
		}
      
      projectDirTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
         public void valueChanged(javax.swing.event.TreeSelectionEvent e) {
            TreePath path = projectDirTree.getSelectionPath();
            if(path==null) {
               selectedProjectDirNode = null;
            } else {
               selectedProjectDirNode = (DefaultMutableTreeNode) path
                  .getLastPathComponent();
            }
            boolean projectNodeSelected = selectedProjectDirNode instanceof ProjectDirModel.ProjectDirNode;
            
            projectFileSendToMenu.setEnabled(!projectNodeSelected);
            projectFileOpenWithMenu.setEnabled(!projectNodeSelected);
            revealFileMenuItem.setEnabled(!projectNodeSelected);
             
            refreshProjectMenuItem.setEnabled(projectNodeSelected);
            removeProjectMenuItem.setEnabled(projectNodeSelected);
   
   
            if (selectedProjectDirNode instanceof ProjectDirModel.FileNode) {
					ProjectDirModel.FileNode node = (ProjectDirModel.FileNode) selectedProjectDirNode;
					ProjectDirModel.ProjectDirNode parent = (ProjectDirModel.ProjectDirNode) node
							.getParent();
					FileInputStream fis = null;
					File f = null;
					try {
						f = new File(parent.directory, node.file.getName());
						fileSummaryComponent.select(f);
					} catch (IOException ioe) {
                  ioe.printStackTrace();
						if (!f.exists()) {
							projectDirModel.refresh(parent);
						}
					} finally {
						if (fis != null) {
							try {
								fis.close();
							} catch (IOException x) {
							}
						}
					}

				} else {
					try {
                  fileSummaryComponent.select(null);
                           
					} catch (IOException x) {
					}
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
                  jobResultFileSendToMenu.setEnabled(true);
                  projectFileSendToMenu.setEnabled(true);
               
						jobResultFileSendToMenu.removeAll();
						projectFileSendToMenu.removeAll();

						for (Iterator it = analysisServicePanel
								.getInputFileParameterNames(); it.hasNext();) {
							final String name = (String) it.next();
                    final String displayName = AnalysisServiceDisplay.getDisplayString(name);
							MenuItemAction mi = new MenuItemAction(displayName) {
								public void actionPerformed(ActionEvent e) {
									analysisServicePanel.setInputFile(name,
											selectedJobNode);
								}
							};
							jobResultFileSendToMenu.add(mi);

							MenuItemAction projectMenuItem = new MenuItemAction(displayName) {
                        public void actionPerformed(
                              ActionEvent e) {
                           analysisServicePanel.setInputFile(
                                 name,
                                 selectedProjectDirNode);
                        }
							};

							projectFileSendToMenu.add(projectMenuItem);
						}

					}
				});

      
      JScrollPane projectSP = new JScrollPane(projectDirTree);
      JScrollPane jobSP = new JScrollPane(jobResultsTree);
      java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit()
				.getScreenSize();
      int width = (int) (screenSize.width * .9);
      int height = (int) (screenSize.height * .9);
      
      
      if(windowStyle==WINDOW_STYLE_ONE_FRAME) {
         
         Border title =  BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(0,0,0,0), "Projects", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.TOP);
       //  projectSP.setBorder(title);
         
         JPanel temp = new JPanel(new BorderLayout());
         temp.setBackground(new Color(24,48,115));
         temp.add(projectSP, BorderLayout.CENTER);
         if(RUNNING_ON_WINDOWS) {
            projectSP.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
            jobSP.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
         }
         
         JLabel l = new JLabel("Projects", JLabel.CENTER);
         l.setForeground(Color.white);
         l.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 14));
         temp.add(l, BorderLayout.NORTH);
         
         JPanel temp2 = new JPanel(new BorderLayout());
         temp2.setBackground(new Color(24,48,115));
         temp2.add(jobSP, BorderLayout.CENTER);
         JLabel l2 = new JLabel("Job Results", JLabel.CENTER);
         l2.setForeground(Color.white);
         l2.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 14));
         temp2.add(l2, BorderLayout.NORTH);
         
         JSplitPane leftPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				temp, temp2);
         if(RUNNING_ON_MAC) {
            leftPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
         }
        
         JPanel leftPanel = new JPanel(new BorderLayout());
         leftPanel.add(leftPane, BorderLayout.CENTER);
         leftPanel.add(fileSummaryComponent, BorderLayout.SOUTH);
         JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				leftPanel, analysisServicePanel);
         getContentPane().add(splitPane, BorderLayout.CENTER);
         getContentPane().add(messageLabel, BorderLayout.SOUTH);
        
         setSize(width, height);
         setLocation((screenSize.width - getWidth()) / 2, 20);
         setTitle(BuildProperties.PROGRAM_NAME);
         leftPane.setDividerLocation((int) (height * 0.4));
         splitPane.setDividerLocation((int) (width * 0.4));
         setJMenuBar(menuBar);
      } else if(windowStyle==WINDOW_STYLE_FRAMES) {
         JFrame projectDialog = new JFrame("Projects");
         projectDialog.setJMenuBar(menuBar); // FIXME
         projectDialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
         projectDialog.getContentPane().add(projectSP);
         int w = (int)(width*0.3);
         int h = (int)(height*.45);
         projectDialog.setSize(w, h);
         projectDialog.setVisible(true);
         projectDialog.setLocation(10, 10);
         
         JFrame jobDialog = new JFrame("Job Results");
         jobDialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
         jobDialog.getContentPane().add(jobSP);
         jobDialog.setSize(w, h);
         jobDialog.setLocation(10, 10 + projectDialog.getHeight());
         jobDialog.setVisible(true);
         
         JFrame moduleDialog = new JFrame("Module");
         
         moduleDialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
         moduleDialog.getContentPane().add(analysisServicePanel);
         moduleDialog.pack();
         w = (int)(width*0.6);
         h = (int)(height*.9);
         moduleDialog.setSize(w, h);
         moduleDialog.setLocation(10 + projectDialog.getWidth(), 10);
         moduleDialog.setVisible(true);

         setTitle(BuildProperties.PROGRAM_NAME);
          
      } else if(windowStyle==WINDOW_STYLE_MDI) {
         JInternalFrame projectsInternalFrame = new JInternalFrame("Projects", true, false, true, true);
         projectsInternalFrame.getContentPane().add(projectSP);
         int w = (int)(width*0.3);
         int h = (int)(height*.45);
         projectsInternalFrame.setSize(w, h);
         projectsInternalFrame.setVisible(true);
         projectsInternalFrame.setLocation(10, 10);
         
         JInternalFrame jobResultsInternalFrame = new JInternalFrame("Job Results", true, false, true, true);
         jobResultsInternalFrame.getContentPane().add(jobSP);
         jobResultsInternalFrame.setSize(w, h);
         jobResultsInternalFrame.setLocation(10, 10 + projectsInternalFrame.getHeight());
         jobResultsInternalFrame.setVisible(true);
         
         JInternalFrame moduleInternalFrame = new JInternalFrame("Module", true, false, true, true);
         
         moduleInternalFrame.getContentPane().add(analysisServicePanel);
         w = (int)(width*0.6);
         h = (int)(height*.9);
         moduleInternalFrame.setSize(w, h);
         moduleInternalFrame.setLocation(10 + projectsInternalFrame.getWidth(), 10);
         moduleInternalFrame.setVisible(true);
         
         JDesktopPane dp = new JDesktopPane();
         dp.setBackground(new Color(139, 139, 139));
         dp.add(projectsInternalFrame);
         dp.add(jobResultsInternalFrame);
         dp.add(moduleInternalFrame);
         setContentPane(new JScrollPane(dp));
         setSize(screenSize.width, screenSize.height);
         setTitle(BuildProperties.PROGRAM_NAME);
         
         addToWindowMenu("Job Results", jobResultsInternalFrame);
         addToWindowMenu("Module", moduleInternalFrame);
         addToWindowMenu("Projects", projectsInternalFrame);
         setJMenuBar(menuBar);
    
      }
      
		splash.dispose();
      show();
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
   
	public void refreshJobs() {
      final List errors = new ArrayList(); 
		new Thread() {
			public void run() {
            try {
               jobModel.getJobsFromServer();
            } catch(WebServiceException wse) {
               wse.printStackTrace();
               synchronized(errors) {
                  if(errors.size()==0) {
                     if(!disconnectedFromServer(wse)) {
                        GenePattern.showErrorDialog("An error occurred while retrieving your jobs. Please try again.");
                     }   
                     errors.add(new Object());
                  }
               }
            }
			}
		}.start();
      
      new Thread() {
         public void run() {
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
                        GenePattern.showErrorDialog("An error occurred while retrieving your job history. Please try again.");
                     }   
                     errors.add(new Object());
                  }
               }
            }
         }
      }.start();
	}
   
   private void createProjectFileActions() {
      projectFileSendToMenu = new MenuAction("Send To", IconManager.loadIcon(IconManager.SEND_TO_ICON));
      projectFileSendToMenu.setEnabled(false);
		
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
				GPpropertiesManager.setProperty(PreferenceKeys.PROJECT_DIRS,
						projectDirModel.getPreferencesString());
			}
		};
      
      
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
            JobModel.JobNode jobNode = (JobModel.JobNode) selectedJobNode;
            try {
               jobModel.delete(jobNode);
            } catch(WebServiceException wse) {
               wse.printStackTrace();
               if(!disconnectedFromServer(wse)) {
                  GenePattern.showErrorDialog("An error occurred deleting job number " + jobNode.job.getJobInfo().getJobNumber() + ". Please try again.");
               }   
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
                  GenePattern.showErrorDialog("An error occurred terminating job number " + jobNode.job.getJobInfo().getJobNumber() + ". Please try again.");
                } 
            }
			}
		};      
      
   }
   
   private void createJobResultFileActions() {
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
											node.download(outputFile);
											projectDirModel.refresh(dir);
                              } catch(IOException ioe) {
                                 ioe.printStackTrace();
                                 GenePattern.showErrorDialog("An error occurred while saving the file " + node.name  + ". Please try again.");
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
                 saveServerFileMenu.insert(menuItem, projectDirModel.indexOf(dir));
					}

					public void projectRemoved(ProjectEvent e) {
						File dir = e.getDirectory();
						for (int i = 0; i < saveServerFileMenu.getItemCount(); i++) {
							JMenuItem m = (JMenuItem) saveServerFileMenu
									.getMenuComponent(i);
							if (m.getText().equals(dir.getPath())) {
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
            try {
               jobModel.delete(serverFileNode);
            } catch(WebServiceException wse) {
               wse.printStackTrace();
               if(!disconnectedFromServer(wse)) {
                  GenePattern.showErrorDialog("An error occurred while deleting the file " + JobModel.getJobResultFileName(serverFileNode) + ". Please try again.");
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
			}
		};
		if (SwingUtilities.isEventDispatchThread()) {
			disableActions.run();
		} else {
			SwingUtilities.invokeLater(disableActions);
		}
   }
   
	public void refreshTasks() {
		setChangeServerActionsEnabled(false);
		new Thread() {
			public void run() {
				try {
					analysisServiceManager.refresh();
				} catch (WebServiceException wse) {
               wse.printStackTrace();
               if(!disconnectedFromServer(wse)) {
                  GenePattern.showErrorDialog("An error occurred while retrieving the modules from the server. Please try again.");
               }   
				}

				final Collection latestTasks = analysisServiceManager
						.getLatestAnalysisServices();

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
		}.start();
	}

   
	void createMenus() {
	   menuBar = new JMenuBar();
		fileMenu = new FileMenu();
		menuBar.add(fileMenu);
      
      MenuAction projectsMenuAction = null;
      if(revealFileMenuItem!=null) {
         projectsMenuAction = new MenuAction("Projects", new Object[]{refreshProjectMenuItem,  removeProjectMenuItem, new JSeparator(), projectFileSendToMenu, projectFileOpenWithMenu, revealFileMenuItem});
      } else {
          projectsMenuAction = new MenuAction("Projects", new Object[]{refreshProjectMenuItem,  removeProjectMenuItem, new JSeparator(), projectFileSendToMenu, projectFileOpenWithMenu});  
      }
      menuBar.add(projectsMenuAction.createMenu());
      
      MenuAction jobResultsMenuAction = new MenuAction("Job Results", new Object[]{reloadMenuItem, deleteJobAction, terminateJobAction, new JSeparator(), jobResultFileSendToMenu, saveServerFileMenu, deleteFileMenuItem, openWithMenu});
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
      menuBar.add(historyMenu);
      
      if(windowStyle==WINDOW_STYLE_MDI) {
         windowMenu = new JMenu("Window");
         menuBar.add(windowMenu);
      }
      
		JMenu helpMenu = new HelpMenu();
      menuBar.add(helpMenu);
		
      
      if(RUNNING_ON_MAC) {
         macos.MacOSMenuHelper.registerHandlers();  
      }
      
       
      MenuAction jobPopupAction = new MenuAction("", new Object[]{reloadMenuItem, deleteJobAction, terminateJobAction});
      jobPopupMenu = jobPopupAction.createPopupMenu();
      
      MenuAction jobResultFilePopupAction = new MenuAction("", new Object[]{jobResultFileSendToMenu, saveServerFileMenu, deleteFileMenuItem, openWithMenu});
      jobResultFilePopupMenu = jobResultFilePopupAction.createPopupMenu();
      
      MenuAction projectDirPopupMenuAction = new MenuAction("", new Object[]{refreshProjectMenuItem,  removeProjectMenuItem});
      projectDirPopupMenu = projectDirPopupMenuAction.createPopupMenu();
     
      MenuAction projectFilePopupMenuAction = new MenuAction("", new Object[]{projectFileSendToMenu, projectFileOpenWithMenu, revealFileMenuItem});
      projectFilePopupMenu = projectFilePopupMenuAction.createPopupMenu();
	}

   
   
   
   
   class HistoryMenu extends JMenu {
      final ActionListener reloadJobActionListener;
      List jobs = new ArrayList();
      List jobsInMenu = new ArrayList();
      JobNumberComparator jobNumberComparator = new JobNumberComparator();
      JMenuItem historyMenuItem = new JMenuItem("View All");
      final int JOBS_IN_MENU = 10;
      HistoryTableModel historyTableModel = new HistoryTableModel();
      JDialog historyDialog;
        
     class HistoryTableModel extends javax.swing.table.AbstractTableModel implements SortTableModel {
        private java.util.Comparator comparator = new JobModel.TaskNameComparator(false);
        
        public void sortOrderChanged(SortEvent e) {
            int column = e.getColumn();
            boolean ascending = e.isAscending();
            if (column == 0) {
               comparator = new JobModel.TaskNameComparator(ascending);
            } else if(column==1) {
               comparator = new JobModel.TaskCompletedDateComparator(ascending);
            } else {
               comparator = new JobModel.TaskSubmittedDateComparator(ascending);
            }
            Collections.sort(jobs, comparator);
            fireTableStructureChanged();
        }
   
        public Object getValueAt(int r, int c) {
           AnalysisJob job = (AnalysisJob) jobs.get(r);
           JobInfo jobInfo = job.getJobInfo();
           boolean complete = JobModel.isComplete(job); 
           switch(c) {
              case 0:
                  return JobModel.jobToString(job);
              case 1:
                  if(!complete) {
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
        
        public int getRowCount() {
            return jobs.size();   
        }
        
        public int getColumnCount() {
            return 3;  
        }
        
        public String getColumnName(int c) {
           switch(c) {
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
   
      
      /*class HistoryUpdateThread extends Thread {
         Calendar now;
         
         
         public HistoryUpdateThread() {
            setDaemon(true);  
            now = Calendar.getInstance();
         }
         
         public void run() {
            Calendar midnight = Calendar.getInstance();
            midnight.set(Calendar.HOUR, 0);
            midnight.set(Calendar.MINUTE, 0);
            midnight.set(Calendar.SECOND, 0);
            midnight.set(Calendar.MILLISECOND, 0);
            long sleepTime = midnight.getTimeInMillis() - now.getTimeInMillis();
            try {
               Thread.sleep(sleepTime);
            } catch(InterruptedException ie){}
            
            historyUpdateThread = new HistoryUpdateThread();
            historyUpdateThread.start();
         }
         
         int daysBefore(long otherMillis) {
            long nowMillis = now.getTimeInMillis();
            return (int)((otherMillis-nowMillis)/1000/60/60/24);
         }
      
      }*/
      
      
      
      public HistoryMenu() {
         super("Job History");
         reloadJobActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               AnalysisJobMenuItem menuItem = (AnalysisJobMenuItem) e.getSource();
               AnalysisJob job = menuItem.job;
               reload(job);
            }
         }; 
         
         historyMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               historyDialog.setVisible(true);
            }
         });
         clear();
         historyDialog = new JDialog((java.awt.Frame)GenePattern.getDialogParent());
         historyDialog.setTitle("History");
         final JTable t = new JTable(historyTableModel);
         t.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
               if(e.getClickCount()==2 && !e.isPopupTrigger()) {
                  int row = t.getSelectedRow();
                  AnalysisJob job = (AnalysisJob) jobs.get(row);
                  reload(job);
               }
            }
         });
         SortableHeaderRenderer r = new SortableHeaderRenderer(t, historyTableModel);
         historyDialog.getContentPane().add(new JScrollPane(t));
         historyDialog.pack();
         historyDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
         
      }
      
     // public void removeAll() {
     //    super.removeAll();  
        // addSeparator();
       //  clearHistoryMenuItem = new JMenuItem("Clear History");
       //  ActionListener clearHistoryListener = new ActionListener() {
       //     public void actionPerformed(ActionEvent e) {
       //        removeAll();
       //     }
       //  };
       //  clearHistoryMenuItem.addActionListener(clearHistoryListener);
       //  add(clearHistoryMenuItem);
       //  clearHistoryMenuItem.setEnabled(false);
      //}
      
      public void clear() {
         jobs.clear();
         jobsInMenu.clear();
         super.removeAll();  
         
         addSeparator();
         add(historyMenuItem);
      }
     
      
      public void add(AnalysisJob job) {
         int insertionIndex = Collections.binarySearch(jobsInMenu, job, jobNumberComparator);   
            
         if (insertionIndex < 0) {
            insertionIndex = -insertionIndex - 1;
         }
         
         if(insertionIndex < JOBS_IN_MENU) {
            AnalysisJobMenuItem menuItem = new AnalysisJobMenuItem(job);
            menuItem.setToolTipText(job.getJobInfo().getTaskLSID());
            menuItem.addActionListener(reloadJobActionListener);
            insert(menuItem, insertionIndex);
            if(getItemCount()==(JOBS_IN_MENU+3)) {
               remove(JOBS_IN_MENU-1); // separator is at JOBS_IN_MENU index
               jobsInMenu.remove(jobsInMenu.size()-1);
            }
            jobsInMenu.add(insertionIndex, job);
         }
         
         jobs.add(job);
         historyTableModel.fireTableStructureChanged();
        // clearHistoryMenuItem.setEnabled(true);
      }
   }
   
   
   static class JobNumberComparator implements java.util.Comparator {
      public int compare(Object obj1, Object obj2) {
         Integer job1Number = new Integer(((AnalysisJob)obj1).getJobInfo().getJobNumber());
         Integer job2Number = new Integer(((AnalysisJob)obj2).getJobInfo().getJobNumber());
         return job2Number.compareTo(job1Number);
         
      }
   }
   
   static class AnalysisJobMenuItem extends JMenuItem {
         AnalysisJob job;
         
         public AnalysisJobMenuItem(AnalysisJob job) {
            super(job.getJobInfo().getTaskName() + " (" + job.getJobInfo().getJobNumber() + ")");
            this.job = job;  
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
               setForeground(AUTHORITY_MINE_COLOR);
            } else if (authType
                  .equals(org.genepattern.util.LSIDUtil.AUTHORITY_FOREIGN)) {
               setForeground(AUTHORITY_FOREIGN_COLOR);
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

			JMenuItem moduleColorKeyMenuItem = new JMenuItem("Module Color Key");
			add(moduleColorKeyMenuItem);
			moduleColorKeyMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JPanel p = new JPanel();
					p.setLayout(new GridLayout(3, 1));
					JLabel colorKeyLabel = new JLabel("color key:");
					JLabel yourTasksLabel = new JLabel("your modules");
					yourTasksLabel.setForeground(AUTHORITY_MINE_COLOR);
					JLabel broadTasksLabel = new JLabel("Broad modules");

					JLabel otherTasksLabel = new JLabel("other modules");
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

		JMenuItem refreshTasksMenuItem;

		JFileChooser projectDirFileChooser;

		public void changeServerActionsEnabled(boolean b) {
			changeServerMenuItem.setEnabled(b);
			refreshMenu.setEnabled(b);
			refreshJobsMenuItem.setEnabled(b);
			refreshTasksMenuItem.setEnabled(b);
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
							GPpropertiesManager.setProperty(
									PreferenceKeys.PROJECT_DIRS,
									projectDirModel.getPreferencesString());
						}
					}
				}
			});
			add(openProjectDirItem);
			jobCompletedDialog = new JobCompletedDialog();
			final javax.swing.JCheckBoxMenuItem showJobCompletedDialogMenuItem = new javax.swing.JCheckBoxMenuItem(
					"Alert On Job Completion");
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
			refreshTasksMenuItem = new JMenuItem("Modules");
			refreshTasksMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					refreshTasks();
				}
			});
			refreshTasksMenuItem.setEnabled(false);
			refreshMenu.add(refreshTasksMenuItem);

			refreshJobsMenuItem = new JMenuItem("Jobs");
			refreshJobsMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					refreshJobs();
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