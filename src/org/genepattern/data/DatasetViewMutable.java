/*
 * DatasetView.java
 *
 * Created on August 7, 2002, 5:16 PM
 */

package org.genepattern.data;


/**
 *  This class represents a mutable version of the DatasetView.
 * 
 * FIXME not implemented yet - should subclass DatasetView.
 *
 * @author  kohm
 */
public class DatasetViewMutable extends DatasetView/*org.genepattern.data.AbstractDataset*/ {

    /** Creates a new instance of DatasetView */
    public DatasetViewMutable(Dataset dataset) {
        this(dataset, createName(dataset));
    }
    /** Creates a new instance of DatasetView */
    public DatasetViewMutable(final Dataset dataset, final String name) {
        this(dataset, name, dataset.getMatrix(), null, null);
    }
    /** Creates a new instance of DatasetView */
    public DatasetViewMutable(final Dataset dataset, final String name,
    final Matrix matrix, final int[] rows, final int[] columns) {
        super(dataset, name, matrix, true/*is mutable*/, rows, columns);
    }
    
    /** produces a clone of this object */
    public Object clone() {
        final DatasetViewMutable dataset = new DatasetViewMutable(this.getParent(),
            this.getName()+"_clone", this.getMatrix(), this.internalGetRowIndices(),
            this.internalGetColumnIndices());
        return dataset;
    }
    /** creates a name from the parent dataset */
    protected static final String createName(Dataset dataset) {
        final String name = dataset.getName();
        if(name != null) {
            return name+"_temp";
        }
        return null;
    }
    
    // methods that allow configuring 

    /** maps this dataset's columns to be in the same order as the other's */
    public void mapColumnsTo(Dataset other) {
        // FIXME this is very slow 
        final int num = other.getColumnCount();
        final int[] col_index = new int[num];
        for(int i = 0; i < num; i++) {
            col_index[i] = parent.getColumnPanel().getIndex(other.getColumnName(i));
        }
        internalSetColumnIndices(col_index);
    }
    /** fixes the rows to be same as the parent dataset's */
    public final void matchParentsRows() {
        internalSetColumnIndices(matchIndex(parent.getRowCount()));
    }
    /** fixes the columns to be same as the parent dataset's */
    public final void matchParentsColumns() {
        internalSetColumnIndices(matchIndex(parent.getColumnCount()));
    }
    
    /** only allows the one row to be viewed */
    public final void exposeOnlyRow(final int r) {
        int[] row_index = internalGetRowIndices();
        if(row_index == null || row_index.length != 1) {
            row_index = new int[1];
            internalSetRowIndices(row_index);
        }
        row_index[0] = r;
    }
    /** only uses the rows defined by the array and in the order of the array */
    public final void useOnlyRows(final int[] rows) {
        final int max_rows = parent.getRowCount();
        internalSetRowIndices(createIndices(rows, max_rows, true/*row*/));
    }
    /** only uses the columns defined by the array and in the order of the array */
    public final void useOnlyColumns(final int[] columns) {
        final int max_cols = parent.getColumnCount();
        internalSetColumnIndices(createIndices(columns, max_cols, false/*column*/));
    }
    private final int[] createIndices(final int[] indices, final int max_parent, final boolean is_row) {
        final int len = indices.length;
        //check to see if there will be an array out of bounds
        for(int i = len - 1; i >= 0; i--) { //rev. loop
            final int val = indices[i];
            if(val >= max_parent || val < 0) {
                final String rc = (is_row ? "row" : "column");
                throw new IndexOutOfBoundsException("Index array: number of "+rc+"s in parent ("+
                max_parent+")  index is out of bounds - "+rc+"["+i+"]="+val);
            }
        }
        final int[] new_index = new int[len];
        System.arraycopy(indices, 0, new_index, 0, len);
        return new_index;
    }
    /** restricts the number of columns available */
    public final void useOnlyColumnsDefinedBy(FeatureSet featureset) {
        throw new UnsupportedOperationException();
    }
    
    // abstract methods from AbstractDataset
    
//    /** Must be called by all public instance methods of this abstract class.
//     * @throws IllegalStateException if the class has not been initialized properly
//     *
//     */
//    protected void checkInit() {
//    }
    
    /** The object id  */
    public Id getId() {
        return null; // FIXME should return an Id
    }
    
//    /** subclasses can decide what to do if the matrix is mutable for example:
//     * Nothing just return it (possibly for mutable Datasets)
//     * Create a new mutable matrix
//     * Create a DefaultMatrix
//     *
//     * in this case just constructs a new MatrixMutable from the specified matrix
//     */
//    protected Matrix processMatrix(Matrix matrix) {
//        return new MatrixMutable(matrix);
//    }
//    /** creates a new NamesPanelMutable from the NamesPanel */
//    protected NamesPanel processNamesPanel(final NamesPanel panel) {
//        return new NamesPanelMutable(panel);
//    }
    
    // Dataset signature methods
//    
//    public final int getColumnIndex(final String colname) {
//        final int j = parent.getColumnIndex(colname);
//        return findIndexIn(col_index, j);
//    }
//    private final int findIndexIn(final int[] indices, final int j) {
//        if(j < 0 || j == indices[j])
//            return j;
//        final int limit = indices.length;
//        for (int i = 0; i < limit; i++) {
//            if(j == indices[i])
//                return i;
//        }
//        return -1;
//    }
//    
//    
//    public final String getColumnName(final int coln) {
//        return parent.getColumnName(col_index[coln]);
//    }
//    
//    /** generally should return unmodifiable list
//     */
//    public final String[] getColumnNames() {
//        throw new UnsupportedOperationException();
//        //return null;
//    }
//    
//    public final String[] getColumnNamesArray() {
//        throw new UnsupportedOperationException();
//        //return null;
//    }
//    
//    public final FloatVector getColumn(final int coln) {
//        throw new UnsupportedOperationException();
//        //return parent.getColumn(col_index[coln]);
//    }
//    
//    public int getColumnCount() {
//        return col_index.length;
//    }
//    
//    public final float getElement(final int row, final int column) {
//        return parent.getElement(row_index[row], col_index[column]);
//    }
//    
//    /** The object id
//     */
//    public final Id getId() {
//        return null;
//    }
//    
//    /** generally should return immuted matrix
//     *the Matrix is where the view should be implemented
//     */
//    public Matrix getMatrix() {
//        throw new UnsupportedOperationException();
//        //return null;
//    }
//    
//    public final String getName() {
//        if(name == null)
//            name = "View of "+parent.getName();
//        return name;
//    }
//    
//    public final FloatVector getRow(final int rown) {
//        throw new UnsupportedOperationException();
//        //return parent.getRow(row_index[rown]);
//    }
//    
//    public final int getRowCount() {
//        return row_index.length;
//    }
//    
//    public final int getRowIndex(final String rowname) {
//        final int j = parent.getColumnIndex(rowname);
//        return findIndexIn(row_index, j);
//    }
//    
//    public final String getRowName(final int rown) {
//        return parent.getRowName(row_index[rown]);
//    }
//    
//    /** generally should return unmodifiable list
//     */
//    public final String[] getRowNames() {
//        throw new UnsupportedOperationException();
//        //return null;
//    }
//    
//    public final String[] getRowNamesArray() {
//        throw new UnsupportedOperationException();
//        //return null;
//    }
//    /** returns true */
//    public final boolean isMutable() {
//        return true;
//    }
    
    // setter methods
    
    /** sets the name of this Dataset */
    public final void setName(String name) {
        this.internalSetName(name);
    }
//    /** sets the Annotator  */
//    public void setRowAnnotator(Annotator annot) {
//        this.internalSetRowAnnotator(annot);
//    }
//    
//    /** sets the Column Annotator  */
//    public void setColumnAnnotator(Annotator annot) {
//        this.internalSetColumnAnnotator(annot);
//    }
    
    /** sets the column panel, which is where the column names,
     * descriptions/annotation are...
     *
     */
    public void setColumnPanel(NamesPanel panel) {
        this.internalSetColumns(panel);
    }
    
    /** sets the row panel, which is where the row names,
     * descriptions/annotation are...
     *
     */
    public void setRowPanel(NamesPanel panel) {
        this.internalSetRows(panel);
    }
    
    // end setters
    
    // fields

}
