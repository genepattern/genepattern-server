/*
 * LegendTable.java
 *
 * Created on February 19, 2002, 1:54 PM
 */

package org.genepattern.modules.ui.graphics;

import javax.swing.JComponent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.genepattern.data.LegendTableModel;
//import edu.mit.genome.gp.GenePattern;
/**
 * This class together with the LegendTableModel allows the column headers 
 * to be self documenting.  Thus if the table is too big for the space allocated
 * to it the column header labels become unreadable.  
 * If the user's mouse lingers over the header a tool tip will appear indicating
 * the label.
 * Likewise, the cells in the table have description tool tips that are displayed
 * when the user's mouse lingers over them.
 *
 * @author  KOhm
 * @version 0.1
 * @see LegendTableModel
 */
public class LegendTable extends AutoResizeTable {

    /** Creates new LegendTable */
    public LegendTable () {
        super(false);
    }
    /** Creates new AutoResizeTable */
    public LegendTable (boolean xcv_modifiable) {
        super(xcv_modifiable);
    }
    /** Creates new LegendTable */
    public LegendTable (LegendTableModel tableModel) {
        //super(tableModel);
        setModel(tableModel);
    }
//    /** Creates new LegendTable */
//    public LegendTable (Object[][] data, String[] cols) {
//        super(data, cols);
//    }
    
    /** besides setting the model, sets the tool tips for the header */
    public void setModel(LegendTableModel model) {
        super.setModel (model);
        setHeaderToolTips(model);
        //setHeaderToolTips(model.getColumnDescriptions());
        //setHeaderToolTips(model.getColumnNames());
        //setColumnToolTips(model.getColumnDescriptions());
    }
    /** overrides the subclass */
//    public void setModel(TableModel model) {
//        if(!model instanceof LegendTableModel)
//            throw new IllegalArgumentException("The model must be a subclass of LegendTableModel:\n"+model);
//        LegendTableModel ltm = (LegendTableModel)model;
//        setModel(ltm);
//    }
    /**
     * Sets the header tool tips 
     * The index of the array of text corresponds to each column
     *
     */
    private final void setHeaderToolTips(final LegendTableModel model) {
        TableColumnModel tcm = this.getColumnModel ();
        TableCellRenderer header_render = null;
        TableColumn tc = null;
        final int limit = this.getColumnCount ();
        for(int col = 0; col < limit; col++) {
            tc = tcm.getColumn (col);
            header_render = tc.getHeaderRenderer ();
            if(header_render == null) {
                header_render = header_renderer_creator.createHeaderCellRenderer ();
                tc.setHeaderRenderer (header_render);
            }
            if(header_render instanceof JComponent) {
                ((JComponent)header_render).setToolTipText (model.getColumnDescription(col));
            } else 
                org.genepattern.util.AbstractReporter.getInstance().logWarning("Warning: Header Renderer cannot have tool tips-"+header_render);
        }
        //System.out.println("tcm="+tcm+" header_render="+header_render+" tc="+tc);
    }
    /**
     * Sets the column tool tips where a column specific tool tip will 
     * appear when lingering over a cell.
     * The index of the array of text corresponds to each column
     *
     */
    private final void setColumnToolTips(String[] tips) {    
       //default_hr = getTableHeader().getDefaultRenderer ();
        DefaultTableCellRenderer renderer;
        TableColumnModel tcm = this.getColumnModel ();
        TableColumn tablecolumn = null;
        for(int col = 0, limit = this.getColumnCount (); col < limit; col++) {
            tablecolumn = tcm.getColumn (col);
//            renderer = tablecolumn.getCellRender(); // prevent excess object creation
//            if(renderer == null || renderer == this.getDefaultRenderer (dataModel.getColumnClass (col)))
            renderer = new DefaultTableCellRenderer ();
            renderer.setToolTipText (tips[col]);
            tablecolumn.setCellRenderer (renderer);
        }
    }
    
    // begin fields
    /** this class simply creates Header renderers */
    private static final HeaderCellRendererCreator header_renderer_creator = new HeaderCellRendererCreator();
    
    // inner classes
    /**
     * A JTableHeader class that simply can generate DefaultTableCellRenderers 
     * from the default protected createDefaultColumnModel().  
     * This means that even if the code changes this class will still get the 
     * proper renderer for displaying the Header cells.
     */
    static public class HeaderCellRendererCreator extends JTableHeader {
        
        /**  Constructs a JTableHeader with a default TableColumnModel. JTableHeader(TableColumnModel cm) */
        public HeaderCellRendererCreator () {
            super();
        }
        /** Constructs a JTableHeader which is initialized with cm as the column model. */
        public HeaderCellRendererCreator (TableColumnModel cm) {
            super(cm);
        }
        /**
         * This method just maps to the subclasses protected 
         * createDefaultRenderer().  
         * Allowing for changes in the code in the future that cause the 
         * Header cells to look different.
         */
        public TableCellRenderer createHeaderCellRenderer() {
            return this.createDefaultRenderer ();
        }
    }
}
