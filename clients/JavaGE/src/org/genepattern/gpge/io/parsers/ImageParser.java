/*
 * ImageParser.java
 *
 * Created on August 22, 2003, 1:46 PM
 */

package org.genepattern.gpge.io.parsers;

import java.io.*;
import java.text.ParseException;
import java.util.*;
 
import javax.imageio.ImageIO;

import org.genepattern.data.*;
import org.genepattern.io.DefaultSummaryInfo;
import org.genepattern.io.SummaryInfo;
import org.genepattern.io.parsers.DataParser;
import org.genepattern.util.*;

import org.genepattern.gpge.*;
import org.genepattern.io.*;
/** Can read in Image File formatted input streams.
 * @author kohm
 */
public class ImageParser implements DataParser {
    

    /** Creates a new instance of ImageParser
     */
    public ImageParser() {
        this.name = "Image data 'parser'";
    }
    
    // DataParser interface method signature
    
    /** determines if the data in the input stream can be decoded by this implementation
     *
     * Note: this method does not throw a <CODE>ParseException</CODE>
     * @param in the input stream
     * @throws IOException if a problem occurs due to an I/O operation
     * @return true if this implementation can decode the data in the <CODE>InputStream</CODE>
     *
     */
    public boolean canDecode(final InputStream in) throws IOException {
        //final ImageInputStream imin = ImageIO.createImageInputStream(in);
        //final Iterator iter = ImageIO.getImageReaders(in);
        //return iter.hasNext();
        return false; // FIXME above code should work
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
        //primary.put("Text_type=", "Simple_text");
        primary.put("Available=", in.available() + " bytes");
        return new DefaultSummaryInfo(primary, null, DataModel.UNKNOWN_MODEL);
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
        return EXTS;
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
        throw new UnsupportedOperationException("This method shouldn't be getting called");
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
    protected static final String[] EXTS;
    /** the name of this Parser */
    protected final String name;
    /** static initializer */
    static {
        // FIXME "ImageIO.getReaderFormatNames()" code should work
        final String[] exts = null;//ImageIO.getReaderFormatNames();
        if( exts == null ) {
            System.err.println("Do something first to initialize ImageIO");
            EXTS = new String[0];
        } else 
            EXTS = exts;
        
    }
}
