/*
 * DatasetTransposedView.java
 *
 * Created on August 21, 2002, 3:01 PM
 */

package org.genepattern.data;

import java.util.*;

import org.genepattern.data.annotation.AnnotationFactory;
import org.genepattern.data.annotation.Annotator;

import org.genepattern.data.*;
import org.genepattern.data.*;

/**
 * Implementation of a transposed dataset
 * @author  kohm
 */
public class DatasetTransposedView implements org.genepattern.data.Dataset {
    
    /** Creates a new instance of DatasetTransposedView */
    public DatasetTransposedView(Dataset dataset) {
        this.original  = dataset;
    }
    
    /** The object id
     */
    public Id getId() {
        if (id == null)
            id = Id.createId();
        return id;
    }
    /** gets the original Dataset */
    public Dataset getOriginalDataset() {
        return original;
    }
    /** returns a DataModel that defines the type of model this implementation represents */
    public org.genepattern.data.DataModel getDataModel() {
        return DATA_MODEL;
    }
    public String getName() {
        if(name == null)
            name = original.getName()+"_transposed";
        return name;
    }

    public int getRowCount() {
        return original.getColumnCount();
    }
    public int getColumnCount() {
        return original.getRowCount();
    }
    public FloatVector getRow(int rown) {
        return original.getColumn(rown);
    }
    public FloatVector getColumn(int coln) {
        return original.getRow(coln);
    }
    public float getElement(int row, int column) {
        return original.getElement(column, row);
    }
    /** generally should return immuted matrix
     */
    public Matrix getMatrix() {
        if(transposed_matrix == null) {
            transposed_matrix = original.getMatrix().getTransposedMatrix();
        }
        return transposed_matrix;
    }
    /** is this mutable  */
    public boolean isMutable() {
        return original.isMutable();
    }
    /** gets the row panel, which is where the row names,
     * descriptions/annotation are...
     *
     */
    public NamesPanel getRowPanel() {
        return original.getColumnPanel();
    }
    /** gets the column panel, which is where the column names,
     * descriptions/annotation are...();
     *
     */
    public NamesPanel getColumnPanel() {
        return original.getRowPanel();
    }
    public String getRowName(int rown) {
        return original.getColumnName(rown);
    }
    /** generally should return unmodifiable list
     */
    public String[] getRowNames() {
        return original.getColumnNames();
    }
    public String getColumnName(int coln) {
        return original.getRowName(coln);
    }
        /** generally should return unmodifiable list
     */
    public String[] getColumnNames() {
        return original.getRowNames();
    }
    /** Returns a new DefaultDataset a duplicate of this object */
    public Object clone() {
        return new org.genepattern.data.DefaultDataset(this, getName());
    }

    //fields
    
    /** the name */
    private String name;
    /** the transposed matrix */
    private Matrix transposed_matrix;
    /** the original dataset */
    private final Dataset original;
    /** the id */
    private Id id;
}
