/*
 * AttributesViewer.java
 *
 * Created on February 11, 2003, 3:06 PM
 */

package org.genepattern.modules.ui.odfviewer;

import java.awt.GridBagConstraints;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.genepattern.data.FeaturesetProperties;
import org.genepattern.io.Remark;


/**
 * This panel displays FeaturesetProperties' attributes that are not part of the
 * main table of data or attributes that are not defined.
 * FIXME need to have a way to identify known attributes and display them differently
 *   this fix needs to be expandable without subclassing.  Add somthing like 
 *   parser component displayers
 * ENDFIXME
 * @author  kohm
 */
public class AttributesViewer extends javax.swing.JPanel {
    
    /** Creates new form AttributesViewer */
    public AttributesViewer() {
        initComponents();
    }
    
    /** helper for figuring out the layout */
    private static final int calcColumnCount(final int total_num) {
        final int sqr = (int)Math.sqrt((float ) total_num);
        if( total_num == (sqr * sqr) )
            return sqr;
        return sqr + 1;
    }
    
    /** sets the attributes from the FeaturesetProperties 
     * that will be displayed
     * <PRE>The pre tag ** test ** </PRE>
     * FIXME here is where the FeaturesetProperties' Model could be used as
     *  a trigger for displaying certain attributes in a alternate and more specific 
     *  manner
     * ENDFIXME
     * @param attributes a mapping of keywords to values
     */    
    public void setAttributes(final FeaturesetProperties props) {
        setAttributes(props.getAttributes());
    }
    /** sets the attributes that will be displayed
     * <PRE>The pre tag ** test ** </PRE>
     * @param attributes a mapping of keywords to values
     */    
    public void setAttributes(final Map attributes) {
        this.removeAll();
        if(attributes.size() == 0) 
            return;
        
        // preserve the Map
        final Map attrs = new HashMap(attributes);
        final Object rems = attrs.get(org.genepattern.io.encoder.FeaturesetPropertiesEncoder.KW_REMARK);
        final Map remarks = createRemarksMap(rems);
        attrs.remove(org.genepattern.io.encoder.FeaturesetPropertiesEncoder.KW_REMARK);
        
        final int cnt = attrs.size();
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        final Set keyset = attrs.keySet();
        //FIXME must remove the remarks and associate with the proper key
        // for it's remark
        final String[] keys = (String[])keyset.toArray(new String[cnt]);
        //FIXME instead of sorting, put the widest JComponent objs in col 0
        java.util.Arrays.sort(keys);

        final int col_cnt = calcColumnCount(cnt);
        final int row_cnt = cnt / col_cnt;
        int j = 0;
        
        for(int c = 0; c < col_cnt; c++) {
            gbc.gridx = c;
            for(int r = 0; r < row_cnt; r++) {
                gbc.gridy = r;
                final String key = keys[j++];
                final Object value = attrs.get(key);
                final Remark remark = (Remark)remarks.get(key);
                addComp(key, value, remark, gbc);
            }
        }
        gbc.gridy++;
        final int remainder = cnt - j;
        for(int i = 0; i < remainder; i++) {
            gbc.gridx = i;
            final String key = keys[j++];
            final Object value = attrs.get(key);
            final Remark remark = (Remark)remarks.get(key);
            addComp(key, value, remark, gbc);
        }
        this.revalidate();
        System.out.println("cnt="+cnt+" col_cnt="+col_cnt+" row_cnt="+row_cnt+" remainder="+remainder);
    }
    /** helper for setAttributes() */
    private void addComp(final String key, final Object value, final Remark remark, final GridBagConstraints gbc) {
        if( value instanceof Object[]) {
            this.add(createComponent(key, (Object[])value, remark), gbc);
        } else if( value instanceof Collection ) {
            this.add(createComponent(key, (java.util.Collection)value, remark), gbc);
        } else {
            this.add(createComponent(key, value, remark), gbc);
        }
    }
    
    /** creates components that display the key and its' value
     * @param key
     * @param value
     * @param comment
     * @return
     */
    protected JComponent createComponent(final String key, final Object value, final Remark comment)  {
        final JPanel panel = new JPanel();
        if( comment != null) {
            panel.setToolTipText(comment.getName());
        }
        panel.add(new JLabel(key));
        final JTextField field = new JTextField(value.toString());
        field.setEditable(false);
        panel.add(field);
        return panel;
    }
    /** creates components that display the key and its' value
     * @param key
     * @param array
     * @param comment
     * @return
     */
    protected JComponent createComponent(final String key, final Object[] array, final Remark comment)  {
        final JPanel panel = new JPanel();
        if( comment != null ) {
            panel.setToolTipText(comment.getName());
        }
        final JList list = new JList(array);
        list.setVisibleRowCount(visibleRowCount);
        panel.add(new JLabel(key));
        panel.add(new JScrollPane(list));
        return panel;
    }
    /** another helper method for setAttributes() gets a Map that associates
     * the with the key or null
     */
    private Map createRemarksMap(final Object rems) {
        if( rems == null ) {
            return java.util.Collections.EMPTY_MAP;
        } else if( rems instanceof Remark[] ) {
            final Remark[] remarks = (Remark[])rems;
            final int limit = remarks.length;
            final Map map = new HashMap((limit * 4) / 3 + 1);
            for(int i = 0; i < limit; i++) {
                final Remark remark = remarks[i];
                final String key = remark.getLocation();
                map.put(key, remark);
            }
            return map;
        } else if( rems instanceof Remark ) {
            final Remark remark = (Remark)rems;
            final String key = remark.getLocation();
            return java.util.Collections.singletonMap(key, remark);
        } else if( rems instanceof Collection ) {
            final Collection remarks = (Collection)rems;
            return createRemarksMap( remarks.toArray(new Remark[remarks.size()]) );
        } else { //error
            throw new IllegalArgumentException("The remarks value is not"
            +" of a known type!:\n"+rems);
        }
    }
    
/** creates components that display the key and its' value
 * @param key
 * @param collection
 * @param comment
 * @return
 */
protected JComponent createComponent(final String key, final Collection collection, final Remark comment)  {
    return createComponent(key, collection.toArray(), comment);
}
/** This method is called from within the constructor to
 * initialize the form.
 * WARNING: Do NOT modify this code. The content of this method is
 * always regenerated by the Form Editor.
 */
    private void initComponents() {//GEN-BEGIN:initComponents

        setLayout(new java.awt.GridBagLayout());

    }//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    /** the number of visible rows in a JList */
    private int visibleRowCount = 3;
}
