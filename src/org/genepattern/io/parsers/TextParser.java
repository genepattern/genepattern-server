/*
 * TextParser.java
 *
 * Created on April 11, 2003, 5:46 PM
 */

package org.genepattern.io.parsers;

import java.io.*;
import java.text.ParseException;
import java.util.*;

import org.genepattern.data.DataObjector;
import org.genepattern.io.DefaultSummaryInfo;
import org.genepattern.io.SummaryInfo;
import org.genepattern.io.TextData;
import org.genepattern.util.*;

import org.genepattern.server.*;
import org.genepattern.io.*;
import org.genepattern.data.*;
import org.genepattern.io.*;
/** Can read in any kind of text input stream.
 * @author kohm
 */
public class TextParser implements DataParser {
    
    /** Creates a new instance of TextParser
     * @param name the name of this parser
     */
    public TextParser(final String name) {
        this.name = name;
    }
    
    // DataParser interface method signature
    
//    // abstract methods
//    /** creates a DataObjector instance from the text */
//    abstract protected DataObjector createDataObjector();
//    /** gets the text data line by line */
//    abstract protected void processLine(String line);
    
    /** determines if the data in the input stream can be decoded by this implementation
     *
     * Note: this method does not throw a <CODE>ParseException</CODE>
     * @param in the input stream
     * @throws IOException if a problem occurs due to an I/O operation
     * @return true if this implementation can decode the data in the <CODE>InputStream</CODE>
     *
     */
    public boolean canDecode(final InputStream in) throws IOException {
        // read 100 bytes of the file and if it contains control characters 
        // return false else true
        return true; // FIXME what about bin files
    }
    /** reads the header lines and creates a summary object
     *
     * @param in the input stream
     * @throws IOException if a problem occurs due to an I/O operation
     * @throws ParseException if there is a problem with the format or content of the data
     * @return SummaryInfo some meta information about the data or a record of the exception
     *
     */
    public SummaryInfo createSummary(final InputStream in) throws IOException, ParseException {
        final Map primary = new HashMap(3);
        primary.put("Text_type=", "Simple_text");
        primary.put("Size=", (long)(Math.ceil(in.available()/1024.0)) + " KB");
//        Exception exception = null;
//        try {
//            parser.processHeader(reader.readLine(), primary);
//        } catch (IOException ex) {
//             exception = ex;
//            reporter.logWarning(getClass()+" cannot read stream "+ex.getMessage());
//        }
//        if( exception != null )
//            return new SummaryError(null, exception, this, primary, null);
        return new DefaultSummaryInfo(primary, null, TextData.DATA_MODEL);
    }
    
    /** returns an array of file extensions. Files of the format that this
     * parser can handle typically have these extensions.
     * Of couse this is no garantee that the file really is
     * of the proper format. Note that the file extensions are without the
     * extension seperator which is usually a dot, '.'character, a.k.a. a period.
     *
     * Note that the file extension strings are expected be in <I>lower case</I> for convenience
     *      and the array <B>sorted</B>!
     *
     * @return String[] an array of file extensions
     *
     */
    public String[] getFileExtensions() {
        return (String[])EXTS.clone();
    }
    
    /** reads the header lines and returns them as a <CODE>String</CODE>
     *
     * @param in the input stream
     * @throws IOException if a problem occurs due to an I/O operation
     * @throws ParseException if there is a problem with the format or content of the data
     * @return String the header lines as one <CODE>String</CODE>
     *
     */
    public String getFullHeader(final InputStream in) throws IOException, ParseException {
        final StringBuffer buf = new StringBuffer();
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in));
        int i = 0;
        for(String line = reader.readLine(); line != null && i < 5; line = reader.readLine(), i++) {
            buf.append(line);
            buf.append('\n');
        }
        return buf.toString();
    }
    
    /** Parses the <CODE>InputStream</CODE> to create a <CODE>DataObjector</CODE> instance with the specified name
     *
     * @return DataObjector the resulting data object
     * @param name the name given to the resulting <CODE>DataObjector</CODE>
     * @param in the input stream
     * @throws IOException if a problem occurs due to an I/O operation
     * @throws ParseException if there is a problem with the format or content of the data
     *
     */
    public DataObjector parse(final InputStream in, final String name) throws IOException, ParseException {
        final StringBuffer buf = new StringBuffer();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        for(String line = reader.readLine(); line != null; line = reader.readLine()) {
            buf.append(line);
            buf.append('\n');
            //processLine(line);
        }
        //return createDataObjector();
        return new TextData(name, buf.toString());
    }
    
    // standard methods to override
    /** returns the name of this DataParser
     * @return String, a text representation of this parser
     */
    public final String toString() {
        return name;
    }
    
    
    // fields
    /** the array of extensions that this supports */
    protected static final String[] EXTS = new String[] {"txt", "html", "htm", "stderr", "stdout"};
    /** the name of this Parser */
    protected final String name;
}
