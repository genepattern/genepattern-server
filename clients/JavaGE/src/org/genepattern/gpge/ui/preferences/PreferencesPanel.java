/*
 * PreferencesPanel.java
 *
 * Created on April 24, 2003, 11:36 AM
 */

package org.genepattern.gpge.ui.preferences;

import java.io.File;
import java.io.FileOutputStream;

import java.util.Properties;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JTabbedPane;

import org.genepattern.gpge.ui.propertyviewer.PropertyViewerPanel;
import org.genepattern.util.GPpropertiesManager;

import java.awt.event.ActionListener;


/**
 *
 * @author  kohm
 */
public class PreferencesPanel extends javax.swing.JPanel {
    
    /** Creates new form PreferencesPanel */
    public PreferencesPanel() {
        initComponents();
//    }
//    /**  */
//    public PreferencesPanel(final Properties gp_props) {
//        this();
        property_panels = new java.util.ArrayList(20);
        final JTabbedPane main_tabbedPane = new javax.swing.JTabbedPane();
        tabbedPane.addTab("Main Options", main_tabbedPane);
        final JTabbedPane advanced_tabbedPane = new javax.swing.JTabbedPane();
        tabbedPane.addTab("Advanced Options", advanced_tabbedPane);
        
        final Properties gp_props = new GPpropAcessor().getProperties();
        
        PropertyViewerPanel prop_panel = new PropertyViewerPanel(gp_props, true);
        property_panels.add(prop_panel);
        main_tabbedPane.addTab("GenePattern", null, prop_panel,
            "Main GenePattern properties");
        // populate the advanced options
        final String[] prop_names = getOgPropNames();
        final Properties[] props = getOgProps(prop_names);
        final int limit = props.length;
        for(int i = 0; i < limit; i++) {
            prop_panel = new PropertyViewerPanel(props[i], true);
            property_panels.add(prop_panel);
            advanced_tabbedPane.addTab(prop_names[i], prop_panel);
        }
        
        final Properties sys_props = System.getProperties();
        advanced_tabbedPane.addTab("System", null, new PropertyViewerPanel(sys_props, false),
            "Java system properties");
        
        
        final ActionListener listener = new ActionListener() {
            public final void actionPerformed(final java.awt.event.ActionEvent e) {
                final JButton button = (JButton)e.getSource();
		try {
                    if( button == save_button) {
                        closeWindow();
                        saveProperties();
                    } else if( button == close_button ) {
                        closeWindow();
                    } else
                        System.err.println("event from unknown button "+button);
                } catch( java.io.IOException ex) {
                    org.genepattern.util.ExceptionHandler.handleException(ex);
                }
            }
        };
        save_button.addActionListener(listener);
        close_button.addActionListener(listener);
    }
    
    /** gets the property names */
    private String[] getOgPropNames() {
        final java.io.File dir = new java.io.File(System.getProperty("omnigene.conf"));
        return dir.list(new java.io.FilenameFilter() {
            public final boolean accept(final java.io.File dir, final String name) {
                return name.endsWith("properties");
            }
        });
    }
    
    /** gets the properties associated with the property label */
    private Properties[] getOgProps(final String[] names) {
        final int limit = names.length;
        final Properties[] props = new Properties[limit];
        for(int i = 0; i < limit; i++) {
            try {
                props[i] = org.genepattern.server.util.PropertyFactory.getInstance().getProperties(names[i]);
            } catch (Exception ex) {
                org.genepattern.gpge.GenePattern.showError(null, "Cannot get the properties for "+names[i], ex);
            }
        }
        return props;
    }
    
    /** saves the properties */
    private void saveProperties() throws java.io.IOException {
        final File og_dir = new File(System.getProperty("omnigene.conf"));
        // save the gp props
        org.genepattern.util.GPpropertiesManager.saveGenePatternProperties();
        
        // save the OmniGene props
        final String[] names = getOgPropNames();
        final Properties[] props = getOgProps(names);
        final int limit = names.length;
        for(int i = 0; i < limit; i++) {
            final FileOutputStream og_out = new FileOutputStream(new File(og_dir, names[i]));
            props[i].store(og_out, null);
            og_out.close();
        }
    }
    /** closes the window that this is contained in */
    private void closeWindow() {
        //first have the property editors stop editing
        for(int i = property_panels.size() - 1; i >= 0; i--) {// rev. loop
            final PropertyViewerPanel panel = 
                                    (PropertyViewerPanel)property_panels.get(i);
            panel.stopCellEditing();
        }
        // dispose of the window 
        final java.awt.Window window = javax.swing.SwingUtilities.getWindowAncestor(this);
        window.dispose();
    }
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        tabbedPane = new javax.swing.JTabbedPane();
        save_button = new javax.swing.JButton();
        close_button = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        tabbedPane.setName("Main");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(tabbedPane, gridBagConstraints);

        save_button.setText("Save");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.weightx = 0.5;
        add(save_button, gridBagConstraints);

        close_button.setText("Close");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.weightx = 0.5;
        add(close_button, gridBagConstraints);

    }//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JButton close_button;
    private javax.swing.JButton save_button;
    // End of variables declaration//GEN-END:variables
    private final List property_panels;
    // I N N E R  C L A S S E S 
    class  GPpropAcessor extends org.genepattern.util.GPpropertiesManager {
        Properties getProperties() {
            return getInternal();
        }
    }
}
