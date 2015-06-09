/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.startapp;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The UI form used to gather config from the user
 *
 * @author Thorin Tabor
 */
public class ConfigApp {
    private JButton saveAndLaunchGenePatternButton;
    private JRadioButton yesRadioButton;
    private JRadioButton noRadioButton;
    private JTextField emailField;
    private JTextField daysPurgeField;
    private JTextField timePurgeField;
    private JTextField rField;
    private JTextField perlField;
    private JPanel genepatternConfigPanel;
    private JTextField r25Field;
    private JTextField javaField;

    private static JFrame _instance;
    private static File workingDir;

    /**
     * Display the config form
     *
     * @param args
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("GenePattern Configuration");
        frame.setContentPane(new ConfigApp().genepatternConfigPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        // Set the singleton
        ConfigApp._instance = frame;

        // Set the working directory
        String workingString = "/Users/tabor/workspace/genepattern/gp-macapp/dist/GenePattern.app/Contents/Resources";
        if (args.length >= 1) {
            workingString = args[0];
        }
        workingDir = new File(workingString);
    }

    /**
     * Attach listeners to the form
     */
    public ConfigApp() {
        saveAndLaunchGenePatternButton.addActionListener(new ActionListener() {

            /**
             * When the save & launch GenePattern button is pressed
             *
             * @param e
             */
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

                // Run the shell script
                // GenePattern.runShellScript(workingDir.getAbsolutePath());

                // Show the dialog
                JFrame frame = ConfigApp.instance();
                JOptionPane.showMessageDialog(frame, "Configuration saved. Please restart GenePattern.");

                // Close
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            }
        });
    }

    /**
     * Get the JFrame instance
     *
     * @return
     */
    public static JFrame instance() {
        return _instance;
    }

    /**
     * Write the config selected by the user to the properties file
     */
    public void writeConfig() {
        PropertiesWriter pw = new PropertiesWriter();
        pw.setEmail(emailField.getText());
        pw.setDaysPurge(daysPurgeField.getText());
        pw.setTimePurge(timePurgeField.getText());
        pw.setPerl(perlField.getText());
        pw.setJava(javaField.getText());
        pw.setR(rField.getText());
        pw.setR25(r25Field.getText());
        pw.setRequirePassword(Boolean.toString(yesRadioButton.isSelected()));

        File newResourcesDir = new File(workingDir.getParent(), "Resources/GenePatternServer/resources");
        String user = System.getProperty("user.name");
        File resourcesDir = new File("/Users/" + user + "/.genepattern/resources");
        File propFile = new File(newResourcesDir, "genepattern.properties");
        try {
            pw.writeUserTime(propFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Copy over the new build.properties file
        File oldBuildProperties = new File(resourcesDir, "build.properties");
        File newBuildProperties = new File(newResourcesDir, "build.properties");

        try {
            FileUtils.copyFile(newBuildProperties, oldBuildProperties);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        rField.setText(data.getR());
        r25Field.setText(data.getR25());
        perlField.setText(data.getPerl());
        javaField.setText(data.getJava());
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
        data.setR25(r25Field.getText());
        data.setR(rField.getText());
        data.setPerl(perlField.getText());
        data.setJava(javaField.getText());
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
        if (r25Field.getText() != null ? !r25Field.getText().equals(data.getR25()) : data.getR25() != null) return true;
        if (rField.getText() != null ? !rField.getText().equals(data.getR()) : data.getR() != null) return true;
        if (perlField.getText() != null ? !perlField.getText().equals(data.getPerl()) : data.getPerl() != null)
            return true;
        if (javaField.getText() != null ? !javaField.getText().equals(data.getJava()) : data.getJava() != null)
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
        final JLabel label4 = new JLabel();
        label4.setText("R");
        genepatternConfigPanel.add(label4, cc.xy(1, 7));
        final JLabel label5 = new JLabel();
        label5.setText("R 2.5");
        genepatternConfigPanel.add(label5, cc.xy(1, 9));
        emailField = new JTextField();
        genepatternConfigPanel.add(emailField, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
        daysPurgeField = new JTextField();
        daysPurgeField.setText("7");
        genepatternConfigPanel.add(daysPurgeField, cc.xy(3, 3, CellConstraints.FILL, CellConstraints.DEFAULT));
        timePurgeField = new JTextField();
        timePurgeField.setText("23:00");
        genepatternConfigPanel.add(timePurgeField, cc.xy(3, 5, CellConstraints.FILL, CellConstraints.DEFAULT));
        rField = new JTextField();
        rField.setText("/usr/bin/r");
        genepatternConfigPanel.add(rField, cc.xy(3, 7, CellConstraints.FILL, CellConstraints.DEFAULT));
        r25Field = new JTextField();
        r25Field.setText("/Library/Frameworks/R.framework/Versions/2.5/Resources");
        genepatternConfigPanel.add(r25Field, cc.xy(3, 9, CellConstraints.FILL, CellConstraints.DEFAULT));
        final JLabel label6 = new JLabel();
        label6.setText("Perl");
        genepatternConfigPanel.add(label6, cc.xy(1, 11));
        perlField = new JTextField();
        perlField.setText("/usr/bin/perl");
        genepatternConfigPanel.add(perlField, cc.xy(3, 11, CellConstraints.FILL, CellConstraints.DEFAULT));
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
        javaField = new JTextField();
        javaField.setText("/usr");
        genepatternConfigPanel.add(javaField, cc.xy(3, 13, CellConstraints.FILL, CellConstraints.DEFAULT));
        final JLabel label8 = new JLabel();
        label8.setText("Java");
        genepatternConfigPanel.add(label8, cc.xy(1, 13));
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
