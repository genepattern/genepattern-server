/*
 * AnythingParser.java
 *
 * Created on August 22, 2003, 1:46 PM
 */

package org.genepattern.gpge.io.parsers;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.genepattern.data.DataModel;
import org.genepattern.data.DataObjector;
import org.genepattern.io.DefaultSummaryInfo;
import org.genepattern.io.SummaryInfo;
import org.genepattern.io.parsers.DataParser;

/**
 * Can read in any kind of input stream. Note this should not be used by
 * UniversalDecoder since in some of the cases it would end up getting some of
 * the files that should be handled by other parsers.
 * 
 * @author kohm
 */
public class AnythingParser implements DataParser {

	//    /** Creates a new instance of AnythingParser
	//     * @param name the name of this parser
	//     */
	//    public AnythingParser(final String name) {
	//        this.name = name;
	//    }
	/**
	 * Creates a new instance of AnythingParser
	 */
	public AnythingParser() {
		this.name = "Unknown data 'parser'";
	}

	// DataParser interface method signature

	/**
	 * determines if the data in the input stream can be decoded by this
	 * implementation
	 * 
	 * Note: this method does not throw a <CODE>ParseException</CODE>
	 * 
	 * @param in
	 *            the input stream
	 * @throws IOException
	 *             if a problem occurs due to an I/O operation
	 * @return true if this implementation can decode the data in the <CODE>
	 *         InputStream</CODE>
	 *  
	 */
	public boolean canDecode(final InputStream in) throws IOException {
		return true;
	}

	/**
	 * reads the header lines and creates a summary object
	 * 
	 * @param in
	 *            the input stream
	 * @throws IOException
	 *             if a problem occurs due to an I/O operation
	 * @throws ParseException
	 *             if there is a problem with the format or content of the data
	 * @return SummaryInfo some meta information about the data or a record of
	 *         the exception
	 *  
	 */
	public SummaryInfo createSummary(final InputStream in) throws IOException,
			ParseException {
		final Map primary = new HashMap(3);
		//primary.put("Text_type=", "Simple_text");
		primary.put("Size=", (long) (Math.ceil(in.available() / 1024.0))
				+ " KB");
		return new DefaultSummaryInfo(primary, null, DataModel.UNKNOWN_MODEL);
	}

	/**
	 * returns an array of file extensions. Files of the format that this parser
	 * can handle typically have these extensions. Of couse this is no garantee
	 * that the file really is of the proper format. Note that the file
	 * extensions are without the extension seperator which is usually a dot,
	 * '.'character, a.k.a. a period.
	 * 
	 * Note that the file extension strings are expected be in <I>lower case
	 * </I> for convenience and the array <B>sorted </B>!
	 * 
	 * @return String[] an array of file extensions
	 *  
	 */
	public String[] getFileExtensions() {
		return EXTS;
	}

	/**
	 * reads the header lines and returns them as a <CODE>String</CODE>
	 * 
	 * @param in
	 *            the input stream
	 * @throws IOException
	 *             if a problem occurs due to an I/O operation
	 * @throws ParseException
	 *             if there is a problem with the format or content of the data
	 * @return String the header lines as one <CODE>String</CODE>
	 *  
	 */
	public String getFullHeader(final InputStream in) throws IOException,
			ParseException {
		return "";
	}

	/**
	 * Parses the <CODE>InputStream</CODE> to create a <CODE>DataObjector
	 * </CODE> instance with the specified name
	 * 
	 * @return DataObjector the resulting data object
	 * @param name
	 *            the name given to the resulting <CODE>DataObjector</CODE>
	 * @param in
	 *            the input stream
	 * @throws IOException
	 *             if a problem occurs due to an I/O operation
	 * @throws ParseException
	 *             if there is a problem with the format or content of the data
	 *  
	 */
	public DataObjector parse(final InputStream in, final String name)
			throws IOException, ParseException {
		throw new UnsupportedOperationException(
				"This method shouldn't be getting called");
		//        final StringBuffer buf = new StringBuffer();
		//        final BufferedReader reader = new BufferedReader(new
		// InputStreamReader(in));
		//        for(String line = reader.readLine(); line != null; line =
		// reader.readLine()) {
		//            buf.append(line);
		//            buf.append('\n');
		//            //processLine(line);
		//        }
		//        return new TextData(name, buf.toString());
	}

	// standard methods to override
	/**
	 * returns the name of this DataParser
	 * 
	 * @return String, a text representation of this parser
	 */
	public final String toString() {
		return name;
	}

	// fields
	/** the array of extensions that this supports */
	protected static final String[] EXTS = new String[0];

	/** the name of this Parser */
	protected final String name;
}