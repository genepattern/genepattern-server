package org.genepattern.gpge.ui.tasks;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.ui.graphics.draggable.ObjectTextField;
import org.genepattern.gpge.ui.maindisplay.MainFrame;
import org.genepattern.gpge.ui.project.ProjectDirModel;
import org.genepattern.util.*;
import org.genepattern.modules.ui.graphics.*;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;
/**
 *  Displays an <tt>AnalysisService</tt>
 *
 * @author    Joshua Gould
 */
public class AnalysisServiceDisplay extends JPanel {
   private AnalysisService selectedService;
   private String latestVersion;
   private javax.swing.event.EventListenerList listenerList = new javax.swing.event.EventListenerList();
   private Map parameterName2ComponentMap;
   private List inputFileParameterNames;

   public AnalysisServiceDisplay() {
      parameterName2ComponentMap = new HashMap();
      inputFileParameterNames = new ArrayList();
      javax.swing.Icon icon = new javax.swing.ImageIcon(ClassLoader
				.getSystemResource("org/genepattern/gpge/resources/intro.gif"));
		 add(new JLabel(icon));
       if(!MainFrame.RUNNING_ON_MAC) {
         this.setBackground(Color.white);
      } 
   }



   public void addAnalysisServiceSelectionListener(
         AnalysisServiceSelectionListener l) {
      listenerList.add(AnalysisServiceSelectionListener.class, l);
   }


   public void removeAnalysisServiceSelectionListener(
         AnalysisServiceSelectionListener l) {
      listenerList.remove(AnalysisServiceSelectionListener.class, l);
   }

   private JPanel createJPanel() {
      JPanel p = new JPanel();
      if(!MainFrame.RUNNING_ON_MAC) {
         p.setBackground(Color.white);
      }
      return p;
   }
   
    private JPanel createJPanel(java.awt.LayoutManager l) {
      JPanel p = new JPanel(l);
      if(!MainFrame.RUNNING_ON_MAC) {
         p.setBackground(Color.white);
      }
      return p;
   }

   /**
    *  Displays the given analysis service
    *
    * @param  selectedService  Description of the Parameter
    */
   public void loadTask(AnalysisService selectedService) {
      this.selectedService = selectedService;
      parameterName2ComponentMap.clear();
      inputFileParameterNames.clear();
      latestVersion = null;
      TaskInfo taskInfo = selectedService.getTaskInfo();
      String taskDisplay = taskInfo.getName();
      JComboBox versionComboBox = null;
      try {
         final LSID lsid = new LSID((String) selectedService.getTaskInfo()
               .getTaskInfoAttributes().get(GPConstants.LSID));
         if(!org.genepattern.gpge.ui.maindisplay.LSIDUtil.isBroadTask(lsid)) {
            String authority = lsid.getAuthority();
            taskDisplay += " (" + authority + ")";
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
            taskDisplay += ", version " + lsid.getVersion() + " (latest)";
         } else {
            taskDisplay += ", version " + lsid.getVersion();
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
      JPanel parameterPanel = createJPanel();
      parameterPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
     
      if(params == null || params.length == 0) {
         parameterPanel.add(new JLabel(selectedService.getTaskInfo().getName() + " has no input parameters"));
      } else {

         StringBuffer rowSpec = new StringBuffer();
         for(int i = 0; i < params.length; i++) {
            if(i > 0) {
               rowSpec.append(", ");
            }
            rowSpec.append("pref, pref, 12px");// input, description space
         }
         // input label, space, input field
         //                     description
         FormLayout formLayout = new FormLayout(
               "right:pref:none, 6px, left:default:grow",
               rowSpec.toString());
         parameterPanel.setLayout(formLayout);
         CellConstraints cc = new CellConstraints();
        
         for(int i = 0; i < params.length; i++) {
            cc.gridWidth = 1;
            ParameterInfo info = params[i];
            final String value = info.getValue();
            Component input = null;
            if(value == null || value.equals("")) {
               if(info.isInputFile()) {
                  input = createInputFileField(info);
               } else {
                  input = createTextInput(info);
               }
            } else {
               input = createComboBox(info);
            }
            int row = i * 3 + 1;
            JLabel inputLabel = new JLabel(getDisplayString(info) + ":");
            if(!isOptional(params[i])) {
               inputLabel.setFont(inputLabel.getFont().deriveFont(java.awt.Font.BOLD));
            }
            parameterPanel.add(inputLabel, cc.xy(1, row));
            JLabel description = new JLabel(info.getDescription());
            parameterPanel.add(input, cc.xy(3, row));
            cc.hAlign = CellConstraints.FILL;
            parameterPanel.add(description, cc.xy(3, row + 1));
            parameterName2ComponentMap.put(info.getName(), input);
         }
      }

      removeAll();

      Component title = new JLabel(taskDisplay);
      Component description = createWrappedLabel(selectedService.getTaskInfo().getDescription());
      
      JPanel topPanel = null;
      
      if(versionComboBox != null) {
         JPanel temp = createJPanel(new FormLayout("left:pref:none, 12px, left:pref:none, 6px, left:pref:none", "pref, 12px"));
         if(!MainFrame.RUNNING_ON_MAC) {
            temp.setBackground(Color.white);
         }
         CellConstraints cc = new CellConstraints();
         JLabel versionLabel = new JLabel("Choose Version:");
         temp.add(title, cc.xy(1, 1));
         temp.add(versionLabel, cc.xy(3, 1));
         temp.add(versionComboBox, cc.xy(5, 1));
         topPanel = createJPanel(new BorderLayout());
         topPanel.add(temp, BorderLayout.NORTH);
         topPanel.add(description, BorderLayout.SOUTH);
      } else {
         CellConstraints cc = new CellConstraints();
         JPanel temp = createJPanel(new FormLayout("left:pref:none", "pref, 12px"));
         if(!MainFrame.RUNNING_ON_MAC) {
            temp.setBackground(Color.white);
         }
         temp.add(title, cc.xy(1, 1));
         topPanel = createJPanel(new BorderLayout());
         topPanel.add(temp, BorderLayout.NORTH);
         topPanel.add(description, BorderLayout.SOUTH);
      }
      topPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
      
      setLayout(new BorderLayout());
      JPanel buttonPanel = createJPanel();
      JButton submitButton = new JButton("Run");
      submitButton.addActionListener(new SubmitActionListener());
     // getRootPane().setDefaultButton(submitButton);
      buttonPanel.add(submitButton);
      JButton resetButton = new JButton("Reset");
      resetButton.addActionListener(new ResetActionListener());
      buttonPanel.add(resetButton);
      JButton helpButton = new JButton("Help");
      helpButton.addActionListener(new HelpActionListener());
      buttonPanel.add(helpButton);
      add(topPanel, BorderLayout.NORTH);
      add(new JScrollPane(parameterPanel), BorderLayout.CENTER);
      add(buttonPanel, BorderLayout.SOUTH);
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


   protected final JTextField createProperTextField(final ParameterInfo info) {
      final int num_cols = 15;
      JTextField field = null;
      final Object value = info.getAttributes().get("type");
      if(value == null || value.equals("java.lang.String")) {
         field = new JTextField(num_cols);
      } else if(value.equals("java.lang.Integer")) {
         field = new IntegerField(num_cols);
      } else if(value.equals("java.lang.Float")) {
         field = new FloatField(num_cols);
      } else {
         field = new JTextField(num_cols);
         System.err.println("Unknown type");
      }
      return field;
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


   private Component createComboBox(ParameterInfo info) {
      // get default
      final String default_val = ((String) info.getAttributes().get(
            GPConstants.PARAM_INFO_DEFAULT_VALUE[0])).trim();
      final ChoiceItem default_item = createDefaultChoice(default_val);
      final StringTokenizer tokenizer = new StringTokenizer(info.getValue(), ";");
      final JComboBox list = new JComboBox();
      list.setFont(getFont());
      int selectIndex = -1;
      for(int i = 0; tokenizer.hasMoreTokens(); i++) {
         final String token = tokenizer.nextToken();
         final ChoiceItem item = createChoiceItem(token);
         list.addItem(item);
         if(selectIndex < 0 && item.hasToken(default_item)) {
            selectIndex = i;
         }
      }

      if(selectIndex >= 0) {
         list.setSelectedIndex(selectIndex);
      } else {
         list.setSelectedIndex(0);
      }
      return list;
   }


   private ObjectTextField createObjectTextField() {
      final ObjectTextField field = new ObjectTextField(null, 20);
      if(!MainFrame.RUNNING_ON_MAC) {
         field.setBackground(Color.white);
      }
      field.setFont(getFont());
      return field;
   }


   private Component createInputFileField(ParameterInfo info) {
      ObjectTextField field = createObjectTextField();
      inputFileParameterNames.add(info.getName());
      parameterName2ComponentMap.put(info.getName(), field);
      String defaultValue = (String) info.getAttributes().get(
            GPConstants.PARAM_INFO_DEFAULT_VALUE[0]);

      if(defaultValue != null && !defaultValue.trim().equals("")) {
         File f = new File(defaultValue);
         if(f.exists()) {
            field.setObject(f);
         } else if(defaultValue.startsWith("http://")
                || defaultValue.startsWith("https://")
                || defaultValue.startsWith("ftp:")) {
            try {
               field.setObject(new URL(defaultValue));
            } catch(MalformedURLException mue) {
               System.err.println("Error setting default value.");
            }
         } else {// for reload of server file
            field.setObject(defaultValue);
         }

      }
      field.setFont(getFont());
      return field;
   }


   /**
    *  Parses the String and returns a ChoiceItem
    *
    * @param  string  Description of the Parameter
    * @return         Description of the Return Value
    */
   private ChoiceItem createChoiceItem(final String string) {
      final int index = string.indexOf('=');
      ChoiceItem choice = null;
      if(index < 0) {
         choice = new ChoiceItem(string, string);
      } else {
         choice = new ChoiceItem(string.substring(index + 1), string.substring(0, index));
      }
      return choice;
   }


   /**
    *  creates the default <CODE>ChoiceItem</CODE> for the combo box
    *
    * @param  default_val  Description of the Parameter
    * @return              Description of the Return Value
    */
   private ChoiceItem createDefaultChoice(final String default_val) {
      if(default_val != null && default_val.length() > 0) {
         return createChoiceItem(default_val);
      }
      return null;
   }


   /**
    *  creates a JTextField
    *
    * @param  info  Description of the Parameter
    * @return       Description of the Return Value
    */
   private Component createTextInput(ParameterInfo info) {

      //final JTextField field = new JTextField(15);
      final JTextField field = createProperTextField(info);
      // set default
      final String default_val = (String) info.getAttributes().get(
            GPConstants.PARAM_INFO_DEFAULT_VALUE[0]);

      try {
         if(default_val != null && default_val.trim().length() > 0) {
            field.setText(default_val);
         } else {
            // if optional value and no default, clear the field
            if(isOptional(info)) {
               field.setText(null);
            }
         }
      } catch(NumberFormatException ex) {
         ex.printStackTrace();
      }
      field.setFont(getFont());
      return field;
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
         ObjectTextField tf = (ObjectTextField) parameterName2ComponentMap.get(parameterName);
         tf.setObject(node);
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
    *  Gets a sorted collection of input file parameter names
    *
    * @return    the input file names
    */
   public java.util.Iterator getInputFileParameterNames() {
      return inputFileParameterNames.iterator();
   }


   protected final boolean isOptional(final ParameterInfo info) {
      final Object optional = info.getAttributes().get("optional");
      return (optional != null && "on".equalsIgnoreCase(optional.toString()));
   }


   private final static String getValue(ParameterInfo info, ObjectTextField field)
          throws java.io.IOException {
      final Object obj = field.getObject();
      if(obj == null) {
         return null;
      }

      if(obj instanceof String) {// reloaded job where input file is
         // on server
         info.getAttributes().put(ParameterInfo.TYPE,
               ParameterInfo.FILE_TYPE);

         info.getAttributes().put(ParameterInfo.MODE,
               ParameterInfo.CACHED_INPUT_MODE);
         return obj.toString();
      }
      if(obj instanceof org.genepattern.gpge.ui.project.ProjectDirModel.FileNode) {
         info.setAsInputFile();
         ProjectDirModel.FileNode node = (ProjectDirModel.FileNode) obj;
         return node.file.getCanonicalPath();
      } else if(obj instanceof   org.genepattern.gpge.ui.tasks.JobModel.ServerFileNode) {
         info.getAttributes().put(ParameterInfo.TYPE,
               ParameterInfo.FILE_TYPE);
         info.getAttributes().put(ParameterInfo.MODE,
               ParameterInfo.CACHED_INPUT_MODE);
         org.genepattern.gpge.ui.tasks.JobModel.ServerFileNode node = (org.genepattern.gpge.ui.tasks.JobModel.ServerFileNode) obj;
         return node.getParameterValue();
      } else if(obj instanceof java.io.File) {
         info.setAsInputFile();
         final File drop_file = (File) obj;
         return drop_file.getCanonicalPath();
      } else if(obj instanceof java.net.URL) {
         return obj.toString();
      } else {
         throw new IllegalStateException();
      }
   }


   private class ResetActionListener implements ActionListener {
      public final void actionPerformed(ActionEvent ae) {
         loadTask(selectedService);
      }
   }


   private class HelpActionListener implements ActionListener {
      public final void actionPerformed(java.awt.event.ActionEvent ae) {
         try {
            String server = selectedService.getServer();
            String username = AnalysisServiceManager.getInstance().getUsername();
            String docURL = server + "/gp/getTaskDoc.jsp?name="
                   + org.genepattern.gpge.ui.maindisplay.LSIDUtil.getTaskId(selectedService.getTaskInfo()) + "&"
                   + GPConstants.USERID + "="+java.net.URLEncoder.encode(username, "UTF-8");
            org.genepattern.util.BrowserLauncher.openURL(docURL);
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
            List actualParameters = new ArrayList();
            ParameterInfo[] formalParameters = selectedService.getTaskInfo().getParameterInfoArray();
            
            if(formalParameters != null) {
               for(int i = 0; i < formalParameters.length; i++) {
                  Component c = (Component) parameterName2ComponentMap.get(formalParameters[i].getName());
                  String value = null;
                  ParameterInfo actualParameter = new ParameterInfo(formalParameters[i].getName(), "", "");
                  actualParameter.setAttributes(new HashMap(2));
                  boolean isCheckBox = false;
                  if(c instanceof ObjectTextField) {
                     try {
                        
                        value = getValue(actualParameter, (ObjectTextField) c);
                        actualParameter.getAttributes().put(
                           GPConstants.PARAM_INFO_CLIENT_FILENAME[0],
                           value);
                     } catch(java.io.IOException ioe) {
                        ioe.printStackTrace();  
                     }
                  } else if(c instanceof JComboBox) {
                     isCheckBox = true;
                     ChoiceItem ci = (ChoiceItem) ((JComboBox)c).getSelectedItem();
                     value = ci.getValue();
                  } else if(c instanceof JTextField) {
                     value = ((JTextField) c).getText();
                  }
                  if(value != null) {
                     value = value.trim();
                  }
                  actualParameter.setValue(value);
   
                  if(!isCheckBox && formalParameters[i].getAttributes().get(
                        GPConstants.PARAM_INFO_OPTIONAL[0]) != null
                         && (value == null || value.equals(""))) {
                     GenePattern.showErrorDialog(
                           "Missing value for required parameter "
                            + getDisplayString(formalParameters[i]));
                     return;
                  }
                  actualParameters.add(actualParameter);
               }
            }
      
            final ParameterInfo[] actualParameterArray = (ParameterInfo[]) actualParameters.toArray(new ParameterInfo[0]);
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
            source.setEnabled(true);
         }
      }
   }


   private static class ChoiceItem {
      // fields
      /**  the text that represents this */
      private final String text;

      /**  the object that this is a wrapper for */
      private final String value;


      ChoiceItem(final String text, final String value) {
         this.text = text.trim();
         this.value = value;
        
      }


      /**
       *  overrides super... returns the supplied text
       *
       * @return    Description of the Return Value
       */
      public final String toString() {
         return text;
      }


      /**
       *  returns true if the <CODE>ChoiceItem</CODE>'s fields equal either the
       *  value or the text
       *
       * @param  item  Description of the Parameter
       * @return       Description of the Return Value
       */
      protected boolean hasToken(final ChoiceItem item) {
         return (item != null && (text.equalsIgnoreCase(item.text) || item.value.toString().equalsIgnoreCase(value.toString())));
      }


      /**
       *  returns the value (which is not displayed)
       *
       * @return    The value
       */
      public final String getValue() {
         return value;
      }
   }
}
