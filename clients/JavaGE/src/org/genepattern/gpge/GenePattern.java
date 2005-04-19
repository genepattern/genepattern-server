/*
 * GenePattern.java
 *
 * Created on August 22, 2002, 10:09 AM
 */
package org.genepattern.gpge;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JWindow;

import org.genepattern.gpge.ui.infopanels.ReportPanel;
import org.genepattern.gpge.util.BuildProperties;
import org.genepattern.util.AbstractReporter;
import org.genepattern.util.Reporter;
import org.genepattern.util.ReporterWithGUI;
import org.genepattern.util.StringUtils;
import org.genepattern.gpge.ui.maindisplay.MainFrame;
import java.awt.Color;

/**
 * Main program class
 * 
 * @author kohm
 */
public final class GenePattern {
  	static javax.swing.JFrame mainFrame;

	/** Creates a new instance of GenePattern */

	public GenePattern() {
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager
					.getSystemLookAndFeelClassName());
			if (!MainFrame.RUNNING_ON_MAC) {
				javax.swing.UIDefaults uiDefaults = javax.swing.UIManager
						.getDefaults();
				/*uiDefaults.put("Panel.background",
						new javax.swing.plaf.ColorUIResource(Color.white));
				uiDefaults.put("CheckBox.background",
						new javax.swing.plaf.ColorUIResource(Color.white));
				uiDefaults.put("RadioButton.background",
						new javax.swing.plaf.ColorUIResource(Color.white));
				uiDefaults.put("Tree.background",
						new javax.swing.plaf.ColorUIResource(Color.white));
				uiDefaults.put("Table.background",
						new javax.swing.plaf.ColorUIResource(Color.white));
				uiDefaults.put("ScrollPane.background",
						new javax.swing.plaf.ColorUIResource(Color.white));
				uiDefaults.put("SplitPane.background",
						new javax.swing.plaf.ColorUIResource(Color.white));
                  */
				uiDefaults.put("Viewport.background",
						new javax.swing.plaf.ColorUIResource(Color.white));
			}
		} catch (Exception e) {
		}
	   mainFrame = new MainFrame();
		((ReporterWithGUI) REPORTER).setDialogParent(mainFrame);

	}

	public static java.awt.Frame getDialogParent() {
		return mainFrame;
	}


	/** shows the splash screen */
	public static JWindow showSplashScreen() {
		java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit()
				.getScreenSize();

		final java.net.URL url = ClassLoader
				.getSystemResource("org/genepattern/gpge/resources/GenePattern_splash.png");
		final ImageIcon icon = new ImageIcon(url);

		final JWindow window = new JWindow();
		final Container contaner = window.getContentPane();
		contaner.add(new JLabel(icon));
		window.setLocation((screenSize.width - icon.getIconWidth()) / 2,
				(screenSize.height - icon.getIconHeight()) / 2);
		window.pack();
		//window.setSize(400,400);
		window.show();
		return window;
	}

	/** quites the program by first saving some stuff... */
	public void quit() {
		System.exit(0);
	}

	// static methods

	/** main */
	public static final void main(String[] args) {
		final GenePattern gp = new GenePattern();
	}

   public static void showMessageDialog(String title, String message) {
      if(MainFrame.windowStyle==MainFrame.WINDOW_STYLE_MDI) {
         javax.swing.JOptionPane.showInternalMessageDialog(mainFrame, message, title,
				javax.swing.JOptionPane.INFORMATION_MESSAGE);
      } else {
          javax.swing.JOptionPane.showMessageDialog(mainFrame, message, title,
				javax.swing.JOptionPane.INFORMATION_MESSAGE);
      }
   }
   
   public static void showMessageDialog(String message) {
      showMessageDialog("GenePattern", message);
   }
   
   
   public static void showErrorDialog(String title, String message) {
      if(MainFrame.windowStyle==MainFrame.WINDOW_STYLE_MDI) {
         javax.swing.JOptionPane.showInternalMessageDialog(mainFrame, message, "Error",
				javax.swing.JOptionPane.ERROR_MESSAGE);
      } else {
         javax.swing.JOptionPane.showMessageDialog(mainFrame, message, "Error",
				javax.swing.JOptionPane.ERROR_MESSAGE);
      }
	}
   
   public static boolean disconnectedFromServer(org.genepattern.webservice.WebServiceException wse, String server) {
      if(wse.getRootCause() instanceof org.apache.axis.AxisFault) {
         org.apache.axis.AxisFault af = (org.apache.axis.AxisFault) wse.getRootCause();
         Throwable t = af.getCause();
         if(t instanceof java.net.ConnectException || t instanceof java.net.UnknownHostException) {
            showErrorDialog("Unable to connect to " + 
                  server);
            return true;
         }
      }  
      return false;
   }
   
	public static void showErrorDialog(String message) {
		showErrorDialog("Error", message);
	}

	public static final void showError(final java.awt.Component parent,
			final String message) {
		REPORTER.showError(message);
	}

	public static final void showError(final java.awt.Component parent,
			final String message, final Throwable t) {
		REPORTER.showError(message, t);
	}

	public static final void showWarning(final java.awt.Component parent,
			final String message) {
		REPORTER.showWarning(message);
	}

	public static final void logWarning(final String warning) {
		REPORTER.logWarning(warning);
	}

	/** logs that an exception was ignored */
	public static final void ignoredException(Throwable th) {
		// not implemented
	}

	public static final Reporter getReporter() {
		return REPORTER;
	}

	/** shows the about dialog */
	public static final void showAbout() {
		//java.net.URL url = ClassLoader.getSystemResource
		// ("edu/mit/genome/gp/resources/About_GenePattern.html");
		String contents = null;
		InputStream in = null;
		try {
			in = ClassLoader
					.getSystemResourceAsStream("org/genepattern/gpge/resources/About_GenePattern.html");
		   StringBuffer sb = new StringBuffer();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String s = null;
			while((s=br.readLine())!=null) {
				sb.append(s);
				sb.append("\n");
			}
			contents = sb.toString();
		} catch (IOException ex) {
			REPORTER.logWarning(
					"Couldn't get the about text for this application", ex);
			contents = BuildProperties.PROGRAM_NAME + ' '
					+ BuildProperties.FULL_VERSION + " Build "
					+ BuildProperties.BUILD;
		} finally {
			try {
				if(in!=null) {
					in.close();
				}
			} catch(IOException x){}
		}
		// change the markers to the real property values
		// System.out.println("Before contents="+contents);
		contents = StringUtils.replaceAll(contents, "${PROGRAM_NAME}",
				BuildProperties.PROGRAM_NAME);
		contents = StringUtils.replaceAll(contents, "${FULL_VERSION}",
				BuildProperties.FULL_VERSION);
		contents = StringUtils.replaceAll(contents, "${BUILD.DATE}", String
				.valueOf(BuildProperties.BUILD_DATE));
		contents = StringUtils.replaceAll(contents, "${BUILD.TAG}", String
				.valueOf(BuildProperties.BUILD_TAG));

		// System.out.println("After contents="+contents);

		//javax.swing.JEditorPane notes = new
		// javax.swing.JEditorPane("text/plain", contents);

		javax.swing.JTextArea notes = new javax.swing.JTextArea(contents);
		notes.setMargin(new Insets(5, 5, 5, 5));
		notes.setEditable(false);

		final javax.swing.JScrollPane scroll_pane = new javax.swing.JScrollPane(
				notes);
		javax.swing.JOptionPane.showMessageDialog(getDialogParent(),
				scroll_pane, "About GenePattern",
				JOptionPane.INFORMATION_MESSAGE);

	}

	/** shows the about dialog */
	public static final void showWarnings() {
		java.net.URL url = ClassLoader
				.getSystemResource("org/genepattern/gpge/resources/Warning_preamble.html");
		try {
			ReportPanel warnings_panel = new ReportPanel(url,
					((ReporterWithGUI) REPORTER).getWarnings());
			JOptionPane.showMessageDialog(getDialogParent(), warnings_panel,
					"All GenePattern Warnings", JOptionPane.WARNING_MESSAGE);
		} catch (java.io.IOException ex) {
			showWarning(null, "Could not display warnings!\nGenePattern\n"
					+ "Whitehead Institute Center for Genome Research\n"
					+ "genepattern@broad.mit.edu");
		}

	}

	/** shows the about dialog */
	public static final void showErrors() {
		java.net.URL url = ClassLoader
				.getSystemResource("org/genepattern/gpge/resources/Error_preamble.html");
		try {
			ReportPanel errors_panel = new ReportPanel(url,
					((ReporterWithGUI) REPORTER).getErrors());
			javax.swing.JOptionPane.showMessageDialog(getDialogParent(),
					errors_panel, "GenePattern Analysis Module Errors",
					JOptionPane.ERROR_MESSAGE);
		} catch (java.io.IOException ex) {
			showWarning(null, "Could not display errors!\nGenePattern\n"
					+ "Whitehead Institute Center for Genome Research\n"
					+ "genepattern@broad.mit.edu");
		}

	}

	/** where the error/warning/info messages go */
	private static Reporter REPORTER;

	/** the java 1.4.1 AWT exception handler property key */
	public static final String AWT_EXCEPTION_HANDLER_KEY = "sun.awt.exception.handler";
	/** static initializer */
	static {
		// This is a graphical client error messages should be shown in dialogs
		// and
		// let other windows know that gp doen't want the JVM to be shutdown
		System.setProperty("gp.graphical", "True");

		REPORTER = AbstractReporter.getInstance();

		// need to catch all exceptions created durring any event and display
		// them to the user
		//FIXME this code snippet should be moved to a more promenent class
		// one that all modeles use
		final String handler = System.getProperty(AWT_EXCEPTION_HANDLER_KEY);
		if (handler == null || handler.trim().length() == 0)
		//	System.setProperty(AWT_EXCEPTION_HANDLER_KEY,
			//		"edu.mit.genome.util.ExceptionHandler");
		// end handler

		// test java version
		try {
			final String version = System.getProperty("java.version");

			final java.util.StringTokenizer tok = new java.util.StringTokenizer(
					version, ".");
			tok.hasMoreTokens();
			final int major = Integer.parseInt(tok.nextToken());
			tok.hasMoreTokens();
			final int minor = Integer.parseInt(tok.nextToken());
			tok.hasMoreTokens();
			final int revis = Integer.parseInt(tok.nextToken("._"));

			if (major == 1) {
				if (minor < 3) {
					final String msg = "Error: The early Java version "
							+ version
							+ " is not supported.\n"
							+ "GenePattern will not run with this version of Java!\n"
							+ "Please upgrade by installing Java 1.3.1"
							+ " or 1.4.1 or greater!";
					logWarning(msg);
					final java.awt.TextArea text_area = new java.awt.TextArea(
							msg, 4, -1, java.awt.TextArea.SCROLLBARS_NONE);
					text_area.setEditable(false);
					final java.awt.Button ok_button = new java.awt.Button("OK");
					final java.awt.Dialog dialog = new java.awt.Dialog(
							new java.awt.Frame(), "Error: Java verision wrong",
							true);
					dialog.add(text_area, BorderLayout.CENTER);
					dialog.add(ok_button, BorderLayout.SOUTH);
					ok_button
							.addActionListener(new java.awt.event.ActionListener() {
								public final void actionPerformed(
										java.awt.event.ActionEvent event) {
									ok_button.removeActionListener(this);
									dialog.hide();
									dialog.dispose();
								}
							});
					dialog.pack();
					dialog.show();

				} else if (minor == 3 && revis == 0) {
					showWarning(
							null,
							"Warning: Java version 1.3.0 "
									+ "is not supported!\n"
									+ "GenePattern will not run properly with Java 1.3.0\n"
									+ "Please upgrade by installing Java 1.3.1 or 1.4.1 or greater.");
				} else if (minor == 4 && revis == 0) {
					showWarning(null, "Warning: Java version 1.4.0 "
							+ "is not supported!\n"
							+ "GenePattern will behave badly with java 1.4.0\n"
							+ "Please install Java 1.3.1 or 1.4.1 or greater.");
				}
			} else if (major > 1) {
				final Integer ok_major_min = Integer
						.getInteger("gp.java.major.min");
				final Integer ok_major_max = Integer
						.getInteger("gp.java.major.max");
				if (!((ok_major_max != null && major < ok_major_max.intValue()) || (ok_major_min != null && major < ok_major_min
						.intValue()))) {
					showWarning(
							null,
							"Warning: Java version "
									+ version
									+ " is not supported!\n"
									+ "GenePattern may not work properly with this later version of Java.\n"
									+ "Look for updates from the GenePattern web site.");
				}
			} else { // major == 0 !!!
				System.err.println("Cannot be Java 0.x");
				final String msg = "Error: The impossibly early Java version "
						+ version
						+ " is not supported.\n"
						+ "GenePattern will not run with this version of Java!\n"
						+ "Please upgrade by installing Java 1.3.1"
						+ " or 1.4.1 or greater!";
				logWarning(msg);
				final java.awt.TextArea text_area = new java.awt.TextArea(msg,
						4, -1, java.awt.TextArea.SCROLLBARS_NONE);
				text_area.setEditable(false);
				final java.awt.Button ok_button = new java.awt.Button("OK");
				final java.awt.Dialog dialog = new java.awt.Dialog(
						new java.awt.Frame(), "Error: Java verision wrong",
						true);
				dialog.add(text_area, BorderLayout.CENTER);
				dialog.add(ok_button, BorderLayout.SOUTH);
				ok_button
						.addActionListener(new java.awt.event.ActionListener() {
							public final void actionPerformed(
									java.awt.event.ActionEvent event) {
								ok_button.removeActionListener(this);
								dialog.hide();
								dialog.dispose();
							}
						});
				dialog.pack();
				dialog.show();
			}
		} catch (NumberFormatException ex) {
			logWarning("While parsing java version " + ex);
		}

		// setup OmniGene properties
		if (System.getProperty("omnigene.conf") == null) { // if it doesn't
														   // exist
			final String home = System.getProperty("user.home");

			final String separator = java.io.File.separator;
			final String base = separator + "gp" + separator + "resources"
					+ separator;
			final String conf_location = home + base;

			System.setProperty("omnigene.conf", conf_location);
			// needed for GenePatternAnalysisTask
			System.setProperty("log4j.configuration", conf_location
					+ "log4j.properties");
		}

	}
}