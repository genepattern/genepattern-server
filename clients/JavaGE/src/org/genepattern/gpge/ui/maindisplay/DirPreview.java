/*
 * JPanel.java
 *
 * Created on June 11, 2003, 12:02 PM
 */

package org.genepattern.gpge.ui.maindisplay;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;

/**
 *
 * @author  kohm
 */
public final class DirPreview extends javax.swing.JPanel implements PropertyChangeListener {
    
    /** Creates new form JPanel */
    public DirPreview() {
        initComponents();
    }

    /** Constructor
     * @param fc the file chooser that this is an accessory for
     * @see javax.swing.JFileChooser#setAccessory()
     */
    public DirPreview(final JFileChooser fc) {
        this();
        fc.setControlButtonsAreShown(false);
        setPreferredSize(new Dimension(200, 0));
        fc.addPropertyChangeListener(this);
        final java.awt.event.ActionListener listener = new java.awt.event.ActionListener() {
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                final JButton button = (JButton)e.getSource();
                //System.out.println("File Chooser Action "+e);
                if(button == approve_button) {
                    fc.approveSelection();
                } else if( button == cancel_button) {
                   fc.cancelSelection(); 
                } else 
                    System.err.println("Unknown button "+button);
            }
        };
        approve_button.addActionListener(listener);
        cancel_button.addActionListener(listener);
        //table = new JTable();
        //this.setViewport(table);
        approve_button.setText(fc.getApproveButtonText());
        approve_button.setMnemonic(fc.getApproveButtonMnemonic());
        load(fc.getCurrentDirectory());
    }

//    public static final void main(final String[] args) {
//        JFileChooser file_chooser = new JFileChooser();
//        file_chooser.setDialogTitle("Choose a Project Directory");
//        file_chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//        file_chooser.setApproveButtonText("Select Directory");
//        file_chooser.setAccessory(new DirPreview(file_chooser));
//        
//        final int returnVal = file_chooser.showDialog(null, "Select Project Directory");
//        final DirPreview dir_prev = (DirPreview)file_chooser.getAccessory();
//        final boolean approved = returnVal == JFileChooser.APPROVE_OPTION;
//        if(approved || (dir_prev != null && dir_prev.getLastSelected() != null) ) {
//            java.io.File file = (approved) ? file_chooser.getSelectedFile() : dir_prev.getLastSelected();
//            if( file == null )
//                file = file_chooser.getCurrentDirectory();
//            //    if( returnVal == JFileChooser.APPROVE_OPTION ) {
//            //      final java.io.File file = file_chooser.getSelectedFile();
//            System.out.println("You chose to open this file: " +
//            file.getName());
//            if( file != null ) {
//                System.out.println("Loading Project Directory...");
//            }
//        }
//    }
    /** gets the last selected directory */
    public File getLastSelected() {
        return old_dir;
    }
    
    /** loads the directory into the list
     * @param dir the file that represents a directory
     */
    public File load(final File dir) {
        if ( dir == null || !dir.isDirectory() || dir.equals(old_dir) ) {
            return null;
        }
        old_dir = dir;
        String[] file_names = dir.list(NO_DIR_FILTER);
	
	  if (file_names == null) file_names = new String[0];  // fix for bug 415		

	  final int len = file_names.length;
        final JList list = new JList(file_names);
        list.setSelectionModel(SELECTION_MODEL);
        jScrollPane1.setViewportView(list);
		
        return dir;
    }
    
    /** this method is fired when  the <CODE>JFileChooser</CODE> has
     * it's state changed
     * @param e the event
     */
    public final void propertyChange(final PropertyChangeEvent e) {
        final String prop = e.getPropertyName();
        //System.out.println(prop+" PropertyChangeEvent="+e);
        //If the directory changed, don't show an image.
        File dir = null;
        if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(prop)) {
            if (isShowing()) {
                dir = load((File) e.getNewValue());
            }
            //If a file became selected, find out which one.
        } else if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop)) {
            if (isShowing()) {
                dir = load((File) e.getNewValue());
            }
        } else if(JFileChooser.FILE_SYSTEM_VIEW_CHANGED_PROPERTY.equals(prop) ) {
            if (isShowing()) {
                dir = load((File) e.getNewValue());
            }
        }  else
            System.out.println(prop+" Other property "+e.getNewValue());
        
        if( dir != null ) 
            jLabel1.setText("Files in Directory "+dir.getName()+File.separator+':');
    }
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        approve_button = new javax.swing.JButton();
        cancel_button = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("Files:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        add(jLabel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(jScrollPane1, gridBagConstraints);

        approve_button.setText("Approve");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        add(approve_button, gridBagConstraints);

        cancel_button.setText("Cancel");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        add(cancel_button, gridBagConstraints);

    }//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton approve_button;
    private javax.swing.JButton cancel_button;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel jLabel1;
    // End of variables declaration//GEN-END:variables
    // start of programmer defined vars
    /** the previous directory */
    private File old_dir = null;
    /** the selection model that prevents selection of the items in the list */
    private static final NoSelectSelectionModel SELECTION_MODEL = new NoSelectSelectionModel();
    /** the file filter that only accepts non-dir files */
    private static final java.io.FilenameFilter NO_DIR_FILTER = new NotDirectoryFilter();
    // I N N E R   C L A S S E S
    /** JList objects that use this model will not be item-selectable, 
     * even if the selection mode is set with setSelectionMode(int).
     */
    private final static class NoSelectSelectionModel implements javax.swing.ListSelectionModel {
        
        public void addListSelectionListener(javax.swing.event.ListSelectionListener listSelectionListener) {
        }
        
        public void addSelectionInterval(int param, int param1) {
        }
        
        public void clearSelection() {
        }
        
        public int getAnchorSelectionIndex() {
            return -1;
        }
        
        public int getLeadSelectionIndex() {
            return -1;
        }
        
        public int getMaxSelectionIndex() {
            return -1;
        }
        
        public int getMinSelectionIndex() {
            return -1;
        }
        
        public int getSelectionMode() {
            return -1;
        }
        
        public boolean getValueIsAdjusting() {
            return false;
        }
        
        public void insertIndexInterval(int param, int param1, boolean param2) {
        }
        
        public boolean isSelectedIndex(int param) {
            return false;
        }
        
        public boolean isSelectionEmpty() {
            return true;
        }
        
        public void removeIndexInterval(int param, int param1) {
        }
        
        public void removeListSelectionListener(javax.swing.event.ListSelectionListener listSelectionListener) {
        }
        
        public void removeSelectionInterval(int param, int param1) {
        }
        
        public void setAnchorSelectionIndex(int param) {
        }
        
        public void setLeadSelectionIndex(int param) {
        }
        
        public void setSelectionInterval(int param, int param1) {
        }
        
        public void setSelectionMode(int param) {
        }
        
        public void setValueIsAdjusting(boolean param) {
        }
        
    }
    /** file name filter for listing only files */
    public static final class NotDirectoryFilter implements java.io.FilenameFilter {
        /** accepts only files that are not dirs */
        public boolean accept(final java.io.File dir, final String str) {
            final File file = new File(dir, str);
            return !(file.isDirectory() || file.isHidden());
        }
        
    }
}
