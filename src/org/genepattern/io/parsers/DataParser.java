/*
 * DataParser.java
 *
 * Created on February 13, 2003, 11:54 AM
 */

package org.genepattern.io.parsers;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import org.genepattern.data.DataObjector;
import org.genepattern.io.SummaryInfo;

/**
 * Classes that implement this interface will be able to parse an input stream
 * and return an appropriate <CODE>DataObjector</CODE> instance or data
 * object.
 * 
 * @author kohm
 */
public interface DataParser {
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
	 */
	public DataObjector parse(final InputStream in, final String name)
			throws IOException, ParseException;

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
	 */
	public boolean canDecode(final InputStream in) throws IOException;

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
	 */
	public String[] getFileExtensions();

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
	 */
	public SummaryInfo createSummary(final InputStream in) throws IOException,
			ParseException;

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
	 */
	public String getFullHeader(final InputStream in) throws IOException,
			ParseException;
}