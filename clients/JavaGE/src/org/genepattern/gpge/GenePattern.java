/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

/*
 * GenePattern.java
 *
 * Created on August 22, 2002, 10:09 AM
 */
package org.genepattern.gpge;

import java.awt.Color;
import java.awt.Container;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import org.genepattern.gpge.ui.infopanels.ReportPanel;
import org.genepattern.gpge.ui.maindisplay.GPGE;
import org.genepattern.gpge.ui.tasks.AnalysisServiceManager;
import org.genepattern.gpge.ui.util.ReporterWithGUI;
import org.genepattern.gpge.util.BuildProperties;
import org.genepattern.util.StringUtils;

/**
 * GPGE Launcher
 * 
 * @author kohm
 */
public final class GenePattern {
    static JFrame parentFrame;

    static Icon icon;

    /** where the module error messages go */
    private static ReporterWithGUI REPORTER;

    private static ImageIcon smallIcon;

    public static Icon getIcon() {
        return icon;
    }

    public static Icon getSmallIcon() {
        return smallIcon;
    }

    public GenePattern() {
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager
                    .getSystemLookAndFeelClassName());

        } catch (Exception e) {
        }
        loadDefaults();
        parentFrame = GPGE.getInstance().getFrame();
        GPGE.getInstance().startUp();
        REPORTER = new ReporterWithGUI(parentFrame);
    }

    static void loadDefaults() {
        URL imgURL = GenePattern.class
                .getResource("/org/genepattern/gpge/resources/GPGE_small.jpg");
        if (imgURL != null) {
            icon = new ImageIcon(imgURL);
        }
        URL imgURL2 = GenePattern.class
                .getResource("/org/genepattern/gpge/resources/GPGE_small2.jpg");
        if (imgURL2 != null) {
            smallIcon = new ImageIcon(imgURL2);
        }

        if (!GPGE.RUNNING_ON_MAC) {
            javax.swing.UIDefaults uiDefaults = javax.swing.UIManager
                    .getDefaults();
            uiDefaults.put("Viewport.background",
                    new javax.swing.plaf.ColorUIResource(Color.white));
        }

    }

    public static java.awt.Frame getDialogParent() {
        return parentFrame;
    }

    /** shows the splash screen */
    public static JWindow showSplashScreen() {
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit()
                .getScreenSize();

        final java.net.URL url = GenePattern.class
                .getResource("/org/genepattern/gpge/resources/GenePattern_splash.png");
        final ImageIcon icon = new ImageIcon(url);

        final JWindow window = new JWindow();
        final Container contaner = window.getContentPane();
        contaner.add(new JLabel(icon));
        window.setLocation((screenSize.width - icon.getIconWidth()) / 2,
                (screenSize.height - icon.getIconHeight()) / 2);
        window.pack();
        // window.setSize(400,400);
        window.show();
        return window;
    }

    // static methods

    /** main */
    public static final void main(String[] args) {
        new GenePattern();
    }

    public static void showMessageDialog(final String title,
            final String message) {
        Runnable runnable = new Runnable() {
            public void run() {
                javax.swing.JOptionPane.showMessageDialog(parentFrame, message,
                        title, javax.swing.JOptionPane.INFORMATION_MESSAGE);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    public static void showMessageDialog(String message) {
        showMessageDialog("GenePattern", message);
    }

    public static void showErrorDialog(final String title, final String message) {
        Runnable runnable = new Runnable() {
            public void run() {
                javax.swing.JOptionPane.showMessageDialog(parentFrame, message,
                        "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    public static boolean disconnectedFromServer(
            org.genepattern.webservice.WebServiceException wse) {
        return disconnectedFromServer(wse, AnalysisServiceManager.getInstance()
                .getServer());
    }

    /**
     * Returns <tt>true</tt> if the exception indicates that the client is
     * disconnected from the server, <tt>false</tt> otherwise. If
     * <tt>true</tt> pops open a dialog to alert the user.
     */
    public static boolean disconnectedFromServer(
            org.genepattern.webservice.WebServiceException wse, String server) {
        if (wse.getRootCause() instanceof org.apache.axis.AxisFault) {
            org.apache.axis.AxisFault af = (org.apache.axis.AxisFault) wse
                    .getRootCause();
            Throwable t = af.getCause();
            if (t instanceof java.net.ConnectException
                    || t instanceof java.net.UnknownHostException) {
                String message = "Unable to connect to " + server;
                javax.swing.JOptionPane.showMessageDialog(parentFrame, message,
                        "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                return true;
            }
        }
        return false;
    }

    public static void showErrorDialog(String message) {
        showErrorDialog("Error", message);
    }

    public static final void showModuleErrorDialog(String title, String message) {
        REPORTER.showError(title, message);
    }

    /** shows the about dialog */
    public static final void showAbout() {
        String contents = null;
        InputStream in = null;
        try {
            in = GenePattern.class
                    .getResourceAsStream("/org/genepattern/gpge/resources/About_GenePattern.html");
            StringBuffer sb = new StringBuffer();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String s = null;
            while ((s = br.readLine()) != null) {
                sb.append(s);
                sb.append("\n");
            }
            contents = sb.toString();
        } catch (IOException ex) {
            System.err
                    .println("Couldn't get the about text for this application");
            contents = BuildProperties.PROGRAM_NAME + ' '
                    + BuildProperties.FULL_VERSION + " Build "
                    + BuildProperties.BUILD;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException x) {
            }
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

        // javax.swing.JEditorPane notes = new
        // javax.swing.JEditorPane("text/plain", contents);

        javax.swing.JTextArea notes = new javax.swing.JTextArea(contents);
        notes.setMargin(new Insets(5, 5, 5, 5));
        notes.setEditable(false);

        final javax.swing.JScrollPane scroll_pane = new javax.swing.JScrollPane(
                notes);
        javax.swing.JOptionPane.showMessageDialog(getDialogParent(),
                scroll_pane, "About GenePattern",
                JOptionPane.INFORMATION_MESSAGE, icon);

    }

    /** shows the about dialog */
    public static final void showErrors() {
        java.net.URL url = GenePattern.class
                .getResource("/org/genepattern/gpge/resources/Error_preamble.html");
        try {
            ReportPanel errors_panel = new ReportPanel(url, REPORTER
                    .getErrors());
            javax.swing.JOptionPane.showMessageDialog(getDialogParent(),
                    errors_panel, "GenePattern Analysis Module Errors",
                    JOptionPane.ERROR_MESSAGE, icon);
        } catch (java.io.IOException ex) {
            System.err.println("Could not display errors");
        }

    }

    public static void setDialogParent(JFrame f) {
        loadDefaults();
        parentFrame = f;
        REPORTER = new ReporterWithGUI(parentFrame);
    }

    /** the java 1.4.1 AWT exception handler property key */
    // public static final String AWT_EXCEPTION_HANDLER_KEY =
    // "sun.awt.exception.handler";
}