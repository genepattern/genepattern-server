/*
 * TemplatePropertiesWrapper.java
 *
 * Created on May 9, 2003, 4:06 PM
 */

package org.genepattern.io.encoder;

import java.util.Map;
import java.util.Collections;

import org.genepattern.data.AbstractObject;
import org.genepattern.data.DataModel;
import org.genepattern.data.FeaturesetProperties;
import org.genepattern.data.Template;

import org.genepattern.io.*;
/**
 * Adaptor class that makes a Template look like a FeaturesetProperties object.
 * @author  kohm
 */
public class TemplatePropertiesWrapper extends AbstractObject implements FeaturesetProperties {
    
    /** Creates a new instance of TemplatePropertiesWrapper */
    public TemplatePropertiesWrapper(final Template template) {
        super(template.getName());
        this.template = template;
        attributes = new java.util.HashMap(3);
        attributes.put("Numeric=", String.valueOf(template.isNumeric()));
        
        final StringBuffer buf = new StringBuffer();
        final int limit = template.getKlassCount();
        for(int i = 0; i < limit; i++) {
            buf.append(template.getKlass(i).getName());
            buf.append('\t');
        }
        attributes.put("KLASSES:", buf.toString());
    }
    
    public void addTableModelListener(javax.swing.event.TableModelListener tableModelListener) {
    }
    
    // The misc other properties of the specific implementation model.
    public Map getAttributes() {
        return attributes;
    }
    
    public Class getColumnClass(int col) {
        //FIXME last col could be a number
        return String.class;
    }
    
    public int getColumnCount() {
        return COL_DESCS.length;
    }
    
    //the description of the column 
    public String getColumnDescription(final int columnIndex) {
        return COL_DESCS[columnIndex];
    }
    
    // sets the specified array values to those of the Column Descriptions
    // if the specified array is null creates a new String[]
    public String[] getColumnDescriptions(final String[] array) {
        if( array == null )
            return (String[])COL_DESCS.clone();
        System.arraycopy(COL_DESCS, 0, array, 0, COL_DESCS.length);
        return array;
    }
    
    public String getColumnName(final int col) {
        return COL_NAMES[col];
    }
    
    // sets the array values to those of the Column Names
    // if the specified array is null creates a new String[]
    public String[] getColumnNames(String[] array) {
        if( array == null )
            return (String[])COL_NAMES.clone();
        System.arraycopy(COL_NAMES, 0, array, 0, COL_NAMES.length);
        return array;
    }
    
    // returns a DataModel that defines the type of model this implementation represents 
    public DataModel getDataModel() {
        return Template.DATA_MODEL;
    }
    
    /** returns the Model that this data object represents  */
    public String getModel() {
        return (String)Template.DATA_MODEL.value;
    }
    
    public int getRowCount() {
        return template.getItemCount();
    }
    
    /** gets the specified row description if none then returns null  */
    public String getRowDescription(final int row) {
        return "";
    }
    
    // sets the array values to those of the Row Descriptions
    // if the specified array is null creates a new String[]
    public String[] getRowDescriptions(String[] array) {
        return null;
    }
    
    // gets the name of the specified row if none then returns null 
    public String getRowName(final int row) {
        return template.getItem(row).getName();
    }
    
    // sets the array values to those of the Row Names
    // if the specified array is null creates a new String[]
    public String[] getRowNames(final String[] array) {
        final int limit = template.getItemCount();
        for(int i = 0; i < limit; i++) {
            array[i] = template.getItem(i).getName();
        }
        return array;
    }
    
    public Object getValueAt(final int row, final int col) {
        switch(col) {
            case 0:
                return template.getItem(row).getSampleLabel();
            case 1:
                return template.getItem(row).getName();
            default:
                throw new ArrayIndexOutOfBoundsException("Column index ("+col+") out of bounds must be 0 - "+getColumnCount()+"!");
        }
    }
    
    // returns true if there are column descriptions 
    public boolean hasColumnDescriptions() {
        return true;
    }
    
    // returns true if there are row descriptions specified 
    public boolean hasRowDescriptions() {
        return false;
    }
    
    // returns true if there are row names specified 
    public boolean hasRowNames() {
        return false;
    }
    
    public boolean isCellEditable(int row, int col) {
        return false;
    }
    
    public void removeTableModelListener(javax.swing.event.TableModelListener tableModelListener) {
    }
    
    public void setValueAt(Object obj, int param, int param2) {
        throw new UnsupportedOperationException();
    }
    // overidden Object methods
    
    // this is a reminder that data objects must override toString()
    public String toString() {
        return template.toString();
    }
    // this is a reminder that data objects must override equals(Object)
    public boolean equals(final Object obj) {
        if( obj instanceof DatasetPropertiesWrapper ) {
            return template.equals( ((TemplatePropertiesWrapper)obj).template );
        }
        return false;
    }

    public int hashCode(){
        return template.hashCode();
    }
    
    // fields
    /** the descriptions */
    protected static final String[] COL_DESCS = new String[] {"The Label of the sample", "Identification value"};
    /** the descriptions */
    protected static final String[] COL_NAMES = new String[] {"Sample", "ID"};
    /** the Template object this is an adaptor for */
    private final Template template;
    /** the attributes */
    private final Map attributes;
}
