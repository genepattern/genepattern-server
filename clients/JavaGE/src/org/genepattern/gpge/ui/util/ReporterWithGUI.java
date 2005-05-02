/*
    ReporterWithGUI.java
    Created on April 3, 2003, 9:43 PM
  */
package org.genepattern.gpge.ui.util;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 *  Displays the messages and records them to the Error and Warning consoles
 *
 * @author    kohm
 */
public final class ReporterWithGUI {

   protected Frame dialogParent = null;
   /**  if reporting is turned on */
   protected final static boolean VERBOSE = true;
   // fields

   /**  where the errors are kept */
   private final StringBuffer errors = new StringBuffer();

   /**  the writer to the StringBuffer */
   private java.io.StringWriter error_writer;

   /** */
   private java.io.PrintWriter error_printer;


   /**
    *  Creates a new instance of ReporterNoGUI
    *
    * @param  parent  Description of the Parameter
    */
   public ReporterWithGUI(Frame parent) {
      this.dialogParent = parent;
   }



   /**
    *  shows the error message either via dialog if operating as gui or to file
    *  or standard out if no gui.
    *
    * @param  title    Description of the Parameter
    * @param  message  Description of the Parameter
    */
   public synchronized void showError(String title, String message) {
      if(message == null) {
         return;
      }

      if(title == null || title.trim().length() == 0) {
         title = "Error: ";
      }

      createDialog(title, message);

      // log it
      errors.append(message);
      errors.append('\n');
   }



   /**
    *  creates the dialog
    *
    * @param  title    Description of the Parameter
    * @param  message  Description of the Parameter
    */
   protected void createDialog(final String title, final String message) {
      JScrollPane pane = createMessagePane(message);
      final javax.swing.JDialog dialog = new org.genepattern.gpge.ui.maindisplay.CenteredDialog(dialogParent);
      dialog.setTitle(title);
      dialog.getContentPane().add(pane);
      JPanel buttonPanel = new JPanel();
      
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.setVisible(false);
            dialog.dispose();
			}
		});
		buttonPanel.add(closeButton);
      dialog.getRootPane().setDefaultButton(closeButton);
		dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
      dialog.setSize(400, 200);
      dialog.setVisible(true);
   }


   /**
    *  gets the errors buffer FIXME this should be transmitting change events
    *  to listeners
    *
    * @return    The errors value
    */
   public StringBuffer getErrors() {
      return errors;
   }

   


   /**
    *  creates the text area that is not editable and is containted within a
    *  scroll pane with a certain size
    *
    * @param  message  Description of the Parameter
    * @return          Description of the Return Value
    */
   protected JScrollPane createMessagePane(final String message) {
      final JTextArea text_area = new JTextArea(message);

      text_area.setLineWrap(true);
      text_area.setEditable(false);
      final JScrollPane pane = new JScrollPane(text_area);

      //pane.setPreferredSize(new java.awt.Dimension(500, 200));
      return pane;
   }



}

