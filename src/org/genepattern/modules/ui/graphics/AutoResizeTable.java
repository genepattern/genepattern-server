/*
 * AutoResizeTable.java
 *
 * Created on December 13, 2001, 1:16 PM
 */

package org.genepattern.modules.ui.graphics;
/**
 *
 * @author  KOhm
 * @version 
 */
public class AutoResizeTable extends BaseTable {

    /** Creates new AutoResizeTable */
    public AutoResizeTable () {
        super(false);
    }
    /** Creates new AutoResizeTable */
    public AutoResizeTable (boolean xcv_modifiable) {
        super(xcv_modifiable);
    }
    /** Creates new AutoResizeTable */
    public AutoResizeTable (javax.swing.table.TableModel tableModel) {
        super(tableModel);
    }
//    /** Creates new AutoResizeTable */
//    public AutoResizeTable (Object[][] data, String[] cols) {
//        super(data, cols);
//    }
    /** overriden method-resizes the columns based on the contents of the model */
    public void setModel (javax.swing.table.TableModel tableModel) {
        if( tableModel != null) {
            super.setModel(tableModel);
            initColumnSizes(getExampleWidths(50));
        }
    }
    /**
     * determines the maximum widths for each of the columns as represented by
     * an array of Strings.
     *
     * @param max the maximum number of characters wide a column can be
     * @return a an array of Strings
     */
    public final String[] getExampleWidths(int max) {
        String[] examples = new String[getColumnCount()];
        for(int c = 0, num_cols = getColumnCount(); c < num_cols; c++) {
            int current_width = 0;
            String col_example = null;
            for(int r = 0, num_rows = getRowCount(); r < num_rows; r++) {
                Object obj = getValueAt(r, c);
                String cell = (obj != null ? obj.toString() : "");
                if(cell.length() > current_width) {
                    current_width = cell.length();
                    col_example = cell;
                }
            }
            if(col_example == null){
                col_example = "";
            } else if(col_example.length() > max) {
                col_example = col_example.substring (0, max);
            }
            examples[c] = col_example;
        }
        return examples;
    }
}
