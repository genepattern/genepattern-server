/*
 * PropertiesTable.java
 *
 * Created on January 20, 2003, 2:13 PM
 */

package org.genepattern.modules.ui.odfviewer;

/**
 *
 * @author  kohm
 */
public class PropertiesTable extends org.genepattern.modules.ui.graphics.LegendTable {
    
    /** Creates a new instance of PropertiesTable */
    public PropertiesTable() {
        super();
    }
    /** Creates a new instance of PropertiesTable */
    public PropertiesTable(final org.genepattern.data.FeaturesetProperties model) {
        //super(model);
        super();
        setModel(model);
    }
    
    /**
     * Creates default cell renderers for objects, numbers, doubles, dates,
     * booleans, and icons.
     * @see javax.swing.table.DefaultTableCellRenderer
     *
     */
    protected void createDefaultRenderers() {
        super.createDefaultRenderers();
        // primitives
        setLazyRenderer(java.lang.Byte.TYPE, "javax.swing.table.DefaultTableCellRenderer");
        setLazyRenderer(java.lang.Short.TYPE, "javax.swing.table.DefaultTableCellRenderer");
        setLazyRenderer(java.lang.Integer.TYPE, "javax.swing.table.DefaultTableCellRenderer");
        setLazyRenderer(java.lang.Long.TYPE, "javax.swing.table.DefaultTableCellRenderer");
        
        setLazyRenderer(java.lang.Boolean.TYPE, "javax.swing.table.DefaultTableCellRenderer");
        
        setLazyRenderer(java.lang.Character.TYPE, "javax.swing.table.DefaultTableCellRenderer");
        
        setLazyRenderer(java.lang.Float.TYPE, "javax.swing.table.DefaultTableCellRenderer");
        setLazyRenderer(java.lang.Double.TYPE, "javax.swing.table.DefaultTableCellRenderer");
        
        
//        setLazyRenderer(java.lang.TYPE, "javax.swing.table.DefaultTableCellRenderer");
//        setLazyRenderer(java.lang.TYPE, "javax.swing.table.DefaultTableCellRenderer");
        
        
    }
    private void setLazyValue(java.util.Hashtable h, Class c, String s) {
	h.put(c, new javax.swing.UIDefaults.ProxyLazyValue(s));
    }

    private void setLazyRenderer(Class c, String s) {
	setLazyValue(defaultRenderersByColumnClass, c, s);
    }
}
