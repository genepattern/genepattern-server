/*
 * DatasetView.java
 *
 * Created on August 7, 2002, 5:16 PM
 */

package org.genepattern.data;


/**
 * An immutable view of another dataset, requires an immutable Dataset.
 *
 * @author  kohm
 */
public class DatasetView extends org.genepattern.data.AbstractDataset {

    /** Creates a new instance of DatasetView */
    public DatasetView(Dataset dataset, String name, int[] row_index, int[] col_index) {
        this(dataset, name, dataset.getMatrix(), false, row_index, col_index);
    }
    /**
     * the master constructor that should be used by all other constructors 
     * of this class and its' subclasses
     */
    protected DatasetView(final Dataset dataset, final String name, final Matrix matrix, final boolean mutable, int[] row_index, int[] col_index) {
        // perhaps a helper constructor class would simplify this?!?
        super(createName(name, dataset), matrix, mutable,
            createNamesPanel(dataset.getRowPanel(),    row_index = getIndices(row_index, dataset.getRowCount(), true)),
            createNamesPanel(dataset.getColumnPanel(), col_index = getIndices(col_index, dataset.getColumnCount(), false)));
        // Note may have to preprocess the matrix using static helper method see processMatrix(Matrix)
        if(row_index == null && col_index == null && !mutable)
            throw new IllegalArgumentException("Identical view of the dataset");
//        if(dataset.isMutable()) // or create a sub set of matrix right away
//            throw new IllegalArgumentException("The Dataset must be immutable");
        this.parent     = dataset;
        this.row_index  = row_index;
        this.col_index  = col_index;
        
        // processing the matrix 
        // see processMatrix(Matrix)
        final Matrix other_matrix = dataset.getMatrix();
        if(other_matrix != null && other_matrix.isMutable())
            this.matrix = createMatrix(other_matrix);
//        if(!mutable)
//            finalCheck(num_rows, num_cols);
    }
    /** helper */
    private static final String createName(final String name, final Dataset dataset) {
        if(dataset == null)
            throw new NullPointerException("Dataset cannot be null");
        return (name != null) ? name : "View of "+dataset.getName();
    }
    /** helper */
    private static final NamesPanel createNamesPanel(final NamesPanel old, final int[] indices) {
        return (indices == null) ? old : old.createSubSet(indices);
    }
    /** helper for constructor */
    private static final int[] getIndices(final int[] indices, final int max, final boolean is_row) {
        if(indices != null) {
            final int cnt = indices.length;
            if(cnt == 0)
                throw new IllegalArgumentException("zero length "+(is_row ? "row": "column")+" index array");
            checkIndicies(cnt, indices, max, is_row);
            return (int[])indices.clone(); // returns a copy of the array
        } else 
            return matchIndex(max);
    }
    
    /** helper method */
    protected static final int[] matchIndex(final int size) {
        final int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = i;
        }
        return array;
    }
    /** helper method for getIndices() */
    protected static final void checkIndicies(final int len, final int[] indices, final int max_parent, final boolean is_row) {
        for(int i = len - 1; i >= 0; i--) { //rev. loop
            final int val = indices[i];
            if(val >= max_parent || val < 0) {
                final String rc = (is_row ? "row" : "column");
                throw new IndexOutOfBoundsException("Index array: number of "+rc+"s in parent ("+
                max_parent+")  index is out of bounds - "+rc+"["+i+"]="+val);
            }
        }
    }
    
    // implemented abstract methods 
    
    /** creates a clone of this instance */
    public Object clone() {
        final DatasetView dataset = new DatasetView(this.parent,
                       this.getName()+"_clone", this.row_index, this.col_index);
        return dataset;
    }
    /** Must be called by all public instance methods of this abstract class.
     * @throws IllegalStateException if the class has not been initialized properly
     *
     */
    protected final void checkInit() {
    }
    /**
     * checks that the matrix dimensions are consistent with
     * the row and column names sizes
     * This method should be called in the subclasses constructor 
     * after setting all the variables.
     */
    protected void finalCheck(final int rows, final int cols) {
        if (row_index != null && col_index != null)
            super.finalCheck(rows, cols);
    }
    /** subclasses can decide what to do if the matrix is mutable for example:
     * Nothing just return it (possibly for mutable Datasets)
     * Create a new mutable matrix
     * Create a DefaultMatrix
     *
     */
    protected Matrix processMatrix(Matrix matrix) {
        // if the matrix is muttable then return null
        // since it is not possible to create the sub matrix right away
        // problem is may not have a handle on row_index, and col_index yet
        return (matrix.isMutable()) ? null : matrix;
    }
    /** subclasses can decide what to do if the NamesPanel is mutable for example:
     * Nothing just return it (possibly for mutable Datasets)
     * Create a new mutable NamesPanel
     * Create an immutable NamesPanel (Immutable Datasets with mutable NamesPanels)
     *
     * Note the NamesPanel is preprocessed in the constructor
     */
    protected NamesPanel processNamesPanel(final NamesPanel panel) {
        return panel;
    }
    /** The object id
     */
    public Id getId() {
        return null;
    }
    
    // begin regular methods (ones that don't help the constructor)
    
    /**
     * creates a new String array that maps the values in the input String[]
     * into the new array via the indices array
     */
    protected final static String[] createMappedStringArray(final String[] names, final int[] indices) {
        final int num = indices.length;
        final String [] temp = new String[num];
        for(int i= 0; i< num; i++) {
            temp[i] = names[indices[i]];
        }
        return temp;
    }
    /** returns the parent Dataset */
    public Dataset getParent() {
        return parent;
    }
    // Dataset methods helper accessor methods
    
    /** gets the matrix, creating it first if needed */
    protected final Matrix internalGetSubMatrix() {
        return matrix;
    }
    private final Matrix createMatrix(final Matrix matrix) {
        return matrix.createSubMatrix(row_index, col_index);
    }
    /** for subclasses only */
    protected final void internalSetRowIndices(final int[] indices) {
        if(isMutable())
            this.row_index = indices;
        else
            throw new UnsupportedOperationException("Not mutable! Cannot set the row indices.");
    }
    /** gets the row indices array */
    protected final int[] internalGetRowIndices() {
        return row_index;
    }
    /** for subclasses only */
    protected final void internalSetColumnIndices(final int[] indices) {
        if(isMutable())
            this.col_index = indices;
        else
            throw new UnsupportedOperationException("Not mutable! Cannot set the row indices.");
    }
    /** gets the row indices array */
    protected final int[] internalGetColumnIndices() {
        return col_index;
    }
    // Dataset signature methods
        
    public final float getElement(final int row, final int column) {
        if(matrix == null)
            return parent.getElement(row_index[row], col_index[column]);
        else
            return matrix.getElement(row, column);
    }
    
    /** generally should return immuted matrix
     *the Matrix is where the view should be implemented
     */
    public Matrix getMatrix() {
        if(matrix == null) {
            matrix = createMatrix(parent.getMatrix());
        }
        return matrix;
    }
    
    public final int getRowCount() {
        return row_index.length;
    }
    
    public int getColumnCount() {
        return col_index.length;
    }
    
    // fields
    
    protected final Dataset parent;
    private int[] col_index, row_index;
    private Matrix matrix;

}
