/*
 * LineReader.java
 *
 * Created on February 21, 2003, 11:14 AM
 */

package org.genepattern.io.parsers;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Keeps track of reading the lines, the total number and number of non blank lines
 * Also throws an exception if the EOF is reached unexpectedly.
 * @author  kohm
 */
public class LineReader {
    
    /** Creates a new instance of LineReader
     * @param input the inpust stream
     */
    public LineReader(final InputStream input) {
        this(createReader(input));
    }
    /** Creates a new instance of LineReader
     * @param file the file to read from
     * @throws FileNotFoundException if the specified file does not exist
     */
    public LineReader(final File file) throws FileNotFoundException {
        this(createReader(file));
    }
    /** Creates a new instance of LineReader */
    public LineReader() {
        this((BufferedReader)null);
    }
    /** utility constructor */
    private LineReader(final BufferedReader reader) {
        this.reader = reader;
    }
    
    /** Reads the current line skipping blank ones or ones with all white space
     * @throws EOFException if file no longer exists
     * @throws IOException if error occurs during an I/O operation
     * @return the line read in
     */
    public final String readLine() throws EOFException, IOException {
        final BufferedReader reader = this.reader; // make local and final
        final String current = setCurrentLine(reader.readLine());
        
        total++;
        if(current == null) {
            if( expect_eof ) {
                return null;
            } else {
                throw new EOFException("Error: Not expecting to reach end"
                    +" of data at line " + total
                    +( (expected_row_count < 0) ? "!": "should be " //FIXME count is wrong!
                    +(expected_row_count - num)+" lines more!" ) );
            }
                
        }
        
        final String the_line = current.trim();
        if( skip_blank_lines && the_line.length() == 0 ) {// skip blank lines
            return readLine();
        }
        num++; // only counts when line not skipped
        return the_line;
    }
    /** Reads the current line skipping blank ones or ones with all white space
     * returning the line in lower case
     * @throws EOFException if file no longer exists
     * @throws IOException if error occurs during an I/O operation
     * @return the line read in
     */
    public final String readLineLowerCase() throws EOFException, IOException {
        final String the_line = readLine();
        if( the_line == null )
            return null;
        return the_line.toLowerCase();
    }
    /** sets the current line
     * This can be overridden by supclasses
     * but must be called to set the line variable
     *
     * protected String setCurrentLine(final String current) {
     *    // do somthing with it
     *    final String something = processString(current);
     *    return super.setCurrentLine(something);
     * }
     * @param current the new line
     * @return String the line
     */
    protected String setCurrentLine(final String current) {
        this.line = current;
        return current;
    }
    /** gets the current line
     * @return String the current line
     */
    public final String getCurrentLine() {
        return line;
    }
    /** sets the expected number of non blank lines
     * @param expected the expected line count
     */
    public final void setExpectedNumLines(final int expected) {
        this.expected_row_count = expected;
    }
    /** gets the total number of lines read one-based
     * @return the total number of lines blank and otherwise
     */
    public final int getTotalLineCount() {
        return total;
    }
    /** get the number of non-blank lines read
     * @return int, the total number of non-blank lines read
     */
    public final int getLineCount() {
        return num;
    }
    /** not normally used - closes the Reader
     * @throws IOException if problem closing file
     */
    public void close() throws IOException {
        reader.close();
    }
    // helper methods
    
    /** creates the reader
     * @param input the input stream
     * @return BufferedReader
     */
    public static final BufferedReader createReader(final InputStream input) {
        return new BufferedReader(new InputStreamReader(input), 32000);
    }
    /** creates the reader
     * @param file the file to read from
     * @throws FileNotFoundException if the specified file does not exist
     * @return BufferedReader
     */
    public static final BufferedReader createReader(final File file) throws FileNotFoundException {
        return new BufferedReader(new FileReader(file), 32000);
    }
    /** sets the BufferedReader
     * @param reader the new reader
     */
    protected void setReader(final BufferedReader reader) {
        this.reader = reader;
    }
    /** if set false then if EOF is reached throws EOFException
     * @param expect_eof if false then readLine method throws end of file exception if reached
     */
    protected void setExpectEOF(final boolean expect_eof) {
        this.expect_eof = expect_eof;
    }
    /** If true skips blank lines. Some parsers at certain sections of the file format
     * must accept blank lines and this will need to be set to false for awhile.
     * @param skip_blank if true skips blank lines
     */
    protected void setSkipBlankLines(final boolean skip_blank) {
        this.skip_blank_lines = skip_blank;
    }
    
    //fields
    /** the current line */
    private String line;
    /** the total number of lines read */
    private int total;
    /** the number of non-blank lines */
    private int num;
    /** the Buffered Reader that is used to read a line */
    private BufferedReader reader;
    /** the expected number of lines or -1 if not known */
    private int expected_row_count = -1;
    /** if false and reaches EOF then throws Exception */
    private boolean expect_eof = true;
    /** if true skips blank lines */
    private boolean skip_blank_lines = true;
    
}
