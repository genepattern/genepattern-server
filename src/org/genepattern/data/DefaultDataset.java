/*
 * WHITEHEAD INSTITUTE
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2001 by the
 * Whitehead Institute for Biomedical Research.  All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever.  The Whitehead Institute can not be responsible for its
 * use, misuse, or functionality.
 */

package org.genepattern.data;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import org.genepattern.data.Id;
import org.genepattern.data.annotation.AnnotationFactory;


/**
 * Title:  enter desc
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */

public class DefaultDataset extends AbstractDataset {

//    /** static initilizer */
//    static {
//        /* Thoughts:
//         * This class can be persisted through XMLEncoder because the property
//         * names, prop_names, match the variables above.  Also the order of the
//         * variables (represented by the Strings) match one of the public constructors
//         *    -- kwo
//         */
//        //create instance of abstract class
//        final java.beans.Encoder encoder = new java.beans.Encoder() {};
//        final String[] prop_names = new String [] {"name", "matrix", "rows", "columns"};
//        // defines how to create an immutable bean - via its' constructor
//        encoder.setPersistenceDelegate(DefaultDataset.class,
//                        new java.beans.DefaultPersistenceDelegate(prop_names));       
//    }
    
    /** Class constructor
     * @param name the name of this dataset
     * @param matrix the matrix object
     * @param rows the rows names panel
     * @param columns the columns names panel
     */
    public DefaultDataset(final String name, final Matrix matrix, final NamesPanel rows, final NamesPanel columns) {
        super(name, matrix, false/*not mutable*/, rows, columns);
        //Datasets.datasetDump(this);
    }
    /** Class constructor
     * @param name the name of this
     * @param data array of data
     * @param rows the row count
     * @param columns the column count
     */
    public DefaultDataset(final String name, final float [] data, final NamesPanel rows, final NamesPanel columns) {
        super(name, new DefaultMatrix(rows.getCount(), columns.getCount(), data), false/*not mutable*/, rows, columns);
    }
    /** Class constructor
     * @param ds the dataset to copy
     * @param name the new name
     */
    public DefaultDataset(final Dataset ds, final String name) {
        this(name, check(ds).getMatrix(), (NamesPanel)ds.getRowPanel().clone(),
            (NamesPanel)ds.getColumnPanel().clone());
    }

// implemented abstract methods
    // creates a copy of this instance
    public Object clone() {
        return new DefaultDataset(this, this.getName()+"_clone");
    }
    /**
     * Must be called by all public instance methods of this class.
     * @throws IllegalStateException if the class has not been initialized properly
     */
    protected final void checkInit() { // does nothing all checking has been done
    }                                  // and the class is immutable
    
    /**
     * Creates a DefaultMatrix if the input matrix is mutable
     * otherwise returns the immutable matrix
     * @param matrix the matrix to process
     * @return Matrix
     */
    protected Matrix processMatrix(final Matrix matrix) {
        return (matrix.isMutable() ) ? new DefaultMatrix(matrix) : matrix;
    }
    /** subclasses can decide what to do if the NamesPanel is mutable for example:
     * Nothing just return it (possibly for mutable Datasets)
     * Create a new mutable NamesPanel
     * Create an immutable NamesPanel (Immutable Datasets with mutable NamesPanels)
     * @param panel The names panel to process
     * @return NamesPanel
     */
    protected NamesPanel processNamesPanel(final NamesPanel panel) {
        return (panel.isMutable()) ? new DefaultNamesPanel((AbstractNamesPanel)panel) : panel;
    } 
    
    public Id getId() {
        //checkInit();
        return null; // why??
    }   

} // End DefaultDataset
