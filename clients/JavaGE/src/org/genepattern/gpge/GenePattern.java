/*
 * GenePattern.java
 *
 * Created on August 22, 2002, 10:09 AM
 */
package org.genepattern.gpge;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JWindow;

import org.genepattern.data.DataModel;
import org.genepattern.data.Dataset;
import org.genepattern.data.Matrix;
import org.genepattern.data.NamesPanel;
import org.genepattern.data.SomProperties;
import org.genepattern.data.Template;
import org.genepattern.gpge.ui.infopanels.ReportPanel;
import org.genepattern.gpge.ui.maindisplay.DataObjectBrowser;
import org.genepattern.gpge.util.BuildProperties;
import org.genepattern.util.AbstractReporter;
import org.genepattern.util.Reporter; 
import org.genepattern.util.ReporterWithGUI;
import org.genepattern.util.StringUtils;



/**
 * Main program class
 *
 * @author  kohm
 */
public final class GenePattern {
	protected static Component dialogParent = null;
	protected static DataObjectBrowser browser = null;
    


    /** Creates a new instance of GenePattern */
    public GenePattern() {
       try {
          javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
       } catch(Exception e) {}
        // find dimensions of screen to calculate center of screen
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsConfiguration gc = ge.getScreenDevices()[0].getConfigurations()[0];
        final Rectangle bounds = gc.getBounds();
        // splash screen
        final JWindow splash = showSplashScreen(bounds);
        
        // setup data models
        DataModel model = Dataset.DATA_MODEL;
        model = Matrix.DATA_MODEL;
        model = NamesPanel.DATA_MODEL;
        model = Template.DATA_MODEL;
	model = SomProperties.DATA_MODEL;
        // end models
        
        // display in a JFrame
        javax.swing.JFrame frame = new javax.swing.JFrame(BuildProperties.PROGRAM_NAME+' '+BuildProperties.FULL_VERSION+"  Build: "+BuildProperties.BUILD);
        //javax.swing.JFrame frame = new javax.swing.JFrame("GenePattern 0.9ea");
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                quit();
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                splash.hide();
                splash.dispose();
                
            }
        });
        // startup main UI
        browser = new DataObjectBrowser(frame);
	  dialogParent = browser;
	 ((ReporterWithGUI)REPORTER).setDialogParent(dialogParent);


        frame.setJMenuBar(browser.getMenuBar());
        java.awt.Container container = frame.getContentPane();
        container.add(browser);
        frame.setSize(850, 500);
        //frame.pack();
        
        frame.setLocation(bounds.x+(bounds.width-frame.getWidth())/2,  20);
        
        frame.show();
        

    }
    
	public static Component getDialogParent(){
		return dialogParent;
	}
	public static DataObjectBrowser getDataObjectBrowser(){
		return browser;
	}

	

    /** shows the splash screen */
    private static JWindow showSplashScreen(final Rectangle bounds) {
        final java.net.URL url = ClassLoader.getSystemResource("org/genepattern/gpge/resources/GenePattern_splash.png");
        final ImageIcon icon = new ImageIcon(url);
        //System.out.println("icon="+icon);
        final JWindow window = new JWindow();
        final Container contaner = window.getContentPane();
        contaner.add(new JLabel(icon));
        window.setLocation(bounds.x+(bounds.width-icon.getIconWidth())/2,
            bounds.y + (bounds.height - icon.getIconHeight())/2);
        window.pack();
        window.show();
        return window;
    }
    
    /** quites the program by first saving some stuff... */
    public void quit() {
        System.exit(0);
    }
    // static methods
    
    /** main  */
    public static final void main(String[] args) {
        final GenePattern gp = new GenePattern();
    }


    public static final void showError(final java.awt.Component parent, final String message) {
        //System.out.println("REPORTER="+getReporter().getClass());
        REPORTER.showError(message);
    }
    public static final void showError(final java.awt.Component parent, final String message, final Throwable t) {
        REPORTER.showError(message, t);
    }
    public static final void showWarning(final java.awt.Component parent, final String message) {
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
    /** creates a GenePattern Properties loaded PreferencesPanel */
    public static final org.genepattern.gpge.ui.preferences.PreferencesPanel createGpPreferencesPanel() {
        return new org.genepattern.gpge.ui.preferences.PreferencesPanel();
    }
    /** shows the about dialog */
    public static final void showAbout() {
        //java.net.URL url = ClassLoader.getSystemResource ("edu/mit/genome/gp/resources/About_GenePattern.html");
        String contents = null;
        try {
            final InputStream in = ClassLoader.getSystemResourceAsStream("org/genepattern/gpge/resources/About_GenePattern.html");
            contents = org.genepattern.io.StorageUtils.createStringFromReader(new InputStreamReader(in));
            in.close();
            //contents = org.genepattern.io.StorageUtils.createStringFromContents(new File(url.getFile()));
        } catch (IOException ex) {
            REPORTER.logWarning("Couldn't get the about text for this application", ex);
            contents = BuildProperties.PROGRAM_NAME+' '+BuildProperties.FULL_VERSION
                        +" Build "+BuildProperties.BUILD;
        }
        // change the markers to the real property values
        // System.out.println("Before contents="+contents);
        contents = StringUtils.replaceAll(contents, "${PROGRAM_NAME}", BuildProperties.PROGRAM_NAME);
        contents = StringUtils.replaceAll(contents, "${FULL_VERSION}", BuildProperties.FULL_VERSION);
        contents = StringUtils.replaceAll(contents, "${BUILD.DATE}", String.valueOf(BuildProperties.BUILD_DATE) );
	contents = StringUtils.replaceAll(contents, "${BUILD.TAG}", String.valueOf(BuildProperties.BUILD_TAG) );

        // System.out.println("After contents="+contents);

        //javax.swing.JEditorPane notes = new javax.swing.JEditorPane("text/plain", contents);

        javax.swing.JTextArea notes = new javax.swing.JTextArea(contents);
	notes.setMargin(new Insets(5,5,5,5));
        notes.setEditable (false);
        
       
        final javax.swing.JScrollPane scroll_pane = new javax.swing.JScrollPane(notes);
        javax.swing.JOptionPane.showMessageDialog(getDialogParent(), scroll_pane, "About GenePattern", JOptionPane.INFORMATION_MESSAGE);

    }
    /** shows the about dialog */
    public static final void showWarnings() {
        java.net.URL url = ClassLoader.getSystemResource ("org/genepattern/gpge/resources/Warning_preamble.html");
        try{
            ReportPanel warnings_panel = new ReportPanel(url, ((ReporterWithGUI)REPORTER).getWarnings());
            JOptionPane.showMessageDialog(getDialogParent(), warnings_panel, "All GenePattern Warnings", JOptionPane.WARNING_MESSAGE);
        } catch (java.io.IOException ex) {
                showWarning(null, "Could not display warnings!\nGenePattern\n"
                +"Whitehead Institute Center for Genome Research\n"
                +"genepattern@broad.mit.edu");
        }
        
    }
        /** shows the about dialog */
    public static final void showErrors() {
        java.net.URL url = ClassLoader.getSystemResource ("org/genepattern/gpge/resources/Error_preamble.html");
        try{
            ReportPanel errors_panel = new ReportPanel(url, ((ReporterWithGUI)REPORTER).getErrors());
            javax.swing.JOptionPane.showMessageDialog(getDialogParent(), errors_panel, "All GenePattern Errors", JOptionPane.ERROR_MESSAGE);
        } catch (java.io.IOException ex) {
                showWarning(null, "Could not display errors!\nGenePattern\n"
                +"Whitehead Institute Center for Genome Research\n"
                +"genepattern@broad.mit.edu");
        }
        
    }

    /** where the error/warning/info messages go */
    private static Reporter REPORTER;
    /** the java 1.4.1 AWT exception handler property key */
    public static final String AWT_EXCEPTION_HANDLER_KEY = "sun.awt.exception.handler";
    /** static initializer */
    static {
        // This is a graphical client error messages should be shown in dialogs and
        // let other windows know that gp doen't want the JVM to be shutdown
        System.setProperty("gp.graphical", "True");
        
        REPORTER = AbstractReporter.getInstance();
       
        // need to catch all exceptions created durring any event and display
        // them to the user
        //FIXME this code snippet should be moved to a more promenent class
        // one that all modeles use
        final String handler = System.getProperty(AWT_EXCEPTION_HANDLER_KEY);
        if( handler == null || handler.trim().length() == 0 )
            System.setProperty(AWT_EXCEPTION_HANDLER_KEY, "edu.mit.genome.util.ExceptionHandler");
        // end handler
        
        // test java version
        try {
            final String version = System.getProperty("java.version");
            System.out.println("java.version="+version);
            final java.util.StringTokenizer tok = new java.util.StringTokenizer(version, ".");
            tok.hasMoreTokens();
            final int major = Integer.parseInt(tok.nextToken());
            tok.hasMoreTokens();
            final int minor = Integer.parseInt(tok.nextToken());
            tok.hasMoreTokens();
            final int revis = Integer.parseInt(tok.nextToken("._"));

            System.out.println("major.minor.rev= "+major+'.'+minor+'.'+revis);
            if( major == 1) {
                if( minor < 3 ){
                    final String msg = "Error: The early Java version "+version
                    +" is not supported.\n"
                    +"GenePattern will not run with this version of Java!\n"
                    +"Please upgrade by installing Java 1.3.1"
                    +" or 1.4.1 or greater!";
                    logWarning(msg);
                    final java.awt.TextArea text_area = new java.awt.TextArea(msg, 4, -1, java.awt.TextArea.SCROLLBARS_NONE);
                    text_area.setEditable(false);
                    final java.awt.Button ok_button = new java.awt.Button("OK");
                    final java.awt.Dialog dialog = new java.awt.Dialog(new java.awt.Frame(), "Error: Java verision wrong", true);
                    dialog.add(text_area, BorderLayout.CENTER);
                    dialog.add(ok_button, BorderLayout.SOUTH);
                    ok_button.addActionListener(new java.awt.event.ActionListener() {
                        public final void actionPerformed(java.awt.event.ActionEvent event) {
                            ok_button.removeActionListener(this);
                            dialog.hide();
                            dialog.dispose();
                        }
                    });
                    dialog.pack();
                    dialog.show();
                    
                }else if( minor == 3 && revis == 0 ) {
                    showWarning(null, "Warning: Java version 1.3.0 "
                    +"is not supported!\n"
                    +"GenePattern will not run properly with Java 1.3.0\n"
                    +"Please upgrade by installing Java 1.3.1 or 1.4.1 or greater.");
                } else if( minor == 4 && revis == 0 ) {
                    showWarning(null, "Warning: Java version 1.4.0 "
                    +"is not supported!\n"
                    +"GenePattern will behave badly with java 1.4.0\n"
                    +"Please install Java 1.3.1 or 1.4.1 or greater.");
                }
            } else if( major > 1 ) {
                final Integer ok_major_min = Integer.getInteger("gp.java.major.min");
                final Integer ok_major_max = Integer.getInteger("gp.java.major.max");
                if( !((ok_major_max != null && major < ok_major_max.intValue())
                || (ok_major_min != null && major < ok_major_min.intValue())) ) {
                    showWarning(null, "Warning: Java version "+version
                    +" is not supported!\n"
                    +"GenePattern may not work properly with this later version of Java.\n"
                    +"Look for updates from the GenePattern web site.");
                } 
            } else { // major == 0 !!!
                System.err.println("Cannot be Java 0.x");
                final String msg = "Error: The impossibly early Java version "+version
                +" is not supported.\n"
                +"GenePattern will not run with this version of Java!\n"
                +"Please upgrade by installing Java 1.3.1"
                +" or 1.4.1 or greater!";
                logWarning(msg);
                final java.awt.TextArea text_area = new java.awt.TextArea(msg, 4, -1, java.awt.TextArea.SCROLLBARS_NONE);
                text_area.setEditable(false);
                final java.awt.Button ok_button = new java.awt.Button("OK");
                final java.awt.Dialog dialog = new java.awt.Dialog(new java.awt.Frame(), "Error: Java verision wrong", true);
                dialog.add(text_area, BorderLayout.CENTER);
                dialog.add(ok_button, BorderLayout.SOUTH);
                ok_button.addActionListener(new java.awt.event.ActionListener() {
                   public final void actionPerformed(java.awt.event.ActionEvent event) {
                       ok_button.removeActionListener(this);
                       dialog.hide();
                       dialog.dispose();
                   }
                });
                dialog.pack();
                dialog.show();
            }
        } catch (NumberFormatException ex) {
            logWarning("While parsing java version "+ex);
        }
        
        // setup OmniGene properties
        if( System.getProperty("omnigene.conf") == null ) { // if it doesn't exist
            final String home = System.getProperty("user.home");
            System.out.println("user.home="+home);
            final String separator = java.io.File.separator;
            final String base = separator+"gp"+separator+"resources"+separator;
            final String conf_location = home+base;
                System.out.println("Setting omnigene.conf='"+conf_location+"'");
                System.setProperty("omnigene.conf", conf_location);
                // needed for GenePatternAnalysisTask
                System.setProperty("log4j.configuration", conf_location+"log4j.properties");
        } else
            System.out.println("Already have the property set "+System.getProperty("omnigene.conf"));

        
    }
}
