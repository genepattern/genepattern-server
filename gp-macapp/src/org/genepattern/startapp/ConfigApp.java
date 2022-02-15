/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.startapp;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * The UI form used to gather config from the user
 *
 * @author Thorin Tabor
 */
public class ConfigApp implements ActionListener {
    private static final String DEFAULT_WORKING_DIR="gp-macapp/resources/GenePattern.app/Contents/Resources";
    
    public static File initGpHome() {
        final String user = System.getProperty("user.name");
        final File gpHome = new File("/Users/" + user + "/.genepattern");
        return gpHome;
    }

    private final File workingDir;
    private final File gpHome;
    
    private JButton saveAndLaunchGenePatternButton;
    private JRadioButton yesRadioButton;
    private JRadioButton noRadioButton;
    private JTextField emailField;
    private JTextField daysPurgeField;
    private JTextField timePurgeField;
    private JPanel genepatternConfigPanel;
    
    private final JFrame frame;
    
    /**
     * Start the GenePattern Configuration app
     * @param args
     */
    public static void main(final String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // display the GenePattern Configuration form
                final ConfigApp configApp;
                if (args.length==0) {
                    configApp=new ConfigApp();
                }
                else if (args.length==1) {
                    configApp=new ConfigApp(args[0], initGpHome().getAbsolutePath());
                }
                else {
                    configApp=new ConfigApp(args[0], args[1]);
                }
                configApp.frame.setVisible(true);
            }
        });
    }

    /**
     * Attach listeners to the form
     */
    public ConfigApp() {
        this(DEFAULT_WORKING_DIR, initGpHome().getAbsolutePath());
    }
    public ConfigApp(final String workingDirStr, final String gpHomeDirStr) {
        this.workingDir=new File(workingDirStr);
        this.gpHome=new File(gpHomeDirStr);
        this.frame = new JFrame("GenePattern Configuration");
        this.frame.setContentPane(this.genepatternConfigPanel);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.pack();
        saveAndLaunchGenePatternButton.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Validate form
        String email = emailField.getText();
        if (!ConfigApp.isValidEmailAddress(email)) {
            JOptionPane.showMessageDialog(null, "Email address is not valid!");
            return;
        }

        String daysPurge = daysPurgeField.getText();
        if (!ConfigApp.isValidDaysPurge(daysPurge)) {
            JOptionPane.showMessageDialog(null, "Days purge is not valid!");
            return;
        }

        String timePurge = timePurgeField.getText();
        if (!ConfigApp.isValidTimePurge(timePurge)) {
            JOptionPane.showMessageDialog(null, "Time purge is not valid!");
            return;
        }

        // If everything checks out, call properties writer
        writeConfig();

        // Create the setup flag
        File resources = new File(workingDir.getParent(), "Resources");
        File readyFlag = new File(resources, "ready");
        try {
            readyFlag.createNewFile();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // Show the dialog
        JOptionPane.showMessageDialog(frame, "Configuration saved. Please restart GenePattern.");

        // Close
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }

    /**
     * Write the config selected by the user to the properties file
     */
    public void writeConfig() {
        // Get file references
        File newResourcesDir = new File(workingDir.getParent(), "Resources/GenePatternServer/resources");
        String user = System.getProperty("user.name");
        File oldResourcesDir = new File("/Users/" + user + "/.genepattern/resources");
        File newPropFile = new File(newResourcesDir, "genepattern.properties");
        File oldPropFile = new File(oldResourcesDir, "genepattern.properties");

        // Write to genepattern.properties
        PropertiesWriter pw = new PropertiesWriter();
        pw.setEmail(emailField.getText());
        pw.setDaysPurge(daysPurgeField.getText());
        pw.setTimePurge(timePurgeField.getText());
        pw.setRequirePassword(Boolean.toString(yesRadioButton.isSelected()));

        try {
            pw.writeUserTime(newPropFile);
            pw.writeUserTime(oldPropFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // Write 'webmaster={registration.email}' to custom.properties
        File customProps=new File(gpHome,"resources/custom.properties");
        final List<String> lines=new ArrayList<String>();
        lines.add("webmaster="+emailField.getText());
        GenePattern.appendToFile(customProps, lines);
    }

    /**
     * Validate the days until purge field
     *
     * @param purge
     * @return
     */
    public static boolean isValidDaysPurge(String purge) {
        try {
            Integer.parseInt(purge);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Validate the purge time field
     *
     * @param purge
     * @return
     */
    public static boolean isValidTimePurge(String purge) {
        Pattern pattern = Pattern.compile("([01]?[0-9]|2[0-3]):[0-5][0-9]");
        Matcher matcher = pattern.matcher(purge);
        return matcher.matches();
    }

    /**
     * Validate the email address field
     *
     * @param email
     * @return
     */
    public static boolean isValidEmailAddress(String email) {
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ex) {
            result = false;
        }
        return result;
    }

    /**
     * Bind the form data to the bean
     *
     * @param data
     */
    public void setData(StartConfigBean data) {
        emailField.setText(data.getEmail());
        daysPurgeField.setText(data.getDaysPurge());
        timePurgeField.setText(data.getTimePurge());
    }

    /**
     * Get the form data from the bean
     *
     * @param data
     */
    public void getData(StartConfigBean data) {
        data.setEmail(emailField.getText());
        data.setDaysPurge(daysPurgeField.getText());
        data.setTimePurge(timePurgeField.getText());
    }

    /**
     * Check if any fields were modified
     *
     * @param data
     * @return
     */
    public boolean isModified(StartConfigBean data) {
        if (emailField.getText() != null ? !emailField.getText().equals(data.getEmail()) : data.getEmail() != null)
            return true;
        if (daysPurgeField.getText() != null ? !daysPurgeField.getText().equals(data.getDaysPurge()) : data.getDaysPurge() != null)
            return true;
        if (timePurgeField.getText() != null ? !timePurgeField.getText().equals(data.getTimePurge()) : data.getTimePurge() != null)
            return true;
        return false;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        genepatternConfigPanel = new JPanel();
        genepatternConfigPanel.setLayout(new FormLayout("fill:d:grow,left:4dlu:noGrow,fill:d:grow", "center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:d:grow"));
        genepatternConfigPanel.setBorder(BorderFactory.createTitledBorder("GenePattern Configuration"));
        saveAndLaunchGenePatternButton = new JButton();
        saveAndLaunchGenePatternButton.setText("Save and Launch GenePattern");
        CellConstraints cc = new CellConstraints();
        genepatternConfigPanel.add(saveAndLaunchGenePatternButton, cc.xy(3, 19));
        final JLabel label1 = new JLabel();
        label1.setText("Days Before Purge");
        genepatternConfigPanel.add(label1, cc.xy(1, 3));
        final JLabel label2 = new JLabel();
        label2.setText("Email Address");
        genepatternConfigPanel.add(label2, cc.xy(1, 1));
        final JLabel label3 = new JLabel();
        label3.setText("Time of Day to Purge");
        genepatternConfigPanel.add(label3, cc.xy(1, 5));
        emailField = new JTextField();
        genepatternConfigPanel.add(emailField, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
        daysPurgeField = new JTextField();
        daysPurgeField.setText("7");
        genepatternConfigPanel.add(daysPurgeField, cc.xy(3, 3, CellConstraints.FILL, CellConstraints.DEFAULT));
        timePurgeField = new JTextField();
        timePurgeField.setText("23:00");
        genepatternConfigPanel.add(timePurgeField, cc.xy(3, 5, CellConstraints.FILL, CellConstraints.DEFAULT));
        final JLabel label7 = new JLabel();
        label7.setText("Require Password");
        genepatternConfigPanel.add(label7, cc.xy(1, 15));
        yesRadioButton = new JRadioButton();
        yesRadioButton.setText("Yes");
        genepatternConfigPanel.add(yesRadioButton, cc.xy(3, 15));
        noRadioButton = new JRadioButton();
        noRadioButton.setSelected(true);
        noRadioButton.setText("No");
        genepatternConfigPanel.add(noRadioButton, cc.xy(3, 17));
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(yesRadioButton);
        buttonGroup.add(noRadioButton);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return genepatternConfigPanel;
    }
}
