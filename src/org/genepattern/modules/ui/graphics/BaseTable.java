/*
 * BaseTable.java
 *
 * Created on July 10, 2001, 1:21 PM
 */

//package edu.mit.genome.expresso.ui.graphics;
package org.genepattern.modules.ui.graphics;

import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.CellEditor;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 *  Stolen from the expresso project...
 * Perhaps not needed for Java 1.4?  JTable now supports cut copy and paste.
 * @author  kohm
 * @version 
 */
public class BaseTable extends javax.swing.JTable {

    public BaseTable(boolean xcv_modifiable) {
        this(xcv_modifiable, new DefaultTableModel());
    }
    /** Creates new BaseTable */
    public BaseTable(final TableModel tm) {
       this(tm != null && !isModelNotEditable(tm), tm);
    }
    public BaseTable(final boolean xcv_modifiable, final TableModel model) {
        super(model);

        // these actions should be kept in variables for menu use
        addAction(new CutAction(xcv_modifiable));
        addAction(new CopyAction());
        if(xcv_modifiable) { 
            addAction(new PasteAction());
        }
        // setup selection to be more like that of a spreadsheet
        // cut and copy are setup for SINGLE_INTERVAL_SELECTION
        setSelectionMode (ListSelectionModel.SINGLE_INTERVAL_SELECTION);
         //SINGLE_SELECTION, SINGLE_INTERVAL_SELECTION, MULTIPLE_INTERVAL_SELECTION
        this.setCellSelectionEnabled (true);
    }
    /** helper method for the constructor
     * Note should probably check to see if this Class has the command 
     * in the InputMap before replacing it.  This would allow future versions
     * of Java's JTable to work as advertised
     */
    private final void addAction(final Action action) { 
        final String command = (String)action.getValue (Action.NAME);
        final KeyStroke[] bound_strokes = getKeyStrokesBoundToCommand (command);
        //System.out.println ();
        final InputMap inputmap = getInputMap ();
        final int limit = bound_strokes.length;
        for(int i = 0; i < limit; i++) {
            inputmap.put (bound_strokes[i], command);
        }
        getActionMap ().put (command, action);   
    }
    /** tests if the model is not readable */
    public static final boolean isModelNotEditable(final TableModel tm) {
        for(int col = tm.getColumnCount () - 1; col >= 0; col--) { //reverse loop
            if(tm.isCellEditable (0, col))
                return false;
        }
        return true;
    }
    /**
     * method for descovering the KeyStrokes bound to a command
     * Could not find a general method to this in the java/javax packages
     */
    private static final KeyStroke[] getKeyStrokesBoundToCommand (final String command) {
        final InputMap inputmap = textcomp.getInputMap ();
        final KeyStroke[] strokes = inputmap.allKeys ();
        final java.util.List command_bound_strokes_list = new java.util.ArrayList ();
        
        //System.out.println ("\nKeyStrokes for comand: "+command);//debug
        final int limit = strokes.length;
        for(int i = 0; i < limit; i++) {
            final Object key_command = inputmap.get (strokes[i]);
            if(key_command.toString ().equals (command)) {
                command_bound_strokes_list.add (strokes[i]);
                //System.out.println (strokes[i]);//debug
            }
        }
        final KeyStroke[] bound_strokes = new KeyStroke[command_bound_strokes_list.size()];
        command_bound_strokes_list.toArray (bound_strokes);
        return bound_strokes;
    }
    
    private void initComponents () {}
     
    /**
     * sets the preferred size of the columns
     *
     * @param model the table model
     * @param longValues an array of objects whos string size will set
     *                   the individual preferred column widths
     */
    protected final void initColumnSizes(final Object[] longValues) {
        final TableModel model = getModel();
//        column = null;
//        java.awt.Component comp = null;
        final int headerWidth = 0;
//        int cellWidth = 0;
        
        for (int i = 0, limit = model.getColumnCount(); i < limit; i++) {
            final TableCellRenderer tcr = getDefaultRenderer(model.getColumnClass(i));
            //System.out.println("i="+i+" longValues[i]="+longValues[i]+" class="+model.getColumnClass(i)+" Default renderer="+tcr);
            
            final TableColumn column = getColumnModel().getColumn(i);
            //System.out.println("model="+model);
            //System.out.println("longValues="+longValues);
//            final Component renderer = getTableCellRendererComponent(
//            this, longValues[i],
//            false, false, 0, i);
//            comp = getDefaultRenderer(model.getColumnClass(i)).renderer;
            
            
            final java.awt.Component comp = tcr.getTableCellRendererComponent(this, longValues[i],
                                                            false, false, 0, i);
            
            final int cellWidth = comp.getPreferredSize().width;
            
            if (false) { //debug
                System.out.println("Initializing width of column "
                + i + ". "
                + "headerWidth = " + headerWidth
                + "; cellWidth = " + cellWidth);
            }
            
            column.setPreferredWidth(Math.max(headerWidth, cellWidth));
        }
    }
    // flyover
   /**
     * Overrides <code>JComponent</code>'s <code>getToolTipText</code>
     * method in order to allow the renderer's tips to be used
     * if it has text set.
     * <p>
     * <bold>Note:</bold> For <code>JTable</code> to properly display
     * tooltips of its renderers
     * <code>JTable</code> must be a registered component with the
     * <code>ToolTipManager</code>.
     * This is done automatically in <code>initializeLocalVars</code>,
     * but if at a later point <code>JTable</code> is told
     * <code>setToolTipText(null)</code> it will unregister the table
     * component, and no tips from renderers will display anymore.
     *
     * @see JComponent#getToolTipText
     */
    public String getToolTipText(final MouseEvent event) {
        String tip = null;
        final Point p = event.getPoint();

        // Locate the cell under the event location
        final int hitColumnIndex = columnAtPoint(p);
        final int hitRowIndex = rowAtPoint(p);

        if ((hitColumnIndex != -1) && (hitRowIndex != -1)) {
            final Object obj = getValueAt(hitRowIndex, hitColumnIndex);
            if(obj != null) {
                if( obj instanceof String[] ) 
                    tip = ((DefaultTableCellRenderer)this.getDefaultRenderer(obj.getClass())).getText();
                else
                    tip = obj.toString();
            }
        }

        // Nothing there then, get our own tip
        if (tip == null)
            tip = getToolTipText();

        return tip;
    }
    
    // cut copy paste helper methods
    /**
     * conveinience method for getting a String that represents the data
     * selected in the table
     *
     * @return a String, the representation of the selected objects
     */
    public final String getSelectedText() {
        stopCellEditing();
        final int[] rows = getSelectedRows(), cols = getSelectedColumns();
        final int rlimit = rows.length, climit = cols.length;
        
        if(rlimit == 0 ||  climit == 0)
            return null;
        
        final char TAB = '\t', NL = '\n';
        //String nl = System.getProperty("path.separator");
        //if( nl != null) nl = "\n\r";
        final StringBuffer sbuf = new StringBuffer();
        
        for(int i = 0; i < rlimit; i++) {
            sbuf.append(getValueAt(rows[i], cols[0]));
            for(int j = 1; j < climit; j++) {
                sbuf.append(TAB);
                sbuf.append(getValueAt(rows[i], cols[j]));
            }
            sbuf.append(NL);
        }
        return sbuf.toString();
    }
    /**
     * stops the editing of a cell (if editing is occuring)
     */
    public final void stopCellEditing() {
        final CellEditor ce = getCellEditor();
        if(ce != null)
            ce.stopCellEditing();
    }
    /**
     * clears the selected cells
     */
    public final void clearSelectedCells() {
        final int[] rows = getSelectedRows(),
        cols = getSelectedColumns();
        for(int i = rows.length - 1; i >= 0; i--) // rev. loop
            for(int j = cols.length - 1; j >= 0; j--)
                setValueAt(null, rows[i], cols[j]);
    }
    
    // paste functionality
    /**
     * pastes the array elements into the corresponding cells
     * The first array dimension is the row and the second is the column
     *
     * @param text the array of strings to paste text[row]column]
     * @return a boolean, true if success
     */
    public final void paste(final String[][] text) throws PasteException, PasteWarning{
        final int rlimit = text.length;
        for(int r = 0; r < rlimit; r++) {
            final String[] row = text[r];
            for(int c = 0, climit = row.length; c < climit; c++) {
                addCell(row[c], r, c);
            }
        }
    }
    /**
     * pastes the array of strings into the specified column
     *
     * @param labels the array of strings to paste into the table
     * @param col the column to paste the labels into starting at row zero
     */
    public final void paste(final String[] labels, final int col) throws PasteException, PasteWarning {
        final int limit = labels.length;
        for(int r = 0; r < limit; r++) {
            addCell(labels[r], r, col);
        }
    }
    /**
     * paste the txt to the table
     *
     * @param text the text to paste
     */
    public final void paste(final String text) throws PasteException, PasteWarning{
        if(text.length() == 0)
            return;
        prepareToPaste(); 
        // the table
        final int sc = getSelectedColumn(), sr = getSelectedRow();
        final int startCol = (sc > -1 ? sc : 0), startRow = (sr > -1 ? sr : 0);
        // start pasting where cell is selected
        int row = startRow, col = startCol;
        char[] chars = text.toCharArray();
        final int limit = chars.length;
        StringBuffer cellText = new StringBuffer(limit);
        
        for (int i = 0; i < limit; i++) {
            char sndval, val =  chars[i];
            boolean newline = false;
            switch(val) {
                case '\n':// falls through
                    newline = true;
                case '\r'://platforms use one or the other to indicate next row
                    if((i + 1) < limit) {
                        sndval = chars[i + 1];
                        if ((newline && sndval == '\r') ||
                        (!newline && sndval == '\n')) // some platforms use a combo
                            i++;        // overlook that char too
                    }
                    try {
                        addCell(cellText.toString(), row, col);
                    } catch (PasteWarning e) { showWarning(e); }
                    cellText.delete(0, limit); // clears the StringBuffer
                    row++; // new line so start at beginning of next row
                    col = startCol;
                    break;
                case '\t': // indicates next cell to the right
                    try {
                        addCell(cellText.toString(), row, col); 
                    } catch (PasteWarning e) { showWarning(e); }
                    cellText.delete(0, limit); // clears the StringBuffer
                    col++;
                    break;
                default:
                    cellText.append(val);
            }
        }
        char prev = chars[chars.length - 1]; // if last char was significant
        if(prev != '\n' && prev != '\r' && prev != '\t') // then save the last
            try {
                addCell(cellText.toString(), row, col);  // string also
            } catch (PasteWarning e) { showWarning(e); }
    }
    /**
     * Another helper method for paste(String) 
     * It does nothing but rethrows the exception.
     * Subclasses could override this method to display messages, conditionaly
     * rethrow the exception, etc.
     *
     * @param e the paste exception
     * @exception PasteException is thrown if the user cancels
     */
    protected void showWarning(final PasteWarning e) throws PasteWarning{
        throw e;
    }
    /**
     * helper method for pasteToTable()-can be overridden by subclass to control
     * what can go into which column or row
     * Currently just adds the text to the cell without throwing an exception.
     *
     * @param name the name of the sample (col = 0) or scan (col >= 1)
     * @param row the number of the row
     * @param col the number of the column 
     */
    protected void addCell(final String name, final int row, final int col) throws PasteException, PasteWarning {
        setValueAt(name, row, col);
    }
    /**
     * This is called before pasting begins which allows for
     * stuff to be done before pasting
     */
    protected void prepareToPaste() { }
    
    // fields
    /** a JTextComponent for getting the paste commands  */
    private static final JTextField textcomp = new JTextField();
    /** the clipboard */
    private static final Clipboard clipboard = Toolkit.getDefaultToolkit ().getSystemClipboard ();
    
    private static ClipboardOwner defaultClipboardOwner = new ClipboardObserver();
    
    // inner classes 
    class PasteAction extends AbstractAction {
        PasteAction () {
            super(javax.swing.text.DefaultEditorKit.pasteAction);
        }
        public final void actionPerformed (ActionEvent e) {
            System.out.println ("got action to paste");
            if (isEnabled () && clipboard != null) {
                Transferable content = clipboard.getContents (this);
                if (content != null) {
                    try {
                        String dstData = (String)(content.getTransferData (DataFlavor.stringFlavor));
                        paste (dstData);
                    } catch (Exception ex) {
                        getToolkit ().beep ();
                    }
                } else {
                    getToolkit ().beep ();
                }
            }
            
//            Transferable transf = clipboard.getContents (this);
//            try {
//                Object data = transf.getTransferData (DataFlavor.getTextPlainUnicodeFlavor ());
//                if(data instanceof String)
//                    paste ((String)data);
//                else
//                    throw new RuntimeException ("Couldn't paste into table:\n"
//                    +"returned data was not a String!\n"
//                    +data.getClass ());
//            } catch (UnsupportedFlavorException ex) {
//                throw new RuntimeException ("Couldn't paste into table:\n"+ex);
//            } catch (java.io.IOException ex) {
//                throw new RuntimeException ("Couldn't paste into table:\n"+ex);
//            } catch (PasteException ex) {
//                throw new RuntimeException ("Couldn't paste into table:\n"+ex);
//            } catch (PasteWarning ex) {
//                throw new RuntimeException ("Couldn't paste into table:\n"+ex);
//            }
        }
    }
    /** */
    class CopyAction extends AbstractAction {
        CopyAction () {
            super(javax.swing.text.DefaultEditorKit.copyAction);
        }
        public final void actionPerformed (ActionEvent e) {
            System.out.println ("got action to copy");
            if (clipboard != null) {
                String srcData = getSelectedText ();
                if(srcData != null) {
                    StringSelection contents = new StringSelection (srcData);
                    clipboard.setContents (contents, defaultClipboardOwner);
                } else
                    getToolkit ().beep ();
            } else
                getToolkit ().beep ();
        }
    }
    /** */
    class CutAction extends AbstractAction {
        CutAction (boolean can_modify) {
            super(javax.swing.text.DefaultEditorKit.cutAction);
            this.can_modify = can_modify;
        }
        public final void actionPerformed (ActionEvent e) {
            System.out.println ("got action to cut");
            if (clipboard != null) {
                String srcData = getSelectedText ();
                if(srcData != null) {
                    StringSelection contents = new StringSelection (srcData);
                    clipboard.setContents (contents, defaultClipboardOwner);
                    if(can_modify)
                        clearSelectedCells();
                } else
                    getToolkit ().beep ();
            } else
                getToolkit ().beep ();
        }
        /** can the selection be cleared - can the table be modified */
        private boolean can_modify = true;
    }
    static class ClipboardObserver implements ClipboardOwner {
        
        public void lostOwnership (Clipboard clipboard, Transferable contents) {
        }
    }
}
