package org.genepattern.gpge.ui.tasks;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.log4j.Category;
import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.ui.maindisplay.DataObjectBrowser;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.RequestHandler;
import org.genepattern.webservice.RequestHandlerFactory;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;
import org.genepattern.gpge.ui.tasks.*;
import org.genepattern.gpge.ui.maindisplay.MainFrame;
/**
 *  <p>
 *
 *  Description: creates UI dynamically for different kind of analysis web
 *  services tasks</p>
 *
 * @author     Hui Gong
 * @created    April 9, 2004
 * @version    $Revision$
 */

public class AnalysisServicePanel extends JPanel {
  
  AnalysisServiceManager serviceManager;
   
   JLabel tasksLabel;
   Map lsid2VersionsMap;

   JPanel versionPanel = new JPanel();
   /**  the latest version for the current task */
   String latestVersion;

   /**  the analysis task panel */
   private JPanel _servicePanel;

   /**  contains the mappings of parameter names to ParamRetrievor instances */
   private final Map name_retriever = new HashMap();
   /**  the list of RendererFactory objects */
   private final java.util.List renderer_factories = new ArrayList();

   private int _id;
   private AnalysisService _selectedService;
   /**  the error handler if one was supplied or null */
   private final OVExceptionHandler exception_handler;

   private static Category cat = Category.getInstance(AnalysisServicePanel.class.getName());


   /**
    *  Constructs a new AnalysisServicePanel with a wrapper for a type of list
    *  instead of a JComboBox for the analysis tasks
    *
    * @param  exception_handler  Description of the Parameter
    * @param  serviceManager.getUsername()           Description of the Parameter
    */
   public AnalysisServicePanel(final OVExceptionHandler exception_handler, AnalysisServiceManager serviceManager) {
      this.serviceManager = serviceManager;
      this.exception_handler = exception_handler;

      tasksLabel = new JLabel();
   
      _servicePanel = new JPanel();//createTaskPane(_selectedService);
      this.setLayout(new BorderLayout());
      JPanel listPanel = new JPanel();
      listPanel.setLayout(new BorderLayout());
      listPanel.add(tasksLabel, BorderLayout.CENTER);
      listPanel.add(versionPanel, BorderLayout.EAST);

      if(!MainFrame.RUNNING_ON_MAC) {
         listPanel.setBackground(java.awt.Color.white);
         versionPanel.setBackground(java.awt.Color.white);
      }
     
      final RendererFactory factory =
         new RendererFactory() {
         
            public UIRenderer createRenderer(final AnalysisService service, java.util.List params) {
               return new org.genepattern.gpge.ui.maindisplay.DefaultUIRenderer();
            }
         };
      addRendererFactory(factory);

      this.add(listPanel, BorderLayout.NORTH);
      this.add(_servicePanel, BorderLayout.CENTER);
      try {
         this.lsid2VersionsMap = new org.genepattern.webservice.AdminProxy(serviceManager.getServer(), serviceManager.getUsername()).getLSIDToVersionsMap();
      } catch(Throwable t) {
         t.printStackTrace();
      }
   }


   public void loadTask(AnalysisService selectedService) {
      latestVersion = null;
      TaskInfo taskInfo = selectedService.getTaskInfo();
      String taskDisplay = taskInfo.getName();

      try {
         LSID lsid = new LSID((String) selectedService.getTaskInfo().getTaskInfoAttributes().get(GPConstants.LSID));
         if(!org.genepattern.gpge.ui.maindisplay.LSIDUtil.isBroadTask(lsid)) {
            String authority = lsid.getAuthority();
            taskDisplay += " (" + authority + ")";
         }
         final String lsidNoVersion = lsid.toStringNoVersion();
         List versions = (List) lsid2VersionsMap.get(lsidNoVersion);
         String currentVersion = lsid.getVersion();
         latestVersion = currentVersion;
         for(int i = 0; i < versions.size(); i++) {
            String version = (String) versions.get(i);
            if(version.compareTo(latestVersion) > 0) {
               latestVersion = version;
            }
         }

         if(lsid.getVersion().equals(latestVersion)) {
            taskDisplay += ", " + lsid.getVersion() + " (latest)";
         } else {
            taskDisplay += ", " + lsid.getVersion();
         }

      } catch(Exception e) {}


      tasksLabel.setText(taskDisplay);
      this._selectedService = selectedService;
      this.remove(_servicePanel);
      this._servicePanel = createTaskPane(selectedService);
      this.add(this._servicePanel, BorderLayout.CENTER);
      this.revalidate();
      this.doLayout();
   }



   /**
    *  add additional RendererFactory objects last ones added are the first ones
    *  tried
    *
    * @param  factory  The feature to be added to the RendererFactory attribute
    */
   public void addRendererFactory(final RendererFactory factory) {
      this.renderer_factories.add(factory);
   }


   /**
    *  fires an error message
    *
    * @param  title    Description of the Parameter
    * @param  message  Description of the Parameter
    * @param  ex       Description of the Parameter
    */
   protected void fireError(String title, String message, final Throwable ex) {
      if(exception_handler == null) {
         if(title == null) {
            title = "Error: ";
         }
         if(message == null) {
            message = "";
         }
         if(ex != null) {
            ex.printStackTrace();
            message = message + '\n' + ex.getMessage();
         }
         System.err.println(message);
         JOptionPane.showMessageDialog(AnalysisServicePanel.this, message, title, JOptionPane.ERROR_MESSAGE);
      } else {
         exception_handler.setError(title, message, ex);
      }
   }
   
 

   /**
    *  fires a warning message
    *
    * @param  title    Description of the Parameter
    * @param  message  Description of the Parameter
    * @param  ex       Description of the Parameter
    */
   protected void fireWarning(String title, String message, final Exception ex) {
      if(exception_handler == null) {
         if(title == null) {
            title = "Error: ";
         }
         if(message == null) {
            message = "";
         }
         if(ex != null) {
            ex.printStackTrace();
            message = message + '\n' + ex.getMessage();
         }
         JOptionPane.showMessageDialog(AnalysisServicePanel.this, message, title, JOptionPane.WARNING_MESSAGE);
      } else {
         exception_handler.setWarning(title, message, ex);
      }
   }


   /**
    *  adds a new
    *
    * @param  service  Description of the Parameter
    * @return          Description of the Return Value
    */
   private JPanel createTaskPane(final AnalysisService service) {
      versionPanel.removeAll();
      String lsidString = (String) service.getTaskInfo().getTaskInfoAttributes().get(GPConstants.LSID);
      if(lsidString != null) {
         try {
            final LSID lsid = new LSID(lsidString);
            final String lsidNoVersion = lsid.toStringNoVersion();
            List versions = (List) lsid2VersionsMap.get(lsidNoVersion);
            Vector versionsCopy = new Vector();
            for(int i = 0; i < versions.size(); i++) {
               String version = (String) versions.get(i);

               if(version.equals(latestVersion)) {
                  version += " (latest)";
               }
               versionsCopy.add(version);
            }
            Collections.sort(versionsCopy, String.CASE_INSENSITIVE_ORDER);

            if(versionsCopy.size() > 1) {
               versionPanel.add(new JLabel("Version:"));
               final JComboBox cb = new JComboBox(versionsCopy);
               if(!MainFrame.RUNNING_ON_MAC) {
                  cb.setBackground(java.awt.Color.white);
               }
               if(lsid.getVersion().equals(latestVersion)) {
                  cb.setSelectedItem(lsid.getVersion() + " (latest)");
               } else {
                  cb.setSelectedItem(lsid.getVersion());
               }

               cb.addActionListener(
                  new ActionListener() {
                     public void actionPerformed(ActionEvent e) {
                        String selectedItem = (String) cb.getSelectedItem();
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
                        String selectedLSID = lsidNoVersion + ":" + selectedItem;
                        AnalysisService svc = serviceManager.getAnalysisService(selectedLSID);
                        if(svc == null) {
                           JOptionPane.showMessageDialog(GenePattern.getDialogParent(), "The task was not found.");
                        } else {
                           loadTask(svc);
                        }
                     }
                  });
               versionPanel.add(cb);
            }
         } catch(Exception e) {}
      }

      this.name_retriever.clear();
      // contains the top_pane, JScrollPane(pane), bottom_pane
      final JPanel main_pane = new JPanel(new BorderLayout());

      final JPanel pane = new JPanel(new GridBagLayout());// middle

      if(!MainFrame.RUNNING_ON_MAC) {
         pane.setBackground(java.awt.Color.white);
      }

      main_pane.add(new JScrollPane(pane), BorderLayout.CENTER);
      final JPanel top_pane = new JPanel();// FlowLayout

      if(!MainFrame.RUNNING_ON_MAC) {
         top_pane.setBackground(java.awt.Color.white);
      }

      ((FlowLayout) top_pane.getLayout()).setAlignment(FlowLayout.LEADING);
      main_pane.add(top_pane, BorderLayout.NORTH);
      final JPanel bottom_pane = new JPanel(new BorderLayout());
      main_pane.add(bottom_pane, BorderLayout.SOUTH);

      final TaskInfo task = service.getTaskInfo();
      // last renderer will get the chance to create the task panel header label
      // and create the submit button
      UIRenderer last_renderer = null;

      if(task != null && task.getParameterInfoArray() != null && task.getParameterInfoArray().length > 0) {
         _id = task.getID();

         //get parameters
         //final ParameterInfo[] params = task.getParameterInfoArray();
         final ParameterInfo[] original_params = task.getParameterInfoArray();
         final int num_params = original_params.length;
         //final int num_params = params.length;
         if(num_params == 0) {
            bottom_pane.add(getErrorLabel("Error: no parameters!"));
            //return scrollPane;
            return main_pane;
         }
         // clone the param infos since they are mutable objects
         final ParameterInfo[] params = new ParameterInfo[num_params];
         for(int i = 0; i < num_params; i++) {
            final ParameterInfo old = original_params[i];
            final ParameterInfo neu = new ParameterInfo(old.getName(), old.getValue(), old.getDescription());
            final HashMap attrs = old.getAttributes();
            if(attrs != null) {
               neu.setAttributes((HashMap) attrs.clone());
            }
            params[i] = neu;
         }
         // end cloning of ParameterInfo objs

         //loop through the parameters to create UI
         final java.util.List param_list = new ArrayList(java.util.Arrays.asList(params));
         // get the last added RendererFactory objects first
         final int num_factories = renderer_factories.size();

         for(int i = num_factories - 1; i >= 0 && param_list.size() > 0; i--) {// rev. loop
            final RendererFactory factory = (RendererFactory) renderer_factories.get(i);
            final UIRenderer renderer = factory.createRenderer(service, param_list);
            if(renderer != null) {
               last_renderer = renderer;
               renderer.render(pane, service, param_list, name_retriever);
            }
         }
         final int num_left_over = param_list.size();
         if(num_left_over > 0) {
            if(num_left_over == num_params) {
               bottom_pane.add(getErrorLabel("Error: none of the " + num_params + " were processed and displayed!"));
               return main_pane;
            } else {
               bottom_pane.add(getWarningLabel("Warning: " + num_left_over
                      + " parameters could not be processed and are missing from the display!"),
                     BorderLayout.SOUTH);
            }

         }

         //add bottom panel for submit button
         if(last_renderer != null) {
            top_pane.add(last_renderer.createTaskLabel(service));
            //((FlowLayout)bottom_pane.getLayout()).setAlignment(FlowLayout.LEADING);
            bottom_pane.add(last_renderer.createSubmitPanel(service, new SubmitActionListener(), new ResetActionListener()),
                  BorderLayout.CENTER);
         }
      } else {
         bottom_pane.add(getWarningLabel("Note: This module has no input parameters."),
               BorderLayout.SOUTH);
         //add bottom panel for submit button
         //((FlowLayout)bottom_pane.getLayout()).setAlignment(FlowLayout.LEADING);
         bottom_pane.add(createSubmitPanel(), BorderLayout.CENTER);
      }

      //return scrollPane;
      return main_pane;
   }
   //private JPanel createLabelPanel(final String desc, final GridBagConstraints gbc, final int gridy){

   private static JComponent createLabelPanel(final String desc) {
      JLabel nameLabel = new JLabel(desc + ":  ");
      return nameLabel;
      //JPanel labelPane = new JPanel();
      //labelPane.add(nameLabel);
      //setGridBagConstraints(gbc, 0, gridy, 1, 1, 100, 1);
      //return labelPane;
   }
   //private JPanel createSubmitPanel(final GridBagConstraints gbc, final int gridy){

   private JComponent createSubmitPanel() {
      JButton submit = new JButton("Submit");
      submit.addActionListener(new SubmitActionListener());
      return submit;
   }


   protected static void setGridBagConstraints(final GridBagConstraints gbc, final int gridx, final int gridy, final int gridw, final int gridh, final int weightx, final int weighty) {
      gbc.gridx = gridx;
      gbc.gridy = gridy;
      gbc.gridwidth = gridw;
      gbc.gridheight = gridh;
      gbc.weightx = weightx;
      gbc.weighty = weighty;
   }


   /**
    *  adds the error message to the container
    *
    * @param  message  Description of the Parameter
    * @return          The errorLabel
    */
   //private void addErrorMessage(final String message, final JComponent pane) {
   private JComponent getErrorLabel(final String message) {
      final JLabel label = new JLabel(message);
      label.setForeground(Color.red);
      return label;
   }


   /**
    *  adds the error message to the container
    *
    * @param  message  Description of the Parameter
    * @return          The warningLabel
    */
   //private void addErrorMessage(final String message, final JComponent pane) {
   private JComponent getWarningLabel(final String message) {
      final JLabel label = new JLabel(message);
      label.setForeground(Color.magenta);
      return label;
   }
   

 


   private class ResetActionListener implements ActionListener {
      public final void actionPerformed(ActionEvent ae) {
         loadTask(_selectedService);
      }
   }


   /**
    *  handles getting the valuesfrom all the input components
    *
    * @author     jgould
    * @created    February 9, 2004
    */
   private class SubmitActionListener implements ActionListener {
      public final void actionPerformed(ActionEvent ae) {
         final JButton source = (JButton) ae.getSource();
         source.setEnabled(false);
         final Map nameRetriever = name_retriever;
         final AnalysisService selectedService = _selectedService;
            new Thread() {
               public final void run() {
                  try {
                     submit(nameRetriever, selectedService);
                  } catch(Throwable th) {
                     exception_handler.setError("Error:", "While submitting " + _selectedService.toString() + ':', th);
                  } finally {
                     if(source != null && source.isDisplayable()) {
                        source.setEnabled(true);
                     }
                  }
               }
            }.start();
      }


      private void submit(Map nameRetriever, AnalysisService _selectedService) {
         final int size = name_retriever.size();
         //final ParameterInfo[] parmInfos = new ParameterInfo[size];
         final List param_list = new ArrayList(size);
         String param_name = "";
         
         try {
            //int i = 0;
            final ParameterInfo[] old_params = _selectedService.getTaskInfo().getParameterInfoArray();
            final int old_parm_cnt = ((old_params != null) ? old_params.length : 0);
            for(int i = 0, j = 0; i < old_parm_cnt; i++) {
               //final String name = (String)iter.next();
               final String name = old_params[i].getName();
               
               param_name = name;
               final ParamRetrievor rtrvr = (ParamRetrievor) name_retriever.get(name);
               if(rtrvr == null) {// skip those not processed
                  
                  continue;
               }
               final boolean is_file = rtrvr.isFile();

               final ParameterInfo param_copy = rtrvr.getParameterInfo();
               if(is_file) {
                  param_copy.getAttributes().put(GPConstants.PARAM_INFO_CLIENT_FILENAME[0], rtrvr.getSourceName());
               }
               final String contents = (is_file) ? rtrvr.getSourceName() : rtrvr.getValue();

               final HashMap old_attrs = param_copy.getAttributes();
               if(contents == null || contents.length() == 0) {
                  final Object optional = old_attrs.get(GPConstants.PARAM_INFO_OPTIONAL[0]);
                 
                  if(optional != null && optional.toString().length() > 0) {
                     
                     continue;
                  } else {
                     
                     fireWarning("Input Error: ", "Required parameter left blank: " + name, null);
                     return;
                  }
               }
               param_copy.setValue(contents);
               param_copy.setDescription((is_file) ? "input file" : "Job");
               
               param_list.add(param_copy);
               j++;

            }
         } catch(IOException ioe) {
            cat.debug("Error in reading value(" + param_name + "). " + ioe.getMessage());
            fireError("Error: ", "While getting the input data for " + param_name + ".", ioe);
            return;
         }
         final int num_params = param_list.size();
         System.out.println(param_list);
         final ParameterInfo[] parmInfos = (ParameterInfo[]) param_list.toArray(new ParameterInfo[num_params]);
         RunTask rt = new RunTask(_selectedService, parmInfos, serviceManager.getUsername());
         rt.exec();
      }

   }

}
