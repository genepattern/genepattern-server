/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.data.expr;

/**
 * An interface for expression data.
 * 
 * @author Joshua Gould
 */
public interface IExpressionData extends IMatrix {

    /**
     * Gets the value at the given row and column as a string.
     * 
     * @param row
     *            The row
     * @param column
     *            The column
     * @return The expression value
     */
    public String getValueAsString(int row, int column);

    /**
     * Gets the metadata name at the given index
     * 
     * @param index
     *            The index
     * @return The metadata name at the given <tt>index</tt>
     * 
     */
    public String getDataName(int index);

    /**
     * Gets the number of data names
     * 
     * @return The number of data names
     */
    public int getDataCount();

    /**
     * Gets the data at the given row and column with the given name
     * 
     * @param row
     *            The row
     * @param column
     *            The column
     * @param name
     *            The name for the data
     * @return The value at the given row and column
     */
    public Object getData(int row, int column, String name);

    /**
     * Gets the row metadata name at the given index
     * 
     * @param index
     *            The index
     * @return The metadata name at the given <tt>index</tt>
     * 
     */
    public String getRowMetadataName(int index);

    /**
     * Gets the number of row meta data names
     * 
     * @return The number of names
     */
    public int getRowMetadataCount();

    /**
     * Gets the column metadata name at the given index
     * 
     * @param index
     *            The index
     * @return The metadata name at the given <tt>index</tt>
     * 
     */
    public String getColumnMetadataName(int index);

    /**
     * Gets the number of column meta data names
     * 
     * @return The number of names
     */
    public int getColumnMetadataCount();

    /**
     * Gets the row metadata at the given row for this name
     * 
     * @param row
     *            The row
     * @param name
     *            The metadata name
     * @return The metadata at the given row
     */
    public String getRowMetadata(int row, String name);

    /**
     * Gets the column metadata at the given column for this name
     * 
     * @param column
     *            The column
     * @param name
     *            The metadata name
     * @return The metadata at the given column
     */
    public String getColumnMetadata(int column, String name);
}
