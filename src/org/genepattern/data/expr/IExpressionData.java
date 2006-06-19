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
     * Tests whether this instance contains data with the given name
     * 
     * @param name
     *            the key for the data
     * @return <tt>true</tt> if this instance contains the data,
     *         <tt>false</tt> otherwise
     */
    public boolean containsData(String name);

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
     * Tests whether row metadata is supplied for this name
     * 
     * @param name
     *            The metadata name
     * @return <tt>true</tt> if this instance contains the metadata,
     *         <tt>false</tt> otherwise
     */
    public boolean containsRowMetadata(String name);

    /**
     * Tests whether column metadata is supplied for this name
     * 
     * @param name
     *            The metadata name
     * @return <tt>true</tt> if this instance contains the metadata,
     *         <tt>false</tt> otherwise
     */
    public boolean containsColumnMetadata(String name);

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
