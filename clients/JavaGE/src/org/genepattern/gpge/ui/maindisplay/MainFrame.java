package org.genepattern.gpge.ui.maindisplay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.net.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.tree.*;
import org.genepattern.gpge.*;
import org.genepattern.gpge.io.*;
import org.genepattern.gpge.ui.browser.*;
import org.genepattern.gpge.ui.graphics.draggable.*;
import org.genepattern.gpge.ui.maindisplay.*;
import org.genepattern.gpge.ui.preferences.*;
import org.genepattern.gpge.ui.tasks.*;
import org.genepattern.gpge.ui.project.*;
import org.genepattern.gpge.ui.treetable.*;
import org.genepattern.gpge.util.BuildProperties;
import org.genepattern.modules.ui.graphics.*;
import org.genepattern.util.*;
import org.genepattern.webservice.*;

/**
 * Description of the Class
 * 
 * @author Joshua Gould
 */
public class MainFrame extends JFrame {
         
	public static boolean RUNNING_ON_MAC = System.getProperty("mrj.version") != null
			&& javax.swing.UIManager.getSystemLookAndFeelClassName()
					.equals(
							javax.swing.UIManager.getLookAndFeel().getClass()
									.getName());
       
	AnalysisServicePanel analysisServicePanel;

	JLabel messageLabel = new JLabel("", JLabel.CENTER);

	AnalysisServiceManager analysisServiceManager;

	public final static Color AUTHORITY_MINE_COLOR = java.awt.Color.decode("0xFF00FF");

	public final static Color AUTHORITY_FOREIGN_COLOR = java.awt.Color
			.decode("0x0000FF");

	AnalysisMenu analysisMenu;

	AnalysisMenu visualizerMenu;
   
   AnalysisMenu pipelineMenu;

   HistoryMenu historyMenu;
   
	JPopupMenu jobPopupMenu = new JPopupMenu();

	JPopupMenu projectDirPopupMenu;

	JPopupMenu projectFilePopupMenu;

	JPopupMenu serverFilePopupMenu = new JPopupMenu();

	SortableTreeTable jobResultsTree;

	JobModel jobModel;

	SortableTreeTable projectDirTree;

	ProjectDirModel projectDirModel;

	DefaultMutableTreeNode selectedJobNode = null;

	DefaultMutableTreeNode selectedProjectDirNode = null;

	JFileChooser saveAsFileChooser = new JFileChooser();

	FileMenu fileMenu;

	final static int MENU_SHORTCUT_KEY_MASK = Toolkit.getDefaultToolkit()
			.getMenuShortcutKeyMask();

	FileInfoComponent fileSummaryComponent = new FileInfoComponent();

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
				messageLabel.setText("Retrieving tasks and jobs from " + server + "...");
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
               jobModel.getJobsFromServer();
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

		createMenuBar();
		jobModel = JobModel.getInstance();      
     
      
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
                     if(params[i].getValue().equals(jobNumber + "/stderr") || params[i].getValue().equals(jobNumber + "\\stderr")) {
                        stderrIndex = i;
                        break;  
                     }
                  }
               }
            }
            if(stderrIndex >= 0) {
               File stderrFile = null;
               try {
                  stderrFile = File.createTempFile("stderr", null);
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
		analysisServicePanel = new AnalysisServicePanel(analysisServiceManager);

		projectDirModel = ProjectDirModel.getInstance();
		projectDirTree = new SortableTreeTable(projectDirModel);

		jobResultsTree = new SortableTreeTable(jobModel);

      
      JMenuItem reloadMenuItem = new JMenuItem("Reload");
		jobPopupMenu.add(reloadMenuItem);
      reloadMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reload(((JobModel.JobNode) selectedJobNode).job);
			}
		});
      
      final JMenuItem deleteJobMenuItem = new JMenuItem(
				"Delete Job", IconManager.loadIcon(IconManager.DELETE_ICON));
      deleteJobMenuItem.addActionListener(new ActionListener() {
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
      });
		jobPopupMenu.add(deleteJobMenuItem);
      
      final JMenuItem terminateJobMenuItem = new JMenuItem("Terminate Job", IconManager.loadIcon(IconManager.STOP_ICON));
		terminateJobMenuItem.addActionListener(new ActionListener() {
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
		});      
      jobPopupMenu.add(terminateJobMenuItem);

		final JMenu saveServerFileMenu = new JMenu("Save To");
		JMenuItem saveToFileSystemMenuItem = new JMenuItem("Other...", IconManager.loadIcon(IconManager.SAVE_AS_ICON));
		saveToFileSystemMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showSaveDialog((JobModel.ServerFileNode) selectedJobNode);
			}
		});
		projectDirModel
				.addProjectDirectoryListener(new ProjectDirectoryListener() {
					public void projectAdded(ProjectEvent e) {
						final File dir = e.getDirectory();
						JMenuItem menuItem = new JMenuItem(dir.getPath(), IconManager.loadIcon(IconManager.SAVE_ICON));
                  
						saveServerFileMenu.insert(menuItem, projectDirModel.indexOf(dir));
						menuItem.addActionListener(new ActionListener() {
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
						});
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
		serverFilePopupMenu.add(saveServerFileMenu);

		final JMenu serverFileSendToMenu = new JMenu("Send To");
      serverFileSendToMenu.setEnabled(false);
      
      serverFileSendToMenu.setIcon(IconManager.loadIcon(IconManager.SEND_TO_ICON));
		serverFilePopupMenu.add(serverFileSendToMenu);

      JMenuItem deleteFileMenuItem = new JMenuItem("Delete File", IconManager.loadIcon(IconManager.DELETE_ICON));
		serverFilePopupMenu.add(deleteFileMenuItem);
      deleteFileMenuItem.addActionListener(new ActionListener() {
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
		});
      
      JMenu openWithMenu = new JMenu("Open With");
      
      JMenuItem jobResultFileTextViewerMenuItem = new JMenuItem("Text Viewer", IconManager.loadIcon(IconManager.TEXT_ICON));
      jobResultFileTextViewerMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
            new Thread() {
               public void run() {
                  textViewer(selectedJobNode);
               }
            }.start();
			}
		});
      openWithMenu.add(jobResultFileTextViewerMenuItem);
      
      JMenuItem jobResultFileDefaultAppMenuItem = new JMenuItem("Default Application");
      jobResultFileDefaultAppMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
            new Thread() {
               public void run() {
                  defaultApplication(selectedJobNode);
               }
            }.start();
			}
		});
      openWithMenu.add(jobResultFileDefaultAppMenuItem);
      serverFilePopupMenu.add(openWithMenu);
      
    
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

				if (selectedJobNode instanceof JobModel.ServerFileNode) {
					JobModel.ServerFileNode node = (JobModel.ServerFileNode) selectedJobNode;

					JobModel.JobNode parent = (JobModel.JobNode) node
							.getParent();

					try {
						HttpURLConnection connection = (HttpURLConnection) parent
								.getURL(node.name).openConnection();
						if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
							GenePattern.showMessageDialog(node.name
									+ " has been deleted from the server.");
							jobModel.remove(node);
							fileSummaryComponent.select(null);
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

				if (!isPopupTrigger(e)) {
					return;
				}

				if (selectedJobNode instanceof JobModel.JobNode) {
					JobModel.JobNode node = (JobModel.JobNode) selectedJobNode;
					deleteJobMenuItem.setEnabled(node.isComplete());
               terminateJobMenuItem.setEnabled(!node.isComplete());
					jobPopupMenu.show(e.getComponent(), e.getX(), e.getY());
				} else if (selectedJobNode instanceof JobModel.ServerFileNode) {
					serverFilePopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
				}
			}

			
		});
		projectDirModel = ProjectDirModel.getInstance();

		String projectDirsString = GPpropertiesManager
				.getProperty(PreferenceKeys.PROJECT_DIRS);
		if (projectDirsString != null) {
			String[] projectDirs = projectDirsString.split(";");
			for (int i = 0; i < projectDirs.length; i++) {
				projectDirModel.add(new File(projectDirs[i]));
			}
		}
		projectDirTree = new SortableTreeTable(projectDirModel, false);
		projectFilePopupMenu = new JPopupMenu();
		final JMenu projectFileSendToMenu = new JMenu("Send To");
      projectFileSendToMenu.setEnabled(false);
      projectFileSendToMenu.setIcon(IconManager.loadIcon(IconManager.SEND_TO_ICON));
		projectFilePopupMenu.add(projectFileSendToMenu);

      JMenu projectFileOpenWithMenu = new JMenu("Open With");
      JMenuItem projectFileTextViewerMenuItem = new JMenuItem("Text Viewer", IconManager.loadIcon(IconManager.TEXT_ICON));
      projectFileTextViewerMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
            new Thread() {
               public void run() {
                  textViewer(selectedProjectDirNode);
               }
            }.start();
			}
		});
      projectFileOpenWithMenu.add(projectFileTextViewerMenuItem);
      
      JMenuItem projectFileDefaultAppMenuItem = new JMenuItem("Default Application");
      projectFileDefaultAppMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
            new Thread() {
               public void run() {
                  defaultApplication(selectedProjectDirNode);
               }
            }.start();
			}
		});
      projectFileOpenWithMenu.add(projectFileDefaultAppMenuItem);
      
      projectFilePopupMenu.add(projectFileOpenWithMenu);
      
      if(RUNNING_ON_MAC) {
         JMenuItem revealInFinderMenuItem = new JMenuItem("Show In Finder");
          revealInFinderMenuItem.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                ProjectDirModel.FileNode fn = (ProjectDirModel.FileNode) selectedProjectDirNode;
                org.genepattern.gpge.util.MacOS.showFileInFinder(fn.file);
             }
          });
      
         projectFilePopupMenu.add(revealInFinderMenuItem);
      } else if(System.getProperty("os.name").startsWith("Windows")) {
         JMenuItem revealInExplorerMenuItem = new JMenuItem("Show File Location");
         revealInExplorerMenuItem.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                try {
                   ProjectDirModel.FileNode fn = (ProjectDirModel.FileNode) selectedProjectDirNode;
                   BrowserLauncher.openURL(fn.file.getParentFile().getCanonicalPath());  
                } catch(IOException x){
                   x.printStackTrace();
                }
             }
          });
         projectFilePopupMenu.add(revealInExplorerMenuItem);            
      }
      
		projectDirPopupMenu = new JPopupMenu();
      JMenuItem refreshProjectMenuItem = new JMenuItem("Refresh", IconManager.loadIcon(IconManager.REFRESH_ICON));
		projectDirPopupMenu.add(refreshProjectMenuItem);
      refreshProjectMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				projectDirModel
						.refresh((ProjectDirModel.ProjectDirNode) selectedProjectDirNode);
			}
		});
      
    
      JMenuItem removeProjectMenuItem = new JMenuItem("Close Project", IconManager.loadIcon(IconManager.REMOVE_ICON));
		projectDirPopupMenu.add(removeProjectMenuItem);
      removeProjectMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				projectDirModel
						.remove((ProjectDirModel.ProjectDirNode) selectedProjectDirNode);
				GPpropertiesManager.setProperty(PreferenceKeys.PROJECT_DIRS,
						projectDirModel.getPreferencesString());
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
                  serverFileSendToMenu.setEnabled(true);
                  projectFileSendToMenu.setEnabled(true);
               
						serverFileSendToMenu.removeAll();
						projectFileSendToMenu.removeAll();

						for (Iterator it = analysisServicePanel
								.getInputFileParameterNames(); it.hasNext();) {
							final String name = (String) it.next();
							JMenuItem mi = new JMenuItem(name);
							mi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent e) {
									analysisServicePanel.setInputFile(name,
											selectedJobNode);
								}
							});
							serverFileSendToMenu.add(mi);

							JMenuItem projectMenuItem = new JMenuItem(name);
							projectMenuItem
									.addActionListener(new ActionListener() {
										public void actionPerformed(
												ActionEvent e) {
											analysisServicePanel.setInputFile(
													name,
													selectedProjectDirNode);
										}
									});

							projectFileSendToMenu.add(projectMenuItem);
						}

					}
				});

      JScrollPane projectSP = new JScrollPane(projectDirTree);
      projectSP.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Projects", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.TOP));
      JScrollPane jobSP = new JScrollPane(jobResultsTree);
      jobSP.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Jobs", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.TOP));
    
		JSplitPane leftPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				projectSP, jobSP);

		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.add(leftPane, BorderLayout.CENTER);
		leftPanel.add(fileSummaryComponent, BorderLayout.SOUTH);

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				leftPanel, analysisServicePanel);
		getContentPane().add(splitPane, BorderLayout.CENTER);

		getContentPane().add(messageLabel, BorderLayout.SOUTH);

		java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit()
				.getScreenSize();
		int width = (int) (screenSize.width * .9);
		int height = (int) (screenSize.height * .9);
		setSize(width, height);
		setLocation((screenSize.width - getWidth()) / 2, 20);
		setTitle(BuildProperties.PROGRAM_NAME);
		splash.hide();
		splash.dispose();
		leftPane.setDividerLocation((int) (height * 0.4));

		splitPane.setDividerLocation((int) (width * 0.4));
		show();

	}

	public void refreshJobs() {
		new Thread() {
			public void run() {
            try {
               jobModel.getJobsFromServer();
            } catch(WebServiceException wse) {
               wse.printStackTrace();
               if(!disconnectedFromServer(wse)) {
                  GenePattern.showErrorDialog("An error occurred while retrieving your jobs. Please try again.");
               }   
            }
			}
		}.start();
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
                  GenePattern.showErrorDialog("An error occurred while retrieving the tasks from the server. Please try again.");
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

	void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		fileMenu = new FileMenu();
		menuBar.add(fileMenu);
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
      
		JMenu helpMenu = new HelpMenu();
      menuBar.add(helpMenu);
		
		setJMenuBar(menuBar);
	}

   class HistoryMenu extends JMenu {
      final ActionListener historyMenuItemActionListener;
      JMenuItem clearHistoryMenuItem;
       
      public HistoryMenu() {
         super("History");
         historyMenuItemActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               AnalysisJobMenuItem menuItem = (AnalysisJobMenuItem) e.getSource();
               AnalysisJob job = menuItem.job;
               reload(job);
            }
         }; 
         removeAll();
      }
      
      public void removeAll() {
         super.removeAll();  
         addSeparator();
         clearHistoryMenuItem = new JMenuItem("Clear History");
         ActionListener clearHistoryListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               removeAll();
            }
         };
         clearHistoryMenuItem.addActionListener(clearHistoryListener);
         add(clearHistoryMenuItem);
         clearHistoryMenuItem.setEnabled(false);
      }
      
      public void add(AnalysisJob job) {
         AnalysisJobMenuItem menuItem = new AnalysisJobMenuItem(job);
         menuItem.setToolTipText(job.getJobInfo().getTaskLSID());
         menuItem.addActionListener(historyMenuItemActionListener);
         historyMenu.insert(menuItem, 0);
         clearHistoryMenuItem.setEnabled(true);
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

         JMenuItem aboutMenuItem = new JMenuItem("About");
			add(aboutMenuItem);
         aboutMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					GenePattern.showAbout();
				}
			});

			JMenuItem moduleColorKeyMenuItem = new JMenuItem("Module Color Key");
			add(moduleColorKeyMenuItem);
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
			refreshTasksMenuItem = new JMenuItem("Tasks");
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

			AbstractAction quitAction = new javax.swing.AbstractAction("Quit") {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					System.exit(0);
				}
			};
			quitAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(
					'Q', MENU_SHORTCUT_KEY_MASK));
			add(quitAction);

		}
	}
}