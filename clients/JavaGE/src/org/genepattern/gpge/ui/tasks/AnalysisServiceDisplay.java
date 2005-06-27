package org.genepattern.gpge.ui.tasks;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.border.Border;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.ui.graphics.draggable.ObjectTextField;
import org.genepattern.gpge.ui.maindisplay.MainFrame;
import org.genepattern.gpge.ui.maindisplay.TogglePanel;
import org.genepattern.gpge.ui.project.ProjectDirModel;
import org.genepattern.modules.ui.graphics.*;
import org.genepattern.util.*;
import org.genepattern.webservice.*;
import org.genepattern.webservice.TaskIntegratorProxy;
import org.genepattern.gpge.ui.maindisplay.LSIDUtil;
import org.genepattern.webservice.WebServiceException;
import org.genepattern.gpge.ui.preferences.PreferenceKeys;
import org.genepattern.gpge.ui.tasks.TaskLauncher;
import org.genepattern.codegenerator.*;
import org.genepattern.gpge.PropertyManager;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 *  Displays an <tt>AnalysisService</tt>
 *
 * @author    Joshua Gould
 */
public class AnalysisServiceDisplay extends JPanel {
   /** the currently displayed <tt>AnalysisService</tt> */
   private AnalysisService selectedService;
   /** whether the <tt>selectedService</tt> has documentation */
   private volatile boolean hasDocumentation;
   private String latestVersion;
   private javax.swing.event.EventListenerList listenerList = new javax.swing.event.EventListenerList();
   
   private boolean advancedGroupExpanded = false;
   private TogglePanel togglePanel;
   private ParameterInfoPanel parameterInfoPanel;
   
   public AnalysisServiceDisplay() {
      this.setBackground(Color.white);
	  showGettingStarted();
   }

   public void showGettingStarted() {
		 java.net.URL url = ClassLoader.getSystemResource
		 ("org/genepattern/gpge/resources/getting_started.html");
		 
		 final JTextPane pane = new JTextPane();
		 pane.setContentType("text/html");
		 pane.addHyperlinkListener(new HyperlinkListener() {
         public void hyperlinkUpdate(HyperlinkEvent evt)
         {
            if(evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
            {
               URL url = evt.getURL();
               try  {
                  BrowserLauncher.openURL(url.toString());
               } catch(Exception e){}
            } else if (evt.getEventType() == HyperlinkEvent.EventType.ENTERED) {
               pane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else if (evt.getEventType() == HyperlinkEvent.EventType.EXITED) {
               pane.setCursor(Cursor.getDefaultCursor());
            }
         } 
       });
   
		 try {
			 pane.setPage(url);
		 } catch(Exception e){
			 e.printStackTrace();
		 }
		 pane.setMargin(new Insets(5, 5, 5, 5));
		 pane.setEditable(false);
		 pane.setBackground(Color.white);
		 removeAll();
		 setLayout(new BorderLayout());
		 add(pane, BorderLayout.CENTER);
		 invalidate();
		 validate();
		 selectedService = null;
		 notifyListeners();
   }


   public void addAnalysisServiceSelectionListener(
         AnalysisServiceSelectionListener l) {
      listenerList.add(AnalysisServiceSelectionListener.class, l);
   }


   public void removeAnalysisServiceSelectionListener(
         AnalysisServiceSelectionListener l) {
      listenerList.remove(AnalysisServiceSelectionListener.class, l);
   }

   
   private Border createBorder(final Border b, final int left, final int top, final int right, final int bottom) {
      return new javax.swing.border.Border() {
			public Insets getBorderInsets(java.awt.Component c) {
				Insets i = b.getBorderInsets(c);
				if(left >= 0) {
               i.left = left;
            } 
            if(top >= 0) {
               i.top = top;  
            }
            if(right >= 0) {
               i.right = right;  
            }
            if(bottom >= 0) {
               i.bottom = bottom;  
            }
            
				return i;
			}
			
			public boolean isBorderOpaque() {
				return b.isBorderOpaque();
			}
			
			public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
				b.paintBorder(c, g, x, y, width, height);
			}

		};  
   }

   /**
    *  Displays the given analysis service
    *
    * @param  selectedService  Description of the Parameter
    */
   public void loadTask(AnalysisService _selectedService) {
      this.selectedService = _selectedService;
      hasDocumentation = true;
      if(togglePanel!=null) {
          advancedGroupExpanded = togglePanel.isExpanded();
      }
      if(selectedService!=null) {
         new Thread() {
            public void run() {
               try {
                  
                  String username = AnalysisServiceManager.getInstance().getUsername();
                  String server = selectedService.getServer();
                  String lsid = LSIDUtil.getTaskId(selectedService.getTaskInfo());
                  String[] supportFileNames = new TaskIntegratorProxy(server, username).getSupportFileNames(lsid);
                  hasDocumentation = supportFileNames!=null && supportFileNames.length > 0;
               } catch(WebServiceException wse) {
                  wse.printStackTrace();  
               }
            }
         }.start();
      }
            
      
      latestVersion = null;
      TaskInfo taskInfo = selectedService.getTaskInfo();
      String taskName = taskInfo.getName();
      String taskVersionDisplay = "";
      JComboBox versionComboBox = null;
      try {
         final LSID lsid = new LSID((String) selectedService.getTaskInfo()
               .getTaskInfoAttributes().get(GPConstants.LSID));
         if(!org.genepattern.gpge.ui.maindisplay.LSIDUtil.isBroadTask(lsid)) {
            String authority = lsid.getAuthority();
            taskVersionDisplay += " (" + authority + ")";
         }

         final String lsidNoVersion = lsid.toStringNoVersion();
         List versions = (List) AnalysisServiceManager.getInstance().getLSIDToVersionsMap().get(lsidNoVersion);
         Vector versionsCopy = new Vector();
         versionsCopy.add("");
         latestVersion = lsid.getVersion();
         if(versions!=null) {
            for(int i = 0; i < versions.size(); i++) {
               String version = (String) versions.get(i);
               if(version.compareTo(latestVersion) > 0) {
                  latestVersion = version;
               }
            }
         }
         if (lsid.getVersion().equals(latestVersion)) {
            taskVersionDisplay += ", version " + lsid.getVersion() + " (latest)";
         } else {
            taskVersionDisplay += ", version " + lsid.getVersion();
         }
         
         if(versions!=null) {
            for(int i = 0; i < versions.size(); i++) {
               String version = (String) versions.get(i);
               if(version.equals(lsid.getVersion())) {
                  continue;  
               }
               
               if(version.equals(latestVersion)) {
                  version += " (latest)";
               }
   
               versionsCopy.add(version);
            }
         }
         Collections.sort(versionsCopy, String.CASE_INSENSITIVE_ORDER);

         if(versionsCopy.size() > 1) {
            versionComboBox = new JComboBox(versionsCopy);
            if(!MainFrame.RUNNING_ON_MAC) {
               versionComboBox.setBackground(java.awt.Color.white);
            }
            if(lsid.getVersion().equals(latestVersion)) {
               versionComboBox.setSelectedItem(lsid.getVersion() + " (latest)");
            } else {
               versionComboBox.setSelectedItem(lsid.getVersion());
            }

            versionComboBox.addActionListener(
               new ActionListener() {
                  public void actionPerformed(ActionEvent e) {
                     String selectedItem = (String) ((JComboBox) e.getSource()).getSelectedItem();
                     if(selectedItem.equals("")) {
                        return;
                     }
                     int index = selectedItem.indexOf(" (latest");

                     if(index > 0) {
                        selectedItem = selectedItem.substring(0, index);
                     }
                     if(selectedItem.equals(lsid.getVersion())) {
                        return;
                     }
                     String selectedLSID = lsidNoVersion + ":"
                            + selectedItem;
                     AnalysisService svc = AnalysisServiceManager.getInstance().getAnalysisService(selectedLSID);
                     if(svc == null) {
                        GenePattern.showMessageDialog(
                              "The task was not found.");
                     } else {
                        loadTask(svc);
                     }
                  }
               });
         }
      } catch(MalformedURLException mfe) {
         mfe.printStackTrace();
      }

      ParameterInfo[] params = taskInfo.getParameterInfoArray();
       
      boolean showDescriptions = Boolean.valueOf(PropertyManager
				.getProperty(PreferenceKeys.SHOW_PARAMETER_DESCRIPTIONS)).booleanValue();
            
      parameterInfoPanel = new ParameterInfoPanel(taskName, params);
      removeAll();

      Component taskNameComponent = new JLabel(taskName);
      taskNameComponent.setFont(taskNameComponent.getFont().deriveFont(java.awt.Font.BOLD));
      
      JLabel taskVersionLabel = new JLabel(taskVersionDisplay);
      
      Component description = createWrappedLabel(selectedService.getTaskInfo().getDescription());
      
      JPanel topPanel = null;
      final JCheckBox showDescriptionsCheckBox = new JCheckBox("Show Parameter Descriptions");
     
      showDescriptionsCheckBox.setSelected(showDescriptions);
      showDescriptionsCheckBox.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            boolean showDescriptions = showDescriptionsCheckBox.isSelected();
            PropertyManager
				.setProperty(PreferenceKeys.SHOW_PARAMETER_DESCRIPTIONS, String.valueOf(showDescriptions));
            parameterInfoPanel.setDescriptionsVisible(showDescriptions);
         }
      });
      if(versionComboBox != null) {
         JPanel temp = new JPanel(new FormLayout("left:pref:none, left:pref:none, 12px, left:pref:none, 6px, left:pref:none, right:pref:g", "pref, 6px")); // title, task version, version label, version combo box, show parameter desc checkbox   
         CellConstraints cc = new CellConstraints();
         JLabel versionLabel = new JLabel("Choose Version:");
         temp.add(taskNameComponent, cc.xy(1, 1));
         temp.add(taskVersionLabel, cc.xy(2, 1));
         temp.add(versionLabel, cc.xy(4, 1));
         temp.add(versionComboBox, cc.xy(6, 1));
         temp.add(showDescriptionsCheckBox, cc.xy(7, 1));
         topPanel = new JPanel(new BorderLayout()); 
         topPanel.add(temp, BorderLayout.NORTH);
         topPanel.add(description, BorderLayout.SOUTH);
      } else {
         CellConstraints cc = new CellConstraints();
         JPanel temp = new JPanel(new FormLayout("left:pref:none, left:pref:none, right:pref:g", "pref, 6px")); // title, task version, show parameter desc checkbox 
         
         temp.add(taskNameComponent, cc.xy(1, 1));
         temp.add(taskVersionLabel, cc.xy(2, 1));
         temp.add(showDescriptionsCheckBox, cc.xy(3, 1));
         topPanel = new JPanel(new BorderLayout()); 
         topPanel.add(temp, BorderLayout.NORTH);
         topPanel.add(description, BorderLayout.SOUTH);
      }
      topPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
      
      setLayout(new BorderLayout());
      JPanel buttonPanel = new JPanel();
      JButton submitButton = new JButton("Run");
      submitButton.addActionListener(new SubmitActionListener());
      buttonPanel.add(submitButton);
      
      JButton resetButton = new JButton("Reset");
      resetButton.addActionListener(new ResetActionListener());
      buttonPanel.add(resetButton);
      JButton helpButton = new JButton("Help");
      helpButton.addActionListener(new HelpActionListener());
      buttonPanel.add(helpButton);
      
      JPanel viewCodePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      
      JLabel viewCodeLabel = new JLabel("View Code:");
      final JComboBox viewCodeComboBox = new JComboBox(new Object[]{"Java", "MATLAB", "R"});
      viewCodePanel.add(viewCodeLabel);
      viewCodePanel.add(viewCodeComboBox);
      
      togglePanel = new TogglePanel("Advanced", viewCodePanel);
      togglePanel.setExpanded(advancedGroupExpanded);
      /*JTaskPaneGroup group = new JTaskPaneGroup();
      group.setText("Advanced Options");
      JTaskPane tp = new JTaskPane();
      group.add(viewCodePanel);
      tp.add(group);*/
      
      JPanel bottomPanel = new JPanel(new BorderLayout());
	  bottomPanel.setBorder(createBorder(UIManager.getLookAndFeelDefaults().getBorder("ScrollPane.border"), 0, 0, 0, 2));
		
	  bottomPanel.add(buttonPanel, BorderLayout.CENTER);
	  bottomPanel.add(togglePanel, BorderLayout.SOUTH);
	  
      viewCodeComboBox.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            String item = (String) viewCodeComboBox.getSelectedItem();
            if("View Code".equals(item)) {
               return;
            }
            String language = item;
            TaskCodeGenerator codeGenerator = null;
            if("Java".equals(language)) {
               codeGenerator = new JavaPipelineCodeGenerator();
            } else if("MATLAB".equals(language)) {
               codeGenerator = new MATLABPipelineCodeGenerator();
            } else if("R".equals(language)) {
               codeGenerator = new RPipelineCodeGenerator();
            } else {
               throw new IllegalArgumentException("Unknown language");
            }  
            String lsid = (String) selectedService.getTaskInfo()
               .getTaskInfoAttributes().get(GPConstants.LSID);
               
            JobInfo jobInfo = new JobInfo(-1, -1, null, null, null, parameterInfoPanel.getParameterInfoArray(), AnalysisServiceManager.getInstance().getUsername(), lsid, selectedService.getTaskInfo().getName());
            AnalysisJob job = new AnalysisJob(selectedService.getServer(), jobInfo, TaskLauncher.isVisualizer(selectedService));
            org.genepattern.gpge.ui.code.Util.viewCode(codeGenerator, job, "Java");
         }
      });
      
      add(topPanel, BorderLayout.NORTH);
     
		JScrollPane sp = new JScrollPane(parameterInfoPanel);
      final javax.swing.border.Border b = sp.getBorder();
      sp.setBorder(createBorder(b, 0, -1, -1, -1));
        add(sp, BorderLayout.CENTER);
	  
      add(bottomPanel, BorderLayout.SOUTH);
      setMinimumSize(new java.awt.Dimension(100, 100));
      revalidate();
      doLayout();
      notifyListeners(); 
   }



   protected void notifyListeners() {
      Object[] listeners = listenerList.getListenerList();
      AnalysisServiceSelectionEvent e = null;
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for(int i = listeners.length - 2; i >= 0; i -= 2) {
         if(listeners[i] == AnalysisServiceSelectionListener.class) {
            // Lazily create the event:
            if(e == null) {
               e = new AnalysisServiceSelectionEvent(this,
                     selectedService);
            }

            ((AnalysisServiceSelectionListener) listeners[i + 1])
                  .valueChanged(e);
         }
      }
   }


   

   static JTextArea createWrappedLabel(String s) {
      JTextArea jTextArea = new JTextArea();
      // Set JTextArea to look like JLabel
      jTextArea.setWrapStyleWord(true);
      jTextArea.setLineWrap(true);
      jTextArea.setEnabled(false);
      jTextArea.setEditable(false);
      jTextArea.setOpaque(false);
      jTextArea.setFont(javax.swing.UIManager.getFont("Label.font"));
      jTextArea.setBackground(UIManager.getColor("Label.background"));
      jTextArea.setDisabledTextColor(UIManager.getColor("Label.foreground"));
      jTextArea.setText(s);
      return jTextArea;
   }


  

   /**
    *  Sets the value of the given parameter to the given node
    *
    * @param  parameterName  the parmeter name
    * @param  node           a tree node
    */
   public void setInputFile(String parameterName,
         javax.swing.tree.TreeNode node) {
      if(selectedService != null) {
         ObjectTextField tf = (ObjectTextField) parameterInfoPanel.getComponent(parameterName);
         if(tf!=null) {
            tf.setObject(node);
         }
      }
   }


   public static String getDisplayString(ParameterInfo p) {
      return getDisplayString(p.getName());
   }
   
   
   public static String getDisplayString(String name) {
      return name.replace('.', ' ');
   }


   /**
    *  Returns <tt>true</tt> of this panel is showing an <tt>AnalysisService
    *  </tt>
    *
    * @return    whether this panel is showing an <tt>AnalysisService</tt>
    */
   public boolean isShowingAnalysisService() {
      return selectedService != null;
   }


   /**
    *  Gets a collection of input file parameters
    *
    * @return    the input file names
    */
   public java.util.Iterator getInputFileParameters() {
      return parameterInfoPanel.getInputFileParameters();
   }

   

   private class ResetActionListener implements ActionListener {
      public final void actionPerformed(ActionEvent ae) {
         loadTask(selectedService);
      }
   }


   private class HelpActionListener implements ActionListener {
      public final void actionPerformed(java.awt.event.ActionEvent ae) {
         try {
            String username = AnalysisServiceManager.getInstance().getUsername();
            String server = selectedService.getServer();
            String lsid = LSIDUtil.getTaskId(selectedService.getTaskInfo());
       
            if(hasDocumentation) {
               String docURL = server + "/gp/getTaskDoc.jsp?name="
                   + lsid + "&"
                   + GPConstants.USERID + "="+java.net.URLEncoder.encode(username, "UTF-8");
               org.genepattern.util.BrowserLauncher.openURL(docURL);
            } else {
               GenePattern.showMessageDialog( selectedService.getTaskInfo().getName() + "has no documentation");  
            }
         } catch(java.io.IOException ex) {
            System.err.println(ex);
         }
      }
   }


   
   
   private class SubmitActionListener implements ActionListener {
      public final void actionPerformed(ActionEvent ae) {
         final JButton source = (JButton) ae.getSource();
         try {
            source.setEnabled(false);
            final ParameterInfo[] actualParameterArray = parameterInfoPanel.getParameterInfoArray();
            final AnalysisService _selectedService = selectedService;
            final String username = AnalysisServiceManager.getInstance().getUsername();
            
               new Thread() {
                  public void run() {
                     RunTask rt = new RunTask(_selectedService, actualParameterArray,
                           username);
                     rt.exec();
                  }
               }.start();
         } finally {
         	if(TaskLauncher.isVisualizer(selectedService)) {
         		try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
         	}
            source.setEnabled(true);
         }
      }
   }


  
}
