/*
 *  ReporterWithGUI.java
 *
 *  Created on April 3, 2003, 9:43 PM
 */
package org.genepattern.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 *  Displays the messages and records them to the Error and Warning consoles
 *
 *@author     kohm
 *
 */
public final class ReporterWithGUI extends AbstractReporter {
    
	protected Component dialogParent = null;

	public void setDialogParent(Component obj){
		dialogParent = obj;
	}


    /**
     *  Creates a new instance of ReporterNoGUI
     */
    protected ReporterWithGUI() { }
    public static void main(String[] args) {
        ReporterWithGUI  r = new ReporterWithGUI();
        r.showError("My Error", "test error message", new Exception("Bogus!!!"));
        System.exit(0);
    }
    
    /**
     *  shows the error message either via dialog if operating as gui or to file or
     *  standard out if no gui.
     *
     *@param  message  Description of the Parameter
     */
    public void showError(final String message) {
        showError(null, message, null);
    }
    
    
    /**
     *  shows the error message either via dialog if operating as gui or to file or
     *  standard out if no gui.
     *
     *@param  message  Description of the Parameter
     *@param  th       Description of the Parameter
     */
    public synchronized void showError(final String message, final Throwable th) {
        showError(null, message, th);
    }
    
    
    /**
     *  shows the error message either via dialog if operating as gui or to file or
     *  standard out if no gui.
     *
     *@param  title    Description of the Parameter
     *@param  message  Description of the Parameter
     *@param  th       Description of the Parameter
     */
    public synchronized void showError(String title, String message, final Throwable th) {
        if(message == null && th == null) {
            return;
        }
        if(th != null) {
            errors.append(getStackTraceback(th));
            errors.append('\n');
            
            if(message != null) {
                ;
                //message = message + '\n' + th + '\n' + error_writer;
            } else {
                message = th.getMessage();
                // + '\n' + error_writer;
            }
            
        }
       if(title == null || title.trim().length() == 0) {
            title = "Error: ";
        }
        title = "Error";
		  if(th!=null) {
			  message = "An error of type " + th.getClass().getName() + " occurred.";
		  } 
       
        createDialog(title, message, getStackTraceback(th), JOptionPane.ERROR_MESSAGE);
        
        if(th != null) {
            // clear the StringWriter
            error_writer.getBuffer().setLength(0);
        }
        // log it
        errors.append(message);
        errors.append('\n');
    }
    
    
    /**
     *  shows the warning message optionally via dialog if operating as gui or to
     *  file or standard out if no gui.
     *
     *@param  message  Description of the Parameter
     */
    public void showWarning(final String message) {
        showWarning(null, message, null);
    }
    
    
    /**
     *  shows the warning message optionally via dialog if operating as gui or to
     *  file or standard out if no gui.
     *
     *@param  message  Description of the Parameter
     *@param  w        Description of the Parameter
     */
    public void showWarning(final String message, final Exception w) {
        showWarning(null, message, w);
    }
    
    
    /**
     *  shows the warning message optionally via dialog if operating as gui or to
     *  file or standard out if no gui.
     *
     *@param  title    Description of the Parameter
     *@param  message  Description of the Parameter
     *@param  w        Description of the Parameter
     */
    public void showWarning(String title, String message, final Exception w) {
        if((message == null || message.trim().length() == 0) && w == null) {
            return;
        }
        if(message == null) {
            message = w.getMessage();
            // + '\n' + getStackTraceback(w);
        } else if(w != null) {
            message = message + '\n' + w.getMessage();
        }
        
        //final JScrollPane pane = createMessagePane(message + '\n' + getStackTraceback(w));
        if(title == null || title.trim().length() == 0) {
            title = "Warning: ";
        }
        createDialog(title, message, getStackTraceback(w), JOptionPane.WARNING_MESSAGE);
        logWarning(message);
    }
    /** creates the dialog */
    protected void createDialog(final String title, final String message, final String trace, final int type) {
        final MessagePane pane = new MessagePane(message, trace);
        JOptionPane.showMessageDialog(dialogParent, pane, title, type);
    }
    
    /**
     *  logs the message
     *
     *@param  message  Description of the Parameter
     */
    public void logWarning(final String message) {
        //System.err.println("Warning Log:   "+message);
        warnings.append(message);
        warnings.append('\n');
        warnings.append('\n');
    }
    
    
    /**
     *  logs the message
     *
     *@param  message  Description of the Parameter
     *@param  w        Description of the Parameter
     */
    public void logWarning(final String message, final Exception w) {
        logWarning(message + '\n' + getStackTraceback(w));
    }
    
    
    /**
     *  gets the warnings buffer FIXME this should be transmitting change events to
     *  listeners
     *
     *@return    The warnings value
     */
    public StringBuffer getWarnings() {
        return warnings;
    }
    
    
    /**
     *  gets the errors buffer FIXME this should be transmitting change events to
     *  listeners
     *
     *@return    The errors value
     */
    public StringBuffer getErrors() {
        return errors;
    }
    
    
    // helpers
    /**
     *  gets the traceback as a string
     *
     *@param  th  Description of the Parameter
     *@return     The stackTraceback value
     */
    protected final String getStackTraceback(final Throwable th) {
        if(th == null) {
            return "";
        }
        if(error_writer == null) {
            error_writer = new java.io.StringWriter();
            error_printer = new java.io.PrintWriter(error_writer);
        } else {
            // clear the StringWriter
            error_writer.getBuffer().setLength(0);
        }
        
        th.printStackTrace(error_printer);
        // write to StringWriter through PrintWriter
        return error_writer.toString();
    }
    
    
    /**
     *  creates the text area that is not editable and is containted within a
     *  scroll pane with a certain size
     *
     *@param  message  Description of the Parameter
     *@return          Description of the Return Value
     */
    protected JScrollPane createMessagePane(final String message) {
        final JTextArea text_area = new JTextArea(message);
        text_area.setEditable(false);
        System.out.println("rows=" + text_area.getLineCount());
        System.out.println("cols=" + StringUtils.getMaxLineCount(message));
        final int rows = Math.min(4, text_area.getLineCount());
        final int cols = Math.min(80, StringUtils.getMaxLineCount(message));
        text_area.setRows(rows);
        text_area.setColumns(cols);
        final JScrollPane pane = new JScrollPane(text_area);
        //pane.setPreferredSize(new java.awt.Dimension(500, 200));
        return pane;
    }
    
    
    //    /** test it */
    
      
    // fields
    /**
     *  where the warnings are kept
     */
    private final StringBuffer warnings = new StringBuffer();
    /**
     *  where the errors are kept
     */
    private final StringBuffer errors = new StringBuffer();
    /**
     *  the writer to the StringBuffer
     */
    private java.io.StringWriter error_writer;
    /**
     */
    private java.io.PrintWriter error_printer;
    
    
    
    private static class MessagePane extends JPanel {
        JScrollPane stackTraceScrollPane;
        //                JButton[] buttons = new JButton[2];
        
        public MessagePane(String message, String stackTrace) {
           
            final JButton detailsButton = new JButton("Details");
            detailsButton.addActionListener(
            new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    MessagePane.this.remove(detailsButton);
                    detailsButton.setVisible(false);
                    showStackTrace();
                }
            });
            
            JPanel buttonPanel = new JPanel(new FlowLayout());
				buttonPanel.add(detailsButton);
            
            
            JPanel p1 = new JPanel(new BorderLayout());
            p1.add(createMessagePane(message), BorderLayout.NORTH);
				boolean detailsAvailable = true;
				if(stackTrace == null || stackTrace.trim().equals("")) {
					detailsAvailable = false;
            } else {
					p1.add(buttonPanel, BorderLayout.CENTER);	
				}
           
            stackTraceScrollPane = createMessagePane(stackTrace);
           
            setLayout(new BorderLayout());
            add(p1, BorderLayout.NORTH);
				if(detailsAvailable) {
					add(stackTraceScrollPane, BorderLayout.CENTER);
					stackTraceScrollPane.setVisible(false);
				}
                      
        }
        
              
        private void showStackTrace() {
            stackTraceScrollPane.setVisible(true);
            stackTraceScrollPane.invalidate();
            stackTraceScrollPane.validate();
            invalidate();
            validate();
            javax.swing.SwingUtilities.windowForComponent(this).pack();
            
        }
        
        
        protected JScrollPane createMessagePane(final String message) {
            final JTextArea text_area = new JTextArea(message, 4, 60);
            text_area.setEditable(false);
            final JScrollPane pane = new JScrollPane(text_area);
            pane.setMaximumSize(new java.awt.Dimension(500, 100));
            pane.setPreferredSize(new java.awt.Dimension(500, 100));
            return pane;
        }
    }
    
}

