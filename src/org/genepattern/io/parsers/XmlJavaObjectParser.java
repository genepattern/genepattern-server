/*
 * XmlJavaObjectParser.java
 *
 * Created on August 21, 2002, 10:16 PM
 */

package org.genepattern.io.parsers;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.genepattern.data.DataModel;
import org.genepattern.data.DataObjector;
import org.genepattern.io.DataObjectWrapper;
import org.genepattern.io.DeSerializer;
import org.genepattern.io.DefaultSummaryInfo;
import org.genepattern.io.SummaryError;
import org.genepattern.io.SummaryInfo;
import org.genepattern.util.AbstractReporter;
import org.genepattern.util.Reporter;

/**
 * Parses XML-serialized Java Object files to create Java Objects.
 * 
 * @author kohm
 */
public class XmlJavaObjectParser extends AbstractDataParser {

	/** Creates a new instance of XmlJavaObjectParser */
	public XmlJavaObjectParser() {
		super(new String[] { "obj", "xml" });
		this.reporter = AbstractReporter.getInstance();
	}

	// DataParser interface signature methods

	/**
	 * determines if the data in the input stream can be decoded by this
	 * implementation
	 * 
	 * @param in
	 *            the input stream
	 * @throws IOException
	 *             if a problem occurs due to an I/O operation
	 * @return true if this implementation can decode the data in the
	 *         InputStream
	 *  
	 */
	public boolean canDecode(final InputStream in) throws IOException {
		final LineReader reader = new LineReader(in);
		//final String first = reader.readLine().toLowerCase();
		final String first = reader.readLineLowerCase();
		if (first == null)
			return false;
		if (!first.startsWith(XML_STUB))
			return false;
		final String second = reader.readLine().toLowerCase();
		if (!(second.indexOf(CLASS_STUB) >= 0 && second.indexOf(DECODER_STUB) >= 0))
			return false;
		final String third = reader.readLine().toLowerCase();
		if (!(third.startsWith(OBJ_STUB) && third.indexOf(CLASS_STUB) >= 0))
			return false;

		return true;
	}

	/**
	 * Parses the InputStream to create a DataObjector instance with the
	 * specified name
	 * 
	 * @param in
	 *            the input stream
	 * @throws IOException
	 *             if a problem occurs due to an I/O operation
	 * @throws ParseException
	 *             if there is a problem with the format or content of the data
	 * @return the resulting data object
	 *  
	 */
	public DataObjector parse(InputStream in, String name) throws IOException,
			ParseException {
		final Object object = DeSerializer.decode(in);
		if (object instanceof DataObjector)
			return (DataObjector) object;
		return new DataObjectWrapper(name, object);
	}

	/** reads the header lines and creates a summary */
	public SummaryInfo createSummary(final InputStream in) throws IOException,
			ParseException {
		final LineReader reader = new LineReader(in);
		Exception exception = null;
		String class_name = null;
		final Map primary = new HashMap(5);
		try {
			final String first = reader.readLine().toLowerCase();
			final String second = reader.readLine();
			final String third = reader.readLine();
			primary.put(FORMAT, first.startsWith(XML_STUB) ? XML : UNKNOWN);

			final String decoder_name = extractClassName(second);
			primary.put(DECODER, decoder_name);

			class_name = extractClassName(third);
			primary.put(CLASS, class_name);

		} catch (IOException ex) {
			exception = ex;
			reporter.logWarning(getClass() + " cannot read stream "
					+ ex.getMessage());
		}
		if (exception != null)
			return new SummaryError(null, exception, this, primary, null);
		final DataModel model = DataModel.findModel(class_name);
		return new DefaultSummaryInfo(primary, null, model);
	}

	/** reads the header lines (first three) and returns them as a String */
	public String getFullHeader(InputStream in) throws IOException,
			ParseException {
		final StringBuffer buf = new StringBuffer();
		final LineReader reader = new LineReader(in);
		buf.append(reader.readLine());
		buf.append(reader.readLine());
		buf.append(reader.readLine());
		return buf.toString();
	}

	// helpers
	/** extracts the class name from the text */
	private String extractClassName(final String text) {
		final int index = text.indexOf(CLASS_STUB);
		if (index >= 0) {
			final int i = index + CLASS_STUB.length() + 1;
			final int e = text.indexOf('"', i + 1);
			return text.substring(i, e);
		} else
			return UNKNOWN;
	}

	// fields
	/** identifies the first line */
	protected static final String XML_STUB = "<?xml ";

	/** identifies the second line */
	protected static final String CLASS_STUB = "class=";

	/** identifies the third line */
	protected static final String OBJ_STUB = "<object ";

	/** identifies the third line */
	protected static final String DECODER_STUB = "java.beans.XMLDecoder";

	/** static String */
	private static final String UNKNOWN = "Unknown";

	/** static String */
	private static final String XML = "XML";

	/** static String */
	private static final String DECODER = "Decoder=";

	/** static String */
	private static final String CLASS = "Class=";

	/** static String */
	private static final String FORMAT = "Format=";

	/** where warnings and error messages go */
	private final Reporter reporter;

	// I N N E R C L A S S E S

}